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

package com.ponysdk.ui.server.basic;

import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Update;
import com.ponysdk.ui.terminal.ui.PTRichTextArea.Justification;

public class PRichTextArea extends PFocusWidget implements PHasHTML {

    private String text;

    private String html;

    private final Formatter formatter = new Formatter();

    public PRichTextArea() {
        super();
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.RICH_TEXT_AREA;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.TEXT, text);
        getPonySession().stackInstruction(update);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(String html) {
        this.html = html;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.HTML, html);
        getPonySession().stackInstruction(update);
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    public class Formatter {

        void createLink(String url) {
            final Update update = new Update(getID());
            update.setMainPropertyValue(PropertyKey.CREATE_LINK, html);
            getPonySession().stackInstruction(update);
        }

        void insertHorizontalRule() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.INSERT_HORIZONTAL_RULE);
            getPonySession().stackInstruction(update);
        }

        void insertHTML(String html) {
            final Update update = new Update(getID());
            update.setMainPropertyValue(PropertyKey.INSERT_HTML, html);
            getPonySession().stackInstruction(update);
        }

        void insertImage(String url) {
            final Update update = new Update(getID());
            update.setMainPropertyValue(PropertyKey.IMAGE, url);
            getPonySession().stackInstruction(update);
        }

        void insertOrderedList() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.ORDERED);
            getPonySession().stackInstruction(update);
        }

        void insertUnorderedList() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.UNORDERED);
            getPonySession().stackInstruction(update);
        }

        void setBackColor(String color) {
            final Update update = new Update(getID());
            update.setMainPropertyValue(PropertyKey.BACK_COLOR, color);
            getPonySession().stackInstruction(update);
        }

        void setFontName(String name) {
            final Update update = new Update(getID());
            update.setMainPropertyValue(PropertyKey.FONT_NAME, name);
            getPonySession().stackInstruction(update);
        }

        void setFontSize(String fontSize) {
            final Update update = new Update(getID());
            update.setMainPropertyValue(PropertyKey.FONT_SIZE, fontSize);
            getPonySession().stackInstruction(update);
        }

        void setForeColor(String color) {
            final Update update = new Update(getID());
            update.setMainPropertyValue(PropertyKey.FONT_COLOR, color);
            getPonySession().stackInstruction(update);
        }

        void setJustification(Justification justification) {
            final Update update = new Update(getID());
            update.setMainPropertyValue(PropertyKey.JUSTIFICATION, justification.name());
            getPonySession().stackInstruction(update);
        }

        void toggleBold() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.TOGGLE_BOLD);
            getPonySession().stackInstruction(update);
        }

        void toggleItalic() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.TOGGLE_ITALIC);
            getPonySession().stackInstruction(update);
        }

        void toggleSubscript() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.TOGGLE_SUBSCRIPT);
            getPonySession().stackInstruction(update);
        }

        void toggleUnderline() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.TOGGLE_UNDERLINE);
            getPonySession().stackInstruction(update);
        }

        void leftIndent() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.LEFT_INDENT);
            getPonySession().stackInstruction(update);
        }

        void redo() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.REDO);
            getPonySession().stackInstruction(update);
        }

        void removeFormat() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.REMOVE_FORMAT);
            getPonySession().stackInstruction(update);
        }

        void removeLink() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.REMOVE_LINK);
            getPonySession().stackInstruction(update);
        }

        void rightIndent() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.TOGGLE_RIGHT_INDENT);
            getPonySession().stackInstruction(update);
        }

        void selectAll() {
            final Update update = new Update(getID());
            update.setMainPropertyKey(PropertyKey.SELECT_ALL);
            getPonySession().stackInstruction(update);
        }
    }

}
