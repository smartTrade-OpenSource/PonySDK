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
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.servlet.WebsocketEncoder;
import com.ponysdk.core.ui.basic.event.PHasHTML;

import java.util.Objects;

/**
 * A widget that can contain arbitrary HTML. This widget uses a &lt;div&gt;
 * element, causing it to be displayed with block layout.
 * <p>
 * If you only need a simple label (text, but not HTML), then the {@link PLabel} widget is more
 * appropriate, as it disallows the use of HTML, which can lead to potential security issues if not
 * used properly.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-HTML { }</li>
 * </ul>
 */
public class PHTML extends PLabel implements PHasHTML {

    private String html;
    private boolean wordWrap = false;

    protected PHTML() {
        super();
    }

    protected PHTML(final String html) {
        this(html, false);
    }

    protected PHTML(final String html, final boolean wordWrap) {
        super();
        this.html = html;
        this.wordWrap = wordWrap;
    }

    @Override
    protected void enrichOnInit(final WebsocketEncoder parser) {
        super.enrichOnInit(parser);
        if (html != null) parser.encode(ServerToClientModel.HTML, html);
        if (wordWrap) parser.encode(ServerToClientModel.WORD_WRAP, wordWrap);
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
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        this.text = null;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.HTML, html));
    }

    @Override
    public void setText(final String text) {
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        this.html = null;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.TEXT, this.text));
    }

    public boolean isWordWrap() {
        return wordWrap;
    }

    public void setWordWrap(final boolean wordWrap) {
        if (Objects.equals(this.wordWrap, wordWrap)) return;
        this.wordWrap = wordWrap;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.WORD_WRAP, this.wordWrap));
    }

    @Override
    public String toString() {
        return super.toString() + ", html=" + html;
    }
}
