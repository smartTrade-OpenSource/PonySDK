// @ts-check
// The Forms page (docs/decisions/0021-forms-binder.md): an order ticket bound to a bean, its rules the bean's
// annotations (Bean Validation). An error is aria-invalid on the field, aria-describedby naming a message right after
// it; keyboard and screen reader get the same.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test forms
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/** Collects page errors + console errors; the protocol reader throws on what the server writes wrong. */
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

async function open(page) {
  await page.goto(BASE_URL + '#forms', { waitUntil: 'domcontentloaded' });
  await expect(page.getByLabel('Quantity')).toBeVisible({ timeout: 10000 });
}

/** Types in a text field and leaves it with Tab: the value reaches the server on change. */
async function enter(field, text) {
  await field.fill(text);
  await field.press('Tab');
}

const saved = page => page.locator('.forms-saved');
const problems = page => page.locator('.forms-problems');
const status = page => page.locator('.forms-status');

/** The message element a field points at with aria-describedby: it must be the field's next sibling. */
async function messageOf(field) {
  await expect(field).toHaveAttribute('aria-describedby', /.+/);
  const id = await field.getAttribute('aria-describedby');
  const message = field.locator('xpath=following-sibling::*[1]');
  await expect(message).toHaveAttribute('id', id);
  await expect(message).toHaveAttribute('role', 'alert');
  return message;
}

test.describe('Forms', () => {

  test('a field says what is wrong with it, and clears when it is right', async ({ page }) => {
    const errors = trackErrors(page);
    await open(page);
    const quantity = page.getByLabel('Quantity');
    await expect(quantity).not.toHaveAttribute('aria-invalid', /.*/); // nothing is said before it is wrong

    await enter(quantity, 'abc'); // a conversion error
    await expect(quantity).toHaveAttribute('aria-invalid', 'true');
    await expect(await messageOf(quantity)).toHaveText('Not a whole number');
    await expect(quantity).toHaveAccessibleDescription('Not a whole number'); // what a screen reader reads

    await enter(quantity, '0'); // a constraint of the bean
    await expect(await messageOf(quantity)).toHaveText('At least 1');
    await enter(quantity, '2000000');
    await expect(await messageOf(quantity)).toHaveText('At most 1 000 000');

    await enter(quantity, '50');
    await expect(quantity).not.toHaveAttribute('aria-invalid', /.*/);
    await expect(quantity).not.toHaveAttribute('aria-describedby', /.*/);
    await expect(quantity.locator('xpath=following-sibling::*[1]')).toBeHidden();
    await expect(quantity).toHaveAccessibleName('Quantity');

    const instrument = page.getByLabel('Instrument');
    await enter(instrument, 'eur');
    await expect(await messageOf(instrument)).toHaveText('Six capital letters, like EURUSD');
    await enter(instrument, 'GBPUSD');
    await expect(instrument).not.toHaveAttribute('aria-invalid', /.*/);
    expect(errors).toEqual([]);
  });

  test('the keyboard reaches everything, and the message is not another stop', async ({ page }) => {
    await open(page);
    const quantity = page.getByLabel('Quantity');
    await quantity.focus();
    await page.keyboard.type('x');
    await page.keyboard.press('Tab'); // the next stop is the limit price, not the message between them
    await expect(page.getByLabel('Limit price')).toBeFocused();
    await expect(quantity).toHaveAttribute('aria-invalid', 'true');

    await quantity.fill('10');
    await page.keyboard.press('Tab');
    await expect(quantity).not.toHaveAttribute('aria-invalid', /.*/);

    // Save by keyboard: Enter on the focused button
    await page.getByRole('button', { name: 'Save' }).focus();
    await page.keyboard.press('Enter');
    await expect(problems(page)).toContainText('Some fields are not valid'); // the side is not chosen
    await expect(page.getByLabel('Side')).toHaveAttribute('aria-invalid', 'true');
  });

  test('a list box, a date box and a check box say it too', async ({ page }) => {
    await open(page);
    const side = page.getByLabel('Side');
    await side.selectOption({ label: 'Buy' });
    await side.selectOption({ index: 0 }); // the empty item: @NotNull
    await expect(side).toHaveAttribute('aria-invalid', 'true');
    await expect(await messageOf(side)).toHaveText('Choose a side');
    await side.selectOption({ label: 'Sell' });
    await expect(side).not.toHaveAttribute('aria-invalid', /.*/);

    // The date box's terminal drops aria-invalid when it parses its text: the server says it again
    const date = page.getByLabel('Value date (yyyy-MM-dd)');
    await enter(date, '2020-01-01');
    await expect(date).toHaveAttribute('aria-invalid', 'true');
    await expect(await messageOf(date)).toHaveText('The value date is in the past');
    await enter(date, '2019-01-01');
    await expect(date).toHaveAttribute('aria-invalid', 'true');
    await enter(date, '2030-01-01');
    await expect(date).not.toHaveAttribute('aria-invalid', /.*/);

    // A check box's root is a label: its attributes are on its input
    const confirmed = page.getByRole('checkbox', { name: 'I confirm the details' });
    await confirmed.check();
    await confirmed.uncheck();
    await expect(confirmed).toHaveAttribute('aria-invalid', 'true');
    await expect(confirmed).toHaveAccessibleDescription('Confirm the details to send the order');
    await confirmed.check();
    await expect(confirmed).not.toHaveAttribute('aria-invalid', /.*/);
    await expect(confirmed).not.toHaveAttribute('aria-describedby', /.*/);
  });

  test('Save writes the bean only when everything is valid, and the rule across fields is the bean\'s', async ({ page }) => {
    const errors = trackErrors(page);
    await open(page);
    await expect(saved(page)).toHaveText('Nothing saved yet.');
    await expect(status(page)).toHaveText('No changes');

    await page.getByLabel('Side').selectOption({ label: 'Buy' });
    await page.getByRole('checkbox', { name: 'I confirm the details' }).check();
    await expect(status(page)).toHaveText('Unsaved changes');
    await enter(page.getByLabel('Quantity'), 'abc');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(saved(page)).toHaveText('Nothing saved yet.'); // nothing is written while a field is wrong
    await expect(problems(page)).toContainText('Some fields are not valid');
    await enter(page.getByLabel('Quantity'), '250');

    await page.getByLabel('Type').selectOption({ label: 'Limit' });
    await page.getByRole('button', { name: 'Save' }).click(); // a limit order without a price
    await expect(problems(page)).toHaveText('A limit order needs a limit price');
    await expect(saved(page)).toHaveText('Nothing saved yet.');
    await expect(page.getByLabel('Limit price')).not.toHaveAttribute('aria-invalid', /.*/); // the bean's error

    await enter(page.getByLabel('Limit price'), '1.0850');
    await expect(problems(page)).toHaveText(''); // an edit makes it stale
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(saved(page)).toContainText('Saved: LIMIT BUY 250 EURUSD @ 1.0850');
    await expect(status(page)).toHaveText('No changes');
    await expect(page.getByLabel('Quantity')).not.toHaveAttribute('aria-invalid', /.*/);

    // Reset shows the saved bean again, without an error
    await enter(page.getByLabel('Quantity'), 'x');
    await expect(page.getByLabel('Quantity')).toHaveAttribute('aria-invalid', 'true');
    await page.getByRole('button', { name: 'Reset' }).click();
    await expect(page.getByLabel('Quantity')).toHaveValue('250');
    await expect(page.getByLabel('Quantity')).not.toHaveAttribute('aria-invalid', /.*/);
    await expect(status(page)).toHaveText('No changes');
    expect(errors).toEqual([]);
  });

  test('the code panels are the running source', async ({ page }) => {
    await open(page);
    await page.getByRole('button', { name: /Show the code: the bean and its constraints/ }).click();
    await expect(page.locator('.code-body').first()).toContainText('@Pattern(regexp = "[A-Z]{6}"');
    await expect(page.locator('.code-body').first()).toContainText('LimitNeedsPrice');
    await page.getByRole('button', { name: /Show the code: the fields and their binding/ }).click();
    await expect(page.locator('.pony-content')).toContainText('new Binder<>(OrderTicket.class)');
  });
});
