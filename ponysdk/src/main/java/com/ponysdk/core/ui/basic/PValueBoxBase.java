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
import java.util.List;
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.event.PHasText;
import com.ponysdk.core.ui.basic.event.PPasteEvent;
import com.ponysdk.core.ui.basic.event.PPasteEvent.PPasteHandler;
import com.ponysdk.core.writer.ModelWriter;

public abstract class PValueBoxBase extends PFocusWidget implements PHasText {

    protected static final String EMPTY = "";

    private List<PPasteHandler> pasteHandlers;

    protected String text = EMPTY;

    protected PValueBoxBase() {
        this(EMPTY);
    }

    protected PValueBoxBase(final String text) {
        super();
        this.text = text != null ? text : EMPTY;
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        if (!EMPTY.equals(text)) writer.write(ServerToClientModel.TEXT, this.text);
    }

    /**
     * Selects all of the text in the box. This will only work when the widget is attached to the
     * document and not hidden.
     */
    public void selectAll() {
        saveUpdate(writer -> writer.write(ServerToClientModel.SELECT_ALL));
    }

    public void select(final int startPosition, final int rangeLength) {
        final int textLength = getText().length();
        if (startPosition < textLength) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.SELECTION_RANGE_START, startPosition);
                writer.write(ServerToClientModel.SELECTION_RANGE_LENGTH, Math.min(rangeLength, textLength - startPosition));
            });
        }
    }

    /**
     * @deprecated Use {@link #select(int, int)} instead
     */
    @Deprecated(forRemoval = true, since = "v2.8.0")
    public void setSelectionRange(final int startPosition, final int rangeLength) {
        select(startPosition, rangeLength);
    }

    public void setCursorPosition(final int cursorPosition) {
        saveUpdate(ServerToClientModel.CURSOR_POSITION, Math.min(cursorPosition, this.text.length()));
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        if (text == null) text = EMPTY; // null not send over json
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        if (initialized) saveUpdate(ServerToClientModel.TEXT, this.text);
    }

    public void addPasteHandler(final PPasteHandler pasteHandler) {
        if (pasteHandlers == null) {
            pasteHandlers = new ArrayList<>();
            saveAddHandler(HandlerModel.HANDLER_PASTE);
        }
        pasteHandlers.add(pasteHandler);
    }

    public void removePasteHandler(final PPasteHandler pasteHandler) {
        if (pasteHandlers != null) {
            pasteHandlers.remove(pasteHandler);
            if (pasteHandlers.isEmpty()) saveRemoveHandler(HandlerModel.HANDLER_PASTE);
        }
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible() || !isEnabled()) return;
        if (instruction.containsKey(ClientToServerModel.HANDLER_PASTE.toStringValue())) {
            if (pasteHandlers != null) {
                final String data = instruction.getString(ClientToServerModel.HANDLER_PASTE.toStringValue());
                final PPasteEvent pasteEvent = new PPasteEvent(this, data);
                pasteHandlers.forEach(handler -> handler.onPaste(pasteEvent));
            }
        } else {
            super.onClientData(instruction);
        }
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    @Override
    public String toString() {
        return super.toString() + ", text=" + text;
    }

}
