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

import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.HasPBlurHandlers;
import com.ponysdk.core.ui.basic.event.HasPClickHandlers;
import com.ponysdk.core.ui.basic.event.HasPDoubleClickHandlers;
import com.ponysdk.core.ui.basic.event.HasPFocusHandlers;
import com.ponysdk.core.ui.basic.event.HasPMouseOverHandlers;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PBlurHandler;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickHandler;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PFocusHandler;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PMouseOverHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

/**
 * Abstract base class for most widgets that can receive keyboard focus.
 */
public abstract class PFocusWidget extends PWidget
        implements Focusable, HasPClickHandlers, HasPDoubleClickHandlers, HasPMouseOverHandlers, HasPFocusHandlers, HasPBlurHandlers {

    private boolean enabled = true;
    private boolean enabledOnRequest = false;
    private boolean showLoadingOnRequest;

    protected PFocusWidget() {
        if (UIContext.get().getConfiguration().isTabindexOnlyFormField()) tabindex = TabindexMode.FOCUSABLE.getTabIndex();
    }

    public void showLoadingOnRequest(final boolean showLoadingOnRequest) {
        if (Objects.equals(this.showLoadingOnRequest, showLoadingOnRequest)) return;
        this.showLoadingOnRequest = showLoadingOnRequest;
        saveUpdate(writer -> writer.write(ServerToClientModel.LOADING_ON_REQUEST, showLoadingOnRequest));
    }

    /**
     * @deprecated Use {@link #focus()} or {@link #blur()}
     * @since v2.7.16
     */
    @Deprecated
    @Override
    public void setFocus(final boolean focused) {
        if (focused) focus();
        else blur();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        if (Objects.equals(this.enabled, enabled)) return;
        this.enabled = enabled;
        saveUpdate(ServerToClientModel.ENABLED, enabled);
    }

    public boolean isShowLoadingOnRequest() {
        return showLoadingOnRequest;
    }

    public boolean isEnabledOnRequest() {
        return enabledOnRequest;
    }

    public void setEnabledOnRequest(final boolean enabledOnRequest) {
        if (Objects.equals(this.enabledOnRequest, enabledOnRequest)) return;
        this.enabledOnRequest = enabledOnRequest;
        saveUpdate(ServerToClientModel.ENABLED_ON_REQUEST, enabledOnRequest);
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
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        if (showLoadingOnRequest || !enabledOnRequest) {
            return addDomHandler((PClickHandler) event -> {
                try {
                    handler.onClick(event);
                } finally {
                    saveUpdate(writer -> writer.write(ServerToClientModel.END_OF_PROCESSING));
                }
            }, PClickEvent.TYPE);
        } else {
            return addDomHandler(handler, PClickEvent.TYPE);
        }
    }

    @Override
    public HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler) {
        if (showLoadingOnRequest || !enabledOnRequest) {
            final PDoubleClickHandler clickHandler = event -> {
                try {
                    handler.onDoubleClick(event);
                } finally {
                    saveUpdate(writer -> writer.write(ServerToClientModel.END_OF_PROCESSING));
                }
            };

            return addDomHandler(clickHandler, PDoubleClickEvent.TYPE);
        } else {
            return addDomHandler(handler, PDoubleClickEvent.TYPE);
        }
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (isVisible() && isEnabled()) super.onClientData(instruction);
    }

    @Override
    public void focus() {
        if (isVisible() && isEnabled()) super.focus();
    }

    @Override
    public void blur() {
        if (isVisible() && isEnabled()) super.blur();
    }

}
