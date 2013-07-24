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

    public SimpleTable() {
        super("table");
    }

    public void addWidget(final IsPWidget widget, final int column, final int row, final int colspan) {

        if (row < 0) throw new IndexOutOfBoundsException("row (" + row + ") < 0)");
        if (column < 0) throw new IndexOutOfBoundsException("column (" + column + ") < 0)");

        PElement newRow;
        final int maxRowIndex = getWidgetCount() - 1;
        if (row > maxRowIndex) {
            newRow = new PElement("tr");
            add(newRow);
        } else {
            newRow = (PElement) getWidget(row);
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

    public void clear(final int from) {
        for (int i = getWidgetCount() - 1; i >= from; i++) {
            remove(i);
        }
    }

    public void removeRow(final int row) {
        checkRowBound(row);
        remove(row);
    }

    public PElement getRow(final int row) {
        checkRowBound(row);
        return (PElement) getWidget(row);
    }

    public PElement getCell(final int row, final int column) {
        checkRowBound(row);
        checkColumnBound(column);
        final PElement r = (PElement) getWidget(row);
        return (PElement) r.getWidget(column);
    }

    public Iterator<PElement> getColumnIterator(final int column) {
        checkColumnBound(column);

        return new Iterator<PElement>() {

            int index = 0;
            int rows = getWidgetCount();

            @Override
            public boolean hasNext() {
                if (index < rows) return true;
                return false;
            }

            @Override
            public PElement next() {
                final PElement row = (PElement) getWidget(index);
                final PElement next = (PElement) row.getWidget(column);
                index++;
                return next;
            }

            @Override
            public void remove() {
                final PElement row = (PElement) getWidget(index);
                row.remove(index);
                index++;
            }

        };
    }

    public void moveRow(final int index, final int beforeIndex) {
        checkRowBound(index);
        checkRowBound(beforeIndex);

        final PWidget source = getWidget(index);
        insert(source, beforeIndex);
    }

    public void moveColumn(final int index, final int beforeIndex) {
        checkColumnBound(index);
        checkColumnBound(beforeIndex);

        for (int r = 0; r < getWidgetCount(); r++) {
            final PElement row = (PElement) getWidget(r);
            row.insert(row.getWidget(index), beforeIndex);
        }
    }

    private void checkRowBound(final int beforeIndex) {
        if ((beforeIndex < 0) || (beforeIndex >= getWidgetCount())) {
            if ((beforeIndex < 0)) throw new IndexOutOfBoundsException("(beforeIndex (" + beforeIndex + ") < 0)");
            else throw new IndexOutOfBoundsException("beforeIndex (" + beforeIndex + ") >= size (" + getWidgetCount() + ")");
        }
    }

    private void checkColumnBound(final int beforeIndex) {
        final PElement header = (PElement) getWidget(0);
        if ((beforeIndex < 0) || (beforeIndex >= header.getWidgetCount())) {
            if ((beforeIndex < 0)) throw new IndexOutOfBoundsException("(beforeIndex (" + beforeIndex + ") < 0)");
            else throw new IndexOutOfBoundsException("beforeIndex (" + beforeIndex + ") >= size (" + header.getWidgetCount() + ")");
        }
    }
}
