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

import java.util.List;

import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.ui.server.basic.event.HasPAllKeyHandlers;
import com.ponysdk.ui.server.basic.event.HasPClickHandlers;
import com.ponysdk.ui.server.basic.event.HasPMouseOverHandlers;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverHandler;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.instruction.Update;

public abstract class PFocusWidget extends PWidget implements Focusable, HasPClickHandlers, HasPMouseOverHandlers, HasPAllKeyHandlers {

    private boolean enabled = true;
    private boolean focused;
    private boolean loadingOnRequest;

    @Override
    public List<PClickHandler> getClickHandlers() {
        return getHandlerList(PClickEvent.TYPE, this);
    }

    @Override
    public List<PMouseOverHandler> getMouseOverHandlers() {
        return getHandlerList(PMouseOverEvent.TYPE, this);
    }

    @Override
    public HandlerRegistration addMouseOverHandler(PMouseOverHandler handler) {
        return addDomHandler(handler, PMouseOverEvent.TYPE);
    }

    @Override
    public HandlerRegistration addKeyUpHandler(PKeyUpHandler handler) {
        return addDomHandler(handler, PKeyUpEvent.TYPE);
    }

    @Override
    public HandlerRegistration addKeyPressHandler(PKeyPressHandler handler) {
        return addDomHandler(handler, PKeyPressEvent.TYPE);
    }

    @Override
    public List<PKeyPressHandler> getKeyPressHandlers() {
        return getHandlerList(PKeyPressEvent.TYPE, this);
    }

    @Override
    public List<PKeyUpHandler> getKeyUpHandlers() {
        return getHandlerList(PKeyUpEvent.TYPE, this);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.ENABLED, enabled);
        getPonySession().stackInstruction(update);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocus(boolean focused) {
        this.focused = focused;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.FOCUSED, focused);
        getPonySession().stackInstruction(update);
    }

    public void showLoadingOnRequest(boolean loadingOnRequest) {
        this.loadingOnRequest = loadingOnRequest;
        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.LOADING, loadingOnRequest);
        getPonySession().stackInstruction(update);
    }

    @Override
    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        if (loadingOnRequest) {
            final PClickHandler clickHandler = new PClickHandler() {

                @Override
                public void onClick(PClickEvent event) {
                    handler.onClick(event);
                    final Update update = new Update(ID);
                    update.setMainPropertyValue(PropertyKey.END_OF_PROCESSING, true);
                    getPonySession().stackInstruction(update);
                }
            };

            return addDomHandler(clickHandler, PClickEvent.TYPE);
        } else {
            return addDomHandler(handler, PClickEvent.TYPE);
        }
    }

}
