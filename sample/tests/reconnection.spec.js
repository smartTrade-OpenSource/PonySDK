// @ts-check
// Reconnection of the showcase (ShowcaseMain: transparent reconnection, 10 s window).
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test reconnection
const { test, expect } = require('./coverage');

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
    get offline() { return offline; },
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

/**
 * Init script: as forgetContextOnReconnect, and the page sees the reconnection's close alone, not its open nor its
 * message. Firefox does that now and then when the server closes the socket as it opens.
 */
function forgetContextOnReconnectCloseOnly() {
  const Native = window.WebSocket;
  const Patched = function (url, protocols) {
    const reconnection = /[?&]Y=\d+/.test(String(url));
    const socket = new Native(String(url).replace(/([?&]Y=)\d+/, '$1999999999'), protocols);
    if (reconnection) {
      for (const handler of ['onopen', 'onmessage']) Object.defineProperty(socket, handler, { set() {}, get() { return null; } });
    }
    return socket;
  };
  Patched.prototype = Native.prototype;
  Object.assign(Patched, { CONNECTING: 0, OPEN: 1, CLOSING: 2, CLOSED: 3 });
  window.WebSocket = Patched;
}

test('a context the server no longer has, told by the close alone: the page reloads', async ({ page }) => {
  await page.addInitScript(forgetContextOnReconnectCloseOnly);
  const sockets = await routeSockets(page);
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  const clickMe = page.getByRole('button', { name: 'Click me' });
  await expect(clickMe).toBeVisible({ timeout: 10000 });

  const reloaded = page.waitForEvent('load', { timeout: 15000 });
  await sockets.cut();
  await reloaded;
  await expect(clickMe).toHaveCount(1, { timeout: 10000 });
});

// Read on the server's side, through the Reconnection page's view of its client, not through routeWebSocket: the
// route replaces the page's socket, so it cannot see what the browser itself sends, and Playwright 1.64 reports no
// close code on Chromium for a page that navigates away. Chromium and WebKit close with the terminal's 4002, Firefox
// with its own 1001 before pagehide runs (ADR-0007).
test('a page left: the server drops its context at once (ADR-0005, ADR-0007)', async ({ page, context }) => {
  await page.goto(BASE_URL + '#reconnection', { waitUntil: 'domcontentloaded' });
  const log = page.locator('.rc-log');
  await expect(log).toContainText('is up in the frame', { timeout: 15000 });

  const client = await context.newPage();
  await client.goto(new URL(await page.locator('.rc-frame').getAttribute('src'), page.url()).href,
    { waitUntil: 'domcontentloaded' });
  await expect(log).toContainText('New session', { timeout: 15000 }); // the page now watches the tab
  await client.goto('about:blank');
  await expect(log).toContainText('destroyed: it ended', { timeout: 5000 });
  await expect(log).not.toContainText('the client is away'); // never suspended for a reconnection
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

test('a server silent for two heartbeat periods: the page closes the connection, then connects again', async ({ page }) => {
  test.slow();
  let silent = false;
  let sockets = 0;
  await page.routeWebSocket(/\/sample\/ws\b/, ws => {
    const first = sockets++ === 0;
    const server = ws.connectToServer();
    ws.onMessage(message => server.send(message));
    server.onMessage(message => { if (!(first && silent)) ws.send(message); }); // the first goes half-open
  });
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  const clickMe = page.getByRole('button', { name: 'Click me' });
  await expect(clickMe).toBeVisible({ timeout: 10000 });
  silent = true;
  const banner = page.locator('#reconnecting');
  await expect(banner).toBeVisible({ timeout: 20000 }); // past the 10 s of two periods of 5 s
  await expect(banner).toBeHidden({ timeout: 15000 }); // on another connection
  expect(sockets).toBeGreaterThan(1);
  await clickMe.click();
  await expect(page.getByText(/server handled click #1/)).toBeVisible();
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

test('a click lost with the connection: the button it disabled is enabled again once the context resumes', async ({ page }) => {
  const sockets = await routeSockets(page);
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  const clickMe = page.getByRole('button', { name: 'Click me' });
  await expect(clickMe).toBeVisible({ timeout: 10000 });
  await clickMe.click();
  await expect(page.getByText('server handled click #1')).toBeVisible();
  await expect(clickMe).toBeEnabled();

  const banner = page.locator('#reconnecting');
  sockets.offline = true;
  await sockets.cut();
  await expect(banner).toBeVisible();
  // The page's overlay takes the pointer, not the keyboard: Enter on the focused button still clicks it.
  // That click never reaches the server, so no END_OF_PROCESSING comes for it
  await clickMe.press('Enter');
  await expect(clickMe).toBeDisabled();

  sockets.offline = false;
  await expect(banner).toBeHidden({ timeout: 10000 });
  await expect(clickMe).toBeEnabled();
  await clickMe.click();
  await expect(page.getByText('server handled click #2')).toBeVisible();
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

test('Move server: the frame reloads into a new session, its watched price back from its address', async ({ page }) => {
  await page.goto(BASE_URL + '#reconnection', { waitUntil: 'domcontentloaded' });
  const log = page.locator('.rc-log');
  const frame = page.frameLocator('.rc-frame');
  await expect(log).toContainText('is up in the frame', { timeout: 15000 });
  const first = (await frame.locator('.rc-client-title').innerText()).replace('Session #', '');

  await frame.locator('.rc-cell').nth(6).click();
  await expect(frame.locator('.rc-watching')).toHaveText("Watching price 7: kept in the page's address");
  await page.getByRole('button', { name: 'Move server', exact: true }).click();
  await expect(log).toContainText('moved, as a server being drained moves each of its pages', { timeout: 10000 });
  await expect(frame.locator('.rc-watching')).toHaveText(/^Watching price 7: restored from the page's address by session #\d+$/,
    { timeout: 15000 });
  await expect(frame.locator('.rc-client-title')).not.toHaveText(`Session #${first}`);
  await expect(frame.locator('.rc-cell').nth(6)).toHaveClass(/rc-watched/);
});

test("the health check the Reconnection page links to answers UP, not cached", async ({ page }) => {
  await page.goto(BASE_URL + '#reconnection', { waitUntil: 'domcontentloaded' });
  const link = page.getByRole('link', { name: "Open this server's health check" });
  const href = await link.evaluate(a => /** @type {HTMLAnchorElement} */ (a).href);
  expect(new URL(href).pathname).toBe(new URL(BASE_URL).pathname + 'health');
  const response = await page.request.get(href);
  expect(response.status()).toBe(200);
  expect(await response.text()).toBe('UP');
  expect(response.headers()['cache-control']).toBe('no-store');
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

/**
 * Init script: the page without transparent reconnection (its meta goes before the connection can be lost), with
 * the hooks ponysdk.js lets a page define; reload, which the page would do, is recorded instead (doReload).
 */
function withoutTransparentReconnection({ hook }) {
  const mark = (name, value) => document.documentElement.setAttribute(`data-${name}`, value);
  document.addEventListener('DOMContentLoaded', () => document.querySelector('meta[name="pony-reconnection"]')?.remove());
  Object.defineProperty(document, 'doReload', { configurable: false, get: () => () => mark('reload', 'asked'), set() {} });
  window.showReconnectionInformation = () => mark('notice', 'shown');
  if (hook) window.onPonyReconnected = () => mark('back', 'told');
}

/** Pings while offline fail: the terminal tries again until one answers. */
async function routePings(page, sockets) {
  let failed = 0;
  await page.route(/[?&]ping=/, route => {
    if (!sockets.offline) return route.continue();
    failed++;
    return route.fulfill({ status: 503 });
  });
  return () => failed;
}

for (const hook of [true, false]) {
  test(`without transparent reconnection: the page's listeners and notice, then ${hook ? 'its hook' : 'a reload'} once the server answers`, async ({ page }) => {
    const errors = [];
    page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
    await page.addInitScript(withoutTransparentReconnection, { hook });
    const sockets = await routeSockets(page);
    const failedPings = await routePings(page, sockets);
    await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('button', { name: 'Click me' })).toBeVisible({ timeout: 10000 });
    await page.evaluate(() => {
      const listeners = /** @type {any} */ (document).onConnectionLostListeners;
      listeners.push(() => { throw new Error('a listener that fails'); });
      listeners.push(() => document.documentElement.setAttribute('data-lost', 'heard'));
    });

    sockets.offline = true;
    await sockets.cut();
    const html = page.locator('html');
    await expect(html).toHaveAttribute('data-lost', 'heard'); // after the one that failed
    await expect(html).toHaveAttribute('data-notice', 'shown');
    expect(errors.some(e => e.includes('A connection-lost listener failed'))).toBe(true);
    await expect.poll(failedPings, { timeout: 10000 }).toBeGreaterThan(0);
    sockets.offline = false;
    if (hook) await expect(html).toHaveAttribute('data-back', 'told', { timeout: 10000 });
    else await expect(html).toHaveAttribute('data-reload', 'asked', { timeout: 10000 });
  });
}
