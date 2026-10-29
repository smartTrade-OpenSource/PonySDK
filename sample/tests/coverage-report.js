// @ts-check
// Maps the V8 coverage written by coverage.js onto the terminal's TypeScript sources through esbuild's source maps,
// and prints the lines and the branches covered per source file. No dependency: the source maps are decoded here.
//   node tests/coverage-report.js <coverage dir> [<terminal dist dir>]
//   COVERAGE_MIN=80 fails below 80% of lines, COVERAGE_BRANCH_MIN=80 below 80% of branches
// The terminal's Node unit tests count too when their V8 coverage is in <coverage dir>/node:
//   (cd ponysdk/terminal && NODE_V8_COVERAGE=<coverage dir>/node npm test)
// Node runs the sources with their types blanked out, so its offsets are the sources' own: a line of code, as the
// source maps define it, ran when the code at one of its mapped positions ran.
// A branch is a block of V8's block coverage: every range of a function but its first, which is the function itself
// (an if or else arm, a ternary arm, the right side of &&, || or ??, a loop body, a catch, a switch case, the code
// after a return or a throw). A block stands at the first mapped source position inside it, the same in the bundles
// and in the sources, and ran when the code at that position ran in a browser or in Node. V8 leaves out a block that
// ran as often as the code holding it, and the blocks of a function no run called (its lines are missed), so the
// blocks are those reported by at least one run; a block with no mapped position (an empty catch) does not count.
// Node's --experimental-test-coverage reports the same blocks, and counts each function as a branch too.
//   COVERAGE_DETAIL=<substring> also prints the missed lines and blocks (line:column) of the source files whose path
//   contains it.
const { readdirSync, readFileSync, existsSync } = require('node:fs');
const { dirname, join, resolve, relative } = require('node:path');

const [dir, dist = resolve(__dirname, '../../ponysdk/terminal/build/dist')] = process.argv.slice(2);
if (!dir) throw new Error('usage: node tests/coverage-report.js <coverage dir> [<terminal dist dir>]');
const terminalRoot = resolve(dist, '../..');

const B64 = new Map([...'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'].map((c, i) => [c, i]));

/** The segments of a source map: [generated line, generated column, source index, original line, original column]. */
function segments(mappings) {
  const out = [];
  let source = 0, originalLine = 0, originalColumn = 0;
  mappings.split(';').forEach((line, generatedLine) => {
    let column = 0;
    for (const segment of line.split(',')) {
      if (!segment) continue;
      const values = [];
      let value = 0, shift = 0;
      for (const c of segment) {
        const digit = /** @type {number} */ (B64.get(c));
        value += (digit & 31) << shift;
        if (digit & 32) shift += 5;
        else { values.push(value & 1 ? -(value >>> 1) : value >>> 1); value = 0; shift = 0; }
      }
      column += values[0];
      if (values.length < 4) continue;
      source += values[1];
      originalLine += values[2];
      originalColumn += values[3];
      out.push([generatedLine, column, source, originalLine, originalColumn]);
    }
  });
  return out;
}

function lineStartsOf(text) {
  const starts = [0];
  for (let i = 0; i < text.length; i++) if (text.charCodeAt(i) === 10) starts.push(i + 1);
  return starts;
}

/** The execution count of each character of a script: inner ranges override the ranges holding them. */
function counts(length, functions) {
  const count = new Int32Array(length);
  const ranges = functions.flatMap(f => f.ranges)
    .sort((a, b) => (b.endOffset - b.startOffset) - (a.endOffset - a.startOffset));
  for (const r of ranges) count.fill(r.count, r.startOffset, r.endOffset);
  return count;
}

/** The ranges of the blocks of a script's functions. */
const blockRanges = functions => functions.flatMap(f => f.ranges.slice(1));

/** The index of the first of the sorted numbers at or after value. */
function firstAtOrAfter(sorted, value) {
  let low = 0, high = sorted.length;
  while (low < high) {
    const middle = (low + high) >>> 1;
    if (sorted[middle] < value) low = middle + 1; else high = middle;
  }
  return low;
}

/**
 * Per source file: the lines that have code, the lines that ran, the mapped positions (offsets in the source) of each
 * line, the sorted mapped positions, the blocks (by position) and the blocks that ran.
 */
const files = new Map();
/** Per built script: its text, the generated offset of each mapped segment, sorted, and its position in the sources. */
const scripts = new Map();
const builtScripts = readdirSync(dist, { recursive: true }).map(String).filter(f => f.endsWith('.js'));
for (const script of builtScripts) {
  const mapFile = join(dist, `${script}.map`);
  if (!existsSync(mapFile)) throw new Error(`no source map for ${script} in ${dist}`);
  const text = readFileSync(join(dist, script), 'utf8');
  const map = JSON.parse(readFileSync(mapFile, 'utf8'));
  const lineStarts = lineStartsOf(text);
  const sources = map.sources.map(s => relative(terminalRoot, resolve(dirname(mapFile), s)));
  const mapped = [];
  for (const [line, column, source, originalLine, originalColumn] of segments(map.mappings)) {
    const file = sources[source];
    if (!file.startsWith('src/')) continue;
    let f = files.get(file);
    if (!f) {
      const source = readFileSync(join(terminalRoot, file), 'utf8');
      files.set(file, f = { length: source.length, lineStarts: lineStartsOf(source), code: new Set(), run: new Set(), positions: new Map(), sorted: [],
        blocks: new Set(), covered: new Set() });
    }
    const position = f.lineStarts[originalLine] + originalColumn;
    f.code.add(originalLine);
    let positions = f.positions.get(originalLine);
    if (!positions) f.positions.set(originalLine, positions = new Set());
    positions.add(position);
    mapped.push({ offset: lineStarts[line] + column, file, line: originalLine, position });
  }
  mapped.sort((a, b) => a.offset - b.offset);
  scripts.set(script, { text, mapped, offsets: mapped.map(m => m.offset) });
}
for (const f of files.values()) f.sorted = [...new Set([...f.positions.values()].flatMap(p => [...p]))].sort((a, b) => a - b);
/** Every browser and Node run: [built script or source file, functions], read once. */
const browserRuns = [];
for (const name of readdirSync(dir).filter(f => f.endsWith('.json'))) {
  for (const entry of JSON.parse(readFileSync(join(dir, name), 'utf8'))) {
    const script = entry.url.match(/\/((?:chunks\/)?[\w-]+\.js)(\?|$)/)[1];
    const s = scripts.get(script);
    if (!s) throw new Error(`no source map for ${script} in ${dist}`);
    // The coverage holds no source: the script the page ran is the built one when its length is (V8 may leave out
    // the top-level function of a script, never a range beyond its end)
    if (entry.functions.some(f => f.ranges[0].startOffset === 0 ? f.ranges[0].endOffset !== s.text.length
      : f.ranges[0].endOffset > s.text.length)) {
      throw new Error(`${entry.url} is not ${join(dist, script)}: rebuild the terminal and restart the server`);
    }
    browserRuns.push([script, entry.functions]);
  }
}
const nodeRuns = [];
const nodeDir = join(dir, 'node');
if (existsSync(nodeDir)) {
  const srcUrl = `file://${join(terminalRoot, 'src')}/`;
  for (const name of readdirSync(nodeDir).filter(f => f.endsWith('.json'))) {
    for (const entry of JSON.parse(readFileSync(join(nodeDir, name), 'utf8')).result) {
      if (!entry.url.startsWith(srcUrl)) continue;
      const file = `src/${decodeURIComponent(entry.url.slice(srcUrl.length))}`;
      if (files.has(file)) nodeRuns.push([file, entry.functions]);
    }
  }
}

// The blocks: each at the first mapped source position inside it
for (const [script, functions] of browserRuns) {
  const { mapped, offsets } = scripts.get(script);
  for (const r of blockRanges(functions)) {
    const m = mapped[firstAtOrAfter(offsets, r.startOffset)];
    if (m && m.offset < r.endOffset) files.get(m.file).blocks.add(m.position);
  }
}
for (const [file, functions] of nodeRuns) {
  const f = files.get(file);
  for (const r of blockRanges(functions)) {
    const position = f.sorted[firstAtOrAfter(f.sorted, r.startOffset)];
    if (position < r.endOffset) f.blocks.add(position);
  }
}

// What ran: a line when one of its positions ran, a block when its position ran, in a browser or in Node
for (const [script, functions] of browserRuns) {
  const { text, mapped } = scripts.get(script);
  const count = counts(text.length, functions);
  for (const { offset, file, line, position } of mapped) {
    if (count[offset] <= 0) continue;
    const f = files.get(file);
    f.run.add(line);
    if (f.blocks.has(position)) f.covered.add(position);
  }
}
for (const [file, functions] of nodeRuns) {
  const f = files.get(file);
  const count = counts(f.length, functions);
  for (const [line, positions] of f.positions) {
    for (const position of positions) if (count[position] > 0) f.run.add(line);
  }
  for (const position of f.blocks) if (count[position] > 0) f.covered.add(position);
}

const detail = process.env.COVERAGE_DETAIL;
const percent = (part, whole) => whole === 0 ? 100 : 100 * part / whole;
let code = 0, run = 0, blocks = 0, covered = 0;
const rows = [...files].map(([file, f]) => ({ file, f, lines: percent(f.run.size, f.code.size), branches: percent(f.covered.size, f.blocks.size) }))
  .sort((a, b) => a.lines - b.lines || a.branches - b.branches);
console.log('  lines       missed  branches    missed  source file');
for (const { file, f, lines, branches } of rows) {
  code += f.code.size; run += f.run.size; blocks += f.blocks.size; covered += f.covered.size;
  console.log(`${lines.toFixed(1).padStart(6)}%  ${String(f.code.size - f.run.size).padStart(4)}    ${branches.toFixed(1).padStart(6)}%  ${String(f.blocks.size - f.covered.size).padStart(4)}    ${file}`);
  if (detail && file.includes(detail)) {
    const missedLines = [...f.code].filter(l => !f.run.has(l)).sort((a, b) => a - b).map(l => l + 1);
    const missedBlocks = [...f.blocks].filter(p => !f.covered.has(p)).sort((a, b) => a - b).map(p => {
      const line = firstAtOrAfter(f.lineStarts, p + 1) - 1;
      return `${line + 1}:${p - f.lineStarts[line] + 1}`;
    });
    if (missedLines.length) console.log(`          lines ${missedLines.join(' ')}`);
    if (missedBlocks.length) console.log(`          blocks ${missedBlocks.join(' ')}`);
  }
}
const all = [...new Set(readdirSync(join(terminalRoot, 'src'), { recursive: true }).map(String))]
  .filter(f => f.endsWith('.ts')).map(f => `src/${f}`);
const never = all.filter(f => !files.has(f));
if (never.length) console.log(`not in the bundles: ${never.join(', ')}`);
const lineTotal = percent(run, code), branchTotal = percent(covered, blocks);
console.log(`lines ${lineTotal.toFixed(1)}% (${run}/${code}), branches ${branchTotal.toFixed(1)}% (${covered}/${blocks}) over ${files.size} of ${all.length} source files`);
let failed = false;
for (const [name, total] of [['COVERAGE_MIN', lineTotal], ['COVERAGE_BRANCH_MIN', branchTotal]]) {
  const min = Number(process.env[name] ?? 0);
  if (total < min) { console.error(`below ${name}=${min}%`); failed = true; }
}
if (failed) process.exit(1);
