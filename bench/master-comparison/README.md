# Master comparison bench

Measures the checked-out PonySDK against another ref under the same load. It produces the "Measured against
master" table of the V3 pull request and the "Measured" cards of the showcase Overview.

```bash
bench/master-comparison/compare.sh [ref=origin/master] [runs=3] [seconds=160]
```

About 18 minutes with the defaults. Needs git, python3, and a machine where both versions build with their own
Gradle wrapper. Both run on the JDK of the checked-out version's toolchain (JDK 25); `BENCH_JDK` overrides it,
`BENCH_PORT` (18090) moves the server and proxy ports. Logs and results go to `build/master-comparison`.

## What it measures

- `BenchEntryPoint`, the same source compiled against both versions: per session a 200-cell grid; every 100 ms,
  130 setter calls on distinct cells (100 `setText` with price strings from a fixed set of 4,096, 30 style
  toggles). Every call changes the cell's state, so neither version can skip it. 200 sessions: 260,000 calls/s.
- `BenchServer`, one JVM per version with `-Xmx2g` and the default GC. Every second it prints the setter calls,
  the process CPU time and the bytes allocated by all threads; near the end, RSS, then the heap used after two
  full GCs.
- `BenchClient` opens the sessions with that version's own `PonySDKWebDriver`, through a TCP proxy that counts
  the bytes on the wire. The driver reports the bytes after inflate.
- Runs alternate between the two versions, each in an empty directory (protocol v3 persists its string
  dictionary in the working directory and would pre-seed the next run). The window runs from 40 s to
  `seconds - 20` after the client starts.
- First page: one session without ticks, the bytes received after 3 s, WebSocket upgrade response included.

CPU and allocation cover the whole server process (Jetty, deflate, GC) divided by the setter calls. Other
workloads on the machine move CPU and RSS: compare runs of one invocation only.
