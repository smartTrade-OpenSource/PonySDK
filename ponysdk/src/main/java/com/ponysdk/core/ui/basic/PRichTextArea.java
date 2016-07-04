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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.terminal.ui.PTRichTextArea;
import com.ponysdk.core.ui.basic.event.PHasHTML;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

import javax.json.JsonObject;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern PATTERN = Pattern.compile("\"", Pattern.LITERAL);
    private static final String REPLACEMENT = Matcher.quoteReplacement("\\\"");
    private final List<PValueChangeHandler<String>> handlers = new ArrayList<>();
    private final Formatter formatter = new Formatter();
    private String html;

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
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.TEXT, text));
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.HTML, PATTERN.matcher(html).replaceAll(REPLACEMENT)));
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue())) {
            fireOnValueChange(new PValueChangeEvent<>(this,
                    instruction.getString(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue())));
        } else {
            super.onClientData(instruction);
        }
    }

    protected void fireOnValueChange(final PValueChangeEvent<String> event) {
        this.html = event.getValue();

        for (final PValueChangeHandler<String> handler : handlers) {
            handler.onValueChange(event);
        }

    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        handlers.add(handler);
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        return handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    public class Formatter {

        public void createLink(final String url) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.CREATE_LINK, url));
        }

        public void insertHorizontalRule() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.INSERT_HORIZONTAL_RULE));
        }

        public void insertHTML(final String html) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.INSERT_HTML, html));
        }

        public void insertImage(final String url) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.IMAGE_URL, url));
        }

        public void insertOrderedList() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.ORDERED));
        }

        public void insertUnorderedList() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.UNORDERED));
        }

        public void setBackColor(final String color) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.BACK_COLOR, color));
        }

        public void setFontName(final String name) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.FONT_NAME, name));
        }

        public void setFontSize(final String fontSize) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.FONT_SIZE, fontSize));
        }

        public void setForeColor(final String color) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.FONT_COLOR, color));
        }

        public void setJustification(final PTRichTextArea.Justification justification) {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.JUSTIFICATION, justification.name()));
        }

        public void toggleBold() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.TOGGLE_BOLD));
        }

        public void toggleItalic() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.TOGGLE_ITALIC));
        }

        public void toggleSubscript() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.TOGGLE_SUBSCRIPT));
        }

        public void toggleUnderline() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.TOGGLE_UNDERLINE));
        }

        public void leftIndent() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.LEFT_INDENT));
        }

        public void redo() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.REDO));
        }

        public void removeFormat() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.REMOVE_FORMAT));
        }

        public void removeLink() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.REMOVE_LINK));
        }

        public void rightIndent() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.TOGGLE_RIGHT_INDENT));
        }

        public void selectAll() {
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.SELECT_ALL));
        }
    }

}
