// @ts-check
// Web Awesome (docs/decisions/0028-web-awesome.md): the library's elements, driven from Java, under the Content-Security-Policy
// of the page, from this application's origin and on its page alone; the order ticket's fields bound with the Binder.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test webawesome
const { test, expect } = require('./coverage');
const AxeBuilder = require('@axe-core/playwright').default;

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

/** Page errors, console errors and the page's CSP violations, from its first script on. */
async function track(page) {
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('requestfailed', r => errors.push('requestfailed: ' + r.url() + ' ' + r.failure()?.errorText));
  page.on('response', r => { if (r.status() >= 400 && !/favicon\.ico$/.test(r.url())) errors.push('response ' + r.status() + ': ' + r.url()); });
  page.on('console', m => {
    if (m.type() !== 'error') return; // not the warnings of the browser: Firefox's "scroll-linked positioning effect" is the sticky header's
    if (/favicon\.ico$/.test(m.location().url || '')) return; // the sample ships no favicon
    errors.push('console: ' + m.text());
  });
  await page.addInitScript(() => {
    window.__violations = [];
    document.addEventListener('securitypolicyviolation', e => window.__violations.push(e.violatedDirective + ' ' + e.blockedURI));
  });
  return errors;
}

async function open(page, errors) {
  await page.goto(BASE_URL + '#web-awesome', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('wa-input').first()).toBeVisible({ timeout: 15000 });
  // Its style sheets and modules are loaded: Escape, which a dialog takes to close, also stops what a page is still loading (WebKit)
  await page.waitForLoadState('networkidle');
  // The library's elements are defined once their modules are loaded
  await page.waitForFunction(() => ['wa-button', 'wa-input', 'wa-select', 'wa-dialog', 'wa-tab-group', 'wa-tree', 'wa-slider']
    .every(tag => customElements.get(tag)), null, { timeout: 15000 });
  // WebKit loads the showcase's first page twice (a reload at start): what the first document had in flight is cancelled
  if (errors) errors.length = 0;
}

const status = (page, text) => page.locator('.wad-status', { hasText: text });
const ticket = page => page.locator('.wad-ticket');
const violations = page => page.evaluate(() => /** @type {any} */ (window).__violations);

test.describe('Web Awesome', () => {

  test('the library is loaded by its page alone, from the application\'s own origin', async ({ page }) => {
    const errors = await track(page);
    const urls = [];
    page.on('request', r => urls.push(r.url()));
    await page.goto(BASE_URL + '#forms', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.forms-card')).toBeVisible({ timeout: 15000 });
    await page.waitForTimeout(500);
    expect(urls.filter(u => /webawesome\/|pony-webawesome/.test(u)), 'a page that does not use it loads nothing of it').toEqual([]);
    expect(await page.evaluate(() => customElements.get('wa-button'))).toBeUndefined();

    await page.locator('.pony-tab', { hasText: 'Web Awesome' }).click();
    await expect(page.locator('wa-button').first()).toBeVisible({ timeout: 15000 });
    await page.waitForFunction(() => customElements.get('wa-button') && customElements.get('wa-input'));
    const library = urls.filter(u => /\/script\/webawesome\//.test(u));
    expect(library.length).toBeGreaterThan(10);
    // Each component is a module of its own, fetched when one of its tags is in the page: the 70 are not
    expect(library.filter(u => /\/components\/[\w-]+\/[\w-]+\.js$/.test(u)).length).toBeLessThan(40);
    const origin = new URL(BASE_URL).origin;
    expect(urls.filter(u => /^https?:/.test(u) && !u.startsWith(origin)), 'no CDN, no font, no icon service').toEqual([]);
    expect(errors).toEqual([]);
    expect(await violations(page)).toEqual([]);
  });

  test('the elements render, and nothing the library does is blocked by the policy', async ({ page }) => {
    const errors = await track(page);
    const response = await page.goto(BASE_URL + '#web-awesome', { waitUntil: 'domcontentloaded' });
    const policy = response.headers()['content-security-policy'];
    expect(policy).toMatch(/^script-src 'self' 'sha256-[A-Za-z0-9+/=]+';/);
    expect(policy).not.toContain('unsafe-inline');
    expect(policy).not.toContain('unsafe-eval');
    await open(page, errors);

    // Shadow roots, adopted style sheets, and the library's inline SVG icons (the system library: data: images)
    const rendered = await page.evaluate(() => ['wa-button', 'wa-input', 'wa-select', 'wa-tab-group', 'wa-slider', 'wa-details']
      .map(tag => [tag, !!document.querySelector(tag)?.shadowRoot?.querySelector('[part]')]));
    expect(rendered.filter(([, ok]) => !ok)).toEqual([]);
    const button = page.locator('wa-button', { hasText: 'brand' });
    expect(await button.evaluate(b => getComputedStyle(b.shadowRoot.querySelector('[part~=base]')).backgroundColor)).not.toBe('rgba(0, 0, 0, 0)');

    // Things that use more than styles: an overlay in the top layer, a popup, a toast, a tooltip
    await page.locator('wa-button', { hasText: 'Open the dialog' }).click();
    await expect(page.locator('wa-dialog').locator('dialog')).toBeVisible();
    await page.keyboard.press('Escape');
    await page.locator('wa-button', { hasText: 'Show a toast' }).click();
    await expect(page.locator('wa-toast-item')).toContainText('Order sent');
    await page.locator('wa-button', { hasText: 'Hover for a tooltip' }).hover();
    await expect.poll(() => page.locator('.wad-row > wa-tooltip').evaluate(t => /** @type {any} */ (t).open)).toBe(true);
    await ticket(page).locator('wa-select').click();
    await expect(page.locator('wa-option', { hasText: 'Buy' })).toBeVisible();
    await page.keyboard.press('Escape');

    expect(await violations(page)).toEqual([]);
    expect(errors).toEqual([]);
  });

  test('buttons, a dialog, a drawer, tabs, a tree, a split panel: the events reach the server', async ({ page }) => {
    const errors = await track(page);
    await open(page, errors);

    await page.locator('wa-button', { hasText: 'brand' }).click();
    await expect(status(page, 'Server: clicked brand')).toBeVisible();

    // The browser changes `open` itself: the dialog opens a second time, which a cached property would skip
    for (let i = 0; i < 2; i++) {
      await page.locator('wa-button', { hasText: 'Open the dialog' }).click();
      await expect.poll(() => page.locator('wa-dialog').evaluate(d => /** @type {any} */ (d).open)).toBe(true);
      await page.keyboard.press('Escape');
      await expect(status(page, 'Server: the dialog is closed')).toBeVisible();
      await expect.poll(() => page.locator('wa-dialog').evaluate(d => /** @type {any} */ (d).open)).toBe(false);
      await page.evaluate(() => { document.querySelector('.wad-status').textContent = ''; }); // the next close must say it again
    }
    await page.locator('wa-button', { hasText: 'Open the drawer' }).click();
    await expect.poll(() => page.locator('wa-drawer').evaluate(d => /** @type {any} */ (d).open)).toBe(true);
    await page.keyboard.press('Escape');
    await expect(status(page, 'Server: the drawer is closed')).toBeVisible();

    await page.locator('wa-tab', { hasText: 'Risk' }).click();
    await expect(status(page, 'Active tab: risk')).toBeVisible();
    await expect(page.locator('wa-tab-panel[name=risk]')).toContainText('The risk of the desk');

    await page.locator('#fx-forward').click();
    await expect(status(page, 'Selected: fx-forward')).toBeVisible();

    await page.evaluate(() => {
      const split = /** @type {any} */ (document.querySelector('wa-split-panel'));
      split.position = 60;
      split.dispatchEvent(new CustomEvent('wa-reposition'));
    });
    await expect(status(page, 'Divider at 60 %')).toBeVisible();

    await page.locator('wa-tag').locator('[part~=remove-button]').click();
    await expect(status(page, 'Server: tag removed')).toBeVisible();

    await page.locator('wa-details summary').click();
    await expect.poll(() => page.locator('wa-details').evaluate(d => /** @type {any} */ (d).open)).toBe(true);
    expect(errors).toEqual([]);
  });

  test('a slider and a rating are fields: the server reads them on change and writes the progress back', async ({ page }) => {
    const errors = await track(page);
    await open(page, errors);
    const thumb = page.locator('wa-slider [part~=thumb]');
    await thumb.scrollIntoViewIfNeeded();
    const box = await thumb.boundingBox();
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
    await page.mouse.down();
    await page.mouse.move(box.x + 100, box.y + box.height / 2, { steps: 5 });
    await page.mouse.up();
    await expect(status(page, 'Server: fill ratio')).toBeVisible();
    const ratio = await page.locator('wa-slider').evaluate(s => /** @type {any} */ (s).value);
    expect(ratio).not.toBe(40);
    await expect.poll(() => page.locator('wa-progress-bar').evaluate(b => /** @type {any} */ (b).value)).toBe(ratio);
    await expect.poll(() => page.locator('wa-progress-ring').evaluate(b => /** @type {any} */ (b).value)).toBe(ratio);

    await page.locator('wa-rating').evaluate(r => r.scrollIntoView({ block: 'center' })); // the sticky header
    const star = await page.locator('wa-rating .symbol').nth(3).boundingBox(); // the library listens on the row of symbols
    await page.mouse.click(star.x + star.width / 2, star.y + star.height / 2);
    await expect(status(page, '4 star')).toBeVisible();
    expect(errors).toEqual([]);
  });

  test('the order ticket: the Binder shows each error on its field, and writes the bean when all are valid', async ({ page }) => {
    const errors = await track(page);
    await open(page, errors);
    const form = ticket(page);
    const inputs = form.locator('wa-input');
    const instrument = inputs.nth(0);

    await instrument.locator('input').fill('abc');
    await instrument.locator('input').press('Tab');
    await expect(form.locator('.pony-FieldError', { hasText: 'Six capital letters, like EURUSD' })).toBeVisible();
    await expect(instrument).toHaveAttribute('aria-invalid', 'true');
    // The library draws it on the control: the danger colour of the theme, through the bridge's token (its border transitions)
    const danger = await form.evaluate(f => {
      const probe = document.createElement('i');
      probe.style.color = 'var(--pony-danger)';
      f.append(probe);
      const color = getComputedStyle(probe).color;
      probe.remove();
      return color;
    });
    await expect.poll(() => instrument.evaluate(i => getComputedStyle(i.shadowRoot.querySelector('[part~=base]')).borderTopColor)).toBe(danger);
    expect(await form.locator('.pony-FieldError').first().getAttribute('role')).toBe('alert');

    await page.locator('wa-button', { hasText: 'Save' }).click();
    await expect(form.locator('.pony-FieldError', { hasText: 'Choose a side' })).toBeVisible();
    await expect(form.locator('.pony-FieldError', { hasText: 'Confirm the details to send the order' })).toBeVisible();
    await expect(form.locator('.forms-problems')).toHaveText('Some fields are not valid: fix the ones marked.');
    await expect(page.locator('.forms-saved')).toHaveText('Nothing saved yet.');

    // Fix every field, by the keyboard and the mouse as a user would
    await instrument.locator('input').fill('GBPUSD');
    await instrument.locator('input').press('Tab');
    await expect(instrument).not.toHaveAttribute('aria-invalid', 'true');
    await form.locator('wa-select').click();
    await page.locator('wa-option', { hasText: 'Sell' }).click();
    await form.locator('wa-radio', { hasText: 'Limit' }).click();
    await inputs.nth(2).locator('input').fill('1.0725');
    await inputs.nth(2).locator('input').press('Tab');
    await form.locator('wa-checkbox').click();
    await page.locator('wa-button', { hasText: 'Save' }).click();
    await expect(page.locator('.forms-saved')).toContainText('Saved: LIMIT SELL 1000 GBPUSD @ 1.0725');
    await expect(form.locator('.pony-FieldError:visible')).toHaveCount(0);

    // A value the user typed over the one the server wrote: Reset writes it again, though it is the same text
    await instrument.locator('input').fill('XXXXXX');
    await instrument.locator('input').press('Tab');
    await page.locator('wa-button', { hasText: 'Reset' }).click();
    await expect.poll(() => instrument.evaluate(i => /** @type {any} */ (i).value)).toBe('GBPUSD');
    expect(errors).toEqual([]);
    expect(await violations(page)).toEqual([]);
  });

  test('the look follows the default theme: the tokens, light and dark', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'light' });
    const errors = await track(page);
    await open(page, errors);
    const read = () => page.locator('.wad-root').evaluate(root => {
      const style = getComputedStyle(root);
      const input = root.querySelector('wa-input');
      const base = getComputedStyle(input.shadowRoot.querySelector('[part~=base]'));
      return {
        pony: style.getPropertyValue('--pony-surface').trim(), wa: style.getPropertyValue('--wa-color-surface-default').trim(),
        control: base.backgroundColor, text: getComputedStyle(input).getPropertyValue('--wa-form-control-value-color').trim(),
        accent: style.getPropertyValue('--wa-color-brand-fill-loud').trim(),
      };
    });
    const light = await read();
    expect(light.wa).toBe(light.pony);
    expect(light.control).toBe('rgb(255, 255, 255)');

    await page.emulateMedia({ colorScheme: 'dark' });
    const dark = await read();
    expect(dark.pony).not.toBe(light.pony);
    expect(dark.wa).toBe(dark.pony);
    // --pony-surface in the dark scheme: the library's controls changed with it (its background transitions)
    await expect.poll(async () => (await read()).control).toBe('rgb(17, 24, 39)');

    // Forced on the container, whatever the system says: the buttons set data-theme with no round trip (ADR-0027)
    await page.getByRole('button', { name: 'Light' }).click();
    await expect.poll(async () => (await read()).control).toBe('rgb(255, 255, 255)');
    await page.getByRole('button', { name: 'Dark' }).click();
    await expect.poll(async () => (await read()).control).toBe('rgb(17, 24, 39)');
    await page.getByRole('button', { name: 'System' }).click();
    await expect(page.locator('.wad-root')).not.toHaveAttribute('data-theme', /.*/);
    expect(errors).toEqual([]);
  });

  for (const scheme of /** @type {const} */ (['light', 'dark'])) {
    test(`axe: the page has no violation of WCAG 2.1 A and AA (${scheme}), with errors shown`, async ({ browser }) => {
      const context = await browser.newContext({ colorScheme: scheme, viewport: { width: 1280, height: 900 } });
      const page = await context.newPage();
      await open(page);
      await page.locator('wa-button', { hasText: 'Save' }).click();
      await expect(ticket(page).locator('.pony-FieldError').first()).toBeVisible();
      await page.waitForTimeout(500);
      const results = await new AxeBuilder({ page }).withTags(TAGS).analyze();
      expect(results.violations.map(v => `${v.id}: ${v.nodes.map(n => n.target.join(' ')).join(' ; ')}`)).toEqual([]);
      await context.close();
    });
  }
});
