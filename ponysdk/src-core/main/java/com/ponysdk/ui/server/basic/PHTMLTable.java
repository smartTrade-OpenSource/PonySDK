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

import com.ponysdk.core.instruction.Add;
import com.ponysdk.core.instruction.Remove;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

/**
 * PHTMLTable contains the common table algorithms for {@link PGrid} and {@link PFlexTable}.
 * <p>
 * <img class='gallery' src='doc-files/PTable.png'/>
 * </p>
 */
public abstract class PHTMLTable extends PPanel {

    protected class Row implements Comparable<Row> {

        private int value;

        protected Row(final int value) {
            this.value = value;
        }

        public void setValue(final int value) {
            this.value = value;
        }

        @Override
        public int compareTo(final Row row) {
            return value >= row.value ? (value != row.value ? 1 : 0) : -1;
        }
    }

    protected class Cell {

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

        private Map<Integer, Set<String>> styleNames = new HashMap<Integer, Set<String>>();

        public void addStyleName(final int row, final String styleName) {
            Set<String> styles = styleNames.get(row);
            if (styles == null) {
                styles = new HashSet<String>();

                styleNames.put(row, styles);
            }
            if (styles.add(styleName)) {
                final Update update = new Update(ID);
                update.put(PROPERTY.ROW, row);
                update.put(PROPERTY.HTMLTABLE_ROW_STYLE, true);
                update.put(PROPERTY.ROW_FORMATTER_ADD_STYLE_NAME, styleName);
                getPonySession().stackInstruction(update);
            }
        }

        public void removeStyleName(final int row, final String styleName) {
            final Set<String> styles = styleNames.get(row);

            if (styles == null) return;

            if (styles.remove(styleName)) {
                final Update update = new Update(ID);
                update.put(PROPERTY.ROW, row);
                update.put(PROPERTY.HTMLTABLE_ROW_STYLE, true);
                update.put(PROPERTY.ROW_FORMATTER_REMOVE_STYLE_NAME, styleName);
                getPonySession().stackInstruction(update);
            }
        }

        protected void insertRowStyle(final int row) {
            final Map<Integer, Set<String>> temp = new HashMap<Integer, Set<String>>();
            for (final Entry<Integer, Set<String>> entry : styleNames.entrySet()) {
                if (entry.getKey() >= row) {
                    temp.put(entry.getKey() + 1, entry.getValue());
                } else temp.put(entry.getKey(), entry.getValue());
            }
            temp.put(row, new HashSet<String>());
            styleNames = temp;
        }

        protected void removeRowStyle(final int row) {
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

        public void addStyleName(final int row, final int column, final String styleName) {
            final Update update = new Update(ID);
            update.put(PROPERTY.ROW, row);
            update.put(PROPERTY.COLUMN, column);
            update.put(PROPERTY.CELL_FORMATTER_ADD_STYLE_NAME, styleName);
            update.put(PROPERTY.HTMLTABLE_CELL_STYLE, true);
            getPonySession().stackInstruction(update);
        }

        public void removeStyleName(final int row, final int column, final String styleName) {
            final Update update = new Update(ID);
            update.put(PROPERTY.ROW, row);
            update.put(PROPERTY.COLUMN, column);
            update.put(PROPERTY.CELL_FORMATTER_REMOVE_STYLE_NAME, styleName);
            update.put(PROPERTY.HTMLTABLE_CELL_STYLE, true);
            getPonySession().stackInstruction(update);
        }

        public void setVerticalAlignment(final int row, final int column, final PVerticalAlignment align) {
            final Update update = new Update(getID());
            update.put(PROPERTY.ROW, row);
            update.put(PROPERTY.COLUMN, column);
            update.put(PROPERTY.CELL_VERTICAL_ALIGNMENT, align.ordinal());
            update.put(PROPERTY.HTMLTABLE_CELL_STYLE, true);
            getPonySession().stackInstruction(update);
        }

        public void setHorizontalAlignment(final int row, final int column, final PHorizontalAlignment align) {
            final Update update = new Update(getID());
            update.put(PROPERTY.ROW, row);
            update.put(PROPERTY.COLUMN, column);
            update.put(PROPERTY.CELL_HORIZONTAL_ALIGNMENT, align.ordinal());
            update.put(PROPERTY.HTMLTABLE_CELL_STYLE, true);
            getPonySession().stackInstruction(update);
        }
    }

    public class PColumnFormatter {

        public void setWidth(final int column, final String width) {
            final Update update = new Update(ID);
            update.put(PROPERTY.HTMLTABLE_COLUMN_STYLE, true);
            update.put(PROPERTY.COLUMN_FORMATTER_COLUMN_WIDTH);
            update.put(PROPERTY.COLUMN, column);
            update.put(PROPERTY.WIDTH, width);
            getPonySession().stackInstruction(update);
        }

        public void addStyleName(final int column, final String styleName) {
            final Update update = new Update(ID);
            update.put(PROPERTY.HTMLTABLE_COLUMN_STYLE, true);
            update.put(PROPERTY.COLUMN_FORMATTER_ADD_STYLE_NAME, styleName);
            update.put(PROPERTY.COLUMN, column);
            getPonySession().stackInstruction(update);
        }

        public void removeStyleName(final int column, final String styleName) {
            final Update update = new Update(ID);
            update.put(PROPERTY.HTMLTABLE_COLUMN_STYLE, true);
            update.put(PROPERTY.COLUMN_FORMATTER_REMOVE_STYLE_NAME, styleName);
            update.put(PROPERTY.COLUMN, column);
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
        return columnByRow.lastKey().value + 1;
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

    public PWidget getWidget(final int row, final int column) {
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

        stackUpdate(PROPERTY.CLEAR, "");
    }

    public void removeRow(final int row) {
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

        stackUpdate(PROPERTY.CLEAR_ROW, row);

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
        stackUpdate(PROPERTY.INSERT_ROW, row);
    }

    @Override
    public boolean remove(final PWidget widget) {
        return remove(widget, true);
    }

    private boolean remove(final PWidget widget, final boolean physicalDetach) {
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

    public void setBorderWidth(final int width) {
        this.borderWidth = width;
        stackUpdate(PROPERTY.BORDER_WIDTH, width);
    }

    public void setCellPadding(final int padding) {
        cellPadding = padding;
        stackUpdate(PROPERTY.CELL_PADDING, padding);
    }

    public void setCellSpacing(final int spacing) {
        cellSpacing = spacing;
        stackUpdate(PROPERTY.CELL_SPACING, spacing);
    }

    protected void setCellFormatter(final PCellFormatter cellFormatter) {
        this.cellFormatter = cellFormatter;
    }

    public void setWidget(final int row, final int column, final PWidget widget) {
        if (widget != null) {
            widget.removeFromParent();

            // Removes any existing widget.
            clearCell(row, column);

            // Logical attach.
            addWidgetToMap(row, column, widget);

            // Physical attach.
            final Add add = new Add(widget.getID(), getID());
            add.put(PROPERTY.ROW, row);
            add.put(PROPERTY.CELL, column);
            getPonySession().stackInstruction(add);
            adopt(widget);
        }
    }

    private PWidget getWidgetFromMap(final int row, final int column) {
        final Map<Integer, PWidget> cellByColumn = columnByRow.get(new Row(row));
        if (cellByColumn != null) { return cellByColumn.get(column); }
        return null;
    }

    private PWidget removeWidgetFromMap(final PWidget widget) {
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

    private void addWidgetToMap(final int row, final int column, final PWidget widget) {
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