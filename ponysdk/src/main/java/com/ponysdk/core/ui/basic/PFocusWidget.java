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

import jakarta.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.core.writer.ModelWriterCallback;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PBlurHandler;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickHandler;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PFocusHandler;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

/**
 * Abstract base class for most widgets that can receive keyboard focus.
 */
public abstract class PFocusWidget extends PWidget {

    private boolean enabled = true;
    private boolean enabledOnRequest = false;
    private boolean showLoadingOnRequest;

    protected PFocusWidget() {
        if (UIContext.get().getConfiguration().isTabindexOnlyFormField()) tabindex = TabindexMode.FOCUSABLE.getTabIndex();
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        if (!enabled) writer.write(ServerToClientModel.ENABLED, false);
    }

    public void showLoadingOnRequest(final boolean showLoadingOnRequest) {
        if (Objects.equals(this.showLoadingOnRequest, showLoadingOnRequest)) return;
        this.showLoadingOnRequest = showLoadingOnRequest;
        saveUpdate(writer -> writer.write(ServerToClientModel.LOADING_ON_REQUEST, showLoadingOnRequest));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        if (Objects.equals(this.enabled, enabled)) return;
        this.enabled = enabled;
        if (initialized) saveUpdate(ServerToClientModel.ENABLED, enabled); // replayed by enrichForUpdate otherwise
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

    public HandlerRegistration addMouseOverHandler(final PMouseOverEvent.Handler handler) {
        return addDomHandler(handler, PMouseOverEvent.TYPE);
    }

    public HandlerRegistration addFocusHandler(final PFocusHandler handler) {
        return addDomHandler(handler, PFocusEvent.TYPE);
    }

    public HandlerRegistration addBlurHandler(final PBlurHandler handler) {
        return addDomHandler(handler, PBlurEvent.TYPE);
    }

    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    public HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler) {
        return addDomHandler(handler, PDoubleClickEvent.TYPE);
    }

    private static final String DOM_HANDLER_TYPE_KEY = ClientToServerModel.DOM_HANDLER_TYPE.toStringValue();
    private static final ModelWriterCallback END_OF_PROCESSING = writer -> writer.write(ServerToClientModel.END_OF_PROCESSING);

    @Override
    public void onClientData(final JsonObject instruction) {
        if (canNotify(instruction)) super.onClientData(instruction);
    }

    /**
     * The terminal disabled the widget (or showed it loading) for this click: give it back once the click is
     * processed, whatever the number of handlers, decided by the flags the terminal applied; also when a subclass
     * filtered the click out (hidden or disabled meanwhile) or a handler threw.
     */
    @Override
    public void onClientDataProcessed(final JsonObject instruction) {
        super.onClientDataProcessed(instruction);
        if ((showLoadingOnRequest || !enabledOnRequest) && isClick(instruction)) saveUpdate(END_OF_PROCESSING);
    }

    private static boolean isClick(final JsonObject instruction) {
        if (!instruction.containsKey(DOM_HANDLER_TYPE_KEY)) return false;
        final int type = instruction.getInt(DOM_HANDLER_TYPE_KEY);
        return type == DomHandlerType.CLICK.getValue() || type == DomHandlerType.DOUBLE_CLICK.getValue();
    }

    @Override
    public void focus() {
        if (isVisible() && isEnabled()) super.focus();
    }

    @Override
    public void blur() {
        if (isVisible() && isEnabled()) super.blur();
    }

    protected boolean canNotify(final JsonObject instruction) {
        return isVisible() && (isEnabled() || instruction.containsKey(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue()));
    }
}
