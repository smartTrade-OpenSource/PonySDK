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
        // What the terminal asks of the window: WebKit headless does not resize or move one
        const calls = [];
        for (const name of ['resizeTo', 'resizeBy', 'moveTo', 'moveBy']) {
            const native = window[name].bind(window);
            window[name] = (x, y) => {
                calls.push(`${name} ${x} ${y}`);
                document.documentElement.setAttribute('data-calls', calls.join(', '));
                native(x, y);
            };
        }
    });
    await open(page, 'window');
    await expect(page).toHaveTitle('Terminal window page');
    const [child] = await Promise.all([page.waitForEvent('popup'), button(page, 'Open window').click()]);
    await expect(status(page)).toHaveText('Status: opened');
    await expect(child.getByText('Child content')).toBeVisible();
    await expect(child.getByText('Terminal window page')).toBeVisible(); // its dictionary entry, from its opener
    await expect(child).toHaveTitle('terminal-child'); // the window's name until the server titles it

    await button(page, 'Title').click();
    await expect(child).toHaveTitle('Child title');
    await button(page, 'Print').click();
    await expect(child.locator('html')).toHaveAttribute('data-printed', 'yes');
    await button(page, 'Resize to').click();
    await button(page, 'Resize by').click();
    await button(page, 'Move').click();
    await expect(child.locator('html')).toHaveAttribute('data-calls',
        'resizeTo 420 320, resizeBy 20 20, moveTo 10 10, moveBy 5 5');
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
    await target.click({ button: 'middle' });
    await expect(status(page)).toContainText('down BUTTON_MIDDLE');
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
        /** @type {any} */ (window).pony.sendDataToServer(ajax.id, { question: 'life', none: null, count: 2 }, resolve);
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

test('widget: a locked event is sent once until the server answers it', async ({ page }) => {
    await open(page, 'widget');
    const locked = page.getByText('Locked', { exact: true });
    await locked.evaluate(e => {
        e.click();
        e.click(); // in the same task: before any answer of the server can come
    });
    await expect(status(page)).toContainText('locked 1');
    await expect(locked).not.toHaveAttribute('aria-busy', 'true');
    await locked.click();
    await expect(status(page)).toContainText('locked 2');
    await expect(status(page)).not.toContainText('locked 3');
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
    // the width the terminal set: WebKit computes none for a col of a table without table-layout: fixed
    expect(await columns.nth(0).evaluate(e => e.style.width)).toBe('60px');
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

test('rich text: the formatter commands sent by the server, a toolbar list, a link and an image from a prompt', async ({ page }) => {
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
    page.once('dialog', dialog => {
        expect(dialog.message()).toBe('Enter an image URL:');
        void dialog.accept('images/pony.png');
    });
    await area.click();
    await page.getByRole('button', { name: 'Insert Image' }).click();
    await expect(area.locator('img[src="images/pony.png"]')).toHaveCount(1);

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
    // the server collects once the JVM has
    let clicks = 0;
    await expect(async () => {
        await button(page, 'Collect').click();
        await expect(page.getByLabel('Status')).toHaveText(`Collected ${++clicks}`, { timeout: 1000 });
    }).toPass({ timeout: 20000 });
    // The next message the server handles tells the terminal (PObjectCache): then an event of the collected
    // component goes nowhere. Sent before, it would reach a server that no longer has the component.
    await button(page, 'Collect').click();
    await expect(page.getByLabel('Status')).toHaveText(`Collected ${++clicks}`);
    const before = events();
    await counter?.evaluate(e => e.dispatchEvent(new CustomEvent('count-changed', { detail: 1 })));
    await page.waitForTimeout(300);
    expect(events()).toBe(before);
});

/** The terminal's warnings that an object has no such property: none is expected from a page of the server. */
function unknownProperties(page) {
    const warnings = [];
    page.on('console', m => {
        if (/has no property/.test(m.text())) warnings.push(m.text());
    });
    return warnings;
}

test('generic: a widget\'s own properties and handlers on every widget type, children inserted and removed', async ({ page }) => {
    const warnings = unknownProperties(page);
    await open(page, 'generic');
    for (const label of ['Upload', 'Generic stack', 'Generic tabs', 'Generic split', 'Generic scroll', 'Generic cells',
        'Generic list', 'Generic grid']) {
        await expect(page.getByLabel(label, { exact: true })).toHaveClass(/\bgeneric\b/);
    }
    await expect(page.getByRole('menubar')).toHaveClass(/\bgeneric\b/);
    await expect(page.getByRole('menuitem', { name: 'Generic item' })).toHaveClass(/\bgeneric\b/);
    await expect(page.locator('.pony-SuggestBox.generic')).toHaveCount(1);
    await expect(page.getByText('2 ponies')).toHaveClass(/\bgeneric\b/);

    const upload = page.getByLabel('Upload', { exact: true });
    await expect(upload.locator('input[type=file]')).toHaveAttribute('tabindex', '3');
    // focus and blur of a widget whose element holds its field: those of the field
    await upload.locator('input[type=file]').focus();
    await expect(status(page)).toContainText('upload focus');
    await page.getByLabel('Generic check').focus();
    await page.getByLabel('Generic check').blur();
    await expect(status(page)).toContainText('check focus, check blur');
    await page.getByRole('menuitem', { name: 'Generic item' }).hover();
    await expect(status(page)).toContainText('item over');
    await page.getByLabel('Generic suggest').hover();
    await expect(status(page)).toContainText('suggest over');
    await page.getByLabel('Generic stack', { exact: true }).hover();
    await expect(status(page)).toContainText('stack over');
    await page.getByLabel('Generic tabs', { exact: true }).hover();
    await expect(status(page)).toContainText('tabs over');
    await page.getByLabel('Pressed').press('a');
    await expect(status(page)).toContainText('press 97');

    await button(page, 'Mark').click();
    await expect(page.getByText('Marked', { exact: true })).toHaveAttribute('data-mark', 'yes x2');
    await expect(page.locator('[role=treeitem]', { hasText: 'Generic tree item' })).toHaveAttribute('data-mark', 'marked');

    const cells = page.getByLabel('Generic cells', { exact: true });
    const list = page.getByLabel('Generic list', { exact: true });
    await button(page, 'Insert').click();
    await expect(cells).toHaveText('First cellInserted cellLast cell');
    await expect(list).toHaveText('Inserted entryLast entry');
    await button(page, 'Remove').click();
    await expect(cells).toHaveText('First cellInserted cell');
    await expect(cells.locator('.pony-Cell')).toHaveCount(2); // the removed child's cell goes with it
    await expect(list).toHaveText('Inserted entry');

    await button(page, 'Show popup').click();
    const popup = page.locator('.pony-PopupPanel.generic');
    await expect(popup).toBeVisible();
    await popup.hover();
    await expect(status(page)).toContainText('popup over');

    // A drag of the scroll bar sends the position once released
    const scroll = page.getByLabel('Generic scroll', { exact: true });
    await scroll.hover();
    await page.mouse.down();
    await page.mouse.up();
    await expect(status(page)).toContainText('scrolled');

    await button(page, 'Unwatch').click();
    await button(page, 'Animate').click();
    await expect(status(page)).toContainText('unwatched, animated');
    expect(warnings).toEqual([]);
});

test('web component: children at an index and at the end, a property as text then removed, a method, an event unlistened', async ({ page }) => {
    const messages = sent(page);
    await open(page, 'webcomponent');
    const component = page.getByLabel('Component', { exact: true });
    await expect(component).toHaveText('Second');
    expect(await component.evaluate(e => /** @type {any} */ (e).note)).toBe('plain text'); // not JSON: as it came
    await button(page, 'Insert first').click();
    await button(page, 'Append').click();
    await expect(component).toHaveText('FirstSecondThird');
    await button(page, 'Remove note').click();
    await expect.poll(() => component.evaluate(e => 'note' in e)).toBe(false);
    await button(page, 'Call').click();
    await expect(component).toHaveAttribute('data-called', 'yes');

    // A detail that has no JSON goes as its text
    await component.evaluate(e => {
        const detail = { toString: () => 'circular' };
        /** @type {any} */ (detail).self = detail;
        e.dispatchEvent(new CustomEvent('ping', { detail }));
    });
    await expect(status(page)).toContainText('ping "circular"');
    await button(page, 'Unlisten').click();
    await expect(status(page)).toContainText('unlistened');
    const pings = messages.filter(m => m.includes('"ping"')).length;
    await component.evaluate(e => e.dispatchEvent(new CustomEvent('ping', { detail: 1 })));
    await button(page, 'Call').click(); // a round trip after the event
    expect(messages.filter(m => m.includes('"ping"')).length).toBe(pings);
});

test('date box: styled days, a format changed by the server, the keyboard in the picker, the time of the day kept', async ({ page }) => {
    await open(page, 'datebox');
    const box = page.getByLabel('Date', { exact: true });
    await expect(box).toHaveValue('2024-05-15 09:30');
    await button(page, 'Day first').click();
    await expect(box).toHaveValue('15/05/2024 09:30');

    await box.focus();
    await box.press('ArrowDown'); // shows the picker: above the box, which is at the bottom of the window
    const popup = page.locator('.pony-DateBoxPopup');
    await expect(popup).toBeVisible();
    const [boxTop, popupBottom] = [(await box.boundingBox())?.y ?? 0, await popup.evaluate(e => e.getBoundingClientRect().bottom)];
    expect(popupBottom).toBeLessThanOrEqual(boxTop + 1);
    const day = text => popup.locator('[role=gridcell]:not([data-filler])', { hasText: new RegExp(`^${text}$`) });
    await expect(day('15')).toHaveClass(/\bmarked\b/);
    await button(page, 'Unmark').click();
    await expect(day('15')).not.toHaveClass(/\bmarked\b/);

    await box.focus();
    await box.press('ArrowDown');
    await box.press('ArrowDown'); // into the days
    const focused = () => page.evaluate(() => document.activeElement?.textContent);
    await expect.poll(focused).toBe('15');
    await page.keyboard.press('ArrowLeft');
    await expect.poll(focused).toBe('14');
    await page.keyboard.press('ArrowUp');
    await expect.poll(focused).toBe('7');
    await page.keyboard.press('End');
    await expect.poll(focused).not.toBe('7');
    await page.keyboard.press('PageUp');
    await expect(popup.locator('.pony-DatePicker-month')).toContainText('April');
    await page.keyboard.press('Home');
    await page.keyboard.press('Enter');
    await expect(status(page)).toContainText(/value \d\d\/04\/2024 09:30/); // the day picked, at the time it had

    // A text the pattern does not read, but the browser does: read in the pattern
    await box.fill('May 20, 2024');
    await box.press('Enter');
    await expect(box).not.toHaveAttribute('aria-invalid', 'true');
    await box.press('ArrowDown');
    await expect(day('20')).toHaveAttribute('aria-selected', 'true');
});

test('misc: a context the server ends asks the page to reload, through ponysdk.js', async ({ page }) => {
    await page.addInitScript(() => {
        const asked = () => document.documentElement.setAttribute('data-reload', 'asked');
        Object.defineProperty(document, 'doReload', { configurable: false, get: () => asked, set() {} });
    });
    await open(page, 'misc');
    await button(page, 'End session').click();
    await expect(page.locator('html')).toHaveAttribute('data-reload', 'asked');
});

test('window.pony: the methods of the GWT terminal that pages and addons call', async ({ page }) => {
    const sockets = [];
    page.on('websocket', ws => sockets.push(ws));
    await page.goto(BASE_URL + '?from=test#terminal-misc'); // a query the base URL leaves out
    await expect(page.getByLabel('Status')).toBeVisible();
    expect(await page.evaluate(() => /** @type {any} */ (window).pony.getHostPageBaseURL())).toBe(BASE_URL);
    const pony = await page.evaluate(() => {
        const terminal = /** @type {any} */ (window).pony;
        const id = terminal.getContextId();
        terminal.setContextId(id);
        const tabindex = terminal.isTabindexOnlyFormField();
        terminal.setTabindexOnlyFormField(!tabindex);
        return { id, same: terminal.getContextId() === id, flipped: terminal.isTabindexOnlyFormField() === !tabindex };
    });
    expect(pony).toEqual({ id: expect.any(Number), same: true, flipped: true });
    expect(pony.id).toBeGreaterThan(0);
    await page.evaluate(() => /** @type {any} */ (window).pony.close());
    await expect.poll(() => sockets.every(ws => ws.isClosed())).toBe(true);
});

test('without requestIdleCallback, the widgets loaded on demand load a second after the page', async ({ page }) => {
    await page.addInitScript(() => { delete /** @type {any} */ (window).requestIdleCallback; });
    const chunks = [];
    page.on('request', request => { if (request.url().includes('/chunks/rich-text.js')) chunks.push(request.url()); });
    await open(page, 'window'); // no rich text on it
    await expect.poll(() => chunks.length, { timeout: 5000 }).toBe(1);
});

test('a window page opened by no terminal says so', async ({ page }) => {
    const errors = [];
    page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
    await page.goto(BASE_URL + '?1=1&2=1'); // UI_CONTEXT_ID and WINDOW_ID, without the opener that has them
    await expect.poll(() => errors.some(e => e.includes('No terminal opened this page'))).toBe(true);
});

/** The terminal's console warnings. */
function warnings(page) {
    const warned = [];
    page.on('console', m => { if (m.type() === 'warning') warned.push(m.text()); });
    return warned;
}

test('a widget whose module does not load: reported, its block skipped, the page goes on', async ({ page }) => {
    const warned = warnings(page);
    await page.route(/\/chunks\/rich-text\.js/, route => route.abort());
    await page.goto(BASE_URL + '#terminal-richtext');
    await expect.poll(() => warned.some(w => /has no RICH_TEXT_AREA yet/.test(w))).toBe(true);
    await expect(page.getByRole('button').first()).toBeVisible(); // the rest of the page
});

test('a block the server would not send: a property an object does not have, a type this terminal does not know', async ({ page }) => {
    const warned = warnings(page);
    let socket;
    await page.routeWebSocket(/\/sample\/ws\b/, ws => {
        ws.connectToServer();
        socket = ws;
    });
    await open(page, 'misc');
    // Keys of ServerToClientModel: TYPE_UPDATE 7 and TYPE_CREATE 6 (an id on 2 bytes), WIDGET_TYPE 194 (a byte),
    // WIDGET_FULL_SIZE 201 (no value), END 4. The cookies are object #0.
    socket.send(Buffer.from([7, 0, 0, 201, 4]));
    socket.send(Buffer.from([6, 0x7f, 0, 194, 127, 4]));
    socket.send(Buffer.from([7, 0, 1, 201, 4])); // the main window, #1
    await expect.poll(() => warned.filter(w => /#[01] has no property WIDGET_FULL_SIZE|has no type #127 yet/.test(w)).length)
        .toBe(3);
    await button(page, 'Later').click();
    await expect(status(page)).toContainText('script 12'); // the page goes on
});

test('misc: a root panel in an element of the page, by its id; an id the page does not have is reported', async ({ page }) => {
    const messages = sent(page);
    await open(page, 'misc');
    await button(page, 'Zone').click();
    await expect(page.locator('#loading')).toContainText('In the zone');
    await button(page, 'No zone').click();
    await expect.poll(() => messages.some(m => m.includes('No element #nowhere'))).toBe(true); // the error, to the server
});

test('window: one of another URL, which is no terminal\'s; a frame whose URL has a query; the tabindex option passed on; the page reloaded by the server', async ({ page }) => {
    await open(page, 'window');
    await page.evaluate(() => /** @type {any} */ (window).pony.setTabindexOnlyFormField(true));
    const [blank] = await Promise.all([page.waitForEvent('popup'), button(page, 'Open blank').click()]);
    await expect(blank).toHaveURL('about:blank');
    await blank.close();

    await button(page, 'Frame').click();
    await expect(page.locator('iframe[title="Query frame"]')).toHaveAttribute('src', /frame-demo\.html\?demo&.*=true$/);

    const [child] = await Promise.all([page.waitForEvent('popup'), button(page, 'Open window').click()]);
    await expect(child.getByText('Child content')).toBeVisible();
    expect(await child.evaluate(() => /** @type {any} */ (window).pony.isTabindexOnlyFormField())).toBe(true);
    await Promise.all([child.waitForEvent('close'), button(page, 'Close').click()]);

    await Promise.all([page.waitForEvent('load'), button(page, 'Reload page').click()]);
    await expect(status(page)).toHaveText('Status:'); // another context
});

test('tree: an item inserted first, the last children removed, a field as an item, moved; an item opened before it is added; no selection', async ({ page }) => {
    await page.goto(BASE_URL + '#terminal-tree');
    const item = name => page.getByRole('treeitem', { name, exact: true });
    const children = page.locator('[role=treeitem]', { hasText: 'First' }).locator(':scope > [role=group] > [role=treeitem]');
    await expect(children).toHaveText(['Child A', 'Child B']);
    await button(page, 'Insert first').click();
    await expect(children).toHaveText(['Child zero', 'Child A', 'Child B']);
    await button(page, 'Remove B').click();
    await expect(item('First')).toHaveAttribute('aria-expanded', 'true');
    await button(page, 'Remove children').click();
    await expect(children).toHaveCount(0);
    await expect(item('First')).not.toHaveAttribute('aria-expanded');

    await button(page, 'Field item').click();
    const field = page.getByLabel('Item field');
    await field.pressSequentially('ab'); // the tree's keys are an item's, not those of a field in it
    await expect(field).toHaveValue('ab');
    await expect(field).toBeFocused();
    await button(page, 'Move field').click();
    await expect(page.getByRole('treeitem').last().getByLabel('Item field')).toHaveValue('ab');
    await expect(page.getByLabel('Item field')).toHaveCount(1);

    await button(page, 'Add opened').click();
    await expect(item('Opened')).toHaveAttribute('aria-expanded', 'true');
    await expect(item('Inner')).toBeVisible();

    await item('Second').click();
    await expect(item('Second')).toHaveAttribute('aria-selected', 'true');
    await button(page, 'Unselect').click();
    await expect(page.locator('[role=treeitem][aria-selected="true"]')).toHaveCount(0);
});

test('menu: a click on an open item closes it; Home, End and other keys; a submenu\'s submenu by the keyboard; an item without a command; a bar removed while a submenu shows', async ({ page }) => {
    await open(page, 'menu');
    const item = name => page.getByRole('menuitem', { name, exact: true });
    await item('File').click();
    await expect(item('New')).toBeVisible();
    await item('File').click(); // its submenu shows: it closes
    await expect(item('New')).toBeHidden();

    await item('File').focus();
    await page.keyboard.press('End');
    await expect(item('Help')).toBeFocused();
    await page.keyboard.press('Home');
    await expect(item('File')).toBeFocused();
    await page.keyboard.press('x'); // not a key of the bar
    await expect(item('File')).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(item('New')).toBeFocused();
    await page.keyboard.press('x'); // a key of neither the submenu nor the bar under it
    await expect(item('New')).toBeFocused();
    await page.keyboard.press('ArrowDown');
    await expect(item('Recent')).toBeFocused();
    await page.keyboard.press('ArrowRight');
    await expect(item('Recent one')).toBeFocused();
    await page.keyboard.press('ArrowLeft');
    await expect(item('Recent one')).toBeHidden();
    await expect(item('Recent')).toBeFocused();

    await item('File').focus();
    await page.keyboard.press('Enter'); // its submenu shows already: it stays
    await expect(item('New')).toBeFocused();
    await item('Plain').click(); // no command: nothing sent
    await page.mouse.click(5, 5);
    await expect(item('New')).toBeHidden();

    await item('File').focus();
    await page.keyboard.press('Enter');
    await expect(item('New')).toBeFocused();
    await button(page, 'Remove menu').focus(); // no press outside the menus: the submenu still shows
    await expect(item('New')).toBeVisible();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('menubar')).toHaveCount(0);
    await expect(status(page)).toHaveText('Status:');
});

test('forms: keys on tabs and on a tab\'s field; a list with nothing chosen; a file taken off; a masked field set, focused, pasted into, typed over; addons on widgets that show', async ({ page }) => {
    await open(page, 'forms');
    const tab = name => page.getByRole('tab', { name, exact: true });
    await tab('First').focus();
    await page.keyboard.press('ArrowLeft'); // from the first, the last
    await expect(page.getByRole('tab').nth(2)).toBeFocused();
    await expect(page.getByText('Third panel')).toBeVisible();
    await page.keyboard.press('Home');
    await expect(tab('First')).toBeFocused();
    await page.keyboard.press('ArrowRight');
    await page.keyboard.press('ArrowLeft');
    await expect(tab('First')).toBeFocused();
    await page.keyboard.press('x'); // not a key of the tabs
    await expect(tab('First')).toBeFocused();
    await page.evaluate(() => /** @type {HTMLElement} */ (document.activeElement).blur());
    await page.getByLabel('Tab field').dispatchEvent('keydown', { key: 'ArrowRight' }); // from a tab's field, no tab focused
    await expect(page.getByText('First panel')).toBeVisible();

    const list = page.getByLabel('Choices');
    await list.selectOption(['Two']);
    await expect(status(page)).toHaveText('Status: choices [1]');
    await list.selectOption([]);
    await expect(status(page)).toHaveText('Status: choices [1], choices []');
    await list.selectOption(['One']);
    await expect(status(page)).toContainText('choices [0]');
    await button(page, 'Deselect').click();
    await expect.poll(() => list.evaluate(e => /** @type {HTMLSelectElement} */ (e).selectedIndex)).toBe(-1);

    const file = page.getByLabel('Form file');
    await file.setInputFiles({ name: 'pony.txt', mimeType: 'text/plain', buffer: Buffer.from('pony') });
    await expect(status(page)).toContainText(/file \S*pony\.txt$/);
    await file.setInputFiles([]);
    await expect(status(page)).toContainText(/file \S*pony\.txt, file (null)?$/);

    const masked = page.getByLabel('Masked');
    await button(page, 'Set masked').click();
    await expect(masked).toHaveValue('12/__'); // the x refused, and what follows it
    await masked.focus(); // by no press: the cursor goes where typing goes on at once
    await expect.poll(() => masked.evaluate(e => /** @type {HTMLInputElement} */ (e).selectionStart)).toBe(3);
    // a text field's paste and drop carry their text (data, no dataTransfer), in each browser
    await masked.evaluate(e => e.dispatchEvent(new InputEvent('beforeinput', { inputType: 'insertFromPaste', data: '7',
        bubbles: true, cancelable: true })));
    await masked.evaluate(e => e.dispatchEvent(new InputEvent('beforeinput', { inputType: 'insertFromDrop', data: '8',
        bubbles: true, cancelable: true })));
    await expect(masked).toHaveValue('12/78');
    await masked.evaluate(e => /** @type {HTMLInputElement} */ (e).setSelectionRange(0, 2));
    await page.keyboard.type('9'); // over the selection cut: typed over what was pulled back
    await expect(masked).toHaveValue('98/__');

    const errors = [];
    page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
    await button(page, 'Highlight').click(); // JSON creation arguments, a widget attached already
    await expect(page.getByText('Highlighted', { exact: true })).toHaveCSS('outline-color', 'rgb(1, 2, 3)');
    await button(page, 'Highlight in no color').click(); // its script refuses it: reported, the page goes on
    await expect.poll(() => errors.some(e => /could not be created/.test(e))).toBe(true);
    await button(page, 'Deselect').click();
    await expect(page.getByText('Unhighlighted', { exact: true })).toHaveCSS('outline-style', 'none');
});

test('date box: a text that is no date marked, the date cleared by the server, years by Shift+PageUp; removed while its picker shows, which hides', async ({ page }) => {
    await open(page, 'datebox');
    const box = page.getByLabel('Date', { exact: true });
    await box.fill('no date');
    await box.press('Enter');
    await expect(box).toHaveAttribute('aria-invalid', 'true');
    await button(page, 'Clear date').click();
    await expect(box).toHaveValue('');
    await expect(box).not.toHaveAttribute('aria-invalid');

    await box.fill('2024-05-15 09:30');
    await box.focus();
    await box.press('ArrowDown');
    await box.press('ArrowDown'); // into the days
    const month = page.locator('.pony-DateBoxPopup .pony-DatePicker-month');
    const year = Number(/\d{4}/.exec(/** @type {string} */ (await month.textContent()))?.[0]);
    await page.keyboard.press('Shift+PageUp');
    await expect(month).toContainText(String(year - 1));
    const popup = page.locator('.pony-DateBoxPopup');
    await button(page, 'Remove box').focus(); // no press outside: the picker still shows
    await expect(popup).toBeVisible();
    await page.keyboard.press('Enter');
    await expect(box).toHaveCount(0);
    await expect(popup).toBeHidden(); // the focus left it
});

test('widget: a binding to a function the page lacks, a native message to no binding, an element\'s style name, a listener\'s options changed', async ({ page }) => {
    const messages = sent(page);
    await open(page, 'widget');
    await button(page, 'Bind nowhere').click();
    await expect.poll(() => messages.some(m => m.includes('No function terminalNowhere'))).toBe(true); // the error, to the server
    await button(page, 'Native unbound').click();
    await expect.poll(() => messages.some(m => /NATIVE message for .*not bound/.test(m))).toBe(true);
    await button(page, 'Restyle element').click();
    await expect(page.getByText('Plain element')).toHaveAttribute('class', 'restyled');
    await button(page, 'Listen with detail').click();
    await page.getByText('Watched', { exact: true }).click();
    await expect(status(page)).toContainText('detail 1');
});

test('panels: a composite styled by the server, a cell panel\'s default alignment, borders taken off, a row past the last, a table cleared', async ({ page }) => {
    await page.goto(BASE_URL + '#terminal-panels');
    await button(page, 'Style composite').click();
    await expect(page.getByText('Wrapped by a composite')).toHaveClass(/\bcomposed\b/);
    await button(page, 'Add centered').click();
    await expect(page.getByLabel('Column').locator('.pony-Cell', { hasText: 'Centered' })).toHaveCSS('text-align', 'center');
    const table = page.getByLabel('Table');
    const rows = table.locator(':scope > tbody > tr');
    await button(page, 'No borders').click();
    await expect(page.getByLabel('Cells')).toHaveCSS('border-top-width', '0px');
    await expect(table).toHaveCSS('border-top-width', '0px');
    await expect(rows.nth(0).locator(':scope > td').first()).toHaveCSS('border-top-width', '0px');
    await button(page, 'Row 5').click();
    await expect(rows).toHaveCount(6);
    await button(page, 'Clear table').click();
    await expect(table.locator('.pony-Label')).toHaveCount(0);
    await expect(rows).toHaveCount(6); // the cells stay
});

test('layouts: an animated constraint, children at the end and the start of their layers, north, south and line start splitters, a split\'s west removed, a header panel resized', async ({ page }) => {
    await open(page, 'layout');
    const aligned = page.getByLabel('Aligned');
    const layer = text => aligned.locator('.pony-Layer', { hasText: text });
    await button(page, 'Animate').click();
    await expect(layer('Top')).toHaveCSS('top', '20px');
    await expect(layer('Top')).toHaveCSS('height', '20px');
    await button(page, 'Align').click();
    await expect.poll(() => aligned.getByText('Top').evaluate(e => [e.style.top, e.style.bottom])).toEqual(['', '0px']);
    await expect.poll(() => aligned.getByText('Bottom').evaluate(e => [e.style.top, e.style.bottom])).toEqual(['0px', '']);

    const vertical = page.getByLabel('Rows');
    const drag = async (splitter, dx, dy) => {
        const box = /** @type {{x: number, y: number, width: number, height: number}} */ (await splitter.boundingBox());
        await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
        await page.mouse.down();
        await page.mouse.move(box.x + box.width / 2 + dx, box.y + box.height / 2 + dy, { steps: 4 });
        await page.mouse.up();
    };
    const splitters = vertical.locator(':scope > .pony-Splitter');
    await expect(splitters).toHaveCount(3);
    await expect(splitters.nth(0)).toHaveAttribute('aria-orientation', 'horizontal');
    await drag(splitters.nth(0), 0, 20); // north, toward the center
    await expect(vertical.getByText('North')).toHaveCSS('height', '70px');
    await drag(splitters.nth(1), 0, -20); // south, toward the center
    await expect(vertical.getByText('South')).toHaveCSS('height', '70px');
    await drag(splitters.nth(2), 10, 0); // line start
    await expect(vertical.getByText('Start')).toHaveCSS('width', '70px');

    const split = page.getByLabel('Split', { exact: true });
    await button(page, 'Remove split west').click();
    await expect(split.getByText('Split west')).toHaveCount(0);
    await expect(split.locator('.pony-Splitter')).toHaveCount(0);

    await button(page, 'Resize header').click(); // the layout is CSS: nothing to do
    await button(page, 'Hide layered').click(); // a round trip after it
    await expect(page.getByText('Layered', { exact: true })).toBeHidden();
    await expect(page.getByLabel('Header panel')).toHaveText('HeaderContentFooter');
});

test('popups: a modal popup whose dialog another script of the page closes is hidden', async ({ page }) => {
    await open(page, 'popup');
    await button(page, 'Show modal').click();
    const modal = page.getByText('Modal popup', { exact: true });
    await expect(modal).toBeVisible();

    await page.evaluate(() => /** @type {HTMLDialogElement} */ (document.querySelector('dialog[open]')).close());

    await expect(modal).toBeHidden();
    await expect(status(page)).toHaveText('Status: modal closed');
});

test('popups: a modal auto-hide popup hides on a press on its backdrop, not on one in it', async ({ page }) => {
    await open(page, 'popup');
    await button(page, 'Show modal').click();
    const modal = page.getByText('Modal popup', { exact: true });
    await expect(modal).toBeVisible();
    await modal.click();
    await expect(modal).toBeVisible();
    const box = /** @type {{x: number, y: number, width: number, height: number}} */ (await modal.boundingBox());
    await page.mouse.click(box.x + box.width / 2, box.y + box.height + 40); // on the backdrop, under it
    await expect(modal).toBeHidden();
    await expect(status(page)).toHaveText('Status: modal closed');
});

test('misc: a cookie with every attribute, removed with its domain; a cookie the page cannot decode; a link taken off', async ({ page, context }) => {
    await context.addInitScript(() => {
        const cookie = /** @type {PropertyDescriptor} */ (Object.getOwnPropertyDescriptor(Document.prototype, 'cookie'));
        const written = [];
        Object.defineProperty(document, 'cookie', {
            configurable: true,
            get: () => cookie.get?.call(document),
            set: value => {
                written.push(value);
                document.documentElement.dataset['cookies'] = written.join(' | ');
                cookie.set?.call(document, value);
            },
        });
    });
    await open(page, 'misc');
    await page.evaluate(() => { document.cookie = 'odd=%E0%A4;path=/'; });
    await page.reload(); // the terminal sends the page's cookies as it starts
    await expect(status(page)).toBeVisible();
    await button(page, 'Read odd').click();
    await expect(status(page)).toHaveText('Status: odd %E0%A4'); // as the page has it
    await button(page, 'Bake options').click();
    await expect(page.locator('html')).toHaveAttribute('data-cookies',
        'options=v;expires=Fri, 01 Jan 2100 00:00:00 GMT;domain=pony.example;path=/;secure;SameSite=Lax');
    await button(page, 'Eat options').click();
    await expect(page.locator('html')).toHaveAttribute('data-cookies', /\| options=;domain=pony\.example;path=\/;expires=Fri, 02-Jan-1970 00:00:00 GMT$/);

    const anchor = page.getByText('Anchor', { exact: true });
    await expect(anchor).toHaveAttribute('href', '#terminal-misc');
    await button(page, 'Unlink').click();
    await expect(anchor).not.toHaveAttribute('href');
});
