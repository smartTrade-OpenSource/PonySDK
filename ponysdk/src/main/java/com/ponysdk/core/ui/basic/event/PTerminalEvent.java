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

package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventHandler;
import jakarta.json.JsonObject;


// TODO No need for an Event, refactor like {@link com.ponysdk.core.ui.basic.PObject.AjaxHandler}
public class PTerminalEvent extends Event<PTerminalEvent.Handler> {

    public static final Type TYPE = new Type();

    public PTerminalEvent(final Object sourceComponent, final JsonObject data) {
        super(sourceComponent);
        setData(data);
    }

    @Override
    public Type getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PTerminalEvent.Handler handler) {
        handler.onTerminalEvent(this);
    }

    @Override
    public JsonObject getData() {
        return (JsonObject) super.getData();
    }

    @FunctionalInterface
    public interface Handler extends EventHandler {

        void onTerminalEvent(PTerminalEvent event);

    }

}
