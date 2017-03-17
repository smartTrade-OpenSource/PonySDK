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

import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.user.client.ui.FocusWidget;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public abstract class PTFocusWidget<T extends FocusWidget> extends PTWidget<T> {

    private boolean showLoadingOnRequest = false;

    private boolean enabledOnRequest = false;

    private boolean enabled = true;

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.LOADING_ON_REQUEST.ordinal() == modelOrdinal) {
            showLoadingOnRequest = binaryModel.getBooleanValue();
            return true;
        } else if (ServerToClientModel.ENABLED_ON_REQUEST.ordinal() == modelOrdinal) {
            enabledOnRequest = binaryModel.getBooleanValue();
            return true;
        } else if (ServerToClientModel.END_OF_PROCESSING.ordinal() == modelOrdinal) {
            if (showLoadingOnRequest) uiObject.removeStyleName("pony-Loading");
            if (!enabledOnRequest) uiObject.setEnabled(enabled);
            return true;
        } else if (ServerToClientModel.ENABLED.ordinal() == modelOrdinal) {
            this.enabled = binaryModel.getBooleanValue();
            uiObject.setEnabled(enabled);
            return true;
        } else if (ServerToClientModel.TABINDEX.ordinal() == modelOrdinal) {
            uiObject.setTabIndex(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.FOCUSED.ordinal() == modelOrdinal) {
            uiObject.setFocus(binaryModel.getBooleanValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    protected void triggerMouseEvent(final DomHandlerType domHandlerType, final MouseEvent<?> event) {
        if (!enabledOnRequest) uiObject.setEnabled(false);
        if (showLoadingOnRequest) uiObject.addStyleName("pony-Loading");
        super.triggerMouseEvent(domHandlerType, event);
    }

}
