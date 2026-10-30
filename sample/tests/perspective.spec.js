// @ts-check
// Perspective (docs/decisions/0029-perspective.md): the showcase's Perspective page, a blotter of 5,000 rows that a feed
// thread updates 1,000 times a second, in a <perspective-viewer> (WebAssembly) served by the application itself.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test perspective
// The viewer's elements are in shadow roots: Playwright's CSS locators go through them.
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/** The page's own text lines: the configuration the user changed, the row clicked, the row selected. */
const statuses = page => page.locator('.fdc3-received');

/** Runs in the page: how many rows the viewer's table holds; 0 while there is no viewer or no table yet. */
const tableSize = page => page.evaluate(async () => {
  const viewer = document.querySelector('perspective-viewer');
  if (!viewer || !viewer.getTable) return 0;
  try {
    return await (await viewer.getTable()).size();
  } catch (e) {
    return 0;
  }
});

/** The `last` column of the first rows, as the table has it now. */
const lastPrices = page => page.evaluate(async () => {
  const table = await document.querySelector('perspective-viewer').getTable();
  const view = await table.view({ columns: ['last'] });
  const columns = await view.to_columns({ end_row: 300 });
  await view.delete();
  return columns.last;
});

/** The viewer's configuration, as it holds it. */
const saved = page => page.evaluate(() => document.querySelector('perspective-viewer').save());

/** How many pixels of the viewer's canvases are not transparent: a chart drawn in a worker has some. */
const painted = page => page.locator('perspective-viewer canvas').evaluateAll(canvases => canvases.map(canvas => {
  const copy = document.createElement('canvas');
  copy.width = canvas.width;
  copy.height = canvas.height;
  const context = copy.getContext('2d');
  context.drawImage(canvas, 0, 0);
  const data = context.getImageData(0, 0, copy.width, copy.height).data;
  let n = 0;
  for (let i = 3; i < data.length; i += 4) if (data[i] !== 0) n++;
  return n;
}).reduce((a, b) => a + b, 0));

/** Records the page's CSP violations from its first script on (the workers' own are not delivered to the page). */
async function recordViolations(page) {
  await page.addInitScript(() => {
    window.__violations = [];
    document.addEventListener('securitypolicyviolation', e => window.__violations.push(e.violatedDirective + ' ' + e.blockedURI));
  });
}

/** Collects the errors of the page, the library's included. */
function collectErrors(page) {
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error' && !/favicon/.test(m.location().url)) errors.push('console: ' + m.text()); });
  return errors;
}

async function openPerspective(page) {
  await page.goto(BASE_URL + '#perspective', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('perspective-viewer')).toBeVisible({ timeout: 30000 });
  await expect.poll(() => tableSize(page), { timeout: 30000 }).toBe(5000);
  await expect(page.locator('perspective-viewer regular-table tbody tr').first()).toBeVisible({ timeout: 10000 });
}

test('the viewer renders the blotter: 5,000 rows, its first rows in the grid', async ({ page }) => {
  const errors = collectErrors(page);
  await openPerspective(page);

  const grid = page.locator('perspective-viewer regular-table');
  await expect(grid.locator('thead th')).toContainText(['id', 'symbol', 'sector', 'currency', 'bid', 'ask', 'last', 'volume', 'pnl', 'updated', 'tradeDate']);
  await expect(grid.locator('tbody tr').first().locator('td').nth(1)).toHaveText('AAAA');
  await expect(grid.locator('tbody tr').nth(1).locator('td').nth(1)).toHaveText('AAAB');
  expect((await saved(page)).plugin).toBe('Datagrid');
  expect(errors).toEqual([]);
});

/** The page's counters, read in one go: updates offered a second, rows sent, batches, KB on the wire, bytes per row. */
async function counters(page) {
  const texts = await page.locator('.perf-value').allInnerTexts();
  const [offered, rows, batches, kilobytes, perRow] = texts.map(t => Number(t.replace(/,/g, '')));
  return { offered, rows, batches, kilobytes, perRow };
}

test('the feed\'s updates apply: the table moves while the server sends one batch per flush', async ({ page }) => {
  await openPerspective(page);

  const first = await lastPrices(page);
  await page.waitForTimeout(1000);
  const second = await lastPrices(page);
  const changed = second.filter((price, i) => price !== first[i]).length;
  expect(changed, 'of the 300 first rows, those a second of the feed moved').toBeGreaterThan(10);

  // 1,000 updates a second offered, a batch per flush at most every 100 ms: about 10 a second
  await expect.poll(async () => (await counters(page)).offered, { timeout: 10000 }).toBeGreaterThan(500);
  await expect.poll(async () => (await counters(page)).batches, { timeout: 10000 }).toBeGreaterThan(5);
  const rate = await counters(page);
  expect(rate.batches).toBeLessThanOrEqual(12);
  expect(rate.perRow, 'bytes on the wire per row sent, framing included').toBeLessThan(60);
});

test('a faster feed is conflated: fewer rows are sent than are offered, in as many batches', async ({ page }) => {
  await openPerspective(page);
  await page.getByRole('button', { name: '100 updates/s' }).click();
  await expect.poll(async () => (await counters(page)).offered, { timeout: 15000 }).toBeLessThan(250);

  await page.getByRole('button', { name: '5,000 updates/s' }).click();
  await expect.poll(async () => (await counters(page)).offered, { timeout: 15000 }).toBeGreaterThan(2500);
  await expect.poll(async () => { const c = await counters(page); return c.offered - c.rows; }, { timeout: 15000 })
    .toBeGreaterThan(50); // the updates of a row that a flush merged
  expect((await counters(page)).batches).toBeLessThanOrEqual(12);
});

test('a configuration set from Java is applied, and is not reported back as the user\'s', async ({ page }) => {
  await openPerspective(page);
  const initial = await statuses(page).first().innerText();

  await page.getByRole('button', { name: 'Pivot by sector and currency' }).click();
  await expect.poll(async () => (await saved(page)).group_by).toEqual(['sector', 'currency']);
  const pivot = await saved(page);
  expect(pivot.plugin).toBe('Datagrid');
  expect(pivot.columns).toEqual(['bid', 'volume', 'pnl']);
  expect(pivot.sort).toEqual([['pnl', 'desc']]);
  expect(pivot.aggregates).toMatchObject({ bid: 'avg', volume: 'sum', pnl: 'sum' });
  await expect(page.locator('perspective-viewer regular-table')).toContainText('TOTAL');

  await page.getByRole('button', { name: 'Chart: P&L by sector' }).click();
  await expect.poll(async () => (await saved(page)).plugin).toBe('Y Bar');
  await expect.poll(() => painted(page), { timeout: 15000 }).toBeGreaterThan(1000);

  await page.getByRole('button', { name: 'Blotter', exact: true }).click();
  await expect.poll(async () => (await saved(page)).plugin).toBe('Datagrid');
  expect((await saved(page)).group_by).toEqual([]);

  await page.waitForTimeout(500); // past the delay a change of the user's is reported after
  expect(await statuses(page).first().innerText(), 'what Java set did not come back').toBe(initial);
});

test('what the user changes in the viewer is reported to the server', async ({ page }) => {
  await openPerspective(page);

  await page.locator('perspective-viewer regular-table thead th', { hasText: 'bid' }).first().click(); // a sort
  await expect(statuses(page).first()).toContainText('Configuration: {"plugin":"Datagrid"', { timeout: 10000 });
  await expect(statuses(page).first()).toContainText('"sort":[["bid"');

  // a grouping, as the user's drag of a column would set it
  await page.evaluate(() => document.querySelector('perspective-viewer').restore({ group_by: ['currency'] }));
  await expect(statuses(page).first()).toContainText('"group_by":["currency"]', { timeout: 10000 });
});

test('a click on a row reaches the server and broadcasts its instrument on the FDC3 channel', async ({ context }) => {
  const page = await context.newPage();
  await page.goto(BASE_URL + '#perspective');
  await expect.poll(() => tableSize(page), { timeout: 30000 }).toBe(5000);
  const monitor = await context.newPage();
  await monitor.goto(BASE_URL + 'fdc3-monitor.html');
  await expect(monitor.locator('#channels [aria-pressed="true"]')).toHaveText('Channel 1');

  await page.locator('perspective-viewer regular-table tbody tr:nth-child(3) td:nth-child(2)').click();

  await expect(statuses(page).nth(1)).toHaveText('Clicked AAAC: broadcast on the FDC3 channel');
  await expect(monitor.locator('#contexts li').first()).toHaveText('fdc3.instrument AAAC from PonySDK Showcase');

  // the grid is in the mode where a click selects the row: the selection is the row, with the values of its columns
  await expect(statuses(page).nth(2)).toContainText('Selected: {"id":2,"symbol":"AAAC","sector":"Health"');
  await page.locator('perspective-viewer regular-table tbody tr:nth-child(3) td:nth-child(2)').click(); // again: deselected
  await expect(statuses(page).nth(2)).toHaveText('Nothing selected.');
});

test('the policy lets the page compile WebAssembly and start the charts\' worker, without eval, and nothing is blocked', async ({ page }) => {
  await recordViolations(page);
  const errors = collectErrors(page);
  const response = await page.goto(BASE_URL + '#perspective', { waitUntil: 'domcontentloaded' });
  const policy = response.headers()['content-security-policy'];
  expect(policy).toMatch(/^script-src 'self' 'sha256-[A-Za-z0-9+/=]+' 'wasm-unsafe-eval' blob:/);
  expect(policy).not.toMatch(/'unsafe-eval'|'unsafe-inline'/);

  await expect.poll(() => tableSize(page), { timeout: 30000 }).toBe(5000);
  await page.getByRole('button', { name: 'Chart: P&L by sector' }).click();
  await expect.poll(() => painted(page), { timeout: 15000 }).toBeGreaterThan(1000); // the worker ran, under the policy

  expect(await page.evaluate(() => window.__violations)).toEqual([]);
  expect(errors).toEqual([]);
});

test('the library is fetched from the application, by the Perspective page only', async ({ page }) => {
  const requests = [];
  page.on('request', r => requests.push(r.url()));
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Click me' })).toBeVisible({ timeout: 10000 });
  await page.waitForTimeout(3000);
  expect(requests.filter(url => url.includes('/script/perspective/')), 'no viewer on the page: no library').toEqual([]);

  await page.locator('.pony-tab', { hasText: 'Perspective' }).click();
  await expect.poll(() => tableSize(page), { timeout: 30000 }).toBe(5000);
  const library = requests.filter(url => url.includes('/script/perspective/'));
  expect(library.filter(url => url.endsWith('.wasm')).map(url => url.split('/').pop()).sort())
    .toEqual(['perspective-server.wasm', 'perspective-viewer.wasm']);
  expect(library.every(url => url.startsWith(new URL(BASE_URL).origin)), 'every file is the application\'s own: no CDN').toBe(true);
  expect(requests.filter(url => !url.startsWith(new URL(BASE_URL).origin) && /^https?:/.test(url))).toEqual([]);
  expect(library.some(url => url.includes('viewer-charts')), 'the charts are fetched when the viewer asks for them').toBe(true);
});

test('leaving the page frees the viewer, and coming back builds another', async ({ page }) => {
  const errors = collectErrors(page);
  await openPerspective(page);

  await page.locator('.pony-tab', { hasText: 'Inputs' }).click();
  await expect(page.locator('perspective-viewer')).toHaveCount(0);
  await page.locator('.pony-tab', { hasText: 'Perspective' }).click();
  await expect.poll(() => tableSize(page), { timeout: 30000 }).toBe(5000);
  await expect(page.locator('perspective-viewer regular-table tbody tr').first()).toBeVisible();
  expect(errors).toEqual([]);
});

// Left at any moment, as a visitor clicking through the tabs does: while the library loads, while the table is built,
// while the feed streams. CI saw "null pointer passed to rust" once, the viewer freed while it still worked
test('leaving the page at any moment, loading or streaming, frees it without an error', async ({ page }) => {
  test.setTimeout(120000);
  const errors = collectErrors(page);
  await page.goto(BASE_URL + '#streams', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.pony-tab', { hasText: 'Perspective' })).toBeVisible({ timeout: 15000 });
  for (const wait of [0, 20, 50, 100, 200, 400, 800, 1500, 3000, 3000]) {
    await page.locator('.pony-tab', { hasText: 'Perspective' }).click();
    await page.waitForTimeout(wait);
    await page.locator('.pony-tab', { hasText: 'Signals' }).click();
    await expect(page.locator('perspective-viewer')).toHaveCount(0);
  }
  await page.waitForTimeout(1000); // what the freed viewers would still do
  expect(errors).toEqual([]);
});
