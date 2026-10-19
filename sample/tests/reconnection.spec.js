// @ts-check
// Reconnection of the showcase (ShowcaseMain: transparent reconnection, 10 s window).
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test reconnection
const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/** Routes the showcase WebSocket through the test; cut() drops the current one with an abnormal close code. */
async function routeSockets(page) {
  const sockets = [];
  await page.routeWebSocket(/\/sample\/ws\b/, ws => {
    ws.connectToServer();
    sockets.push(ws);
  });
  return { cut: () => sockets[sockets.length - 1].close({ code: 4000, reason: 'simulated network drop' }) };
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

  // The server answers the reconnection with a new context: only a reload gives a page that matches it
  const reloaded = page.waitForEvent('load', { timeout: 15000 });
  await sockets.cut();
  await reloaded;

  await expect(clickMe).toHaveCount(1, { timeout: 10000 });
  await clickMe.click();
  await expect(page.getByText('server handled click #1')).toBeVisible();
  expect(new URL(page.url()).hash).toBe('#inputs');
});
