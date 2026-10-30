// @ts-check
// Client commands (docs/decisions/0027-client-commands.md): the showcase's Client commands page. A click shows, hides,
// toggles and focuses with no round trip; the event goes to the server too when a server listener wants it; a target the
// server destroyed is skipped.
//   ./gradlew :sample:runShowcase    then    BASE_URL='http://localhost:8081/sample/' npx playwright test client-commands
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/**
 * The page, with its WebSocket routed through the test: what the server sends can be held or delayed, and what the
 * terminal sends is kept. Held messages are released in order.
 */
async function commandsPage(page, { delay = 0 } = {}) {
  const problems = [];
  page.on('pageerror', e => problems.push('pageerror: ' + e.message));
  page.on('console', m => {
    if ((m.type() === 'error' || m.type() === 'warning') && !/favicon\.ico$/.test(m.location().url || '')) problems.push(m.type() + ': ' + m.text());
  });
  const net = {
    held: false, delay, queue: [], sent: [], ws: null,
    /** What was held goes to the page, in order. */
    release() {
      net.held = false;
      for (const message of net.queue.splice(0)) net.ws.send(message);
    },
  };
  await page.routeWebSocket(/\/ws(\?|$)/, ws => {
    net.ws = ws;
    const server = ws.connectToServer();
    ws.onMessage(message => {
      net.sent.push(message);
      server.send(message);
    });
    server.onMessage(message => {
      if (net.held) net.queue.push(message);
      else if (net.delay > 0) setTimeout(() => ws.send(message), net.delay);
      else ws.send(message);
    });
  });
  // what the client panel shows at the moment the terminal sends a message: the commands ran before the event went
  await page.addInitScript(() => {
    const send = WebSocket.prototype.send;
    window['__panelAtSend'] = [];
    WebSocket.prototype.send = function (data) {
      const panel = document.querySelector('.cmd-client .cmd-panel');
      if (typeof data === 'string' && data.includes('"click"') && panel) window['__panelAtSend'].push(panel.style.display);
      return send.call(this, data);
    };
  });
  await page.goto(BASE_URL + '#client-commands');
  await expect(page.locator('.pony-page-title', { hasText: 'Client commands' })).toBeVisible({ timeout: 10000 });
  return { problems, net };
}

test('a click changes the page before any message of the server arrives', async ({ page }) => {
  const { problems, net } = await commandsPage(page);
  const clientPanel = page.locator('.cmd-client .cmd-panel');
  const serverPanel = page.locator('.cmd-server .cmd-panel');
  const toggle = page.locator('.cmd-client .pony-Button', { hasText: 'Toggle the panel' });
  await expect(clientPanel).toBeVisible();

  net.held = true; // the server's answers wait: nothing arrives from now on
  await toggle.click();
  await expect(clientPanel).toBeHidden({ timeout: 500 });
  await expect(toggle).toHaveClass(/cmd-open/);
  await expect(page.locator('.cmd-client input')).toBeFocused();

  // the same click on the server's panel waits for the answer
  await page.locator('.cmd-server .pony-Button', { hasText: 'Toggle the panel' }).click();
  await page.waitForTimeout(500);
  await expect(serverPanel).toBeVisible();

  net.release(); // the answers come: the server's panel follows, the client's stays where the click put it
  await expect(serverPanel).toBeHidden({ timeout: 5000 });
  await expect(clientPanel).toBeHidden();
  expect(problems).toEqual([]);
});

test('the commands run before the event is sent, which still reaches the server', async ({ page }) => {
  const { problems, net } = await commandsPage(page);
  const toggle = page.locator('.cmd-client .pony-Button', { hasText: 'Toggle the panel' });
  await page.locator('.cmd-slow input').uncheck(); // the answers are not delayed
  await expect(page.locator('.cmd-client .cmd-panel')).toBeVisible();
  await page.evaluate(() => { window['__panelAtSend'] = []; });
  const before = net.sent.length;

  await toggle.click();
  await expect(page.locator('.cmd-seen')).toHaveText('The server has seen 1 click(s)', { timeout: 5000 });
  expect(net.sent.length).toBeGreaterThan(before);
  expect(await page.evaluate(() => window['__panelAtSend'])).toEqual(['none']); // already hidden when it went out
  await expect(page.locator('.cmd-client .cmd-panel')).toBeHidden();

  // the server did not follow: its view of the panel is the one it set
  await page.getByRole('button', { name: 'What does the server see?' }).click();
  await expect(page.locator('.cmd-knows')).toContainText('The server thinks the panel is visible');

  await toggle.click(); // and back: the browser's state alone changed
  await expect(page.locator('.cmd-client .cmd-panel')).toBeVisible();
  await expect(page.locator('.cmd-seen')).toHaveText('The server has seen 2 click(s)', { timeout: 5000 });
  expect(problems).toEqual([]);
});

test('commands alone send nothing to the server', async ({ page }) => {
  const { problems, net } = await commandsPage(page);
  const hint = page.getByRole('button', { name: 'A hint, purely client side' });
  const pill = page.locator('.cmd-pill');
  await expect(pill).not.toHaveClass(/cmd-on/);
  const before = net.sent.length;

  await hint.hover();
  await expect(pill).toHaveClass(/cmd-on/);
  await page.mouse.move(5, 5);
  await expect(pill).not.toHaveClass(/cmd-on/);
  expect(net.sent.slice(before).filter(message => typeof message === 'string' && /mouse(enter|leave)/.test(message))).toEqual([]);
  expect(problems).toEqual([]);
});

test('a target the server collected is skipped, the other commands run, nothing is reported', async ({ page }) => {
  const { problems } = await commandsPage(page);
  await page.locator('.cmd-slow input').uncheck();
  const toggle = page.locator('.cmd-client .pony-Button', { hasText: 'Toggle the panel' });
  await page.getByRole('button', { name: 'Remove the panel' }).click();
  await expect(page.locator('.cmd-collected')).toHaveText('The panel is collected', { timeout: 5000 });
  await expect(page.locator('.cmd-client .cmd-panel')).toHaveCount(0);
  // the collection reaches the terminal with the next message the server handles (PObjectCache)
  await page.getByRole('button', { name: 'What does the server see?' }).click();
  await expect(page.locator('.cmd-knows')).toHaveText('The panel is gone');
  await page.waitForTimeout(300);

  await toggle.click();
  await expect(toggle).toHaveClass(/cmd-open/); // the other commands ran
  await expect(page.locator('.cmd-client input')).toBeFocused();
  await expect(page.locator('.cmd-seen')).toHaveText('The server has seen 1 click(s)', { timeout: 5000 }); // the event too
  expect(problems).toEqual([]);
});

// The figures of ADR-0027: from the click to the visible change, with and without a round trip
for (const delay of [0, 100, 300]) {
  test(`latency, the server answering after ${delay} ms: the client command does not wait`, async ({ page }) => {
    const { net } = await commandsPage(page, { delay });
    await page.locator('.cmd-slow input').uncheck();
    net.delay = delay; // the answer of the server crosses the network: on top of its own work

    const measure = (panelSelector, buttonSelector) => page.evaluate(async ([panelSelector, buttonSelector]) => {
      const panel = document.querySelector(panelSelector);
      const button = document.querySelector(buttonSelector);
      const was = getComputedStyle(panel).display;
      return await new Promise(resolve => {
        const observer = new MutationObserver(() => {
          if (getComputedStyle(panel).display !== was) {
            observer.disconnect();
            resolve(performance.now() - start);
          }
        });
        observer.observe(panel, { attributes: true });
        const start = performance.now();
        button.click();
      });
    }, [panelSelector, buttonSelector]);

    const server = await measure('.cmd-server .cmd-panel', '.cmd-server .pony-Button');
    const client = await measure('.cmd-client .cmd-panel', '.cmd-client .pony-Button');
    test.info().annotations.push({ type: 'latency', description: `delay ${delay} ms: server ${server.toFixed(1)} ms, client ${client.toFixed(1)} ms` });
    console.log(`latency delay=${delay} ms: server round trip ${server.toFixed(1)} ms, client command ${client.toFixed(1)} ms`);
    expect(client).toBeLessThan(50);
    expect(server).toBeGreaterThanOrEqual(delay);
    if (delay > 0) expect(server).toBeGreaterThan(client * 2);
  });
}
