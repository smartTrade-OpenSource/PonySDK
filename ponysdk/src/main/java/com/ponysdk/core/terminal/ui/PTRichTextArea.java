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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.ui.RichTextArea;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.converter.GWTConverter;

public class PTRichTextArea extends PTFocusWidget<RichTextArea> implements BlurHandler {

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        uiObject.addBlurHandler(this);
    }

    @Override
    protected RichTextArea createUIObject() {
        return new RichTextArea() {

            @Override
            public int getTabIndex() {
                final int tabIndex = super.getTabIndex();
                return tabIndex == -1 ? -2 : tabIndex;
            }
        };
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.HTML == model) {
            uiObject.setHTML(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.URL == model) {
            uiObject.getFormatter().createLink(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.INSERT_HORIZONTAL_RULE == model) {
            uiObject.getFormatter().insertHorizontalRule();
            return true;
        } else if (ServerToClientModel.INSERT_HTML == model) {
            uiObject.getFormatter().insertHTML(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.IMAGE_URL == model) {
            uiObject.getFormatter().insertImage(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.ORDERED == model) {
            uiObject.getFormatter().insertOrderedList();
            return true;
        } else if (ServerToClientModel.UNORDERED == model) {
            uiObject.getFormatter().insertUnorderedList();
            return true;
        } else if (ServerToClientModel.BACK_COLOR == model) {
            uiObject.getFormatter().setBackColor(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.FONT_COLOR == model) {
            uiObject.getFormatter().setForeColor(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.FONT_NAME == model) {
            uiObject.getFormatter().setFontName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.FONT_SIZE == model) {
            uiObject.getFormatter().setFontSize(GWTConverter.asFontSize(binaryModel.getIntValue()));
            return true;
        } else if (ServerToClientModel.JUSTIFICATION == model) {
            uiObject.getFormatter().setJustification(GWTConverter.asJustification(binaryModel.getIntValue()));
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void onBlur(final BlurEvent event) {
        final PTInstruction instruction = new PTInstruction(getObjectID());
        instruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, uiObject.getHTML());
        uiBuilder.sendDataToServer(uiObject, instruction);
    }

}
