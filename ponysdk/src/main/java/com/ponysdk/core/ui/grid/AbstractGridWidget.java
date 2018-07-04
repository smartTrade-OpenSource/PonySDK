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

package com.ponysdk.core.ui.grid;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.list.SimpleListView;

public abstract class AbstractGridWidget extends PElement implements SimpleListView {

    private final PComplexPanel head;
    private final PComplexPanel body;

    public AbstractGridWidget(final String tag) {
        super(tag);

        addStyleName("pony-PFlexTable");
        addStyleName("pony-SimpleList");

        head = createTableHeader();
        add(head);

        body = createTableBody();
        add(body);
    }

    @Override
    public void insert(final PWidget child, final int beforeIndex) {
        final PComplexPanel parentElement = beforeIndex == 0 ? head : body;
        parentElement.add(child);
    }

    @Override
    public void addWidget(final IsPWidget widget, final int column, final int row, final int colspan) {
        if (row < 0) throw new IndexOutOfBoundsException("row (" + row + ") < 0)");
        if (column < 0) throw new IndexOutOfBoundsException("column (" + column + ") < 0)");

        if (row > 0) addBodyWidget(widget, column, row - 1, colspan);
        else addHeadWidget(widget, column, row, colspan);
    }

    public void addBodyWidget(final IsPWidget widget, final int column, final int row, final int colspan) {
        if (row < 0) throw new IndexOutOfBoundsException("row (" + row + ") < 0)");
        if (column < 0) throw new IndexOutOfBoundsException("column (" + column + ") < 0)");

        PComplexPanel newRow;
        final int maxRowIndex = body.getWidgetCount();
        if (row >= maxRowIndex) {
            newRow = createTableRow();
            body.add(newRow);
        } else {
            newRow = (PComplexPanel) body.getWidget(row);
        }

        PComplexPanel newCell;
        final int maxCellIndex = newRow.getWidgetCount() - 1;
        if (column > maxCellIndex) {
            newCell = createTableCell();
            newRow.add(newCell);
        } else {
            newCell = (PComplexPanel) newRow.getWidget(column);
            newCell.clear();
        }

        newCell.add(widget);
        newCell.addStyleName("ptc");

        if (colspan > 1) newCell.setAttribute("colspan", colspan + "");
    }

    public void addHeadWidget(final IsPWidget widget, final int column, final int row, final int colspan) {

        if (row < 0) throw new IndexOutOfBoundsException("row (" + row + ") < 0)");
        if (column < 0) throw new IndexOutOfBoundsException("column (" + column + ") < 0)");

        PComplexPanel newRow;
        final int maxRowIndex = head.getWidgetCount() - 1;
        if (row > maxRowIndex) {
            newRow = createTableRow();
            head.add(newRow);
        } else {
            newRow = (PComplexPanel) head.getWidget(row);
        }

        PComplexPanel newCell;
        final int maxCellIndex = newRow.getWidgetCount() - 1;
        if (column > maxCellIndex) {
            newCell = createTableHeaderCell();
            newRow.add(newCell);
        } else {
            newCell = (PComplexPanel) newRow.getWidget(column);
            newCell.clear();
        }

        newCell.add(widget);
        newCell.addStyleName("ptc");

        if (colspan > 1) newCell.setAttribute("colspan", colspan + "");
    }

    @Override
    public void clear() {
        head.clear();
        body.clear();
    }

    @Override
    public void clear(final int from) {
        if (from < 0) throw new IndexOutOfBoundsException("row (" + from + ") < 0)");

        if (from == 0) {
            head.clear();
            for (int i = body.getWidgetCount() - 1; i >= 0; i--) {
                body.remove(i);
            }
        } else {
            for (int i = body.getWidgetCount() - 1; i >= from - 1; i--) {
                body.remove(i);
            }
        }
    }

    @Override
    public void removeRow(final int row) {
        checkRowBound(row);
        remove(row);
    }

    @Override
    public boolean remove(final int row) {
        final PComplexPanel parentElement = row == 0 ? head : body;
        return parentElement.remove(parentElement.getWidget(row - (row == 0 ? 0 : 1)));
    }

    public PComplexPanel getRow(final int row) {
        checkRowBound(row);

        final PComplexPanel parentElement = row == 0 ? head : body;

        return (PComplexPanel) parentElement.getWidget(row - (row == 0 ? 0 : 1));
    }

    public PComplexPanel getCell(final int row, final int column) {
        final PComplexPanel parentElement = row == 0 ? head : body;

        checkRowBound(row);
        checkColumnBound(column);
        final PComplexPanel r = (PComplexPanel) parentElement.getWidget(row - (row == 0 ? 0 : 1));
        return (PComplexPanel) r.getWidget(column);
    }

    public Iterator<PComplexPanel> getColumnIterator(final int column) {
        checkColumnBound(column);

        return new Iterator<>() {

            int index = 0;
            int rows = body.getWidgetCount();

            @Override
            public boolean hasNext() {
                return index < rows;
            }

            @Override
            public PComplexPanel next() {
                if (hasNext()) {
                    final PComplexPanel row = (PComplexPanel) body.getWidget(index);
                    final PComplexPanel next = (PComplexPanel) row.getWidget(column);
                    index++;
                    return next;
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                final PComplexPanel row = (PComplexPanel) body.getWidget(index);
                row.remove(index);
                index++;
            }

        };
    }

    @Override
    public void moveRow(final int index, final int beforeIndex) {
        checkRowBound(index);
        checkRowBound(beforeIndex);

        final PComplexPanel parentElement = index == 0 ? head : body;

        final PWidget source = parentElement.getWidget(index - (index == 0 ? 0 : 1));
        parentElement.insert(source, beforeIndex - (beforeIndex == 0 ? 0 : 1));
    }

    @Override
    public void moveColumn(final int index, final int beforeIndex) {
        checkColumnBound(index);
        checkColumnBound(beforeIndex);

        final PComplexPanel parentElement = index == 0 ? head : body;

        for (int r = 0; r < parentElement.getWidgetCount(); r++) {
            final PComplexPanel row = (PComplexPanel) parentElement.getWidget(r);
            row.insert(row.getWidget(index), beforeIndex);
        }
    }

    private void checkRowBound(final int beforeIndex) {
        final PComplexPanel parentElement = beforeIndex == 0 ? head : body;

        if (beforeIndex < 0 || beforeIndex >= parentElement.getWidgetCount() + (beforeIndex == 0 ? 0 : 1)) {
            if (beforeIndex < 0) throw new IndexOutOfBoundsException("(beforeIndex (" + beforeIndex + ") < 0)");
            else throw new IndexOutOfBoundsException(
                "beforeIndex (" + beforeIndex + ") >= size (" + parentElement.getWidgetCount() + ")");
        }
    }

    private void checkColumnBound(final int beforeIndex) {
    }

    @Override
    public void clearList() {
        clear(0);
    }

    @Override
    public void insertRow(final int row) {
        insert(createTableRow(), row);
    }

    @Override
    public void setColumns(final int size) {
        throw new IllegalAccessError("Not implemented");
    }

    @Override
    public void selectRow(final int row) {
        throw new IllegalAccessError("Not implemented");
    }

    @Override
    public void unSelectRow(final int row) {
        throw new IllegalAccessError("Not implemented");
    }

    @Override
    public void addRowStyle(final int row, final String styleName) {
        getRow(row).addStyleName(styleName);
    }

    @Override
    public void removeRowStyle(final int row, final String styleName) {
        getRow(row).removeStyleName(styleName);
    }

    @Override
    public void addColumnStyle(final int column, final String styleName) {
        final Iterator<PComplexPanel> iterator = getColumnIterator(column);
        while (iterator.hasNext()) {
            final PComplexPanel e = iterator.next();
            e.addStyleName(styleName);
        }
    }

    @Override
    public void removeColumnStyle(final int column, final String styleName) {
        final Iterator<PComplexPanel> iterator = getColumnIterator(column);
        while (iterator.hasNext()) {
            final PComplexPanel e = iterator.next();
            e.removeStyleName(styleName);
        }
    }

    @Override
    public void setColumnWidth(final int column, final String width) {
        final PComplexPanel cell = getCell(0, column);
        cell.setStyleProperty("width", width);
    }

    @Override
    public void addHeaderStyle(final String styleName) {
        getRow(0).addStyleName(styleName);
    }

    @Override
    public void addCellStyle(final int row, final int col, final String styleName) {
        getCell(row, col).addStyleName(styleName);
    }

    @Override
    public void removeCellStyle(final int row, final int col, final String styleName) {
        getCell(row, col).removeStyleName(styleName);
    }

    public void removeColumn(final int index) {
        for (int r = 0; r < head.getWidgetCount(); r++) {
            final PElement tr = (PElement) head.getWidget(r);
            tr.remove(index);
        }

        for (int r = 0; r < body.getWidgetCount(); r++) {
            final PComplexPanel tr = (PComplexPanel) body.getWidget(r);
            tr.remove(index);
        }
    }

    protected abstract PComplexPanel createTableHeader();

    protected abstract PComplexPanel createTableBody();

    public abstract PComplexPanel createTableRow();

    protected abstract PComplexPanel createTableCell();

    protected abstract PComplexPanel createTableHeaderCell();

    public PComplexPanel getHead() {
        return head;
    }

    public PComplexPanel getBody() {
        return body;
    }

}
