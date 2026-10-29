// @ts-check
// Smoke test of the whole showcase: every tab renders and the terminal raises no error.
// Exercises the protocol reader on real traffic (short/long ASCII, UTF-8, dictionary, arrays, grids).
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test showcase-smoke
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

test('every showcase tab renders without client errors', async ({ page }) => {
  test.setTimeout(90000);
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => {
    if (m.type() !== 'error') return;
    if (/favicon\.ico$/.test(m.location().url || '')) return; // the sample ships no favicon
    errors.push('console: ' + m.text());
  });

  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
  const tabs = page.locator('.pony-tab');
  await expect(tabs.first()).toBeVisible({ timeout: 10000 });
  const count = await tabs.count();
  expect(count).toBeGreaterThanOrEqual(10);

  for (let i = 0; i < count; i++) {
    const tab = tabs.nth(i);
    const name = (await tab.innerText()).trim();
    await tab.click();
    await expect(tab, name).toHaveClass(/pony-tab-active/);
    const content = page.locator('.pony-content');
    await expect.poll(async () => (await content.innerText()).trim().length, { message: name, timeout: 10000 })
      .toBeGreaterThan(0);
    await page.waitForTimeout(400); // let schedulers / lazy content push a few frames
    expect(errors, 'after tab ' + name).toEqual([]);
  }

  // Strings longer than the fromCharCode threshold (TextDecoder path) reach the DOM intact
  await page.locator('.pony-tab', { hasText: 'Web Components' }).click();
  await expect(page.locator('my-card')).toContainText('Card body text set from server via property API.');
});

// The page has a file name: the socket is the application's, at the page's directory, not under the file
test('the bootstrap page at index.html connects', async ({ page }) => {
  const sockets = [];
  page.on('websocket', ws => sockets.push(new URL(ws.url()).pathname));
  await page.goto(BASE_URL + 'index.html', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.pony-tab').first()).toBeVisible({ timeout: 10000 });
  expect(sockets).toEqual([new URL('ws', BASE_URL).pathname]);
});

// The tree's invisible root item (PTree's TREE_ROOT) is added to the tree by the server: it must not show
test('a tree shows its items only, not its root item', async ({ page }) => {
  await page.goto(BASE_URL + '#data');
  const tree = page.locator('.data-tree');
  await expect(tree.getByText('Rates desk')).toBeVisible({ timeout: 10000 });
  expect(await tree.evaluate(t => [...t.children].map(c => c.tagName))).toEqual(['UL']);
  // a widget as an item's content sits on the item's row, as a text does
  const rows = tree.locator('.pony-TreeItem-row');
  const text = await rows.filter({ hasText: 'Bund 10Y' }).locator('.pony-TreeItem-content').boundingBox();
  const widget = await rows.filter({ hasText: 'UST 10Y' }).locator('.pony-TreeItem-content').boundingBox();
  expect(Math.abs(widget.x - text.x)).toBeLessThan(1);
  expect(widget.height).toBeLessThan(text.height * 1.5);
});

// An upload is complete once the server answered: the browser must not report it aborted afterwards
test('an upload completes: the server reports the file, the browser no failed request', async ({ page }) => {
  const failed = [];
  page.on('requestfailed', r => failed.push(`${new URL(r.url()).pathname} ${r.failure()?.errorText}`));
  await page.goto(BASE_URL);
  await page.locator('.pony-tab', { hasText: 'Inputs' }).click();
  const panel = page.locator('.pony-content');
  await panel.locator('input[type=file]').first().setInputFiles({ name: 'a.txt', mimeType: 'text/plain', buffer: Buffer.from('hello') });
  const upload = page.waitForResponse(r => r.url().includes('/sample/stream'));
  await panel.getByRole('button', { name: 'Submit' }).first().click();
  expect((await upload).status()).toBe(200);
  await expect(panel.locator('.pony-ex-echo', { hasText: 'server: received' })).toHaveText('server: received a.txt, 5 bytes');
  await page.waitForTimeout(300);
  expect(failed).toEqual([]);
});

// ADR-0013: a feed faster than the screen, conflated: far fewer values applied than offered, the prices moving
test('streams: the latest quote per instrument is applied, most quotes replaced before being shown', async ({ page }) => {
  await page.goto(BASE_URL + '#streams');
  const values = page.locator('.perf-grid .perf-value');
  const price = page.locator('.stream-price').first();
  await expect(values.nth(0)).not.toHaveText('—', { timeout: 10000 });
  const before = await price.innerText();
  await expect.poll(async () => {
    const [offered, applied] = (await values.allInnerTexts()).map(t => Number(t.replace(/,/g, '')));
    return offered > 5_000 && applied > 0 && applied < offered / 5;
  }, { timeout: 10000 }).toBe(true);
  await expect(price).not.toHaveText(before);
  expect(Number(await values.nth(2).innerText())).toBeGreaterThan(80);
});
