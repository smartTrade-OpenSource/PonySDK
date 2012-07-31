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

import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.ui.PTRichTextArea.Justification;

/**
 * A rich text editor that allows complex styling and formatting.there is a formatter interface, accessed via
 * {@link #getFormatter()}.A browser that does not support rich text editing at all will return
 * <code>null</code> for both of these, while one that supports only the basic functionality will return
 * <code>null</code> for the latter.
 * <p>
 * <img class='gallery' src='doc-files/PRichTextArea.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-RichTextArea</dt>
 * <dd>Applied to the rich text element.</dd>
 * </dl>
 */
public class PRichTextArea extends PFocusWidget implements PHasHTML {

    private String text;

    private String html;

    private final Formatter formatter = new Formatter();

    public PRichTextArea() {
        super();
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.RICH_TEXT_AREA;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(final String text) {
        this.text = text;
        final Update update = new Update(getID());
        update.put(PROPERTY.TEXT, text);
        getUIContext().stackInstruction(update);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        this.html = html;
        final Update update = new Update(getID());
        update.put(PROPERTY.HTML, html);
        getUIContext().stackInstruction(update);
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    public class Formatter {

        void createLink(final String url) {
            final Update update = new Update(getID());
            update.put(PROPERTY.CREATE_LINK, html);
            getUIContext().stackInstruction(update);
        }

        void insertHorizontalRule() {
            final Update update = new Update(getID());
            update.put(PROPERTY.INSERT_HORIZONTAL_RULE);
            getUIContext().stackInstruction(update);
        }

        void insertHTML(final String html) {
            final Update update = new Update(getID());
            update.put(PROPERTY.INSERT_HTML, html);
            getUIContext().stackInstruction(update);
        }

        void insertImage(final String url) {
            final Update update = new Update(getID());
            update.put(PROPERTY.IMAGE, url);
            getUIContext().stackInstruction(update);
        }

        void insertOrderedList() {
            final Update update = new Update(getID());
            update.put(PROPERTY.ORDERED);
            getUIContext().stackInstruction(update);
        }

        void insertUnorderedList() {
            final Update update = new Update(getID());
            update.put(PROPERTY.UNORDERED);
            getUIContext().stackInstruction(update);
        }

        void setBackColor(final String color) {
            final Update update = new Update(getID());
            update.put(PROPERTY.BACK_COLOR, color);
            getUIContext().stackInstruction(update);
        }

        void setFontName(final String name) {
            final Update update = new Update(getID());
            update.put(PROPERTY.FONT_NAME, name);
            getUIContext().stackInstruction(update);
        }

        void setFontSize(final String fontSize) {
            final Update update = new Update(getID());
            update.put(PROPERTY.FONT_SIZE, fontSize);
            getUIContext().stackInstruction(update);
        }

        void setForeColor(final String color) {
            final Update update = new Update(getID());
            update.put(PROPERTY.FONT_COLOR, color);
            getUIContext().stackInstruction(update);
        }

        void setJustification(final Justification justification) {
            final Update update = new Update(getID());
            update.put(PROPERTY.JUSTIFICATION, justification.name());
            getUIContext().stackInstruction(update);
        }

        void toggleBold() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_BOLD);
            getUIContext().stackInstruction(update);
        }

        void toggleItalic() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_ITALIC);
            getUIContext().stackInstruction(update);
        }

        void toggleSubscript() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_SUBSCRIPT);
            getUIContext().stackInstruction(update);
        }

        void toggleUnderline() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_UNDERLINE);
            getUIContext().stackInstruction(update);
        }

        void leftIndent() {
            final Update update = new Update(getID());
            update.put(PROPERTY.LEFT_INDENT);
            getUIContext().stackInstruction(update);
        }

        void redo() {
            final Update update = new Update(getID());
            update.put(PROPERTY.REDO);
            getUIContext().stackInstruction(update);
        }

        void removeFormat() {
            final Update update = new Update(getID());
            update.put(PROPERTY.REMOVE_FORMAT);
            getUIContext().stackInstruction(update);
        }

        void removeLink() {
            final Update update = new Update(getID());
            update.put(PROPERTY.REMOVE_LINK);
            getUIContext().stackInstruction(update);
        }

        void rightIndent() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_RIGHT_INDENT);
            getUIContext().stackInstruction(update);
        }

        void selectAll() {
            final Update update = new Update(getID());
            update.put(PROPERTY.SELECT_ALL);
            getUIContext().stackInstruction(update);
        }
    }

}
