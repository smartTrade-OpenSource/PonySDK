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

package com.ponysdk.core.terminal.ui;

import java.util.function.Consumer;

import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.user.client.ui.FocusWidget;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public abstract class PTFocusWidget<T extends FocusWidget> extends PTWidget<T> {

    protected boolean enabled = true;
    private boolean showLoadingOnRequest = false;
    private boolean enabledOnRequest = false;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        if (PonySDK.get().isTabindexOnlyFormField()) uiObject.setTabIndex(-1);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.LOADING_ON_REQUEST == model) {
            showLoadingOnRequest = binaryModel.getBooleanValue();
            return true;
        } else if (ServerToClientModel.ENABLED_ON_REQUEST == model) {
            enabledOnRequest = binaryModel.getBooleanValue();
            return true;
        } else if (ServerToClientModel.END_OF_PROCESSING == model) {
            if (showLoadingOnRequest) uiObject.removeStyleName("pony-Loading");
            if (!enabledOnRequest) uiObject.setEnabled(enabled);
            return true;
        } else if (ServerToClientModel.ENABLED == model) {
            this.enabled = binaryModel.getBooleanValue();
            uiObject.setEnabled(enabled);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    protected void triggerDomEvent(final DomHandlerType domHandlerType, final DomEvent<?> event,
                                   final Consumer<PTInstruction> enricher) {
        if (enabled) super.triggerDomEvent(domHandlerType, event, enricher);
    }

    @Override
    protected void triggerMouseClickEvent(final DomHandlerType domHandlerType, final MouseEvent<?> event) {
        if (!enabled) return;
        if (!enabledOnRequest) uiObject.setEnabled(false);
        if (showLoadingOnRequest) uiObject.addStyleName("pony-Loading");
        super.triggerMouseEvent(domHandlerType, event);
    }

    @Override
    protected void triggerKeyEvent(final DomHandlerType domHandlerType, final KeyEvent<?> event, final int[] keyFilter) {
        if (enabled) super.triggerKeyEvent(domHandlerType, event, keyFilter);
    }

}
