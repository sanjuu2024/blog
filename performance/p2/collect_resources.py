#!/usr/bin/env python3
"""Sample the P2 Linux server deployment without exporting credentials."""

import argparse
import csv
import datetime as dt
import importlib.util
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import sys
import time
import uuid


def command(arguments):
    process = subprocess.run(arguments, capture_output=True, text=True, check=False, timeout=20)
    if process.returncode:
        raise RuntimeError("Resource collection command failed: " + arguments[0])
    return process.stdout.strip()


def cpu_snapshot():
    values = [int(value) for value in Path('/proc/stat').read_text().splitlines()[0].split()[1:9]]
    return sum(values), values[3] + values[4]


def interrupt_collection(_signum, _frame):
    raise KeyboardInterrupt


def stop_helpers(helpers):
    """Only stop the processes owned by this sampling session, then close their logs."""
    for helper in helpers:
        process = helper['process']
        helper['unexpectedExit'] = process.poll() is not None
        try:
            if process.poll() is None:
                process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
        finally:
            helper['log'].close()


def start_helpers(directory, interval):
    environment = {**os.environ, 'LC_ALL': 'C', 'TZ': 'UTC', 'S_TIME_FORMAT': 'ISO'}
    commands = [('iostat', ['iostat', '-xz', '-t', str(interval)]),
                ('vmstat', ['vmstat', '-t', str(interval)]),
                ('network', ['sar', '-n', 'DEV', str(interval)])]
    helpers = []
    try:
        for name, arguments in commands:
            handle = (directory / (name + '.txt')).open('w', encoding='utf-8')
            handle.write('# UTC collection started ' + dt.datetime.now(dt.timezone.utc).isoformat() + '\n')
            handle.flush()
            try:
                process = subprocess.Popen(arguments, stdout=handle, stderr=subprocess.STDOUT,
                                           env=environment, start_new_session=True)
            except BaseException:
                handle.close()
                raise
            helpers.append({'name': name, 'process': process, 'log': handle})
    except BaseException:
        stop_helpers(helpers)
        raise
    return helpers


def sample_containers(docker, containers, base):
    """Batch docker stats so each service does not add a separate sampling delay."""
    try:
        stats = [json.loads(line) for line in command(docker + ['stats', '--no-stream', '--format', '{{json .}}', *containers.values()]).splitlines()]
        by_id = {item['ID']: item for item in stats}
    except (RuntimeError, ValueError, KeyError, subprocess.TimeoutExpired):
        by_id = {}
    for service, container in containers.items():
        row = {**base, 'service': service}
        try:
            stats = by_id[container]
            row.update(containerCpuPct=stats['CPUPerc'], containerMemory=stats['MemUsage'])
            state = json.loads(command(docker + ['inspect', '--format', '{"restartCount":{{.RestartCount}},"oomKilled":{{.State.OOMKilled}}}', container]))
            row.update(state)
            if service == 'backend':
                status = command(docker + ['exec', container, 'sh', '-c', 'grep VmRSS /proc/1/status'])
                row['javaRssKiB'] = status.split()[1]
            elif service == 'postgres':
                values = command(docker + ['exec', container, 'sh', '-c',
                    'psql -U "$POSTGRES_USER" -d blog_p2_performance -Atc "SELECT count(*), count(*) FILTER (WHERE state = \'active\') FROM pg_stat_activity WHERE datname = current_database() AND pid <> pg_backend_pid()"'])
                row['dbConnections'], row['dbActive'] = values.split('|')
            elif service == 'redis':
                info = command(docker + ['exec', container, 'sh', '-c',
                    'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli -n 2 INFO memory'])
                memory = dict(line.split(':', 1) for line in info.splitlines() if ':' in line)
                row['redisUsedBytes'], row['redisPeakBytes'] = memory.get('used_memory', ''), memory.get('used_memory_peak', '')
        except (RuntimeError, ValueError, KeyError, IndexError, subprocess.TimeoutExpired):
            row['collectionError'] = 'Check container health; this sample is incomplete'
        row['time'] = dt.datetime.now(dt.timezone.utc).isoformat()
        yield row


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', default='blog-p2-acceptance')
    parser.add_argument('--ingress-project', default='blog-ingress')
    parser.add_argument('--out', required=True)
    parser.add_argument('--interval', type=int, default=5)
    parser.add_argument('--sudo-docker', action='store_true', help='Use sudo -n for Docker only; first run sudo -v in this terminal')
    args = parser.parse_args()
    if args.interval < 1 or not Path('/proc/meminfo').exists():
        parser.error('Run on the target Linux Docker host with a positive interval')
    if not shutil.which('docker'):
        parser.error('docker is required')
    if args.sudo_docker and not shutil.which('sudo'):
        parser.error('sudo is required for --sudo-docker')
    if any(not shutil.which(tool) for tool in ('iostat', 'vmstat', 'sar')):
        parser.error('iostat, vmstat and sar are required; install sysstat and procps')
    # Keep Python/report ownership with the operator; never prompt inside sampling.
    docker = ['sudo', '-n', 'docker'] if args.sudo_docker else ['docker']
    script = Path(__file__).with_name('run.py')
    spec = importlib.util.spec_from_file_location('p2_runner', script)
    runner = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runner)
    root = runner.private_path(args.out, directory=True)
    containers = {}
    for service in ('backend', 'postgres', 'redis', 'nginx'):
        ids = command(docker + ['ps', '-q', '--filter', 'label=com.docker.compose.project=' + args.project,
                       '--filter', 'label=com.docker.compose.service=' + service]).splitlines()
        if len(ids) != 1:
            parser.error('Expected one running container for ' + service)
        containers[service] = ids[0]
    ids = command(docker + ['ps', '-q', '--filter', 'label=com.docker.compose.project=' + args.ingress_project,
                   '--filter', 'label=com.docker.compose.service=nginx']).splitlines()
    if len(ids) != 1:
        parser.error('Expected one running shared ingress nginx; check --ingress-project')
    containers['ingress'] = ids[0]
    limits = {}
    for service, container in containers.items():
        # Select only resource fields, never serialize Config.Env or a complete docker inspect.
        limits[service] = json.loads(command(docker + ['inspect', '--format',
            '{"memoryBytes":{{.HostConfig.Memory}},"nanoCpus":{{.HostConfig.NanoCpus}},"restartCount":{{.RestartCount}},"oomKilled":{{.State.OOMKilled}}}', container]))
    directory = root / (dt.datetime.now(dt.timezone.utc).strftime('%Y%m%d_%H%M%S') + '_' + uuid.uuid4().hex[:6])
    directory.mkdir(parents=True)
    (directory / 'container-limits.json').write_text(json.dumps(limits, indent=2), encoding='utf-8')
    metadata = {'startedAt': dt.datetime.now(dt.timezone.utc).isoformat(), 'intervalSeconds': args.interval,
                'timezone': 'UTC', 'project': args.project, 'ingressProject': args.ingress_project}
    (directory / 'collection.json').write_text(json.dumps(metadata, indent=2), encoding='utf-8')
    fields = ['time', 'hostCpuPct', 'hostMemoryUsedPct', 'availableMemoryKiB', 'swapUsedKiB',
              'service', 'containerCpuPct', 'containerMemory', 'javaRssKiB', 'dbConnections', 'dbActive',
              'redisUsedBytes', 'redisPeakBytes', 'restartCount', 'oomKilled', 'collectionError']
    filename = directory / ('resources-' + dt.datetime.now(dt.timezone.utc).strftime('%Y%m%d_%H%M%S') + '.csv')
    previous = cpu_snapshot()
    print('Collecting resources + iostat/vmstat/sar in UTC; Ctrl+C stops all collectors. ' + str(directory), flush=True)
    helpers = []
    last_errors = None
    # SSH disconnection and service termination must also stop our detached helper processes.
    previous_handlers = {value: signal.signal(value, interrupt_collection) for value in (signal.SIGTERM, signal.SIGHUP)}
    try:
        helpers = start_helpers(directory, args.interval)
        with filename.open('w', encoding='utf-8-sig', newline='') as handle:
            writer = csv.DictWriter(handle, fields)
            writer.writeheader()
            while True:
                started = time.monotonic()
                current = cpu_snapshot()
                total_delta, idle_delta = current[0] - previous[0], current[1] - previous[1]
                previous = current
                mem = {line.split(':')[0]: int(line.split()[1]) for line in Path('/proc/meminfo').read_text().splitlines()}
                base = {'hostCpuPct': round(100 * (1 - idle_delta / total_delta), 2) if total_delta else '',
                        'hostMemoryUsedPct': round(100 * (1 - mem['MemAvailable'] / mem['MemTotal']), 2),
                        'availableMemoryKiB': mem['MemAvailable'], 'swapUsedKiB': mem['SwapTotal'] - mem['SwapFree']}
                failed_tools = [helper['name'] for helper in helpers if helper['process'].poll() is not None]
                errors = set()
                for row in sample_containers(docker, containers, base):
                    if failed_tools:
                        row['collectionError'] = 'Auxiliary collectors stopped: ' + ','.join(failed_tools)
                    if row.get('collectionError'):
                        errors.add(row['collectionError'])
                    writer.writerow(row)
                handle.flush()
                if errors != last_errors:
                    status = '; '.join(sorted(errors)) if errors else 'Sample saved; JMeter may start'
                    print(dt.datetime.now(dt.timezone.utc).isoformat() + ' ' + status, flush=True)
                    last_errors = errors
                time.sleep(max(0, args.interval - (time.monotonic() - started)))
    except KeyboardInterrupt:
        print('\nStopping collectors...', flush=True)
    finally:
        stop_helpers(helpers)
        for value, handler in previous_handlers.items():
            signal.signal(value, handler)
        metadata['endedAt'] = dt.datetime.now(dt.timezone.utc).isoformat()
        metadata['tools'] = {helper['name']: {'exitCode': helper['process'].returncode,
                                            'unexpectedExit': helper['unexpectedExit']} for helper in helpers}
        (directory / 'collection.json').write_text(json.dumps(metadata, indent=2), encoding='utf-8')
        print('Saved ' + str(directory), flush=True)


if __name__ == '__main__':
    try:
        main()
    except (ValueError, RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        print('Error: ' + str(error), file=sys.stderr)
        sys.exit(2)
