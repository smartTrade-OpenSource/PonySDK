/*
 * The accessibility of the DataGrid: roles and indexes of the cells, the roving tabindex and the keyboard
 * (docs/decisions/0025-datagrid-accessibility.md, the amendment of that ADR on its loading).
 *
 * ponysdk.js loads it beside itself when the first DataGrid2 is created, so that a page without a grid does not
 * carry it; the grid installs its methods once it is here (installAria in ponysdk.js).
 *
 * A row is two elements, one per part of the grid (pinned columns, scrolled columns). The server marks the pinned
 * one as the row and the other as presentational, and tells what only it knows: the index, the counts, the
 * selection, the sort. Here the cells are laid (role, column index) and the pinned half owns the cells of the
 * other, so that a screen reader hears one row with its cells in column order. None of it is sent: it comes from
 * the page, where the server cannot see.
 */
(function () {
    "use strict";

    // The methods of the grid's add-on: `this` is the add-on
    AbstractAddon.dataGridAria = {

        // The active cell is (g, col), g the row of the grid (the header is 0)
        initAria: function () {
            this.hasHeader = false;
            this.hasFooter = false;
            this.pinnedCount = 0;
            this.colCount = 0;
            this.active = { g: 0, col: 0 };
            this.tabStop = null;
            this.pendingFocus = false;
            // a scrolled part with no tab stop in it would be one itself, in Chrome: the cells are the stops
            [this.body, this.unpinnedHeader, this.unpinnedBody, this.unpinnedFooter].forEach(function (part) {
                part.setAttribute('tabindex', '-1');
            });
            this.element.addEventListener('keydown', this.onKeyDown.bind(this));
            this.element.addEventListener('keyup', this.onKeyUp.bind(this));
            this.element.addEventListener('focusin', this.onFocusIn.bind(this));
        },

        // The columns or the rows changed: once per batch of instructions, when the page holds them all
        updateAria: function () {
            if (this.ariaPending) return;
            this.ariaPending = true;
            queueMicrotask(function () {
                this.ariaPending = false;
                this.layoutAria();
            }.bind(this));
        },

        layoutAria: function () {
            this.hasHeader = this.pinnedHeader.getAttribute('role') === 'row';
            this.hasFooter = this.pinnedFooter.getAttribute('role') === 'row';
            this.pinnedCount = this.pinnedHeader.children.length;
            this.colCount = this.pinnedCount + this.unpinnedHeader.children.length;
            if (this.hasHeader) this.layoutRow(this.pinnedHeader, this.unpinnedHeader, 'h', 'columnheader');
            var rows = this.pinnedBody.children, others = this.unpinnedBody.children;
            for (var i = 0; i < rows.length; i++) this.layoutRow(rows[i], others[i], 'r' + i, 'gridcell');
            if (this.hasFooter) {
                this.layoutRow(this.pinnedFooter, this.unpinnedFooter, 'f', 'gridcell');
                this.nameFooterWidgets();
            }
            this.placeTabStop(this.tabStopCell());
        },

        // The cells of both halves, in column order; the pinned half owns the cells of the other
        layoutRow: function (pinnedRow, unpinnedRow, tag, role) {
            var cells = pinnedRow.children, owned = [], i, prefix = this.widgetID + tag + 'c';
            for (i = 0; i < cells.length; i++) {
                this.layoutCell(cells[i], role, i + 1);
                // a cell that was in the other half has its id still, and the cell now at its place too
                if (cells[i].id.indexOf(prefix) === 0) cells[i].removeAttribute('id');
            }
            cells = unpinnedRow.children;
            for (i = 0; i < cells.length; i++) {
                var id = prefix + i;
                if (cells[i].id !== id) cells[i].id = id;
                this.layoutCell(cells[i], role, this.pinnedCount + i + 1);
                owned.push(id);
            }
            var owns = owned.join(' ');
            if (owns === '') pinnedRow.removeAttribute('aria-owns');
            else if (pinnedRow.getAttribute('aria-owns') !== owns) pinnedRow.setAttribute('aria-owns', owns);
        },

        // A widget in the footer (a filter) has no label: the name of its column, in the header, names it
        nameFooterWidgets: function () {
            var col = 0, ours = this.widgetID + 'h';
            [this.pinnedFooter, this.unpinnedFooter].forEach(function (part) {
                for (var i = 0; i < part.children.length; i++, col++) {
                    var header = this.hasHeader ? this.cellAt(0, col) : null;
                    // the caption the column is dragged by, else the whole header cell
                    var name = header && (header.querySelector('.pony-grid-draggable-col') || header);
                    // the header cell has its id already when it is in the scrolled part, the caption never does
                    if (name && name !== header) name.id = this.widgetID + 'hn' + col;
                    else if (name && name.id === '') name.id = this.widgetID + 'hn' + col;
                    var widgets = part.children[i].querySelectorAll('input,select,textarea');
                    for (var j = 0; j < widgets.length; j++) {
                        var labelled = widgets[j].getAttribute('aria-labelledby');
                        // one named by the application, or by a label around it, is left as it is
                        if (!name || widgets[j].hasAttribute('aria-label') || widgets[j].labels.length > 0
                            || (labelled !== null && labelled.indexOf(ours) !== 0)) continue;
                        widgets[j].setAttribute('aria-labelledby', name.id);
                    }
                }
            }, this);
        },

        layoutCell: function (cell, role, index) {
            if (cell.getAttribute('role') !== role) cell.setAttribute('role', role);
            if (cell.getAttribute('aria-colindex') !== String(index)) cell.setAttribute('aria-colindex', String(index));
            if (!cell.hasAttribute('tabindex')) cell.setAttribute('tabindex', '-1'); // roving: one cell is the tab stop
            // the widgets in the cells (a check box per row, a filter per column) are not tab stops: the grid is one
            var widgets = cell.querySelectorAll('input,select,textarea,button,a[href]');
            for (var i = 0; i < widgets.length; i++) {
                if (!widgets[i].hasAttribute('tabindex')) widgets[i].setAttribute('tabindex', '-1');
            }
        },

        // The cell at the row g of the grid and the column col, null when its row is not in the window drawn
        cellAt: function (g, col) {
            var h = this.hasHeader ? 1 : 0, d = g - h, pinnedRow, unpinnedRow;
            if (g < h) {
                pinnedRow = this.pinnedHeader;
                unpinnedRow = this.unpinnedHeader;
            } else if (d < this.absRowCount) {
                var slot = d - this.firstRowIndex;
                if (slot < 0 || slot >= this.relRowCount) return null;
                pinnedRow = this.pinnedBody.children[slot];
                unpinnedRow = this.unpinnedBody.children[slot];
            } else if (this.hasFooter && d === this.absRowCount) {
                pinnedRow = this.pinnedFooter;
                unpinnedRow = this.unpinnedFooter;
            } else {
                return null;
            }
            if (!pinnedRow) return null;
            return (col < this.pinnedCount ? pinnedRow.children[col] : unpinnedRow.children[col - this.pinnedCount]) || null;
        },

        // Where a cell of this grid is: its row g and its column col; null for anything else
        locate: function (cell) {
            var row = cell.parentElement, col = parseInt(cell.getAttribute('aria-colindex'), 10) - 1;
            if (!row || isNaN(col)) return null;
            var h = this.hasHeader ? 1 : 0;
            if (row === this.pinnedHeader || row === this.unpinnedHeader) return h ? { g: 0, col: col } : null;
            if (row === this.pinnedFooter || row === this.unpinnedFooter) {
                return this.hasFooter ? { g: h + this.absRowCount, col: col } : null;
            }
            var body = row.parentElement;
            if (body !== this.pinnedBody && body !== this.unpinnedBody) return null;
            return { g: h + this.firstRowIndex + Array.prototype.indexOf.call(body.children, row), col: col };
        },

        lastRow: function () {
            return Math.max(0, (this.hasHeader ? 1 : 0) + this.absRowCount + (this.hasFooter ? 1 : 0) - 1);
        },

        pageRows: function () {
            return this.rowHeight > 0 ? Math.max(1, Math.floor(this.body.clientHeight / this.rowHeight) - 1) : 1;
        },

        // The cell of the cell's own grid that has the focus, null when the focus is elsewhere
        focusedCell: function () {
            var focused = this.element.getRootNode().activeElement;
            return focused && this.element.contains(focused) && this.locate(focused) ? focused : null;
        },

        placeTabStop: function (cell) {
            if (this.tabStop === cell) return;
            if (this.tabStop) this.tabStop.setAttribute('tabindex', '-1');
            this.tabStop = cell;
            if (cell) cell.setAttribute('tabindex', '0');
        },

        // The cell the Tab key reaches: the active one, else the nearest of the rows drawn
        tabStopCell: function () {
            var h = this.hasHeader ? 1 : 0, col = Math.min(this.active.col, Math.max(0, this.colCount - 1));
            var cell = this.cellAt(this.active.g, col);
            if (cell) return cell;
            var drawn = Math.max(0, Math.min(this.relRowCount, this.absRowCount - this.firstRowIndex));
            if (drawn === 0) return this.cellAt(0, col);
            var first = h + this.firstRowIndex;
            return this.cellAt(Math.min(Math.max(this.active.g, first), first + drawn - 1), col);
        },

        onFocusIn: function (event) {
            var cell = event.target.closest('[role=gridcell],[role=columnheader]');
            var at = cell && this.locate(cell);
            if (!at) return;
            this.active = at;
            this.pendingFocus = false;
            this.placeTabStop(cell);
        },

        // The cell of the active row and column takes the focus, after a scroll to it when its row is not drawn
        focusCell: function (g, col) {
            this.active = { g: g, col: col };
            var cell = this.cellAt(g, col);
            if (!cell) {
                this.pendingFocus = true;
                this.scrollToRow(g);
                return;
            }
            this.pendingFocus = false;
            this.placeTabStop(cell);
            cell.focus({ preventScroll: true });
            cell.scrollIntoView({ block: 'nearest', inline: 'nearest' });
        },

        scrollToRow: function (g) {
            var d = g - (this.hasHeader ? 1 : 0);
            if (d < 0 || d >= this.absRowCount) return;
            this.body.scrollTop = Math.max(0, d * this.rowHeight * this.ratio - this.body.clientHeight / 2);
        },

        // The window of rows moved: the active row is in another element, or the rows asked for have come
        onWindowMoved: function () {
            var focused = this.focusedCell(), target = this.cellAt(this.active.g, this.active.col);
            if (this.pendingFocus) {
                if (target) this.focusCell(this.active.g, this.active.col);
            } else if (target) {
                this.placeTabStop(target);
                if (focused && focused !== target) target.focus({ preventScroll: true });
            } else if (focused) { // the row left the window with the scrolling: the focus stays where it is
                this.active = this.locate(focused);
                this.placeTabStop(focused);
            } else {
                this.placeTabStop(this.tabStopCell());
            }
        },

        onKeyDown: function (event) {
            var at = this.locate(event.target); // the cell itself: an editor in a cell keeps its keys
            if (event.key === 'Escape' && !at && !event.defaultPrevented) return this.leaveWidget(event);
            if (!at || event.defaultPrevented || event.altKey || event.metaKey) return;
            if (this.pendingFocus) at = this.active; // keys pressed faster than the server answers go on from where the last led
            var g = at.g, col = at.col;
            if (event.ctrlKey && event.key !== 'Home' && event.key !== 'End') return;
            switch (event.key) {
                case 'ArrowDown': g++; break;
                case 'ArrowUp': g--; break;
                case 'ArrowRight': col++; break;
                case 'ArrowLeft': col--; break;
                case 'Home': col = 0; if (event.ctrlKey) g = 0; break;
                case 'End': col = this.colCount - 1; if (event.ctrlKey) g = this.lastRow(); break;
                case 'PageDown': g += this.pageRows(); this.body.scrollTop += this.pageRows() * this.rowHeight; break;
                case 'PageUp': g -= this.pageRows(); this.body.scrollTop -= this.pageRows() * this.rowHeight; break;
                case ' ':
                case 'Enter':
                    var d = at.g - (this.hasHeader ? 1 : 0);
                    if (d < 0 && event.target.hasAttribute('data-column-id')) { // a header: the server sorts the column
                        event.preventDefault();
                        this.sendDataToServer({ col: parseInt(event.target.getAttribute('data-column-id'), 10), sort: 1 });
                        return;
                    }
                    if (d < 0 || d >= this.absRowCount) return; // a footer is not selected
                    event.preventDefault();
                    if (!this.pendingFocus) this.selectRow(event.target, event.shiftKey); // else the cell is not the one the keys led to
                    return;
                case 'F2': // goes to the widget when the key is up: a key up in a filter is a change to its server
                    event.preventDefault();
                    return;
                default: return;
            }
            event.preventDefault();
            this.focusCell(Math.min(Math.max(g, 0), this.lastRow()), Math.min(Math.max(col, 0), Math.max(0, this.colCount - 1)));
        },

        // As a click on the row does: the server's selection model decides (Shift: the range from the last one)
        selectRow: function (cell, shift) {
            cell.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, shiftKey: shift }));
        },

        // F2 on a cell goes to its widget (a filter, a check box), Escape comes back to the cell
        onKeyUp: function (event) {
            if (event.key !== 'F2' || !this.locate(event.target)) return;
            var widget = event.target.querySelector('input,select,textarea,button,a[href]');
            if (widget) widget.focus();
        },

        leaveWidget: function (event) {
            var cell = event.target.closest('[role=gridcell],[role=columnheader]');
            if (!cell || !this.locate(cell)) return;
            event.preventDefault();
            cell.focus({ preventScroll: true });
        }

    };

})();
