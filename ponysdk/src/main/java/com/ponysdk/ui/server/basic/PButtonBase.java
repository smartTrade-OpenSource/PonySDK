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

import com.ponysdk.core.Parser;
import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * Abstract base class for {@link PButton}, {@link PCheckBox}.
 */
public abstract class PButtonBase extends PFocusWidget implements PHasHTML {

    private static final String DEFAULT_TEXT_VALUE = null;
    private static final String DEFAULT_HTML_VALUE = null;

    private String text = DEFAULT_TEXT_VALUE;
    private String html = DEFAULT_HTML_VALUE;

    public PButtonBase() {
    }

    public PButtonBase(final String text) {
        this.text = text;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        if (this.text != DEFAULT_TEXT_VALUE) parser.parse(ServerToClientModel.TEXT, this.text);
        if (this.html != DEFAULT_HTML_VALUE) parser.parse(ServerToClientModel.HTML, this.html.replace("\"", "\\\""));
    }

    @Override
    public void setText(final String text) {
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        executeUpdate(ServerToClientModel.TEXT, this.text);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        executeUpdate(ServerToClientModel.HTML, this.html.replace("\"", "\\\""));
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return super.toString() + ", text=" + text + ", html=" + html;
    }
}
