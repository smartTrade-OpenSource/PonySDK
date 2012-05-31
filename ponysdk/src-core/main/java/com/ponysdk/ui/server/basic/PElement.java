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

import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * All HTML element interfaces derive from this class.Useful to create native HTML component.
 */
public class PElement extends PComplexPanel {

    private final String tagName;
    private String innerTxt;

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
        this.innerTxt = innerHTML;

        final Update update = new Update(ID);
        update.put(PROPERTY.INNER_HTML, innerTxt);
        getPonySession().stackInstruction(update);
    }

    public void setInnerText(final String innerTxt) {
        this.innerTxt = innerTxt;

        final Update update = new Update(ID);
        if (innerTxt == null) {
            update.put(PROPERTY.CLEAR_INNER_TEXT, true);
        } else {
            update.put(PROPERTY.INNER_TEXT, innerTxt);
        }

        getPonySession().stackInstruction(update);
    }

    public String getInnerTxt() {
        return innerTxt;
    }

    public String getInnerHTML() {
        return innerTxt;
    }

}
