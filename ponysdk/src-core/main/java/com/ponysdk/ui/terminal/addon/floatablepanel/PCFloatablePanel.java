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

package com.ponysdk.ui.terminal.addon.floatablepanel;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.ponysdk.ui.terminal.ui.PCScrollPanel;

public class PCFloatablePanel extends SimplePanel implements RequiresResize {

    private int startHorizontalPosition;

    private int startVerticalPosition;

    private int lastHorizontalScrollPosition = 0;

    private int lastVerticalScrollPosition = 0;

    private boolean topFloating;

    private boolean leftFloating;

    final SimplePanel panel = new SimpleLayoutPanel();

    private ScrollPanel scrollPanel;

    public PCFloatablePanel() {}

    private void correctPosition() {

        setWidth(scrollPanel.getElement().getClientWidth() + "px");

        final int scrollTop = scrollPanel.getAbsoluteTop();
        final int scrollLeft = scrollPanel.getAbsoluteLeft();
        final int verticalScrollPosition = scrollPanel.getVerticalScrollPosition();
        final int horizontalScrollPosition = scrollPanel.getHorizontalScrollPosition();

        int X = leftFloating ? scrollLeft : getAbsoluteLeft();
        int Y = topFloating ? scrollTop : getAbsoluteTop();

        if (setAndGetVerticalScrollChanged(scrollPanel)) {
            if (startVerticalPosition < verticalScrollPosition) {
                topFloating = true;
                Y = scrollTop;
            } else {
                topFloating = false;
                Y = scrollTop - verticalScrollPosition + startVerticalPosition;
            }
        }

        if (setAndGetHorizontalScrollChanged(scrollPanel)) {
            if (horizontalScrollPosition > startHorizontalPosition) {
                leftFloating = true;
                X = scrollLeft;
            } else {
                leftFloating = false;
                X = scrollLeft - horizontalScrollPosition + startHorizontalPosition;
            }
        }
        if (topFloating || leftFloating) {
            getElement().getStyle().setProperty("top", Y + "px");
            getElement().getStyle().setProperty("left", X + "px");
            if (!isFloating()) {
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                    @Override
                    public void execute() {
                        addStyleName("pony-PCPositionPanel-Floating");
                        getElement().getStyle().setProperty("position", "fixed");
                        panel.getElement().getStyle().setProperty("height", getOffsetHeight() + "px");
                        getElement().getParentElement().insertBefore(panel.getElement(), getElement());
                        setWidth(scrollPanel.getElement().getClientWidth() + "px");
                    }
                });
            }
        } else if (!topFloating && !leftFloating && isFloating()) {
            topFloating = false;
            leftFloating = false;
            getElement().getParentElement().removeChild(panel.getElement());
            getElement().getStyle().setProperty("position", "relative");
            getElement().getStyle().clearProperty("top");
            getElement().getStyle().clearProperty("left");
            removeStyleName("pony-PCPositionPanel-Floating");
        }
    }

    private boolean isFloating() {
        return "fixed".equals(getElement().getStyle().getProperty("position"));
    }

    private void initPositions() {
        startVerticalPosition = getAbsoluteTop() - scrollPanel.getAbsoluteTop();
        startHorizontalPosition = getAbsoluteLeft() - scrollPanel.getAbsoluteLeft();
        panel.getElement().getStyle().setProperty("height", getOffsetHeight() + "px");
        setWidth(scrollPanel.getElement().getClientWidth() + "px");
    }

    public void setScrollPanel(PCScrollPanel scrollPanel) {
        this.scrollPanel = scrollPanel;
        scrollPanel.addFloatablePanel(this);
        initPositions();
    }

    private boolean setAndGetHorizontalScrollChanged(ScrollPanel scrollPanel) {
        final boolean changed = scrollPanel.getHorizontalScrollPosition() != lastHorizontalScrollPosition;
        lastHorizontalScrollPosition = scrollPanel.getHorizontalScrollPosition();

        if (!changed) {
            if (scrollPanel.getAbsoluteLeft() != getAbsoluteLeft()) { return true; }
        }

        return changed;
    }

    private boolean setAndGetVerticalScrollChanged(ScrollPanel scrollPanel) {
        final boolean changed = scrollPanel.getVerticalScrollPosition() != lastVerticalScrollPosition;
        lastVerticalScrollPosition = scrollPanel.getVerticalScrollPosition();
        return changed;
    }

    @Override
    public void onResize() {
        correctPosition();
    }

}
