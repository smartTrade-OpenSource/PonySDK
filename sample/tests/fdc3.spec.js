// @ts-check
// FDC3 (docs/decisions/0014-fdc3.md): the showcase's FDC3 page and the monitor, a plain JavaScript application, talk
// through the sample's demonstration desktop agent, as two applications of a desktop do through theirs.
//   ./gradlew :sample:runShowcase    then    BASE_URL='http://[::1]:8081/sample/' npx playwright test fdc3
const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';

async function showcase(context) {
  const page = await context.newPage();
  await page.goto(BASE_URL + '#fdc3');
  await expect(page.getByText(/PonySDK demo agent 1\.0, FDC3 2\.1/)).toBeVisible({ timeout: 10000 });
  await expect(page.locator('.fdc3-channel-on')).toHaveText('Channel 1');
  return page;
}

async function monitor(context) {
  const page = await context.newPage();
  await page.goto(BASE_URL + 'fdc3-monitor.html');
  await expect(page.locator('#channels [aria-pressed="true"]')).toHaveText('Channel 1');
  return page;
}

test('a broadcast goes both ways on the channel', async ({ context }) => {
  const pony = await showcase(context);
  const plain = await monitor(context);

  await pony.getByRole('button', { name: 'NVDA' }).click();
  await expect(plain.locator('#contexts li').first()).toHaveText('fdc3.instrument NVDA from PonySDK Showcase');

  await plain.getByRole('button', { name: 'Broadcast GOOG' }).click();
  await expect(pony.getByText('Following GOOG from the channel')).toBeVisible();
});

test('another channel does not hear it, and a channel joined gives its current context', async ({ context }) => {
  const pony = await showcase(context);
  const plain = await monitor(context);
  await plain.getByRole('button', { name: 'Channel 2' }).click();
  await pony.getByRole('button', { name: 'AAPL' }).click();
  await plain.waitForTimeout(300);
  await expect(plain.locator('#contexts li')).toHaveCount(0);

  await plain.getByRole('button', { name: 'Channel 1' }).click();
  await expect(plain.locator('#contexts li').first()).toHaveText('fdc3.instrument AAPL from PonySDK Showcase');
});

test('an intent raised by the monitor is handled in Java, and its result goes back', async ({ context }) => {
  const pony = await showcase(context);
  const plain = await monitor(context);
  await plain.getByRole('button', { name: 'Raise ViewInstrument for TSLA' }).click();
  await expect(plain.locator('#result')).toHaveText('Handled by PonySDK Showcase, result {"type":"ponysdk.shown","ticker":"TSLA"}');
  await expect(pony.getByText('ViewInstrument TSLA, raised by another application')).toBeVisible();
});

test('two showcase windows: one raises the intent, the other handles it', async ({ context }) => {
  const first = await showcase(context);
  const second = await showcase(context);
  await first.getByRole('button', { name: 'Raise ViewInstrument for MSFT' }).click();
  await expect(first.locator('.fdc3-raised')).toHaveText('Handled by PonySDK Showcase, result {"type":"ponysdk.shown","ticker":"MSFT"}');
  await expect(second.getByText('ViewInstrument MSFT, raised by another application')).toBeVisible();
});

test('alone, the intent finds no application', async ({ context }) => {
  const pony = await showcase(context);
  await pony.getByRole('button', { name: 'Raise ViewInstrument for MSFT' }).click();
  await expect(pony.locator('.fdc3-raised')).toHaveText('Not handled: NoAppsFound');
});

test('a page without a desktop agent says so', async ({ page }) => {
  await page.addInitScript(() => { delete window.BroadcastChannel; }); // the demonstration agent then sets none
  await page.goto(BASE_URL + '#fdc3');
  await expect(page.locator('.fdc3-agent')).toHaveText('No desktop agent in this page', { timeout: 10000 });
});
