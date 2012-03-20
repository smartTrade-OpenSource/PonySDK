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

import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTextArea extends PTextBoxBase {

    private int visibleLines = 5;

    private int characterWidth = 25;

    public PTextArea() {
        this(null);
    }

    public PTextArea(String text) {
        super();
        setVisibleLines(visibleLines);
        setCharacterWidth(characterWidth);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TEXT_AREA;
    }

    public int getVisibleLines() {
        return visibleLines;
    }

    public void setVisibleLines(int visibleLines) {
        this.visibleLines = visibleLines;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.VISIBLE_LINES, visibleLines);
        getPonySession().stackInstruction(update);
    }

    public int getCharacterWidth() {
        return characterWidth;
    }

    public void setCharacterWidth(int characterWidth) {
        this.characterWidth = characterWidth;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.CHARACTER_WIDTH, characterWidth);
        getPonySession().stackInstruction(update);
    }

}
