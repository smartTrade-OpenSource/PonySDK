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

import java.util.Collection;
import java.util.Objects;

import com.ponysdk.core.Parser;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.ui.server.basic.event.HasPAllDragAndDropHandlers;
import com.ponysdk.ui.server.basic.event.HasPClickHandlers;
import com.ponysdk.ui.server.basic.event.HasPDoubleClickHandlers;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDoubleClickEvent;
import com.ponysdk.ui.server.basic.event.PDoubleClickHandler;
import com.ponysdk.ui.server.basic.event.PDragEndEvent;
import com.ponysdk.ui.server.basic.event.PDragEndHandler;
import com.ponysdk.ui.server.basic.event.PDragEnterEvent;
import com.ponysdk.ui.server.basic.event.PDragEnterHandler;
import com.ponysdk.ui.server.basic.event.PDragLeaveEvent;
import com.ponysdk.ui.server.basic.event.PDragLeaveHandler;
import com.ponysdk.ui.server.basic.event.PDragOverEvent;
import com.ponysdk.ui.server.basic.event.PDragOverHandler;
import com.ponysdk.ui.server.basic.event.PDragStartEvent;
import com.ponysdk.ui.server.basic.event.PDragStartHandler;
import com.ponysdk.ui.server.basic.event.PDropEvent;
import com.ponysdk.ui.server.basic.event.PDropHandler;
import com.ponysdk.ui.server.basic.event.PHasText;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * A widget that contains arbitrary text, <i>not</i> interpreted as HTML. This widget uses a
 * &lt;div&gt; element, causing it to be displayed with block layout.
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Label { }</li>
 * </ul>
 */
public class PLabel extends PWidget implements PHasText, HasPClickHandlers, HasPDoubleClickHandlers, HasPAllDragAndDropHandlers {

    private static final String DEFAULT_TEXT_VALUE = null;

    private String text = DEFAULT_TEXT_VALUE;

    public PLabel() {
    }

    public PLabel(final String text) {
        this.text = text;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        if (this.text != DEFAULT_TEXT_VALUE) parser.parse(ServerToClientModel.TEXT, this.text);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.LABEL;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(final String text) {
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        executeUpdate(ServerToClientModel.TEXT, this.text);
    }

    @Override
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    @Override
    public Collection<PClickHandler> getClickHandlers() {
        return getHandlerSet(PClickEvent.TYPE, this);
    }

    @Override
    public HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler) {
        return addDomHandler(handler, PDoubleClickEvent.TYPE);
    }

    @Override
    public Collection<PDoubleClickHandler> getDoubleClickHandlers() {
        return getHandlerSet(PDoubleClickEvent.TYPE, this);
    }

    @Override
    public HandlerRegistration addDragEndHandler(final PDragEndHandler handler) {
        return addDomHandler(handler, PDragEndEvent.TYPE);
    }

    @Override
    public HandlerRegistration addDragEnterHandler(final PDragEnterHandler handler) {
        return addDomHandler(handler, PDragEnterEvent.TYPE);
    }

    @Override
    public HandlerRegistration addDragStartHandler(final PDragStartHandler handler) {
        return addDomHandler(handler, PDragStartEvent.TYPE);
    }

    @Override
    public HandlerRegistration addDragLeaveHandler(final PDragLeaveHandler handler) {
        return addDomHandler(handler, PDragLeaveEvent.TYPE);
    }

    @Override
    public HandlerRegistration addDragOverHandler(final PDragOverHandler handler) {
        return addDomHandler(handler, PDragOverEvent.TYPE);
    }

    @Override
    public HandlerRegistration addDropHandler(final PDropHandler handler) {
        return addDomHandler(handler, PDropEvent.TYPE);
    }

    @Override
    public String toString() {
        return super.toString() + ", text=" + text;
    }

}
