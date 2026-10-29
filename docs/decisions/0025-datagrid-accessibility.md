# ADR-0025: DataGrid2 read as a grid by a screen reader, and driven from the keyboard

- Status: Accepted
- Date: 2026-10-29

## Context

`DataGrid2` drew divs. A screen reader read them as text, in DOM order; the cells could not take the focus, so the
keyboard reached nothing in a grid of 10,000 rows. Commit 7bcef9bb gave `ListBox` and the dropdowns their ARIA and
left this grid out: its structure is the difficulty.

- A logical row is two DOM rows: one in the part of the pinned columns, one in the part of the scrolled columns (the
  second scrolls sideways, both scroll up and down together). A grid role on that structure exposes two half rows.
- The grid draws a window of rows (three times what is visible), sent by the server as the user scrolls: the row
  count and the index of a row are not the DOM's.
- A hidden column is removed from the page; a column scrolled out of the view stays in it, drawn when it comes in.
- Selection, sort and the count are known to the server only; the terminal knows the page.

## Decision

1. **WAI-ARIA grid.** The root is a `grid` (the application names it with `aria-label`), with `aria-rowcount`
   (the live data, plus the header and the footer when there are) and `aria-colcount` (the columns shown),
   `aria-multiselectable` when `isSelectionEnabled()`. The header is a row of `columnheader`s with `aria-sort` on
   the sorted columns, the footer a row of `gridcell`s, each body row a `row` with `aria-rowindex` and `aria-selected`
   (absent on a row that cannot be selected), each cell a `gridcell` with `aria-colindex`. The containers between
   (header, body, parts) are `presentation`.
2. **One row, two elements: the pinned half is the row and owns the cells of the other.** The pinned element is
   `role="row"`, its `aria-owns` lists the ids of the cells of the scrolled element, which is `presentation`. A
   screen reader hears one row, the cells of the pinned part first, then the others: the order they are drawn in.
3. **The server says what only it knows, once and when it changes** (`DefaultDataGridView`): the roles of the grid,
   the containers and the rows; `aria-rowcount`, `aria-colcount` (shown columns, at each state change),
   `aria-rowindex` (set when the first row of the window moved, not at each draw), `aria-selected` (set with the
   selection the row already got), `aria-sort` (with the column's sort, on its header cell), `aria-multiselectable`.
4. **The terminal lays what comes from the page** (`ponysdk.js`, the grid's add-on): the role and `aria-colindex` of
   each cell, the ids of the scrolled cells and the `aria-owns` of the pinned row, the roving `tabindex`. The server
   asks for it with one add-on call, `updateAria`, when the columns or the rows changed (a column pinned, shown,
   hidden or moved, rows added or removed, a configuration), coalesced to one run per batch of instructions. No new
   protocol key.
5. **Keyboard.** The grid is one tab stop (roving `tabindex`: one cell has 0, the others -1, the scrolled parts and
   the widgets inside the cells -1). Arrows move the active cell; `Home`/`End` go to the ends of the row,
   `Ctrl+Home`/`Ctrl+End` to the ends of the grid (header to footer); `PageUp`/`PageDown` scroll by a page and move
   the active cell by a page. `Space` and `Enter` select the row by dispatching a click on it: the grid's selection
   model decides, `Shift` selects the range as `Shift`+click does. On a header they sort the column, as a click on
   its name does: the terminal tells the server by the add-on's JSON (`{col, sort}`, as it tells a width or a move),
   and the server sorts in the next direction. `F2` goes to the widget in the cell (a filter), `Escape` back. The active cell is scrolled into view (`scrollIntoView` nearest, in the scrolled part too). The
   active cell is a (row, column) of the grid, not an element: when the window moves, the focus follows the row to
   its new element; a row not in the window is asked for by scrolling, the focus goes to it when it comes.
6. **Focus indicator** in `ponysdk.css`: `:focus-visible` on a cell, an outline inside it. Nothing else is styled.

## Options considered

- **Roles and ids from Java, on each cell.** Measured on the showcase grid (63 rows drawn × 53 columns, first draw of
  the tab): 429,149 B before; `role` + `aria-colindex` on the cells and headers +63,929 B (+14.9 %); with the ids
  and the `aria-owns` lists as well +124,809 B (+29.1 %). A role and a column index are functions of the position of
  a cell, which the page knows: decisions 3 and 4 cost +0.8 %. Rejected.
- **`aria-owns` of the scrolled row element (one id per row) instead of its cells.** Chrome's tree is right with it
  (checked on a static page through the DevTools Protocol): the cells of an owned `presentation` element are shown
  under the owner. Firefox and WebKit's own trees could not be read here, and an owned element that is itself
  ignored is the less travelled path in them: the list of cells is what the specification's examples do.
  Rejected. (Its cost is two attributes per row, not 53 ids.)
- **Both halves `role="row"`, with the same `aria-rowindex`.** A screen reader reads two rows, the cell navigation
  of a table (up, down) leaves the half it is in. Rejected.
- **One element per row, the two halves inside it.** The halves are two columns of one layout (the pinned one does
  not scroll sideways) and `DataGridAdapter.onCreateRow` gets each half to style: a layout and an API change.
- **`aria-activedescendant` on the grid instead of a roving `tabindex`.** No DOM focus to move when a row's element
  is reused for another row, but the cells' own focus is what the browser scrolls into view, and what a widget in a
  cell (an editor) needs. Roving `tabindex`; the focus follows the row when the window moves.
- **A `MutationObserver` to lay the cells again, instead of `updateAria`.** It sees the text of every cell that a
  tick changes. The call is a message per change of columns or rows, not per tick.
- **Selection from the keyboard by a key event to the server.** A click dispatched on the row goes the path a
  click takes: one selection model, the anchor of `Shift` kept.

## Consequences

Measured on the showcase DataGrid (53 columns, 200 rows, 63 rows drawn), Chrome, protocol bytes before deflate as
the page receives them:

- First draw: 429,149 -> 432,536 B (+3,387 B, +0.8 %): the roles of about 140 containers and rows, `aria-rowindex`
  and `aria-selected` on 63 rows, the two counts, the `updateAria` call.
- A scroll that fetches a window of 63 rows: 6,998 -> 7,546 B (+548 B, +8.7 B per row): the indexes. An unchanged
  window costs nothing.
- A tick that redraws rows whose data changed, the selection not: +0 B (a unit test counts the attributes written
  again on `onUpdateRows`). A selection change: one `aria-selected` per row changed.
- Server CPU of a tick, 30 rows x 20 columns drawn again: 11.7-15.6 us before, 11.9-15.1 us after, 9,600 B
  allocated in both (five rounds of 50,000 each way): no difference above the noise.
- Terminal: `ponysdk.js` 52,817 -> 66,618 B (10,899 -> 14,196 B gzip) for the section and the keyboard; `terminal.js` is
  unchanged. Laying the cells, 63 rows x 53 columns: 2.3-4.1 ms, at the load and at a column change, not per tick.
- 13 Java tests (`DefaultDataGridViewAriaTest`), 12 Playwright tests (`datagrid-a11y.spec.js`) in Chrome, Firefox and
  WebKit, one of them Chrome-only: it reads Chrome's own accessibility tree through the DevTools Protocol and finds
  every row with its 53 cells, the pinned and the scrolled ones, in order. Firefox and WebKit are checked through
  Playwright's role locators and `toMatchAriaSnapshot`, which compute the tree from the DOM and `aria-owns`: not
  their platform trees. No screen reader (VoiceOver, NVDA, JAWS) was run on it.

Limits:

- A column scrolled out of the view is not drawn: a screen reader that goes to it hears `...` until the server draws
  it, a few tens of milliseconds. `setRefreshOnColumnVisibilityChanged(false)` draws every column.
- A locator or a style that looks for the cells of a row under its element finds the pinned ones: the others are in
  the element next to it. The Playwright suite reads the cells of a row from its accessibility tree for this reason.
- A header the application draws with its own sort control keeps it: `Enter` and `Space` on the header cell sort
  through the grid, whatever the header holds.
- The widgets in the cells are not tab stops (`F2` goes to one); one the application adds later is.
- A decorator that moves the focus itself keeps doing so: the filter footer disables its box while the grid is
  filtered and gives it the focus back after the draw.
