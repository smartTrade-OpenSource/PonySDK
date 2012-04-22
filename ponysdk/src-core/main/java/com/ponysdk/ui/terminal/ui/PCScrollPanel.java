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

package com.ponysdk.ui.terminal.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.ponysdk.ui.terminal.addon.floatablepanel.PCFloatablePanel;

public class PCScrollPanel extends ScrollPanel implements ScrollHandler, RequiresResize, ResizeHandler {

    private final List<PCFloatablePanel> floatablePanels = new ArrayList<PCFloatablePanel>();

    public PCScrollPanel() {
        Window.addResizeHandler(this);
        addScrollHandler(this);
    }

    @Override
    public void onScroll(final ScrollEvent event) {
        if (!isAttached()) return;
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                fireOnResize();
            }
        });
    }

    private void fireOnResize() {
        for (final PCFloatablePanel floatablePanel : floatablePanels) {
            floatablePanel.onResize();
        }
    }

    public void addFloatablePanel(final PCFloatablePanel floatablePanel) {
        floatablePanels.add(floatablePanel);
    }

    @Override
    public void onResize() {
        super.onResize();
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                fireOnResize();
            }
        });
    }

    @Override
    public void onResize(final ResizeEvent event) {
        super.onResize();
        fireOnResize();
    }

}
