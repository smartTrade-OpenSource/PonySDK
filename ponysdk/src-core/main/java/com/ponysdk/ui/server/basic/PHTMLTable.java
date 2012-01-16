/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Remove;
import com.ponysdk.ui.terminal.instruction.Update;

public abstract class PHTMLTable extends PPanel {

    protected class Row implements Comparable<Row> {

        private int value;

        protected Row(int value) {
            this.value = value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(Row row) {
            return value >= row.value ? (value != row.value ? 1 : 0) : -1;
        }
    }

    protected class Cell {

        private int row;

        private int column;

        protected Cell(int rowIndex, int cellIndex) {
            this.column = cellIndex;
            this.row = rowIndex;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public void setColumn(int column) {
            this.column = column;
        }

    }

    public class PRowFormatter {

        private Map<Integer, Set<String>> styleNames = new HashMap<Integer, Set<String>>();

        public void addStyleName(final int row, final String styleName) {
            Set<String> styles = styleNames.get(row);
            if (styles == null) {
                styles = new HashSet<String>();

                styleNames.put(row, styles);
            }
            if (styles.add(styleName)) {
                final Update update = new Update(ID);
                update.setMainPropertyKey(PropertyKey.HTMLTABLE_ROW_STYLE);
                update.getMainProperty().setProperty(PropertyKey.ROW, row);
                update.getMainProperty().setProperty(PropertyKey.ROW_FORMATTER_ADD_STYLE_NAME, styleName);
                getPonySession().stackInstruction(update);
            }
        }

        public void removeStyleName(final int row, final String styleName) {
            final Set<String> styles = styleNames.get(row);

            if (styles == null) return;

            if (styles.remove(styleName)) {
                final Update update = new Update(ID);
                update.setMainPropertyKey(PropertyKey.HTMLTABLE_ROW_STYLE);
                update.getMainProperty().setProperty(PropertyKey.ROW, row);
                update.getMainProperty().setProperty(PropertyKey.ROW_FORMATTER_REMOVE_STYLE_NAME, styleName);
                getPonySession().stackInstruction(update);
            }
        }

        public void insertRowStyle(int row) {
            final Map<Integer, Set<String>> temp = new HashMap<Integer, Set<String>>();
            for (final Entry<Integer, Set<String>> entry : styleNames.entrySet()) {
                if (entry.getKey() >= row) {
                    temp.put(entry.getKey() + 1, entry.getValue());
                } else temp.put(entry.getKey(), entry.getValue());
            }
            temp.put(row, new HashSet<String>());
            styleNames = temp;
        }

        public void removeRowStyle(int row) {
            styleNames.remove(row);
            final Map<Integer, Set<String>> temp = new HashMap<Integer, Set<String>>();
            for (final Entry<Integer, Set<String>> entry : styleNames.entrySet()) {
                if (entry.getKey() > row) {
                    temp.put(entry.getKey() - 1, entry.getValue());
                } else temp.put(entry.getKey(), entry.getValue());
            }
            styleNames = temp;
        }
    }

    public class PCellFormatter {

        public void addStyleName(int row, int column, String styleName) {
            final Update update = new Update(ID);
            update.setMainPropertyKey(PropertyKey.HTMLTABLE_CELL_STYLE);
            update.getMainProperty().setProperty(PropertyKey.ROW, row);
            update.getMainProperty().setProperty(PropertyKey.COLUMN, column);
            update.getMainProperty().setProperty(PropertyKey.CELL_FORMATTER_ADD_STYLE_NAME, styleName);
            getPonySession().stackInstruction(update);
        }

        public void removeStyleName(int row, int column, String styleName) {
            final Update update = new Update(ID);
            update.setMainPropertyKey(PropertyKey.HTMLTABLE_CELL_STYLE);
            update.getMainProperty().setProperty(PropertyKey.ROW, row);
            update.getMainProperty().setProperty(PropertyKey.COLUMN, column);
            update.getMainProperty().setProperty(PropertyKey.CELL_FORMATTER_REMOVE_STYLE_NAME, styleName);
            getPonySession().stackInstruction(update);
        }
    }

    public class PColumnFormatter {

        public void setWidth(int column, String width) {
            final Update update = new Update(ID);
            update.setMainPropertyKey(PropertyKey.COLUMN_FORMATTER_COLUMN_WIDTH);
            update.getMainProperty().setProperty(PropertyKey.COLUMN, column);
            update.getMainProperty().setProperty(PropertyKey.WIDTH, width);
            getPonySession().stackInstruction(update);
        }

        public void addStyleName(int column, String styleName) {
            final Update update = new Update(ID);
            update.setMainPropertyKey(PropertyKey.COLUMN_FORMATTER_ADD_STYLE_NAME);
            update.getMainProperty().setProperty(PropertyKey.COLUMN, column);
            update.getMainProperty().setProperty(PropertyKey.STYLE_NAME, styleName);
            getPonySession().stackInstruction(update);
        }

        public void removeStyleName(int column, String styleName) {
            final Update update = new Update(ID);
            update.setMainPropertyKey(PropertyKey.COLUMN_FORMATTER_REMOVE_STYLE_NAME);
            update.getMainProperty().setProperty(PropertyKey.COLUMN, column);
            update.getMainProperty().setProperty(PropertyKey.STYLE_NAME, styleName);
            getPonySession().stackInstruction(update);
        }
    }

    private final TreeMap<Row, TreeMap<Integer, PWidget>> columnByRow = new TreeMap<Row, TreeMap<Integer, PWidget>>();

    private final Map<PWidget, Cell> cellByWidget = new HashMap<PWidget, PHTMLTable.Cell>();

    private PCellFormatter cellFormatter;

    private final PColumnFormatter columnFormatter = new PColumnFormatter();

    private int cellPadding;

    private int cellSpacing;

    private int borderWidth;

    private final PRowFormatter rowFormatter = new PRowFormatter();

    public int getRowCount() {
        if (columnByRow.isEmpty()) return 0;
        return columnByRow.lastKey().value;
    }

    public int getCellCount(int row) {
        final TreeMap<Integer, PWidget> cellByColumn = columnByRow.get(new Row(row));
        if (cellByColumn == null || cellByColumn.isEmpty()) return 0;
        return cellByColumn.lastKey();
    }

    public void clearCell(int row, int col) {
        final PWidget widget = getWidgetFromMap(row, col);
        if (widget != null) {
            remove(widget);
        }
    }

    public PCellFormatter getCellFormatter() {
        return cellFormatter;
    }

    public PColumnFormatter getColumnFormatter() {
        return columnFormatter;
    }

    public int getCellPadding() {
        return cellPadding;
    }

    public int getCellSpacing() {
        return cellSpacing;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public PWidget getWidget(int row, int column) {
        return getWidgetFromMap(row, column);
    }

    @Override
    public void clear() {
        final List<PWidget> values = new ArrayList<PWidget>();
        for (final TreeMap<Integer, PWidget> widgetByColumn : columnByRow.values()) {
            values.addAll(widgetByColumn.values());
        }

        for (final PWidget w : values) {
            remove(w, false);
        }

        stackUpdate(PropertyKey.CLEAR, "");
    }

    public void removeRow(int row) {
        final TreeMap<Integer, PWidget> widgetByColumn = columnByRow.remove(new Row(row));
        if (widgetByColumn == null) return;
        getRowFormatter().removeRowStyle(row);

        final List<PWidget> values = new ArrayList<PWidget>(widgetByColumn.values());
        for (final PWidget w : values) {
            remove(w, false);
        }

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

        stackUpdate(PropertyKey.CLEAR_ROW, Integer.toString(row));

    }

    public void insertRow(int row) {
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
        stackUpdate(PropertyKey.INSERT_ROW, Integer.toString(row));
    }

    @Override
    public boolean remove(PWidget widget) {
        return remove(widget, true);
    }

    private boolean remove(PWidget widget, boolean physicalDetach) {
        // Validate.
        if (widget.getParent() != this) { return false; }

        // Orphan.
        try {
            orphan(widget);
        } finally {
            // Physical detach.
            if (physicalDetach) {
                final Remove remove = new Remove(widget.getID(), getID());
                getPonySession().stackInstruction(remove);
            }

            // Logical detach.
            removeWidgetFromMap(widget);
        }
        return true;
    }

    public void setBorderWidth(int width) {
        this.borderWidth = width;
        stackUpdate(PropertyKey.BORDER_WIDTH, Integer.toString(width));
    }

    public void setCellPadding(int padding) {
        cellPadding = padding;
        stackUpdate(PropertyKey.CELL_PADDING, Integer.toString(padding));
    }

    public void setCellSpacing(int spacing) {
        cellSpacing = spacing;
        stackUpdate(PropertyKey.CELL_SPACING, Integer.toString(spacing));
    }

    protected void setCellFormatter(PCellFormatter cellFormatter) {
        this.cellFormatter = cellFormatter;
    }

    public void setWidget(int row, int column, PWidget widget) {
        if (widget != null) {
            widget.removeFromParent();

            // Removes any existing widget.
            clearCell(row, column);

            // Logical attach.
            addWidgetToMap(row, column, widget);

            // Physical attach.
            final Add add = new Add(widget.getID(), getID());
            add.getMainProperty().setProperty(PropertyKey.ROW, row);
            add.getMainProperty().setProperty(PropertyKey.CELL, column);
            getPonySession().stackInstruction(add);
            adopt(widget);
        }
    }

    private PWidget getWidgetFromMap(int row, int column) {
        final Map<Integer, PWidget> cellByColumn = columnByRow.get(new Row(row));
        if (cellByColumn != null) { return cellByColumn.get(column); }
        return null;
    }

    private PWidget removeWidgetFromMap(PWidget widget) {
        final Cell cell = cellByWidget.remove(widget);
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

    private void addWidgetToMap(int row, int column, PWidget widget) {
        final Row irow = new Row(row);
        final Cell cell = new Cell(row, column);
        cellByWidget.put(widget, cell);
        TreeMap<Integer, PWidget> cellByColumn = columnByRow.get(irow);
        if (cellByColumn == null) {
            cellByColumn = new TreeMap<Integer, PWidget>();
            columnByRow.put(irow, cellByColumn);
        }
        cellByColumn.put(column, widget);
    }

    @Override
    public Iterator<PWidget> iterator() {
        throw new UnsupportedOperationException("unsupported iterator() method called in PHTMLTable");
    }

    public PRowFormatter getRowFormatter() {
        return rowFormatter;
    }

}