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

import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Update;

public class PHTML extends PLabel implements PHasHTML {

    private String html;

    private boolean wordWrap;

    public PHTML() {}

    public PHTML(String text) {
        this(text, false);
    }

    public PHTML(String text, boolean wordWrap) {
        setHTML(text);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.HTML;
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(String html) {
        this.html = html;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.HTML, html);
        getPonySession().stackInstruction(update);
    }

    public boolean isWordWrap() {
        return wordWrap;
    }

    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.WORD_WRAP, wordWrap);
        getPonySession().stackInstruction(update);
    }

}
