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
import com.ponysdk.core.ui.basic.event.HasPClickHandlers;
import com.ponysdk.core.ui.basic.event.HasPDoubleClickHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragEndHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragEnterHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragLeaveHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragOverHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragStartHandlers;
import com.ponysdk.core.ui.basic.event.HasPDropHandlers;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickHandler;
import com.ponysdk.core.ui.basic.event.PDragEndEvent;
import com.ponysdk.core.ui.basic.event.PDragEndHandler;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterHandler;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragLeaveHandler;
import com.ponysdk.core.ui.basic.event.PDragOverEvent;
import com.ponysdk.core.ui.basic.event.PDragOverHandler;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDragStartHandler;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PDropHandler;
import com.ponysdk.core.ui.basic.event.PHasText;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.writer.ModelWriter;

/**
 * A widget that contains arbitrary text, <i>not</i> interpreted as HTML. This widget uses a
 * &lt;div&gt; element, causing it to be displayed with block layout.
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Label { }</li>
 * </ul>
 */
public class PLabel extends PWidget implements PHasText, HasPClickHandlers, HasPDoubleClickHandlers, HasPDragEndHandlers,
        HasPDragEnterHandlers, HasPDragStartHandlers, HasPDragLeaveHandlers, HasPDragOverHandlers, HasPDropHandlers {

    protected String text;
    private String attributeLinkedToValue;

    protected PLabel() {
        this(null);
    }

    protected PLabel(final String text) {
        this.text = text;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        if (this.attributeLinkedToValue != null)
            writer.write(ServerToClientModel.ATTRIBUTE_LINKED_TO_VALUE, this.attributeLinkedToValue);
        if (this.text != null) writer.write(ServerToClientModel.TEXT, this.text);
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
        if (initialized) saveUpdate(ServerToClientModel.TEXT, this.text);
    }

    public String getAttributeLinkedToValue() {
        return attributeLinkedToValue;
    }

    public void setAttributeLinkedToValue(final String attributeLinkedToValue) {
        if (Objects.equals(this.attributeLinkedToValue, attributeLinkedToValue)) return;
        this.attributeLinkedToValue = attributeLinkedToValue;
        if (initialized) saveUpdate(ServerToClientModel.ATTRIBUTE_LINKED_TO_VALUE, this.attributeLinkedToValue);
    }

    @Override
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    @Override
    public HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler) {
        return addDomHandler(handler, PDoubleClickEvent.TYPE);
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
