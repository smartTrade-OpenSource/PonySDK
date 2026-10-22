// @ts-check
// Reconnection of the showcase (ShowcaseMain: transparent reconnection, 10 s window).
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test reconnection
const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/**
 * Routes the showcase WebSocket through the test: cut() drops the current one with an abnormal close code, and
 * while offline is set the new ones are refused the same way.
 */
async function routeSockets(page) {
  const sockets = [];
  let offline = false;
  await page.routeWebSocket(/\/sample\/ws\b/, ws => {
    if (offline) return ws.close({ code: 4000, reason: 'still offline' });
    ws.connectToServer();
    sockets.push(ws);
  });
  return {
    cut: () => sockets[sockets.length - 1].close({ code: 4000, reason: 'simulated network drop' }),
    set offline(value) { offline = value; },
  };
}

/**
 * Init script: a reconnection asks for a context the server does not know, as after a server restart or a drop
 * longer than the reconnection window. "Y" is ClientToServerModel.RECONNECT_UI_CONTEXT_ID.
 */
function forgetContextOnReconnect() {
  const Native = window.WebSocket;
  const Patched = function (url, protocols) {
    return new Native(String(url).replace(/([?&]Y=)\d+/, '$1999999999'), protocols);
  };
  Patched.prototype = Native.prototype;
  Object.assign(Patched, { CONNECTING: 0, OPEN: 1, CLOSING: 2, CLOSED: 3 });
  window.WebSocket = Patched;
}

test('a context the server no longer has: the page reloads instead of stacking a second app', async ({ page }) => {
  await page.addInitScript(forgetContextOnReconnect);
  const sockets = await routeSockets(page);
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  const clickMe = page.getByRole('button', { name: 'Click me' });
  await expect(clickMe).toBeVisible({ timeout: 10000 });

  // The server answers CREATE_CONTEXT with no context behind it: the page reloads and starts one
  const reloaded = page.waitForEvent('load', { timeout: 15000 });
  await sockets.cut();
  await reloaded;

  await expect(clickMe).toHaveCount(1, { timeout: 10000 });
  await clickMe.click();
  await expect(page.getByText('server handled click #1')).toBeVisible();
  expect(new URL(page.url()).hash).toBe('#inputs');
});

test('a page left closes its socket with 4002, the code that makes the server drop its context (ADR-0005)', async ({ page }) => {
  const closes = [];
  await page.routeWebSocket(/\/sample\/ws\b/, ws => {
    const server = ws.connectToServer();
    ws.onClose((code, reason) => {
      closes.push(code);
      server.close({ code, reason });
    });
  });
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Click me' })).toBeVisible({ timeout: 10000 });
  await page.goto('about:blank');
  await expect.poll(() => closes).toEqual([4002]);
});

test('a page busy for two heartbeat periods keeps its connection: its browser answers the pings', async ({ page }) => {
  const sockets = [];
  page.on('websocket', ws => sockets.push(ws));
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  const clickMe = page.getByRole('button', { name: 'Click me' });
  await expect(clickMe).toBeVisible({ timeout: 10000 });
  await clickMe.click();
  await expect(page.getByText('server handled click #1')).toBeVisible();

  // 12 s without a message from the page: past the 10 s the server allows a silent client (2 periods of 5 s)
  await page.evaluate(() => { const end = Date.now() + 12000; while (Date.now() < end); });
  await clickMe.click();
  await expect(page.getByText('server handled click #2')).toBeVisible(); // same server state
  expect(sockets.length).toBe(1); // and no reconnection
});

test('a short drop: a banner while the connection is lost, then the same context resumes', async ({ page }) => {
  const sockets = await routeSockets(page);
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  const clickMe = page.getByRole('button', { name: 'Click me' });
  await expect(clickMe).toBeVisible({ timeout: 10000 });
  await clickMe.click();
  await expect(page.getByText('server handled click #1')).toBeVisible();

  const banner = page.locator('#reconnecting');
  sockets.offline = true;
  await sockets.cut();
  await expect(banner).toBeVisible();
  await expect(banner).toContainText('Connection to server lost');
  await expect(page.locator('.pony-badge')).toHaveCSS('color', 'rgb(180, 83, 9)'); // --warn

  sockets.offline = false;
  await expect(banner).toBeHidden({ timeout: 10000 });
  await expect(page.locator('.pony-badge')).toHaveCSS('color', 'rgb(4, 120, 87)'); // --good
  await clickMe.click();
  await expect(page.getByText('server handled click #2')).toBeVisible(); // same server state: resumed, not reloaded
});

// On a page made of terminal objects only (OraclePages#reconnect): it runs on both terminals
test.describe('a page of terminal objects', () => {
  test('a short drop: the same context resumes', async ({ page }) => {
    await page.addInitScript(() => { window.onPonyReconnected = () => { window.reconnected = true; }; });
    const sockets = await routeSockets(page);
    await page.goto(BASE_URL + '#oracle-reconnect');
    await page.getByRole('button', { name: 'Clicks: 0' }).click();
    await expect(page.getByRole('button', { name: 'Clicks: 1' })).toBeVisible();
    let reloaded = false;
    page.on('load', () => { reloaded = true; });

    sockets.offline = true;
    await sockets.cut();
    await page.waitForTimeout(2500); // a ping cycle while offline
    sockets.offline = false;
    await page.waitForFunction(() => window.reconnected === true, null, { timeout: 15000 }); // RECONNECT_CONTEXT

    await page.getByRole('button', { name: 'Clicks: 1' }).click();
    await expect(page.getByRole('button', { name: 'Clicks: 2' })).toBeVisible(); // the server's count went on
    expect(reloaded).toBe(false);
  });

  test('a context the server no longer has: the page reloads', async ({ page }) => {
    await page.addInitScript(forgetContextOnReconnect);
    const sockets = await routeSockets(page);
    await page.goto(BASE_URL + '#oracle-reconnect');
    await page.getByRole('button', { name: 'Clicks: 0' }).click();
    await expect(page.getByRole('button', { name: 'Clicks: 1' })).toBeVisible();

    const reloaded = page.waitForEvent('load', { timeout: 15000 });
    await sockets.cut();
    await reloaded;
    await expect(page.getByRole('button', { name: 'Clicks: 0' })).toBeVisible({ timeout: 10000 });
  });
});

test('the Reconnection page drives its frame session through SessionAdmin, up to the cap', async ({ page }) => {
  test.setTimeout(60000);
  await page.goto(BASE_URL + '#reconnection', { waitUntil: 'domcontentloaded' });
  const state = page.locator('.rc-state');
  const log = page.locator('.rc-log');
  const button = name => page.getByRole('button', { name, exact: true });
  const frame = page.frameLocator('.rc-frame');
  await expect(log).toContainText('is up in the frame', { timeout: 15000 });
  await expect(state).toHaveText('Connected');

  // Connected: the rows reach the client
  await button('Push 1 000 rows').click();
  await expect(frame.locator('.rc-rows-head')).toContainText('1000 rows');

  // Away: its reconnection refused, the server holds what the application writes
  await button('Disconnect').click();
  await expect(state).toHaveText('Client away · held');
  await expect(frame.locator('#reconnecting')).toBeVisible();
  await expect(page.locator('.rc-gauge-value').first()).toHaveText(/^[\d ]+ \/ 10 000$/);

  // Back: the same session resumes
  await button('Reconnect').click();
  await expect(log).toContainText('Client back after', { timeout: 10000 });
  await expect(state).toHaveText('Connected');
  await expect(frame.locator('#reconnecting')).toBeHidden();

  // Over the cap: destroyed, the frame reloads into a new session
  await button('Disconnect').click();
  await expect(state).toHaveText('Client away · held');
  await button('Push 1 000 rows').click();
  await button('Push 1 000 rows').click();
  await expect(log).toContainText('went over the cap', { timeout: 10000 });
  await expect(log).toContainText('New session', { timeout: 15000 });
  await expect(state).toHaveText('Connected');
});
