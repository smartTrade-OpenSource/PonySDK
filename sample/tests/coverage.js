// @ts-check
// Playwright's test and expect, plus the terminal's coverage when PW_COVERAGE names a directory: each Chromium page
// records the V8 coverage of the terminal's scripts, written there for coverage-report.js to map onto the sources.
const base = require('@playwright/test');
const { mkdirSync, writeFileSync } = require('node:fs');
const { join } = require('node:path');
const { randomUUID } = require('node:crypto');

const DIR = process.env.PW_COVERAGE;
const TERMINAL_SCRIPT = /\/(terminal|legacy|chunks\/[\w-]+)\.js(\?|$)/;

function start(page) {
  const close = page.close.bind(page);
  page.close = async options => { await stop(page); return close(options); };
  return page.coverage.startJSCoverage({ resetOnNavigation: false, reportAnonymousScripts: false });
}

async function stop(page) {
  if (page.isClosed() || page.__coverageStopped) return;
  page.__coverageStopped = true;
  const entries = (await page.coverage.stopJSCoverage()).filter(entry => TERMINAL_SCRIPT.test(entry.url));
  if (entries.length > 0) writeFileSync(join(DIR, `${randomUUID()}.json`), JSON.stringify(entries));
}

const test = !DIR ? base.test : base.test.extend({
  coverage: [async ({ context, browserName }, use) => {
    if (browserName !== 'chromium') return use(undefined);
    mkdirSync(DIR, { recursive: true });
    const pages = [];
    context.on('page', page => { pages.push(page); start(page).catch(() => {}); });
    await use(undefined);
    for (const page of pages) await stop(page).catch(() => {});
  }, { auto: true }],
});

module.exports = { test, expect: base.expect };
