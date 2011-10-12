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
package com.ponysdk.core.activity;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.event.BroadcastEventHandler;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.Event.Type;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.place.Place;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;

public abstract class AbstractActivity implements Activity {

    protected boolean started = false;

    @Override
    public void goTo(Place place, PAcceptsOneWidget world) {
        if (!started) {
            start(world);
            started = true;
        }
    }

    protected abstract void start(PAcceptsOneWidget world);

    public EventBus getRootEventBus() {
        return PonySession.getRootEventBus();
    }

    public <H extends EventHandler> HandlerRegistration addHandler(Type<H> type, H handler) {
        return PonySession.addHandler(type, handler);
    }

    public <H extends EventHandler> HandlerRegistration addHandlerToSource(Type<H> type, Object source, H handler) {
        return PonySession.addHandlerToSource(type, source, handler);
    }

    @SuppressWarnings("unchecked")
    public <H extends EventHandler> HandlerRegistration addHandlerToSource(Type<H> type, Object source) {
        return addHandlerToSource(type, source, (H) this);
    }

    public void fireEvent(Event<?> event) {
        PonySession.fireEvent(event);
    }

    public void fireEventFromSource(Event<?> event, Object source) {
        PonySession.fireEventFromSource(event, source);
    }

    public void addHandler(BroadcastEventHandler handler) {
        PonySession.addHandler(handler);
    }
}