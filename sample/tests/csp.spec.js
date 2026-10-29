// @ts-check
// The showcase under the Content-Security-Policy PonySDK builds (ADR-0015): no inline script but the hashed one, no
// eval, and nothing the page does is blocked. The smoke test visits every tab; a violation is a console error there.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test csp
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/** Records the page's CSP violations from its first script on. */
async function recordViolations(page) {
  await page.addInitScript(() => {
    window.__violations = [];
    document.addEventListener('securitypolicyviolation',
      e => window.__violations.push(e.violatedDirective + ' ' + e.blockedURI));
  });
}

test('the page forbids inline scripts and eval, and is framed by its embedding sites only', async ({ page }) => {
  const response = await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
  const policy = response.headers()['content-security-policy'];
  expect(policy).toMatch(/^script-src 'self' 'sha256-[A-Za-z0-9+/=]+';/);
  expect(policy).not.toContain('unsafe-inline');
  expect(policy).not.toContain('unsafe-eval');
  expect(policy).toContain("object-src 'none'; base-uri 'self'; frame-ancestors 'self' http://");
  expect(response.headers()['x-content-type-options']).toBe('nosniff');
});

test('the showcase calls its JavaScript by name, with nothing blocked', async ({ page }) => {
  await recordViolations(page);
  await page.goto(BASE_URL + '#misc', { waitUntil: 'domcontentloaded' });
  // The early socket ran (its hash is allowed): the terminal adopted it
  await expect(page.getByRole('button', { name: 'Run JS alert' })).toBeVisible({ timeout: 10000 });
  // PScript.call(main, "showcaseFavicon")
  await expect(page.locator('link[rel="icon"][type="image/svg+xml"]')).toHaveCount(1);

  const dialog = new Promise(resolve => page.once('dialog', d => { resolve(d.message()); d.accept(); }));
  await page.getByRole('button', { name: 'Run JS alert' }).click();
  expect(await dialog).toBe('Hello from PScript!');

  expect(await page.evaluate(() => window.__violations)).toEqual([]);
});

// The drop-down list scrolls its items as an infinite scroll (ponysdk.js): a resize schedules its next view
test('an open drop-down list evaluates no string when it resizes', async ({ page }) => {
  await recordViolations(page);
  await page.goto(BASE_URL + '#theme-preview', { waitUntil: 'domcontentloaded' });
  const currency = page.getByRole('button', { name: /Currency/ });
  await expect(currency).toBeVisible({ timeout: 10000 });
  await currency.click();
  await page.setViewportSize({ width: 1000, height: 600 }); // the list resizes
  await page.waitForTimeout(500); // past the 150 ms after a resize

  expect(await page.evaluate(() => window.__violations)).toEqual([]);
});

test('a script the policy does not list is refused', async ({ page }) => {
  await recordViolations(page);
  await page.goto(BASE_URL + '#misc', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Run JS alert' })).toBeVisible({ timeout: 10000 });
  // An injected script from a source the policy does not list, as an HTML injection would add
  const outcome = await page.evaluate(() => new Promise(resolve => {
    const script = document.createElement('script');
    script.src = 'data:text/javascript,window.__injected = true';
    script.onerror = () => resolve('script refused');
    script.onload = () => resolve('script ran');
    document.head.appendChild(script);
  }));
  expect(outcome).toBe('script refused');
  expect(await page.evaluate(() => window.__violations.length)).toBeGreaterThan(0);
});
