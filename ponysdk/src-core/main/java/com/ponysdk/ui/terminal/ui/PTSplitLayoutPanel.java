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
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class PTSplitLayoutPanel extends PTDockLayoutPanel {

    private UIService uiService;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        super.create(buffer, objectId, uiService);

        this.uiService = uiService;
    }

    @Override
    protected MySplitLayoutPanel createUIObject() {
        return new MySplitLayoutPanel();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.MIN_SIZE.equals(binaryModel.getModel())) {
            final int minSize = binaryModel.getIntValue();
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            cast().setWidgetMinSize(w, minSize);
            return true;
        }
        if (ServerToClientModel.SNAP_CLOSED_SIZE.equals(binaryModel.getModel())) {
            final int snapClosedSize = binaryModel.getIntValue();
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            cast().setWidgetSnapClosedSize(w, snapClosedSize);
            return true;
        }
        if (ServerToClientModel.TOGGLE_DISPLAY_ALLOWED.equals(binaryModel.getModel())) {
            final boolean enable = binaryModel.getBooleanValue();
            final Widget w = asWidget(buffer.getBinaryModel().getIntValue(), uiService);
            cast().setWidgetToggleDisplayAllowed(w, enable);
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService) {
        if (HandlerModel.HANDLER_RESIZE_HANDLER.equals(handlerModel)) {
            cast().resizeHandler = true;
            cast().objectId = getObjectID();
            cast().uiService = uiService;
        } else {
            super.addHandler(buffer, handlerModel, uiService);
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
        protected int objectId = -1;

        @Override
        public void onResize() {
            super.onResize();

            if (resizeHandler) {
                if (command != null)
                    command.cancelled = true;

                command = new SendResizeCommand();

                Scheduler.get().scheduleFixedDelay(command, 500);
            }
        }

        public class SendResizeCommand implements RepeatingCommand {

            public boolean cancelled = false;

            @Override
            public boolean execute() {
                if (cancelled)
                    return false;

                int i = 0;
                final JSONArray jsonArray = new JSONArray();
                for (final Widget w : getChildren()) {
                    final PTObject ptObject = uiService.getPTObject(w);
                    if (ptObject != null) {
                        final Double wSize = getWidgetSize(w);
                        final PTInstruction ws = new PTInstruction(ptObject.getObjectID());
                        ws.put(ClientToServerModel.SIZE, wSize);
                        jsonArray.set(i, ws);
                        i++;
                    }
                }
                if (i > 0) {
                    final PTInstruction eventInstruction = new PTInstruction(objectId);
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(ClientToServerModel.HANDLER_RESIZE_HANDLER);
                    eventInstruction.put(ClientToServerModel.VALUE, jsonArray);
                    uiService.sendDataToServer(eventInstruction);
                }

                return false;
            }

        }
    }

}
