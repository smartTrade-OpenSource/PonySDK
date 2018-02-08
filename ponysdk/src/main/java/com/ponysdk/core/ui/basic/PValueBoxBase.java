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

import java.util.Objects;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.event.PHasText;
import com.ponysdk.core.writer.ModelWriter;

public abstract class PValueBoxBase extends PFocusWidget implements PHasText {

    protected static final String EMPTY = "";

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

    public void setCursorPosition(final int cursorPosition) {
        saveUpdate(ServerToClientModel.CURSOR_POSITION, Math.min(cursorPosition, this.text.length()));
    }

    public void setSelectionRange(final int startPosition, final int rangeLength) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.SELECTION_RANGE_START, startPosition);
            writer.write(ServerToClientModel.SELECTION_RANGE_LENGTH, rangeLength);
        });
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

    @Override
    public String toString() {
        return super.toString() + ", text=" + text;
    }

}
