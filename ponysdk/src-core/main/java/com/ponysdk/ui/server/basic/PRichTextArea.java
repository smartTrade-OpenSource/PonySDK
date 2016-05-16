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
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;
import com.ponysdk.ui.terminal.ui.PTRichTextArea.Justification;

/**
 * A rich text editor that allows complex styling and formatting.there is a
 * formatter interface, accessed via {@link #getFormatter()}. A browser that
 * does not support rich text editing at all will return <code>null</code> for
 * both of these, while one that supports only the basic functionality will
 * return <code>null</code> for the latter.
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
        saveUpdate(ServerToClientModel.TEXT, text);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        if (Objects.equals(this.html, html))
            return;
        this.html = html;
        saveUpdate(ServerToClientModel.HTML, this.html.replace("\"", "\\\""));
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE_HANDLER.toStringValue())) {
            fireOnValueChange(new PValueChangeEvent<>(this, instruction.getString(ClientToServerModel.HTML.toStringValue())));
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
            saveUpdate(ServerToClientModel.CREATE_LINK, url);
        }

        public void insertHorizontalRule() {
            saveUpdate(ServerToClientModel.INSERT_HORIZONTAL_RULE);
        }

        public void insertHTML(final String html) {
            saveUpdate(ServerToClientModel.INSERT_HTML, html);
        }

        public void insertImage(final String url) {
            saveUpdate(ServerToClientModel.IMAGE_URL, url);
        }

        public void insertOrderedList() {
            saveUpdate(ServerToClientModel.ORDERED);
        }

        public void insertUnorderedList() {
            saveUpdate(ServerToClientModel.UNORDERED);
        }

        public void setBackColor(final String color) {
            saveUpdate(ServerToClientModel.BACK_COLOR, color);
        }

        public void setFontName(final String name) {
            saveUpdate(ServerToClientModel.FONT_NAME, name);
        }

        public void setFontSize(final String fontSize) {
            saveUpdate(ServerToClientModel.FONT_SIZE, fontSize);
        }

        public void setForeColor(final String color) {
            saveUpdate(ServerToClientModel.FONT_COLOR, color);
        }

        public void setJustification(final Justification justification) {
            saveUpdate(ServerToClientModel.JUSTIFICATION, justification.name());
        }

        public void toggleBold() {
            saveUpdate(ServerToClientModel.TOGGLE_BOLD);
        }

        public void toggleItalic() {
            saveUpdate(ServerToClientModel.TOGGLE_ITALIC);
        }

        public void toggleSubscript() {
            saveUpdate(ServerToClientModel.TOGGLE_SUBSCRIPT);
        }

        public void toggleUnderline() {
            saveUpdate(ServerToClientModel.TOGGLE_UNDERLINE);
        }

        public void leftIndent() {
            saveUpdate(ServerToClientModel.LEFT_INDENT);
        }

        public void redo() {
            saveUpdate(ServerToClientModel.REDO);
        }

        public void removeFormat() {
            saveUpdate(ServerToClientModel.REMOVE_FORMAT);
        }

        public void removeLink() {
            saveUpdate(ServerToClientModel.REMOVE_LINK);
        }

        public void rightIndent() {
            saveUpdate(ServerToClientModel.TOGGLE_RIGHT_INDENT);
        }

        public void selectAll() {
            saveUpdate(ServerToClientModel.SELECT_ALL);
        }
    }

}
