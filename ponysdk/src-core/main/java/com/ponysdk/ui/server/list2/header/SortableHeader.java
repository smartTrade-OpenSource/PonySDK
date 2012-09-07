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

package com.ponysdk.ui.server.list2.header;

import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventBusAware;
import com.ponysdk.core.query.Criterion;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.list.event.SortColumnEvent;
import com.ponysdk.ui.server.list.event.SortColumnHandler;

public class SortableHeader implements HeaderCellRenderer, SortColumnHandler, EventBusAware {

    private SortingType sortingType = SortingType.NONE;
    private EventBus eventBus;

    private final HeaderCellRenderer renderer;
    private final Criterion criterionField;
    private PWidget widget;

    public SortableHeader(final HeaderCellRenderer renderer, final Criterion criterionField) {
        this.renderer = renderer;
        this.criterionField = criterionField;
    }

    @Override
    public void setEventBus(final EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.addHandler(SortColumnEvent.TYPE, this);
    }

    @Override
    public void onColumnSort(final SortColumnEvent event) {
        if (!event.getSource().equals(this)) {
            if (sortingType == SortingType.NONE) return;

            widget.removeStyleName(getAssociatedStyleName(sortingType));
            widget.addStyleName(getAssociatedStyleName(SortingType.NONE));
            sortingType = SortingType.NONE;
        }
    }

    private void askSort(final String pojoPropertyKey) {
        final SortColumnEvent sortColumnEvent = new SortColumnEvent(SortableHeader.this, getNextSortingType(), pojoPropertyKey);
        eventBus.fireEvent(sortColumnEvent);
    }

    private SortingType getNextSortingType() {

        widget.removeStyleName(getAssociatedStyleName(sortingType));

        if (sortingType == SortingType.NONE) sortingType = SortingType.ASCENDING;
        else if (sortingType == SortingType.DESCENDING) sortingType = SortingType.ASCENDING;
        else if (sortingType == SortingType.ASCENDING) sortingType = SortingType.DESCENDING;
        else sortingType = SortingType.NONE;

        widget.addStyleName(getAssociatedStyleName(sortingType));

        return sortingType;
    }

    private String getAssociatedStyleName(final SortingType sortingType) {
        switch (sortingType) {
            case ASCENDING:
                return PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE_ASCENDING;
            case DESCENDING:
                return PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE_DESCENDING;
            case NONE:
                return PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE_NONE;
        }
        return PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE_NONE;
    }

    @Override
    public IsPWidget render() {
        final IsPWidget isWidget = renderer.render();
        widget = isWidget.asWidget();
        widget.addStyleName(PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE);
        widget.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                askSort(criterionField.getPojoProperty());
            }
        }, PClickEvent.TYPE);
        return isWidget;
    }

}
