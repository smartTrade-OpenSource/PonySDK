// @ts-check
// A server that stops moves its pages to the other one (ADR-0017): two showcase servers behind a load balancer.
//   ./gradlew :sample:runTwoServers    then    npx playwright test drain
const { test, expect } = require('./coverage');

const LB = process.env.LB_URL || 'http://localhost:8090/';
const APP = LB + 'sample/';

/** @returns {Promise<{name: string, state: string, sockets: number}[]>} */
async function servers(request) {
  return (await (await request.get(LB + 'lb')).json()).servers;
}

test.beforeEach(async ({ request, browserName }) => {
  test.skip(browserName !== 'chromium', 'one browser: the test stops a server the others would share');
  const reachable = await request.get(LB + 'lb').then(r => r.ok(), () => false);
  test.skip(!reachable, 'no balancer: ./gradlew :sample:runTwoServers');
});

test('a server that stops moves its pages to the other one, in waves, each at its address', async ({ context, request }) => {
  test.setTimeout(180000);
  for (const server of await servers(request)) {
    if (server.state === 'stopped') await request.post(LB + 'lb/start/' + server.name);
  }
  await expect.poll(async () => (await servers(request)).map(s => s.state), { timeout: 120000 }).toEqual(['up', 'up']);

  const tokens = ['data', 'streams', 'component-costs'];
  const pages = [];
  for (const token of tokens) {
    const page = await context.newPage();
    await page.goto(APP + '#' + token, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.pony-tab-active')).toBeVisible({ timeout: 15000 });
    pages.push(page);
  }
  const tabs = await Promise.all(pages.map(page => page.locator('.pony-tab-active').innerText()));
  const from = (await context.cookies(LB)).find(c => c.name === 'lb')?.value;
  const to = from === 'a' ? 'b' : 'a';
  const sockets = async name => (await servers(request)).find(s => s.name === name)?.sockets;
  await expect.poll(() => sockets(from)).toBe(tokens.length);

  const reloads = pages.map(page => page.waitForEvent('load', { timeout: 15000 }).then(() => Date.now()));
  await request.post(LB + 'lb/stop/' + from);
  const times = await Promise.all(reloads);
  // the showcase drains over 5 s, its pages one by one over four fifths of it: not all in the same second
  expect(Math.max(...times) - Math.min(...times)).toBeGreaterThan(1000);

  for (const [i, page] of pages.entries()) {
    await expect(page).toHaveURL(APP + '#' + tokens[i]);
    await expect(page.locator('.pony-tab-active')).toHaveText(tabs[i], { timeout: 15000 });
  }
  await expect.poll(() => sockets(to), { timeout: 15000 }).toBe(tokens.length);
  expect((await context.cookies(LB)).find(c => c.name === 'lb')?.value).toBe(to);
  await expect.poll(async () => (await servers(request)).find(s => s.name === from)?.state, { timeout: 15000 })
    .toBe('stopped');

  await request.post(LB + 'lb/start/' + from); // back to two servers, as a rolling deployment ends
});

test('the balancer acts only on its own servers, not on what every object inherits', async ({ request }) => {
  const before = await servers(request);
  for (const name of ['__proto__', 'constructor', 'toString', 'c']) {
    expect((await request.post(LB + 'lb/start/' + name)).status(), name).toBe(404);
    expect((await request.post(LB + 'lb/stop/' + name)).status(), name).toBe(404);
  }
  expect((await servers(request)).map(s => s.name)).toEqual(before.map(s => s.name));
});
