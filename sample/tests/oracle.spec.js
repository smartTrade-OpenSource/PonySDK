// @ts-check
const { test, expect } = require('./coverage');
const fs = require('fs');
const path = require('path');

/*
 * Behaviour oracle of the terminal (docs/decisions/0002-javascript-terminal.md, section 8).
 *
 * A scenario drives the widget of an oracle page (OraclePages.java) and records the frames the client sends. The
 * traces under oracle-traces/ were recorded with the former GWT terminal, which the TypeScript terminal replaced: its
 * DOM differs by design, what the server receives must not. The terminal passes when it sends the same, once
 * normalized; the GWT traces are references, never recorded again.
 *
 *   ./gradlew :sample:runShowcase
 *   BASE_URL=... npx playwright test oracle                  check
 *   ORACLE_RECORD=1 BASE_URL=... npx playwright test oracle  record the scenarios that have no trace yet: no other
 *                                                            terminal confirms these, review them by hand
 *
 * Selectors go by role and accessible name.
 */

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
const RECORD = process.env.ORACLE_RECORD === '1';
const TRACES = path.join(__dirname, 'oracle-traces');

/** Name → key of the client's JSON messages, read from the source of the protocol. */
const KEY = (() => {
    const source = fs.readFileSync(path.join(__dirname,
        '../../ponysdk/src/main/java/com/ponysdk/core/model/ClientToServerModel.java'), 'utf8');
    return Object.fromEntries([...source.matchAll(/^\s+([A-Z_]+)\("([^"]+)"\)/gm)].map(m => [m[1], m[2]]));
})();

/** Timing, not behaviour: dropped. */
const DROPPED = [KEY.HEARTBEAT_REQUEST, KEY.TERMINAL_LATENCY];
/** Pixel positions depend on the DOM and the layout: kept as '#'. */
const GEOMETRY = [KEY.WIDGET_POSITION, KEY.POPUP_POSITION];
/** Text written by a terminal for a developer: only its presence counts. */
const MESSAGES = [KEY.ERROR_MSG, KEY.WARN_MSG, KEY.INFO_MSG];
/** The URL a window or a frame opened with carries the context id, which depends on the server's past. */
const CONTEXT_IN_URL = new RegExp(`([?&]${KEY.UI_CONTEXT_ID}=)\\d+`);

/** Within the borders a theme may add. */
function near(actual, expected) {
    return Math.abs(actual - expected) <= 4;
}

/** Left, top, width and height of an element inside a container, rounded: the layout GWT gave. */
async function box(locator, container) {
    const b = await locator.boundingBox();
    const c = await container.boundingBox();
    return [Math.round(b.x - c.x), Math.round(b.y - c.y), Math.round(b.width), Math.round(b.height)];
}

/**
 * Where the terminal departs from GWT on purpose: what it must send is the GWT trace transformed. Each entry says why;
 * the reference stays the one recorded with GWT.
 */
const KNOWN_DIFFERENCES = {
    radio: {
        reason: 'GWT sends the value of a clicked radio button twice (CheckBox and RadioButton both fire it)',
        expected: trace => trace.filter((message, i) => i === 0 || JSON.stringify(message) !== JSON.stringify(trace[i - 1])),
    },
};

/** Steps of each scenario; sent(n) waits until the page sent n frames of its trace. */
const scenarios = {
    button: async page => {
        const button = page.getByRole('button', { name: 'Oracle button' });
        await button.click();
        await button.dblclick();
    },
    'button-busy': async page => {
        const button = page.getByRole('button', { name: 'Oracle button' });
        await button.click();
        await button.dblclick({ force: true }); // on the disabled button: the terminal sends nothing
        await expect(button).toBeEnabled();
        await button.click();
    },
    textbox: async page => {
        const box = page.getByRole('textbox');
        await box.click();
        await box.pressSequentially('pony', { delay: 20 });
        await box.blur(); // not Tab: Firefox keeps the focus on the only field of the page
    },
    checkbox: async page => {
        const box = page.getByRole('checkbox', { name: 'Oracle checkbox' });
        await box.check();
        await box.uncheck();
    },
    listbox: async page => {
        const list = page.getByRole('combobox');
        await list.selectOption('Beta');
        await list.selectOption('Gamma');
    },
    element: async (page, sent) => {
        await sent(3); // cookies, document visibility, element visibility: before the click, whatever the timing
        await page.getByRole('button', { name: 'Run' }).click();
        await expect(page.getByRole('button', { name: 'oracle=baked-42' })).toBeVisible();
        await expect(page).toHaveURL(/#oracle-element-done$/);
    },
    webcomponent: async page => {
        const increment = page.getByRole('button', { name: '+1' });
        await increment.click();
        await expect(page.getByText('6', { exact: true })).toBeVisible();
        await increment.click();
        await expect(page.getByText('0', { exact: true })).toBeVisible(); // reset by the server at 7
    },
    texts: async page => {
        await page.getByText('Oracle label').click();
        await page.getByText('Oracle html').click();
        await page.getByText('Oracle anchor').click();
        await page.getByRole('img', { name: 'Oracle image' }).click();
        await expect(page.getByText('3 ponies')).toBeVisible();
    },
    radio: async page => {
        await page.getByRole('radio', { name: 'Radio B' }).check();
        await page.getByRole('radio', { name: 'Radio C' }).check();
        await page.getByRole('button', { name: 'Select A' }).click();
        await expect(page.getByRole('radio', { name: 'Radio A' })).toBeChecked();
    },
    fields: async page => {
        const area = page.getByRole('textbox', { name: 'Oracle area' });
        await area.fill('two\nlines');
        await area.blur();
        const password = page.getByLabel('Oracle password');
        await password.fill('secret');
        await password.blur();
        const enter = page.getByRole('textbox', { name: 'Oracle enter' });
        await enter.click();
        await enter.pressSequentially('ab', { delay: 20 });
        await enter.press('Enter');
    },
    'listbox-multi': async page => {
        await page.getByRole('listbox').selectOption(['Beta', 'Delta']);
    },
    scroll: async (page, sent) => {
        const panel = page.getByLabel('Oracle scroll');
        await sent(2);
        await panel.evaluate(e => { e.scrollTop = 300; });
        await sent(3);
        await panel.evaluate(e => { e.scrollLeft = 50; });
        await sent(4);
    },
    panels: async page => {
        await page.getByRole('button', { name: 'H0' }).click();
        await page.getByRole('button', { name: 'V2' }).click();
        await page.getByLabel('Oracle focus panel').focus();
        await page.keyboard.press('x');
        await page.getByRole('button', { name: 'Swap' }).click();
        await page.getByText('After').click();
    },
    composite: async page => {
        await page.getByRole('button', { name: 'Inside composite' }).click();
    },
    upload: async page => {
        await page.getByLabel('Oracle file').setInputFiles({ name: 'pony.txt', mimeType: 'text/plain', buffer: Buffer.from('hello') });
        await expect(page.getByText('Uploaded: hello')).toBeVisible();
    },
    flextable: async page => {
        const rows = page.getByRole('row');
        await expect(rows).toHaveCount(3);
        await expect(rows.nth(1).getByRole('button')).toHaveText(['New']);
        await expect(rows.nth(2).getByRole('button')).toHaveText(['R1C0']);
        await page.getByRole('button', { name: 'Remove row 0' }).click();
        await expect(rows).toHaveCount(2);
        await rows.nth(0).getByRole('button', { name: 'New' }).click();
        await rows.nth(1).getByRole('button', { name: 'R1C0' }).click();
    },
    grid: async page => {
        const rows = page.getByRole('row');
        await expect(rows).toHaveCount(2);
        await page.getByRole('button', { name: 'Insert row' }).click();
        await expect(rows).toHaveCount(3);
        await expect(rows.nth(1).getByRole('button')).toHaveText(['G10']);
        await expect(rows.nth(1).getByRole('cell')).toHaveCount(2);
        await rows.nth(2).getByRole('button', { name: 'G11' }).click();
    },
    disclosure: async (page, sent) => {
        const header = page.getByText('Oracle details');
        const content = page.getByText('Hidden content');
        await expect(content).toBeHidden();
        await header.click();
        await expect(content).toBeVisible();
        await sent(3);
        await header.click();
        await expect(content).toBeHidden();
        await sent(4);
        await page.getByRole('button', { name: 'Open from server' }).click();
        await expect(content).toBeVisible();
    },
    tabpanel: async (page, sent) => {
        await expect(page.getByText('First panel')).toBeVisible();
        await page.getByText('Tab 2', { exact: true }).click();
        await expect(page.getByText('Second panel')).toBeVisible();
        await expect(page.getByText('First panel')).toBeHidden();
        await page.getByText('Tab 3', { exact: true }).click();
        await expect(page.getByText('Third panel')).toBeVisible();
        await page.getByRole('button', { name: 'Select first' }).click();
        await expect(page.getByText('First panel')).toBeVisible();
    },
    popup: async page => {
        await page.getByRole('button', { name: 'Show popup' }).click();
        await page.getByRole('button', { name: 'Inside popup' }).click();
        await page.mouse.click(5, 300); // outside: an auto-hide popup hides
        await expect(page.getByRole('button', { name: 'Inside popup' })).toBeHidden();
        await page.getByRole('button', { name: 'Show placed' }).click();
        await expect(page.getByText('Placed popup')).toBeVisible();
        await page.getByRole('button', { name: 'Hide placed' }).click();
        await expect(page.getByText('Placed popup')).toBeHidden();
    },
    dialog: async page => {
        await page.getByRole('button', { name: 'Open dialog' }).click();
        const caption = page.getByText('Oracle dialog', { exact: true });
        const before = await caption.boundingBox();
        await page.mouse.move(before.x + 10, before.y + before.height / 2);
        await page.mouse.down();
        await page.mouse.move(before.x + 60, before.y + before.height / 2 + 30, { steps: 5 });
        await page.mouse.up();
        const after = await caption.boundingBox();
        expect(Math.round(after.x - before.x)).toBe(50);
        expect(Math.round(after.y - before.y)).toBe(30);
        await page.getByRole('button', { name: 'Close Oracle dialog' }).click();
        await expect(caption).toBeHidden();
        await page.getByRole('button', { name: 'Open modal' }).click();
        await page.getByRole('button', { name: 'Close Oracle modal' }).click();
        await expect(page.getByText('Oracle modal', { exact: true })).toBeHidden();
    },
    menu: async page => {
        const item = name => page.getByRole('menuitem', { name, exact: true });
        await item('File').click();
        await item('New').click();
        await expect(item('New')).toBeHidden();
        await item('File').click();
        await item('Edit').hover();
        await item('Copy').click();
        await item('File').click();
        await item('Recent').hover();
        await item('Doc 1').click();
        await item('File').click();
        await item('Disabled').click({ force: true }); // nothing
        await page.mouse.click(5, 300); // outside: the menus close
        await expect(item('New')).toBeHidden();
        await item('Help').click();
    },
    tree: async page => {
        // By the mouse: GWT's keyboard handling missed keys right after a click
        const fruits = page.getByText('Fruits', { exact: true });
        const toggle = async () => {
            const box = await fruits.boundingBox();
            await page.mouse.click(box.x - 8, box.y + box.height / 2); // just left of the text
        };
        await fruits.click();
        await toggle(); // opens
        await expect(page.getByText('Apple', { exact: true })).toBeVisible();
        await page.getByText('Apple', { exact: true }).click();
        await page.getByText('Apple', { exact: true }).click(); // selected again
        await toggle(); // closes
        await expect(page.getByText('Apple', { exact: true })).toBeHidden();
        await page.getByRole('button', { name: 'Select Carrot' }).click();
    },
    suggest: async page => {
        // Picked with the mouse: the box loses the focus first, so every browser sends its change event (on Enter,
        // Chrome sends one and WebKit not, a difference of the browsers, not of the terminals)
        const box = page.getByLabel('Oracle suggest');
        await box.click();
        await box.pressSequentially('ap', { delay: 30 });
        await page.getByText('Apricot', { exact: true }).click();
        await expect(page.getByText('Selected: Apricot')).toBeVisible(); // the server wrote it back too
        await expect(box).toHaveValue('Apricot');
        await box.fill('');
        await box.pressSequentially('re', { delay: 30 });
        await page.getByText('Red apple', { exact: true }).click();
        await expect(page.getByText('Selected: Red apple')).toBeVisible();
        await expect(box).toHaveValue('Red apple');
    },
    layoutpanel: async page => {
        const layout = page.getByLabel('Oracle layout');
        const a = page.getByRole('button', { name: 'A', exact: true });
        await expect.poll(() => box(a, layout)).toEqual([10, 10, 100, 30]);
        await expect.poll(() => box(page.getByRole('button', { name: 'B', exact: true }), layout)).toEqual([310, 50, 80, 30]);
        const c = await box(page.getByText('C', { exact: true }), layout);
        expect([c[0], c[1] + c[3]]).toEqual([0, 200]); // at the beginning, on the bottom
        await a.click();
        await page.getByRole('button', { name: 'Move A' }).click();
        await expect.poll(() => box(a, layout)).toEqual([200, 10, 100, 30]);
        await a.click();
    },
    dock: async page => {
        const dock = page.getByLabel('Oracle dock');
        const center = page.getByRole('button', { name: 'Center' });
        await expect.poll(() => box(center, dock)).toEqual([80, 50, 320, 250]);
        await center.click();
        await page.getByRole('button', { name: 'Grow north' }).click();
        await expect.poll(() => box(center, dock)).toEqual([80, 100, 320, 200]);
        await center.click();
    },
    split: async (page, sent) => {
        const split = page.getByLabel('Oracle split');
        const center = page.getByRole('button', { name: 'Center pane' });
        await expect.poll(() => box(center, split)).toEqual([108, 0, 292, 300]);
        const west = await page.getByText('West pane').boundingBox();
        const x = west.x + west.width + 4; // the splitter, after the west pane
        await page.mouse.move(x, west.y + 100);
        await page.mouse.down();
        await page.mouse.move(x + 40, west.y + 100, { steps: 4 });
        await page.mouse.up();
        await expect.poll(() => box(center, split)).toEqual([148, 0, 252, 300]);
        await sent(3); // the sizes, 500 ms after the drag
        await center.click();
    },
    tablayout: async page => {
        const tabs = page.getByLabel('Oracle tab layout');
        await expect(page.getByText('TL1 panel')).toBeVisible();
        expect(near((await box(page.getByText('TL1 panel'), tabs))[2], 400)).toBe(true); // the panel fills the width
        await page.getByText('TL 2', { exact: true }).click();
        await expect(page.getByText('TL2 panel')).toBeVisible();
        await page.getByText('TL 3', { exact: true }).click();
        await expect(page.getByText('TL3 panel')).toBeVisible();
        await page.getByRole('button', { name: 'Select TL 1' }).click();
        await expect(page.getByText('TL1 panel')).toBeVisible();
        await page.getByText('TL 3', { exact: true }).click();
        await expect(page.getByText('TL3 panel')).toBeVisible();
        await page.getByRole('button', { name: 'Remove TL 3' }).click();
        await expect(page.getByText('TL 3', { exact: true })).toHaveCount(0);
        await expect(page.getByText('TL1 panel')).toBeVisible();
    },
    stack: async page => {
        const stack = page.getByLabel('Oracle stack');
        await expect(page.getByText('S1 content')).toBeVisible();
        await page.getByText('S2', { exact: true }).click();
        await expect(page.getByText('S2 content')).toBeVisible();
        await expect(page.getByText('S1 content')).toBeHidden();
        // under the two headers, across, down to the third header
        await expect.poll(async () => (await box(page.getByText('S2 content'), stack)).slice(1).every((v, i) => near(v, [60, 300, 210][i]))).toBe(true);
        await page.getByRole('button', { name: 'Show S3' }).click();
        await expect(page.getByText('S3 content')).toBeVisible();
        await page.getByRole('button', { name: 'Remove S3' }).click();
        await expect(page.getByText('S1 content')).toBeVisible();
    },
    headerpanel: async page => {
        const panel = page.getByLabel('Oracle header panel');
        const body = page.getByRole('button', { name: 'Body button' });
        await expect.poll(async () => (await box(body, panel)).slice(0, 2)).toEqual([0, 30]); // under the header, its own size
        await body.click();
    },
    datepicker: async page => {
        const day = text => page.getByText(text, { exact: true });
        const range = (start, end) => expect(page.getByText(`Range: ${start} ${end}`)).toBeVisible();
        await range('2026-02-23', '2026-04-05'); // March 2026, from Monday
        await day('12').click();
        await expect(page.getByText('Picked: 2026-03-12')).toBeVisible();
        await day('20').click({ force: true }); // disabled: nothing
        await expect(day('17')).toHaveClass(/oracle-holiday/);
        await page.getByRole('button', { name: /^(›|Next month)$/ }).click();
        await range('2026-03-30', '2026-05-10');
        await day('15').click();
        await expect(page.getByText('Picked: 2026-04-15')).toBeVisible();
        await day('31').click(); // March's, shown before April: March shows
        await range('2026-02-23', '2026-04-05');
        await expect(page.getByText('Picked: 2026-03-31')).toBeVisible();
        await page.getByRole('button', { name: 'Show May' }).click();
        await range('2026-04-27', '2026-06-07');
        await page.getByRole('button', { name: /^(»|Next year)$/ }).click();
        await range('2027-04-26', '2027-06-06');
    },
    datebox: async page => {
        const box = page.getByLabel('Oracle date');
        const day = text => page.getByText(text, { exact: true });
        await expect(box).toHaveValue('12/03/2026');
        await box.click();
        await day('15').click();
        await expect(page.getByText('Date: 15/03/2026')).toBeVisible();
        await expect(box).toHaveValue('15/03/2026');
        await expect(day('15')).toBeHidden();
        await box.fill('20/04/2026');
        await box.press('Tab');
        await expect(page.getByText('Date: 20/04/2026')).toBeVisible();
        await page.getByRole('button', { name: 'Set 1 May' }).click();
        await expect(box).toHaveValue('01/05/2026');
        await box.click();
        await expect(day('31')).toBeVisible(); // May
        await box.press('Escape');
        await expect(day('31')).toBeHidden();
        await page.getByRole('button', { name: 'Disable date' }).click();
        await expect(box).toBeDisabled();
    },
    richtext: async page => {
        const area = page.getByRole('textbox', { name: 'Oracle rich text' });
        await area.click();
        await page.keyboard.type('Hello');
        await page.getByText('HTML: none').click(); // outside: the area sends its HTML
        await expect(page.getByText('HTML: Hello')).toBeVisible();
        await page.getByRole('button', { name: 'Set HTML' }).click();
        const width = (await area.boundingBox()).width;
        await area.click({ position: { x: width - 5, y: 12 } }); // after the first line
        await page.keyboard.type(' more');
        await page.getByText('HTML: Hello').click();
        await expect(page.getByText('HTML: <b>Bold</b> start more')).toBeVisible();
    },
    mask: async page => {
        const box = page.getByLabel('Oracle mask');
        await expect(box).toHaveValue('__/__/____');
        await box.click();
        // the terminal puts the cursor at the first empty slot once focused (GWT did in a timer)
        await expect.poll(() => box.evaluate(e => e.selectionStart)).toBe(0);
        await box.pressSequentially('12032026', { delay: 30 });
        await expect(box).toHaveValue('12/03/2026');
        await box.press('Backspace');
        await box.press('Backspace');
        await expect(box).toHaveValue('12/03/20__');
        await expect(page.getByText('Mask: 12/03/20__')).toBeVisible();
    },
    frame: async page => {
        const frame = page.frameLocator(`iframe[src*="&${KEY.FRAME_ID}="]`);
        await frame.getByRole('button', { name: 'Inside' }).click();
        await expect(frame.getByRole('button', { name: 'Clicked' })).toBeVisible();
    },
};

function maskNumbers(value) {
    if (typeof value === 'number') return '#';
    if (Array.isArray(value)) return value.map(maskNumbers);
    if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, maskNumbers(v)]));
    return value;
}

function normalize(value, key) {
    if (GEOMETRY.includes(key)) return maskNumbers(value);
    if (MESSAGES.includes(key)) return '<message>';
    if (key === KEY.HANDLER_OPEN && typeof value === 'string') return value.replace(CONTEXT_IN_URL, '$1#');
    // clientX, clientY, x, y of a mouse event; the button and the modifier keys stay
    if (key === KEY.EVENT_INFO && Array.isArray(value)) return value.map((v, i) => (i < 4 ? '#' : v));
    if (Array.isArray(value)) return value.map(v => normalize(v));
    if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, normalize(v, k)]));
    return value;
}

/** The frames a page sent: JSON, normalized, in order. */
function readable(payloads) {
    return payloads.flatMap(payload => {
        if (typeof payload !== 'string') return [{ binary: Buffer.from(payload).toString('base64') }];
        const message = JSON.parse(payload);
        if (DROPPED.some(k => k in message)) return [];
        return [normalize(message)];
    });
}

/** Resolves once no frame was sent for 300 ms. */
async function settle(payloads) {
    const deadline = Date.now() + 5000;
    let count = -1;
    while (count !== payloads.length && Date.now() < deadline) {
        count = payloads.length;
        await new Promise(r => setTimeout(r, 300));
    }
}

test.describe('behaviour oracle', () => {
    for (const [name, steps] of Object.entries(scenarios)) {
        test(name, async ({ page }) => {
            const payloads = [];
            page.on('websocket', ws => ws.on('framesent', frame => payloads.push(frame.payload)));
            await page.goto(BASE_URL + '#' + 'oracle-' + name);
            // frames that count: heartbeats and latency reports, dropped from the trace, are not
            await steps(page, count => expect.poll(() => readable(payloads).length).toBeGreaterThanOrEqual(count));
            await settle(payloads);
            const trace = readable(payloads);
            const file = path.join(TRACES, name + '.json');

            if (RECORD && !fs.existsSync(file)) {
                fs.mkdirSync(TRACES, { recursive: true });
                fs.writeFileSync(file, JSON.stringify(trace, null, 2) + '\n');
                return;
            }
            expect(fs.existsSync(file), 'no reference trace: record it with ORACLE_RECORD=1, then review it').toBe(true);
            const reference = JSON.parse(fs.readFileSync(file, 'utf8'));
            const difference = KNOWN_DIFFERENCES[name];
            expect(trace, difference?.reason).toEqual(difference ? difference.expected(reference) : reference);
        });
    }
});
