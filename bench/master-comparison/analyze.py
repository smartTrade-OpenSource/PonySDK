"""Prints each run of compare.sh, then the comparison table (README.md).

Usage: analyze.py <runs dir> <window start s> <window end s> <ref name>
"""
import glob
import os
import sys

RUNS, FROM, TO, REF = sys.argv[1], int(sys.argv[2]) * 1000, int(sys.argv[3]) * 1000, sys.argv[4]
CALLS_PER_S = 200 * 1300  # 200 sessions, 130 calls every 100 ms (BenchEntryPoint)


def rows(path, tag):
    with open(path) as f:
        return [line.split() for line in f if line.startswith(tag + ' ')]


def at(rs, t):
    return min(rs, key=lambda r: abs(int(r[1]) - t))


def load(run):
    s, c = rows(run + '/server.log', 'stat'), rows(run + '/client.log', 'cstat')
    live = rows(run + '/server.log', 'live')
    if not live:
        sys.exit(run + ': no "live" line in server.log (the run ended before its full GC)')
    t0 = int(c[0][1])
    s0, s1, c0, c1 = at(s, t0 + FROM), at(s, t0 + TO), at(c, t0 + FROM), at(c, t0 + TO)
    calls = int(s1[2]) - int(s0[2])
    secs = (int(c1[1]) - int(c0[1])) / 1000
    return {
        'calls/s': calls / ((int(s1[1]) - int(s0[1])) / 1000),
        'sessions': min(int(r[5]) for r in s if t0 + FROM <= int(r[1]) <= t0 + TO),
        'cpu': (int(s1[3]) - int(s0[3])) / calls,
        'alloc': (int(s1[4]) - int(s0[4])) / calls,
        'wire': (int(c1[2]) - int(c0[2])) / secs / 1e6,
        'raw': (int(c1[4]) - int(c0[4])) / secs / 1e6,
        'rss': int(live[0][2]) / 1024,
        'live': int(live[0][1]) / 1e6,
    }


METRICS = [
    ('CPU per setter call', 'cpu', '{:,.0f} ns'),
    ('Allocation per call', 'alloc', '{:.1f} B'),
    ('Stream on the wire', 'wire', '{:.3f} MB/s'),
    ('Stream before deflate', 'raw', '{:.2f} MB/s'),
    ('RSS under load', 'rss', '{:,.0f} MB'),
    ('Live heap (after full GC)', 'live', '{:.1f} MB'),
    ('First page on the wire', 'first', '{:,.0f} B'),
]

values = {}
for v in ('ref', 'head'):
    loads = []
    for run in sorted(glob.glob(f'{RUNS}/load-{v}-*')):
        r = load(run)
        loads.append(r)
        warn = '  <- not the full load' if r['sessions'] < 200 or r['calls/s'] < 0.99 * CALLS_PER_S else ''
        print(f"{os.path.basename(run):12} {r['calls/s']:8.0f} calls/s  sessions {r['sessions']}  "
              + '  '.join(f'{k} ' + f.format(r[k]) for _, k, f in METRICS if k != 'first') + warn)
    values[v] = {k: [r[k] for r in loads] for _, k, _ in METRICS if k != 'first'}
    values[v]['first'] = [int(rows(run + '/client.log', 'cstat')[-1][2])
                          for run in sorted(glob.glob(f'{RUNS}/first-{v}-*'))]


def cell(vs, fmt):
    lo, hi = fmt.format(min(vs)), fmt.format(max(vs))
    return (lo if lo == hi else f"{lo.split(' ')[0]} – {hi}").replace(',', ' ')


def mean(vs):
    return sum(vs) / len(vs)


print(f'\n| | {REF} | HEAD | |\n|---|---|---|---|')
for name, k, fmt in METRICS:
    ref, head = values['ref'][k], values['head'][k]
    diff = f'{(mean(head) / mean(ref) - 1) * 100:+.0f} %'.replace('-', '−')
    print(f'| {name} | {cell(ref, fmt)} | {cell(head, fmt)} | {diff} |')
