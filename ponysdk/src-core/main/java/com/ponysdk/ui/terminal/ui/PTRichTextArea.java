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

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.ui.RichTextArea;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class PTRichTextArea extends PTFocusWidget<RichTextArea> implements BlurHandler {

    private UIService uiService;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        super.create(buffer, objectId, uiService);

        this.uiService = uiService;
        uiObject.addBlurHandler(this);
    }

    @Override
    protected RichTextArea createUIObject() {
        return new RichTextArea();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.HTML.equals(binaryModel.getModel())) {
            uiObject.setHTML(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.CREATE_LINK.equals(binaryModel.getModel())) {
            uiObject.getFormatter().createLink(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.INSERT_HORIZONTAL_RULE.equals(binaryModel.getModel())) {
            uiObject.getFormatter().insertHorizontalRule();
            return true;
        }
        if (ServerToClientModel.INSERT_HTML.equals(binaryModel.getModel())) {
            uiObject.getFormatter().insertHTML(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.IMAGE_URL.equals(binaryModel.getModel())) {
            uiObject.getFormatter().insertImage(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.ORDERED.equals(binaryModel.getModel())) {
            uiObject.getFormatter().insertOrderedList();
            return true;
        }
        if (ServerToClientModel.UNORDERED.equals(binaryModel.getModel())) {
            uiObject.getFormatter().insertUnorderedList();
            return true;
        }
        if (ServerToClientModel.BACK_COLOR.equals(binaryModel.getModel())) {
            uiObject.getFormatter().setBackColor(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.FONT_COLOR.equals(binaryModel.getModel())) {
            uiObject.getFormatter().setForeColor(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.FONT_NAME.equals(binaryModel.getModel())) {
            uiObject.getFormatter().setFontName(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.FONT_SIZE.equals(binaryModel.getModel())) {
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
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public void onBlur(final BlurEvent event) {
        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(getObjectID());
        instruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE_HANDLER);
        instruction.put(ClientToServerModel.HTML, uiObject.getHTML());
        uiService.sendDataToServer(uiObject, instruction);
    }

    public enum FontSize {
        LARGE,
        MEDIUM,
        SMALL,
        X_LARGE,
        X_SMALL,
        XX_LARGE,
        XX_SMALL;
    }

    public enum Justification {
        CENTER,
        FULL,
        LEFT,
        RIGHT;
    }

}
