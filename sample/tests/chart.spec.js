// @ts-check
// PChart (docs/decisions/0026-chart.md): the showcase's Charts and Performance pages.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test chart
// The chart's elements are in a shadow root: Playwright's CSS locators go through it.
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/** How many pixels of a canvas are not transparent: a drawn chart has some; none before it is sized. */
const painted = canvas => canvas.evaluate(c => {
  if (!c.width || !c.height) return 0;
  const data = /** @type {HTMLCanvasElement} */ (c).getContext('2d').getImageData(0, 0, c.width, c.height).data;
  let n = 0;
  for (let i = 3; i < data.length; i += 4) if (data[i] !== 0) n++;
  return n;
});

test('the drawing code is fetched when a page creates its first chart, not before', async ({ page }) => {
  const chunks = [];
  page.on('request', r => { if (/\/chunks\/chart\.js(\?|$)/.test(r.url())) chunks.push(r.url()); });
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Click me' })).toBeVisible({ timeout: 10000 });
  await page.waitForTimeout(6000); // the terminal loads its other chunks when the browser is idle, one second in
  expect(chunks, 'no chart on the page: no chunk').toEqual([]);

  await page.locator('.pony-tab', { hasText: 'Charts' }).click();
  await expect(page.locator('.showcase-chart canvas').first()).toBeAttached({ timeout: 10000 });
  expect(chunks).toHaveLength(1);

  await page.locator('.pony-tab', { hasText: 'Performance' }).click();
  await expect(page.locator('.spark-card canvas').first()).toBeAttached({ timeout: 10000 });
  expect(chunks, 'loaded once').toHaveLength(1);
});

test('a live chart is drawn on a canvas, its points arrive and its label follows them at a low rate', async ({ page }) => {
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  page.on('console', m => { if (m.type() === 'error' && !/favicon/.test(m.location().url)) errors.push(m.text()); });
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  const chart = page.locator('.showcase-chart');
  const image = chart.locator('[role=img]');

  await expect(chart.locator('canvas')).toHaveCount(2); // the data, and the crosshair over it
  await expect(image).toHaveAttribute('aria-label', /^Rebased prices, start = 100: EURUSD \d+\.\d{3}, GBPUSD \d+\.\d{3}, AUDUSD \d+\.\d{3}, USDCAD \d+\.\d{3} at \d\d:\d\d:\d\d$/, { timeout: 10000 });
  await expect.poll(() => painted(chart.locator('canvas').first()), { timeout: 10000 }).toBeGreaterThan(500);
  await expect(chart.locator('.legend li')).toHaveText([/EURUSD \d+\.\d{3}/, /GBPUSD \d+\.\d{3}/, /AUDUSD \d+\.\d{3}/, /USDCAD \d+\.\d{3}/]);

  // points arrive 10 times a second; the label, read 20 times a second, changes about once a second
  const labels = new Set();
  for (let i = 0; i < 70; i++) {
    labels.add(await image.getAttribute('aria-label'));
    await page.waitForTimeout(50);
  }
  expect(labels.size, 'the label changed').toBeGreaterThan(1);
  expect(labels.size, 'at a low rate: ' + [...labels].join(' | ')).toBeLessThanOrEqual(6);
  expect(errors).toEqual([]);
});

test('the last rows are a table a screen reader can walk, newest first, hidden from the eye', async ({ page }) => {
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  const table = page.locator('.showcase-chart table');
  await expect(table.locator('caption')).toHaveText('Rebased prices, start = 100');
  await expect(table.locator('th')).toHaveText(['Time', 'EURUSD', 'GBPUSD', 'AUDUSD', 'USDCAD']);
  await expect.poll(() => table.locator('tr').count(), { timeout: 10000 }).toBeGreaterThan(3);
  expect(await table.locator('tr').count(), 'a header and at most 10 rows').toBeLessThanOrEqual(11);
  const times = await table.locator('tr > td:first-child').allTextContents();
  expect([...times].sort().reverse(), 'newest first').toEqual(times);
  const box = await page.locator('.showcase-chart .sr').boundingBox(); // what clips the table
  expect(box.width).toBeLessThanOrEqual(1);
  expect(box.height).toBeLessThanOrEqual(1);
});

test('the pointer shows the values at its time in a tooltip, and a crosshair', async ({ page }) => {
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  const chart = page.locator('.showcase-chart');
  await expect.poll(() => painted(chart.locator('canvas').first()), { timeout: 10000 }).toBeGreaterThan(500);
  const tip = chart.locator('.tip');
  await expect(tip).toBeHidden();

  const box = await chart.locator('.plot').boundingBox();
  await page.mouse.move(box.x + box.width - 40, box.y + box.height / 2);
  await expect(tip).toBeVisible();
  await expect(tip).toContainText(/\d\d:\d\d:\d\d/);
  await expect(tip).toContainText(/EURUSD\s+\d+\.\d{3}/);
  await expect(tip).toContainText(/USDCAD\s+\d+\.\d{3}/);
  expect(await painted(chart.locator('canvas').nth(1)), 'the crosshair and its points').toBeGreaterThan(20);
  const tipBox = await tip.boundingBox();
  expect(tipBox.x + tipBox.width, 'inside the chart').toBeLessThanOrEqual(box.x + box.width + 1);

  await page.mouse.move(box.x + 5, box.y + box.height / 2); // over the axis: no point
  await expect(tip).toBeHidden();
  await page.mouse.move(box.x + box.width - 40, box.y + box.height / 2);
  await expect(tip).toBeVisible();
  await page.mouse.move(box.x + box.width + 50, box.y - 50);
  await expect(tip).toBeHidden();
  expect(await painted(chart.locator('canvas').nth(1)), 'the crosshair is gone').toBe(0);
});

test('the tooltip is beside the crosshair, on the side where it fits', async ({ page }) => {
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  const card = page.locator('.chart-grid .spark-card').first(); // two minutes of history: points all along the axis
  await expect.poll(() => painted(card.locator('canvas').first()), { timeout: 10000 }).toBeGreaterThan(500);
  const plot = card.locator('.plot');
  const tip = card.locator('.tip');
  const crosshair = async fraction => { // the tooltip's box, and the pointer's x, in the page
    const size = await plot.boundingBox();
    await plot.hover({ position: { x: size.width * fraction, y: size.height / 2 } });
    await expect(tip).toBeVisible();
    const box = await plot.boundingBox(); // after the scroll the hover made
    return { tip: await tip.boundingBox(), pointer: box.x + box.width * fraction, plot: box };
  };
  const left = await crosshair(0.3);
  expect(left.tip.x, 'right of the crosshair').toBeGreaterThan(left.pointer);
  const right = await crosshair(0.9);
  expect(right.tip.x + right.tip.width, 'at the edge: left of the crosshair').toBeLessThanOrEqual(right.pointer + 1);
  expect(right.tip.x, 'and inside the chart').toBeGreaterThanOrEqual(right.plot.x);
});

test('the area and the bars are drawn, and the bars come in a series each', async ({ page }) => {
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  const cards = page.locator('.chart-grid .spark-card');
  await expect(cards).toHaveCount(2);
  for (const card of await cards.all()) {
    await expect.poll(() => painted(card.locator('canvas').first()), { timeout: 10000 }).toBeGreaterThan(500);
  }
  await expect(cards.nth(0).locator('[role=img]')).toHaveAttribute('aria-label', /^Desk P&L, EUR: P&L -?\d+ at \d\d:\d\d:\d\d$/);
  await expect(cards.nth(1).locator('[role=img]')).toHaveAttribute('aria-label', /^Executions per second: Buys \d+, Sells \d+ at /);
  await expect(cards.nth(1).locator('.legend li')).toHaveText([/Buys \d+/, /Sells \d+/]);
});

test('a feed of 10,000 ticks a second sends the same ten points a second', async ({ page }) => {
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: '10,000 ticks/s' }).click();
  const value = label => page.locator('.perf-card', { hasText: label }).locator('.perf-value');
  await expect.poll(async () => Number((await value('Offered by the feed').innerText()).replace(/,/g, '')), { timeout: 15000 })
    .toBeGreaterThan(3000);
  const points = Number(await value('Sent to the chart').innerText());
  expect(points, 'conflated to the chart\'s interval').toBeLessThanOrEqual(12);
  expect(points).toBeGreaterThanOrEqual(5);
  const perPoint = Number(await value('framing included').innerText());
  expect(perPoint, 'bytes on the wire per point of four series').toBeGreaterThan(0);
  expect(perPoint).toBeLessThan(100);
});

test('the colours come from --pony-chart-N, and the tooltip follows the colour scheme', async ({ page }) => {
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  const chart = page.locator('.showcase-chart');
  const swatch = chart.locator('.legend .swatch').first();
  await expect.poll(() => painted(chart.locator('canvas').first()), { timeout: 10000 }).toBeGreaterThan(500);
  const colour = () => swatch.evaluate(e => getComputedStyle(e).backgroundColor);
  expect(await colour()).toBe('rgb(78, 121, 167)'); // the fallback of --pony-chart-1

  await page.evaluate(() => document.documentElement.style.setProperty('--pony-chart-1', '#ff0000'));
  await expect.poll(colour).toBe('rgb(255, 0, 0)');

  const box = await chart.locator('.plot').boundingBox();
  const tipBackground = async scheme => {
    await page.emulateMedia({ colorScheme: scheme });
    await page.mouse.move(box.x + box.width - 60, box.y + box.height / 2 + (scheme === 'dark' ? 5 : 0));
    await expect(chart.locator('.tip')).toBeVisible();
    return chart.locator('.tip').evaluate(e => getComputedStyle(e).backgroundColor);
  };
  const light = await tipBackground('light');
  const dark = await tipBackground('dark');
  expect(dark, 'the tooltip is dark in dark mode').not.toBe(light);
});

test('the Performance page draws its 60-second charts with PChart', async ({ page }) => {
  await page.goto(BASE_URL + '#performance', { waitUntil: 'domcontentloaded' });
  const cards = page.locator('.spark-grid .spark-card');
  await expect(cards).toHaveCount(3);
  await expect(cards.nth(0).locator('[role=img]')).toHaveAttribute('aria-label', /^Live heap, last 60 seconds: Live heap, MB \d+ at /, { timeout: 10000 });
  await expect(cards.nth(1).locator('[role=img]')).toHaveAttribute('aria-label', /^Sent on the wire, last 60 seconds: /);
  await expect(cards.nth(2).locator('[role=img]')).toHaveAttribute('aria-label', /^Roundtrip latency, last 60 seconds: /);
  for (const card of await cards.all()) {
    await expect.poll(() => painted(card.locator('canvas').first()), { timeout: 10000 }).toBeGreaterThan(300);
  }
});

test('clear removes the points, and the values that follow are still right: both sides start their changes again', async ({ page }) => {
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  const chart = page.locator('.showcase-chart');
  const values = () => chart.locator('.legend .value').allTextContents().then(texts => texts.map(Number));
  await expect.poll(async () => (await values()).every(v => v > 90 && v < 110), { timeout: 10000 }).toBe(true);

  await page.getByRole('button', { name: 'Clear' }).click();
  await page.waitForTimeout(1500);
  const after = await values();
  expect(after, 'four series, each back at its level near 100, not a change added to what was before').toHaveLength(4);
  for (const v of after) {
    expect(v).toBeGreaterThan(90);
    expect(v).toBeLessThan(110);
  }
  await expect(chart.locator('[role=img]')).toHaveAttribute('aria-label', /^Rebased prices, start = 100: EURUSD (9|10)\d\.\d{3},/);
  const [hours, minutes] = (await chart.locator('[role=img]').getAttribute('aria-label')).match(/at (\d\d):(\d\d):\d\d$/).slice(1).map(Number);
  const now = await page.evaluate(() => [new Date().getHours(), new Date().getMinutes()]);
  expect((hours * 60 + minutes - now[0] * 60 - now[1] + 1440) % 1440, 'the time too is a change from zero again').toBeLessThanOrEqual(1);
});

test('a chart leaving the page frees its timers: nothing runs after the tab changes', async ({ page }) => {
  await page.goto(BASE_URL + '#charts', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.showcase-chart canvas').first()).toBeAttached({ timeout: 10000 });
  await page.locator('.pony-tab', { hasText: 'Inputs' }).click();
  await expect(page.locator('.showcase-chart')).toHaveCount(0);
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  await page.waitForTimeout(1500); // a frame or a timer of the destroyed chart would throw here
  expect(errors).toEqual([]);
});
