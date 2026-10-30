// @ts-check
// What the Perspective page costs the browser (docs/decisions/0029-perspective.md), on Chrome: the bytes of the library the
// page loads (decoded, and as the server sends them: gzip where Jetty compresses), and the main thread's busy time per
// second at 100, 1,000 and 5,000 updates a second of the feed, from CDP's Performance metrics (task, script, layout, style
// recalculation), against the same page's idle time and the Inputs page's. The engine runs in a worker and the charts draw
// in another: the main thread is what the page's own scripts, the decoding of the batches and the grid's DOM cost.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ node sample/tests/bench/perspective.js [seconds]
const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
const SECONDS = Number(process.argv[2] || 8);

const kb = bytes => (bytes / 1024).toFixed(1) + ' KB';

/** The metrics of the main thread, in seconds of busy time. */
async function metrics(cdp) {
  const { metrics } = await cdp.send('Performance.getMetrics');
  const byName = Object.fromEntries(metrics.map(m => [m.name, m.value]));
  return {
    task: byName.TaskDuration, script: byName.ScriptDuration, layout: byName.LayoutDuration, style: byName.RecalcStyleDuration,
  };
}

/** Busy time per second of a window: milliseconds of the task, of the script, of layout and style. */
async function busy(cdp, ms) {
  const before = await metrics(cdp);
  await new Promise(resolve => setTimeout(resolve, ms));
  const after = await metrics(cdp);
  const seconds = ms / 1000;
  const per = key => ((after[key] - before[key]) * 1000 / seconds).toFixed(0).padStart(4);
  return `${per('task')} ms/s   script ${per('script')}   layout ${per('layout')}   style ${per('style')}`;
}

(async () => {
  const browser = await chromium.launch({ channel: process.env.PW_CHANNEL || 'chrome' });
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();
  const cdp = await context.newCDPSession(page);
  await cdp.send('Network.enable');
  await cdp.send('Performance.enable');
  await cdp.send('Network.setCacheDisabled', { cacheDisabled: true });

  /** @type {Map<string, { url: string, type: string, encoding: string, decoded: number, transferred: number }>} */
  const files = new Map();
  cdp.on('Network.responseReceived', e => {
    files.set(e.requestId, { url: e.response.url, type: e.response.mimeType, encoding: e.response.headers['content-encoding'] || '', decoded: 0, transferred: 0 });
  });
  cdp.on('Network.dataReceived', e => { const f = files.get(e.requestId); if (f) f.decoded += e.dataLength; });
  cdp.on('Network.loadingFinished', e => { const f = files.get(e.requestId); if (f) f.transferred = e.encodedDataLength; });

  // The Inputs page first: what a page without a viewer costs, and loads of the library (none)
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Click me' }).waitFor({ timeout: 15000 });
  await page.waitForTimeout(3000);
  console.log('Inputs page, idle:                  ' + await busy(cdp, SECONDS * 1000));
  const before = [...files.values()].filter(f => f.url.includes('/script/perspective/')).length;

  files.clear();
  const opened = Date.now();
  await page.locator('.pony-tab', { hasText: 'Perspective' }).click();
  await page.locator('perspective-viewer regular-table tbody tr').first().waitFor({ timeout: 30000 });
  const firstRows = Date.now() - opened;
  await page.waitForTimeout(1000);

  const library = [...files.values()].filter(f => f.url.includes('/script/perspective/'));
  // The engine's worker file is requested by the worker, whose network the page's session does not see: fetched here
  const workerUrl = BASE_URL + 'script/perspective/client/dist/cdn/perspective-server.worker.js';
  const worker = await page.evaluate(async url => {
    const body = await (await fetch(url, { cache: 'reload' })).arrayBuffer();
    const entry = /** @type {PerformanceResourceTiming} */ (performance.getEntriesByName(url).pop());
    return { decoded: body.byteLength, transferred: entry.transferSize };
  }, workerUrl);
  library.push({ url: workerUrl, type: 'text/javascript', encoding: 'gzip', decoded: worker.decoded, transferred: worker.transferred });
  console.log(`\nFrom the click on the tab to the first rows of the grid, nothing cached, over localhost: ${firstRows} ms`);
  console.log(`The library, by the Perspective page (${before} files before it): ${library.length} files`);
  const row = (name, list) => {
    const decoded = list.reduce((n, f) => n + f.decoded, 0);
    const transferred = list.reduce((n, f) => n + f.transferred, 0);
    console.log(`  ${name.padEnd(34)} ${kb(decoded).padStart(10)} decoded ${kb(transferred).padStart(10)} on the wire`);
  };
  for (const f of library.sort((a, b) => b.decoded - a.decoded)) {
    console.log(`  ${f.url.split('/script/perspective/')[1].padEnd(64)} ${kb(f.decoded).padStart(10)} ${kb(f.transferred).padStart(10)} ${f.encoding}`);
  }
  console.log('');
  row('JavaScript', library.filter(f => /javascript/.test(f.type)));
  row('WebAssembly', library.filter(f => /wasm/.test(f.type)));
  row('CSS', library.filter(f => /css/.test(f.type)));
  row('All', library);
  row('All but the charts', library.filter(f => !f.url.includes('viewer-charts')));

  console.log('\nMain thread, the page at rest and the feed at three rates (the table of 5,000 rows, the grid):');
  console.log('  100 updates/s   ' + await (async () => { await page.getByRole('button', { name: '100 updates/s' }).click(); await page.waitForTimeout(2000); return busy(cdp, SECONDS * 1000); })());
  console.log('  1,000 updates/s ' + await (async () => { await page.getByRole('button', { name: '1,000 updates/s' }).click(); await page.waitForTimeout(2000); return busy(cdp, SECONDS * 1000); })());
  console.log('  5,000 updates/s ' + await (async () => { await page.getByRole('button', { name: '5,000 updates/s' }).click(); await page.waitForTimeout(2000); return busy(cdp, SECONDS * 1000); })());
  console.log('  1,000 updates/s, pivot ' + await (async () => {
    await page.getByRole('button', { name: '1,000 updates/s' }).click();
    await page.getByRole('button', { name: 'Pivot by sector and currency' }).click();
    await page.waitForTimeout(2000);
    return busy(cdp, SECONDS * 1000);
  })());
  console.log('  1,000 updates/s, chart ' + await (async () => {
    await page.getByRole('button', { name: 'Chart: P&L by sector' }).click();
    await page.waitForTimeout(2500);
    return busy(cdp, SECONDS * 1000);
  })());
  const heap = await page.evaluate(() => /** @type {any} */ (performance).memory && Math.round(/** @type {any} */ (performance).memory.usedJSHeapSize / 1048576));
  console.log(`\nJS heap of the page: ${heap} MB (the engine's WebAssembly memory is not in it)`);
  await browser.close();
})();
