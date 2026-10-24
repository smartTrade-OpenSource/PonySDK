// @ts-check
// Web Components E2E against the showcase (ShowcaseMain: transparent reconnection + string dictionary on).
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test webcomponents
const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/** Collects page errors + console errors; the protocol reader throws on e.g. "Unknown dictionary ID". */
function trackErrors(page) {
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => {
    if (m.type() !== 'error') return;
    if (/favicon\.ico$/.test(m.location().url || '')) return; // the sample ships no favicon
    errors.push('console: ' + m.text());
  });
  return errors;
}

async function openWebComponentsTab(page) {
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
  await page.locator('.pony-tab', { hasText: 'Web Components' }).click();
  await expect(page.locator('my-counter')).toBeVisible({ timeout: 10000 });
}

const status = page => page.locator('my-dashboard [slot="footer"]');

test.describe('Web Components', () => {

  test('counter: attribute, property, event and method round-trips', async ({ page }) => {
    const errors = trackErrors(page);
    await openWebComponentsTab(page);
    const counter = page.locator('my-counter');

    await expect(counter).toContainText('Clicks:');      // attribute set before attach
    await expect(counter).toContainText('0');            // property set before attach

    await counter.locator('button').click();             // client event -> server listener
    await expect(status(page)).toContainText('count-changed');
    await expect(status(page)).toContainText('"count":1');

    await page.getByRole('button', { name: 'Set count = 42' }).click(); // property from server
    await expect(counter).toContainText('42');

    await page.getByRole('button', { name: 'Reset counter' }).click();  // method call
    await expect(counter).toContainText(/:\s*0/);

    expect(errors).toEqual([]);
  });

  test('slots: initial children projected, children added later are created', async ({ page }) => {
    const errors = trackErrors(page);
    await openWebComponentsTab(page);
    const dashboard = page.locator('my-dashboard');

    await expect(dashboard.locator('[slot="toolbar"]', { hasText: 'Refresh' })).toBeVisible();
    await expect(dashboard.locator('[slot="content"]')).toContainText("PLabel inside 'content' slot");
    await expect(page.locator('my-card')).toContainText('Card body text set from server');

    // Regression: SlotPanel.add on a live component never created the child
    await page.getByRole('button', { name: 'Add toolbar item' }).click();
    await page.getByRole('button', { name: 'Add toolbar item' }).click();
    const items = dashboard.locator('[slot="toolbar"].dyn-toolbar-item');
    await expect(items).toHaveCount(2);
    await expect(items.nth(1)).toHaveText('Item 2');

    expect(errors).toEqual([]);
  });

  test('object property: merge-patch travels and re-renders an identity-checking component', async ({ page }) => {
    const errors = trackErrors(page);
    const frames = [];
    page.on('websocket', ws => ws.on('framereceived', f => frames.push(Buffer.from(f.payload))));
    await openWebComponentsTab(page);
    const stats = page.locator('my-stats');

    await expect(stats.locator('[data-key="trades"]')).toContainText('0');
    await expect(stats.locator('[data-key="note"]')).toContainText('initial snapshot');
    const rendersBefore = await stats.evaluate(el => el.renderCount);

    frames.length = 0;
    await page.getByRole('button', { name: 'Patch: +1 trade' }).click();
    // Regression: the patch mutated the object in place and re-assigned the same reference,
    // so a component with an identity check (Lit's hasChanged) never re-rendered.
    await expect(stats.locator('[data-key="trades"]')).toContainText('1');
    expect(await stats.evaluate(el => el.renderCount)).toBeGreaterThan(rendersBefore);
    // Untouched keys survive the patch
    await expect(stats.locator('[data-key="desk"]')).toContainText('FX Spot EUR/USD');

    // Only the changed key went over the wire, not the whole object
    const wire = Buffer.concat(frames);
    expect(wire.includes('{"trades":1}')).toBe(true);
    expect(wire.includes('FX Spot EUR/USD')).toBe(false);

    await page.getByRole('button', { name: 'Patch: drop note' }).click(); // patch with null = delete key
    await expect(stats.locator('[data-key="note"]')).toHaveCount(0);
    await expect(stats.locator('[data-key="trades"]')).toContainText('1');

    expect(errors).toEqual([]);
  });

  test('transparent reconnection: dictionary entries lost in flight are re-announced', async ({ page }) => {
    const errors = trackErrors(page);
    const sockets = [];
    let dropToClient = false;
    await page.routeWebSocket(/\/sample\/ws\b/, ws => {
      const server = ws.connectToServer();
      server.onMessage(m => { if (!dropToClient) ws.send(m); }); // client->server still auto-forwarded
      // Explicit close handlers: no automatic propagation, each side is closed on purpose below
      ws.onClose(() => {});
      server.onClose(() => {});
      sockets.push({ ws, server });
    });

    await openWebComponentsTab(page);
    const dashboard = page.locator('my-dashboard');
    await expect(dashboard).not.toHaveAttribute('theme', 'light');

    // The server answers with DICTIONARY_ADD for "theme"/"light"... which never reach the client
    dropToClient = true;
    await page.getByRole('button', { name: 'Refresh' }).click();
    await page.waitForTimeout(500);

    // Network drop: the server suspends the UIContext, the client (abnormal close code) reconnects
    await sockets[0].server.close({ code: 4000, reason: 'simulated network drop' });
    await sockets[0].ws.close({ code: 4000, reason: 'simulated network drop' });
    dropToClient = false;
    await expect.poll(() => sockets.length, { timeout: 20000 }).toBe(2);
    await expect(page.locator('#reconnection')).toBeHidden({ timeout: 10000 });

    // The attribute name "theme" was interned while frames were being dropped. Re-using it must
    // re-announce it (ADD), not send a REF to an id the client never received.
    // (Export, not Refresh: Refresh stays disabled since its END_OF_PROCESSING was dropped too.)
    await page.getByRole('button', { name: 'Export' }).click();
    await expect(dashboard).toHaveAttribute('theme', 'dark', { timeout: 5000 });
    await expect(status(page)).toContainText('Export triggered!');

    // The session survived (no reload): state from before the drop is still there and live
    await page.getByRole('button', { name: 'Set count = 42' }).click();
    await expect(page.locator('my-counter')).toContainText('42');

    expect(errors.filter(e => !/WebSocket|ERR_|Failure detected|Connection to the server lost|reconnect/i.test(e))).toEqual([]);
  });
});
