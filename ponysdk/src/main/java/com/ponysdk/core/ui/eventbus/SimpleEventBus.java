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

package com.ponysdk.core.ui.eventbus;

import java.util.HashMap;
import java.util.Set;

import com.ponysdk.core.util.SetUtils;

public class SimpleEventBus extends AbstractEventBus {

    @Override
    protected HashMap<Object, Set<EventHandler>> createEventHandlerMap() {
        return new HashMap<>(1); // Alway only one element in this map because it's a SimpleEventBus, so only on one PObject
    }

    @Override
    protected Set<EventHandler> createHandlerSet() {
        return SetUtils.newArraySet(4);
    }

}
