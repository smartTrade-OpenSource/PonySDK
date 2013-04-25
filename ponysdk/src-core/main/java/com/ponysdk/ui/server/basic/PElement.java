/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import com.ponysdk.core.stm.TxnString;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * All HTML element interfaces derive from this class.Useful to create native HTML component.
 */
public class PElement extends PComplexPanel {

    private final String tagName;

    private TxnString innerText;
    private TxnString innerHTML;

    public PElement(final String tagName) {
        super();
        this.tagName = tagName;
        this.create.put(PROPERTY.TAG, tagName);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ELEMENT;
    }

    public String getTagName() {
        return tagName;
    }

    public void setInnerHTML(final String html) {
        if (innerHTML == null) innerHTML = new TxnString();
        if (innerHTML.set(html)) saveUpdate(PROPERTY.INNER_HTML, html);
    }

    public void setInnerText(final String text) {
        if (innerText == null) innerText = new TxnString();
        if (innerText.set(text)) saveUpdate(PROPERTY.INNER_TEXT, text);
    }

    public String getInnerText() {
        if (innerText == null) return null;
        return innerText.get();
    }

    public String getInnerHTML() {
        if (innerHTML == null) return null;
        return innerHTML.get();
    }
}
