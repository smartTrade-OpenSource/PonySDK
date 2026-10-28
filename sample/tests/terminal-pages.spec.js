// @ts-check
const { test, expect } = require('./coverage');

/*
 * The terminal's features that the showcase's pages do not show, on the pages of TerminalPages.java
 * (#terminal-<name>): each page shows in its status label what reached the server.
 *
 *   ./gradlew :sample:runShowcase    then    BASE_URL=... npx playwright test terminal-pages
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

async function open(page, name) {
    await page.goto(BASE_URL + '#terminal-' + name);
    await expect(page.getByLabel('Status')).toBeVisible();
}

const button = (page, name) => page.getByRole('button', { name, exact: true });
const status = page => page.getByLabel('Status');

/** The JSON messages the page sends to the server. */
function sent(page) {
    const messages = [];
    page.on('websocket', ws => ws.on('framesent', frame => messages.push(String(frame.payload))));
    return messages;
}

test('window: the server opens a window, then titles, sizes, moves, prints, focuses and closes it', async ({ page, context }) => {
    await context.addInitScript(() => {
        window.print = () => document.documentElement.setAttribute('data-printed', 'yes');
    });
    await open(page, 'window');
    await expect(page).toHaveTitle('Terminal window page');
    const [child] = await Promise.all([page.waitForEvent('popup'), button(page, 'Open window').click()]);
    await expect(status(page)).toHaveText('Status: opened');
    await expect(child.getByText('Child content')).toBeVisible();
    await expect(child).toHaveTitle('terminal-child'); // the window's name until the server titles it

    await button(page, 'Title').click();
    await expect(child).toHaveTitle('Child title');
    await button(page, 'Print').click();
    await expect(child.locator('html')).toHaveAttribute('data-printed', 'yes');
    await button(page, 'Resize to').click();
    await expect.poll(() => child.evaluate(() => window.outerWidth)).toBe(420);
    await button(page, 'Resize by').click();
    await expect.poll(() => child.evaluate(() => window.outerWidth)).toBe(440);
    await button(page, 'Move').click();
    await button(page, 'Focus').click();

    await Promise.all([child.waitForEvent('close'), button(page, 'Close').click()]);
    await expect(status(page)).toHaveText('Status: opened, closed');
});

test('window: one that navigates away is closed for the server', async ({ page }) => {
    await open(page, 'window');
    const [child] = await Promise.all([page.waitForEvent('popup'), button(page, 'Open window').click()]);
    await expect(status(page)).toHaveText('Status: opened');
    await button(page, 'Navigate').click();
    await expect(child).toHaveURL('about:blank');
    await expect(status(page)).toHaveText('Status: opened, closed');
});

test('window: a window the browser blocks is destroyed and reported', async ({ page }) => {
    const messages = sent(page);
    await open(page, 'window');
    await page.evaluate(() => {
        window.open = () => null;
    });
    await button(page, 'Open window').click();
    // HANDLER_DESTROY ("V"), then WARN_MSG ("7")
    await expect.poll(() => messages.some(m => m.includes('"V"'))).toBe(true);
    await expect.poll(() => messages.some(m => /"7":"Can't open PTWindow #\d+/.test(m))).toBe(true);
    await expect(status(page)).toHaveText('Status:');
});

test('widget: mouse, wheel, context menu and focus events reach the server; prevented and stopped events', async ({ page }) => {
    await open(page, 'widget');
    const target = page.getByLabel('Target', { exact: true });
    await target.hover();
    await expect(status(page)).toContainText(/\bover$/);
    await page.mouse.wheel(0, 120);
    await expect(status(page)).toContainText('wheel 1');
    await target.click({ button: 'right' });
    await expect(status(page)).toContainText('down BUTTON_RIGHT');
    await expect(status(page)).toContainText('menu');
    await expect(status(page)).toContainText('up');
    await button(page, 'Title').hover();
    await expect(status(page)).toContainText(/, out$/);

    // ONCONTEXTMENU prevented, ONMOUSEUP stopped at the target
    const effects = await target.evaluate(element => {
        let reached = false;
        document.addEventListener('mouseup', () => reached = true, { once: true });
        element.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
        const notPrevented = element.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, cancelable: true }));
        return { reached, notPrevented };
    });
    expect(effects).toEqual({ reached: false, notPrevented: false });

    // a key down handler filtered to Enter
    const keys = page.getByLabel('Enter only');
    await button(page, 'Focus box').click();
    await expect(keys).toBeFocused();
    await expect(status(page)).toContainText(', focus');
    await page.keyboard.press('a');
    await page.keyboard.press('Enter');
    await expect(status(page)).toContainText('focus, key 13');
    await button(page, 'Blur box').click();
    await expect(keys).not.toBeFocused();
    await button(page, 'Focus quietly').click();
    await expect(keys).toBeFocused();
    await expect(status(page)).not.toContainText('key 65');
});

test('widget: drag and drop tells the server its source', async ({ page }) => {
    await open(page, 'widget');
    await page.getByText('Drag source').dragTo(page.getByText('Drop zone'));
    await expect(status(page)).toContainText('drag start');
    await expect(status(page)).toContainText('drag enter');
    await expect(status(page)).toContainText('drop Drag source');
    await expect(status(page)).toContainText('drag end');
    await expect(status(page)).not.toContainText('drag over'); // as the GWT terminal: no drag over handler
});

test('widget: title, styles, full size, native binding, AJAX and terminal data', async ({ page }) => {
    await open(page, 'widget');
    const target = page.getByLabel('Target', { exact: true });
    await button(page, 'Title').click();
    await expect(target).toHaveAttribute('title', 'A tip');
    await button(page, 'Untitle').click();
    await expect(target).not.toHaveAttribute('title');

    const styled = page.getByText('Styled', { exact: true });
    await expect(styled).toHaveClass(/\bfirst second\b/);
    await button(page, 'Unstyle').click();
    await expect(styled).not.toHaveClass(/\bfirst\b/);
    await expect(styled).toHaveClass(/\bpony-Label\b/);
    await button(page, 'Full size').click();
    await expect(styled).toHaveAttribute('style', /width: 100%; height: 100%/);

    await button(page, 'Native').click();
    await expect(page.getByText('Native: hello')).toBeVisible();

    const answer = await page.evaluate(() => new Promise(resolve => {
        const ajax = /** @type {HTMLElement} */ (document.querySelector('[aria-label="Ajax"]'));
        /** @type {any} */ (window).pony.sendDataToServer(ajax.id, { question: 'life' }, resolve);
    }));
    expect(answer).toBe('answer to life');
    await page.evaluate(() => {
        const ajax = /** @type {HTMLElement} */ (document.querySelector('[aria-label="Ajax"]'));
        /** @type {any} */ (window).pony.request(ajax.id, { word: 'hi' });
    });
    await expect(status(page)).toContainText('native hi');
});

test('widget: a removed visibility handler and event listener no longer report', async ({ page }) => {
    await open(page, 'widget');
    const watched = page.getByText('Watched', { exact: true });
    await expect(status(page)).toContainText('visible');
    await watched.click();
    await expect(status(page)).toContainText('listened');
    await button(page, 'Unwatch').click();
    await watched.click();
    await button(page, 'Title').click(); // a round trip after the click
    await expect(page.getByLabel('Target', { exact: true })).toHaveAttribute('title', 'A tip');
    await expect(status(page)).not.toContainText('listened, listened');
});

test('text box: paste, filter, selection, sizes', async ({ page }) => {
    await open(page, 'textbox');
    const box = page.getByLabel('Pasted box');
    await expect(box).toHaveAttribute('size', '12');
    await expect(box).toHaveAttribute('maxlength', '8');
    const area = page.getByLabel('Area');
    await expect(area).toHaveAttribute('rows', '4');
    await expect(area).toHaveAttribute('cols', '30');

    await box.fill('pasted');
    await box.dispatchEvent('paste');
    await expect(status(page)).toHaveText('Status: paste pasted');
    await button(page, 'Select').click();
    await expect(box).toHaveValue('abcdef');
    await expect.poll(() => box.evaluate(e => [e.selectionStart, e.selectionEnd])).toEqual([1, 4]);
    await button(page, 'Stop paste').click();
    await box.dispatchEvent('paste');
    await button(page, 'Select').click(); // a round trip after the paste
    await expect(status(page)).toHaveText('Status: paste pasted');

    const digits = page.getByLabel('Digits');
    await digits.pressSequentially('a1.b2');
    await expect(digits).toHaveValue('1.2');
    await digits.evaluate(e => {
        e.value = 'x9y';
        e.dispatchEvent(new InputEvent('input', { inputType: 'insertFromPaste', bubbles: true }));
    });
    await expect(digits).toHaveValue('9');
});

test('layouts: hidden and removed layers and docks, a replaced simple layout, an east and a line end', async ({ page }) => {
    await open(page, 'layout');
    await expect(page.getByLabel('Simple')).toHaveText('Second');
    const layered = page.getByText('Layered', { exact: true });
    await expect(layered).toBeVisible();
    await button(page, 'Hide layered').click();
    await expect(layered).toBeHidden();
    await button(page, 'Remove layered').click();
    await expect(layered).toHaveCount(0);
    await expect(page.getByText('Kept')).toBeVisible();

    const dock = page.getByLabel('Dock');
    const east = dock.getByText('East');
    const end = dock.getByText('End');
    const box = /** @type {{x: number, width: number}} */ (await dock.boundingBox());
    // east is outermost on the right, the line end inside it
    await expect.poll(async () => Math.round(((await east.boundingBox()) ?? { x: 0 }).x - box.x)).toBe(240);
    await expect.poll(async () => Math.round(((await end.boundingBox()) ?? { x: 0 }).x - box.x)).toBe(200);
    await button(page, 'Hide east').click();
    await expect(east).toBeHidden();
    await button(page, 'Remove east').click();
    await expect(east).toHaveCount(0);
    await expect(end).toBeVisible();
});

test('split layout: a splitter toggled by a double click, snapped closed by a drag, resizes reported', async ({ page }) => {
    await open(page, 'layout');
    const split = page.getByLabel('Split');
    const west = split.getByText('Split west');
    const splitter = split.locator('.pony-Splitter');
    await splitter.dblclick();
    await expect(west).toHaveCSS('width', '0px');
    await expect(status(page)).toContainText('resized 0');
    await splitter.dblclick();
    await expect(west).toHaveCSS('width', '100px');
    await expect(status(page)).toContainText('resized 0/0, resized 100');

    // the panel is half the page wide: the page narrower is a resize
    await page.setViewportSize({ width: 1000, height: 720 });
    await expect(status(page)).toContainText('resized 100/0, resized 100');

    // dragged under the snap size (30 px): closed
    const handle = /** @type {{x: number, y: number, width: number, height: number}} */ (await splitter.boundingBox());
    await page.mouse.move(handle.x + handle.width / 2, handle.y + handle.height / 2);
    await page.mouse.down();
    await page.mouse.move(handle.x - 80, handle.y + handle.height / 2, { steps: 4 });
    await page.mouse.up();
    await expect(west).toHaveCSS('width', '0px');

    await button(page, 'Stop resize reports').click();
    const before = await status(page).textContent();
    await splitter.dblclick();
    await expect(west).toHaveCSS('width', '100px');
    await page.waitForTimeout(700); // longer than the 500 ms the reports wait
    await button(page, 'Hide layered').click(); // a round trip
    await expect(page.getByText('Layered', { exact: true })).toBeHidden();
    await expect(status(page)).toHaveText(/** @type {string} */ (before));
});

test('root layout panel: fills the window', async ({ page }) => {
    await page.goto(BASE_URL + '#terminal-rootlayout');
    const root = page.locator('.pony-RootLayoutPanel');
    await expect(root).toContainText('In the root layout');
    const box = /** @type {{width: number, height: number}} */ (await root.boundingBox());
    expect(box).toMatchObject({ width: 1280, height: 720 });
});

test('panels: scrolled by the server; cells aligned, sized and inserted; table rows, cells and columns styled', async ({ page }) => {
    await page.goto(BASE_URL + '#terminal-panels');
    const scrolled = page.getByLabel('Scrolled');
    const position = () => scrolled.evaluate(e => [e.scrollLeft, e.scrollTop]);
    await button(page, 'Scroll to 30').click();
    await expect.poll(position).toEqual([30, 30]);
    await button(page, 'Scroll to bottom').click();
    await button(page, 'Scroll to right').click();
    await expect.poll(position).toEqual(await scrolled.evaluate(e => [e.scrollWidth - e.clientWidth, e.scrollHeight - e.clientHeight]));
    await button(page, 'Scroll to top').click();
    await expect.poll(position).toEqual([0, 0]);

    const cells = page.getByLabel('Cells');
    await expect(cells).toHaveText('OneTwoThree');
    const cell = text => cells.locator('.pony-Cell', { has: page.getByText(text, { exact: true }) });
    await expect(cell('One')).toHaveCSS('text-align', 'right');
    await expect(cell('One')).toHaveCSS('justify-content', 'flex-end');
    await expect(cell('Three')).toHaveCSS('width', '80px');
    await expect(cells).toHaveCSS('gap', '4px');
    await expect(cells).toHaveCSS('border-top-width', '2px');

    const table = page.getByLabel('Table');
    const rows = table.locator(':scope > tbody > tr');
    await expect(table).toHaveCSS('border-spacing', '3px');
    await expect(table).toHaveCSS('border-top-width', '1px');
    await expect(rows.nth(0)).toHaveClass('head');
    await expect(rows.nth(1)).toHaveClass('odd');
    await expect(rows.nth(2)).toBeHidden();
    await button(page, 'Show row').click();
    await expect(rows.nth(2)).toBeVisible();
    const corner = rows.nth(0).locator(':scope > td').nth(0);
    await expect(corner).toHaveClass('corner');
    await expect(corner).toHaveAttribute('rowspan', '2');
    await expect(rows.nth(0).locator(':scope > td').nth(1)).toHaveClass('ptc b');
    await expect(rows.nth(1).locator(':scope > td').nth(0)).toHaveCSS('vertical-align', 'top');
    const columns = table.locator(':scope > colgroup > col');
    await expect(columns.nth(0)).toHaveClass('first');
    await expect(columns.nth(0)).toHaveCSS('width', '60px');
    await expect(columns.nth(1)).toHaveClass('second');
});

test('tree: Home, End and Up move the focus; the server renames, collapses, removes and clears', async ({ page }) => {
    await page.goto(BASE_URL + '#terminal-tree');
    const item = name => page.getByRole('treeitem', { name, exact: true });
    await expect(item('Child A')).toBeVisible();
    await item('First').focus();
    await page.keyboard.press('End');
    await expect(item('Third')).toBeFocused();
    await page.keyboard.press('ArrowUp');
    await expect(item('Second')).toBeFocused();
    await page.keyboard.press('Home');
    await expect(item('First')).toBeFocused();
    await page.keyboard.press('x'); // not a key of the tree
    await expect(item('First')).toBeFocused();

    await button(page, 'Rename').click();
    await expect(page.getByRole('treeitem', { name: /^Renamed/ })).toBeVisible();
    await button(page, 'Collapse').click();
    await expect(item('Child A')).toBeHidden();
    await button(page, 'Remove B').click();
    await expect(page.getByText('Child B')).toHaveCount(0);
    await button(page, 'Clear').click();
    await expect(page.getByRole('treeitem')).toHaveCount(0);
});

test('rich text: the formatter commands sent by the server, a toolbar list, a link from a prompt', async ({ page }) => {
    await page.goto(BASE_URL + '#terminal-richtext');
    const area = page.getByRole('textbox', { name: 'Rich text' });
    await expect(area).toHaveText('Some text');
    await button(page, 'Server select all').click();
    await button(page, 'Server bold').click();
    await expect(area.locator('b')).toHaveText('Some text');
    await button(page, 'Server large').click();
    await expect(area.locator('font[size]')).toHaveText('Some text');
    await button(page, 'Server center').click();
    await expect(area.locator('[style*="text-align: center"], div[align="center"]').first()).toBeVisible();
    await button(page, 'Server red').click();
    await expect(area.locator('font[color="#ff0000"], font[color="red"]').first()).toBeVisible();

    await area.selectText();
    await page.getByRole('combobox', { name: 'Font' }).selectOption({ index: 2 });
    await expect(page.getByRole('combobox', { name: 'Font' })).toHaveValue('');
    await expect(area.locator('font[face]').first()).toBeVisible();

    page.once('dialog', dialog => dialog.accept('https://example.com/'));
    await area.selectText();
    await page.getByRole('button', { name: 'Create Link' }).click();
    await expect(area.locator('a[href="https://example.com/"]')).toBeVisible();
    page.once('dialog', dialog => dialog.dismiss());
    await page.getByRole('button', { name: 'Create Link' }).click();
    await expect(area.locator('a')).toHaveCount(1);

    await button(page, 'Plain text').click();
    await expect(area).toHaveText('Plain');
    await expect(area.locator('*')).toHaveCount(0);
    await button(page, 'Disable').click();
    await expect(area).toHaveAttribute('contenteditable', 'false');
    await expect(area).toHaveAttribute('aria-disabled', 'true');
    await button(page, 'Enable').click();
    await expect(area).toHaveAttribute('contenteditable', 'true');
    await expect(area).not.toHaveAttribute('aria-disabled');
});

test('popups: a press on the glass hides an auto-hide popup, a fixed dialog stays, a popup is dragged', async ({ page }) => {
    await open(page, 'popup');
    await button(page, 'Show decorated').click();
    const glass = page.locator('.pony-Glass.terminal-glass');
    await expect(glass).toBeVisible();
    const decorated = page.getByText('Decorated', { exact: true });
    await expect(decorated).toBeVisible();
    await page.mouse.click(5, 5);
    await expect(decorated).toBeHidden();
    await expect(glass).toHaveCount(0);
    await expect(status(page)).toHaveText('Status: decorated closed');

    await button(page, 'Show fixed dialog').click();
    const dialog = page.getByRole('dialog', { name: 'Fixed dialog' });
    await expect(dialog).toBeVisible();
    const before = await dialog.boundingBox();
    await dialog.getByText('Fixed dialog').dragTo(page.locator('body'), { targetPosition: { x: 50, y: 50 } });
    expect(await dialog.boundingBox()).toEqual(before);
    await page.mouse.click(5, 5);
    await expect(dialog).toBeHidden();
    await expect(status(page)).toHaveText('Status: decorated closed, dialog closed');

    await button(page, 'Show draggable popup').click();
    const popup = page.getByText('Draggable popup', { exact: true });
    const start = /** @type {{x: number, y: number}} */ (await popup.boundingBox());
    await page.mouse.move(start.x + 5, start.y + 5);
    await page.mouse.down();
    await page.mouse.move(start.x + 105, start.y + 55, { steps: 5 });
    await page.mouse.up();
    await expect.poll(async () => {
        const moved = /** @type {{x: number, y: number}} */ (await popup.boundingBox());
        return [Math.round(moved.x - start.x), Math.round(moved.y - start.y)];
    }).toEqual([100, 50]);
});

test('images: a clipped image, a URL changed, an image streamed; a file the server sends', async ({ page }) => {
    await page.goto(BASE_URL + '#terminal-image');
    const clipped = page.getByAltText('Clipped');
    await expect(clipped).toHaveCSS('object-position', '-4px -6px');
    await expect(clipped).toHaveCSS('width', '20px');
    await expect(clipped).toHaveCSS('height', '30px');
    await button(page, 'Change URL').click();
    await expect(page.getByAltText('Changed')).toHaveAttribute('src', /pony\.png\?changed$/);
    const streamed = page.getByAltText('Streamed');
    await expect.poll(() => streamed.evaluate(e => /** @type {HTMLImageElement} */ (e).naturalWidth)).toBe(10);

    const [download] = await Promise.all([page.waitForEvent('download'), button(page, 'Download').click()]);
    expect(download.suggestedFilename()).toBe('terminal.csv');
});

test('misc: scripts later and failing, cookies removed, then many widgets changed by the server', async ({ page }) => {
    await open(page, 'misc');
    await button(page, 'Later').click();
    await expect(status(page)).toHaveText('Status: script 12');
    await button(page, 'Fail').click();
    await expect(status(page)).toHaveText('Status: script 12, script failed');
    await button(page, 'Bake').click();
    await expect(status(page)).toContainText('terminal=baked');
    await button(page, 'Eat').click();
    await expect.poll(() => page.evaluate(() => document.cookie)).not.toContain('terminal=');

    // a suggestion for two words: those of both
    const suggest = page.getByRole('combobox', { name: 'Suggest' });
    await suggest.pressSequentially('red a', { delay: 30 });
    const suggestions = page.locator('[role="option"]');
    await expect(suggestions).toHaveText(['Red apple']);
    await page.keyboard.press('Escape');
    await suggest.fill('');
    await page.keyboard.press('Enter');

    await button(page, 'Slow').click();
    await expect(status(page)).toContainText('slow');
    const linked = page.getByText('Linked', { exact: true });
    await expect(linked).toHaveAttribute('data-value', 'Linked');

    await button(page, 'Change').click();
    await expect(page.getByText('Changed', { exact: true })).toHaveAttribute('data-value', 'Changed');
    await expect(page.getByText('Wrapped')).toHaveCSS('white-space', 'nowrap');
    const list = page.getByLabel('List');
    await expect(list.locator('option')).toHaveText(['Alpha', 'Bravo']);
    await expect(list).toHaveAttribute('size', '3');
    await expect(page.locator('b', { hasText: 'Bold check' })).toBeVisible();
    await expect(page.getByRole('checkbox')).toHaveAttribute('data-input', 'yes');
    await expect(page.getByText('Disclosed')).toBeHidden();
    await expect(page.getByLabel('Stack')).not.toContainText('Stack one');
    await expect(page.getByText('Stack two')).toBeVisible();
    await expect(page.getByRole('tab')).toHaveText(['Two']);
    await expect(page.getByRole('tab', { name: 'Two' })).toHaveAttribute('aria-selected', 'true');
    await expect(page.locator('input[type="file"]')).toBeDisabled();
    await suggest.pressSequentially('b', { delay: 30 });
    await expect(suggestions).toHaveText(['Blue']);
    await page.keyboard.press('Escape');

    await button(page, 'Empty').click();
    await expect(page.getByRole('menuitem')).toHaveCount(0);
    await expect(page.getByText('Changed', { exact: true })).toHaveCount(0);
    await expect(page.locator('[data-value]')).toHaveCount(0);
});

test('misc: a context the server ends reloads the page', async ({ page }) => {
    await open(page, 'misc');
    await Promise.all([page.waitForEvent('load'), button(page, 'End session').click()]);
    await expect(page.getByLabel('Status')).toHaveText('Status:');
});

test('collect: the objects the server collected release their listeners', async ({ page }) => {
    const messages = sent(page);
    await open(page, 'collect');
    const counter = await page.locator('my-counter').elementHandle();
    const events = () => messages.filter(m => m.includes('"W":"count-changed"')).length;
    await counter?.evaluate(e => e.dispatchEvent(new CustomEvent('count-changed', { detail: 1 })));
    await expect.poll(events).toBe(1);
    // the server collects once the JVM has: an event of the collected component goes nowhere
    await expect(async () => {
        await button(page, 'Collect').click();
        await expect(page.getByLabel('Status')).toHaveText('Collected');
        const before = events();
        await counter?.evaluate(e => e.dispatchEvent(new CustomEvent('count-changed', { detail: 1 })));
        await page.waitForTimeout(300);
        expect(events()).toBe(before);
    }).toPass({ timeout: 20000 });
});
