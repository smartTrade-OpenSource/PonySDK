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

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.HasPBlurHandlers;
import com.ponysdk.core.ui.basic.event.HasPClickHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragEndHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragEnterHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragLeaveHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragOverHandlers;
import com.ponysdk.core.ui.basic.event.HasPDragStartHandlers;
import com.ponysdk.core.ui.basic.event.HasPDropHandlers;
import com.ponysdk.core.ui.basic.event.HasPFocusHandlers;
import com.ponysdk.core.ui.basic.event.HasPMouseDownHandlers;
import com.ponysdk.core.ui.basic.event.HasPMouseOutHandlers;
import com.ponysdk.core.ui.basic.event.HasPMouseOverHandlers;
import com.ponysdk.core.ui.basic.event.HasPMouseUpHandlers;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PBlurHandler;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
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
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PFocusHandler;
import com.ponysdk.core.ui.basic.event.PMouseDownEvent;
import com.ponysdk.core.ui.basic.event.PMouseDownHandler;
import com.ponysdk.core.ui.basic.event.PMouseOutEvent;
import com.ponysdk.core.ui.basic.event.PMouseOutHandler;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PMouseOverHandler;
import com.ponysdk.core.ui.basic.event.PMouseUpEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

/**
 * A simple panel that makes its contents focusable, and adds the ability to catch mouse and
 * keyboard events.
 */
public class PFocusPanel extends PSimplePanel implements HasPDragEndHandlers, HasPDragEnterHandlers, HasPDragStartHandlers,
        HasPDragLeaveHandlers, HasPDragOverHandlers, HasPDropHandlers, HasPMouseOverHandlers, HasPMouseOutHandlers,
        HasPMouseDownHandlers, HasPMouseUpHandlers, HasPClickHandlers, HasPFocusHandlers, HasPBlurHandlers {

    protected PFocusPanel() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FOCUS_PANEL;
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
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    @Override
    public HandlerRegistration addMouseOverHandler(final PMouseOverHandler handler) {
        return addDomHandler(handler, PMouseOverEvent.TYPE);
    }

    @Override
    public HandlerRegistration addFocusHandler(final PFocusHandler handler) {
        return addDomHandler(handler, PFocusEvent.TYPE);
    }

    @Override
    public HandlerRegistration addBlurHandler(final PBlurHandler handler) {
        return addDomHandler(handler, PBlurEvent.TYPE);
    }

    @Override
    public HandlerRegistration addMouseOutHandler(final PMouseOutHandler handler) {
        return addDomHandler(handler, PMouseOutEvent.TYPE);
    }

    @Override
    public HandlerRegistration addMouseDownHandler(final PMouseDownHandler handler) {
        return addDomHandler(handler, PMouseDownEvent.TYPE);
    }

    @Override
    public HandlerRegistration addMouseUpHandler(final PMouseUpHandler handler) {
        return addDomHandler(handler, PMouseUpEvent.TYPE);
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

}
