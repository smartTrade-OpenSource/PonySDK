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
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextArea;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTTextArea extends PTTextBoxBase<TextArea> {

    @Override
    protected TextArea createUIObject() {
        return new TextArea() {

            @Override
            public int getTabIndex() {
                final int tabIndex = super.getTabIndex();
                return tabIndex == -1 ? -2 : tabIndex;
            }

            @Override
            public void onBrowserEvent(final Event event) {
                super.onBrowserEvent(event);
                if (handlePasteEnabled && Event.ONPASTE == event.getTypeInt() && enabled) {
                    Scheduler.get().scheduleDeferred(() -> sendPasteEvent(event));
                }
            }
        };
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.VISIBLE_LINES == model) {
            uiObject.setVisibleLines(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.CHARACTER_WIDTH == model) {
            uiObject.setCharacterWidth(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
