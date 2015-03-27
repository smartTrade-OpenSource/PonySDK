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

import java.util.Objects;

import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * All HTML element interfaces derive from this class.Useful to create native HTML component.
 */
public class PElement extends PComplexPanel {

    private final String tagName;

    private String innerText;
    private String innerHTML;

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

    public void setInnerHTML(final String innerHTML) {
        if (Objects.equals(this.innerHTML, innerHTML)) return;
        this.innerHTML = innerHTML;
        saveUpdate(PROPERTY.INNER_HTML, this.innerHTML);
    }

    public void setInnerText(final String innerText) {
        if (Objects.equals(this.innerText, innerText)) return;
        this.innerText = innerText;
        saveUpdate(PROPERTY.INNER_TEXT, this.innerText);
    }

    public String getInnerText() {
        return innerText;
    }

    public String getInnerHTML() {
        return innerHTML;
    }
}
