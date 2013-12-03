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

package com.ponysdk.ui.server.celltable;

import java.util.Iterator;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PWidget;

public class SimpleTable extends PElement {

    private final PElement thead = new PElement("thead");
    private final PElement tbody = new PElement("tbody");

    public SimpleTable() {
        super("table");

        super.insert(thead, 0);
        super.insert(tbody, 1);
    }

    @Override
    public void insert(final PWidget child, final int beforeIndex) {
        final PElement parentElement = (beforeIndex == 0) ? thead : tbody;
        parentElement.insert(child, beforeIndex);
    }

    public void addWidget(final IsPWidget widget, final int column, final int row, final int colspan) {

        if (row < 0) throw new IndexOutOfBoundsException("row (" + row + ") < 0)");
        if (column < 0) throw new IndexOutOfBoundsException("column (" + column + ") < 0)");

        if (row > 0) {
            addBodyWidget(widget, column, row, colspan);
        } else {
            addHeadWidget(widget, column, row, colspan);
        }
    }

    public void addBodyWidget(final IsPWidget widget, final int column, final int row, final int colspan) {

        if (row < 0) throw new IndexOutOfBoundsException("row (" + row + ") < 0)");
        if (column < 0) throw new IndexOutOfBoundsException("column (" + column + ") < 0)");

        PElement newRow;
        final int maxRowIndex = tbody.getWidgetCount();
        if (row > maxRowIndex) {
            newRow = new PElement("tr");
            tbody.add(newRow);
        } else {
            newRow = (PElement) tbody.getWidget(row - 1);
        }

        PElement newCell;
        final int maxCellIndex = newRow.getWidgetCount() - 1;
        if (column > maxCellIndex) {
            newCell = new PElement("td");
            newRow.add(newCell);
        } else {
            newCell = (PElement) newRow.getWidget(column);
            newCell.clear();
        }

        newCell.add(widget);
        newCell.addStyleName("pony-PFlextable-Cell");

        if (colspan > 1) newCell.setAttribute("colspan", colspan + "");
    }

    public void addHeadWidget(final IsPWidget widget, final int column, final int row, final int colspan) {

        if (row < 0) throw new IndexOutOfBoundsException("row (" + row + ") < 0)");
        if (column < 0) throw new IndexOutOfBoundsException("column (" + column + ") < 0)");

        PElement newRow;
        final int maxRowIndex = thead.getWidgetCount() - 1;
        if (row > maxRowIndex) {
            newRow = new PElement("tr");
            thead.add(newRow);
        } else {
            newRow = (PElement) thead.getWidget(row);
        }

        PElement newCell;
        final int maxCellIndex = newRow.getWidgetCount() - 1;
        if (column > maxCellIndex) {
            newCell = new PElement("th");
            newRow.add(newCell);
        } else {
            newCell = (PElement) newRow.getWidget(column);
            newCell.clear();
        }

        newCell.add(widget);
        newCell.addStyleName("pony-PFlextable-Cell");

        if (colspan > 1) newCell.setAttribute("colspan", colspan + "");
    }

    public void clear(final int from) {
        if (from == 0) thead.clear();

        for (int i = tbody.getWidgetCount() - 1; i >= from - 1; i++) {
            tbody.remove(i);
        }
    }

    public void removeRow(final int row) {
        checkRowBound(row);
        remove(row);
    }

    @Override
    public boolean remove(final int row) {
        final PElement parentElement = (row == 0) ? thead : tbody;
        return parentElement.remove(parentElement.getWidget(row - (row == 0 ? 0 : 1)));
    }

    public PElement getRow(final int row) {
        checkRowBound(row);

        final PElement parentElement = (row == 0) ? thead : tbody;

        return (PElement) parentElement.getWidget(row - (row == 0 ? 0 : 1));
    }

    public PElement getCell(final int row, final int column) {
        final PElement parentElement = (row == 0) ? thead : tbody;

        checkRowBound(row);
        checkColumnBound(column);
        final PElement r = (PElement) parentElement.getWidget(row);
        return (PElement) r.getWidget(column);
    }

    public Iterator<PElement> getColumnIterator(final int column) {
        checkColumnBound(column);

        return new Iterator<PElement>() {

            int index = 0;
            int rows = tbody.getWidgetCount();

            @Override
            public boolean hasNext() {
                if (index < rows) return true;
                return false;
            }

            @Override
            public PElement next() {
                final PElement row = (PElement) tbody.getWidget(index);
                final PElement next = (PElement) row.getWidget(column);
                index++;
                return next;
            }

            @Override
            public void remove() {
                final PElement row = (PElement) tbody.getWidget(index);
                row.remove(index);
                index++;
            }

        };
    }

    public void moveRow(final int index, final int beforeIndex) {
        checkRowBound(index);
        checkRowBound(beforeIndex);

        final PElement parentElement = (index == 0) ? thead : tbody;

        final PWidget source = parentElement.getWidget(index - (index == 0 ? 0 : 1));
        parentElement.insert(source, beforeIndex - (beforeIndex == 0 ? 0 : 1));
    }

    public void moveColumn(final int index, final int beforeIndex) {
        checkColumnBound(index);
        checkColumnBound(beforeIndex);

        final PElement parentElement = (index == 0) ? thead : tbody;

        for (int r = 0; r < parentElement.getWidgetCount(); r++) {
            final PElement row = (PElement) parentElement.getWidget(r);
            row.insert(row.getWidget(index), beforeIndex);
        }
    }

    private void checkRowBound(final int beforeIndex) {
        final PElement parentElement = (beforeIndex == 0) ? thead : tbody;

        if ((beforeIndex < 0) || (beforeIndex >= parentElement.getWidgetCount() + (beforeIndex == 0 ? 0 : 1))) {
            if ((beforeIndex < 0)) throw new IndexOutOfBoundsException("(beforeIndex (" + beforeIndex + ") < 0)");
            else throw new IndexOutOfBoundsException("beforeIndex (" + beforeIndex + ") >= size (" + parentElement.getWidgetCount() + ")");
        }
    }

    private void checkColumnBound(final int beforeIndex) {
        return;
        // final PElement parentElement = (beforeIndex == 0) ? thead : tbody;
        // if ((beforeIndex < 0) || (beforeIndex >= parentElement.getWidgetCount())) {
        // if ((beforeIndex < 0)) throw new IndexOutOfBoundsException("(beforeIndex (" + beforeIndex +
        // ") < 0)");
        // else throw new IndexOutOfBoundsException("beforeIndex (" + beforeIndex + ") >= size (" +
        // parentElement.getWidgetCount() + ")");
        // }
    }
}
