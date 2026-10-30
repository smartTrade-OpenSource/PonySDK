// @ts-check
// Applications embedded in another page with <pony-app> (docs/decisions/0008-embedded-application.md): the portal,
// a plain page of another origin that ShowcaseMain serves on port 8082, and the showcase's Embedding page.
//   ./gradlew :sample:runShowcase    then    BASE_URL='http://[::1]:8081/sample/' npx playwright test embedding
const fs = require('node:fs');
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
/** The portal: same host as the showcase, so the same site (ADR-0008, decision 9). */
const PORTAL_URL = process.env.PORTAL_URL || (() => {
  const url = new URL(BASE_URL);
  return `${url.protocol}//${url.hostname}:8082/portal.html`;
})();

/**
 * The portal on another site than the application's (ADR-0009): 127.0.0.1 for [::1] or localhost, localhost for
 * 127.0.0.1. Chrome and WebKit then neither keep nor send the session cookie.
 */
const OTHER_SITE_PORTAL = (() => {
  const url = new URL(BASE_URL);
  const host = url.hostname === '127.0.0.1' ? 'localhost' : '127.0.0.1';
  return `${url.protocol}//${host}:8082/portal.html?app=${encodeURIComponent(BASE_URL)}`;
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
    // In the document: its shadow root only places the container (a slot), and the host's children
    return app.shadowRoot.querySelector('[data-pony-body]') === null
      && document.querySelector('#light > [data-pony-body] .pony-ticket') !== null;
  })).toBe(true);
});

test('isolation="none": the host\'s loading text, a text node, goes once the application shows', async ({ page }) => {
  await openPortal(page);
  await page.evaluate(() => {
    const app = document.createElement('pony-app');
    app.id = 'light-text';
    app.setAttribute('isolation', 'none');
    app.setAttribute('view', 'ticket');
    app.append('Loading the light ticket…');
    app.setAttribute('src', document.getElementById('ticket-1').getAttribute('src'));
    document.querySelector('.portal-tickets').append(app);
  });
  await expect(page.locator('#light-text .pony-ticket')).toBeVisible({ timeout: 15000 });
  // A text node: no selector hides it; its box is empty once hidden
  const height = () => page.evaluate(() => {
    const range = document.createRange();
    range.selectNode([...document.getElementById('light-text').childNodes].find(n => n.nodeType === Node.TEXT_NODE));
    return range.getBoundingClientRect().height;
  });
  await expect.poll(height).toBe(0);
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
  // One taken out while it waits for the server: it stops trying, the others go on
  await page.evaluate(() => document.getElementById('showcase').remove());
  await page.waitForTimeout(2500); // longer than a retry period
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
  await context.addInitScript(() => {
    window.focus = () => document.documentElement.setAttribute('data-focused', 'yes'); // the server's focus, by its parent
  });
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
  await first.locator('.pony-ticket-pop-out').click(); // brings it to the front
  await expect(popup.locator('html')).toHaveAttribute('data-focused', 'yes');
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
  await popup.close(); // without its page's pagehide: its parent sees it closed
  await expect(first.locator('.pony-ticket-pop-out')).toHaveText('Pop out', { timeout: 5000 });
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

test('the legacy class names of an embedded application: on its container, not on the host page (ADR-0009)', async ({ page }) => {
  // The showcase runs without the legacy layer: its manifest is given one
  await page.route('**/sample/ponyapp.json', async route => {
    const response = await route.fetch();
    await route.fulfill({ response, json: { ...(await response.json()), legacy: 'ponyterminal/legacy.js' } });
  });
  await openPortal(page);
  await page.evaluate(() => {
    const app = document.createElement('pony-app');
    app.id = 'legacy';
    app.setAttribute('src', document.getElementById('ticket-1').getAttribute('src') + '#oracle-legacy');
    document.querySelector('.portal-app').append(app);
  });
  const app = page.locator('#legacy');
  const name = n => new RegExp(`(^|\\s)${n}(\\s|$)`);
  await expect(app.getByRole('button', { name: 'Plain' })).toHaveClass(name('gwt-Button'), { timeout: 15000 });
  const styled = app.getByRole('button', { name: 'Styled', exact: true });
  await expect(styled).toHaveClass(name('app-button')); // the server's style name in place of the primary name
  await expect(styled).not.toHaveClass(name('gwt-Button'));
  await expect(page.locator('html')).not.toHaveAttribute('data-pony-legacy');

  // Two in the host's document: its pointer and focus events listened to once
  await page.evaluate(() => {
    for (const id of ['legacy-a', 'legacy-b']) {
      const app = document.createElement('pony-app');
      app.id = id;
      app.setAttribute('isolation', 'none');
      app.setAttribute('src', document.getElementById('ticket-1').getAttribute('src') + '#oracle-legacy');
      document.querySelector('.portal-app').append(app);
    }
  });
  for (const id of ['legacy-a', 'legacy-b']) {
    await expect(page.locator(`#${id}`).getByRole('button', { name: 'Plain' })).toHaveClass(name('gwt-Button'), { timeout: 15000 });
  }
});

test('two PonySDK versions on a page: each application runs on its own version\'s terminal (ADR-0009)', async ({ page }) => {
  // Another version: the same files under another URL, which the application's manifest names as its terminal
  await page.route(/\/sample\/v2\/ponyterminal\//, async route => route.fulfill({
    response: await route.fetch({ url: route.request().url().replace('/sample/v2/ponyterminal/', '/sample/ponyterminal/') }) }));
  await page.route('**/sample/ponyapp.json', async route => {
    const response = await route.fetch();
    await route.fulfill({ response, json: { ...(await response.json()), terminal: 'v2/ponyterminal/terminal.js' } });
  });
  const requested = [];
  page.on('request', request => requested.push(new URL(request.url()).pathname));
  const { first } = await openPortal(page);
  expect(requested).toContain('/sample/ponyterminal/chunks/pony-app.js'); // the element, the page's terminal's
  expect(requested).toContain('/sample/v2/ponyterminal/chunks/pony-app.js'); // the application, its own terminal's

  await page.locator('#instrument').selectOption('GBPUSD'); // attributes and events, through the other terminal
  await expect(first.getByText('GBP / USD')).toBeVisible();
  await first.locator('.pony-ticket-buy').click();
  await expect(page.locator('#trades tr').first()).toContainText('BUY');

  // An addon of the application: registered by the page's ponysdk.js, created by the other terminal
  await page.evaluate(() => {
    const app = document.createElement('pony-app');
    app.setAttribute('src', document.getElementById('ticket-1').getAttribute('src') + '#binary-protocol');
    document.querySelector('.portal-app').append(app);
  });
  await expect.poll(() => page.evaluate(() => window.__createLen), { timeout: 15000 }).toBe(5);

  // Taken out of the page: the other terminal's application goes
  await page.evaluate(() => { window.removedTicket = document.getElementById('ticket-1'); window.removedTicket.remove(); });
  await expect.poll(() => page.evaluate(() => window.removedTicket.shadowRoot.querySelector('.pony-ticket') === null)).toBe(true);
});

test('taken out while the terminal of another version starts its application: that application stops', async ({ page }) => {
  await openPortal(page);
  let release = () => {};
  const held = new Promise(resolve => { release = resolve; });
  await page.route('**/sample/late/ponyapp.json', async route => {
    const response = await route.fetch({ url: route.request().url().replace('/late/', '/') });
    await route.fulfill({ response, json: { ...(await response.json()), terminal: '../v2/ponyterminal/terminal.js' } });
  });
  await page.route(/\/sample\/v2\/ponyterminal\//, async route => {
    if (/\/chunks\/pony-app\.js/.test(route.request().url())) await held; // mountPonyApp waits for it
    await route.fulfill({ response: await route.fetch({ url: route.request().url().replace('/sample/v2/ponyterminal/', '/sample/ponyterminal/') }) });
  });
  const sockets = [];
  page.on('websocket', ws => sockets.push(ws.url()));
  const requested = page.waitForRequest(/\/sample\/v2\/ponyterminal\/chunks\/pony-app\.js/);
  await embed(page, 'late', 'late/', { view: 'ticket' });
  await requested;
  await page.evaluate(() => document.getElementById('late').remove());
  release();
  await page.waitForTimeout(1000);
  expect(sockets.filter(url => url.includes('/late/'))).toEqual([]);
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

test("the portal's pages load an application from this machine only, whatever the query says", async ({ page }) => {
  const elsewhere = [];
  await page.route('**/*', route => {
    const host = new URL(route.request().url()).hostname;
    if (host === 'attacker.example') {
      elsewhere.push(route.request().url());
      return route.abort();
    }
    return route.continue();
  });
  const query = '?app=' + encodeURIComponent('https://attacker.example/');
  const index = new URL(PORTAL_URL);
  for (const path of ['portal.html', 'index.html']) {
    index.pathname = '/' + path;
    await page.goto(index.href + query);
    await page.waitForLoadState('load');
  }
  await page.waitForTimeout(500);
  expect(elsewhere).toEqual([]);
});

test('an attribute taken off the element reaches the application as removed', async ({ page }) => {
  const { second } = await openPortal(page);
  await page.locator('#ticket-2').evaluate(e => e.removeAttribute('instrument'));
  await expect(second.getByText('EUR / USD')).toBeVisible(); // the ticket's default instrument
});

test('an element whose credential function fails starts its application without one', async ({ page }) => {
  const errors = trackErrors(page);
  await openPortal(page);
  await page.evaluate(() => {
    const app = /** @type {any} */ (document.createElement('pony-app'));
    app.id = 'no-credential';
    app.setAttribute('src', document.getElementById('ticket-1').getAttribute('src'));
    app.setAttribute('view', 'ticket');
    app.credential = () => { throw new Error('no session'); };
    document.querySelector('.portal-app').append(app);
  });
  await expect(page.locator('#no-credential .pony-ticket-origin')).toContainText('no credential', { timeout: 15000 });
  expect(errors.some(e => e.includes('the credential function failed'))).toBe(true);
});

test('an element whose src is no application says it cannot start', async ({ page }) => {
  const errors = trackErrors(page);
  await openPortal(page);
  await page.evaluate(() => {
    const app = document.createElement('pony-app');
    app.setAttribute('src', new URL('/nowhere/', document.getElementById('ticket-1').getAttribute('src')).href);
    document.querySelector('.portal-app').append(app);
  });
  await expect.poll(() => errors.some(e => e.includes('cannot start'))).toBe(true);
});

test('the host page\'s window.pony is an addon registry: it sends nothing, the addons send through their terminal', async ({ page }) => {
  const warned = [];
  page.on('console', m => { if (m.type() === 'warning') warned.push(m.text()); });
  await openPortal(page);
  await page.evaluate(() => /** @type {any} */ (window).pony.sendDataToServer());
  await expect.poll(() => warned.some(w => w.includes('window.pony is the page\'s addon registry'))).toBe(true);
});

test('the shared sheets of the application\'s manifest go to the shadow roots of the page\'s web components', async ({ page }) => {
  await page.route('**/sample/ponyapp.json', async route => {
    const response = await route.fetch();
    await route.fulfill({ response, json: { ...(await response.json()), sharedSheets: ['css/shared.css'] } });
  });
  await openPortal(page);
  const sheets = await page.evaluate(() => {
    const element = document.createElement('div');
    const root = /** @type {any} */ (window).pony.wc.initShadow(element);
    return [...root.querySelectorAll('link')].map(link => link.href);
  });
  expect(sheets).toContain(new URL('css/shared.css', BASE_URL).href);
});

test('another site: a stylesheet its server serves without CORS loads as it is, not adapted to the element', async ({ page }) => {
  const warned = [];
  page.on('console', m => { if (m.type() === 'warning') warned.push(m.text()); });
  // The load with CORS fails as a refused one does: a network error (a route's own response would get CORS headers)
  await page.route(url => url.href === new URL('css/showcase.css', BASE_URL).href, async route =>
    (await route.request().headerValue('origin')) !== null ? route.abort() : route.continue());
  await page.goto(OTHER_SITE_PORTAL);
  const first = page.locator('#ticket-1');
  await expect(first.getByText('EUR / USD')).toBeVisible({ timeout: 15000 });
  await expect.poll(() => warned.some(w => /showcase\.css: refused with CORS, loaded as it is/.test(w))).toBe(true);
  const link = await page.locator('#ticket-1').evaluate(e => {
    const sheet = [...e.shadowRoot.querySelectorAll('link')].find(l => l.href.endsWith('css/showcase.css'));
    return sheet && { crossOrigin: sheet.crossOrigin, loaded: sheet.sheet !== null };
  });
  expect(link).toEqual({ crossOrigin: null, loaded: true });
});

test('another site: a stylesheet refused with CORS after its element left the page loads nothing more', async ({ page }) => {
  const warned = [];
  page.on('console', m => { if (m.type() === 'warning' && /refused with CORS/.test(m.text())) warned.push(m.text()); });
  let refuse = () => {};
  const refused = new Promise(resolve => { refuse = resolve; });
  let held = 0;
  await page.route(url => url.href === new URL('css/showcase.css', BASE_URL).href, async route => {
    if ((await route.request().headerValue('origin')) === null) return route.continue();
    held++;
    await refused;
    await route.abort();
  });
  await page.goto(OTHER_SITE_PORTAL);
  await expect.poll(() => held).toBeGreaterThan(0);
  await page.waitForTimeout(1000); // every element's request
  const others = await page.evaluate(() => {
    document.getElementById('ticket-1').remove();
    return [...document.querySelectorAll('pony-app')]
      .filter(e => [...(e.shadowRoot?.querySelectorAll('link') ?? [])].some(l => l.href.endsWith('css/showcase.css'))).length;
  });
  refuse();
  // The others load it as it is, the one gone no more
  await expect.poll(() => warned.length).toBe(others);
  await page.waitForTimeout(500);
  expect(warned.length).toBe(others);
});

/** A new <pony-app> element in the portal, of the application's URL plus suffix, or without src when suffix is null. */
async function embed(page, id, suffix, attributes = {}, credential = undefined, unslashed = false) {
  await page.evaluate(([id, suffix, attributes, credential, unslashed]) => {
    const app = /** @type {any} */ (document.createElement('pony-app'));
    app.id = id;
    const src = /** @type {any} */ (window).portalApp + suffix;
    if (suffix !== null) app.setAttribute('src', unslashed ? src.replace(/\/$/, '') : src);
    for (const [name, value] of Object.entries(attributes)) app.setAttribute(name, value);
    if (credential !== undefined) app.credential = () => credential;
    document.querySelector('.portal-app').append(app);
  }, [id, suffix, attributes, credential, unslashed]);
  return page.locator(`#${id}`);
}

test('in an element: a host event listened to then no longer, a number as an event\'s detail, a window titled before it is ready, a reload that starts the application again, menus and picker gone, an addon\'s own request', async ({ page, context }) => {
  await openPortal(page);
  const app = await embed(page, 'host', '#terminal-host');
  const status = app.getByLabel('Status');
  await expect(status).toHaveText('Status:', { timeout: 15000 });
  // A message of the host page's own origin: not one of the application's windows
  await page.evaluate(() => window.postMessage({ pony: 'ready' }, '*'));
  await app.evaluate(e => e.dispatchEvent(new CustomEvent('ping', { detail: 1 })));
  await expect(status).toHaveText('Status: ping 1');
  await app.getByRole('button', { name: 'Unlisten ping' }).click();
  await expect(status).toHaveText('Status: ping 1, unlistened');
  await app.evaluate(e => {
    e.dispatchEvent(new CustomEvent('ping', { detail: 2 })); // no longer listened to
    e.addEventListener('number', event => e.setAttribute('data-number', JSON.stringify(/** @type {CustomEvent} */ (event).detail)));
  });
  // An update the host's object does not know: skipped, the application goes on
  const skipped = page.waitForEvent('console', m => m.type() === 'warning' && /has no property FUNCTION_NAME/.test(m.text()));
  await app.getByRole('button', { name: 'Call on host' }).click();
  await skipped;
  await app.getByRole('button', { name: 'Send number' }).click();
  await expect(app).toHaveAttribute('data-number', '42'); // a number, not its text
  // An addon's own HTTP request, from another origin than the application's: the context's key goes with it
  await app.getByRole('button', { name: 'Ask' }).click();
  await expect(app.getByText('Asked', { exact: true })).toHaveAttribute('data-answer', 'answer to life');
  await expect(status).toHaveText('Status: ping 1, unlistened');

  const opened = context.waitForEvent('page');
  await app.getByRole('button', { name: 'Open titled' }).click();
  const child = await opened;
  await expect(child.getByText('Host child content')).toBeVisible({ timeout: 15000 });
  await expect(child).toHaveTitle('Titled at once'); // after the title its init gives
  await child.close();

  // Reloaded with a submenu and a date picker showing: the application goes with them
  await app.getByRole('menuitem', { name: 'Host menu' }).focus();
  await page.keyboard.press('Enter');
  await expect(app.getByRole('menuitem', { name: 'Host sub item' })).toBeVisible();
  await app.getByLabel('Host date').focus();
  const picker = app.locator('.pony-DateBoxPopup');
  await expect(picker).toBeVisible();
  await app.getByRole('button', { name: 'Reload' }).focus();
  await page.keyboard.press('Enter');
  await expect(status).toHaveText('Status:', { timeout: 15000 }); // another context
  await expect(app.getByRole('menuitem', { name: 'Host sub item' })).toBeHidden();
  await expect(picker).toBeHidden();
});

test('elements: one without src stays as it is; one whose src lacks its final slash, one whose credential is no text, one with history="hash"', async ({ page }) => {
  const errors = trackErrors(page);
  await openPortal(page);
  const empty = await embed(page, 'empty', null);
  // its src set before it is in the page: changed once there, the element restarts, and WebKit reports the socket it
  // closes while connecting as a console error (once in ten runs)
  const unslashed = await embed(page, 'unslashed', '', { view: 'ticket' }, 42, true);
  await expect(unslashed.getByText('EUR / USD')).toBeVisible({ timeout: 15000 });
  await expect(unslashed.locator('.pony-ticket-origin')).toContainText('no credential');
  await expect(empty).toHaveText('');

  const hashed = await embed(page, 'hashed', '#inputs', { history: 'hash' });
  await hashed.locator('.pony-tab', { hasText: 'Layouts' }).click({ timeout: 15000 });
  await expect(hashed.locator('.pony-crumb')).toContainText('Layouts');
  expect(new URL(page.url()).hash).not.toBe(''); // the host page's fragment: the application's history
  expect(errors).toEqual([]);
});

test('a manifest of an earlier version, without a terminal, with a stylesheet of another origin; a manifest refused; a terminal that runs no application', async ({ page }) => {
  const errors = trackErrors(page);
  await page.addInitScript(() => {
    const crossOrigin = /** @type {PropertyDescriptor} */ (Object.getOwnPropertyDescriptor(HTMLLinkElement.prototype, 'crossOrigin'));
    const recorded = /** @type {any} */ (window).crossOrigins = [];
    Object.defineProperty(HTMLLinkElement.prototype, 'crossOrigin', {
      configurable: true,
      get() { return crossOrigin.get?.call(this); },
      set(value) {
        if (this.href === '' && this.id === 'portal-sheet') recorded.push(value);
        crossOrigin.set?.call(this, value);
      },
    });
  });
  const portal = new URL(PORTAL_URL).origin;
  await page.route('**/refused/ponyapp.json', route => route.fulfill({ status: 503,
    headers: { 'Access-Control-Allow-Origin': portal, 'Access-Control-Allow-Credentials': 'true' } }));
  await page.route('**/sample/ponyapp.json', async route => {
    const response = await route.fetch();
    const manifest = await response.json();
    delete manifest.terminal; // an application of an earlier version runs on the element's terminal
    manifest.styles.push({ id: 'portal-sheet', href: `${portal}/portal.css` });
    await route.fulfill({ response, json: manifest });
  });
  await page.route('**/sample/other/ponyapp.json', async route => {
    const response = await route.fetch({ url: route.request().url().replace('/other/', '/') });
    await route.fulfill({ response, json: { ...(await response.json()), terminal: 'not-pony.js' } });
  });
  await page.route('**/sample/other/not-pony.js', route => route.fulfill({ contentType: 'text/javascript', body: 'export const nothing = 0;',
    headers: { 'Access-Control-Allow-Origin': '*' } }));
  await openPortal(page);
  await page.evaluate(() => {
    const app = document.createElement('pony-app');
    app.id = 'refused';
    app.setAttribute('src', new URL('/refused/', /** @type {any} */ (window).portalApp).href);
    document.querySelector('.portal-app').append(app);
  });
  await expect.poll(() => errors.some(e => /refused\/"> cannot start/.test(e))).toBe(true); // HTTP 503

  const first = page.locator('#ticket-1'); // its manifest names no terminal: it runs on the element's
  await expect(first.getByText('EUR / USD')).toBeVisible();
  // read with CORS, without credentials: another origin than the application's
  await expect.poll(() => page.evaluate(() => /** @type {any} */ (window).crossOrigins)).toContain('anonymous');

  await embed(page, 'other', 'other/', { view: 'ticket' });
  await expect.poll(() => errors.some(e => /other\/"> cannot start/.test(e))).toBe(true); // its terminal runs no application

  // Taken out while the terminal of another version loads: it runs nothing
  let release = () => {};
  const held = new Promise(resolve => { release = resolve; });
  await page.route('**/sample/slow/ponyapp.json', async route => {
    const response = await route.fetch({ url: route.request().url().replace('/slow/', '/') });
    await route.fulfill({ response, json: { ...(await response.json()), terminal: '../v2/ponyterminal/terminal.js' } });
  });
  await page.route(/\/sample\/v2\/ponyterminal\//, async route => {
    await held;
    await route.fulfill({ response: await route.fetch({ url: route.request().url().replace('/sample/v2/ponyterminal/', '/sample/ponyterminal/') }) });
  });
  const sockets = [];
  page.on('websocket', ws => sockets.push(ws.url()));
  const requested = page.waitForRequest(/\/sample\/v2\/ponyterminal\/terminal\.js/);
  await embed(page, 'slow', 'slow/', { view: 'ticket' });
  await requested;
  await page.evaluate(() => document.getElementById('slow').remove());
  release();
  await page.waitForTimeout(1000);
  expect(sockets.filter(url => url.includes('/slow/'))).toEqual([]);

  // An application whose ponysdk.js is not the page's: the page's serves it
  const warned = [];
  page.on('console', m => { if (m.type() === 'warning') warned.push(m.text()); });
  await page.route('**/sample/again/ponyapp.json', async route => {
    const response = await route.fetch({ url: route.request().url().replace('/again/', '/') });
    const { terminal, ...manifest } = await response.json(); // on the element's terminal
    await route.fulfill({ response, json: { ...manifest, scripts: [`${manifest.scripts[0]}?again`], styles: [], sharedSheets: [] } });
  });
  await embed(page, 'again', 'again/', { view: 'ticket' });
  await expect.poll(() => warned.some(w => /ponysdk\.js\?again not loaded: the page's ponysdk\.js serves/.test(w))).toBe(true);
  await page.evaluate(() => document.getElementById('again').remove());
});

test('stylesheets: one the application imports is adapted too, one of another origin it imports cannot be, a selector that cannot be one stays', async ({ page, browserName }) => {
  const warned = [];
  page.on('console', m => { if (m.type() === 'warning') warned.push(m.text()); });
  const portal = new URL(PORTAL_URL).origin;
  await page.route('**/sample/css/showcase.css', async route => {
    const response = await route.fetch();
    await route.fulfill({ response, body: `@import url("imported.css");\n@import url("${portal}/portal.css");\n`
      + await response.text() });
  });
  await page.route('**/sample/css/imported.css', route => route.fulfill({ contentType: 'text/css',
    body: 'html .pony-ticket-instrument { outline: 3px solid rgb(1, 2, 3); }\nhtml:has(.nothing) .nothing { color: red; }' }));
  // An element of the application's own origin: the sheets its sheet imports from there can be read
  await page.goto(BASE_URL + '#embedding');
  const ticket = page.locator('pony-app.pony-embedding-ticket');
  await expect(ticket.getByText('EUR / USD')).toBeVisible({ timeout: 15000 });
  await expect(ticket.locator('.pony-ticket-instrument')).toHaveCSS('outline-color', 'rgb(1, 2, 3)');
  // Chromium imports another origin's sheet without CORS, its rules hidden, and takes no :has() in :host(); Firefox
  // and WebKit import it with the CORS of the sheet that imports it, and take the selector
  if (browserName === 'chromium') {
    await expect.poll(() => warned.some(w => /portal\.css: its rules cannot be read/.test(w))).toBe(true);
    await expect.poll(() => warned.some(w => /":host\(:has\(\.nothing\)\) \.nothing" is no selector/.test(w))).toBe(true);
  }
});
