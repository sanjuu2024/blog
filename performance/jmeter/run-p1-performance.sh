#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JMETER_BIN="${JMETER_BIN:-jmeter}"
ARTICLE_ID="${ARTICLE_ID:?Set ARTICLE_ID}"
OUT_DIR="${OUT_DIR:-$HOME/blog-p1-acceptance/performance/$(date +%Y%m%d_%H%M%S)}"
mkdir -p "$OUT_DIR"
run() {
  local jmx="$1" name="$2"; shift 2
  "$JMETER_BIN" -n -t "$SCRIPT_DIR/$jmx" "$@" -l "$OUT_DIR/$name.jtl" -e -o "$OUT_DIR/$name-report"
}
for scenario in PERF-01 PERF-02 PERF-03 PERF-04 PERF-05 PERF-06; do
  for run_no in warmup 1 2 3; do
    duration=300; [ "$run_no" = warmup ] && duration=120
    run blog-p1-public-read.jmx "$scenario-run-$run_no" -Jscenario="$scenario" -JarticleId="$ARTICLE_ID" -Jthreads=10 -JrampSeconds=30 -JdurationSeconds="$duration" -JthroughputPerMinute=600
  done
done
run blog-p1-stability.jmx PERF-09 -JarticleId="$ARTICLE_ID" -Jthreads=10 -JrampSeconds=30 -JdurationSeconds=1800 -JthroughputPerMinute=600
for scenario in PERF-07 PERF-08; do
  run blog-p1-write-limit.jmx "$scenario" -Jscenario="$scenario" -JarticleId="$ARTICLE_ID" -Jusername="${USERNAME:-perf_user_001}" -Jpassword="${PASSWORD:-123456}" -JwriteThreads=10
done
echo "P1 performance suite completed: $OUT_DIR"
