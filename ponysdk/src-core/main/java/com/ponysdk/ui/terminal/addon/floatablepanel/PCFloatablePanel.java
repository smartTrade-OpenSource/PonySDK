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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;

public class PCFloatablePanel extends SimplePanel implements ScrollHandler, ResizeHandler {

    private ScrollPanel linkedScrollPanel;
    private int startHorizontalPosition;
    private int startVerticalPosition;
    private int lastHorizontalScrollPosition = 0;
    private int lastVerticalScrollPosition = 0;
    private boolean topFloating;
    private boolean leftFloating;

    final SimplePanel panel = new SimplePanel();

    public PCFloatablePanel() {
    }

    public PCFloatablePanel(ScrollPanel linkedScrollPanel) {
        setLinkedScrollPanel(linkedScrollPanel);
    }

    @Override
    public void onScroll(ScrollEvent event) {
        if (!isAttached())
            return;
        correctPosition();
    }

    public void update() {
        if (linkedScrollPanel != null)
            correctPosition();
    }

    private void correctPosition() {
        if (linkedScrollPanel == null) {
            return;
        }
        setWidth(linkedScrollPanel.getElement().getClientWidth() + "px");

        final int scrollTop = linkedScrollPanel.getAbsoluteTop();
        final int scrollLeft = linkedScrollPanel.getAbsoluteLeft();
        final int verticalScrollPosition = linkedScrollPanel.getVerticalScrollPosition();
        final int horizontalScrollPosition = linkedScrollPanel.getHorizontalScrollPosition();

        int X = leftFloating ? scrollLeft : getAbsoluteLeft();
        int Y = topFloating ? scrollTop : getAbsoluteTop();

        GWT.log("init-scrollWidth" + linkedScrollPanel.getOffsetWidth());
        GWT.log("init-getClientWidth" + linkedScrollPanel.getElement().getClientWidth());

        if (setAndGetVerticalScrollChanged()) {
            if (startVerticalPosition < verticalScrollPosition) {
                topFloating = true;
                Y = scrollTop;
            } else {
                topFloating = false;
                Y = scrollTop - verticalScrollPosition + startVerticalPosition;
            }
        }

        if (setAndGetHorizontalScrollChanged()) {
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
                addStyleName("pony-PCPositionPanel-Floating");
                getElement().getStyle().setProperty("position", "fixed");
                panel.getElement().getStyle().setProperty("height", getOffsetHeight() + "px");
                getElement().getParentElement().insertBefore(panel.getElement(), getElement());
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

    @Override
    protected void onAttach() {
        super.onAttach();
        if (linkedScrollPanel != null) {
            linkedScrollPanel.setHorizontalScrollPosition(lastHorizontalScrollPosition);
            linkedScrollPanel.setVerticalScrollPosition(lastVerticalScrollPosition);
        }
        initPositions();
        correctPosition();
    }

    private void initPositions() {
        if (linkedScrollPanel != null) {
            startVerticalPosition = getAbsoluteTop() - linkedScrollPanel.getAbsoluteTop();
            startHorizontalPosition = getAbsoluteLeft() - linkedScrollPanel.getAbsoluteLeft();
            panel.getElement().getStyle().setProperty("height", getOffsetHeight() + "px");
            setWidth(linkedScrollPanel.getElement().getClientWidth() + "px");
        }
    }

    @Override
    public void onResize(ResizeEvent event) {
        initPositions();
        correctPosition();
    }

    public void setLinkedScrollPanel(ScrollPanel linkedScrollPanel) {
        this.linkedScrollPanel = linkedScrollPanel;
        initPositions();
        this.linkedScrollPanel.addScrollHandler(this);
        Window.addResizeHandler(this);
        linkedScrollPanel.addHandler(this, ResizeEvent.getType());
    }

    private boolean setAndGetHorizontalScrollChanged() {
        final boolean changed = linkedScrollPanel.getHorizontalScrollPosition() != lastHorizontalScrollPosition;
        lastHorizontalScrollPosition = linkedScrollPanel.getHorizontalScrollPosition();
        return changed;
    }

    private boolean setAndGetVerticalScrollChanged() {
        final boolean changed = linkedScrollPanel.getVerticalScrollPosition() != lastVerticalScrollPosition;
        lastVerticalScrollPosition = linkedScrollPanel.getVerticalScrollPosition();
        return changed;
    }

}
