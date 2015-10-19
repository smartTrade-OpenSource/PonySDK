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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.json.JsonObject;

import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.ui.PTRichTextArea.Justification;

/**
 * A rich text editor that allows complex styling and formatting.there is a formatter interface, accessed via
 * {@link #getFormatter()}.A browser that does not support rich text editing at all will return
 * <code>null</code> for both of these, while one that supports only the basic functionality will return
 * <code>null</code> for the latter.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-RichTextArea</dt>
 * <dd>Applied to the rich text element.</dd>
 * </dl>
 */
public class PRichTextArea extends PFocusWidget implements PHasHTML, HasPValueChangeHandlers<String> {

    private String html;

    private List<PValueChangeHandler<String>> handlers;

    private final Formatter formatter = new Formatter();

    public PRichTextArea() {
        super();
        init();
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.RICH_TEXT_AREA;
    }

    @Override
    public String getText() {
        return html;
    }

    @Override
    public void setText(final String text) {
        this.html = text;
        saveUpdate(Model.TEXT, text);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        this.html = html;
        saveUpdate(Model.HTML, html);
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(Model.HANDLER_STRING_VALUE_CHANGE_HANDLER.getKey())) {
            fireOnValueChange(new PValueChangeEvent<>(this, instruction.getString(Model.HTML.getKey())));
        } else {
            super.onClientData(instruction);
        }
    }

    protected void fireOnValueChange(final PValueChangeEvent<String> event) {
        this.html = event.getValue();

        if (handlers != null) {
            for (final PValueChangeHandler<String> handler : handlers) {
                handler.onValueChange(event);
            }
        }

    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        if (handlers == null) {
            handlers = new ArrayList<>(1);
        } else {
            handlers.add(handler);
        }
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        if (handlers == null) {
            return false;
        } else {
            return handlers.remove(handler);
        }
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    public class Formatter {

        public void createLink(final String url) {
            saveUpdate(Model.CREATE_LINK, url);
        }

        public void insertHorizontalRule() {
            saveUpdate(Model.INSERT_HORIZONTAL_RULE);
        }

        public void insertHTML(final String html) {
            saveUpdate(Model.INSERT_HTML, html);
        }

        public void insertImage(final String url) {
            saveUpdate(Model.IMAGE, url);
        }

        public void insertOrderedList() {
            saveUpdate(Model.ORDERED);
        }

        public void insertUnorderedList() {
            saveUpdate(Model.UNORDERED);
        }

        public void setBackColor(final String color) {
            saveUpdate(Model.BACK_COLOR, color);
        }

        public void setFontName(final String name) {
            saveUpdate(Model.FONT_NAME, name);
        }

        public void setFontSize(final String fontSize) {
            saveUpdate(Model.FONT_SIZE, fontSize);
        }

        public void setForeColor(final String color) {
            saveUpdate(Model.FONT_COLOR, color);
        }

        public void setJustification(final Justification justification) {
            saveUpdate(Model.JUSTIFICATION, justification.name());
        }

        public void toggleBold() {
            saveUpdate(Model.TOGGLE_BOLD);
        }

        public void toggleItalic() {
            saveUpdate(Model.TOGGLE_ITALIC);
        }

        public void toggleSubscript() {
            saveUpdate(Model.TOGGLE_SUBSCRIPT);
        }

        public void toggleUnderline() {
            saveUpdate(Model.TOGGLE_UNDERLINE);
        }

        public void leftIndent() {
            saveUpdate(Model.LEFT_INDENT);
        }

        public void redo() {
            saveUpdate(Model.REDO);
        }

        public void removeFormat() {
            saveUpdate(Model.REMOVE_FORMAT);
        }

        public void removeLink() {
            saveUpdate(Model.REMOVE_LINK);
        }

        public void rightIndent() {
            saveUpdate(Model.TOGGLE_RIGHT_INDENT);
        }

        public void selectAll() {
            saveUpdate(Model.SELECT_ALL);
        }
    }

}
