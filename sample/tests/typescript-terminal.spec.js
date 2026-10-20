// @ts-check
const { test, expect } = require('@playwright/test');

/*
 * What the TypeScript terminal adds to the GWT one (docs/decisions/0002-javascript-terminal.md): keyboard and
 * accessibility of its native elements. Skipped when the page runs the GWT terminal.
 *
 *   ./gradlew :sample:runShowcase -Pterminal=typescript    then    BASE_URL=... npx playwright test typescript-terminal
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

async function open(page, oraclePage) {
    await page.goto(BASE_URL + '#oracle-' + oraclePage);
    const typescript = await page.locator('script[src$="ponyterminal/terminal.js"]').count() > 0;
    test.skip(!typescript, 'the GWT terminal does not have it');
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
