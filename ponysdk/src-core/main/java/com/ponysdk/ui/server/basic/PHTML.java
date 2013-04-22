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

import com.ponysdk.core.stm.TxnBoolean;
import com.ponysdk.core.stm.TxnObject;
import com.ponysdk.core.stm.TxnString;
import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A widget that can contain arbitrary HTML. This widget uses a &lt;div&gt; element, causing it to be
 * displayed with block layout.
 * <p>
 * If you only need a simple label (text, but not HTML), then the {@link PLabel} widget is more appropriate,
 * as it disallows the use of HTML, which can lead to potential security issues if not used properly.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-HTML { }</li>
 * </ul>
 */
public class PHTML extends PLabel implements PHasHTML {

    private final TxnString html = new TxnString();
    private final TxnBoolean wordWrap = new TxnBoolean(false);

    public PHTML() {
        super();
        this.html.setListener(this);
        this.wordWrap.setListener(this);
    }

    public PHTML(final String text) {
        this(text, false);
    }

    public PHTML(final String text, final boolean wordWrap) {
        this.html.setListener(this);
        this.wordWrap.setListener(this);
        setHTML(text);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.HTML;
    }

    @Override
    public String getHTML() {
        return html.get();
    }

    @Override
    public void setHTML(final String html) {
        this.html.set(html);
    }

    public boolean isWordWrap() {
        return wordWrap.get();
    }

    public void setWordWrap(final boolean wordWrap) {
        this.wordWrap.set(wordWrap);
    }

    @Override
    public void beforeFlush(final TxnObject<?> txnObject) {
        if (txnObject == html) {
            saveUpdate(PROPERTY.HTML, html.get());
        } else if (txnObject == wordWrap) {
            saveUpdate(PROPERTY.WORD_WRAP, wordWrap.get());
        } else {
            super.beforeFlush(txnObject);
        }
    }

}
