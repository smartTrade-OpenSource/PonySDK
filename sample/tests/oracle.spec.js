// @ts-check
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

/*
 * Behaviour oracle of the terminal (docs/decisions/0002-javascript-terminal.md, section 8).
 *
 * A scenario drives the widget of an oracle page (OraclePages.java) and records the frames the client sends. The DOM
 * differs between terminals by design; what the server receives must not. The traces under oracle-traces/ were
 * recorded with the GWT terminal; a terminal passes when it sends the same, once normalized.
 *
 *   ./gradlew :sample:runShowcase [-Pterminal=typescript]
 *   BASE_URL=... npx playwright test oracle                  check
 *   ORACLE_RECORD=1 BASE_URL=... npx playwright test oracle  record (GWT terminal only)
 *
 * Selectors go by role and accessible name, so that they hold on both DOMs.
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
            await steps(page);
            await settle(payloads);
            const trace = readable(payloads);
            const file = path.join(TRACES, name + '.json');

            if (RECORD) {
                fs.mkdirSync(TRACES, { recursive: true });
                fs.writeFileSync(file, JSON.stringify(trace, null, 2) + '\n');
                return;
            }
            expect(fs.existsSync(file), 'no reference trace: record it with ORACLE_RECORD=1 on the GWT terminal').toBe(true);
            expect(trace).toEqual(JSON.parse(fs.readFileSync(file, 'utf8')));
        });
    }
});
