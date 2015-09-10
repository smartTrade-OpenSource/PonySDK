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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTSplitLayoutPanel extends PTDockLayoutPanel {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new MySplitLayoutPanel());
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.MIN_SIZE)) {
            final int minSize = update.getInt(Model.MIN_SIZE);
            final Widget w = asWidget(update.getLong(Model.WIDGET), uiService);
            cast().setWidgetMinSize(w, minSize);
        } else if (update.containsKey(Model.SNAP_CLOSED_SIZE)) {
            final int snapClosedSize = update.getInt(Model.SNAP_CLOSED_SIZE);
            final Widget w = asWidget(update.getLong(Model.WIDGET), uiService);
            cast().setWidgetSnapClosedSize(w, snapClosedSize);
        } else if (update.containsKey(Model.TOGGLE_DISPLAY_ALLOWED)) {
            final boolean enable = update.getBoolean(Model.TOGGLE_DISPLAY_ALLOWED);
            final Widget w = asWidget(update.getLong(Model.WIDGET), uiService);
            cast().setWidgetToggleDisplayAllowed(w, enable);
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void addHandler(final PTInstruction instruction, final UIService uiService) {
        if (instruction.containsKey(Model.HANDLER_RESIZE_HANDLER)) {
            cast().resizeHandler = true;
            cast().addInstruction = instruction;
            cast().uiService = uiService;
        } else {
            super.addHandler(instruction, uiService);
        }
    }

    @Override
    public MySplitLayoutPanel cast() {
        return (MySplitLayoutPanel) uiObject;
    }

    public class MySplitLayoutPanel extends SplitLayoutPanel {

        protected SendResizeCommand command;

        protected boolean resizeHandler = false;
        protected UIService uiService = null;
        protected PTInstruction addInstruction = null;

        @Override
        public void onResize() {
            super.onResize();

            if (resizeHandler) {
                if (command != null) command.cancelled = true;

                command = new SendResizeCommand();

                Scheduler.get().scheduleFixedDelay(command, 500);
            }
        }

        public class SendResizeCommand implements RepeatingCommand {

            public boolean cancelled = false;

            @Override
            public boolean execute() {
                if (cancelled) return false;

                int i = 0;
                final JSONArray jsonArray = new JSONArray();
                for (final Widget w : getChildren()) {
                    final PTObject ptObject = uiService.getPTObject(w);
                    if (ptObject != null) {
                        final Double wSize = getWidgetSize(w);
                        final PTInstruction ws = new PTInstruction();
                        ws.setObjectID(ptObject.getObjectID());
                        ws.put(Model.SIZE, wSize);
                        jsonArray.set(i, ws);
                        i++;
                    }
                }
                if (i > 0) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(addInstruction.getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.HANDLER_RESIZE_HANDLER);
                    eventInstruction.put(Model.VALUE.getKey(), jsonArray);
                    uiService.sendDataToServer(eventInstruction);
                }

                return false;
            }

        }
    }

}
