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

import com.ponysdk.core.model.ServerToClientModel;

public abstract class PValueBoxBase extends PFocusWidget {

    protected PValueBoxBase() {
    }

    /**
     * Selects all of the text in the box. This will only work when the widget is attached to the
     * document and not hidden.
     */
    public void selectAll() {
        saveUpdate(writer -> writer.write(ServerToClientModel.SELECT_ALL));
    }

    public void setCursorPosition(final int cursorPosition) {
        saveUpdate(writer -> writer.write(ServerToClientModel.CURSOR_POSITION, cursorPosition));
    }

    public void setSelectionRange(final int startPosition, final int rangeLength) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.SELECTION_RANGE_START, startPosition);
            writer.write(ServerToClientModel.SELECTION_RANGE_LENGTH, rangeLength);
        });
    }

}
