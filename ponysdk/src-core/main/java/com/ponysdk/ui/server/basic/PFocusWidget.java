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
import java.util.Objects;

import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.ui.server.basic.event.HasPAllKeyHandlers;
import com.ponysdk.ui.server.basic.event.HasPBlurHandlers;
import com.ponysdk.ui.server.basic.event.HasPClickHandlers;
import com.ponysdk.ui.server.basic.event.HasPDoubleClickHandlers;
import com.ponysdk.ui.server.basic.event.HasPFocusHandlers;
import com.ponysdk.ui.server.basic.event.HasPMouseOverHandlers;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PBlurHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDoubleClickEvent;
import com.ponysdk.ui.server.basic.event.PDoubleClickHandler;
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
public abstract class PFocusWidget extends PWidget implements Focusable, HasPClickHandlers, HasPDoubleClickHandlers, HasPMouseOverHandlers, HasPAllKeyHandlers, HasPFocusHandlers, HasPBlurHandlers {

    private boolean enabled = true;
    private boolean enabledOnRequest = false;

    private boolean focused = false;
    private boolean showLoadingOnRequest;
    private int tabindex = Integer.MIN_VALUE;

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
        if (Objects.equals(this.enabled, enabled)) return;
        this.enabled = enabled;
        saveUpdate(PROPERTY.ENABLED, enabled);
    }

    public void setTabindex(final int tabindex) {
        if (this.tabindex == tabindex) return;
        this.tabindex = tabindex;
        saveUpdate(PROPERTY.TABINDEX, tabindex);
    }

    public void setEnabledOnRequest(final boolean enabledOnRequest) {
        if (Objects.equals(this.enabledOnRequest, enabledOnRequest)) return;
        this.enabledOnRequest = enabledOnRequest;
        saveUpdate(PROPERTY.ENABLED_ON_REQUEST, this.enabledOnRequest);
    }

    public void showLoadingOnRequest(final boolean showLoadingOnRequest) {
        if (Objects.equals(this.showLoadingOnRequest, showLoadingOnRequest)) return;
        this.showLoadingOnRequest = showLoadingOnRequest;
        saveUpdate(PROPERTY.LOADING_ON_REQUEST, showLoadingOnRequest);
    }

    @Override
    public void setFocus(final boolean focused) {
        if (Objects.equals(this.focused, focused)) return;
        this.focused = focused;
        saveUpdate(PROPERTY.FOCUSED, focused);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isShowLoadingOnRequest() {
        return showLoadingOnRequest;
    }

    public boolean isEnabledOnRequest() {
        return enabledOnRequest;
    }

    public boolean isFocused() {
        return focused;
    }

    public int getTabindex() {
        return tabindex;
    }

    @Override
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        if (showLoadingOnRequest || !enabledOnRequest) {
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
    public HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler) {
        if (showLoadingOnRequest || !enabledOnRequest) {
            final PDoubleClickHandler clickHandler = new PDoubleClickHandler() {

                @Override
                public void onDoubleClick(final PDoubleClickEvent event) {
                    handler.onDoubleClick(event);
                    saveUpdate(PROPERTY.END_OF_PROCESSING, true);
                }
            };

            return addDomHandler(clickHandler, PDoubleClickEvent.TYPE);
        } else {
            return addDomHandler(handler, PDoubleClickEvent.TYPE);
        }
    }

    @Override
    public Collection<PDoubleClickHandler> getDoubleClickHandlers() {
        return getHandlerSet(PDoubleClickEvent.TYPE, this);
    }

}
