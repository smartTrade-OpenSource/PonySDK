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

package com.ponysdk.core.ui.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PFontSize;
import com.ponysdk.core.model.PJustification;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

/**
 * A rich text editor that allows complex styling and formatting.there is a formatter interface, accessed via
 * {@link #getFormatter()}. A browser that does not support rich text editing at all will return <code>null</code> for
 * both of these, while one that supports only the basic functionality will return <code>null</code> for the latter.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-RichTextArea</dt>
 * <dd>Applied to the rich text element.</dd>
 * </dl>
 */
public class PRichTextArea extends PFocusWidget implements HasPValueChangeHandlers<String> {

    private List<PValueChangeHandler<String>> handlers;
    private final Formatter formatter = new Formatter();
    private String text;
    private String html;

    protected PRichTextArea() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.RICH_TEXT_AREA;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        this.html = null;
        saveUpdate(ServerToClientModel.TEXT, text);
    }

    public String getHTML() {
        return html;
    }

    public void setHTML(final String html) {
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        this.text = null;
        saveUpdate(ServerToClientModel.HTML, html);
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible() || !isEnabled()) return;
        if (instruction.containsKey(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue())) {
            fireOnValueChange(
                new PValueChangeEvent<>(this, instruction.getString(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue())));
        } else {
            super.onClientData(instruction);
        }
    }

    protected void fireOnValueChange(final PValueChangeEvent<String> event) {
        this.html = event.getData();
        if (handlers != null) handlers.forEach(handler -> handler.onValueChange(event));
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        if (handlers == null) handlers = new ArrayList<>();
        handlers.add(handler);
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        return handlers != null && handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return handlers != null ? Collections.unmodifiableCollection(handlers) : Collections.emptyList();
    }

    @Override
    protected String dumpDOM() {
        return "<input>" + text != null ? text : html + "</input>";
    }

    public class Formatter {

        public void createLink(final String url) {
            saveUpdate(writer -> writer.write(ServerToClientModel.URL, url));
        }

        public void insertHorizontalRule() {
            saveUpdate(writer -> writer.write(ServerToClientModel.INSERT_HORIZONTAL_RULE));
        }

        public void insertHTML(final String html) {
            saveUpdate(writer -> writer.write(ServerToClientModel.INSERT_HTML, html));
        }

        public void insertImage(final String url) {
            saveUpdate(writer -> writer.write(ServerToClientModel.IMAGE_URL, url));
        }

        public void insertOrderedList() {
            saveUpdate(writer -> writer.write(ServerToClientModel.ORDERED));
        }

        public void insertUnorderedList() {
            saveUpdate(writer -> writer.write(ServerToClientModel.UNORDERED));
        }

        public void setBackColor(final String color) {
            saveUpdate(writer -> writer.write(ServerToClientModel.BACK_COLOR, color));
        }

        public void setFontName(final String name) {
            saveUpdate(writer -> writer.write(ServerToClientModel.FONT_NAME, name));
        }

        public void setFontSize(final PFontSize fontSize) {
            saveUpdate(writer -> writer.write(ServerToClientModel.FONT_SIZE, fontSize.getValue()));
        }

        public void setForeColor(final String color) {
            saveUpdate(writer -> writer.write(ServerToClientModel.FONT_COLOR, color));
        }

        public void setJustification(final PJustification justification) {
            saveUpdate(writer -> writer.write(ServerToClientModel.JUSTIFICATION, justification.getValue()));
        }

        public void toggleBold() {
            saveUpdate(writer -> writer.write(ServerToClientModel.TOGGLE_BOLD));
        }

        public void toggleItalic() {
            saveUpdate(writer -> writer.write(ServerToClientModel.TOGGLE_ITALIC));
        }

        public void toggleSubscript() {
            saveUpdate(writer -> writer.write(ServerToClientModel.TOGGLE_SUBSCRIPT));
        }

        public void toggleUnderline() {
            saveUpdate(writer -> writer.write(ServerToClientModel.TOGGLE_UNDERLINE));
        }

        public void leftIndent() {
            saveUpdate(writer -> writer.write(ServerToClientModel.LEFT_INDENT));
        }

        public void redo() {
            saveUpdate(writer -> writer.write(ServerToClientModel.REDO));
        }

        public void removeFormat() {
            saveUpdate(writer -> writer.write(ServerToClientModel.REMOVE_FORMAT));
        }

        public void removeLink() {
            saveUpdate(writer -> writer.write(ServerToClientModel.REMOVE_LINK));
        }

        public void rightIndent() {
            saveUpdate(writer -> writer.write(ServerToClientModel.TOGGLE_RIGHT_INDENT));
        }

        public void selectAll() {
            saveUpdate(writer -> writer.write(ServerToClientModel.SELECT_ALL));
        }

    }

}
