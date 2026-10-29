// @ts-check
// The locale of a page (docs/decisions/0022-locale.md): from the browser's languages, or the visitor's choice kept in a
// cookie; its texts, numbers and dates; the direction of the page for Arabic; the date picker in the page's language.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test i18n
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
const COOKIE = 'ponysdk-locale';
/** The portal of the embedding demo, on the port after the showcase's. */
const PORTAL_URL = (() => {
  const url = new URL(BASE_URL);
  return `${url.protocol}//${url.hostname}:${Number(url.port) + 1}/portal.html?app=${encodeURIComponent(BASE_URL)}`;
})();

async function open(page) {
  await page.goto(BASE_URL + '#i18n', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.i18n-title')).toBeVisible({ timeout: 15000 });
}

/** The page's root, as the browser reads it. */
function root(page) {
  return page.evaluate(() => ({
    lang: document.documentElement.getAttribute('lang'), dir: document.documentElement.getAttribute('dir'),
    direction: getComputedStyle(document.documentElement).direction,
  }));
}

/** A click on a language of the switch: the page reloads at its address, in that language. */
async function switchTo(page, name) {
  await Promise.all([page.waitForEvent('load', { timeout: 15000 }), page.getByRole('button', { name }).click()]);
  expect(new URL(page.url()).hash).toBe('#i18n');
  await expect(page.locator('.i18n-title')).toBeVisible({ timeout: 15000 });
}

test.describe('the browser\'s language', () => {
  test.use({ locale: 'fr-FR' });

  test('picks the language of the page: its texts, its numbers, its dates', async ({ page }) => {
    await open(page);
    expect(await root(page)).toEqual({ lang: 'fr', dir: 'ltr', direction: 'ltr' });
    await expect(page.locator('.i18n-title')).toHaveText('Guichet des commandes');
    await expect(page.locator('.i18n-greeting')).toHaveText('Bienvenue, Nicolas');
    await expect(page.locator('.i18n-orders')).toHaveText('3 commandes ouvertes');
    await expect(page.locator('.i18n-volume')).toHaveText('Volume : 1\u202f234\u202f567,891');
    await expect(page.locator('.i18n-amount')).toContainText('1 234,50 €');
    await expect(page.locator('.i18n-today')).toHaveText(/^Aujourd'hui, nous sommes le jeudi 29 octobre 2026$/);
    await expect(page.locator('.i18n-ok')).toHaveText('OK'); // the framework's own messages
  });
});

test.describe('the page of a browser in English', () => {
  test.use({ locale: 'en-US' });

  test('the first supported locale is British English, its week starts on Monday', async ({ page }) => {
    await open(page);
    expect(await root(page)).toEqual({ lang: 'en-GB', dir: 'ltr', direction: 'ltr' });
    await expect(page.locator('.i18n-title')).toHaveText('Order desk');
    await expect(page.locator('.i18n-volume')).toHaveText('Volume: 1,234,567.891');
    await expect(page.locator('.i18n-today')).toHaveText(/^Today is Thursday,? 29 October 2026$/);
    await expect(page.locator('.i18n-ok')).toHaveText('Ok');
    const heads = page.locator('.i18n-screen .pony-DatePicker th');
    await expect(heads.first()).toHaveText('M');
    await expect(heads.nth(6)).toHaveText('S');
  });

  test('the switch changes the language, the direction and the date picker, and the page keeps its address', async ({ page, context }) => {
    await open(page);

    await switchTo(page, 'Français');
    expect(await root(page)).toEqual({ lang: 'fr', dir: 'ltr', direction: 'ltr' });
    await expect(page.locator('.i18n-title')).toHaveText('Guichet des commandes');
    await expect(page.locator('.i18n-screen .pony-DatePicker-month')).toHaveText(/^\S+ \d{4}$/);
    await expect(page.locator('.i18n-screen .pony-DatePicker th').first()).toHaveText('L');

    await switchTo(page, 'العربية');
    expect(await root(page)).toEqual({ lang: 'ar', dir: 'rtl', direction: 'rtl' });
    await expect(page.locator('.i18n-title')).toHaveText('مكتب الطلبات');
    await expect(page.locator('.i18n-ok')).toHaveText('موافق');
    await expect(page.locator('.i18n-screen .pony-DatePicker-month')).toContainText(/[؀-ۿ]/);
    // The whole page is right to left: the sidebar, first at the left in English, is at the right
    const nav = await page.locator('.pony-nav').boundingBox();
    expect(nav.x).toBeGreaterThan(page.viewportSize().width / 2);

    await switchTo(page, 'English');
    expect(await root(page)).toEqual({ lang: 'en-GB', dir: 'ltr', direction: 'ltr' });
    await expect(page.locator('.i18n-title')).toHaveText('Order desk');

    // The choice is a cookie of the whole site, kept for a year
    const cookie = (await context.cookies()).find(c => c.name === COOKIE);
    expect(cookie).toMatchObject({ value: 'en-GB', path: '/', sameSite: 'Lax' });
    expect(cookie.expires * 1000 - Date.now()).toBeGreaterThan(360 * 86_400_000);
  });

  test('a reload, or another page, finds the visitor\'s choice, whatever the browser says', async ({ page, context }) => {
    await open(page);
    await switchTo(page, 'العربية');

    await page.reload({ waitUntil: 'domcontentloaded' });
    await expect(page.locator('.i18n-title')).toHaveText('مكتب الطلبات', { timeout: 15000 });
    const other = await context.newPage();
    await open(other);
    expect(await root(other)).toEqual({ lang: 'ar', dir: 'rtl', direction: 'rtl' });
    await expect(other.locator('.i18n-title')).toHaveText('مكتب الطلبات');
  });

  test('the page is served in the right direction already: no first paint in the wrong one', async ({ context, request }) => {
    await context.addCookies([{ name: COOKIE, value: 'ar', url: BASE_URL }]);
    const arabic = await context.request.get(BASE_URL);
    expect(await arabic.text()).toContain('<html lang="ar" dir="rtl">');
    expect(arabic.headers()['vary']).toBe('Accept-Language, Cookie');

    const french = await request.get(BASE_URL, { headers: { 'Accept-Language': 'de;q=0.9, fr-CH;q=0.8, en;q=0.5' } });
    expect(await french.text()).toContain('<html lang="fr" dir="ltr">');
    const unknown = await request.get(BASE_URL, { headers: { 'Accept-Language': 'ja' } });
    expect(await unknown.text()).toContain('<html lang="en-GB" dir="ltr">');
    const garbage = await request.get(BASE_URL, { headers: { 'Accept-Language': 'fr;q=oops' } });
    expect(await garbage.text()).toContain('<html lang="en-GB" dir="ltr">');
  });

  test('a plural is a choice in the text, by the rules the text writes', async ({ page }) => {
    await open(page);
    const orders = page.locator('.i18n-orders');
    const less = page.getByRole('button', { name: '-1' });
    const more = page.getByRole('button', { name: '+1' });
    await expect(orders).toHaveText('3 open orders');
    await less.click();
    await less.click();
    await expect(orders).toHaveText('One open order');
    await less.click();
    await expect(orders).toHaveText('No open orders');
    await less.click();
    await expect(orders).toHaveText('No open orders');
    await more.click();
    await expect(orders).toHaveText('One open order');

    await switchTo(page, 'Français');
    await expect(orders).toHaveText('3 commandes ouvertes');
    await less.click();
    await less.click();
    await expect(orders).toHaveText('Une commande ouverte');

    await switchTo(page, 'العربية');
    await expect(orders).toHaveText('3 طلبات مفتوحة');
    await less.click();
    await expect(orders).toHaveText('طلبان مفتوحان');
    await less.click();
    await expect(orders).toHaveText('طلب مفتوح واحد');
    await less.click();
    await expect(orders).toHaveText('لا توجد طلبات مفتوحة');
  });

  test('a date box reads and writes the names of the page\'s language, and its picker follows', async ({ page }) => {
    await open(page);
    const box = page.locator('.i18n-datebox');
    const picked = page.locator('.i18n-picked');
    await expect(picked).toHaveText('No delivery date');

    await box.fill('5 March 2026');
    await box.press('Tab');
    await expect(picked).toHaveText('Delivery on 5 March 2026');
    await box.click();
    const popup = page.locator('.pony-DateBoxPopup');
    await expect(popup.locator('.pony-DatePicker-month')).toHaveText('March 2026');
    await expect(popup.locator('[aria-selected=true]')).toHaveAttribute('aria-label', 'Thursday 5 March 2026');
    await box.press('Escape');

    await switchTo(page, 'Français');
    await box.fill('5 mars 2026');
    await box.press('Tab');
    await expect(picked).toHaveText('Livraison le 5 mars 2026');
    await box.click();
    await expect(popup.locator('.pony-DatePicker-month')).toHaveText('mars 2026');
    await expect(popup.locator('[aria-selected=true]')).toHaveAttribute('aria-label', 'jeudi 5 mars 2026');
    await expect(popup.locator('th').first()).toHaveText('L');
    await box.press('Escape');
    // a name nobody reads is no date (the browser's own reading of a text stands in, as GWT's date box does)
    await box.fill('5 mars-ish 2026');
    await box.press('Tab');
    await expect(box).toHaveAttribute('aria-invalid', 'true');
  });

  test('in Arabic the date box takes Arabic digits, its popup opens under the field\'s right edge, and Left goes to the next day', async ({ page }) => {
    await open(page);
    await switchTo(page, 'العربية');
    const box = page.locator('.i18n-datebox');
    const picked = page.locator('.i18n-picked');
    await box.fill('٥ مارس ٢٠٢٦');
    await box.press('Tab');
    await expect(picked).toHaveText('التسليم في 5 مارس 2026');

    await box.click();
    const popup = page.locator('.pony-DateBoxPopup');
    await expect(popup.locator('.pony-DatePicker-month')).toContainText('مارس');
    const field = await box.boundingBox();
    const shown = await popup.boundingBox();
    expect(Math.abs(shown.x + shown.width - (field.x + field.width))).toBeLessThan(2);
    expect(shown.x).toBeGreaterThanOrEqual(0);

    const day = popup.locator('[aria-selected=true]');
    await day.focus();
    await page.keyboard.press('ArrowLeft');
    await expect(popup.locator('[role=gridcell]:focus')).toHaveText('6');
    await page.keyboard.press('ArrowRight');
    await page.keyboard.press('ArrowRight');
    await expect(popup.locator('[role=gridcell]:focus')).toHaveText('4');
    // the buttons that move the month are turned about
    expect(await popup.locator('.pony-DatePicker-previous').evaluate(e => getComputedStyle(e).transform)).toBe('matrix(-1, 0, 0, 1, 0, 0)');
    await box.press('Escape');

    await switchTo(page, 'English');
    await box.click();
    expect(Math.abs((await popup.boundingBox()).x - (await box.boundingBox()).x)).toBeLessThan(2);
    expect(await popup.locator('.pony-DatePicker-previous').evaluate(e => getComputedStyle(e).transform)).toBe('none');
    const left = popup.locator('[role=gridcell]:not([data-filler])').nth(4);
    await left.focus();
    await page.keyboard.press('ArrowLeft');
    await expect(popup.locator('[role=gridcell]:focus')).toHaveText('4');
  });

  test('the document element follows the body, as the server wrote it: lang and dir, set and removed', async ({ page }) => {
    await open(page);
    await page.evaluate(() => document.body.setAttribute('dir', 'rtl'));
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
    await page.evaluate(() => {
      document.body.removeAttribute('dir');
      document.body.removeAttribute('lang');
    });
    await expect(page.locator('html')).not.toHaveAttribute('dir');
    await expect(page.locator('html')).not.toHaveAttribute('lang');
  });

  test('a language the application does not support is refused: nothing changes', async ({ page, context }) => {
    await context.addCookies([{ name: COOKIE, value: 'de', url: BASE_URL }]);
    await open(page);
    expect(await root(page)).toMatchObject({ lang: 'en-GB' });
    await expect(page.locator('.i18n-title')).toHaveText('Order desk');
  });
});

test.describe('an application embedded in another page', () => {
  test.use({ locale: 'ar-EG' });

  test('has its own language and direction on its container, and leaves the host page\'s alone', async ({ page }) => {
    test.skip(!(await page.request.get(PORTAL_URL).then(r => r.ok(), () => false)), 'no portal at ' + PORTAL_URL);
    await page.goto(PORTAL_URL);
    const container = page.locator('#ticket-1 [data-pony-body]');
    await expect(container).toHaveAttribute('lang', 'ar', { timeout: 15000 });
    await expect(container).toHaveAttribute('dir', 'rtl');
    expect(await page.evaluate(() => document.documentElement.getAttribute('dir'))).toBeNull();
  });
});
