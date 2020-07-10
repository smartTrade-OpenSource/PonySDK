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
import com.ponysdk.core.writer.ModelWriter;

/**
 * Abstract base class for {@link PButton}, {@link PCheckBox}.
 *
 * @see com.google.gwt.user.client.ui.ButtonBase
 */
abstract class PButtonBase extends PFocusWidget {

    private String text;

    private String html;

    /**
     * Instantiates a new PButtonBase
     */
    PButtonBase() {
    }

    /**
     * Instantiates a new PButtonBase
     *
     * @param text the text
     */
    PButtonBase(final String text) {
        this.text = text;
    }

    /**
     * Instantiates a new PButtonBase
     *
     * @param text the text
     * @param html the html
     */
    PButtonBase(final String text, final String html) {
        this.text = text;
        this.html = html;
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        if (text != null) writer.write(ServerToClientModel.TEXT, text);
        if (html != null) writer.write(ServerToClientModel.HTML, html);
    }

    public String getHTML() {
        return html;
    }

    public void setHTML(final String html) {
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        this.text = null;
        if (initialized) saveUpdate(ServerToClientModel.HTML, html);
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        this.html = null;
        if (initialized) saveUpdate(ServerToClientModel.TEXT, this.text);
    }

    @Override
    public String toString() {
        return super.toString() + ", text=" + text + ", html=" + html;
    }

    @Override
    protected String dumpDOM() {
        return "<button class=\"" + getStyleNames().collect(Collectors.joining(" ")) + "\">" + text != null ? text
                : html + "</button>";
    }
}
