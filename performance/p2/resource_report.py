"""Associate server samples with JMeter target-request windows, without guessing missing data."""

import csv
import datetime as dt
import json
import math
import re
import statistics


SERVICES = ('backend', 'postgres', 'redis', 'nginx', 'ingress')
# Output name -> source field and divisor. Empty samples stay empty, never become zero.
METRICS = {
    'hostCpuPct': ('hostCpuPct', 1),
    'hostMemoryUsedPct': ('hostMemoryUsedPct', 1),
    'swapUsedMiB': ('swapUsedKiB', 1024),
    'containerCpuPct': ('containerCpuPct', 1),
    'javaRssMiB': ('javaRssKiB', 1024),
    'dbConnections': ('dbConnections', 1),
    'dbActive': ('dbActive', 1),
    'redisUsedMiB': ('redisUsedBytes', 1024 ** 2),
    'redisPeakMiB': ('redisPeakBytes', 1024 ** 2),
    'restartCount': ('restartCount', 1),
}
PEAK_FIELDS = [*METRICS, 'containerMemoryMiB', 'restartDelta', 'oomKilled', 'collectionErrors']


def timestamp(value):
    parsed = dt.datetime.fromisoformat(value.replace('Z', '+00:00'))
    if parsed.tzinfo is None:
        raise ValueError('Resource association requires timestamps with a UTC offset')
    return parsed.timestamp()


def number(value):
    try:
        result = float(str(value).rstrip('%'))
        return result if math.isfinite(result) else None
    except (ValueError, TypeError):
        return None


def memory_mib(value):
    match = re.fullmatch(r'\s*([\d.]+)\s*(B|KiB|MiB|GiB|TiB|kB|KB|MB|GB|TB)\s*', value.split('/')[0])
    if not match:
        return None
    units = {'B': 1, 'KiB': 1024, 'MiB': 1024 ** 2, 'GiB': 1024 ** 3, 'TiB': 1024 ** 4,
             'kB': 1000, 'KB': 1000, 'MB': 1000 ** 2, 'GB': 1000 ** 3, 'TB': 1000 ** 4}
    return float(match[1]) * units[match[2]] / 1024 ** 2


def maximum(values):
    measured = [value for value in values if value is not None and value != '']
    return round(max(measured), 3) if measured else ''


def load_samples(directory):
    files = sorted(directory.rglob('resources-*.csv'))
    if not files:
        raise ValueError('No resources-*.csv found in the supplied resource directory')
    samples = {service: [] for service in SERVICES}
    for filename in files:
        metadata = filename.with_name('collection.json')
        interval = float(json.loads(metadata.read_text(encoding='utf-8')).get('intervalSeconds', 5)) if metadata.exists() else 5
        if not math.isfinite(interval) or interval <= 0:
            raise ValueError('Invalid sampling interval in ' + str(metadata))
        with filename.open(encoding='utf-8-sig', newline='') as handle:
            for row in csv.DictReader(handle):
                service = row.get('service')
                if service not in samples:
                    continue
                row['_time'] = timestamp(row['time'])
                row['_interval'] = interval
                samples[service].append(row)
    for service in samples:
        # Copies of the same sampling session must not double-count samples or errors.
        samples[service] = sorted({row['time']: row for row in samples[service]}.values(), key=lambda row: row['_time'])
    return samples


def round_resources(row, service, samples):
    result = {key: row.get(key, '') for key in ('scenario', 'phase', 'run', 'warmup', 'startedAt', 'endedAt')}
    result.update(service=service, samples=0, status='NO_WINDOW', maxGapSeconds='', **dict.fromkeys(PEAK_FIELDS, ''))
    if not row.get('startedAt') or not row.get('endedAt'):
        return result
    start, end = timestamp(row['startedAt']), timestamp(row['endedAt'])
    if end < start:
        raise ValueError('Round endedAt precedes startedAt')
    selected = [sample for sample in samples if start <= sample['_time'] <= end]
    result.update(samples=len(selected), status='NO_SAMPLES')
    if not selected:
        return result
    for output, (source, divisor) in METRICS.items():
        result[output] = maximum(value / divisor if (value := number(sample.get(source))) is not None else None for sample in selected)
    result['containerMemoryMiB'] = maximum(memory_mib(sample.get('containerMemory', '')) for sample in selected)
    oom = [str(sample.get('oomKilled', '')).lower() for sample in selected]
    result['oomKilled'] = 1 if 'true' in oom else 0 if all(value == 'false' for value in oom) else ''
    result['collectionErrors'] = sum(bool(sample.get('collectionError')) for sample in selected)
    interval = statistics.median(sample['_interval'] for sample in selected)
    gaps = [selected[0]['_time'] - start, end - selected[-1]['_time']]
    gaps += [later['_time'] - earlier['_time'] for earlier, later in zip(selected, selected[1:])]
    result['maxGapSeconds'] = round(max(gaps), 3)
    before = [sample for sample in samples if sample['_time'] <= start]
    baseline = number(before[-1].get('restartCount')) if before and start - before[-1]['_time'] <= interval * 2.5 else None
    counts = [number(sample.get('restartCount')) for sample in selected]
    if baseline is not None and all(value is not None for value in counts):
        # A counter decrease indicates a recreated container; do not silently report zero restarts.
        sequence = [baseline, *counts]
        if all(later >= earlier for earlier, later in zip(sequence, sequence[1:])):
            result['restartDelta'] = max(counts) - baseline
    required = ['hostCpuPct', 'hostMemoryUsedPct', 'swapUsedKiB', 'containerCpuPct', 'restartCount']
    required += {'backend': ['javaRssKiB'], 'postgres': ['dbConnections', 'dbActive'],
                 'redis': ['redisUsedBytes', 'redisPeakBytes']}.get(service, [])
    incomplete = any(any(number(sample.get(key)) is None for key in required)
                     or memory_mib(sample.get('containerMemory', '')) is None
                     or str(sample.get('oomKilled', '')).lower() not in ('true', 'false') for sample in selected)
    if end - start < interval or len(selected) < 2:
        result['status'] = 'INSUFFICIENT'
    elif result['collectionErrors'] or incomplete or max(gaps) > interval * 2.5 or result['restartDelta'] == '':
        result['status'] = 'PARTIAL'
    else:
        result['status'] = 'SAMPLED'
    return result


def write_csv(path, rows):
    if not rows:
        return
    with path.open('w', encoding='utf-8-sig', newline='') as handle:
        writer = csv.DictWriter(handle, list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)


def summary_descriptions(summaries):
    groups = {}
    for row in summaries:
        groups.setdefault((row['scenario'], row['phase']), []).append(row)
    result = {}
    for key, group in groups.items():
        def peak(field):
            return maximum(number(row.get(field)) for row in group)

        def display(field):
            value = peak(field)
            return '缺样' if value == '' else str(value)

        status = ','.join(sorted({row['status'] for row in group}))
        containers = '; '.join(f"{row['service']} {row['containerCpuPct'] if row['containerCpuPct'] != '' else '缺样'}%/"
                               f"{row['containerMemoryMiB'] if row['containerMemoryMiB'] != '' else '缺样'}MiB" for row in group)
        result[key] = {
            'cpuRssHeap': f"{status}; 整机CPU {display('hostCpuPct')}%/内存 {display('hostMemoryUsedPct')}%/swap {display('swapUsedMiB')}MiB; RSS {display('javaRssMiB')}MiB; {containers}; 堆待核对",
            'dbRedisGc': f"DB连接/活跃 {display('dbConnections')}/{display('dbActive')}; Redis {display('redisUsedMiB')}MiB; OOM {display('oomKilled')}; 重启增量见resource-summary.csv; GC待核对",
        }
    return result


def read_resource_summary(directory):
    path = directory / 'resource-summary.csv'
    if not path.exists():
        return {}
    with path.open(encoding='utf-8-sig', newline='') as handle:
        return summary_descriptions(list(csv.DictReader(handle)))


def summarize_resources(directory, rows, source):
    samples = load_samples(source)
    details = [round_resources(row, service, samples[service]) for row in rows for service in SERVICES]
    write_csv(directory / 'resource-results.csv', details)
    groups = {}
    for row in details:
        if not row['warmup']:
            groups.setdefault((row['scenario'], row['phase'], row['service']), []).append(row)
    summaries = []
    for (scenario, phase, service), group in groups.items():
        summary = dict(scenario=scenario, phase=phase, service=service, rounds=len(group),
                       status=','.join(sorted({row['status'] for row in group})))
        for field in PEAK_FIELDS:
            values = [row[field] for row in group]
            summary[field] = maximum(values)
            if field in ('restartDelta', 'collectionErrors'):
                summary[field] = sum(values) if all(value != '' for value in values) else ''
        summaries.append(summary)
    write_csv(directory / 'resource-summary.csv', summaries)
    lines = ['# P2 资源汇总', '', '仅匹配正式轮目标请求窗口；数值为采样峰值，重启增量/采样错误为合计。SAMPLED 不表示资源验收通过。', '',
             '| 场景 / 阶段 / 服务 | 轮数 / 采样状态 | 整机 CPU / 内存 % | 容器 CPU % / MiB | RSS MiB | DB 连接 / 活跃 | Redis MiB | 重启增量 / OOM / 采样错误 |',
             '| --- | --- | --- | --- | --- | --- | --- | --- |']
    for row in summaries:
        value = lambda key: '缺样' if row[key] == '' else row[key]
        lines.append(f"| {row['scenario']} / {row['phase']} / {row['service']} | {row['rounds']} / {row['status']} | "
                     f"{value('hostCpuPct')} / {value('hostMemoryUsedPct')} | {value('containerCpuPct')} / {value('containerMemoryMiB')} | "
                     f"{value('javaRssMiB')} | {value('dbConnections')} / {value('dbActive')} | {value('redisUsedMiB')} | "
                     f"{value('restartDelta')} / {value('oomKilled')} / {value('collectionErrors')} |")
    (directory / 'resource-summary.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    return summary_descriptions(summaries)
