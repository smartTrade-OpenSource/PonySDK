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

import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.event.PEventBusAware;
import com.ponysdk.core.query.ComparatorType;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.FormField.ResetHandler;
import com.ponysdk.ui.server.list.event.ComparatorTypeChangeEvent;
import com.ponysdk.ui.server.list.event.RefreshListEvent;

public class ComplexHeaderCellRenderer implements HeaderCellRenderer, PEventBusAware {

    protected final PFlexTable container;

    protected PEventBus eventBus;

    protected FormField formField;

    private String caption;

    private SortableHeader sortableHeader;

    private PWidget header;

    public ComplexHeaderCellRenderer(final String caption, final String pojoPropertyKey) {
        this(caption, new FormField(), pojoPropertyKey);
    }

    public ComplexHeaderCellRenderer(final String caption, final FormField formField, final String pojoPropertyKey) {
        this(caption, formField, pojoPropertyKey, false);
    }

    public ComplexHeaderCellRenderer(final String caption, final FormField formField, final String pojoPropertyKey, final boolean enableComparatorType) {

        this.caption = caption;
        this.container = new PFlexTable();
        this.formField = formField;
        this.header = new PLabel(caption);
        this.sortableHeader = new SortableHeader(header, pojoPropertyKey);

        container.setSizeFull();
        container.setCellPadding(0);
        container.setCellSpacing(0);

        container.setWidget(0, 0, header);
        container.setWidget(1, 0, formField.render().asWidget());

        if (enableComparatorType) {
            final PListBox listBox = new PListBox(false, false);

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
                public void onChange(final PChangeEvent event) {
                    final String selectedItem = listBox.getSelectedItem();
                    if (selectedItem == null) return;
                    fireComparatorTypeChange(pojoPropertyKey, ComparatorType.fromName(selectedItem));
                }
            });

            formField.addResetHandler(new ResetHandler() {

                @Override
                public void onReset() {
                    listBox.setSelectedIndex(0);
                }
            });

            container.setWidget(1, 1, listBox);
            container.getFlexCellFormatter().setColSpan(0, 0, 2);
        }

        formField.addDomHandler(new PKeyUpFilterHandler(PKeyCodes.ENTER) {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                final RefreshListEvent refreshListEvent = new RefreshListEvent(ComplexHeaderCellRenderer.this, formField);
                eventBus.fireEvent(refreshListEvent);
            }
        }, PKeyUpEvent.TYPE);

        container.addStyleName(PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX);
    }

    @Override
    public IsPWidget render() {
        return container;
    }

    private void fireComparatorTypeChange(final String pojoPropertyKey, final ComparatorType comparatorType) {
        final ComparatorTypeChangeEvent comparatorTypeEvent = new ComparatorTypeChangeEvent(ComplexHeaderCellRenderer.this, comparatorType, pojoPropertyKey);
        eventBus.fireEvent(comparatorTypeEvent);
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public void setEventBus(final PEventBus eventBus) {
        this.eventBus = eventBus;
        this.sortableHeader.setEventBus(eventBus);
    }

}
