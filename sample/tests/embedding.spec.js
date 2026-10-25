// @ts-check
// Applications embedded in another page with <pony-app> (docs/decisions/0008-embedded-application.md): the portal,
// a plain page of another origin that ShowcaseMain serves on port 8082, and the showcase's Embedding page.
//   ./gradlew :sample:runShowcase    then    BASE_URL='http://[::1]:8081/sample/' npx playwright test embedding
const fs = require('node:fs');
const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
/** The portal: same host as the showcase, so the same site (ADR-0008, decision 9). */
const PORTAL_URL = process.env.PORTAL_URL || (() => {
  const url = new URL(BASE_URL);
  return `${url.protocol}//${url.hostname}:8082/`;
})();

/**
 * The portal on another site than the application's (ADR-0009): 127.0.0.1 for [::1] or localhost, localhost for
 * 127.0.0.1. Chrome and WebKit then neither keep nor send the session cookie.
 */
const OTHER_SITE_PORTAL = (() => {
  const url = new URL(BASE_URL);
  const host = url.hostname === '127.0.0.1' ? 'localhost' : '127.0.0.1';
  return `${url.protocol}//${host}:8082/?app=${encodeURIComponent(BASE_URL)}`;
})();

/** Page errors and console errors, the portal's missing favicon aside. */
function trackErrors(page) {
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => {
    if (m.type() !== 'error' || /favicon\.ico$/.test(m.location().url || '')) return;
    errors.push('console: ' + m.text());
  });
  return errors;
}

async function openPortal(page) {
  await page.goto(PORTAL_URL);
  const first = page.locator('#ticket-1');
  const second = page.locator('#ticket-2');
  await expect(first.getByText('EUR / USD')).toBeVisible({ timeout: 15000 });
  await expect(second.getByText('USD / JPY')).toBeVisible({ timeout: 15000 });
  return { first, second };
}

test('the portal embeds two tickets and the showcase, each on its own context, their styles apart', async ({ page }) => {
  const errors = trackErrors(page);
  const { first, second } = await openPortal(page);
  await expect(page.locator('#showcase .pony-logo')).toHaveText('PonySDK', { timeout: 15000 });

  const context = async ticket => (await ticket.locator('.pony-ticket-origin').innerText()).replace(/.*context #/, '');
  expect(await context(first)).not.toBe(await context(second));
  await expect(first.locator('.pony-ticket-origin')).toContainText(new URL(PORTAL_URL).origin);

  // The portal's broad rules (button, serif) stay out of the shadow roots, the applications' out of the portal
  await expect(page.locator('#reset')).toHaveCSS('background-color', 'rgb(219, 39, 119)');
  await expect(first.locator('.pony-ticket-buy')).not.toHaveCSS('background-color', 'rgb(219, 39, 119)');
  await expect(first.locator('.pony-ticket-instrument')).not.toHaveCSS('font-family', /Georgia/);
  await expect(page.locator('.portal-brand')).toHaveCSS('font-family', /Georgia/);
  // The showcase's page rules, written for its page, apply in the element: :root's variables on :host (ADR-0009)
  await expect(first.locator('.pony-ticket-buy span')).toHaveCSS('color', 'rgb(4, 120, 87)');
  // The elements' own children (their loading text) are hidden once the applications show
  expect(await page.evaluate(() => [...document.querySelectorAll('pony-app')]
    .map(app => getComputedStyle(app.shadowRoot.querySelector('slot')).display))).toEqual(['none', 'none', 'none']);
  expect(errors).toEqual([]);
});

/** A font of the system, for a stylesheet's @font-face: none is in the repository. */
const SYSTEM_FONT = ['/System/Library/Fonts/Supplemental/Arial.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',
  '/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf', 'C:/Windows/Fonts/arial.ttf'].find(f => fs.existsSync(f));

test('a font the application declares in its stylesheet shows in the shadow root (ADR-0009)', async ({ page }) => {
  test.skip(SYSTEM_FONT === undefined, 'no system font to serve');
  // The showcase's stylesheet declares a font by a URL relative to it; Chrome and Firefox ignore an @font-face of a
  // shadow root: the element declares it to the document
  await page.route('**/sample/css/showcase.css', async route => {
    const response = await route.fetch();
    await route.fulfill({ response, body: await response.text()
      + '\n@font-face { font-family: PonyTestFont; src: url("../fonts/pony-test.ttf"); }'
      + '\n.pony-test-font { font: 20px PonyTestFont, monospace; }' });
  });
  await page.route('**/sample/fonts/pony-test.ttf', route => route.fulfill({
    body: fs.readFileSync(SYSTEM_FONT), contentType: 'font/ttf', headers: { 'Access-Control-Allow-Origin': '*' } }));
  const { first } = await openPortal(page);
  const widths = await first.evaluate(async element => {
    const span = style => {
      const s = document.createElement('span');
      s.textContent = 'iiiiiiWWWWWW';
      if (style === 'test') s.className = 'pony-test-font';
      else s.style.font = '20px monospace';
      element.shadowRoot.querySelector('[data-pony-body]').append(s);
      return s;
    };
    const declared = span('test');
    const fallback = span('fallback');
    await document.fonts.load('20px PonyTestFont');
    await new Promise(requestAnimationFrame);
    return [declared.offsetWidth, fallback.offsetWidth];
  });
  expect(widths[0], 'the declared font, not the monospace fallback').not.toBe(widths[1]);
});

test('attributes in: the host changes the instrument, the application follows', async ({ page }) => {
  const { first, second } = await openPortal(page);
  await page.locator('#instrument').selectOption('GBPUSD');
  await expect(first.getByText('GBP / USD')).toBeVisible();
  await expect(second.getByText('USD / JPY')).toBeVisible(); // the other ticket is not concerned
});

test('events out: a trade lands in the host page\'s own blotter', async ({ page }) => {
  const { first } = await openPortal(page);
  await first.locator('.pony-ticket-buy').click();
  const row = page.locator('#trades tr').first();
  await expect(row).toContainText('ticket-1');
  await expect(row).toContainText('BUY');
  await expect(row).toContainText('1,000,000');
  await expect(row).toContainText('EURUSD');
});

test('title and location: the application asks, the host decides (ADR-0009)', async ({ page, context }) => {
  const { first } = await openPortal(page);
  await expect(page.locator('#ticket-1-title')).toHaveText('EUR/USD ticket'); // pony-title
  await page.locator('#instrument').selectOption('USDCHF');
  await expect(page.locator('#ticket-1-title')).toHaveText('USD/CHF ticket');
  expect(await page.title()).toBe('Northwind Treasury portal'); // the host's own

  const opened = context.waitForEvent('page'); // pony-location: the portal opens a tab
  await first.locator('.pony-ticket-full').click();
  const tab = await opened;
  await tab.waitForLoadState('domcontentloaded');
  expect(tab.url().split('#')[0]).toBe(new URL('./', await first.getAttribute('src')).href);
  expect(page.url()).toBe(PORTAL_URL); // the portal stays
  await tab.close();
});

test('events in: the host\'s reset event reaches the application', async ({ page }) => {
  const { second } = await openPortal(page);
  const quantity = second.locator('.pony-ticket-quantity');
  await quantity.fill('42');
  await quantity.press('Tab'); // the server reads it on change
  await second.locator('.pony-ticket-buy').click();
  await expect(page.locator('#trades tr').first()).toContainText('42');
  await page.locator('#reset').click();
  await expect(quantity).toHaveValue('500000'); // the element's quantity attribute
});

test('a sell asks in a dialog, shown in the element with the application\'s styles', async ({ page }) => {
  const { first } = await openPortal(page);
  await first.locator('.pony-ticket-sell').click();
  const dialog = first.locator('.pony-ticket-dialog');
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('Confirm the sale');
  const confirm = dialog.getByRole('button', { name: 'Confirm' });
  await expect(confirm).not.toHaveCSS('background-color', 'rgb(219, 39, 119)');
  await confirm.click();
  await expect(dialog).toBeHidden();
  await expect(page.locator('#trades tr').first()).toContainText('SELL');
});

test('a press inside a popup of a shadow root is not taken for a press outside it', async ({ page }) => {
  await openPortal(page);
  const showcase = page.locator('#showcase');
  const box = showcase.locator('.pony-row', { hasText: 'DateBox with picker' }).locator('input');
  await box.click();
  const picker = showcase.locator('#' + await box.getAttribute('aria-controls')); // the page has another picker
  const day = picker.locator('[role=gridcell]:not([data-filler])', { hasText: /^15$/ });
  await expect(day).toBeVisible();
  await day.click();
  await expect(box).toHaveValue(/^\d{4}\/\d\d\/15$/);
});

test('the embedded application\'s navigation leaves the host page\'s fragment alone', async ({ page }) => {
  await openPortal(page);
  const showcase = page.locator('#showcase');
  await showcase.locator('.pony-tab', { hasText: 'Layouts' }).click();
  await expect(showcase.locator('.pony-crumb')).toContainText('Layouts');
  expect(new URL(page.url()).hash).toBe('');
});

test('an element taken out of the page ends its application; put back, it starts another', async ({ page }) => {
  const sockets = [];
  page.on('websocket', ws => sockets.push(ws));
  const { second } = await openPortal(page);
  const before = (await second.locator('.pony-ticket-origin').innerText()).replace(/.*context #/, '');
  const open = () => sockets.filter(ws => !ws.isClosed()).length;
  await expect.poll(open).toBe(3); // two tickets and the showcase

  await page.evaluate(() => { window.removedTicket = document.getElementById('ticket-2'); window.removedTicket.remove(); });
  await expect.poll(open).toBe(2);

  await page.evaluate(() => document.querySelector('.portal-tickets').append(window.removedTicket));
  await expect(second.getByText('USD / JPY')).toBeVisible({ timeout: 15000 });
  const after = (await second.locator('.pony-ticket-origin').innerText()).replace(/.*context #/, '');
  expect(after).not.toBe(before);
  expect(open()).toBe(3);
});

test('isolation="none": the application renders in the host\'s document', async ({ page }) => {
  await openPortal(page);
  await page.evaluate(() => {
    const app = document.createElement('pony-app');
    app.id = 'light';
    app.setAttribute('isolation', 'none');
    app.setAttribute('view', 'ticket');
    app.setAttribute('instrument', 'USDCHF');
    app.setAttribute('src', document.getElementById('ticket-1').getAttribute('src'));
    document.querySelector('.portal-tickets').append(app);
  });
  await expect(page.locator('#light').getByText('USD / CHF')).toBeVisible({ timeout: 15000 });
  // The showcase's :root and body rules are the element's, not the host page's (ADR-0009)
  await expect(page.locator('#light .pony-ticket-buy span')).toHaveCSS('color', 'rgb(4, 120, 87)');
  await expect(page.locator('body')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)');
  expect(await page.evaluate(() => {
    const app = document.getElementById('light');
    return app.shadowRoot === null && document.querySelector('#light > [data-pony-body] .pony-ticket') !== null;
  })).toBe(true);
});

test('a lost connection: the host is told, then the applications resume their contexts', async ({ page }) => {
  const sockets = [];
  let offline = false;
  await page.routeWebSocket(/\/sample\/ws\b/, ws => {
    if (offline) return ws.close({ code: 4000, reason: 'still offline' });
    ws.connectToServer();
    sockets.push(ws);
  });
  const { first } = await openPortal(page);
  const before = await first.locator('.pony-ticket-origin').innerText();

  offline = true;
  for (const ws of sockets.splice(0)) await ws.close({ code: 4000, reason: 'simulated network drop' });
  await expect(page.locator('#status')).toHaveAttribute('data-state', 'lost');
  offline = false;
  await expect(page.locator('#status')).toHaveAttribute('data-state', 'back', { timeout: 15000 });

  await expect(first.locator('.pony-ticket-origin')).toHaveText(before); // the same context
  await page.locator('#instrument').selectOption('USDCHF');
  await expect(first.getByText('USD / CHF')).toBeVisible();
});

test('another site: the credential reaches the application, its context key keeps uploads and the reconnection', async ({ page }) => {
  const errors = trackErrors(page);
  const sockets = [];
  let offline = false;
  await page.routeWebSocket(/\/sample\/ws\b/, ws => {
    if (offline) return ws.close({ code: 4000, reason: 'still offline' });
    ws.connectToServer();
    sockets.push(ws);
  });
  await page.goto(OTHER_SITE_PORTAL);
  const first = page.locator('#ticket-1');
  await expect(first.getByText('EUR / USD')).toBeVisible({ timeout: 15000 });
  await expect(first.locator('.pony-ticket-origin')).toContainText('signed in as alice@northwind');

  // An upload of the embedded showcase: found by its key, not by a cookie
  const showcase = page.locator('#showcase');
  const input = showcase.locator('input[type=file]').first();
  await input.setInputFiles({ name: 'a.txt', mimeType: 'text/plain', buffer: Buffer.from('hello') });
  const upload = page.waitForResponse(r => r.url().includes('/sample/stream'));
  await showcase.getByRole('button', { name: 'Submit' }).first().click();
  expect((await upload).status()).toBe(200);
  expect(errors).toEqual([]); // the drop below logs the lost connection

  // A lost connection resumes the same contexts
  const before = await page.locator('pony-app .pony-ticket-origin').allInnerTexts();
  offline = true;
  for (const ws of sockets.splice(0)) await ws.close({ code: 4000, reason: 'simulated network drop' });
  await expect(page.locator('#status')).toHaveAttribute('data-state', 'lost');
  offline = false;
  await expect(page.locator('#status')).toHaveAttribute('data-state', 'back', { timeout: 15000 });
  await expect.poll(() => page.locator('pony-app .pony-ticket-origin').allInnerTexts()).toEqual(before);
  await page.locator('#instrument').selectOption('GBPUSD');
  await expect(first.getByText('GBP / USD')).toBeVisible();
});

test('a ticket popped out in a window of its own talks to its element by postMessage (ADR-0009)', async ({ page, context }) => {
  const { first } = await openPortal(page);
  const opened = context.waitForEvent('page');
  await first.locator('.pony-ticket-pop-out').click();
  const popup = await opened;
  const prices = popup.locator('.pony-ticket-popped-prices');
  await expect(prices).toHaveText(/^\d+\.\d+ \/ \d+\.\d+$/, { timeout: 15000 });
  await expect(popup).toHaveTitle('EUR/USD ticket');
  const before = await prices.innerText();
  await expect(prices).not.toHaveText(before); // pushed by the server through the element's terminal
  await popup.locator('.pony-ticket-popped-buy').click();
  await expect(page.locator('#trades tr').first()).toContainText('BUY'); // from the window, to the portal's blotter

  await expect(first.locator('.pony-ticket-pop-out')).toHaveText(/^Popped out/);
  await popup.close(); // the server is told: popping out again opens another window
  await expect(first.locator('.pony-ticket-pop-out')).toHaveText('Pop out');
  const again = context.waitForEvent('page');
  await first.locator('.pony-ticket-pop-out').click();
  await expect((await again).locator('.pony-ticket-popped-prices')).toHaveText(/\d/, { timeout: 15000 });
});

test('another site: a popped out ticket proves its context with the element\'s keys', async ({ page, context }) => {
  await page.goto(OTHER_SITE_PORTAL);
  const first = page.locator('#ticket-1');
  await expect(first.getByText('EUR / USD')).toBeVisible({ timeout: 15000 });
  const opened = context.waitForEvent('page');
  await first.locator('.pony-ticket-pop-out').click();
  const popup = await opened;
  await expect(popup.locator('.pony-ticket-popped-prices')).toHaveText(/\d/, { timeout: 15000 });
  await popup.locator('.pony-ticket-popped-buy').click();
  await expect(page.locator('#trades tr').first()).toContainText('BUY');
});

test('a frame of an embedded application runs a terminal of its own, fed by postMessage (ADR-0009)', async ({ page }) => {
  await openPortal(page);
  await page.evaluate(() => {
    const app = document.createElement('pony-app');
    app.id = 'framed';
    app.setAttribute('src', document.getElementById('ticket-1').getAttribute('src') + '#oracle-frame');
    document.querySelector('.portal-app').append(app);
  });
  const frame = page.locator('#framed iframe').contentFrame();
  await frame.getByRole('button', { name: 'Inside' }).click({ timeout: 15000 });
  await expect(frame.getByRole('button', { name: 'Clicked' })).toBeVisible();
});

test('a page of an origin the application does not list can neither feed its child page nor hear from it', async ({ page, context }) => {
  const errors = [];
  context.on('page', p => p.on('console', m => { if (m.type() === 'error') errors.push(m.text()); }));
  await page.goto('data:text/html,<title>another origin</title>');
  // A window page of the application, as an embedded terminal opens it: WINDOW_ID, UI_CONTEXT_ID, BRIDGED
  const child = new URL('?2=1&1=1&br=1', BASE_URL).href;
  const received = await page.evaluate(async url => {
    const got = [];
    const ready = new Promise(resolve => window.addEventListener('message', e => {
      got.push(e.data);
      resolve();
    }));
    const opened = window.open(url);
    await ready;
    opened.postMessage({ pony: 'init', keys: null, title: 'fed', entries: [] }, '*');
    await new Promise(resolve => setTimeout(resolve, 1500));
    return got;
  }, child);
  expect(received).toEqual([{ pony: 'ready' }]); // nothing else: its messages wait for an accepted parent
  await expect.poll(() => errors.some(e => /not a parent of this application/.test(e))).toBe(true);
});

test('an embedded application\'s cookies are its origin\'s: written and read again by a new context (ADR-0009)', async ({ page, context }) => {
  await openPortal(page);
  const src = await page.locator('#ticket-1').getAttribute('src');
  // The Misc page counts its loads with a cookie: an element of its own each time, so a context of its own
  const load = async () => {
    await page.evaluate(url => {
      document.getElementById('misc')?.remove();
      const app = document.createElement('pony-app');
      app.id = 'misc';
      app.setAttribute('src', url + '#misc');
      document.querySelector('.portal-app').append(app);
    }, src);
    const label = page.locator('#misc .misc-cookie');
    await expect(label).toHaveText(/^Loaded \d+ time/, { timeout: 15000 });
    return Number(/\d+/.exec(await label.innerText())[0]);
  };
  const first = await load();
  // By name: WebKit gives an IPv6 host's cookies the domain ::1, which a URL filter of [::1] misses
  await expect.poll(async () => (await context.cookies()).find(c => c.name === 'showcase-visits')?.value).toBe(String(first));
  expect(await page.evaluate(() => document.cookie)).not.toContain('showcase-visits'); // not the portal's own
  expect(await load()).toBe(first + 1);
});

test('the Embedding page: an application embeds another and drives it from Java', async ({ page }) => {
  const errors = trackErrors(page);
  await page.goto(BASE_URL + '#embedding');
  const ticket = page.locator('pony-app.pony-embedding-ticket');
  await expect(ticket.getByText('EUR / USD')).toBeVisible({ timeout: 15000 });

  await page.locator('.pony-embedding-host select').selectOption('USDJPY'); // PWebComponent.attr
  await expect(ticket.getByText('USD / JPY')).toBeVisible();
  await ticket.locator('.pony-ticket-buy').click();
  await expect(page.locator('.pony-embedding-trade').first()).toContainText('BUY'); // PWebComponent.onEvent
  await expect(page.locator('.pony-embedding-trade').first()).toContainText('USDJPY');
  expect(errors).toEqual([]);
});
