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

package com.ponysdk.ui.server.list.renderer.cell;

import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.event.PEventBusAware;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.list.event.RowDeletedEvent;
import com.ponysdk.ui.server.list.event.RowDeletedHandler;
import com.ponysdk.ui.server.list.event.RowInsertedEvent;
import com.ponysdk.ui.server.list.event.RowInsertedHandler;
import com.ponysdk.ui.server.list.event.ShowSubListEvent;

public class DetailsCellRenderer<D, V> implements CellRenderer<D, V>, PEventBusAware {

    private PEventBus eventBus;

    @Override
    public void setEventBus(PEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public IsPWidget render(int row, D data, V value) {
        PSimplePanel panel = new PSimplePanel();

        final PAnchor button = new PAnchor("");

        button.addStyleName(PonySDKTheme.COMPLEXLIST_DETAILS_PLUS);

        button.addClickHandler(new DetailsCellClickHandler(button, data, row));

        panel.setWidget(button);

        return panel;
    }

    private final class DetailsCellClickHandler implements PClickHandler, RowInsertedHandler, RowDeletedHandler {

        private final PAnchor details;

        private final D data;

        private int row;

        private boolean isOpen = false;

        private DetailsCellClickHandler(PAnchor details, D data, int row) {
            this.details = details;
            this.data = data;
            this.row = row;
            eventBus.addHandler(RowInsertedEvent.TYPE, this);
            eventBus.addHandler(RowDeletedEvent.TYPE, this);
        }

        @Override
        public void onClick(PClickEvent clickEvent) {
            if (!isOpen) {
                details.setStyleName(PonySDKTheme.COMPLEXLIST_DETAILS_MINUS);
                eventBus.fireEvent(new ShowSubListEvent<D>(this, data, true, row));
            } else {
                details.setStyleName(PonySDKTheme.COMPLEXLIST_DETAILS_PLUS);
                eventBus.fireEvent(new ShowSubListEvent<D>(this, data, false, row));
            }
            isOpen = !isOpen;

        }

        @Override
        public void onRowDeleted(RowDeletedEvent event) {
            if (row > event.getRow() + event.getDeletedRowCount()) {
                row -= event.getDeletedRowCount();
            }
        }

        @Override
        public void onRowInserted(RowInsertedEvent event) {
            if (row > event.getRow()) {
                row += event.getInsertedRowCount();
            }
        }

    }

}