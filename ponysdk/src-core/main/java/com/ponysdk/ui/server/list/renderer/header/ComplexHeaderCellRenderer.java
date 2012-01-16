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

package com.ponysdk.ui.server.list.renderer.header;

import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventBusAware;
import com.ponysdk.core.query.ComparatorType;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.list.event.ComparatorTypeChangeEvent;
import com.ponysdk.ui.server.list.event.RefreshListEvent;
import com.ponysdk.ui.server.list.event.SortColumnEvent;
import com.ponysdk.ui.server.list.event.SortColumnHandler;

public class ComplexHeaderCellRenderer implements HeaderCellRenderer, SortColumnHandler, EventBusAware {

    private static final String ARROW_DOWN_IMAGE_URL = "images/down_16.png";

    private static final String ARROW_UP_IMAGE_URL = "images/up_16.png";

    protected SortingType sortingType = SortingType.ASCENDING;

    protected final PVerticalPanel container;

    protected EventBus eventBus;

    protected FormField formField;

    public static final int KEY_ENTER = 13;

    private PAnchor header;

    private String caption;

    public ComplexHeaderCellRenderer(String caption, final String pojoPropertyKey) {
        this(caption, new FormField(), pojoPropertyKey);
    }

    public ComplexHeaderCellRenderer(String caption, final FormField formField, final String pojoPropertyKey) {
        this(caption, new FormField(), pojoPropertyKey, true);
    }

    public ComplexHeaderCellRenderer(String caption, final FormField formField, final String pojoPropertyKey, boolean enableComparatorType) {

        this.caption = caption;

        this.container = new PVerticalPanel();
        this.formField = formField;

        header = new PAnchor(caption);
        header.setWidth("100%");
        header.setHeight("20px");

        PHorizontalPanel subPanel = new PHorizontalPanel();
        subPanel.setSizeFull();

        header.setStyleProperty("cursor", "hand");
        header.setStyleProperty("cursor", "pointer");
        header.setStyleProperty("textAlign", "center");
        header.setStyleProperty("display", "block");

        header.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                askSort(pojoPropertyKey);
            }
        });

        subPanel.add(formField.render().asWidget());

        if (enableComparatorType) {
            final PListBox listBox = new PListBox();

            // for (String comparatorTypeName : ComparatorType.getNames()) {
            // listBox.addItem(comparatorTypeName);
            // }

            listBox.addItem(ComparatorType.EQ.getName());
            listBox.addItem(ComparatorType.GE.getName());
            listBox.addItem(ComparatorType.GT.getName());
            listBox.addItem(ComparatorType.LE.getName());
            listBox.addItem(ComparatorType.LT.getName());

            listBox.addChangeHandler(new PChangeHandler() {

                @Override
                public void onChange(Object source, int selectedIndex) {
                    if (listBox.getItem(selectedIndex).isEmpty()) return;
                    fireComparatorTypeChange(pojoPropertyKey, ComparatorType.fromName(listBox.getItem(selectedIndex)));
                }
            });

            subPanel.add(listBox);
        }

        formField.addDomHandler(new PKeyUpFilterHandler(KEY_ENTER) {

            @Override
            public void onKeyUp(int keyCode) {
                if (keyCode != KEY_ENTER) return;
                final RefreshListEvent refreshListEvent = new RefreshListEvent(ComplexHeaderCellRenderer.this, formField);
                eventBus.fireEvent(refreshListEvent);
            }
        }, PKeyUpEvent.TYPE);

        container.add(header);
        container.add(subPanel);

        container.addStyleName("pony-ComplexList-ComplexHeader");

    }

    protected SortingType getNextSortingType() {
        if (sortingType == SortingType.NONE) {
            header.setStyleProperty("background", "#4f4f4f url(" + ARROW_DOWN_IMAGE_URL + ") no-repeat 98% 50%");
            return SortingType.ASCENDING;
        } else if (sortingType == SortingType.DESCENDING) {
            header.setStyleProperty("background", "#4f4f4f url(" + ARROW_DOWN_IMAGE_URL + ") no-repeat 98% 50%");
            return SortingType.ASCENDING;
        } else if (sortingType == SortingType.ASCENDING) {
            header.setStyleProperty("background", "#4f4f4f url(" + ARROW_UP_IMAGE_URL + ") no-repeat 98% 50%");
            return SortingType.DESCENDING;
        }
        return SortingType.NONE;
    }

    @Override
    public IsPWidget render() {
        return container;
    }

    @Override
    public void onColumnSort(SortColumnEvent event) {
        if (!event.getSource().equals(this)) {
            sortingType = SortingType.NONE;
            header.setStyleProperty("background", "none");
        } else {
            sortingType = event.getSortingType();
        }
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.addHandler(SortColumnEvent.TYPE, this);
    }

    private void fireComparatorTypeChange(final String pojoPropertyKey, ComparatorType comparatorType) {
        final ComparatorTypeChangeEvent comparatorTypeEvent = new ComparatorTypeChangeEvent(ComplexHeaderCellRenderer.this, comparatorType, pojoPropertyKey);
        eventBus.fireEvent(comparatorTypeEvent);
    }

    private void askSort(final String pojoPropertyKey) {
        final SortColumnEvent sortColumnEvent = new SortColumnEvent(ComplexHeaderCellRenderer.this, getNextSortingType(), pojoPropertyKey);
        eventBus.fireEvent(sortColumnEvent);
    }

    @Override
    public String getCaption() {
        return caption;
    }
}
