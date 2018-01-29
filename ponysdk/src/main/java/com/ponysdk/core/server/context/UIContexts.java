/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.server.context;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UIContexts {

    private static final UIContexts INSTANCE = new UIContexts();
    private final Map<Integer, UIContext> contexts = new ConcurrentHashMap<>();

    private UIContexts() {
    }

    public static UIContexts get() {
        return INSTANCE;
    }

    public static UIContext getContext(final Integer id) {
        return get().getContext0(id);
    }

    private UIContext getContext0(final Integer id) {
        return contexts.get(id);
    }

    public static Collection<UIContext> getContexts() {
        return get().getContexts0();
    }

    private Collection<UIContext> getContexts0() {
        return contexts.values();
    }
}
