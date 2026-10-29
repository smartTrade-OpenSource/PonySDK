// @ts-check
// Playwright's test and expect, plus the terminal's coverage when PW_COVERAGE names a directory: each Chromium page
// records the V8 coverage of the terminal's scripts, written there for coverage-report.js to map onto the sources.
//
// V8 counts per renderer process: the pages of one process share its isolate, and what one CDP session takes holds
// the scripts of all of them. A session that goes, with its page, puts the whole isolate back to best-effort coverage,
// which drops the counts of the other pages: so one session covers each process, started by the first page found in
// it, and a page is checked again after each load and each page closed. The coverage is taken, never stopped: before
// a page closes or navigates (a reload, a new document), after a page closed, and at the end of the test.
const base = require('@playwright/test');
const { mkdirSync, writeFileSync } = require('node:fs');
const { join } = require('node:path');
const { randomUUID } = require('node:crypto');

const DIR = process.env.PW_COVERAGE;
const TERMINAL_SCRIPT = /\/(terminal|legacy|chunks\/[\w-]+)\.js(\?|$)/;
const MARK = '__ponyCoverageMark';

/** The coverage of the pages of one test: its sessions, one per process, and one operation on them at a time. */
class Coverage {
  constructor(context) {
    this.context = context;
    /** @type {Map<import('@playwright/test').Page, import('@playwright/test').CDPSession>} */
    this.sessions = new Map();
    this.queue = Promise.resolve();
  }

  /** Runs one operation after the others: a take resets the counts a check reads. */
  serial(operation) {
    return this.queue = this.queue.then(operation).catch(() => {}); // a page gone meanwhile
  }

  /** Writes what the sessions took; true when one of them ran the mark. */
  async takeAll() {
    let marked = false;
    for (const [page, session] of [...this.sessions]) {
      let result;
      try {
        ({ result } = await session.send('Profiler.takePreciseCoverage'));
      } catch {
        this.sessions.delete(page);
        continue;
      }
      const entries = result.filter(entry => TERMINAL_SCRIPT.test(entry.url)).map(({ url, functions }) => ({ url, functions }));
      if (entries.length > 0) writeFileSync(join(DIR, `${randomUUID()}.json`), JSON.stringify(entries));
      marked ||= result.some(entry => entry.functions.some(f => f.functionName === MARK && f.ranges[0].count > 0));
    }
    return marked;
  }

  take() {
    return this.serial(() => this.takeAll());
  }

  async start(page) {
    const session = await this.context.newCDPSession(page);
    await session.send('Profiler.enable');
    await session.send('Profiler.startPreciseCoverage', { callCount: true, detailed: true });
    this.sessions.set(page, session);
  }

  /** Starts a session for the page unless one of the sessions already covers its process. */
  check(page) {
    return this.serial(async () => {
      if (page.isClosed() || this.sessions.has(page)) return;
      // A page held by a dialog runs nothing: checked again at its next load
      const marked = await Promise.race([page.evaluate(function __ponyCoverageMark() {}).then(() => true),
        new Promise(resolve => setTimeout(resolve, 2000, false))]);
      if (marked && !await this.takeAll()) await this.start(page);
    });
  }

  /**
   * A page of its own gets its session at once, before it loads anything; a page another opened is in its opener's
   * process until its first load tells.
   */
  cover(page) {
    const close = page.close.bind(page);
    page.close = async options => { await this.take(); return close(options); };
    page.on('request', request => {
      if (request.isNavigationRequest() && request.frame() === page.mainFrame()) this.take();
    });
    page.on('load', () => this.check(page));
    page.on('close', () => {
      this.sessions.delete(page);
      for (const other of this.context.pages()) this.check(other);
    });
    this.serial(async () => {
      if (await page.opener() === null) await this.start(page);
    });
  }
}

const test = !DIR ? base.test : base.test.extend({
  coverage: [async ({ context, browserName }, use) => {
    if (browserName !== 'chromium') return use(undefined);
    mkdirSync(DIR, { recursive: true });
    const coverage = new Coverage(context);
    context.on('page', page => coverage.cover(page));
    await use(undefined);
    await coverage.take();
  }, { auto: true }],
});

module.exports = { test, expect: base.expect };
