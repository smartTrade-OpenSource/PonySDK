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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.model.ServerBinaryModel;

/**
 * PHTMLTable contains the common table algorithms for {@link PGrid} and
 * {@link PFlexTable}.
 */
public abstract class PHTMLTable<T extends PCellFormatter> extends PPanel {

    private final TreeMap<Row, TreeMap<Integer, PWidget>> columnByRow = new TreeMap<>();
    private final Map<PWidget, Cell> cellByWidget = new HashMap<>();
    private final PColumnFormatter columnFormatter = new PColumnFormatter();
    private final PRowFormatter rowFormatter = new PRowFormatter();

    private T cellFormatter;
    private int cellPadding;
    private int cellSpacing;
    private int borderWidth;

    protected PHTMLTable() {
    }

    @Override
    void init0() {
        super.init0();
        cellByWidget.keySet().forEach(widget -> widget.attach(window, frame));
    }

    public int getRowCount() {
        if (columnByRow.isEmpty()) return 0;
        else return columnByRow.lastKey().value + 1;
    }

    public int getCellCount(final int row) {
        final TreeMap<Integer, PWidget> cellByColumn = columnByRow.get(new Row(row));
        if (cellByColumn == null || cellByColumn.isEmpty()) return 0;
        return cellByColumn.lastKey() + 1;
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
        final List<PWidget> values = new ArrayList<>();
        columnByRow.values().forEach(widgetByColumn -> values.addAll(widgetByColumn.values()));

        values.forEach(widget -> remove(widget, false));

        saveUpdate(writer -> writer.write(ServerToClientModel.CLEAR));
    }

    public void removeRow(final int row) {
        final TreeMap<Integer, PWidget> widgetByColumn = columnByRow.remove(new Row(row));
        if (widgetByColumn == null) return;
        getRowFormatter().removeRowStyle(row);

        final List<PWidget> values = new ArrayList<>(widgetByColumn.values());
        values.forEach(widget -> remove(widget, false));

        for (final Entry<Row, TreeMap<Integer, PWidget>> entry : columnByRow.entrySet()) {
            final Row irow = entry.getKey();
            if (irow.value > row) {
                for (final PWidget widget : entry.getValue().values()) {
                    final Cell cell = cellByWidget.get(widget);
                    cell.row = cell.row - 1;
                }
                irow.value = irow.value - 1;
            }
        }

        saveUpdate(writer -> writer.write(ServerToClientModel.CLEAR_ROW, row));
    }

    public void insertRow(final int row) {
        for (final Entry<Row, TreeMap<Integer, PWidget>> entry : columnByRow.entrySet()) {
            final Row irow = entry.getKey();
            if (irow.value >= row) {
                for (final PWidget widget : entry.getValue().values()) {
                    final Cell cell = cellByWidget.get(widget);
                    cell.row = cell.row + 1;
                }
                irow.value = irow.value + 1;
            }
        }
        rowFormatter.insertRowStyle(row);
        saveUpdate(writer -> writer.write(ServerToClientModel.INSERT_ROW, row));
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
                    widget.saveRemove(widget.getID(), ID);
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

            // Physical attach.
            widget.attach(window, frame);
            widget.saveAdd(widget.getID(), ID, new ServerBinaryModel(ServerToClientModel.ROW, row),
                new ServerBinaryModel(ServerToClientModel.COLUMN, column));

        }
    }

    private PWidget getWidgetFromMap(final int row, final int column) {
        final Map<Integer, PWidget> cellByColumn = columnByRow.get(new Row(row));
        if (cellByColumn != null) {
            return cellByColumn.get(column);
        }
        return null;
    }

    private PWidget removeWidgetFromMap(final PWidget widget) {
        final Cell cell = cellByWidget.remove(widget);
        if (cell == null) return null; // already removed
        final Row row = new Row(cell.row);
        final Map<Integer, PWidget> cellByColumn = columnByRow.get(row);
        if (cellByColumn != null) {
            final PWidget w = cellByColumn.remove(cell.column);
            if (cellByColumn.isEmpty()) {
                columnByRow.remove(row);
            }
            return w;
        }
        return null;
    }

    private void addWidgetToMap(final int row, final int column, final PWidget widget) {
        final Row irow = new Row(row);
        final Cell cell = new Cell(row, column);
        cellByWidget.put(widget, cell);
        TreeMap<Integer, PWidget> cellByColumn = columnByRow.get(irow);
        if (cellByColumn == null) {
            cellByColumn = new TreeMap<>();
            columnByRow.put(irow, cellByColumn);
        }
        cellByColumn.put(column, widget);
    }

    @Override
    public Iterator<PWidget> iterator() {
        return Collections.emptyIterator();
    }

    public PRowFormatter getRowFormatter() {
        return rowFormatter;
    }

    static class Row implements Comparable<Row> {

        private int value;

        protected Row(final int value) {
            this.value = value;
        }

        public void setValue(final int value) {
            this.value = value;
        }

        @Override
        public int compareTo(final Row row) {
            return value >= row.value ? value != row.value ? 1 : 0 : -1;
        }
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
            Set<String> styles = styleNames.get(row);
            if (styles == null) {
                styles = new HashSet<>();
                styleNames.put(row, styles);
            }

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
    protected String dumpDOM() {
        String DOM = "<table>";

        for (Entry<Row, TreeMap<Integer, PWidget>> entry : columnByRow.entrySet()) {
            DOM += "<tr row = \"" + entry.getKey().value + "\">";
            for (Entry<Integer, PWidget> td : entry.getValue().entrySet()) {
                DOM += "<td col = \"" + td.getKey() + "\">" + td.getValue().dumpDOM() + "</td>";
            }
            DOM += "</tr>";
        }

        DOM += "</table>";

        return DOM;
    }
}
