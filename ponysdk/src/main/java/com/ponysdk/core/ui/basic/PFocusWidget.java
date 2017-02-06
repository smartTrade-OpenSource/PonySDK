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

import com.ponysdk.core.model.ServerToClientModel;
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
    private boolean focused = false;
    private boolean showLoadingOnRequest;
    private int tabindex = Integer.MIN_VALUE;

    protected PFocusWidget() {
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

    public void showLoadingOnRequest(final boolean showLoadingOnRequest) {
        if (Objects.equals(this.showLoadingOnRequest, showLoadingOnRequest)) return;
        this.showLoadingOnRequest = showLoadingOnRequest;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.LOADING_ON_REQUEST, showLoadingOnRequest));
    }

    @Override
    public void setFocus(final boolean focused) {
        if (Objects.equals(this.focused, focused)) return;
        this.focused = focused;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.FOCUSED, focused));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        if (Objects.equals(this.enabled, enabled)) return;
        this.enabled = enabled;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.ENABLED, enabled));
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
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.ENABLED_ON_REQUEST, enabledOnRequest));
    }

    public boolean isFocused() {
        return focused;
    }

    public int getTabindex() {
        return tabindex;
    }

    public void setTabindex(final int tabindex) {
        if (this.tabindex == tabindex) return;
        this.tabindex = tabindex;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.TABINDEX, tabindex));
    }

    @Override
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        if (showLoadingOnRequest || !enabledOnRequest) {
            return addDomHandler(event -> {
                handler.onClick(event);
                saveUpdate(writer -> writer.writeModel(ServerToClientModel.END_OF_PROCESSING));
            } , PClickEvent.TYPE);
        } else {
            return addDomHandler(handler, PClickEvent.TYPE);
        }
    }

    @Override
    public HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler) {
        if (showLoadingOnRequest || !enabledOnRequest) {
            final PDoubleClickHandler clickHandler = event -> {
                handler.onDoubleClick(event);
                saveUpdate(writer -> writer.writeModel(ServerToClientModel.END_OF_PROCESSING));
            };

            return addDomHandler(clickHandler, PDoubleClickEvent.TYPE);
        } else {
            return addDomHandler(handler, PDoubleClickEvent.TYPE);
        }
    }

}
