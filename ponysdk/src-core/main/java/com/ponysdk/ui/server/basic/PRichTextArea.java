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

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.ui.PTRichTextArea.Justification;

/**
 * A rich text editor that allows complex styling and formatting.there is a formatter interface, accessed via
 * {@link #getFormatter()}.A browser that does not support rich text editing at all will return
 * <code>null</code> for both of these, while one that supports only the basic functionality will return
 * <code>null</code> for the latter. <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-RichTextArea</dt>
 * <dd>Applied to the rich text element.</dd>
 * </dl>
 */
public class PRichTextArea extends PFocusWidget implements PHasHTML, HasPValueChangeHandlers<String> {

    private String html;

    private final List<PValueChangeHandler<String>> handlers = new ArrayList<PValueChangeHandler<String>>();

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
        final Update update = new Update(getID());
        update.put(PROPERTY.TEXT, text);
        Txn.get().getTxnContext().save(update);
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
        Txn.get().getTxnContext().save(update);
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    @Override
    public void onClientData(final JSONObject e) throws JSONException {
        if (e.has(HANDLER.KEY) && e.getString(HANDLER.KEY).equals(HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER)) {
            final PValueChangeEvent<String> event = new PValueChangeEvent<String>(this, e.getString(PROPERTY.HTML));
            fireOnValueChange(event);
        } else {
            super.onClientData(e);
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
    public void removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    public class Formatter {

        public void createLink(final String url) {
            final Update update = new Update(getID());
            update.put(PROPERTY.CREATE_LINK, url);
            Txn.get().getTxnContext().save(update);
        }

        public void insertHorizontalRule() {
            final Update update = new Update(getID());
            update.put(PROPERTY.INSERT_HORIZONTAL_RULE);
            Txn.get().getTxnContext().save(update);
        }

        public void insertHTML(final String html) {
            final Update update = new Update(getID());
            update.put(PROPERTY.INSERT_HTML, html);
            Txn.get().getTxnContext().save(update);
        }

        public void insertImage(final String url) {
            final Update update = new Update(getID());
            update.put(PROPERTY.IMAGE, url);
            Txn.get().getTxnContext().save(update);
        }

        public void insertOrderedList() {
            final Update update = new Update(getID());
            update.put(PROPERTY.ORDERED);
            Txn.get().getTxnContext().save(update);
        }

        public void insertUnorderedList() {
            final Update update = new Update(getID());
            update.put(PROPERTY.UNORDERED);
            Txn.get().getTxnContext().save(update);
        }

        public void setBackColor(final String color) {
            final Update update = new Update(getID());
            update.put(PROPERTY.BACK_COLOR, color);
            Txn.get().getTxnContext().save(update);
        }

        public void setFontName(final String name) {
            final Update update = new Update(getID());
            update.put(PROPERTY.FONT_NAME, name);
            Txn.get().getTxnContext().save(update);
        }

        public void setFontSize(final String fontSize) {
            final Update update = new Update(getID());
            update.put(PROPERTY.FONT_SIZE, fontSize);
            Txn.get().getTxnContext().save(update);
        }

        public void setForeColor(final String color) {
            final Update update = new Update(getID());
            update.put(PROPERTY.FONT_COLOR, color);
            Txn.get().getTxnContext().save(update);
        }

        public void setJustification(final Justification justification) {
            final Update update = new Update(getID());
            update.put(PROPERTY.JUSTIFICATION, justification.name());
            Txn.get().getTxnContext().save(update);
        }

        public void toggleBold() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_BOLD);
            Txn.get().getTxnContext().save(update);
        }

        public void toggleItalic() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_ITALIC);
            Txn.get().getTxnContext().save(update);
        }

        public void toggleSubscript() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_SUBSCRIPT);
            Txn.get().getTxnContext().save(update);
        }

        public void toggleUnderline() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_UNDERLINE);
            Txn.get().getTxnContext().save(update);
        }

        public void leftIndent() {
            final Update update = new Update(getID());
            update.put(PROPERTY.LEFT_INDENT);
            Txn.get().getTxnContext().save(update);
        }

        public void redo() {
            final Update update = new Update(getID());
            update.put(PROPERTY.REDO);
            Txn.get().getTxnContext().save(update);
        }

        public void removeFormat() {
            final Update update = new Update(getID());
            update.put(PROPERTY.REMOVE_FORMAT);
            Txn.get().getTxnContext().save(update);
        }

        public void removeLink() {
            final Update update = new Update(getID());
            update.put(PROPERTY.REMOVE_LINK);
            Txn.get().getTxnContext().save(update);
        }

        public void rightIndent() {
            final Update update = new Update(getID());
            update.put(PROPERTY.TOGGLE_RIGHT_INDENT);
            Txn.get().getTxnContext().save(update);
        }

        public void selectAll() {
            final Update update = new Update(getID());
            update.put(PROPERTY.SELECT_ALL);
            Txn.get().getTxnContext().save(update);
        }
    }

}
