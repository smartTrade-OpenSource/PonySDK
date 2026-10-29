// @ts-check
// Routing (docs/decisions/0020-routing.md): the showcase's Routing page, a small application whose screens are routes
// under the page's own token (#routing/orders/42).
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test routing
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

/** The address bar's fragment, decoded: the history token. */
const token = page => decodeURIComponent(new URL(page.url()).hash.replace(/^#/, ''));

test('a deep link opens the page, and the route under its token', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders/3');
  await expect(page.locator('.pony-tab-active')).toHaveText('Routing');
  await expect(page.locator('.route-title')).toHaveText('Order #3, Cobalt Bank');
  expect(token(page)).toBe('routing/orders/3');
});

test('the query filters the list, and a reload keeps what was on screen', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders?status=open');
  await expect(page.locator('.route-row')).toHaveCount(3);
  await expect(page.locator('.route-filter-on')).toHaveText('open');
  await page.reload();
  await expect(page.locator('.pony-tab-active')).toHaveText('Routing');
  await expect(page.locator('.route-row')).toHaveCount(3);
  await expect(page.locator('.route-filter-on')).toHaveText('open');
  expect(token(page)).toBe('routing/orders?status=open');
});

test('opening the tab shows the root of the router and writes the page token', async ({ page }) => {
  await page.goto(BASE_URL);
  await page.locator('.pony-tab', { hasText: 'Routing' }).click();
  await expect(page.locator('.route-title')).toHaveText('Orders');
  expect(token(page)).toBe('routing');
  await page.locator('.route-nav').getByRole('button', { name: 'Orders', exact: true }).click();
  await expect.poll(() => token(page)).toBe('routing/orders'); // written once the server has handled the click
});

test('back and forward walk the routes', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders');
  await expect(page.locator('.route-title')).toHaveText('Orders');
  await page.locator('.route-row', { hasText: 'Delta Treasury' }).getByRole('button', { name: 'View' }).click();
  await expect(page.locator('.route-title')).toHaveText('Order #4, Delta Treasury');
  expect(token(page)).toBe('routing/orders/4');

  await page.goBack();
  await expect(page.locator('.route-title')).toHaveText('Orders');
  expect(token(page)).toBe('routing/orders');
  await expect(page.locator('.pony-tab-active')).toHaveText('Routing');

  await page.goForward();
  await expect(page.locator('.route-title')).toHaveText('Order #4, Delta Treasury');
  expect(token(page)).toBe('routing/orders/4');
});

test('back out of the page goes to the tab before it, and the router stops', async ({ page }) => {
  await page.goto(BASE_URL + '#streams');
  await expect(page.locator('.pony-tab-active')).toHaveText('Streams');
  await page.locator('.pony-tab', { hasText: 'Routing' }).click();
  await expect(page.locator('.route-title')).toHaveText('Orders');
  await page.locator('.route-nav').getByRole('button', { name: 'Admin', exact: true }).click();
  await expect(page.getByText('Access denied')).toBeVisible();
  await page.goBack(); // to the router's root
  await expect(page.locator('.route-title')).toHaveText('Orders');
  await page.goBack(); // to the tab before
  await expect(page.locator('.pony-tab-active')).toHaveText('Streams');
  await expect(page.locator('.route-app')).toHaveCount(0);
});

test('the unsaved-changes guard keeps the user on the form until they leave', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders');
  await page.locator('.route-row', { hasText: 'Borealis Fund' }).getByRole('button', { name: 'View' }).click();
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await expect(page.locator('.route-title')).toHaveText('Edit order #2');
  expect(token(page)).toBe('routing/orders/2/edit');
  const note = page.locator('.route-body input');
  await note.fill('call back on monday');
  await note.press('Tab'); // the value change reaches the server
  await expect(page.getByText('Unsaved changes', { exact: true })).toBeVisible();

  // a click on a link of the application
  await page.locator('.route-nav').getByRole('button', { name: 'Orders', exact: true }).click();
  await expect(page.locator('.route-confirm')).toContainText('Leave for routing/orders?');
  await expect(page.locator('.route-title')).toHaveText('Edit order #2');
  expect(token(page)).toBe('routing/orders/2/edit');

  await page.getByRole('button', { name: 'Stay' }).click();
  await expect(page.locator('.route-confirm')).toBeHidden();
  await expect(page.locator('.route-title')).toHaveText('Edit order #2');

  // the browser's back button is held too, and the address goes back to the form
  await page.goBack();
  await expect(page.locator('.route-confirm')).toContainText('Leave for routing/orders/2?');
  await expect(page.locator('.route-title')).toHaveText('Edit order #2');
  await expect.poll(() => token(page)).toBe('routing/orders/2/edit');

  await page.getByRole('button', { name: 'Leave' }).click();
  await expect(page.locator('.route-title')).toHaveText('Order #2, Borealis Fund');
  expect(token(page)).toBe('routing/orders/2');
});

test('saving the form lets the user leave without asking', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders/2/edit');
  const note = page.locator('.route-body input');
  await note.fill('done');
  await note.press('Tab');
  await expect(page.getByText('Unsaved changes', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Saved', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Back to the order' }).click();
  await expect(page.locator('.route-title')).toHaveText('Order #2, Borealis Fund');
  expect(token(page)).toBe('routing/orders/2');
});

test('a form left after the user confirmed shows the page asked for', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders/5/edit');
  const note = page.locator('.route-body input');
  await note.fill('x');
  await note.press('Tab');
  await expect(page.getByText('Unsaved changes', { exact: true })).toBeVisible();
  await page.locator('.route-nav').getByRole('button', { name: 'Orders', exact: true }).click();
  await page.getByRole('button', { name: 'Leave' }).click();
  await expect(page.locator('.route-title')).toHaveText('Orders');
  expect(token(page)).toBe('routing/orders');
});

test('the admin route is denied until the role is granted', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders');
  await expect(page.locator('.route-title')).toHaveText('Orders');
  await page.locator('.route-nav').getByRole('button', { name: 'Admin', exact: true }).click();
  await expect(page.locator('.route-body')).toHaveText('Access denied: routing/admin');
  expect(token(page)).toBe('routing/admin');

  await page.getByRole('button', { name: 'Role: trader' }).click();
  await expect(page.getByRole('button', { name: 'Role: admin' })).toBeVisible();
  await page.locator('.route-nav').getByRole('button', { name: 'Orders', exact: true }).click();
  await page.locator('.route-nav').getByRole('button', { name: 'Admin', exact: true }).click();
  await expect(page.locator('.route-title')).toHaveText('Admin console');
});

test('a deep link to the admin route is denied too', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/admin');
  await expect(page.locator('.route-body')).toHaveText('Access denied: routing/admin');
  await expect(page.locator('.pony-tab-active')).toHaveText('Routing');
});

test('a path that matches nothing shows the not-found view, from a link and from the address', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders');
  await expect(page.locator('.route-title')).toHaveText('Orders');
  await page.getByRole('link', { name: 'A page that does not exist' }).click();
  await expect(page.locator('.route-title')).toHaveText('Page not found');
  await expect(page.getByText('Nothing at #routing/nowhere')).toBeVisible();
  await page.getByRole('button', { name: 'Orders', exact: true }).last().click();
  await expect(page.locator('.route-title')).toHaveText('Orders');

  await page.goto(BASE_URL + '#routing/orders/1/unknown');
  await expect(page.locator('.route-title')).toHaveText('Page not found');
});

test('an order that does not exist is the view\'s own message, not a not-found', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders/abc');
  await expect(page.locator('.route-title')).toHaveText('No order abc');
  await page.goto(BASE_URL + '#routing/orders/99');
  await expect(page.locator('.route-title')).toHaveText('No order 99');
});

test('the showcase navigation still works around the router', async ({ page }) => {
  await page.goto(BASE_URL + '#routing/orders/3');
  await expect(page.locator('.route-title')).toHaveText('Order #3, Cobalt Bank');
  await page.locator('.pony-tab', { hasText: 'Overview' }).click();
  await expect(page.locator('.pony-tab-active')).toHaveText('Overview');
  expect(token(page)).toBe('overview');
  await page.locator('.pony-tab', { hasText: 'Routing' }).click();
  await expect(page.locator('.route-title')).toHaveText('Orders');
  expect(token(page)).toBe('routing');
});
