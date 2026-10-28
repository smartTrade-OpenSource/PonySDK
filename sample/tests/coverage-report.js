// @ts-check
// Maps the V8 coverage written by coverage.js onto the terminal's TypeScript sources through esbuild's source maps,
// and prints the lines covered per source file. No dependency: the source maps are decoded here.
//   node tests/coverage-report.js <coverage dir> [<terminal dist dir>]     COVERAGE_MIN=80 fails below 80% of lines
const { readdirSync, readFileSync, existsSync } = require('node:fs');
const { dirname, join, resolve, relative } = require('node:path');

const [dir, dist = resolve(__dirname, '../../ponysdk/terminal/build/dist')] = process.argv.slice(2);
if (!dir) throw new Error('usage: node tests/coverage-report.js <coverage dir> [<terminal dist dir>]');
const terminalRoot = resolve(dist, '../..');

const B64 = new Map([...'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'].map((c, i) => [c, i]));

/** The segments of a source map: [generated line, generated column, source index, original line]. */
function segments(mappings) {
  const out = [];
  let source = 0, originalLine = 0;
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
      out.push([generatedLine, column, source, originalLine]);
    }
  });
  return out;
}

/** The execution count of each character of a script: inner ranges override the ranges holding them. */
function counts(entry) {
  const count = new Int32Array(entry.source.length);
  const ranges = entry.functions.flatMap(f => f.ranges)
    .sort((a, b) => (b.endOffset - b.startOffset) - (a.endOffset - a.startOffset));
  for (const r of ranges) count.fill(r.count, r.startOffset, r.endOffset);
  return count;
}

/** Per source file, the lines that have code and the lines that ran. */
const files = new Map();
const scripts = new Map();
for (const name of readdirSync(dir).filter(f => f.endsWith('.json'))) {
  for (const entry of JSON.parse(readFileSync(join(dir, name), 'utf8'))) {
    const script = entry.url.match(/\/((?:chunks\/)?[\w-]+\.js)(\?|$)/)[1];
    const mapFile = join(dist, `${script}.map`);
    if (!existsSync(mapFile)) throw new Error(`no source map for ${script} in ${dist}`);
    if (readFileSync(join(dist, script), 'utf8') !== entry.source) {
      throw new Error(`${entry.url} is not ${join(dist, script)}: rebuild the terminal and restart the server`);
    }
    if (!scripts.has(script)) {
      const map = JSON.parse(readFileSync(mapFile, 'utf8'));
      const lineStarts = [0];
      for (let i = 0; i < entry.source.length; i++) if (entry.source.charCodeAt(i) === 10) lineStarts.push(i + 1);
      const sources = map.sources.map(s => relative(terminalRoot, resolve(dirname(mapFile), s)));
      scripts.set(script, { lineStarts, sources, segments: segments(map.mappings) });
    }
    const { lineStarts, sources, segments: segs } = scripts.get(script);
    const count = counts(entry);
    for (const [line, column, source, originalLine] of segs) {
      const file = sources[source];
      if (!file.startsWith('src/')) continue;
      let f = files.get(file);
      if (!f) files.set(file, f = { code: new Set(), run: new Set() });
      f.code.add(originalLine);
      if (count[lineStarts[line] + column] > 0) f.run.add(originalLine);
    }
  }
}

let code = 0, run = 0;
const rows = [...files].map(([file, f]) => [file, f.run.size, f.code.size]).sort((a, b) => a[1] / a[2] - b[1] / b[2]);
for (const [file, r, c] of rows) {
  code += c; run += r;
  console.log(`${(100 * r / c).toFixed(1).padStart(5)}%  ${String(c - r).padStart(4)} missed  ${file}`);
}
const all = [...new Set(readdirSync(join(terminalRoot, 'src'), { recursive: true }).map(String))]
  .filter(f => f.endsWith('.ts')).map(f => `src/${f}`);
const never = all.filter(f => !files.has(f));
if (never.length) console.log(`never loaded: ${never.join(', ')}`);
const total = 100 * run / code;
console.log(`lines ${total.toFixed(1)}% (${run}/${code}) over ${files.size} of ${all.length} source files`);
const min = Number(process.env.COVERAGE_MIN ?? 0);
if (total < min) {
  console.error(`below COVERAGE_MIN=${min}%`);
  process.exit(1);
}
