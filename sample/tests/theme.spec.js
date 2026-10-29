// @ts-check
// The default theme (docs/decisions/0024-default-theme.md): its tokens in both schemes, the contrast of its key pairs
// computed in the page, the focus ring, the scoping under a class, the Theme page of the showcase.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test theme
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
const PREVIEW = BASE_URL + '#theme-preview';
const STYLESHEET = new URL('style/pony-theme.css', BASE_URL).href;

/** Pairs of tokens and the least ratio WCAG 2.x AA asks: 4.5 for text, 3 for the borders and the ring of a control. */
const PAIRS = [
  ['text', 'bg', 4.5], ['text', 'surface', 4.5], ['text', 'surface-2', 4.5], ['text', 'accent-soft', 4.5],
  ['muted', 'bg', 4.5], ['muted', 'surface-2', 4.5], ['accent', 'bg', 4.5], ['accent', 'surface-2', 4.5],
  ['on-accent', 'accent', 4.5], ['on-accent', 'danger', 4.5],
  ['danger', 'bg', 4.5], ['danger', 'danger-soft', 4.5], ['success', 'bg', 4.5], ['success', 'success-soft', 4.5],
  ['warning', 'bg', 4.5], ['warning', 'warning-soft', 4.5], ['text', 'danger-soft', 4.5], ['text', 'warning-soft', 4.5],
  ['border-strong', 'bg', 3], ['border-strong', 'surface-2', 3], ['focus', 'bg', 3], ['focus', 'surface-2', 3],
];

/** In the page: the WCAG ratio of two tokens of an element, each resolved by the browser to rgb. */
const RATIOS = `(pairs) => {
  const root = document.documentElement;
  const probe = document.createElement('span');
  root.append(probe);
  const rgb = token => {
    probe.style.color = 'var(--pony-' + token + ')';
    return getComputedStyle(probe).color.match(/[\\d.]+/g).slice(0, 3).map(Number);
  };
  const luminance = ([r, g, b]) => [r, g, b].map(c => (c /= 255) <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4))
    .reduce((s, c, i) => s + c * [0.2126, 0.7152, 0.0722][i], 0);
  const out = pairs.map(([f, b, min]) => {
    const [lf, lb] = [luminance(rgb(f)), luminance(rgb(b))];
    return { pair: f + '/' + b, min, ratio: (Math.max(lf, lb) + 0.05) / (Math.min(lf, lb) + 0.05) };
  });
  probe.remove();
  return out;
}`;

const token = (page, name) => page.evaluate(n => getComputedStyle(document.documentElement).getPropertyValue(n).trim(), name);

async function openPreview(page, scheme) {
  await page.emulateMedia({ colorScheme: scheme });
  await page.goto(PREVIEW, { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.th-page .pony-Button').first()).toBeVisible({ timeout: 15000 });
  await expect(page.locator('link#pony-theme')).toHaveCount(1);
}

test('the tokens follow the system scheme, and [data-theme] overrides it', async ({ page }) => {
  await openPreview(page, 'light');
  expect(await token(page, '--pony-bg')).toBe('#ffffff');
  expect(await token(page, '--pony-accent')).toBe('#2563eb');
  expect(await token(page, '--pony-space-3')).toBe('12px');
  expect(await token(page, '--pony-font-family')).toContain('system-ui');
  expect(await page.evaluate(() => getComputedStyle(document.documentElement).colorScheme)).toBe('light');

  await page.emulateMedia({ colorScheme: 'dark' });
  expect(await token(page, '--pony-bg')).toBe('#0b1120');
  expect(await token(page, '--pony-accent')).toBe('#60a5fa');
  expect(await page.evaluate(() => getComputedStyle(document.documentElement).colorScheme)).toBe('dark');

  await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'light')); // forced, against the system
  expect(await token(page, '--pony-bg')).toBe('#ffffff');
  await page.emulateMedia({ colorScheme: 'light' });
  await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'dark'));
  expect(await token(page, '--pony-bg')).toBe('#0b1120');
});

for (const scheme of ['light', 'dark']) {
  test(`${scheme}: the key pairs of tokens meet WCAG AA`, async ({ page }) => {
    await openPreview(page, /** @type {'light'|'dark'} */ (scheme));
    const ratios = await page.evaluate(`(${RATIOS})(${JSON.stringify(PAIRS)})`);
    const below = ratios.filter(r => r.ratio < r.min).map(r => `${r.pair} ${r.ratio.toFixed(2)} < ${r.min}`);
    expect(below).toEqual([]);
    expect(ratios.length).toBe(PAIRS.length);
  });

  test(`${scheme}: the text of rendered widgets has AA contrast against its background`, async ({ page }) => {
    await openPreview(page, /** @type {'light'|'dark'} */ (scheme));
    const rendered = await page.evaluate(() => {
      const parse = c => c.match(/[\d.]+/g).map(Number);
      const lum = ([r, g, b]) => [r, g, b].map(c => (c /= 255) <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4))
        .reduce((s, c, i) => s + c * [0.2126, 0.7152, 0.0722][i], 0);
      const ratio = (a, b) => (Math.max(lum(a), lum(b)) + 0.05) / (Math.min(lum(a), lum(b)) + 0.05);
      /** The background that shows behind an element: its own, else its ancestors'. */
      const back = el => {
        for (let e = el; e; e = e.parentElement) {
          const c = parse(getComputedStyle(e).backgroundColor);
          if (c.length < 4 || c[3] > 0.99) return c.slice(0, 3);
        }
        return [255, 255, 255];
      };
      return ['.pony-Button:not(:disabled):not(.pony-primary):not(.pony-danger)', '.pony-Button.pony-primary', '.pony-Button.pony-danger',
        '.pony-TextBox', '.pony-TextArea', '.pony-ListBox', '.pony-CheckBox', '.pony-Anchor',
        '[role=tab][aria-selected=true]', '[role=tab][aria-selected=false]', '.pony-MenuItem', '.pony-TreeItem-content',
        '.pony-DatePicker-month', '.pony-grid-header', '.pony-grid-row', '.pony-DisclosurePanel > summary']
        .map(selector => {
          const el = /** @type {HTMLElement} */ (document.querySelector(selector));
          return { selector, ratio: el ? ratio(parse(getComputedStyle(el).color).slice(0, 3), back(el)) : -1 };
        });
    });
    expect(rendered.filter(r => r.ratio < 0).map(r => r.selector), 'every selector matches a widget').toEqual([]);
    expect(rendered.filter(r => r.ratio < 4.5).map(r => `${r.selector} ${r.ratio.toFixed(2)}`)).toEqual([]);
  });
}

test('the keyboard focus draws the ring, and a press of the mouse does not', async ({ page }) => {
  await openPreview(page, 'light');
  await page.keyboard.press('Tab');
  const ring = await page.evaluate(() => {
    const s = getComputedStyle(document.activeElement);
    return { style: s.outlineStyle, width: s.outlineWidth, color: s.outlineColor, offset: s.outlineOffset, cls: document.activeElement.className };
  });
  expect(ring).toMatchObject({ style: 'solid', width: '2px', color: 'rgb(37, 99, 235)', offset: '2px' });
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => getComputedStyle(document.activeElement).outlineStyle)).toBe('solid');

  const field = page.locator('.pony-TextBox').first();
  await field.focus();
  await expect.poll(() => field.evaluate(e => getComputedStyle(e).borderTopColor)).toBe('rgb(37, 99, 235)'); // the focused field, once its transition ends
  await page.setViewportSize({ width: 1100, height: 900 });
  await page.locator('.pony-Button', { hasText: 'Default' }).click();
  expect(await page.locator('.pony-Button', { hasText: 'Default' }).evaluate(e => getComputedStyle(e).outlineStyle)).toBe('none');
});

test('states come from the ARIA attributes and the classes of the terminal', async ({ page }) => {
  await openPreview(page, 'light');
  const computed = (selector, ...props) => page.locator(selector).first().evaluate((e, p) => {
    const s = getComputedStyle(e);
    return p.map(n => s.getPropertyValue(n));
  }, props);
  // aria-invalid: the border takes the danger colour
  expect(await computed('.pony-TextBox[aria-invalid=true]', 'border-top-color')).toEqual(['rgb(185, 28, 28)']);
  // disabled: not the normal text colour
  expect(await computed('.pony-TextBox:disabled', 'cursor')).toEqual(['not-allowed']);
  // aria-selected: accent text and underline
  expect(await computed('[role=tab][aria-selected=true]', 'color', 'border-bottom-color')).toEqual(['rgb(37, 99, 235)', 'rgb(37, 99, 235)']);
  // aria-busy: a spinner after the label
  expect(await computed('.pony-Button[aria-busy=true]', 'cursor')).toEqual(['progress']);
  expect(await page.locator('.pony-Button[aria-busy=true]').first().evaluate(e => getComputedStyle(e, '::after').animationName)).toBe('pony-spin');
  // aria-expanded: the chevron of a stack header turns
  expect(await page.locator('.pony-StackLayoutPanel-header[aria-expanded=true]').first()
    .evaluate(e => getComputedStyle(e, '::before').transform)).not.toBe('none');
  expect(await page.locator('.pony-StackLayoutPanel-header[aria-expanded=false]').first()
    .evaluate(e => getComputedStyle(e, '::before').transform)).toBe('none');
  // a selected tree item and today in the date picker
  expect(await computed('.pony-TreeItem[aria-selected=true] > .pony-TreeItem-row', 'background-color')).toEqual(['rgb(219, 234, 254)']);
  expect((await computed('.pony-DatePicker-day[aria-current=date]', 'box-shadow'))[0]).not.toBe('none');
});

test('prefers-reduced-motion stops the transitions and the spinner', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto(PREVIEW, { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.pony-Button[aria-busy=true]').first()).toBeVisible({ timeout: 15000 });
  expect(await token(page, '--pony-duration')).toBe('0s');
  expect(await page.locator('.pony-Button[aria-busy=true]').first().evaluate(e => getComputedStyle(e, '::after').animationName)).toBe('none');
  expect(await page.locator('.pony-Button').first().evaluate(e => getComputedStyle(e).transitionDuration.split(', ')))
    .toEqual(['0s', '0s']); // background-color and border-color
});

test('a dialog, a modal backdrop and a popover take the theme, and only a modal dialog dims the page', async ({ page }) => {
  await openPreview(page, 'light');
  await page.getByRole('button', { name: 'Dialog', exact: true }).click();
  const dialog = page.locator('dialog.pony-DialogBox');
  await expect(dialog).toBeVisible();
  expect(await dialog.evaluate(e => {
    const s = getComputedStyle(e);
    return { background: s.backgroundColor, radius: s.borderTopLeftRadius, shadow: s.boxShadow !== 'none' };
  })).toEqual({ background: 'rgb(255, 255, 255)', radius: '10px', shadow: true });
  expect(await dialog.locator('.pony-Caption').evaluate(e => getComputedStyle(e).backgroundColor)).toBe('rgb(241, 245, 249)');
  expect(await page.locator('.pony-Glass').evaluate(e => getComputedStyle(e).backgroundColor)).toBe('rgba(15, 23, 42, 0.45)');
  await dialog.getByRole('button', { name: 'Close' }).click();

  await page.getByText('File', { exact: true }).click();
  const popover = page.locator('.pony-MenuPopup').first();
  await expect(popover).toBeVisible();
  expect(await popover.evaluate(e => getComputedStyle(e).backgroundColor)).toBe('rgb(255, 255, 255)');
  expect(await popover.evaluate(e => getComputedStyle(e, '::backdrop').backgroundColor)).toBe('rgba(0, 0, 0, 0)');
});

test('the theme scopes under a class, and an application rule of one class wins over it', async ({ page }) => {
  // On the server's origin (a page of another origin would need the stylesheet's CORS): its stylesheet's own URL, rebuilt
  await page.goto(STYLESHEET);
  await page.evaluate(([href]) => {
    document.head.innerHTML = `<link rel="stylesheet" href="${href}">`;
    document.body.innerHTML = `
      <div id="scope" class="pony-theme"><button class="pony-Button">In</button><label class="pony-CheckBox"><input type="checkbox"><span>c</span></label></div>
      <button id="outside" class="pony-Button">Out</button>`;
  }, [new URL(STYLESHEET).pathname]);
  await page.waitForFunction(() => getComputedStyle(document.getElementById('scope')).getPropertyValue('--pony-accent') !== '');
  const read = (selector, prop) => page.locator(selector).first().evaluate((e, p) => getComputedStyle(e).getPropertyValue(p), prop);
  expect(await read('#scope .pony-Button', 'min-height')).toBe('32px');
  expect(await read('#scope', 'background-color')).toBe('rgb(255, 255, 255)');
  expect(await read('#outside', 'min-height')).not.toBe('32px'); // outside the container the theme does nothing
  expect(await page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--pony-accent'))).toBe('');

  await page.evaluate(() => document.getElementById('scope').setAttribute('data-theme', 'dark'));
  expect(await read('#scope', 'background-color')).toBe('rgb(11, 17, 32)');
  await expect.poll(() => read('#scope .pony-Button', 'background-color')).toBe('rgb(17, 24, 39)'); // after its transition

  await page.addStyleTag({ content: '.pony-Button { border-radius: 20px; --pony-accent: #7c3aed; }' });
  expect(await read('#scope .pony-Button', 'border-top-left-radius')).toBe('20px');
  expect(await read('#scope', 'background-color')).toBe('rgb(11, 17, 32)');
});

test('the Theme page: the scheme buttons set the frame, the token table lists the stylesheet', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'light' });
  await page.goto(BASE_URL + '#theme', { waitUntil: 'domcontentloaded' });
  const frame = page.frameLocator('.th-frame');
  await expect(frame.locator('.pony-Button').first()).toBeVisible({ timeout: 15000 });
  const frameRoot = () => page.frames().find(f => f !== page.mainFrame() && f.url().includes('theme-preview'));
  const background = () => frameRoot().evaluate(() => getComputedStyle(document.documentElement).backgroundColor);
  expect(await background()).toBe('rgb(255, 255, 255)');
  await page.getByRole('button', { name: 'Dark', exact: true }).click();
  await expect.poll(background).toBe('rgb(11, 17, 32)');
  await page.getByRole('button', { name: 'Light', exact: true }).click();
  await expect.poll(background).toBe('rgb(255, 255, 255)');
  await page.getByRole('button', { name: 'System', exact: true }).click();
  await expect.poll(() => frameRoot().evaluate(() => document.documentElement.hasAttribute('data-theme'))).toBe(false);

  const tokens = page.locator('.th-tokens');
  await expect(tokens.locator('.th-token-name', { hasText: '--pony-accent' }).first()).toBeVisible();
  await expect(tokens.locator('.th-token', { hasText: '--pony-bg' }).first()).toContainText('#0b1120');
  // the showcase's own look is untouched by the frame
  expect(await page.locator('.pony-header').evaluate(e => getComputedStyle(e).position)).toBe('sticky');
});
