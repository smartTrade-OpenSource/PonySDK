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
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTRichTextArea extends PTWidget {

    @Override
    public void create(Create create, UIService uiService) {
        init(new RichTextArea());
    }

    @Override
    public void update(Update update, UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();

        switch (propertyKey) {
            case HTML:
                cast().setHTML(property.getValue());
                break;
            case CREATE_LINK:
                cast().getFormatter().createLink(property.getValue());
                break;
            case INSERT_HORIZONTAL_RULE:
                cast().getFormatter().insertHorizontalRule();
                break;
            case INSERT_HTML:
                cast().getFormatter().insertHTML(property.getValue());
                break;
            case IMAGE:
                cast().getFormatter().insertImage(property.getValue());
                break;
            case ORDERED:
                cast().getFormatter().insertOrderedList();
                break;
            case UNORDERED:
                cast().getFormatter().insertUnorderedList();
                break;
            case BACK_COLOR:
                cast().getFormatter().setBackColor(property.getValue());
                break;
            case FONT_NAME:
                cast().getFormatter().setFontName(property.getValue());
                break;
            case FONT_SIZE:
                FontSize fontSize = FontSize.valueOf(property.getValue());
                switch (fontSize) {
                    case LARGE:
                        cast().getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.LARGE);
                        break;
                    case SMALL:
                        cast().getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.SMALL);
                        break;
                    case MEDIUM:
                        cast().getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.MEDIUM);
                        break;
                    case X_LARGE:
                        cast().getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.X_LARGE);
                        break;
                    case X_SMALL:
                        cast().getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.X_SMALL);
                        break;
                    case XX_LARGE:
                        cast().getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.XX_LARGE);
                        break;
                    case XX_SMALL:
                        cast().getFormatter().setFontSize(com.google.gwt.user.client.ui.RichTextArea.FontSize.XX_SMALL);
                        break;

                    default:
                        break;
                }
                break;
            case FONT_COLOR:
                cast().getFormatter().createLink(property.getValue());
                break;
            case JUSTIFICATION:
                cast().getFormatter().createLink(property.getValue());
                break;
            case TOGGLE_BOLD:
                cast().getFormatter().createLink(property.getValue());
                break;
            case TOGGLE_ITALIC:
                cast().getFormatter().createLink(property.getValue());
                break;
            case TOGGLE_SUBSCRIPT:
                cast().getFormatter().createLink(property.getValue());
                break;
            case TOGGLE_UNDERLINE:
                cast().getFormatter().createLink(property.getValue());
                break;
            case LEFT_INDENT:
                cast().getFormatter().createLink(property.getValue());
                break;
            case REDO:
                cast().getFormatter().createLink(property.getValue());
                break;
            case REMOVE_FORMAT:
                cast().getFormatter().createLink(property.getValue());
                break;
            case REMOVE_LINK:
                cast().getFormatter().createLink(property.getValue());
                break;
            case TOGGLE_RIGHT_INDENT:
                cast().getFormatter().createLink(property.getValue());
                break;
            case SELECT_ALL:
                cast().getFormatter().createLink(property.getValue());
                break;
            default:
                break;
        }

        super.update(update, uiService);
    }

    @Override
    public RichTextArea cast() {
        return (RichTextArea) uiObject;
    }

    public enum FontSize {
        LARGE, MEDIUM, SMALL, X_LARGE, X_SMALL, XX_LARGE, XX_SMALL;
    }

    public enum Justification {

        CENTER, FULL, LEFT, RIGHT;

    }
}
