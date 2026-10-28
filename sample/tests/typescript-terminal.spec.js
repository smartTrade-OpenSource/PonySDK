// @ts-check
const { test, expect } = require('./coverage');

/*
 * What the TypeScript terminal added to the GWT one it replaced (docs/decisions/0002-javascript-terminal.md): keyboard
 * and accessibility of its native elements, which the oracle, recorded with GWT, does not cover.
 *
 *   ./gradlew :sample:runShowcase    then    BASE_URL=... npx playwright test typescript-terminal
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

async function open(page, oraclePage) {
    await page.goto(BASE_URL + '#oracle-' + oraclePage);
}

test('tabs: the arrow keys, Home and End move the selection, which goes to the server', async ({ page }) => {
    const sent = [];
    page.on('websocket', ws => ws.on('framesent', frame => sent.push(String(frame.payload))));
    await open(page, 'tabpanel');
    const tabs = page.getByRole('tab');
    await expect(tabs).toHaveText(['Tab 1', 'Tab 2', 'Tab 3']);
    await tabs.nth(0).focus();
    await page.keyboard.press('ArrowRight');
    await expect(tabs.nth(1)).toHaveAttribute('aria-selected', 'true');
    await expect(tabs.nth(1)).toBeFocused();
    await expect(page.getByRole('tabpanel')).toHaveText('Second panel');
    await page.keyboard.press('End');
    await expect(tabs.nth(2)).toHaveAttribute('aria-selected', 'true');
    await page.keyboard.press('ArrowRight'); // wraps
    await expect(tabs.nth(0)).toHaveAttribute('aria-selected', 'true');
    // HANDLER_SELECTION ("I") for each move
    await expect.poll(() => sent.filter(m => /"I":\d/.test(m)).map(m => /"I":(\d)/.exec(m)[1])).toEqual(['1', '2', '0']);
});

test('a disabled button is disabled for the browser, not hidden from it', async ({ page }) => {
    await open(page, 'button-busy');
    const button = page.getByRole('button', { name: 'Oracle button' });
    await button.click();
    await expect(button).toBeDisabled(); // until the server has processed the click (300 ms)
    await expect(button).not.toHaveAttribute('aria-hidden', 'true');
    await expect(button).toBeEnabled();
});

test('suggest box: Up and Down move in the list, Enter takes the suggestion', async ({ page }) => {
    const sent = [];
    page.on('websocket', ws => ws.on('framesent', frame => sent.push(String(frame.payload))));
    await open(page, 'suggest');
    const box = page.getByRole('combobox', { name: 'Oracle suggest' });
    await box.click();
    await box.pressSequentially('ap', { delay: 30 });
    await expect(page.getByRole('option')).toHaveText(['Apple', 'Apricot', 'Red apple']);
    await expect(box).toHaveAttribute('aria-expanded', 'true');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowUp');
    await expect(page.getByRole('option', { name: 'Apricot' })).toHaveAttribute('aria-selected', 'true');
    await page.keyboard.press('Enter');
    await expect(box).toHaveValue('Apricot');
    await expect(box).toHaveAttribute('aria-expanded', 'false');
    // HANDLER_STRING_SELECTION ("K") with the display string
    await expect.poll(() => sent.some(m => m.includes('"K":"<strong>Ap</strong>ricot"'))).toBe(true);
});

test('menu: the arrows move in the bar and open a submenu, Escape closes it', async ({ page }) => {
    await open(page, 'menu');
    const item = name => page.getByRole('menuitem', { name, exact: true });
    await item('File').focus();
    await page.keyboard.press('ArrowRight');
    await expect(item('Edit')).toBeFocused();
    await page.keyboard.press('ArrowLeft');
    await page.keyboard.press('ArrowDown');
    await expect(item('New')).toBeVisible();
    await expect(item('New')).toBeFocused();
    await expect(item('File')).toHaveAttribute('aria-expanded', 'true');
    await page.keyboard.press('Escape');
    await expect(item('New')).toBeHidden();
    await expect(item('File')).toBeFocused();
});

test('tree: the arrows select, open and close, and the server is told', async ({ page }) => {
    const sent = [];
    page.on('websocket', ws => ws.on('framesent', frame => sent.push(String(frame.payload))));
    await open(page, 'tree');
    const item = name => page.getByRole('treeitem', { name, exact: true });
    await page.getByText('Fruits', { exact: true }).click();
    await page.keyboard.press('ArrowRight'); // opens
    await expect(page.getByText('Apple', { exact: true })).toBeVisible();
    await page.keyboard.press('ArrowRight'); // to the first child
    await page.keyboard.press('ArrowDown');
    await expect(page.getByText('Banana', { exact: true }).locator('xpath=ancestor::li[1]')).toHaveAttribute('aria-selected', 'true');
    await page.keyboard.press('ArrowLeft'); // to the parent
    await page.keyboard.press('ArrowLeft'); // closes it
    await expect(page.getByText('Apple', { exact: true })).toBeHidden();
    // HANDLER_OPEN ("G"), then HANDLER_CLOSE ("F"), with the item's id
    await expect.poll(() => sent.filter(m => /"[GF]":\d/.test(m)).length).toBe(2);
});

test('date picker: the arrows and the pages move through the days, Enter picks one', async ({ page }) => {
    const sent = [];
    page.on('websocket', ws => ws.on('framesent', frame => sent.push(String(frame.payload))));
    await open(page, 'datepicker');
    const cell = name => page.getByRole('gridcell', { name, exact: true });
    await expect(page.getByRole('grid', { name: 'March 2026' })).toBeVisible();
    await expect(cell('Sunday 1 March 2026')).toHaveAttribute('tabindex', '0'); // the first of the month
    await cell('Sunday 1 March 2026').focus();
    await page.keyboard.press('ArrowRight');
    await page.keyboard.press('ArrowDown');
    await expect(cell('Monday 9 March 2026')).toBeFocused();
    await page.keyboard.press('PageDown');
    await expect(page.getByRole('grid', { name: 'April 2026' })).toBeVisible();
    await expect(cell('Thursday 9 April 2026')).toBeFocused();
    await page.keyboard.press('Home');
    await page.keyboard.press('Enter');
    await expect(cell('Monday 6 April 2026')).toHaveAttribute('aria-selected', 'true');
    await expect(page.getByText('Picked: 2026-04-06')).toBeVisible();
    // HANDLER_SHOW_RANGE ("J") of April, from Page Down
    expect(sent.some(m => m.includes('"q":"2026-03-30","r":"2026-05-10"'))).toBe(true);
});

test('date box: Down goes into the picker, Enter picks the day, Escape comes back', async ({ page }) => {
    await open(page, 'datebox');
    const box = page.getByRole('combobox', { name: 'Oracle date' });
    await box.focus(); // opens the picker, as GWT
    await expect(box).toHaveAttribute('aria-expanded', 'true');
    await page.keyboard.press('ArrowDown');
    await expect(page.locator(':focus')).toHaveAttribute('role', 'gridcell');
    await page.keyboard.press('ArrowRight');
    await page.keyboard.press('Enter');
    await expect(box).toBeFocused();
    await expect(box).toHaveAttribute('aria-expanded', 'false');
    await expect(box).toHaveValue(/^\d\d\/03\/2026$/);
    await expect(page.getByText(/^Date: \d\d\/03\/2026$/)).toBeVisible();
    await page.keyboard.press('ArrowDown'); // opens
    await page.keyboard.press('ArrowDown'); // goes in
    await page.keyboard.press('Escape');
    await expect(box).toBeFocused();
    await expect(box).toHaveAttribute('aria-expanded', 'false');
});

test('rich text: the toolbar formats the selection and shows its state', async ({ page }) => {
    const sent = [];
    page.on('websocket', ws => ws.on('framesent', frame => sent.push(String(frame.payload))));
    await open(page, 'richtext');
    const area = page.getByRole('textbox', { name: 'Oracle rich text' });
    await area.click();
    await page.keyboard.type('Hello');
    await page.keyboard.press('ControlOrMeta+a');
    const bold = page.getByRole('button', { name: 'Toggle Bold' });
    await bold.click();
    await expect(bold).toHaveAttribute('aria-pressed', 'true');
    await expect(area).toBeFocused(); // a press on the toolbar keeps the focus in the area
    await page.getByText('HTML: none').click();
    await expect(page.getByText('HTML: <b>Hello</b>')).toBeVisible();
});

test('mask: a character that does not fit is refused, the mask\'s own is typed over', async ({ page }) => {
    await open(page, 'mask');
    const box = page.getByRole('textbox', { name: 'Oracle mask' });
    await box.click();
    await box.pressSequentially('1x2/0', { delay: 30 });
    await expect(box).toHaveValue('12/0_/____');
    await expect(page.getByText('Mask: 12/0_/____')).toBeVisible();

    await box.press('End');
    await box.press('3'); // past the mask: refused
    await expect(box).toHaveValue('12/0_/____');
    await box.press('Home');
    await box.press('Delete'); // the next characters move back, the mask's own stay
    await expect(box).toHaveValue('20/__/____');
    await box.press('Backspace'); // nothing before the cursor
    await expect(box).toHaveValue('20/__/____');
    await box.press('ControlOrMeta+a');
    await box.press('Backspace'); // the selection cut
    await expect(box).toHaveValue('__/__/____');
    await expect(page.getByText('Mask: __/__/____')).toBeVisible();
});

test('protocol 4: a DOM event by name, debounced in the browser, with its fields', async ({ page }) => {
    const sent = [];
    page.on('websocket', ws => ws.on('framesent', frame => sent.push(String(frame.payload))));
    await open(page, 'events');
    await page.getByLabel('Events box').pressSequentially('hello', { delay: 20 });
    await expect(page.getByText('Input: hello (1)')).toBeVisible(); // five keys, one event
    // EVENT ("Z") with EVENT_FIELDS ("z")
    expect(sent.filter(m => m.includes('"Z":"input"'))).toEqual([expect.stringContaining('"z":{"target.value":"hello"}')]);
});

test('protocol 4: a handler the server removed is no longer sent', async ({ page }) => {
    const sent = [];
    page.on('websocket', ws => ws.on('framesent', frame => sent.push(String(frame.payload))));
    await open(page, 'events');
    const scroll = page.getByLabel('Events scroll');
    await scroll.evaluate(e => e.scrollTo(0, 100));
    await expect(page.getByText('Scrolls: 1')).toBeVisible();
    await page.getByRole('button', { name: 'Stop scrolls' }).click();
    await page.waitForTimeout(500); // the click reaches the server, the removal the terminal
    const before = sent.filter(m => m.includes('"M":')).length; // HANDLER_SCROLL
    await scroll.evaluate(e => e.scrollTo(0, 300));
    await page.waitForTimeout(500);
    expect(sent.filter(m => m.includes('"M":')).length).toBe(before);
    await expect(page.getByText('Scrolls: 1')).toBeVisible();
});
