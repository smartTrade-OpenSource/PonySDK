# Accessibility

What a screen reader hears and what the keyboard reaches in the widgets PonySDK draws from plain elements. The
widgets the terminal draws itself (buttons, text boxes, trees) carry their roles and states already; these are the
ones built in Java.

| Widget | A screen reader hears | From the keyboard |
|---|---|---|
| `ListBox`, `DropDownContainer` | A button that opens a list (`aria-haspopup`, `aria-expanded`); a `listbox` of `option`s with `aria-selected`, `aria-disabled`, `aria-multiselectable` | The widget's own keys |
| `DataGrid2` | A `grid`: its rows, columns, selection and sort ([below](#datagrid2)) | One tab stop; arrows, `Home`, `End`, `PageUp`, `PageDown`, `Space`, `Enter` |

## Name what the application shows

A grid has no name of its own: the application gives it one, as it does to an icon button.

```java
gridView.asWidget().setAttribute("aria-label", "Open orders");
```

## DataGrid2

A screen reader reads a `grid` of `row`s of `gridcell`s, headed by `columnheader`s, however the grid is drawn:

- `aria-rowcount` is the whole live data (with the header and the footer), not the rows drawn: the grid draws a
  window of rows, the rest stays on the server. Each row says its place with `aria-rowindex`, which follows the
  scrolling.
- `aria-colcount` and `aria-colindex` count the columns shown: a hidden column is not in the page, so not in the
  grid. A pinned column is read before the scrolled ones, as it is drawn.
- A selected row is `aria-selected="true"`, an unselected one `false`, a row that cannot be selected (`isSelectable`)
  has none; `aria-multiselectable` is set when `DataGridAdapter.isSelectionEnabled()`.
- The column a sort is on has `aria-sort="ascending"` or `"descending"` on its header.
- While rows are fetched, `aria-busy` is on the grid.

The pinned columns and the scrolled columns of a row are two elements, drawn side by side; the terminal links them so
that a screen reader hears one row, whatever the columns are pinned ([ADR-0025](../decisions/0025-datagrid-accessibility.md)).
A test or a style that looks for the cells of a row under its element finds the pinned ones only: the cells of the
scrolled part are in the element next to it, which has the same index in its part.

The terminal's part (roles and indexes of the cells, the keyboard) is `script/datagrid-a11y.js`, which `ponysdk.js`
fetches from its own directory when the first grid is created: a page without a grid does not load it, and a page
with a Content-Security-Policy needs nothing more than `'self'` (an embedded application's origin is listed as
[embedding](embedding.md) says). A grid is usable before the script arrives and gets its roles when it has.

### Keyboard

The grid is one tab stop: `Tab` enters it on one cell (the first at the start), `Tab` again leaves it. In it:

| Key | Effect |
|---|---|
| Arrows | Move the active cell by one |
| `Home`, `End` | Move it to the first and the last cell of the row |
| `Ctrl+Home`, `Ctrl+End` | Move it to the first and the last cell of the grid (the header, the footer) |
| `PageUp`, `PageDown` | Move it a page up or down: the grid scrolls by a page, the rows asked for are fetched |
| `Space`, `Enter` | Selects the row as a click does; with `Shift`, as `Shift`+click: the rows from the last one. On a header: sorts the column, ascending then descending |
| `F2` | Goes to the widget in the cell (a filter, a check box); `Escape` comes back to the cell |

The active cell is scrolled into view, in the scrolled columns too. A mouse click on a cell makes it the active one.
The widgets in the cells (the check box of a row, a filter) are not tab stops: the cells are. `.pony-grid [role=gridcell]:focus-visible`
and `.pony-grid [role=columnheader]:focus-visible` draw the active cell's outline (`ponysdk.css`); a theme restyles
them.

### What to know

- The columns scrolled out of the view are not drawn until they come in: a screen reader that moves to one hears
  `...` first. `setRefreshOnColumnVisibilityChanged(false)` draws every column, at the price of every cell of every
  row on the wire.
- The grid takes the keys when a cell has the focus; a widget in a cell (an editor) keeps its own.
