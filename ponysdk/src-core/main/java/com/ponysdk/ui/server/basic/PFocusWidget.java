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
import com.ponysdk.core.stm.TxnBoolean;
import com.ponysdk.core.stm.TxnObject;
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

    private final TxnBoolean enabled = new TxnBoolean(Boolean.TRUE);
    private final TxnBoolean enabledOnRequest = new TxnBoolean();
    private final TxnBoolean focused = new TxnBoolean();
    private final TxnBoolean showLoadingOnRequest = new TxnBoolean();

    public PFocusWidget() {
        super();
        this.enabled.setListener(this);
        this.enabledOnRequest.setListener(this);
        this.focused.setListener(this);
        this.showLoadingOnRequest.setListener(this);
    }

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
        this.enabled.set(enabled);
    }

    public void setEnabledOnRequest(final boolean enabledOnRequest) {
        this.enabledOnRequest.set(enabledOnRequest);
    }

    public void showLoadingOnRequest(final boolean showLoadingOnRequest) {
        this.showLoadingOnRequest.set(showLoadingOnRequest);
    }

    @Override
    public void setFocus(final boolean focused) {
        this.focused.set(focused);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public boolean isShowLoadingOnRequest() {
        return showLoadingOnRequest.get();
    }

    public boolean isEnabledOnRequest() {
        return enabledOnRequest.get();
    }

    public boolean isFocused() {
        return focused.get();
    }

    @Override
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        if (showLoadingOnRequest.get() || !enabledOnRequest.get()) {
            final PClickHandler clickHandler = new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    handler.onClick(event);
                    saveUpdate(PROPERTY.END_OF_PROCESSING, true);
                }
            };

            return addDomHandler(clickHandler, PClickEvent.TYPE);
        } else {
            return addDomHandler(handler, PClickEvent.TYPE);
        }
    }

    @Override
    public void beforeFlush(final TxnObject<?> txnObject) {
        if (txnObject == enabled) {
            saveUpdate(PROPERTY.ENABLED, enabled.get());
        } else if (txnObject == enabledOnRequest) {
            saveUpdate(PROPERTY.ENABLED_ON_REQUEST, enabledOnRequest.get());
        } else if (txnObject == focused) {
            saveUpdate(PROPERTY.FOCUSED, focused.get());
        } else if (txnObject == showLoadingOnRequest) {
            saveUpdate(PROPERTY.LOADING_ON_REQUEST, showLoadingOnRequest.get());
        } else {
            super.beforeFlush(txnObject);
        }
    }

}