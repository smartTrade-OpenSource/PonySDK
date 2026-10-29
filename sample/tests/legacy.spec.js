// @ts-check
const { test, expect } = require('./coverage');

/*
 * The legacy layer (docs/decisions/0002-javascript-terminal.md, section 10): GWT's class names on the terminal's DOM.
 * With ApplicationConfiguration.setLegacyClassNames(true), BootstrapServlet loads legacy.js just before terminal.js
 * (BootstrapServletTest); here the served ponysdk.js gets a first line that writes that script element, so that the
 * showcase's own page loads legacy.js from its URL before terminal.js: a page the test would write itself counts as
 * outside the local network, and Chrome blocks its WebSocket to it.
 *
 *   ./gradlew :sample:runShowcase    then    BASE_URL=... npx playwright test legacy
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

async function open(page, oraclePage) {
    await page.route(url => url.href === BASE_URL + 'script/ponysdk.js', async route => {
        const response = await route.fetch();
        const legacy = 'document.write(\'<script type="text/javascript" src="ponyterminal/legacy.js"></script>\');\n';
        await route.fulfill({ response, body: legacy + await response.text() });
    });
    await page.goto(BASE_URL + '#oracle-' + oraclePage);
    await expect(page.locator('html')).toHaveAttribute('data-pony-legacy', '');
}

/** A class name, whole. */
const name = n => new RegExp(`(^|\\s)${n}(\\s|$)`);

test('primary names, the server\'s style name in place of one, and the disabled state', async ({ page }) => {
    await open(page, 'legacy');
    await expect(page.getByRole('button', { name: 'Plain' })).toHaveClass(name('gwt-Button'));
    const styled = page.getByRole('button', { name: 'Styled', exact: true });
    await expect(styled).toHaveClass(name('app-button'));
    await expect(styled).not.toHaveClass(name('gwt-Button'));
    const off = page.locator('.pony-CheckBox', { hasText: /^Off$/ });
    await expect(off).toHaveClass(name('gwt-CheckBox'));
    await expect(off).toHaveClass(name('gwt-CheckBox-disabled'));
    const styledOff = page.locator('.pony-CheckBox', { hasText: 'Styled off' });
    await expect(styledOff).toHaveClass(name('app-box-disabled'));
    await expect(styledOff).not.toHaveClass(name('gwt-CheckBox'));
    await expect(page.getByText('A label')).toHaveClass(name('gwt-Label'));
});

test('menu: orientation, the item under the pointer or open, the disabled one, the popup', async ({ page }) => {
    await open(page, 'menu');
    const item = text => page.getByRole('menuitem', { name: text, exact: true });
    await expect(page.getByRole('menubar')).toHaveClass(name('gwt-MenuBar-horizontal'));
    await item('File').hover();
    await expect(item('File')).toHaveClass(name('gwt-MenuItem-selected'));
    await item('File').click();
    await expect(page.locator('.gwt-MenuBarPopup').first()).toBeVisible();
    await expect(page.getByRole('menu').first()).toHaveClass(name('gwt-MenuBar-vertical'));
    await expect(item('Disabled')).toHaveClass(name('gwt-MenuItem-disabled'));
    await item('Help').hover(); // closes File's menu
    await expect(item('Help')).toHaveClass(name('gwt-MenuItem-selected'));
    await expect(item('File')).not.toHaveClass(name('gwt-MenuItem-selected'));
});

test('date picker: the parts and the days, as GWT named them', async ({ page }) => {
    await open(page, 'datepicker');
    const day = text => page.getByText(text, { exact: true });
    await expect(page.getByLabel('Oracle picker')).toHaveClass(name('gwt-DatePicker'));
    await expect(page.locator('.datePickerWeekendLabel')).toHaveCount(2);
    await expect(page.locator('.datePickerNextButton')).toHaveCount(1);
    await expect(day('17')).toHaveClass(name('oracle-holiday'));
    await expect(day('20')).toHaveClass(name('datePickerDayIsDisabled'));
    await day('12').click();
    await expect(day('12')).toHaveClass(name('datePickerDayIsValue'));
    await expect(day('12')).toHaveClass(name('datePickerDayIsValueAndHighlighted'));
    await day('13').hover();
    await expect(day('13')).toHaveClass(name('datePickerDayIsHighlighted'));
    await expect(day('12')).not.toHaveClass(name('datePickerDayIsHighlighted'));
});

test('tab layout panel: the selected tab', async ({ page }) => {
    await open(page, 'tablayout');
    const tab = text => page.getByRole('tab', { name: text });
    await expect(tab('TL 1')).toHaveClass(name('gwt-TabLayoutPanelTab-selected'));
    await expect(page.locator('.gwt-TabLayoutPanelTabs')).toHaveCount(1);
    await tab('TL 2').click();
    await expect(tab('TL 2')).toHaveClass(name('gwt-TabLayoutPanelTab-selected'));
    await expect(tab('TL 1')).not.toHaveClass(name('gwt-TabLayoutPanelTab-selected'));
});

test('tree: the selected item', async ({ page }) => {
    await open(page, 'tree');
    const fruits = page.getByText('Fruits', { exact: true });
    await expect(fruits).toHaveClass(name('gwt-TreeItem'));
    await fruits.click();
    await expect(fruits).toHaveClass(name('gwt-TreeItem-selected'));
});

test('disclosure panel: open and closed', async ({ page }) => {
    await open(page, 'disclosure');
    const panel = page.locator('.gwt-DisclosurePanel');
    await expect(panel).toHaveClass(name('gwt-DisclosurePanel-closed'));
    await page.getByText('Oracle details').click();
    await expect(panel).toHaveClass(name('gwt-DisclosurePanel-open'));
    await expect(page.getByText('Oracle details')).toHaveClass(name('header'));
});

test('loaded once the page is parsed, it decorates the document all the same', async ({ page }) => {
    await page.goto(BASE_URL + '#oracle-legacy');
    const plain = page.getByRole('button', { name: 'Plain' });
    await expect(plain).toBeVisible();
    await expect(plain).not.toHaveClass(name('gwt-Button'));
    await page.addScriptTag({ url: BASE_URL + 'ponyterminal/legacy.js' });
    await expect(page.locator('html')).toHaveAttribute('data-pony-legacy', '');
    await expect(plain).toHaveClass(name('gwt-Button'));
});
