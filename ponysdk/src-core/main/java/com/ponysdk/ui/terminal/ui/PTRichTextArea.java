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

import com.google.gwt.user.client.ui.RichTextArea;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTRichTextArea extends PTFocusWidget<RichTextArea> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new RichTextArea());
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {

        if (update.containsKey(PROPERTY.HTML)) {
            uiObject.setHTML(update.getString(PROPERTY.HTML));
        } else if (update.containsKey(PROPERTY.CREATE_LINK)) {
            uiObject.getFormatter().createLink(update.getString(PROPERTY.CREATE_LINK));
        } else if (update.containsKey(PROPERTY.INSERT_HORIZONTAL_RULE)) {
            uiObject.getFormatter().insertHorizontalRule();
        } else if (update.containsKey(PROPERTY.INSERT_HTML)) {
            uiObject.getFormatter().insertHTML(update.getString(PROPERTY.INSERT_HTML));
        } else if (update.containsKey(PROPERTY.IMAGE)) {
            uiObject.getFormatter().insertImage(update.getString(PROPERTY.IMAGE));
        } else if (update.containsKey(PROPERTY.ORDERED)) {
            uiObject.getFormatter().insertOrderedList();
        } else if (update.containsKey(PROPERTY.UNORDERED)) {
            uiObject.getFormatter().insertUnorderedList();
        } else if (update.containsKey(PROPERTY.BACK_COLOR)) {
            uiObject.getFormatter().setBackColor(update.getString(PROPERTY.BACK_COLOR));
        } else if (update.containsKey(PROPERTY.FONT_COLOR)) {
            uiObject.getFormatter().setForeColor(update.getString(PROPERTY.FONT_COLOR));
        } else if (update.containsKey(PROPERTY.FONT_NAME)) {
            uiObject.getFormatter().setFontName(update.getString(PROPERTY.FONT_NAME));
        } else if (update.containsKey(PROPERTY.FONT_SIZE)) {
            final FontSize fontSize = FontSize.valueOf(update.getString(PROPERTY.FONT_SIZE));
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
