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
package com.ponysdk.ui.terminal.addon.notification;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class Notification extends DecoratedPopupPanel implements ResizeHandler {

    /**
     * The default style name.
     */
    private static final String DEFAULT_STYLENAME = "pony-Notification";

    public static final int DEFAULT_DELAY = 2500;
    public static final int WIDTH = 200;
    public static final int HEIGHT = 70;

    private final Label caption, description;

    private final Timer hideTimer = new Timer() {
        @Override
        public void run() {
            hide();
        }
    };

    private final boolean autoHide;

    public Notification() {
        this(null);
    }

    protected Notification(String caption) {
        this(caption, null);
    }

    public Notification(String caption, String content) {
        this(caption, content, false);
    }

    public Notification(String caption, String description, final boolean autoHide) {
        super(autoHide, false);
        Window.addResizeHandler(this);

        setAnimationEnabled(true);

        this.autoHide = autoHide;

        this.caption = new Label(caption);
        this.caption.setStyleName(DEFAULT_STYLENAME + "-caption");

        this.description = new Label(description);
        this.description.setStyleName(DEFAULT_STYLENAME + "-content");

        final FlowPanel panel = new FlowPanel();
        panel.setStyleName(DEFAULT_STYLENAME + "-panel");

        panel.setPixelSize(WIDTH, HEIGHT);

        DOM.setStyleAttribute(panel.getElement(), "overflow", "hidden");

        final SimplePanel div1 = new SimplePanel();
        div1.add(this.caption);

        final SimplePanel div2 = new SimplePanel();
        div2.add(this.description);

        panel.add(div1);
        panel.add(div2);

        setWidget(panel);

        addStyleName(DEFAULT_STYLENAME);
        DOM.setIntStyleAttribute(getElement(), "zIndex", Integer.MAX_VALUE);
    }

    public String getCaption() {
        return caption.getText();
    }

    private void updatePosition() {
        setPopupPosition(Window.getClientWidth() - getOffsetWidth(), Window.getClientHeight() - getOffsetHeight());
    }

    @Override
    public void show() {
        super.show();
        if (!autoHide) {
            hideTimer.schedule(DEFAULT_DELAY);
        }
        updatePosition();
    }

    @Override
    public void onResize(ResizeEvent event) {
        updatePosition();
    }
}
