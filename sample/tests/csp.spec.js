// @ts-check
const { test, expect } = require('@playwright/test');

test.describe('Strict Content Security Policy', () => {

  /** @type {string[]} */
  let consoleViolations;
  /** @type {string[]} */
  let pageErrors;

  test.beforeEach(async ({ page }) => {
    consoleViolations = [];
    pageErrors = [];

    // The GWT terminal runs inside a same-origin about:blank iframe whose events the main
    // document does not see, so violations are collected two ways. First, the browser reports
    // every violation on the console, whatever frame raised it.
    page.on('console', (msg) => {
      if (/Content Security Policy|Refused to/i.test(msg.text())) consoleViolations.push(msg.text());
    });
    page.on('pageerror', (error) => pageErrors.push(String(error)));

    // Second, a listener on the main document.
    await page.addInitScript(() => {
      // @ts-ignore
      window.__cspViolations = [];
      document.addEventListener('securitypolicyviolation', (e) => {
        // @ts-ignore
        window.__cspViolations.push({ directive: e.violatedDirective, blocked: e.blockedURI, sample: e.sample });
      });
    });
  });

  test('serves the strict policy', async ({ page }) => {
    const response = await page.goto('./');
    expect(response.headers()['content-security-policy']).toContain("script-src 'self'");
  });

  test('renders under a strict policy with no violation', async ({ page }) => {
    await page.goto('./');
    await page.waitForSelector('.csp-ready', { timeout: 15000 });

    await expect(page.locator('.csp-script-result')).toHaveText('echo:ping');
    await expect(page.locator('.csp-checkbox input')).toBeChecked();
    await expect(page.locator('.csp-functional-label')).toHaveText('A-B');
    for (const selector of ['.csp-clipped-image', '.csp-file-upload', '.csp-rich-text', '.csp-disclosure', '.csp-tree']) {
      await expect(page.locator(selector).first()).toBeVisible();
    }

    // Opening the list box renders its items through the InfiniteScrollAddon.
    await page.locator('.csp-listbox').click();

    // Leave time for violations raised late.
    await page.waitForTimeout(1000);

    const documentViolations = await page.evaluate(() => {
      // @ts-ignore
      return window.__cspViolations;
    });
    expect(consoleViolations, `console violations: ${JSON.stringify(consoleViolations, null, 2)}`).toEqual([]);
    expect(documentViolations, `securitypolicyviolation events: ${JSON.stringify(documentViolations, null, 2)}`).toEqual([]);
    expect(pageErrors, `page errors: ${JSON.stringify(pageErrors, null, 2)}`).toEqual([]);
  });

});
