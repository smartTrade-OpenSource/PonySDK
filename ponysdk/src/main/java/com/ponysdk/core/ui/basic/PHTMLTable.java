/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.model.ServerBinaryModel;

import java.util.*;
import java.util.Map.Entry;

/**
 * PHTMLTable contains the common table algorithms for {@link PGrid} and
 * {@link PFlexTable}.
 */
public abstract class PHTMLTable<T extends PCellFormatter> extends PPanel {

    /**
     * Widgets of the cells: {@code rows.get(r)[c]}, null where a cell holds no widget, a null row when none of its
     * cells does, and no trailing null row. Was a TreeMap of TreeMaps plus a HashMap: about 100 bytes per cell.
     */
    private final ArrayList<PWidget[]> rows = new ArrayList<>();
    /** Position of each cell widget, kept up to date when rows shift: removing a widget does not scan the table. */
    private final IdentityHashMap<PWidget, Cell> cellByWidget = new IdentityHashMap<>();
    private final PColumnFormatter columnFormatter = new PColumnFormatter();
    private final PRowFormatter rowFormatter = new PRowFormatter();

    private T cellFormatter;
    private int cellPadding;
    private int cellSpacing;
    private int borderWidth;

    protected PHTMLTable() {
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getCellCount(final int row) {
        final PWidget[] cells = row < rows.size() ? rows.get(row) : null;
        if (cells == null) return 0;
        int last = cells.length - 1;
        while (last >= 0 && cells[last] == null) last--;
        return last + 1;
    }

    public void clearCell(final int row, final int col) {
        final PWidget widget = getWidgetFromMap(row, col);
        if (widget != null) {
            remove(widget);
        }
    }

    public T getCellFormatter() {
        return cellFormatter;
    }

    protected void setCellFormatter(final T cellFormatter) {
        this.cellFormatter = cellFormatter;
    }

    public PColumnFormatter getColumnFormatter() {
        return columnFormatter;
    }

    public int getCellPadding() {
        return cellPadding;
    }

    public void setCellPadding(final int padding) {
        if (Objects.equals(this.cellPadding, padding)) return;
        cellPadding = padding;
        saveUpdate(writer -> writer.write(ServerToClientModel.CELL_PADDING, padding));
    }

    public int getCellSpacing() {
        return cellSpacing;
    }

    public void setCellSpacing(final int spacing) {
        if (Objects.equals(this.cellSpacing, spacing)) return;
        cellSpacing = spacing;
        saveUpdate(writer -> writer.write(ServerToClientModel.CELL_SPACING, spacing));
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(final int width) {
        if (Objects.equals(this.borderWidth, width)) return;
        this.borderWidth = width;
        saveUpdate(writer -> writer.write(ServerToClientModel.BORDER_WIDTH, width));
    }

    public PWidget getWidget(final int row, final int column) {
        return getWidgetFromMap(row, column);
    }

    @Override
    public void clear() {
        forEach(widget -> remove(widget, false));

        saveUpdate(writer -> writer.write(ServerToClientModel.CLEAR));
    }

    public void removeRow(final int row) {
        final PWidget[] cells = row < rows.size() ? rows.get(row) : null;
        if (cells == null) return; // a row without widget is not known here: nothing shifts (unchanged behaviour)
        getRowFormatter().removeRowStyle(row);

        for (final PWidget widget : cells) {
            if (widget != null) remove(widget, false);
        }
        if (row < rows.size()) {
            rows.remove(row);
            shiftRows(row, -1);
            trimTrailingEmptyRows();
        }

        saveUpdate(writer -> writer.write(ServerToClientModel.CLEAR_ROW, row));
    }

    public void insertRow(final int row) {
        if (row < rows.size()) {
            rows.add(row, null);
            shiftRows(row + 1, 1);
        }
        rowFormatter.insertRowStyle(row);
        saveUpdate(writer -> writer.write(ServerToClientModel.INSERT_ROW, row));
    }

    /** Moves the recorded position of the widgets of the rows from {@code from} on (they already moved in the list). */
    private void shiftRows(final int from, final int delta) {
        for (int r = from; r < rows.size(); r++) {
            final PWidget[] cells = rows.get(r);
            if (cells == null) continue;
            for (final PWidget widget : cells) {
                if (widget != null) cellByWidget.get(widget).row += delta;
            }
        }
    }

    private void trimTrailingEmptyRows() {
        while (!rows.isEmpty() && rows.get(rows.size() - 1) == null) rows.remove(rows.size() - 1);
    }

    @Override
    public boolean remove(final PWidget widget) {
        return remove(widget, true);
    }

    private boolean remove(final PWidget widget, final boolean physicalDetach) {
        // Validate.
        if (widget.getParent() != this) {
            return false;
        }

        // Orphan.
        try {
            orphan(widget);
        } finally {
            // Logical detach.
            if (removeWidgetFromMap(widget) != null) {
                // Physical detach.
                if (physicalDetach) {
                    if (isInitialized()) widget.saveRemove(widget.getID(), ID);
                    else safeStackedInstructions().append(() -> { // in order with the adds (setWidget)
                        if (widget.isInitialized() && widget.getParent() != this) widget.saveRemove(widget.getID(), ID);
                    });
                }
            }
        }
        return true;
    }

    public void setWidget(final int row, final int column, final IsPWidget widget) {
        if (widget != null) {
            setWidget(row, column, widget.asWidget());
        }
    }

    public void setWidget(final int row, final int column, final PWidget widget) {
        if (widget != null) {
            widget.removeFromParent();

            // Removes any existing widget.
            clearCell(row, column);

            // Logical attach.
            addWidgetToMap(row, column, widget);
            adopt(widget);

            // Physical attach. Before the table is on the terminal, the add waits in the table's queue, in order with
            // its rows and styles (insertRow, removeRow, formatters): on the widget's own queue, it would overtake
            // them. A widget moved to another parent meanwhile is that parent's.
            if (isInitialized()) addToTerminal(widget, row, column);
            else safeStackedInstructions().append(() -> {
                if (widget.getParent() == this || widget.getParent() == null) addToTerminal(widget, row, column);
            });
        }
    }

    private void addToTerminal(final PWidget widget, final int row, final int column) {
        widget.attach(window, getFrame());
        widget.saveAdd(widget.getID(), ID, new ServerBinaryModel(ServerToClientModel.ROW, row),
            new ServerBinaryModel(ServerToClientModel.COLUMN, column));
    }

    private PWidget getWidgetFromMap(final int row, final int column) {
        final PWidget[] cells = row >= 0 && row < rows.size() ? rows.get(row) : null;
        return cells != null && column >= 0 && column < cells.length ? cells[column] : null;
    }

    private PWidget removeWidgetFromMap(final PWidget widget) {
        final Cell cell = cellByWidget.remove(widget);
        if (cell == null) return null; // already removed
        final PWidget[] cells = cell.row < rows.size() ? rows.get(cell.row) : null;
        if (cells == null || cells[cell.column] != widget) return null;
        cells[cell.column] = null;
        for (final PWidget w : cells) {
            if (w != null) return widget;
        }
        rows.set(cell.row, null); // its last widget
        trimTrailingEmptyRows();
        return widget;
    }

    private void addWidgetToMap(final int row, final int column, final PWidget widget) {
        cellByWidget.put(widget, new Cell(row, column));
        while (rows.size() <= row) rows.add(null);
        PWidget[] cells = rows.get(row);
        if (cells == null) rows.set(row, cells = new PWidget[column + 1]);
        else if (cells.length <= column) rows.set(row, cells = Arrays.copyOf(cells, Math.max(column + 1, cells.length * 2)));
        cells[column] = widget;
    }

    /** The widgets of the cells, row by row: a snapshot, iterating (attach, destroy) may remove cells. */
    @Override
    public Iterator<PWidget> iterator() {
        final List<PWidget> widgets = new ArrayList<>(cellByWidget.size());
        for (final PWidget[] cells : rows) {
            if (cells == null) continue;
            for (final PWidget widget : cells) {
                if (widget != null) widgets.add(widget);
            }
        }
        return Collections.unmodifiableList(widgets).iterator();
    }

    public PRowFormatter getRowFormatter() {
        return rowFormatter;
    }

    protected static class Cell {

        private int row;

        private int column;

        protected Cell(final int rowIndex, final int cellIndex) {
            this.column = cellIndex;
            this.row = rowIndex;
        }

        public void setRow(final int row) {
            this.row = row;
        }

        public void setColumn(final int column) {
            this.column = column;
        }

    }

    public class PRowFormatter {

        private Map<Integer, Set<String>> styleNames = new HashMap<>();

        public void addStyleName(final int row, final String styleName) {
            Set<String> styles = styleNames.computeIfAbsent(row, k -> new HashSet<>());

            if (styles.add(styleName)) {
                saveUpdate(writer -> {
                    writer.write(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME, styleName);
                    writer.write(ServerToClientModel.ROW, row);
                });
            }
        }

        public void removeStyleName(final int row, final String styleName) {
            final Set<String> styles = styleNames.get(row);

            if (styles == null) return;

            if (styles.remove(styleName)) {
                saveUpdate(writer -> {
                    writer.write(ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME, styleName);
                    writer.write(ServerToClientModel.ROW, row);
                });
            }
        }

        public void setStyleName(final int row, final String styleName) {
            Set<String> styles = styleNames.get(row);
            if (styles == null) {
                styles = new HashSet<>();
                styleNames.put(row, styles);
            } else {
                styles.clear();
            }

            styles.add(styleName);

            saveUpdate(writer -> {
                writer.write(ServerToClientModel.ROW_FORMATTER_SET_STYLE_NAME, styleName);
                writer.write(ServerToClientModel.ROW, row);
            });
        }

        public void showRow(final int row) {
            saveUpdate(writer -> writer.write(ServerToClientModel.ROW_FORMATTER_SHOW_ROW, row));
        }

        public void hideRow(final int row) {
            saveUpdate(writer -> writer.write(ServerToClientModel.ROW_FORMATTER_HIDE_ROW, row));
        }

        protected void insertRowStyle(final int row) {
            final Map<Integer, Set<String>> temp = new HashMap<>();
            for (final Entry<Integer, Set<String>> entry : styleNames.entrySet()) {
                if (entry.getKey() >= row) {
                    temp.put(entry.getKey() + 1, entry.getValue());
                } else temp.put(entry.getKey(), entry.getValue());
            }
            temp.put(row, new HashSet<>());
            styleNames = temp;
        }

        protected void removeRowStyle(final int row) {
            styleNames.remove(row);
            final Map<Integer, Set<String>> temp = new HashMap<>();
            for (final Entry<Integer, Set<String>> entry : styleNames.entrySet()) {
                if (entry.getKey() > row) {
                    temp.put(entry.getKey() - 1, entry.getValue());
                } else temp.put(entry.getKey(), entry.getValue());
            }
            styleNames = temp;
        }
    }

    public class PColumnFormatter {

        public void setWidth(final int column, final String width) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.COLUMN_FORMATTER_COLUMN_WIDTH, width);
                writer.write(ServerToClientModel.COLUMN, column);
            });
        }

        public void addStyleName(final int column, final String styleName) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.COLUMN_FORMATTER_ADD_STYLE_NAME, styleName);
                writer.write(ServerToClientModel.COLUMN, column);
            });
        }

        public void removeStyleName(final int column, final String styleName) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.COLUMN_FORMATTER_REMOVE_STYLE_NAME, styleName);
                writer.write(ServerToClientModel.COLUMN, column);
            });
        }

        public void setStyleName(final int column, final String styleName) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.COLUMN_FORMATTER_SET_STYLE_NAME, styleName);
                writer.write(ServerToClientModel.COLUMN, column);
            });
        }
    }


    @Override
    public String dumpDOM() {
        String DOM = "<table pid=\"" + ID + "\">";

        for (int r = 0; r < rows.size(); r++) {
            final PWidget[] cells = rows.get(r);
            if (cells == null) continue;
            DOM += "<tr row = \"" + r + "\">";
            for (int c = 0; c < cells.length; c++) {
                if (cells[c] != null) DOM += "<td col = \"" + c + "\">" + cells[c].dumpDOM() + "</td>";
            }
            DOM += "</tr>";
        }

        DOM += "</table>";

        return DOM;
    }
}
