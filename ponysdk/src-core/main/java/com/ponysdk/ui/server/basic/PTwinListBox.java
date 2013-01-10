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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PListBox.ListItem;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class PTwinListBox<T> extends PFlexTable implements HasPChangeHandlers {

    private boolean enabled = true;

    private final String leftCaption;
    private final String rightCaption;

    private PListBox leftListBox;
    private PListBox rightListBox;

    private PButton switchButton;

    private final List<PChangeHandler> handlers = new ArrayList<PChangeHandler>();

    public PTwinListBox() {
        this(null, null, false);
    }

    public PTwinListBox(final String leftCaption, final String rightCaption) {
        this(leftCaption, rightCaption, false);
    }

    public PTwinListBox(final String leftCaption, final String rightCaption, final boolean containsEmptyItem) {
        addStyleName(PonySDKTheme.TWIN_LISTBOX);
        this.leftCaption = leftCaption;
        this.rightCaption = rightCaption;
        init(containsEmptyItem);
    }

    private void init(final boolean containsEmptyItem) {
        if (leftCaption != null) {
            setWidget(0, 0, new PLabel(leftCaption));
            getFlexCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_CENTER);
        }

        if (rightCaption != null) {
            setWidget(0, 2, new PLabel(rightCaption));
            getFlexCellFormatter().setHorizontalAlignment(0, 2, PHorizontalAlignment.ALIGN_CENTER);
        }

        leftListBox = new PListBox(containsEmptyItem, true);
        leftListBox.addStyleName(PonySDKTheme.TWIN_LISTBOX_LEFT_LISTBOX);

        rightListBox = new PListBox(containsEmptyItem, true);
        rightListBox.addStyleName(PonySDKTheme.TWIN_LISTBOX_RIGHT_LISTBOX);

        setWidget(1, 0, leftListBox);
        setWidget(1, 2, rightListBox);

        switchButton = new PButton("<>");
        switchButton.addStyleName(PonySDKTheme.TWIN_LISTBOX_SWITCH_BUTTON);
        switchButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final List<ListItem> leftRemovedItems = new ArrayList<ListItem>();
                for (int i = leftListBox.getItemCount(); i > 0; i--) {
                    if (leftListBox.isItemSelected(i - 1)) {
                        leftRemovedItems.add(leftListBox.removeItem(i - 1));
                    }
                }

                final List<ListItem> rightRemovedItems = new ArrayList<ListItem>();
                for (int i = rightListBox.getItemCount(); i > 0; i--) {
                    if (rightListBox.isItemSelected(i - 1)) {
                        rightRemovedItems.add(rightListBox.removeItem(i - 1));
                    }
                }

                for (int i = leftRemovedItems.size() - 1; i >= 0; i--) {
                    final ListItem listItem = leftRemovedItems.get(i);
                    rightListBox.addItem(listItem.label, listItem.value);
                }
                for (int i = rightRemovedItems.size() - 1; i >= 0; i--) {
                    final ListItem listItem = rightRemovedItems.get(i);
                    leftListBox.addItem(listItem.label, listItem.value);
                }

                fireChangeHandler();

            }
        });
        setWidget(1, 1, switchButton);
    }

    protected void fireChangeHandler() {
        final PChangeEvent event = new PChangeEvent(this);
        for (final PChangeHandler handler : handlers) {
            handler.onChange(event);
        }
    }

    @Override
    public void clear() {
        rightListBox.clear();
        leftListBox.clear();
    }

    public PListBox getLeftListBox() {
        return leftListBox;
    }

    public PListBox getRightListBox() {
        return rightListBox;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        rightListBox.setEnabled(enabled);
        leftListBox.setEnabled(enabled);
        switchButton.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        handlers.add(handler);
    }

    public boolean removeChangeHandler(final PChangeHandler handler) {
        return handlers.remove(handler);
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

}
