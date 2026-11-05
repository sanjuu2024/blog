#!/usr/bin/env python3
"""Run the independent P2 server acceptance suite and export measured results."""

import argparse
import csv
import datetime as dt
import json
import math
import re
from pathlib import Path
import shutil
import statistics
import subprocess
import sys
import uuid


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_DIR = SCRIPT_DIR.parents[1]
# threads, RPS, p95 target, p99 target; rates are totals across the thread group.
READS = {
    "P2-BASE-01": (10, 10, 500, 1000),
    "P2-BASE-02": (10, 10, 500, 1000),
    "P2-BASE-03": (10, 5, 1000, 2000),
    "P2-BASE-04": (10, 5, 500, 1000),
    "P2-BASE-05": (10, 5, 500, 1000),
    "P2-BASE-06": (20, 15, 750, 1500),
    "P2-BASE-09": (10, 10, 750, 1500),
    "P2-PERF-01": (10, 5, 1000, 2000),
    "P2-PERF-02": (10, 10, 500, 1000),
    "P2-PERF-03": (5, 2, 1000, 2000),
    "P2-PERF-04": (10, 10, 500, 1000),
    "P2-PERF-05": (5, 5, 500, 1000),
    "P2-PERF-11": (10, 5, 500, 1000),
    "P2-PERF-12": (20, 5, 750, 1500),
}
WRITES = {"P2-BASE-07", "P2-BASE-08", *(f"P2-PERF-{n:02}" for n in range(6, 11))}
DEFAULTS = [f"P2-BASE-{n:02}" for n in range(1, 7)] + [f"P2-PERF-{n:02}" for n in (1, 2, 3, 4, 5, 11)]
FIELDS = [
    "scenario", "phase", "run", "warmup", "sha", "samples", "actualRps", "targetRps",
    "unexpectedErrors", "errorPct", "expectedRejections", "meanMs", "p50Ms", "p95Ms", "p99Ms",
    "httpResult", "jtl", "cpuRssHeap", "dbRedisGc", "dataVerification",
]


def percentile(values, fraction):
    ordered = sorted(values)
    return ordered[max(0, math.ceil(len(ordered) * fraction) - 1)] if ordered else 0


def outside_repository(path):
    resolved = Path(path).expanduser().resolve()
    if resolved == REPO_DIR or REPO_DIR in resolved.parents:
        raise ValueError("Keep properties and reports outside the repository")
    return resolved


def read_properties(path):
    """Read the simple key=value UTF-8 format used by p2.properties.example."""
    result = {}
    for raw in path.read_text(encoding="utf-8-sig").splitlines():
        line = raw.strip()
        if not line or line.startswith(("#", "!")):
            continue
        key, sep, value = line.partition("=")
        if not sep or "\\" in raw:
            raise ValueError("Use plain key=value properties, forward slashes, no Java escape sequences")
        result[key.strip()] = value.strip()
    return result


def metrics(jtl, scenario, phase, run, warmup, target, sha, exit_code):
    with jtl.open(encoding="utf-8-sig", newline="") as handle:
        all_rows = list(csv.DictReader(handle))
    rows = [row for row in all_rows if row["label"].startswith("TARGET ")]
    setup_errors = sum(row.get("success") != "true" for row in all_rows if not row["label"].startswith("TARGET "))
    errors = sum(row.get("success") != "true" for row in rows) + setup_errors
    elapsed = [float(row["elapsed"]) for row in rows]
    start = min((float(row["timeStamp"]) for row in rows), default=0)
    end = max((float(row["timeStamp"]) + float(row["elapsed"]) for row in rows), default=0)
    # A single concurrent batch has no useful throughput target.
    rps = len(rows) / max((end - start) / 1000, 0.001) if rows else 0
    if exit_code or not rows:
        errors = max(errors, 1)
    return {
        "scenario": scenario, "phase": phase, "run": run, "warmup": warmup, "sha": sha,
        "samples": len(rows), "actualRps": round(rps, 3), "targetRps": target,
        "unexpectedErrors": errors, "errorPct": round(100 * errors / max(len(rows), 1), 3),
        "expectedRejections": sum(row.get("responseMessage", "").startswith("EXPECTED_REJECTION") for row in rows),
        "meanMs": round(statistics.mean(elapsed), 3) if elapsed else 0,
        "p50Ms": percentile(elapsed, .50), "p95Ms": percentile(elapsed, .95),
        "p99Ms": percentile(elapsed, .99), "httpResult": "HTTP_FAIL" if errors else "HTTP_PASS",
        "jtl": str(jtl), "cpuRssHeap": "MANUAL", "dbRedisGc": "MANUAL",
        "dataVerification": "DB_PENDING" if scenario in WRITES else "CONTENT_AND_RESOURCES_PENDING",
    }


def evaluate(row):
    if row["httpResult"] != "HTTP_PASS":
        return False
    if row["scenario"] in READS:
        _, _, p95, p99 = READS[row["scenario"]]
        return (row["p95Ms"] <= p95 and row["p99Ms"] <= p99
                and row["actualRps"] >= row["targetRps"] * .9)
    return True


def export_results(directory, rows):
    with (directory / "run-results.csv").open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    groups = {}
    for row in rows:
        if not row["warmup"]:
            groups.setdefault((row["scenario"], row["phase"]), []).append(row)
        elif row["httpResult"] != "HTTP_PASS":
            groups.setdefault((row["scenario"], row["phase"] + "-warmup-failed"), []).append(row)
    summaries = []
    for (scenario, phase), group in groups.items():
        summary = {"scenario": scenario, "phase": phase, "runs": len(group), "sha": group[0]["sha"]}
        for key in ("samples", "actualRps", "meanMs", "p50Ms", "p95Ms", "p99Ms"):
            summary[key] = round(statistics.median(row[key] for row in group), 3)
        summary.update({
            "worstP95Ms": max(row["p95Ms"] for row in group),
            "worstP99Ms": max(row["p99Ms"] for row in group),
            "unexpectedErrors": sum(row["unexpectedErrors"] for row in group),
            "maxErrorPct": max(row["errorPct"] for row in group),
            "expectedRejections": sum(row["expectedRejections"] for row in group),
            "result": "HTTP_PASS" if all(evaluate(row) for row in group) else "FAIL",
            "cpuRssHeap": "待填写", "dbRedisGc": "待填写",
            "dataVerification": group[0]["dataVerification"],
        })
        summaries.append(summary)
    if summaries:
        with (directory / "summary.csv").open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, list(summaries[0]))
            writer.writeheader()
            writer.writerows(summaries)
    lines = ["# P2 HTTP 压测汇总", "", "数值为正式轮次中位数；任一轮失败会保留。资源及数据库核对需人工补充。", "",
             "| 编号 / 阶段 | SHA | 轮数 / 样本数中位数 / RPS | 非预期错误 / 最差轮错误率 | p50 / p95 / p99 ms | 最差 p95 / p99 | CPU / RSS / 堆 | DB / Redis / GC | 结论与数据核对 |",
             "| --- | --- | --- | --- | --- | --- | --- | --- | --- |"]
    for row in summaries:
        lines.append(f"| {row['scenario']} / {row['phase']} | {row['sha']} | {row['runs']} / {row['samples']} / {row['actualRps']} | "
                     f"{row['unexpectedErrors']} / {row['maxErrorPct']}% | {row['p50Ms']} / {row['p95Ms']} / {row['p99Ms']} | "
                     f"{row['worstP95Ms']} / {row['worstP99Ms']} | 待填写 | 待填写 | {row['result']}; {row['dataVerification']} |")
    (directory / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return summaries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--properties")
    parser.add_argument("--out", default=str(Path.home() / "blog-p2-acceptance" / "performance"))
    parser.add_argument("--jmeter", default="jmeter")
    parser.add_argument("--scenarios", default=",".join(DEFAULTS))
    parser.add_argument("--warmup-seconds", type=int, default=120)
    parser.add_argument("--run-seconds", type=int, default=300)
    parser.add_argument("--formal-runs", type=int, default=3)
    parser.add_argument("--ramp-seconds", type=int, default=30)
    parser.add_argument("--stability-seconds", type=int, default=1800)
    parser.add_argument("--summarize", help="Rebuild summaries from an existing run-results.csv")
    parser.add_argument("--pause-between-phases", action="store_true", help="Pause for SQL snapshots between like/unlike or publish/read-all phases")
    parser.add_argument("--html-report", action="store_true", help="Generate each JMeter HTML dashboard; CSV/JTL/Markdown are always exported")
    args = parser.parse_args()
    if args.summarize:
        directory = outside_repository(args.summarize)
        with (directory / "run-results.csv").open(encoding="utf-8-sig", newline="") as handle:
            rows = list(csv.DictReader(handle))
        numeric = ("samples", "actualRps", "targetRps", "unexpectedErrors", "errorPct", "expectedRejections", "meanMs", "p50Ms", "p95Ms", "p99Ms")
        for row in rows:
            for key in numeric:
                row[key] = float(row[key])
            row["warmup"] = row["warmup"] == "True"
        export_results(directory, rows)
        print(f"Summary: {directory / 'summary.md'}")
        return 0
    if not args.properties:
        parser.error("--properties is required")
    if min(args.warmup_seconds, args.run_seconds, args.formal_runs, args.stability_seconds) < 1 or args.ramp_seconds < 0:
        parser.error("Durations and formal-runs must be positive; ramp-seconds cannot be negative")
    properties = outside_repository(args.properties)
    config = read_properties(properties)
    reserved = {"scenario", "phase", "threads", "loops", "operationDelayMs", "scriptDir", "configFile", "metadataFile", "actorSeed", "durationSeconds", "rampSeconds", "throughputPerMinute"}
    if reserved & config.keys() or any(key.startswith(("p2.", "jmeter.")) for key in config):
        raise ValueError("Private properties must not override runtime scenario, script or JMeter settings")
    if config.get("testEnvironment") != "performance":
        raise ValueError("Explicit testEnvironment=performance acknowledgement is required")
    scenarios = args.scenarios.split(",")
    if any(scenario not in READS and scenario not in WRITES for scenario in scenarios):
        raise ValueError("Unknown scenario; see README.md")
    if set(scenarios) & WRITES:
        if config.get("allowWrites") != "true" or config.get("layer") != "application":
            raise ValueError("Writes require allowWrites=true and an isolated layer=application endpoint")
    for scenario, flag in (("P2-BASE-08", "turnstileStub"), ("P2-PERF-09", "smtpStub"), ("P2-PERF-10", "allowAllUsers")):
        if scenario in scenarios and config.get(flag) != "true":
            raise ValueError(f"{scenario} requires {flag}=true and its documented prerequisites")
    executable = shutil.which(args.jmeter)
    if not executable:
        raise ValueError("JMeter executable not found; supply --jmeter")
    directory = outside_repository(args.out) / (dt.datetime.now().strftime("%Y%m%d_%H%M%S") + "_" + uuid.uuid4().hex[:6])
    directory.mkdir(parents=True)
    sha_result = subprocess.run(["git", "rev-parse", "HEAD"], cwd=REPO_DIR, capture_output=True, text=True, check=False)
    sha = sha_result.stdout.strip() if sha_result.returncode == 0 else "unknown"
    target_commit = config.get("targetCommit", "")
    if target_commit and (not re.fullmatch(r"[0-9a-f]{40}", target_commit) or target_commit != sha):
        raise ValueError("targetCommit must be a full server SHA matching the load-generator checkout")
    # Record only safe settings; never copy credentials or the complete properties file.
    (directory / "environment.json").write_text(json.dumps({
        "sha": sha, "date": dt.datetime.now().astimezone().isoformat(),
        "targetCommitConfirmed": bool(target_commit),
        "host": config.get("host"), "port": config.get("port"), "protocol": config.get("protocol"),
        "layer": config.get("layer"), "visitorMode": config.get("visitorMode", "per-thread"),
        "scenarios": scenarios, "warmupSeconds": args.warmup_seconds, "runSeconds": args.run_seconds,
        "formalRuns": args.formal_runs, "stabilitySeconds": args.stability_seconds,
        "acceptanceDuration": args.run_seconds >= 300 and args.warmup_seconds >= 120 and args.formal_runs >= 3 and args.stability_seconds >= 1800,
        "resourceEvidence": "Collect on the target host; HTTP test success is not full acceptance",
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    rows = []
    failed = False
    actor_seed = uuid.uuid4().hex

    def run(scenario, phase, number, warmup=False, rate=None, duration=None):
        threads, default_rate, _, _ = READS.get(scenario, (10, 0, 0, 0))
        target_rate = default_rate if rate is None else rate
        name = f"{scenario}-{phase}-{'warmup' if warmup else number}"
        folder = directory / name
        folder.mkdir()
        is_write = scenario in WRITES
        arguments = [executable, "-n", "-t", str(SCRIPT_DIR / ("concurrency.jmx" if is_write else "read.jmx")),
                     f"-JconfigFile={properties.as_posix()}", f"-JscriptDir={SCRIPT_DIR.as_posix()}",
                     f"-Jscenario={scenario}", f"-Jphase={phase}", f"-JactorSeed={actor_seed}",
                     f"-Jthreads={1 if scenario == 'P2-PERF-08' else threads}",
                     f"-Jloops={31 if scenario == 'P2-PERF-08' else 1}",
                     f"-JoperationDelayMs={1000 if scenario == 'P2-PERF-08' else 0}",
                     f"-JthroughputPerMinute={target_rate * 60}",
                     f"-JdurationSeconds={duration or args.run_seconds}", f"-JrampSeconds={args.ramp_seconds}",
                     f"-JmetadataFile={(folder / 'metadata.json').as_posix()}",
                     "-Jjmeter.save.saveservice.output_format=csv", "-Jjmeter.save.saveservice.print_field_names=true",
                     "-Jjmeter.save.saveservice.response_data=false", "-Jjmeter.save.saveservice.samplerData=false",
                     "-Jjmeter.save.saveservice.response_data.on_error=false",
                     "-Jjmeter.save.saveservice.requestHeaders=false", "-Jjmeter.save.saveservice.responseHeaders=false",
                     "-l", str(folder / "results.jtl"), "-j", str(folder / "jmeter.log")]
        if args.html_report:
            arguments += ["-e", "-o", str(folder / "report")]
        print(f"Running {name}: {1 if scenario == 'P2-PERF-08' else threads} threads, {target_rate} RPS, {duration or args.run_seconds}s", flush=True)
        with (folder / "console.log").open("w", encoding="utf-8") as output:
            process = subprocess.run(arguments, cwd=SCRIPT_DIR, stdout=output, stderr=subprocess.STDOUT, check=False)
        jtl = folder / "results.jtl"
        if not jtl.exists():
            raise RuntimeError(f"JMeter did not produce JTL; inspect {folder / 'console.log'}")
        row = metrics(jtl, scenario, phase, number, warmup, target_rate, sha, process.returncode)
        rows.append(row)
        export_results(directory, rows)
        # Cold JIT/startup latency is not part of acceptance; functional warmup errors still stop execution.
        passed = row["httpResult"] == "HTTP_PASS" if warmup else evaluate(row)
        print(f"  {'PASS' if passed else 'FAIL'}: samples={row['samples']}, RPS={row['actualRps']}, p95={row['p95Ms']}, errors={row['unexpectedErrors']}", flush=True)
        return passed

    try:
        for scenario in scenarios:
            if scenario in WRITES:
                phases = ("like", "unlike", "multi-like") if scenario == "P2-PERF-07" else ("publish", "read-all") if scenario == "P2-PERF-10" else ("concurrent",)
                for phase in phases:
                    if not run(scenario, phase, 1):
                        failed = True
                        break
                    if args.pause_between_phases:
                        input(f"Save the SQL snapshot for {scenario}/{phase}, then press Enter to continue: ")
                if failed:
                    break
            elif scenario == "P2-PERF-12":
                stable_rate = None
                for rate in (5, 10, 20, 40):
                    if not run(scenario, f"{rate}rps", 0, True, rate, args.warmup_seconds):
                        failed = True
                        break
                    if not run(scenario, f"{rate}rps", 1, rate=rate):
                        failed = True
                        break
                    stable_rate = rate
                if stable_rate:
                    (directory / "capacity.json").write_text(json.dumps({"lastHttpStableRps": stable_rate, "resourceVerification": "MANUAL"}), encoding="utf-8")
                    if not run(scenario, f"{stable_rate}rps-soak", 1, rate=stable_rate, duration=args.stability_seconds):
                        failed = True
            else:
                if not run(scenario, "read", 0, True, duration=args.warmup_seconds):
                    failed = True
                    break
                count = 1 if scenario == "P2-BASE-09" else args.formal_runs
                for number in range(1, count + 1):
                    duration = args.stability_seconds if scenario == "P2-BASE-09" else args.run_seconds
                    if not run(scenario, "read", number, duration=duration):
                        failed = True
                        break
                if failed:
                    break
    finally:
        export_results(directory, rows)
        print(f"Results: {directory}", flush=True)
    return 1 if failed else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, RuntimeError, OSError) as error:
        print(f"Error: {error}", file=sys.stderr)
        sys.exit(2)
