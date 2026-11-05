#!/usr/bin/env python3
"""Sample the P2 Linux server deployment without exporting credentials."""

import argparse
import csv
import datetime as dt
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import time


def command(arguments):
    process = subprocess.run(arguments, capture_output=True, text=True, check=False, timeout=20)
    if process.returncode:
        raise RuntimeError("Resource collection command failed: " + arguments[0])
    return process.stdout.strip()


def cpu_snapshot():
    values = [int(value) for value in Path('/proc/stat').read_text().splitlines()[0].split()[1:9]]
    return sum(values), values[3] + values[4]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', default='blog-p2-acceptance')
    parser.add_argument('--out', required=True)
    parser.add_argument('--interval', type=int, default=5)
    parser.add_argument('--sudo-docker', action='store_true', help='Use sudo -n for Docker only; first run sudo -v in this terminal')
    args = parser.parse_args()
    if args.interval < 1 or not Path('/proc/meminfo').exists():
        parser.error('Run on the Linux/WSL Docker host with a positive interval')
    if not shutil.which('docker'):
        parser.error('docker is required')
    if args.sudo_docker and not shutil.which('sudo'):
        parser.error('sudo is required for --sudo-docker')
    # Keep Python/report ownership with the operator; never prompt inside sampling.
    docker = ['sudo', '-n', 'docker'] if args.sudo_docker else ['docker']
    script = Path(__file__).with_name('run.py')
    spec = importlib.util.spec_from_file_location('p2_runner', script)
    runner = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runner)
    directory = runner.outside_repository(args.out)
    directory.mkdir(parents=True, exist_ok=True)
    containers = {}
    for service in ('backend', 'postgres', 'redis', 'nginx'):
        ids = command(docker + ['ps', '-q', '--filter', 'label=com.docker.compose.project=' + args.project,
                       '--filter', 'label=com.docker.compose.service=' + service]).splitlines()
        if len(ids) != 1:
            parser.error('Expected one running container for ' + service)
        containers[service] = ids[0]
    limits = {}
    for service, container in containers.items():
        # Select only resource fields, never serialize Config.Env or a complete docker inspect.
        limits[service] = json.loads(command(docker + ['inspect', '--format',
            '{"memoryBytes":{{.HostConfig.Memory}},"nanoCpus":{{.HostConfig.NanoCpus}},"restartCount":{{.RestartCount}},"oomKilled":{{.State.OOMKilled}}}', container]))
    (directory / 'container-limits.json').write_text(json.dumps(limits, indent=2), encoding='utf-8')
    fields = ['time', 'hostCpuPct', 'hostMemoryUsedPct', 'availableMemoryKiB', 'swapUsedKiB',
              'service', 'containerCpuPct', 'containerMemory', 'javaRssKiB', 'dbConnections', 'dbActive',
              'redisUsedBytes', 'redisPeakBytes', 'restartCount', 'oomKilled', 'collectionError']
    filename = directory / ('resources-' + dt.datetime.now().strftime('%Y%m%d_%H%M%S') + '.csv')
    previous = cpu_snapshot()
    print('Collecting host/container/DB/Redis metrics; Ctrl+C stops collection. ' + str(filename), flush=True)
    try:
        with filename.open('w', encoding='utf-8-sig', newline='') as handle:
            writer = csv.DictWriter(handle, fields)
            writer.writeheader()
            while True:
                started = time.monotonic()
                current = cpu_snapshot()
                total_delta, idle_delta = current[0] - previous[0], current[1] - previous[1]
                previous = current
                mem = {line.split(':')[0]: int(line.split()[1]) for line in Path('/proc/meminfo').read_text().splitlines()}
                base = {'time': dt.datetime.now().astimezone().isoformat(),
                        'hostCpuPct': round(100 * (1 - idle_delta / total_delta), 2) if total_delta else '',
                        'hostMemoryUsedPct': round(100 * (1 - mem['MemAvailable'] / mem['MemTotal']), 2),
                        'availableMemoryKiB': mem['MemAvailable'], 'swapUsedKiB': mem['SwapTotal'] - mem['SwapFree']}
                for service, container in containers.items():
                    row = {**base, 'service': service}
                    try:
                        stats = json.loads(command(docker + ['stats', '--no-stream', '--format', '{{json .}}', container]))
                        row.update(containerCpuPct=stats['CPUPerc'], containerMemory=stats['MemUsage'])
                        state = json.loads(command(docker + ['inspect', '--format', '{"restartCount":{{.RestartCount}},"oomKilled":{{.State.OOMKilled}}}', container]))
                        row.update(state)
                        if service == 'backend':
                            status = command(docker + ['exec', container, 'sh', '-c', 'grep VmRSS /proc/1/status'])
                            row['javaRssKiB'] = status.split()[1]
                        elif service == 'postgres':
                            values = command(docker + ['exec', container, 'sh', '-c',
                                'psql -U "$POSTGRES_USER" -d blog_p2_performance -Atc "SELECT count(*), count(*) FILTER (WHERE state = \'active\' AND pid <> pg_backend_pid()) FROM pg_stat_activity WHERE datname = current_database()"'])
                            row['dbConnections'], row['dbActive'] = values.split('|')
                        elif service == 'redis':
                            info = command(docker + ['exec', container, 'sh', '-c',
                                'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli -n 2 INFO memory'])
                            memory = dict(line.split(':', 1) for line in info.splitlines() if ':' in line)
                            row['redisUsedBytes'], row['redisPeakBytes'] = memory.get('used_memory', ''), memory.get('used_memory_peak', '')
                    except (RuntimeError, ValueError, KeyError, subprocess.TimeoutExpired):
                        row['collectionError'] = 'Check container health; this sample is incomplete'
                    writer.writerow(row)
                handle.flush()
                time.sleep(max(0, args.interval - (time.monotonic() - started)))
    except KeyboardInterrupt:
        print('\nSaved ' + str(filename))


if __name__ == '__main__':
    main()
