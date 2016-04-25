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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.user.client.ui.FocusWidget;
import com.ponysdk.ui.terminal.DomHandlerType;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public abstract class PTFocusWidget<T extends FocusWidget> extends PTWidget<T> {

    private boolean showLoadingOnRequest = false;

    private boolean enabledOnRequest = false;

    private boolean enabled = true;

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.LOADING_ON_REQUEST.equals(binaryModel.getModel())) {
            showLoadingOnRequest = binaryModel.getBooleanValue();
            return true;
        }
        if (ServerToClientModel.ENABLED_ON_REQUEST.equals(binaryModel.getModel())) {
            enabledOnRequest = binaryModel.getBooleanValue();
            return true;
        }
        if (ServerToClientModel.END_OF_PROCESSING.equals(binaryModel.getModel())) {
            if (showLoadingOnRequest)
                uiObject.removeStyleName("pony-Loading");
            if (!enabledOnRequest)
                uiObject.setEnabled(enabled);
            return true;
        }
        if (ServerToClientModel.ENABLED.equals(binaryModel.getModel())) {
            this.enabled = binaryModel.getBooleanValue();
            uiObject.setEnabled(enabled);
            return true;
        }
        if (ServerToClientModel.TABINDEX.equals(binaryModel.getModel())) {
            uiObject.setTabIndex(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.FOCUSED.equals(binaryModel.getModel())) {
            uiObject.setFocus(binaryModel.getBooleanValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    protected void triggerMouseEvent(final DomHandlerType domHandlerType, final UIService uiService, final MouseEvent<?> event) {
        if (!enabledOnRequest) uiObject.setEnabled(false);
        if (showLoadingOnRequest) uiObject.addStyleName("pony-Loading");
        super.triggerMouseEvent(domHandlerType, uiService, event);
    }

}
