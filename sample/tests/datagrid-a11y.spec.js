// @ts-check
// The showcase's DataGrid for a screen reader and from the keyboard (docs/decisions/0025-datagrid-accessibility.md).
// A row of the grid is two elements, one per part (pinned columns, scrolled columns): a screen reader must hear one
// row with all its cells in column order.
//   ./gradlew :sample:runShowcase    then    BASE_URL=http://localhost:8081/sample/ npx playwright test datagrid-a11y
const { test, expect } = require('./coverage');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081/sample/';
const ROWS = 200; // the data loaded; the grid adds a header and a footer
const COLUMNS = 53; // 52 of data and the check boxes of the selection

/** Opens the DataGrid tab and waits for its first rows. */
async function openGrid(page) {
  await page.goto(BASE_URL + '#datagrid');
  const grid = page.getByRole('grid', { name: 'Sample data' });
  await expect(grid.getByRole('gridcell', { name: 'a000000199' })).toBeVisible({ timeout: 20000 });
  return grid;
}

/**
 * The cells of a row as a screen reader gets them, by name, in order. A locator under a row looks in its element only,
 * which holds the pinned columns: the accessibility tree of the row is the one to read.
 */
async function cellsOf(row) {
  const lines = (await row.ariaSnapshot()).split('\n');
  return lines.map(line => line.match(/^\s*- (?:gridcell|columnheader)(?: "(.*)")?/)).filter(Boolean).map(m => m[1] ?? '');
}

/** Where the focus is: its role, column and row of the grid, and its text. */
function focused(page) {
  return page.evaluate(() => {
    const cell = document.activeElement;
    const info = { role: cell.getAttribute('role'), col: cell.getAttribute('aria-colindex'), text: cell.innerText.trim(), row: null, part: '' };
    const parent = cell.parentElement;
    if (!parent) return info;
    if (parent.classList.contains('pony-grid-pinned-header') || parent.classList.contains('pony-grid-unpinned-header')) {
      info.part = 'header';
      info.row = '1';
    } else if (parent.classList.contains('pony-grid-pinned-footer') || parent.classList.contains('pony-grid-unpinned-footer')) {
      info.part = 'footer';
      info.row = document.querySelector('.pony-grid-pinned-footer').getAttribute('aria-rowindex');
    } else {
      info.part = 'body';
      const slot = Array.prototype.indexOf.call(parent.parentElement.children, parent);
      info.row = document.querySelector('.pony-grid-pinned-body').children[slot].getAttribute('aria-rowindex');
    }
    return info;
  });
}

/** The first cell of the data of a row shows the number of its row: a000000199 is the first row of the grid, the 2nd. */
const textOfRow = (row) => 'a' + String(ROWS + 1 - row).padStart(9, '0');

test('the grid is a grid, with its rows, columns and what it can select', async ({ page }) => {
  const grid = await openGrid(page);

  await expect(grid).toHaveAttribute('aria-rowcount', String(ROWS + 2)); // the live data, a header and a footer
  await expect(grid).toHaveAttribute('aria-colcount', String(COLUMNS));
  await expect(grid).toHaveAttribute('aria-multiselectable', 'true');
  await expect(grid.getByRole('columnheader')).toHaveCount(COLUMNS);
  await expect(grid.getByRole('row').first()).toHaveAttribute('aria-rowindex', '1');
  await expect(grid.getByRole('row').nth(1)).toHaveAttribute('aria-selected', 'false');
  await expect(grid.getByRole('row').nth(1)).toHaveAttribute('aria-rowindex', '2');
});

test('a row is one row with the cells of both parts, in column order', async ({ page }) => {
  const grid = await openGrid(page);
  const row = grid.getByRole('row').nth(1);

  await expect(row).toMatchAriaSnapshot(`
    - row:
      - gridcell
      - gridcell "a000000199"
      - gridcell "b000000199"
      - gridcell "c000000199"
  `);
  const cells = await cellsOf(row);
  expect(cells).toHaveLength(COLUMNS);
  expect(cells.slice(1, 4)).toEqual(['a000000199', 'b000000199', 'c000000199']);
  // the header the same: the check box of the pinned part, then the columns of the scrolled part
  expect(await cellsOf(grid.getByRole('row').first())).toHaveLength(COLUMNS);
  // the cells say their column
  await expect(grid.getByRole('gridcell', { name: 'a000000199' })).toHaveAttribute('aria-colindex', '2');
  await expect(grid.getByRole('gridcell', { name: 'c000000199' })).toHaveAttribute('aria-colindex', '4');
});

test('the browser tree has the same rows and cells (Chrome\'s accessibility tree)', async ({ page, browserName }) => {
  test.skip(browserName !== 'chromium', 'the accessibility tree is read through the Chrome DevTools Protocol');
  const grid = await openGrid(page);
  await expect(grid.getByRole('row').nth(1)).toBeVisible();

  const cdp = await page.context().newCDPSession(page);
  await cdp.send('Accessibility.enable');
  const { nodes } = await cdp.send('Accessibility.getFullAXTree');
  const byId = new Map(nodes.map(n => [n.nodeId, n]));
  const named = (n) => (n.name && n.name.value) || '';
  // the nodes the tree shows under a node: an ignored one or a plain container shows its own children
  const shown = (n) => (n.childIds || []).flatMap(id => {
    const c = byId.get(id);
    return c.ignored || ['generic', 'none'].includes(c.role.value) ? shown(c) : [c];
  });
  const gridNode = nodes.find(n => !n.ignored && n.role.value === 'grid' && named(n) === 'Sample data');
  expect(gridNode, 'the grid is in the tree').toBeTruthy();
  const rows = shown(gridNode).filter(n => n.role.value === 'row');
  expect(rows.length, 'a header, the rows drawn and a footer').toBeGreaterThan(30);

  const header = shown(rows[0]);
  expect(header.map(n => n.role.value)).toEqual(Array(COLUMNS).fill('columnheader'));
  const first = shown(rows[1]);
  expect(first.map(n => n.role.value)).toEqual(Array(COLUMNS).fill('gridcell'));
  expect(first.slice(1, 4).map(named)).toEqual(['a000000199', 'b000000199', 'c000000199']);
  // no row is the half of one: every row has all its cells
  for (const r of rows.slice(0, 20)) expect(shown(r).length).toBe(COLUMNS);
});

test('the sort of a column is on its header', async ({ page }) => {
  const grid = await openGrid(page);
  const header = grid.getByRole('columnheader').nth(1); // the column "a"
  await expect(header).not.toHaveAttribute('aria-sort', /.+/);

  await header.locator('.dg-col-name').click();
  await expect(header).toHaveAttribute('aria-sort', 'ascending');
  await expect(grid.getByRole('gridcell', { name: 'a000000000' })).toBeVisible();
  await expect(grid.locator('[aria-sort]')).toHaveCount(1);

  await header.locator('.dg-col-name').click();
  await expect(header).toHaveAttribute('aria-sort', 'descending');

  await page.getByRole('button', { name: 'Clear sorts' }).click();
  await expect(grid.locator('[aria-sort]')).toHaveCount(0);
});

test('Enter on a header sorts its column', async ({ page }) => {
  const grid = await openGrid(page);
  const header = grid.getByRole('columnheader').nth(1); // the column "a"
  await header.focus();

  await page.keyboard.press('Enter');
  await expect(header).toHaveAttribute('aria-sort', 'ascending');
  await expect(grid.getByRole('gridcell', { name: 'a000000000' })).toBeVisible();
  await expect(header).toBeFocused();

  await page.keyboard.press('Space');
  await expect(header).toHaveAttribute('aria-sort', 'descending');
  await expect(grid.getByRole('gridcell', { name: 'a000000199' })).toBeVisible();
});

test('a hidden column is not in the grid, a pinned one is read first', async ({ page }) => {
  const grid = await openGrid(page);
  const header = (name) => grid.locator('.dg-col-head').filter({ has: page.locator('.dg-col-name', { hasText: new RegExp('^' + name + '$') }) });

  await header('c').getByText('Hide').click();
  await expect(grid).toHaveAttribute('aria-colcount', String(COLUMNS - 1));
  await expect(grid.getByRole('columnheader')).toHaveCount(COLUMNS - 1);
  const row = grid.getByRole('row').nth(1);
  expect(await cellsOf(row)).toHaveLength(COLUMNS - 1);
  await expect(grid.getByRole('gridcell', { name: 'c000000199' })).toHaveCount(0);
  await expect(grid.getByRole('gridcell', { name: 'd000000199' })).toHaveAttribute('aria-colindex', '4');

  // pinned, a column goes to the pinned part: it is read after the check boxes, before the columns scrolled
  await header('b').getByText('Pin').click();
  await expect(grid.getByRole('gridcell', { name: 'b000000199' })).toHaveAttribute('aria-colindex', '2');
  await expect(grid.getByRole('gridcell', { name: 'a000000199' })).toHaveAttribute('aria-colindex', '3');
  const cells = await cellsOf(row);
  expect(cells.slice(1, 4)).toEqual(['b000000199', 'a000000199', 'd000000199']);
  expect(cells).toHaveLength(COLUMNS - 1);
  await expect(grid).toHaveAttribute('aria-colcount', String(COLUMNS - 1));
});

test('the rows of the window are numbered in the data, whatever the scrolling', async ({ page }) => {
  const grid = await openGrid(page);
  const body = page.locator('.pony-grid-body');

  await body.evaluate(b => { b.scrollTop = b.scrollHeight / 2; });
  const firstRow = page.locator('.pony-grid-pinned-body .pony-grid-row[role=row]:not([style*="display: none"])').first();
  await expect.poll(async () => Number(await firstRow.getAttribute('aria-rowindex')), { timeout: 10000 }).toBeGreaterThan(40);

  // each row says its place and shows the data of that place
  const rows = await page.locator('.pony-grid-pinned-body .pony-grid-row:not([style*="display: none"])').evaluateAll(rows =>
    rows.map((row, i) => [row.getAttribute('aria-rowindex'),
      document.querySelector('.pony-grid-unpinned-body').children[Array.prototype.indexOf.call(row.parentElement.children, row)].children[0].innerText]));
  expect(rows.length).toBeGreaterThan(30);
  for (const [index, text] of rows) expect(text).toBe(textOfRow(Number(index)));
  await expect(grid).toHaveAttribute('aria-rowcount', String(ROWS + 2));
});

test('the grid is one tab stop, its cells reached with the arrows', async ({ page }) => {
  const grid = await openGrid(page);
  const tabStop = grid.locator('[role=gridcell][tabindex="0"], [role=columnheader][tabindex="0"]');
  await expect(tabStop).toHaveCount(1);
  // the widgets of the cells (a check box per row, a filter per column) are not stops
  await expect(grid.locator('input:not([tabindex="-1"])')).toHaveCount(0);

  const after = page.getByRole('button', { name: /Load more/ });
  await after.focus();
  await page.keyboard.press('Shift+Tab');
  await expect(tabStop).toBeFocused(); // one Tab from the end of the grid
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => document.querySelector('.pony-grid').contains(document.activeElement))).toBe(false); // the grid is left

  await page.keyboard.press('Shift+Tab');
  await expect(tabStop).toBeFocused();
  expect(await focused(page)).toMatchObject({ role: 'columnheader', col: '1', row: '1' });
  await page.keyboard.press('ArrowRight');
  expect(await focused(page)).toMatchObject({ role: 'columnheader', col: '2', row: '1' });
  await page.keyboard.press('ArrowDown');
  expect(await focused(page)).toMatchObject({ role: 'gridcell', col: '2', row: '2', text: 'a000000199' });
  await page.keyboard.press('ArrowDown');
  expect(await focused(page)).toMatchObject({ role: 'gridcell', col: '2', row: '3', text: 'a000000198' });
  await page.keyboard.press('ArrowLeft');
  expect(await focused(page)).toMatchObject({ role: 'gridcell', col: '1', row: '3' });
  await page.keyboard.press('ArrowLeft');
  expect(await focused(page)).toMatchObject({ col: '1', row: '3' }); // the first column is the first
  await page.keyboard.press('ArrowUp');
  await page.keyboard.press('ArrowUp');
  expect(await focused(page)).toMatchObject({ role: 'columnheader', row: '1' });
  await page.keyboard.press('ArrowUp');
  expect(await focused(page)).toMatchObject({ role: 'columnheader', row: '1' }); // and the first row

  await expect(tabStop).toHaveCount(1);
  await expect(tabStop).toBeFocused();
});

test('Home and End go to the ends of the row, with Ctrl to the ends of the grid; the cell is scrolled into view', async ({ page }) => {
  const grid = await openGrid(page);
  await grid.getByRole('gridcell', { name: 'a000000198' }).focus();
  const scrolled = page.locator('.pony-grid-unpinned-body');
  expect(await scrolled.evaluate(s => s.scrollLeft)).toBe(0);

  await page.keyboard.press('End');
  expect(await focused(page)).toMatchObject({ role: 'gridcell', col: String(COLUMNS), row: '3' });
  // the last column was out of the view: it is in it now, and the header goes along
  const inView = await page.evaluate(() => {
    const cell = document.activeElement.getBoundingClientRect(), box = document.querySelector('.pony-grid-unpinned-body').getBoundingClientRect();
    return cell.left >= box.left - 1 && cell.right <= box.right + 1;
  });
  expect(inView).toBe(true);
  expect(await scrolled.evaluate(s => s.scrollLeft)).toBeGreaterThan(1000);
  expect(await page.locator('.pony-grid-unpinned-header').evaluate(s => s.scrollLeft)).toBeGreaterThan(1000);

  await page.keyboard.press('Home'); // the first column is pinned: it is in view, the scrolled part stays
  expect(await focused(page)).toMatchObject({ col: '1', row: '3' });
  await page.keyboard.press('ArrowRight'); // the first of the scrolled part
  expect(await focused(page)).toMatchObject({ col: '2', row: '3' });
  await expect.poll(() => scrolled.evaluate(s => s.scrollLeft)).toBe(0);
  // a column out of the view was not drawn: the server draws it when it comes in
  await expect.poll(async () => (await focused(page)).text).toBe('a000000198');

  await page.keyboard.press('Control+End');
  expect(await focused(page)).toMatchObject({ role: 'gridcell', col: String(COLUMNS), part: 'footer' });
  await page.keyboard.press('Control+Home');
  expect(await focused(page)).toMatchObject({ role: 'columnheader', col: '1', row: '1' });
});

test('PageDown and PageUp scroll by a page and move the active cell, past the rows drawn', async ({ page }) => {
  const grid = await openGrid(page);
  const body = page.locator('.pony-grid-body');
  await grid.getByRole('gridcell', { name: 'a000000199' }).focus();
  const start = Number((await focused(page)).row);
  expect(start).toBe(2);

  await page.keyboard.press('PageDown');
  const moved = Number((await focused(page)).row) - start;
  expect(moved).toBeGreaterThanOrEqual(10);
  expect(await body.evaluate(b => b.scrollTop)).toBeGreaterThan(100);

  // down to the 150th row, 63 rows are drawn: the rows asked for come from the server, the focus goes to them
  for (let i = 0; i < 6; i++) await page.keyboard.press('PageDown');
  await expect.poll(async () => Number((await focused(page)).row), { timeout: 10000 }).toBeGreaterThan(100);
  const there = await focused(page);
  expect(there.text).toBe(textOfRow(Number(there.row)));
  await expect(page.locator(':focus')).toBeInViewport();

  await page.keyboard.press('PageUp');
  await expect.poll(async () => Number((await focused(page)).row)).toBe(Number(there.row) - moved);
  const back = await focused(page);
  expect(back.text).toBe(textOfRow(Number(back.row)));

  // the last row of the data, from the footer: its rows are not drawn
  await page.keyboard.press('Control+End');
  expect(await focused(page)).toMatchObject({ part: 'footer' });
  await page.keyboard.press('ArrowUp');
  await expect.poll(async () => (await focused(page)).row, { timeout: 10000 }).toBe(String(ROWS + 1));
  expect((await focused(page)).text).toBe('Z000000000'); // the last column of the last row of the data
});

test('Space and Enter select the row as a click does, Shift selects the rows between', async ({ page }) => {
  const grid = await openGrid(page);
  const selected = grid.locator('[role=row][aria-selected="true"]');
  await expect(grid.getByRole('row').nth(1)).toHaveAttribute('aria-selected', 'false');
  await expect(selected).toHaveCount(0);

  await grid.getByRole('gridcell', { name: 'c000000199' }).focus();
  await page.keyboard.press('Space');
  await expect(grid.getByRole('row').nth(1)).toHaveAttribute('aria-selected', 'true');
  await expect(grid.getByRole('row').nth(1)).toMatchAriaSnapshot('- row [selected]');
  await expect(selected).toHaveCount(1);
  await page.keyboard.press('Enter');
  await expect(selected).toHaveCount(0);
  await page.keyboard.press('Space');
  await expect(selected).toHaveCount(1);

  // from this row to the 4th: a range
  for (let i = 0; i < 3; i++) await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Shift+Space');
  await expect(selected).toHaveCount(4);
  await expect(grid.getByRole('row').nth(4)).toHaveAttribute('aria-selected', 'true');
  await expect(grid.getByRole('row').nth(5)).toHaveAttribute('aria-selected', 'false');
  // the check boxes of the selection are those of the model
  await expect(grid.locator('.pony-grid-pinned-body input:checked')).toHaveCount(4);

  // a click does the same
  await grid.getByRole('gridcell', { name: 'c000000190' }).click();
  await expect(grid.getByRole('row').nth(10)).toHaveAttribute('aria-selected', 'true');
});

test('the focus is seen, and F2 on a filter\'s cell goes to the filter, Escape back to the cell', async ({ page }) => {
  const grid = await openGrid(page);
  await grid.getByRole('gridcell', { name: 'a000000199' }).focus();
  await page.keyboard.press('Control+End');
  await page.keyboard.press('ArrowLeft'); // a column of the scrolled part, in the footer
  const id = await page.evaluate(() => document.activeElement.id);
  const cell = page.locator('[id="' + id + '"]');
  await expect(cell).toBeFocused();
  await expect(cell).toHaveAttribute('role', 'gridcell');
  const outline = await cell.evaluate(e => getComputedStyle(e).outlineStyle + ' ' + getComputedStyle(e).outlineWidth);
  expect(outline).not.toMatch(/^none/);

  await page.keyboard.press('F2');
  await expect(cell.locator('input')).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(cell).toBeFocused();
  expect((await focused(page)).role).toBe('gridcell');
});
