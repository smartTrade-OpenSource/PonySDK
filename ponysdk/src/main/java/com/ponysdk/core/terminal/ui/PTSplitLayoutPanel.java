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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.PTSplitLayoutPanel.MySplitLayoutPanel;

public class PTSplitLayoutPanel extends PTDockLayoutPanel<MySplitLayoutPanel> {

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        uiObject.uiBuilder = uiBuilder;
    }

    @Override
    protected MySplitLayoutPanel createUIObject() {
        return new MySplitLayoutPanel(objectID);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.MIN_SIZE == model) {
            final int minSize = binaryModel.getIntValue();
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setWidgetMinSize(w, minSize);
            return true;
        } else if (ServerToClientModel.SNAP_CLOSED_SIZE == model) {
            final int snapClosedSize = binaryModel.getIntValue();
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setWidgetSnapClosedSize(w, snapClosedSize);
            return true;
        } else if (ServerToClientModel.TOGGLE_DISPLAY_ALLOWED == model) {
            final boolean enable = binaryModel.getBooleanValue();
            final Widget w = asWidget(buffer.readBinaryModel().getIntValue(), uiBuilder);
            uiObject.setWidgetToggleDisplayAllowed(w, enable);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_RESIZE == handlerModel) uiObject.resizeHandler = true;
        else super.addHandler(buffer, handlerModel);
    }

    @Override
    public void removeHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_RESIZE == handlerModel) uiObject.resizeHandler = false;
        else super.removeHandler(buffer, handlerModel);
    }

    static final class MySplitLayoutPanel extends SplitLayoutPanel {

        private int objectId = -1;
        private UIBuilder uiBuilder;
        private boolean resizeHandler;

        private SendResizeCommand command;

        public MySplitLayoutPanel(final int objectId) {
            this.objectId = objectId;
        }

        @Override
        public void onResize() {
            super.onResize();

            if (resizeHandler) {
                if (command != null) command.cancelled = true;

                command = new SendResizeCommand(objectId, uiBuilder);

                Scheduler.get().scheduleFixedDelay(command, 500);
            }
        }

        public class SendResizeCommand implements RepeatingCommand {

            private int objectId = -1;
            private final UIBuilder uiBuilder;

            public SendResizeCommand(final int objectId, final UIBuilder uiBuilder) {
                this.objectId = objectId;
                this.uiBuilder = uiBuilder;
            }

            boolean cancelled = false;

            @Override
            public boolean execute() {
                if (cancelled) return false;

                int i = 0;
                final JSONArray jsonArray = new JSONArray();
                for (final Widget w : getChildren()) {
                    final PTObject ptObject = uiBuilder.getPTObject(w);
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
                    eventInstruction.put(ClientToServerModel.HANDLER_RESIZE, jsonArray);
                    uiBuilder.sendDataToServer(eventInstruction);
                }

                return false;
            }

        }
    }

}
