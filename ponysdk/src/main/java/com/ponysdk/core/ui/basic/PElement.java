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

package com.ponysdk.core.ui.basic;

import java.util.Objects;
import java.util.stream.Collectors;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.writer.ModelWriter;

/**
 * All HTML element interfaces derive from this class.Useful to create native
 * HTML component.
 */
public class PElement extends PComplexPanel {

    private final String tagName;

    private String innerText;
    private String innerHTML;

    protected PElement(final String tagName) {
        super();
        this.tagName = tagName;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.TAG, tagName);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ELEMENT;
    }

    public String getTagName() {
        return tagName;
    }

    public String getInnerText() {
        return innerText;
    }

    public void setInnerText(final String innerText) {
        if (Objects.equals(this.innerText, innerText)) return;
        this.innerText = innerText;
        this.innerHTML = null;
        saveUpdate(ServerToClientModel.TEXT, this.innerText);
    }

    public String getInnerHTML() {
        return innerHTML;
    }

    public void setInnerHTML(final String innerHTML) {
        if (Objects.equals(this.innerHTML, innerHTML)) return;
        this.innerHTML = innerHTML;
        this.innerText = null;
        saveUpdate(ServerToClientModel.HTML, this.innerHTML);
    }

    @Override
    protected String dumpDOM() {
        if (getWidgetCount() == 0) {
            return "<" + tagName + " class=\"" + getStyleNames().collect(Collectors.joining(" ")) + "\">"
                    + (innerText != null ? innerText : innerHTML) + "</" + tagName + ">";
        } else {
            return "<" + tagName + " class=\"" + getStyleNames().collect(Collectors.joining(" ")) + "\">" + dumpChildDOM() + "</"
                    + tagName + ">";
        }
    }

    private String dumpChildDOM() {
        String DOM = "";
        for (PWidget w : children) {
            DOM += w.dumpDOM();
        }
        return DOM;
    }
}
