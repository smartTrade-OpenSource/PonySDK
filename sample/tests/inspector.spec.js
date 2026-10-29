// @ts-check
// The inspector of a server in debug mode (ApplicationConfiguration.setDebugMode): the showcase started with -Pdebug.
//   ./gradlew :sample:runShowcase -Pdebug    then    BASE_URL=http://localhost:8081/sample/ npx playwright test inspector
// Without debug mode the widgets carry no data-pony attribute and these tests are skipped, unless PONY_DEBUG is set.
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

test.beforeEach(async ({ page }) => {
  await page.goto(BASE_URL + '#inputs', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Click me' })).toBeVisible({ timeout: 10000 });
  const debug = await page.locator('[data-pony]').count();
  // PONY_DEBUG: the run started the server in debug mode, these tests must not be skipped (the CI's coverage job)
  if (process.env.PONY_DEBUG) expect(debug, 'the server is not in debug mode').toBeGreaterThan(0);
  else test.skip(debug === 0, 'the server is not in debug mode');
});

test('each widget names its Java class and the line of the application that created it', async ({ page }) => {
  const button = page.getByRole('button', { name: 'Click me' });
  await expect(button).toHaveAttribute('data-pony', /^PButton InputsPage\.java:\d+$/);
  await expect(button).toHaveAttribute('data-pony-id', /^\d+$/);
});

test('the panel counts the bytes by class and by object, and the events sent', async ({ page }) => {
  const inspector = page.locator('pony-inspector');
  await expect(inspector.getByRole('button', { name: 'Pony inspector' })).toBeVisible();
  await page.keyboard.press('Alt+Shift+KeyI');
  const panel = inspector.getByRole('region', { name: 'Pony inspector' });
  await expect(panel).toBeVisible();
  await expect(panel).toContainText('Bytes by class');
  await expect(panel).toContainText('PButton');

  await page.getByRole('button', { name: 'Click me' }).click();
  await expect(page.getByText('server handled click #1')).toBeVisible();
  await expect(panel).toContainText(/messages out [1-9]/);

  await panel.getByRole('button', { name: 'Close' }).click();
  await expect(panel).toBeHidden();
});

test('pick names the element under the pointer; a click selects it without reaching the application', async ({ page }) => {
  const inspector = page.locator('pony-inspector');
  await inspector.getByRole('button', { name: 'Pony inspector' }).click();
  const panel = inspector.getByRole('region', { name: 'Pony inspector' });
  await panel.getByRole('button', { name: 'Pick' }).click();

  const button = page.getByRole('button', { name: 'Click me' });
  const id = await button.getAttribute('data-pony-id');
  await button.hover();
  await expect(inspector.locator('.label')).toContainText(new RegExp(`PButton InputsPage\\.java:\\d+ · #${id}`));
  await button.click();
  await expect(panel.locator('.selected')).toContainText(`#${id} PButton`);
  await expect(page.getByText('server handled click #1')).toBeHidden(); // picked, not clicked
  await expect(panel.getByRole('button', { name: 'Pick' })).toHaveAttribute('aria-pressed', 'false');
});

test('picking stops on Escape; over the inspector, or after a reset, it names what it can', async ({ page }) => {
  const inspector = page.locator('pony-inspector');
  const badge = inspector.getByRole('button', { name: 'Pony inspector' });
  await badge.click();
  const panel = inspector.getByRole('region', { name: 'Pony inspector' });
  const pick = panel.getByRole('button', { name: 'Pick' });
  const label = inspector.locator('.label');

  await pick.click();
  await page.keyboard.press('Escape');
  await expect(pick).toHaveAttribute('aria-pressed', 'false');

  await pick.click();
  const button = page.getByRole('button', { name: 'Click me' });
  await button.hover();
  await expect(label).toBeVisible();
  await badge.hover(); // the inspector itself is no widget of the page
  await expect(label).toBeHidden();

  // an element at the top of the page: its name goes below it
  const top = await page.evaluate(() => [...document.querySelectorAll('[data-pony-id]')]
    .map(e => e.getBoundingClientRect()).find(r => r.top <= 24 && r.height > 0 && r.height < 60 && r.width > 0 && r.width < 400));
  expect(top).toBeTruthy();
  await page.mouse.move(top.left + top.width / 2, top.top + top.height / 2);
  await expect(label).toBeVisible();
  expect((await label.boundingBox()).y).toBeGreaterThan(top.top);

  await page.keyboard.press('Escape');
  await panel.getByRole('button', { name: 'Reset' }).click();
  await expect(panel).toContainText('frames in 0');
  await pick.click();
  const id = await button.getAttribute('data-pony-id');
  await button.hover();
  await expect(label).toHaveText(new RegExp(`· #${id}$`)); // nothing counted since the reset
  await button.click();
  await expect(panel.locator('.selected')).toHaveText(new RegExp(`#${id} PButton InputsPage\\.java:\\d+$`));
});

test('an object that is no widget is counted as an object; outside any widget, picking names nothing', async ({ page }) => {
  await page.evaluate(() => { location.hash = '#fdc3'; }); // its FDC3 addon: a terminal object, no element
  await page.keyboard.press('Alt+Shift+KeyI');
  const inspector = page.locator('pony-inspector');
  const panel = inspector.getByRole('region', { name: 'Pony inspector' });
  await expect(panel.locator('table').first()).toContainText('object', { timeout: 10000 });

  await panel.getByRole('button', { name: 'Pick' }).click();
  await page.evaluate(() => {
    document.documentElement.dispatchEvent(new MouseEvent('mousemove', { bubbles: true }));
    document.documentElement.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
  });
  await expect(inspector.locator('.label')).toBeHidden();
  await expect(panel.getByRole('button', { name: 'Pick' })).toHaveAttribute('aria-pressed', 'false');
});
