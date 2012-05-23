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

package com.ponysdk.core.deprecated;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.event.PBroadcastEventHandler;
import com.ponysdk.core.event.PEvent;
import com.ponysdk.core.event.PEvent.Type;
import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.event.PEventHandler;
import com.ponysdk.core.event.PHandlerRegistration;

public abstract class AbstractActivity implements Activity {

    public AbstractActivity() {}

    public PEventBus getRootEventBus() {
        return PonySession.getRootEventBus();
    }

    public <H extends PEventHandler> PHandlerRegistration addHandler(final Type<H> type, final H handler) {
        return PonySession.addHandler(type, handler);
    }

    public <H extends PEventHandler> PHandlerRegistration addHandlerToSource(final Type<H> type, final Object source, final H handler) {
        return PonySession.addHandlerToSource(type, source, handler);
    }

    @SuppressWarnings("unchecked")
    public <H extends PEventHandler> PHandlerRegistration addHandlerToSource(final Type<H> type, final Object source) {
        return addHandlerToSource(type, source, (H) this);
    }

    public void fireEvent(final PEvent<?> event) {
        PonySession.fireEvent(event);
    }

    public void fireEventFromSource(final PEvent<?> event, final Object source) {
        PonySession.fireEventFromSource(event, source);
    }

    public void addHandler(final PBroadcastEventHandler handler) {
        PonySession.addHandler(handler);
    }

}