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

public class PTRichTextArea extends PTFocusWidget<RichTextArea> implements BlurHandler {

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        uiObject.addBlurHandler(this);
    }

    @Override
    protected RichTextArea createUIObject() {
        return new RichTextArea();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.HTML.ordinal() == modelOrdinal) {
            uiObject.setHTML(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.CREATE_LINK.ordinal() == modelOrdinal) {
            uiObject.getFormatter().createLink(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.INSERT_HORIZONTAL_RULE.ordinal() == modelOrdinal) {
            uiObject.getFormatter().insertHorizontalRule();
            return true;
        } else if (ServerToClientModel.INSERT_HTML.ordinal() == modelOrdinal) {
            uiObject.getFormatter().insertHTML(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.IMAGE_URL.ordinal() == modelOrdinal) {
            uiObject.getFormatter().insertImage(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.ORDERED.ordinal() == modelOrdinal) {
            uiObject.getFormatter().insertOrderedList();
            return true;
        } else if (ServerToClientModel.UNORDERED.ordinal() == modelOrdinal) {
            uiObject.getFormatter().insertUnorderedList();
            return true;
        } else if (ServerToClientModel.BACK_COLOR.ordinal() == modelOrdinal) {
            uiObject.getFormatter().setBackColor(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.FONT_COLOR.ordinal() == modelOrdinal) {
            uiObject.getFormatter().setForeColor(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.FONT_NAME.ordinal() == modelOrdinal) {
            uiObject.getFormatter().setFontName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.FONT_SIZE.ordinal() == modelOrdinal) {
            final FontSize fontSize = FontSize.valueOf(binaryModel.getStringValue());
            switch (fontSize) {
                case LARGE:
                    uiObject.getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.LARGE);
                    break;
                case SMALL:
                    uiObject.getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.SMALL);
                    break;
                case MEDIUM:
                    uiObject.getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.MEDIUM);
                    break;
                case X_LARGE:
                    uiObject.getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.X_LARGE);
                    break;
                case X_SMALL:
                    uiObject.getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.X_SMALL);
                    break;
                case XX_LARGE:
                    uiObject.getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.XX_LARGE);
                    break;
                case XX_SMALL:
                    uiObject.getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.XX_SMALL);
                    break;
                default:
                    break;
            }
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

    public enum FontSize {
        LARGE,
        MEDIUM,
        SMALL,
        X_LARGE,
        X_SMALL,
        XX_LARGE,
        XX_SMALL
    }

    public enum Justification {
        CENTER,
        FULL,
        LEFT,
        RIGHT
    }

}
