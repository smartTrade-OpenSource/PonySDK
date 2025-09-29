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

import java.util.Objects;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.writer.ModelWriter;

/**
 * A widget that can contain arbitrary HTML. This widget uses a &lt;div&gt;
 * element, causing it to be displayed with block layout.
 * <p>
 * If you only need a simple label (text, but not HTML), then the {@link PLabel} widget is more
 * appropriate, as it disallows the use of HTML, which can lead to potential security issues if not
 * used properly.
 * </p>
 * <h2>CSS Style Rules</h2>
 * <ul class='css'>
 * <li>.gwt-HTML { }</li>
 * </ul>
 */
public class PHTML extends PLabel {

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
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        if (html != null) writer.write(ServerToClientModel.HTML, html);
        if (wordWrap) writer.write(ServerToClientModel.WORD_WRAP, wordWrap);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.HTML;
    }

    public String getHTML() {
        return html;
    }

    public void setHTML(final String html) {
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        this.text = null;
        if (initialized) saveUpdate(ServerToClientModel.HTML, this.html);
    }

    @Override
    public void setText(final String text) {
        super.setText(text);
        this.html = null;
    }

    public boolean isWordWrap() {
        return wordWrap;
    }

    public void setWordWrap(final boolean wordWrap) {
        if (Objects.equals(this.wordWrap, wordWrap)) return;
        this.wordWrap = wordWrap;
        if (initialized) saveUpdate(ServerToClientModel.WORD_WRAP, this.wordWrap);
    }

    @Override
    public String toString() {
        return super.toString() + ", html=" + html;
    }
}
