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

import java.util.Objects;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * A widget that can contain arbitrary HTML. This widget uses a &lt;div&gt;
 * element, causing it to be displayed with block layout.
 * <p>
 * If you only need a simple label (text, but not HTML), then the {@link PLabel}
 * widget is more appropriate, as it disallows the use of HTML, which can lead
 * to potential security issues if not used properly.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-HTML { }</li>
 * </ul>
 */
public class PHTML extends PLabel implements PHasHTML {

    private String html;
    private boolean wordWrap = false;

    public PHTML() {
        super();
    }

    public PHTML(final String text) {
        this(text, false);
    }

    public PHTML(final String html, final boolean wordWrap) {
        super(false);
        this.html = html;
        this.wordWrap = wordWrap;
        init();
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        if (html != null) parser.parse(ServerToClientModel.HTML, this.html.replace("\"", "\\\""));
        if (wordWrap) parser.parse(ServerToClientModel.WORD_WRAP, this.wordWrap);
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
    public void setHTML(final String html) {
        if (Objects.equals(this.html, html))
            return;
        this.html = html;
        saveUpdate(ServerToClientModel.HTML, this.html.replace("\"", "\\\""));
    }

    public boolean isWordWrap() {
        return wordWrap;
    }

    public void setWordWrap(final boolean wordWrap) {
        if (Objects.equals(this.wordWrap, wordWrap))
            return;
        this.wordWrap = wordWrap;
        saveUpdate(ServerToClientModel.WORD_WRAP, this.wordWrap);
    }

    @Override
    public String toString() {
        return toString("text=" + text + ", html=" + html);
    }
}
