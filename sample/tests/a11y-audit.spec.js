// @ts-check
// An accessibility audit of every widget the showcase shows (docs/guide/accessibility.md, "The audit"): axe-core, with the
// WCAG 2.1 A and AA rules, on each tab and on the themed widgets (#theme-preview, in both colour schemes). A violation
// that is not in ALLOWED fails the test; the allow-list says, for each entry, why it stays.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test a11y-audit
const { test, expect } = require('./coverage');
const AxeBuilder = require('@axe-core/playwright').default;

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

/** The tabs, by the token of their URL (UISampleEntryPoint). */
const TABS = ['overview', 'code', 'inputs', 'forms', 'layouts', 'data', 'datagrid', 'web-components', 'flexlayout',
  'charts', 'misc', 'theme', 'performance', 'streams', 'signals', 'component-costs', 'binary-protocol', 'reconnection',
  'embedding', 'fdc3', 'routing'];

/**
 * What stays, each with its reason. An entry is a rule and a regular expression on the HTML of the node axe reports;
 * nothing else is accepted.
 * @type {{ rule: string, html: RegExp, why: string }[]}
 */
const ALLOWED = [
  {
    rule: 'scrollable-region-focusable',
    html: /class="[^"]*pony-grid-(body|unpinned-(header|body|footer))[ "]/,
    why: 'The scrolled parts of a DataGrid2 are scroll containers that must not be tab stops: Chrome and Firefox make a '
      + 'scroller without a tabbable child one (the grid is one tab stop, a cell, ADR-0025), and tabindex="-1" stops that. '
      + 'axe then finds no tabbable content in the region. The keyboard reaches all of it: the arrows scroll the active '
      + 'cell into view (datagrid-a11y.spec.js).',
  },
  {
    rule: 'aria-required-children',
    html: /^<div class="pony-grid[ "]/,
    why: 'For the same tabindex="-1": axe takes the focusable scrolled parts, between the grid and its rows, for children '
      + 'that are not rows. The tree is right (the rows own their cells with aria-owns; datagrid-a11y.spec.js reads '
      + 'Chrome\'s accessibility tree: every row with its 53 cells in order), and ARIA 1.2 lets generic elements sit '
      + 'between the two.',
  },
];

/** Whether a node of a violation is one of the allowed. */
function allowed(rule, node) {
  return ALLOWED.some(a => a.rule === rule && a.html.test(node.html));
}

/**
 * Runs axe on the page as it is. Returns the violations as one line per rule and node, the report of the test; the
 * ones not allowed are the failures.
 */
async function audit(page, name) {
  const results = await new AxeBuilder({ page }).withTags(TAGS).analyze();
  const counts = results.violations.map(v => {
    const stays = v.nodes.filter(node => allowed(v.id, node)).length;
    return `${v.id}: ${v.nodes.length}${stays ? ` (${stays} allowed)` : ''}`;
  });
  if (counts.length) console.log(`[axe] ${name}: ${counts.join(', ')}`);
  const failures = [];
  for (const v of results.violations) {
    for (const node of v.nodes) {
      if (!allowed(v.id, node)) failures.push(`${v.id} (${v.impact}) ${node.target.join(' ')}: ${node.failureSummary?.split('\n').slice(1).join(' ').trim()}`);
    }
  }
  return failures;
}

/** The tab is open and has drawn: its content is there, and the schedulers it starts have pushed a first frame. */
async function openTab(page, slug) {
  await page.goto(BASE_URL + '#' + slug, { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.pony-tab-active')).toBeVisible({ timeout: 15000 });
  await expect.poll(async () => (await page.locator('.pony-content').innerText()).trim().length, { timeout: 15000 })
    .toBeGreaterThan(0);
  if (slug === 'datagrid') await expect(page.getByRole('grid', { name: 'Sample data' }).getByRole('gridcell').first()).toBeVisible({ timeout: 20000 });
  await page.waitForTimeout(800);
}

for (const slug of TABS) {
  test(`axe: the ${slug} tab has no violation of WCAG 2.1 A and AA`, async ({ page }) => {
    test.setTimeout(60000);
    await openTab(page, slug);
    expect(await audit(page, slug)).toEqual([]);
  });
}

for (const scheme of ['light', 'dark']) {
  test(`axe: the themed widgets (#theme-preview, ${scheme}) have no violation of WCAG 2.1 A and AA`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: /** @type {'light' | 'dark'} */ (scheme) });
    await page.goto(BASE_URL + '#theme-preview', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.th-page .pony-Button').first()).toBeVisible({ timeout: 15000 });
    await page.waitForTimeout(800);
    expect(await audit(page, 'theme-preview-' + scheme)).toEqual([]);
  });
}

// A tab is a tab: it holds no button (nested-interactive), and the keyboard closes it as the mouse does
test('a closable border tab of the FlexLayout holds no control, and Delete closes it', async ({ page }) => {
  await page.goto(BASE_URL + '#flexlayout', { waitUntil: 'domcontentloaded' });
  const closable = page.locator('.fl-sidebar-tab:has(.fl-sidebar-tab-x)');
  await expect(closable.first()).toBeVisible({ timeout: 15000 });
  expect(await page.locator('.fl-sidebar-tab button').count()).toBe(0);

  const tabs = page.locator('.fl-sidebar-tab');
  const count = await tabs.count();
  await closable.first().focus();
  await page.keyboard.press('Delete');
  await expect(tabs).toHaveCount(count - 1);
});

// A name for what has no text: a screen reader says which of the lists is which
test('the lists of a twin list box are named by its captions', async ({ page }) => {
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('listbox', { name: 'Available' })).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole('listbox', { name: 'Selected' })).toHaveCount(1);
});

// WCAG 3.1.1: the page says its language
test('the page has a language', async ({ page }) => {
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
  // the showcase supports locales: the visitor's (Playwright says en-US) gives en-GB; without them, setLanguage's "en"
  await expect(page.locator('html')).toHaveAttribute('lang', /^en(-[A-Z]{2})?$/);
});

// The chevron of a drop-down list is the main button's twin: hidden, so a screen reader hears one button
test('a drop-down list is one button for a screen reader', async ({ page }) => {
  await page.goto(BASE_URL + '#theme-preview', { waitUntil: 'domcontentloaded' });
  const dropDown = page.locator('.dd-container-widget').first();
  await expect(dropDown).toBeVisible({ timeout: 15000 });
  await expect(dropDown.locator('.dd-container-state')).toHaveAttribute('aria-hidden', 'true');
  await expect(dropDown.getByRole('button')).toHaveCount(1);
});
