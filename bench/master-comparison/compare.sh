#!/bin/bash
# Usage: bench/master-comparison/compare.sh [ref=origin/master] [runs=3] [seconds=160]
# Compares the checked-out PonySDK with <ref> under the same load: see README.md.
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(git -C "$HERE" rev-parse --show-toplevel)
REF=${1:-origin/master} RUNS=${2:-3} SECS=${3:-160}
PORT=${BENCH_PORT:-18090}
WORK=$ROOT/build/master-comparison
FROM=40 TO=$((SECS - 20)) GC_AT=$((SECS - 10))
[ "$SECS" -ge 70 ] || { echo "seconds must be at least 70 (40 s of warm-up, 10 s measured, 20 s for the GC)"; exit 1; }

trap 'kill $(jobs -p) 2> /dev/null || true' EXIT
for p in "$PORT" $((PORT + 1)); do
    if (exec 3<> "/dev/tcp/127.0.0.1/$p") 2> /dev/null; then echo "port $p is in use (set BENCH_PORT)"; exit 1; fi
done

rm -rf "$WORK" && mkdir -p "$WORK/ref/src" "$WORK/head" "$WORK/runs"
echo "ref  $REF = $(git -C "$ROOT" rev-parse --short "$REF")"
echo "head $(git -C "$ROOT" rev-parse --short HEAD)$(git -C "$ROOT" diff --quiet HEAD -- ponysdk || echo ' + uncommitted changes in ponysdk/')"

# Both versions built by their own Gradle; the checked-out classes are copied so that a rebuild during the runs
# does not change them.
git -C "$ROOT" archive "$REF" | tar -x -C "$WORK/ref/src"
(cd "$WORK/ref/src" && ./gradlew -q --no-configuration-cache -I "$HERE/classpath.gradle" -Pbench.out="$WORK/ref" \
    :ponysdk:benchClasspath)
(cd "$ROOT" && ./gradlew -q --no-configuration-cache -I "$HERE/classpath.gradle" -Pbench.out="$WORK/head" \
    :ponysdk:benchClasspath)
cp -R "$ROOT/ponysdk/build/classes/java/main" "$WORK/head/classes"
cp -R "$ROOT/ponysdk/build/resources/main" "$WORK/head/resources"
for cp in server client; do
    sed -i.bak -e "s|$ROOT/ponysdk/build/classes/java/main|$WORK/head/classes|" \
        -e "s|$ROOT/ponysdk/build/resources/main|$WORK/head/resources|" "$WORK/head/$cp.cp"
done

JDK=${BENCH_JDK:-$(cat "$WORK/head/jdk.home")}
JAVA=$JDK/bin/java
for v in ref head; do
    "$JDK/bin/javac" -nowarn -d "$WORK/$v/out" -cp "$(cat "$WORK/$v/server.cp")" "$HERE"/src/bench/*.java
done
echo "JDK  $("$JAVA" -version 2>&1 | head -1)"
echo "load average before the runs: $(uptime | sed 's/.*load average[s]*: //')"

# run <ref|head> <sessions> <seconds> <ticks> <full GC at second, 0 = none> <name>
run() {
    local v=$1 sessions=$2 secs=$3 ticks=$4 gc_at=$5 dir=$WORK/runs/$6
    mkdir -p "$dir"
    (cd "$dir" && exec "$JAVA" -Xmx2g -Dbench.ticks="$ticks" -cp "$WORK/$v/out:$(cat "$WORK/$v/server.cp")" \
        bench.BenchServer "$PORT" > server.log 2>&1) &
    local server=$!
    for _ in $(seq 150); do grep -q '^ready' "$dir/server.log" && break; sleep 0.2; done
    grep -q '^ready' "$dir/server.log" || { echo "$6: the server did not start, see $dir/server.log"; exit 1; }
    (cd "$dir" && exec "$JAVA" -Xmx4g -cp "$WORK/$v/out:$(cat "$WORK/$v/client.cp")" \
        bench.BenchClient "$PORT" $((PORT + 1)) "$sessions" "$secs" > client.log 2>&1) &
    local client=$!
    if [ "$gc_at" -gt 0 ]; then (sleep "$gc_at" && touch "$dir/gc.req") & fi
    wait $client || { echo "$6: the client failed, see $dir/client.log"; exit 1; }
    kill $server && wait $server || true
}

# Alternated runs, each in an empty directory: protocol v3 persists its string dictionary in the working
# directory and would pre-seed the next run.
for i in 1 2; do for v in ref head; do run $v 1 3 false 0 first-$v-$i; done; done
for i in $(seq "$RUNS"); do
    for v in ref head; do
        run $v 200 "$SECS" true "$GC_AT" load-$v-$i
        echo "load-$v-$i done"
    done
done
python3 -I "$HERE/analyze.py" "$WORK/runs" "$FROM" "$TO" "$REF"
