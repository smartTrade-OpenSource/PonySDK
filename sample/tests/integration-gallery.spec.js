// @ts-check
// The gallery of the ways to integrate PonySDK (sample/src/main/resources/portal/index.html, ADR-0008 and ADR-0009):
// each of its pages works. The portal of U4, U6 and U7 is embedding.spec.js's.
//   ./gradlew :sample:runShowcase    then    BASE_URL='http://[::1]:8081/sample/' npx playwright test integration-gallery
const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
/** The gallery: the portal server of ShowcaseMain, another origin of the same host. */
const GALLERY = (() => {
  const url = new URL(BASE_URL);
  return `${url.protocol}//${url.hostname}:8082/`;
})();

/** Page errors and console errors, the missing favicons aside. */
function trackErrors(page) {
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => {
    if (m.type() === 'error' && !/favicon\.ico$/.test(m.location().url || '')) errors.push('console: ' + m.text());
  });
  return errors;
}

test('the gallery shows the nine ways in, each with its link on this machine', async ({ page }) => {
  await page.goto(GALLERY);
  const cases = page.locator('.case');
  await expect(cases).toHaveCount(9);
  for (let i = 0; i < 9; i++) await expect(cases.nth(i).locator('.case-code')).toHaveText(`U${i + 1}`);
  for (const href of await page.locator('.case-open').evaluateAll(links => links.map(a => a.href))) {
    expect(href).toMatch(/^http/);
    expect(href).not.toContain('{');
  }
  // U6 is another site: another host name than this page's
  const otherSite = new URL(await page.locator('.case', { hasText: 'From another site' }).locator('a').getAttribute('href'));
  expect(otherSite.hostname).not.toBe(new URL(GALLERY).hostname);
});

test('U2, a custom page of the application: its HTML, then the widgets, on the application\'s socket', async ({ page }) => {
  const errors = trackErrors(page);
  const sockets = [];
  page.on('websocket', ws => sockets.push(new URL(ws.url()).pathname));
  await page.goto(BASE_URL + 'desk.html');
  await expect(page.locator('.desk-header')).toContainText('Trading desk');
  await expect(page.locator('.pony-tab').first()).toBeVisible({ timeout: 15000 });
  expect(sockets).toEqual([new URL('ws', BASE_URL).pathname]);
  expect(errors).toEqual([]);
});

test('U3, in an iframe: another document, the showcase as it is', async ({ page }) => {
  await page.goto(GALLERY + 'iframe.html');
  await expect(page.locator('.gallery-frame').contentFrame().locator('.pony-logo')).toHaveText('PonySDK', { timeout: 15000 });
});

test('U5, in the page\'s document: the ticket is the document\'s, the loading text gone', async ({ page }) => {
  const errors = trackErrors(page);
  await page.goto(GALLERY + 'light.html');
  await expect(page.locator('#light').getByText('USD / CHF')).toBeVisible({ timeout: 15000 });
  expect(await page.evaluate(() => document.querySelector('#light .pony-ticket') !== null)).toBe(true);
  await expect(page.locator('#light')).not.toContainText('Loading the ticket', { useInnerText: true });
  expect(errors).toEqual([]);
});

test('U8, windows and frames: a frame of the application, and a ticket popped out', async ({ page, context }) => {
  await page.goto(GALLERY + 'windows.html');
  const frame = page.locator('#framed iframe').contentFrame();
  await frame.getByRole('button', { name: 'Inside' }).click({ timeout: 15000 });
  await expect(frame.getByRole('button', { name: 'Clicked' })).toBeVisible();

  const opened = context.waitForEvent('page');
  await page.locator('#ticket .pony-ticket-pop-out').click();
  await expect((await opened).locator('.pony-ticket-popped-prices')).toHaveText(/\d/, { timeout: 15000 });
});

test('U9, two versions on a page: the page\'s terminal defines the element, the application\'s runs it', async ({ page }) => {
  const errors = trackErrors(page);
  await page.goto(GALLERY + 'versions.html');
  await expect(page.locator('#ticket').getByText('GBP / USD')).toBeVisible({ timeout: 15000 });
  await expect(page.locator('#terminals li')).toHaveText([
    new URL('vendor/ponysdk/ponyterminal/terminal.js', GALLERY).href,
    new URL('ponyterminal/terminal.js', BASE_URL).href,
  ]);
  expect(errors).toEqual([]);
});
