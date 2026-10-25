// @ts-check
// Smoke test of the whole showcase: every tab renders and the terminal raises no error.
// Exercises the protocol reader on real traffic (short/long ASCII, UTF-8, dictionary, arrays, grids).
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test showcase-smoke
const { test, expect } = require('@playwright/test');

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
