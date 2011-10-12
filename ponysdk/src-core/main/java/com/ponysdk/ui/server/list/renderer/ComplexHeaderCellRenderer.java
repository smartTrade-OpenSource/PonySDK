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
package com.ponysdk.ui.server.list.renderer;

import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventBusAware;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.list.event.RefreshListEvent;
import com.ponysdk.ui.server.list.event.SortColumnEvent;
import com.ponysdk.ui.server.list.event.SortColumnHandler;

public class ComplexHeaderCellRenderer implements HeaderCellRenderer, SortColumnHandler, EventBusAware {

    private static final String ARROW_DOWN_IMAGE_URL = "images/arrow-down.png";
    private static final String ARROW_UP_IMAGE_URL = "images/arrow-up.png";

    protected SortingType sortingType = SortingType.ASCENDING;

    private final PVerticalPanel container;

    private final PAnchor link;
    private final PImage sortIcon;

    private EventBus eventBus;
    public static final int KEY_ENTER = 13;

    public ComplexHeaderCellRenderer(String caption, final String pojoPropertyKey) {
        this(caption, new FormField(), pojoPropertyKey);
    }

    public ComplexHeaderCellRenderer(String caption, final FormField formField, final String pojoPropertyKey) {
        container = new PVerticalPanel();
        container.setWidth("180px");
        container.setHeight("50px");

        link = new PAnchor(caption);
        link.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                askSort(pojoPropertyKey);
            }
        });

        sortIcon = new PImage(ARROW_DOWN_IMAGE_URL);
        sortIcon.setWidth("16px");
        sortIcon.setHeight("16px");

        sortIcon.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                askSort(pojoPropertyKey);

            }
        });

        final PHorizontalPanel gridLayout = new PHorizontalPanel();
        gridLayout.add(link);
        gridLayout.add(sortIcon);

        container.add(gridLayout);
        container.add(formField.render().asWidget());
        formField.addDomHandler(new PKeyUpFilterHandler(KEY_ENTER) {

            @Override
            public void onKeyUp(int keyCode) {
                if (keyCode != KEY_ENTER)
                    return;
                final RefreshListEvent refreshListEvent = new RefreshListEvent(ComplexHeaderCellRenderer.this, formField);
                eventBus.fireEvent(refreshListEvent);
            }
        }, PKeyUpEvent.TYPE);
    }

    protected SortingType getNextSortingType() {
        if (sortingType == SortingType.NONE) {
            sortIcon.setUrl(ARROW_DOWN_IMAGE_URL);
            return SortingType.ASCENDING;
        } else if (sortingType == SortingType.DESCENDING) {
            sortIcon.setUrl(ARROW_DOWN_IMAGE_URL);
            return SortingType.ASCENDING;
        } else if (sortingType == SortingType.ASCENDING) {
            sortIcon.setUrl(ARROW_UP_IMAGE_URL);
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
        } else {
            sortingType = event.getSortingType();
        }

        switch (sortingType) {
        case NONE:
            // sortIcon.setIcon(null);
            break;
        case ASCENDING:
            // sortIcon.setIcon(new ThemeResource("../runo/icons/16/arrow-up.png"));
            break;
        case DESCENDING:
            // sortIcon.setIcon(new ThemeResource("../runo/icons/16/arrow-down.png"));
            break;
        default:
            break;
        }
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.addHandler(SortColumnEvent.TYPE, this);
    }

    private void askSort(final String pojoPropertyKey) {
        final SortColumnEvent sortColumnEvent = new SortColumnEvent(ComplexHeaderCellRenderer.this, getNextSortingType(), pojoPropertyKey);
        eventBus.fireEvent(sortColumnEvent);
    }

    @Override
    public String getCaption() {
        if (link != null) {
            return link.getText();
        }

        return null;
    }
}
