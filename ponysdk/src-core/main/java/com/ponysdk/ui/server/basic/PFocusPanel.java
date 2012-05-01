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

import java.util.Collection;

import com.ponysdk.core.event.PHandlerRegistration;
import com.ponysdk.ui.server.basic.event.HasPAllDragAndDropHandlers;
import com.ponysdk.ui.server.basic.event.HasPAllFocusHandlers;
import com.ponysdk.ui.server.basic.event.HasPAllKeyHandlers;
import com.ponysdk.ui.server.basic.event.HasPAllMouseHandlers;
import com.ponysdk.ui.server.basic.event.HasPClickHandlers;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PBlurHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDragEndEvent;
import com.ponysdk.ui.server.basic.event.PDragEndHandler;
import com.ponysdk.ui.server.basic.event.PDragEnterEvent;
import com.ponysdk.ui.server.basic.event.PDragEnterHandler;
import com.ponysdk.ui.server.basic.event.PFocusEvent;
import com.ponysdk.ui.server.basic.event.PFocusHandler;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverHandler;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A simple panel that makes its contents focusable, and adds the ability to catch mouse and keyboard events.
 */
public class PFocusPanel extends PSimplePanel implements HasPAllDragAndDropHandlers, HasPAllMouseHandlers, HasPClickHandlers, HasPAllKeyHandlers, HasPAllFocusHandlers {

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FOCUS_PANEL;
    }

    @Override
    public PHandlerRegistration addDragEndHandler(final PDragEndHandler handler) {
        return addDomHandler(handler, PDragEndEvent.TYPE);
    }

    @Override
    public PHandlerRegistration addDragEndHandler(final PDragEnterHandler handler) {
        return addDomHandler(handler, PDragEnterEvent.TYPE);
    }

    @Override
    public PHandlerRegistration addKeyUpHandler(final PKeyUpHandler handler) {
        return addDomHandler(handler, PKeyUpEvent.TYPE);
    }

    @Override
    public PHandlerRegistration addKeyPressHandler(final PKeyPressHandler handler) {
        return addDomHandler(handler, PKeyPressEvent.TYPE);
    }

    @Override
    public Collection<PKeyUpHandler> getKeyUpHandlers() {
        return getHandlerSet(PKeyUpEvent.TYPE, this);
    }

    @Override
    public Collection<PKeyPressHandler> getKeyPressHandlers() {
        return getHandlerSet(PKeyPressEvent.TYPE, this);
    }

    @Override
    public PHandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    @Override
    public Collection<PClickHandler> getClickHandlers() {
        return getHandlerSet(PClickEvent.TYPE, this);
    }

    @Override
    public PHandlerRegistration addMouseOverHandler(final PMouseOverHandler handler) {
        return addDomHandler(handler, PMouseOverEvent.TYPE);
    }

    @Override
    public Collection<PMouseOverHandler> getMouseOverHandlers() {
        return getHandlerSet(PMouseOverEvent.TYPE, this);
    }

    @Override
    public PHandlerRegistration addFocusHandler(final PFocusHandler handler) {
        return addDomHandler(handler, PFocusEvent.TYPE);
    }

    @Override
    public PHandlerRegistration addBlurHandler(final PBlurHandler handler) {
        return addDomHandler(handler, PBlurEvent.TYPE);
    }

}
