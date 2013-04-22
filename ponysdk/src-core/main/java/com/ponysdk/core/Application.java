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

package com.ponysdk.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ponysdk.core.servlet.Session;

public class Application {

    private static final AtomicLong ponySessionIDcount = new AtomicLong();

    private final Session session;

    private final Map<Long, UIContext> uiContexts = new ConcurrentHashMap<Long, UIContext>();

    public Application(final Session session) {
        this.session = session;
    }

    public void registerUIContext(final UIContext uiContext) {
        final long id = ponySessionIDcount.incrementAndGet();
        uiContexts.put(id, uiContext);
        uiContext.setUiContextID(id);
    }

    public void unregisterUIContext(final Long reloadedViewID) {
        uiContexts.remove(reloadedViewID);
    }

    public boolean unregisterUIContext(final UIContext uiContext) {
        boolean removed = false;
        final Iterator<Entry<Long, UIContext>> iterator = uiContexts.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<Long, UIContext> entry = iterator.next();
            if (entry.getValue().equals(uiContext)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    public UIContext getUIContext(final long key) {
        return uiContexts.get(key);
    }

    public Collection<UIContext> getUIContexts() {
        return uiContexts.values();
    }

    public Session getSession() {
        return session;
    }

    public void setAttribute(final String name, final Object value) {
        session.setAttribute(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) session.getAttribute(name);
    }

}