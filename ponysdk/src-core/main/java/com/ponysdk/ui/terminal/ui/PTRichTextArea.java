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
import com.ponysdk.ui.terminal.model.Model;

public class PTRichTextArea extends PTFocusWidget<RichTextArea>implements BlurHandler {

    private UIService uiService;

    @Override
    public void onBlur(final BlurEvent event) {
        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(getObjectID());
        instruction.put(Model.HANDLER_STRING_VALUE_CHANGE_HANDLER);
        instruction.put(Model.HTML, uiObject.getHTML());
        uiService.sendDataToServer(uiObject, instruction);
    }

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final RichTextArea richTextArea = new RichTextArea();
        init(create, uiService, richTextArea);
        this.uiService = uiService;
        richTextArea.addBlurHandler(this);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.HTML)) {
            uiObject.setHTML(update.getString(Model.HTML));
        } else if (update.containsKey(Model.CREATE_LINK)) {
            uiObject.getFormatter().createLink(update.getString(Model.CREATE_LINK));
        } else if (update.containsKey(Model.INSERT_HORIZONTAL_RULE)) {
            uiObject.getFormatter().insertHorizontalRule();
        } else if (update.containsKey(Model.INSERT_HTML)) {
            uiObject.getFormatter().insertHTML(update.getString(Model.INSERT_HTML));
        } else if (update.containsKey(Model.IMAGE)) {
            uiObject.getFormatter().insertImage(update.getString(Model.IMAGE));
        } else if (update.containsKey(Model.ORDERED)) {
            uiObject.getFormatter().insertOrderedList();
        } else if (update.containsKey(Model.UNORDERED)) {
            uiObject.getFormatter().insertUnorderedList();
        } else if (update.containsKey(Model.BACK_COLOR)) {
            uiObject.getFormatter().setBackColor(update.getString(Model.BACK_COLOR));
        } else if (update.containsKey(Model.FONT_COLOR)) {
            uiObject.getFormatter().setForeColor(update.getString(Model.FONT_COLOR));
        } else if (update.containsKey(Model.FONT_NAME)) {
            uiObject.getFormatter().setFontName(update.getString(Model.FONT_NAME));
        } else if (update.containsKey(Model.FONT_SIZE)) {
            final FontSize fontSize = FontSize.valueOf(update.getString(Model.FONT_SIZE));
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

        } else {
            super.update(update, uiService);
        }

    }

    public enum FontSize {
        LARGE, MEDIUM, SMALL, X_LARGE, X_SMALL, XX_LARGE, XX_SMALL;
    }

    public enum Justification {
        CENTER, FULL, LEFT, RIGHT;
    }

}
