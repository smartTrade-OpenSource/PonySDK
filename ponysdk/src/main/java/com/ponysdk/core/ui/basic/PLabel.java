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
import com.ponysdk.core.ui.basic.event.*;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.util.Incubation;
import com.ponysdk.core.writer.ModelWriter;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A widget that contains arbitrary text, <i>not</i> interpreted as HTML. This widget uses a &lt;div&gt; element,
 * causing it to be displayed with block layout.
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Label { }</li>
 * </ul>
 */
public class PLabel extends PWidget {

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

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        if (initialized) saveUpdate(ServerToClientModel.TEXT, this.text);
    }

    public String getAttributeLinkedToValue() {
        return attributeLinkedToValue;
    }

    /**
     * Link an HTML attribute (like "data-title") directly to the value.
     */
    @Incubation(since = "2.8.11")
    public void setAttributeLinkedToValue(final String attributeLinkedToValue) {
        if (Objects.equals(this.attributeLinkedToValue, attributeLinkedToValue)) return;
        this.attributeLinkedToValue = attributeLinkedToValue;
        if (initialized) saveUpdate(ServerToClientModel.ATTRIBUTE_LINKED_TO_VALUE, this.attributeLinkedToValue);
    }

    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    public HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler) {
        return addDomHandler(handler, PDoubleClickEvent.TYPE);
    }

    public HandlerRegistration addDragEndHandler(final PDragEndHandler handler) {
        return addDomHandler(handler, PDragEndEvent.TYPE);
    }

    public HandlerRegistration addDragEnterHandler(final PDragEnterHandler handler) {
        return addDomHandler(handler, PDragEnterEvent.TYPE);
    }

    public HandlerRegistration addDragStartHandler(final PDragStartHandler handler) {
        return addDomHandler(handler, PDragStartEvent.TYPE);
    }

    public HandlerRegistration addDragLeaveHandler(final PDragLeaveEvent.Handler handler) {
        return addDomHandler(handler, PDragLeaveEvent.TYPE);
    }

    public HandlerRegistration addDragOverHandler(final PDragOverEvent.Handler handler) {
        return addDomHandler(handler, PDragOverEvent.TYPE);
    }

    public HandlerRegistration addDropHandler(final PDropHandler handler) {
        return addDomHandler(handler, PDropEvent.TYPE);
    }

    @Override
    public String toString() {
        return super.toString() + ", text=" + text;
    }

    @Override
    protected String dumpDOM() {
        return "<label pid=\"" + ID + "\" class=\"" + getStyleNames().collect(Collectors.joining(" ")) + "\">" + text + "</label>";
    }
}
