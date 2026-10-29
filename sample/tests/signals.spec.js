// @ts-check
// Signals (docs/decisions/0019-signals.md): the showcase's Signals page. A feed thread sets signals, the widgets bound
// to them follow once per flush; a text box is bound both ways; a list keeps the widget of a key.
//   ./gradlew :sample:runShowcase    then    BASE_URL='http://localhost:8081/sample/' npx playwright test signals
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

async function signalsPage(page) {
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => {
    if (m.type() === 'error' && !/favicon\.ico$/.test(m.location().url || '')) errors.push('console: ' + m.text());
  });
  await page.goto(BASE_URL + '#signals');
  await expect(page.locator('.pony-page-title', { hasText: 'Signals' })).toBeVisible({ timeout: 10000 });
  return errors;
}

// The terminal sends a text box's value on change: Tab
async function type(box, text) {
  await box.fill(text);
  await box.press('Tab');
}

const number = async locator => Number((await locator.innerText()).replace(/[,\s]/g, ''));

test('the feed sets far more signals than the screen shows, and the prices move', async ({ page }) => {
  const errors = await signalsPage(page);
  const price = page.locator('.stream-board .stream-price').first();
  const first = await price.innerText();
  const values = page.locator('.perf-grid .perf-value');

  await expect(price).not.toHaveText(first, { timeout: 10000 });
  await expect.poll(async () => {
    const sets = await number(values.nth(0));
    const shown = await number(values.nth(1));
    return sets > 5_000 && shown > 0 && shown < sets / 5;
  }, { timeout: 10000 }).toBe(true);
  expect(await number(values.nth(2))).toBeGreaterThan(80);
  expect(errors).toEqual([]);
});

test('a price takes the rising style while its bid rises', async ({ page }) => {
  await signalsPage(page);
  const prices = page.locator('.stream-board .stream-price');

  await expect.poll(async () => (await prices.evaluateAll(l => l.map(e => e.classList.contains('signal-up')))).join(), { timeout: 10000 })
    .toMatch(/true/);
  await expect.poll(async () => (await prices.evaluateAll(l => l.map(e => e.classList.contains('signal-up')))).join(), { timeout: 10000 })
    .toMatch(/false/);
});

test('the text box and its signal follow each other, the bindings on the signal with them', async ({ page }) => {
  const errors = await signalsPage(page);
  const box = page.locator('.signal-quantity');
  const held = page.locator('.signal-held');
  const invalid = page.locator('.signal-invalid');
  const reset = page.getByRole('button', { name: 'Back to 100' });
  const total = page.locator('.signal-total');

  await expect(box).toHaveValue('100');
  await expect(held).toHaveText('The signal holds "100"');
  await expect(invalid).toBeHidden();
  await expect(reset).toBeDisabled();
  await expect(total).not.toHaveText('—');

  // The user types: the signal holds it, and what is bound to it follows
  await type(box, '250');
  await expect(held).toHaveText('The signal holds "250"');
  await expect(reset).toBeEnabled();
  await type(box, 'abc');
  await expect(invalid).toBeVisible();
  await expect(total).toHaveText('—');
  await type(box, '7');
  await expect(invalid).toBeHidden();
  await expect(total).not.toHaveText('—');

  // The signal is set by the page: the box follows
  await reset.click();
  await expect(box).toHaveValue('100');
  await expect(held).toHaveText('The signal holds "100"');
  await expect(reset).toBeDisabled();
  expect(errors).toEqual([]);
});

test('the quantity multiplies the total', async ({ page }) => {
  await signalsPage(page);
  const box = page.locator('.signal-quantity');
  const total = page.locator('.signal-total');
  await expect(total).not.toHaveText('—');

  // 1 and 1 000: the bids move by a few thousandths, the ratio is that of the quantities
  await type(box, '1');
  await expect.poll(async () => number(total), { timeout: 5000 }).toBeLessThan(20);
  const one = await number(total);
  await type(box, '1000');
  await expect.poll(async () => number(total), { timeout: 5000 }).toBeGreaterThan(one * 900);
  expect(await number(total)).toBeLessThan(one * 1100);
});

test('a list keeps the widget of a key: moved, not made again', async ({ page }) => {
  const errors = await signalsPage(page);
  const rows = page.locator('.signal-row');
  const names = () => rows.locator('.signal-row-name').allInnerTexts();
  const made = page.locator('.signal-made');
  await expect(rows).toHaveCount(3);
  expect(await names()).toEqual(['EURUSD', 'USDJPY', 'GBPUSD']);
  await expect(made).toHaveText('3 rows on screen, 3 made');
  // a mark on the first row's element: the same element, if the widget is kept
  await rows.first().evaluate(e => { e['__kept'] = true; });

  await page.getByRole('button', { name: 'Reverse' }).click();
  await expect(rows.first().locator('.signal-row-name')).toHaveText('GBPUSD');
  expect(await names()).toEqual(['GBPUSD', 'USDJPY', 'EURUSD']);
  await expect(made).toHaveText('3 rows on screen, 3 made');
  expect(await rows.last().evaluate(e => e['__kept'] === true)).toBe(true);

  await page.getByRole('button', { name: 'Add a row' }).click();
  await expect(rows).toHaveCount(4);
  expect(await names()).toEqual(['GBPUSD', 'USDJPY', 'EURUSD', 'AUDUSD']);
  await expect(made).toHaveText('4 rows on screen, 4 made');

  await page.getByRole('button', { name: 'Remove the first' }).click();
  await expect(rows).toHaveCount(3);
  expect(await names()).toEqual(['USDJPY', 'EURUSD', 'AUDUSD']);
  await expect(made).toHaveText('3 rows on screen, 4 made');
  expect(await rows.nth(1).evaluate(e => e['__kept'] === true)).toBe(true);

  // the rows follow the signals of their instrument
  const price = rows.first().locator('.signal-price');
  const before = await price.innerText();
  await expect(price).not.toHaveText(before, { timeout: 5000 });
  expect(errors).toEqual([]);
});

test('the code of the page is cut from its source', async ({ page }) => {
  await signalsPage(page);
  await page.getByRole('button', { name: 'Show the code: the ticker and its feed' }).click();
  const code = page.locator('.code-body:visible');
  await expect(code).toContainText('Signal.computed');
  await expect(code).toContainText('Bind.styleName');
  await page.getByRole('button', { name: 'Show the code: the list' }).click();
  await expect(page.locator('.code-body:visible').last()).toContainText('Bind.children');
});
