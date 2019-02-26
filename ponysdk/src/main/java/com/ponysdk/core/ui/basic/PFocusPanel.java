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
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PBlurHandler;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PDragEndEvent;
import com.ponysdk.core.ui.basic.event.PDragEndHandler;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterHandler;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragOverEvent;
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
import com.ponysdk.core.ui.basic.event.PMouseUpEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

/**
 * A simple panel that makes its contents focusable, and adds the ability to catch mouse and
 * keyboard events.
 */
public class PFocusPanel extends PSimplePanel {

    protected PFocusPanel() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FOCUS_PANEL;
    }

    public HandlerRegistration addDragEndHandler(final PDragEndHandler handler) {
        return addDomHandler(handler, PDragEndEvent.TYPE);
    }

    public HandlerRegistration addDragEnterHandler(final PDragEnterHandler handler) {
        return addDomHandler(handler, PDragEnterEvent.TYPE);
    }

    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    public HandlerRegistration addMouseOverHandler(final PMouseOverEvent.Handler handler) {
        return addDomHandler(handler, PMouseOverEvent.TYPE);
    }

    public HandlerRegistration addFocusHandler(final PFocusHandler handler) {
        return addDomHandler(handler, PFocusEvent.TYPE);
    }

    public HandlerRegistration addBlurHandler(final PBlurHandler handler) {
        return addDomHandler(handler, PBlurEvent.TYPE);
    }

    public HandlerRegistration addMouseOutHandler(final PMouseOutHandler handler) {
        return addDomHandler(handler, PMouseOutEvent.TYPE);
    }

    public HandlerRegistration addMouseDownHandler(final PMouseDownHandler handler) {
        return addDomHandler(handler, PMouseDownEvent.TYPE);
    }

    public HandlerRegistration addMouseUpHandler(final PMouseUpHandler handler) {
        return addDomHandler(handler, PMouseUpEvent.TYPE);
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

}
