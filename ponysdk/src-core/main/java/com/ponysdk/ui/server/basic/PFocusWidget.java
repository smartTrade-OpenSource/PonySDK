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

import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.HasPAllKeyHandlers;
import com.ponysdk.ui.server.basic.event.HasPBlurHandlers;
import com.ponysdk.ui.server.basic.event.HasPClickHandlers;
import com.ponysdk.ui.server.basic.event.HasPFocusHandlers;
import com.ponysdk.ui.server.basic.event.HasPMouseOverHandlers;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PBlurHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PFocusEvent;
import com.ponysdk.ui.server.basic.event.PFocusHandler;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverHandler;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;

/**
 * Abstract base class for most widgets that can receive keyboard focus.
 */
public abstract class PFocusWidget extends PWidget implements Focusable, HasPClickHandlers, HasPMouseOverHandlers, HasPAllKeyHandlers, HasPFocusHandlers, HasPBlurHandlers {

    private boolean enabled = true;

    private boolean enabledOnRequest = false;

    private boolean focused;

    private boolean showLoadingOnRequest;

    @Override
    public Collection<PClickHandler> getClickHandlers() {
        return getHandlerSet(PClickEvent.TYPE, this);
    }

    @Override
    public Collection<PMouseOverHandler> getMouseOverHandlers() {
        return getHandlerSet(PMouseOverEvent.TYPE, this);
    }

    @Override
    public HandlerRegistration addMouseOverHandler(final PMouseOverHandler handler) {
        return addDomHandler(handler, PMouseOverEvent.TYPE);
    }

    @Override
    public HandlerRegistration addKeyUpHandler(final PKeyUpHandler handler) {
        return addDomHandler(handler, PKeyUpEvent.TYPE);
    }

    @Override
    public HandlerRegistration addKeyPressHandler(final PKeyPressHandler handler) {
        return addDomHandler(handler, PKeyPressEvent.TYPE);
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
    public Collection<PKeyPressHandler> getKeyPressHandlers() {
        return getHandlerSet(PKeyPressEvent.TYPE, this);
    }

    @Override
    public Collection<PKeyUpHandler> getKeyUpHandlers() {
        return getHandlerSet(PKeyUpEvent.TYPE, this);
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        final Update update = new Update(getID());
        update.put(PROPERTY.ENABLED, enabled);
        getUIContext().stackInstruction(update);
    }

    public void setEnabledOnRequest(final boolean enabledOnRequest) {
        this.enabledOnRequest = enabledOnRequest;
        final Update update = new Update(ID);
        update.put(PROPERTY.ENABLED_ON_REQUEST, enabledOnRequest);
        getUIContext().stackInstruction(update);
    }

    public void showLoadingOnRequest(final boolean showLoadingOnRequest) {
        this.showLoadingOnRequest = showLoadingOnRequest;
        final Update update = new Update(ID);
        update.put(PROPERTY.LOADING_ON_REQUEST, showLoadingOnRequest);
        getUIContext().stackInstruction(update);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocus(final boolean focused) {
        this.focused = focused;
        final Update update = new Update(getID());
        update.put(PROPERTY.FOCUSED, focused);
        getUIContext().stackInstruction(update);
    }

    @Override
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        if (showLoadingOnRequest || !enabledOnRequest) {
            final PClickHandler clickHandler = new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    handler.onClick(event);
                    final Update update = new Update(ID);
                    update.put(PROPERTY.END_OF_PROCESSING, true);
                    getUIContext().stackInstruction(update);
                }
            };

            return addDomHandler(clickHandler, PClickEvent.TYPE);
        } else {
            return addDomHandler(handler, PClickEvent.TYPE);
        }
    }

}
