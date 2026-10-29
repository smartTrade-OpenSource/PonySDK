// @ts-check
// Canvas or SVG for PChart (docs/decisions/0026-chart.md): the same chart drawn both ways, 10 series of 600 points
// sliding at 10 Hz (a point per series every 100 ms, drawn when it arrives) and at 60 fps (a point per frame), on
// Chrome. Per run: the main thread's busy time per second from CDP's Performance metrics (task, script, layout,
// style recalculation), the draw function's own time, and the frames the page got.
//   node sample/tests/bench/chart-render.js [seconds]      (DPR=1 for a 1x screen; 2 by default)
const { chromium } = require('playwright');

const PAGE = `<!doctype html><meta charset=utf-8><body style="margin:0"><div id=host style="width:900px;height:360px"></div><script>
const SERIES = 10, POINTS = 600, W = 900, H = 360, L = 56, B = 24, T = 8, R = 8;
const COLORS = ['#4e79a7','#f28e2b','#e15759','#76b7b2','#59a14f','#edc948','#b07aa1','#ff9da7','#9c755f','#bab0ac'];
const data = Array.from({ length: SERIES }, (_, s) => Float64Array.from({ length: POINTS }, (_, i) => 100 + s * 3 + Math.sin(i / 20 + s)));
const times = Float64Array.from({ length: POINTS }, (_, i) => i * 100);
let tick = 0;
function step(n) { // n points slide in
  for (let k = 0; k < n; k++) {
    tick++;
    for (let s = 0; s < SERIES; s++) { data[s].copyWithin(0, 1); data[s][POINTS - 1] = 100 + s * 3 + Math.sin((POINTS + tick) / 20 + s) + Math.random() * 0.3; }
    times.copyWithin(0, 1); times[POINTS - 1] = times[POINTS - 2] + 100;
  }
}
function scale() {
  let lo = Infinity, hi = -Infinity;
  for (const d of data) for (let i = 0; i < POINTS; i++) { if (d[i] < lo) lo = d[i]; if (d[i] > hi) hi = d[i]; }
  const pad = (hi - lo) * 0.05; lo -= pad; hi += pad;
  return { lo, hi, x: t => L + (t - times[0]) / (times[POINTS - 1] - times[0]) * (W - L - R), y: v => H - B - (v - lo) / (hi - lo) * (H - B - T) };
}
const labelsY = sc => Array.from({ length: 6 }, (_, i) => { const v = sc.lo + (sc.hi - sc.lo) * i / 5; return [v, v.toFixed(2)]; });
const labelsX = () => Array.from({ length: 8 }, (_, i) => { const t = times[0] + (times[POINTS - 1] - times[0]) * i / 7; return [t, new Date(t).toISOString().slice(11, 19)]; });

const impl = {
  canvas() {
    const dpr = devicePixelRatio, c = document.createElement('canvas');
    c.width = W * dpr; c.height = H * dpr; c.style.width = W + 'px'; c.style.height = H + 'px';
    host.replaceChildren(c);
    const g = c.getContext('2d'); g.scale(dpr, dpr); g.font = '11px sans-serif';
    return () => {
      const sc = scale();
      g.clearRect(0, 0, W, H);
      g.strokeStyle = 'rgba(128,128,128,.3)'; g.fillStyle = '#666'; g.lineWidth = 1;
      g.beginPath();
      for (const [v, text] of labelsY(sc)) { const y = Math.round(sc.y(v)) + .5; g.moveTo(L, y); g.lineTo(W - R, y); g.fillText(text, 4, y + 4); }
      g.stroke();
      for (const [t, text] of labelsX()) g.fillText(text, sc.x(t) - 20, H - 8);
      g.lineWidth = 1.5; g.lineJoin = 'round';
      for (let s = 0; s < SERIES; s++) {
        g.strokeStyle = COLORS[s]; g.beginPath();
        const d = data[s]; g.moveTo(sc.x(times[0]), sc.y(d[0]));
        for (let i = 1; i < POINTS; i++) g.lineTo(sc.x(times[i]), sc.y(d[i]));
        g.stroke();
      }
    };
  },
  svg() {
    const NS = 'http://www.w3.org/2000/svg', svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('width', W); svg.setAttribute('height', H); host.replaceChildren(svg);
    const mk = (n, a, parent = svg) => { const e = document.createElementNS(NS, n); for (const k in a) e.setAttribute(k, a[k]); parent.append(e); return e; };
    const grid = Array.from({ length: 6 }, () => ({ line: mk('line', { stroke: 'rgba(128,128,128,.3)' }), text: mk('text', { x: 4, fill: '#666', 'font-size': 11 }) }));
    const xt = Array.from({ length: 8 }, () => mk('text', { y: H - 8, fill: '#666', 'font-size': 11 }));
    const paths = COLORS.map(c => mk('path', { fill: 'none', stroke: c, 'stroke-width': 1.5, 'stroke-linejoin': 'round' }));
    return () => {
      const sc = scale();
      labelsY(sc).forEach(([v, text], i) => { const y = Math.round(sc.y(v)) + .5; const g = grid[i]; g.line.setAttribute('x1', L); g.line.setAttribute('x2', W - R); g.line.setAttribute('y1', y); g.line.setAttribute('y2', y); g.text.setAttribute('y', y + 4); g.text.textContent = text; });
      labelsX().forEach(([t, text], i) => { xt[i].setAttribute('x', sc.x(t) - 20); xt[i].textContent = text; });
      for (let s = 0; s < SERIES; s++) {
        const d = data[s]; let p = 'M' + sc.x(times[0]).toFixed(1) + ' ' + sc.y(d[0]).toFixed(1);
        for (let i = 1; i < POINTS; i++) p += 'L' + sc.x(times[i]).toFixed(1) + ' ' + sc.y(d[i]).toFixed(1);
        paths[s].setAttribute('d', p);
      }
    };
  },
};

window.run = (name, hz, ms) => new Promise(done => {
  const draw = impl[name](); step(0); draw();
  const drawTimes = [], frames = [];
  let last = performance.now(), dirty = false;
  const end = last + ms;
  const interval = hz < 60 ? setInterval(() => { step(1); dirty = true; }, 1000 / hz) : 0;
  (function frame(now) {
    frames.push(now - last); last = now;
    if (hz >= 60) { step(1); dirty = true; }
    if (dirty) { const a = performance.now(); draw(); drawTimes.push(performance.now() - a); dirty = false; }
    if (now < end) requestAnimationFrame(frame);
    else { clearInterval(interval); done({ drawTimes, frames: frames.slice(5) }); }
  })(performance.now());
});
</script>`;

const pct = (a, p) => { const s = [...a].sort((x, y) => x - y); return s[Math.min(s.length - 1, Math.floor(s.length * p))]; };
const mean = a => a.reduce((x, y) => x + y, 0) / a.length;

(async () => {
  const seconds = Number(process.argv[2] || 8);
  const dpr = Number(process.env.DPR || 2);
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  console.log(`Chrome ${browser.version()}, 10 series x 600 points, 900x360, ${seconds}s per run, device pixel ratio ${dpr}`);
  console.log('impl    rate   main thread ms/s (task / script / layout / style)   draw ms mean / p95   frame ms p95   fps');
  for (const hz of [10, 60]) {
    for (const name of ['canvas', 'svg']) {
      const context = await browser.newContext({ viewport: { width: 960, height: 400 }, deviceScaleFactor: dpr });
      const page = await context.newPage();
      await page.setContent(PAGE);
      const cdp = await context.newCDPSession(page);
      await cdp.send('Performance.enable');
      await page.evaluate(([n]) => window.run(n, 60, 500), [name]); // warm up
      const metrics = async () => Object.fromEntries((await cdp.send('Performance.getMetrics')).metrics.map(m => [m.name, m.value]));
      const m0 = await metrics();
      const result = await page.evaluate(([n, h, ms]) => window.run(n, h, ms), [name, hz, seconds * 1000]);
      const m1 = await metrics();
      const per = key => ((m1[key] - m0[key]) * 1000 / seconds).toFixed(0);
      console.log(`${name.padEnd(7)} ${String(hz).padStart(3)} Hz   ${per('TaskDuration').padStart(5)} / ${per('ScriptDuration').padStart(5)} / ${per('LayoutDuration').padStart(5)} / ${per('RecalcStyleDuration').padStart(5)}`
        + `        ${mean(result.drawTimes).toFixed(2).padStart(6)} / ${pct(result.drawTimes, .95).toFixed(2).padStart(6)}`
        + `        ${pct(result.frames, .95).toFixed(1).padStart(6)}   ${(1000 / mean(result.frames)).toFixed(0)}`);
      await context.close();
    }
  }
  await browser.close();
})();
