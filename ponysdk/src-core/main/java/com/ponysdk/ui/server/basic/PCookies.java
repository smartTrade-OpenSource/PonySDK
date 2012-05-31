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

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.instruction.Create;
import com.ponysdk.core.instruction.Remove;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

public class PCookies {

    private final Map<String, String> cachedCookies = new ConcurrentHashMap<String, String>();

    public PCookies(final Map<String, String> cookies) throws Exception {
        for (final Entry<String, String> entry : cookies.entrySet()) {
            cachedCookies.put(entry.getKey(), entry.getValue());
        }
    }

    public String getCookie(final String name) {
        return cachedCookies.get(name);
    }

    public String removeCookie(final String name) {
        final Remove remove = new Remove();
        remove.put(PROPERTY.COOKIE, name);
        PonySession.getCurrent().stackInstruction(remove);
        return cachedCookies.remove(name);
    }

    public void setCookie(final String name, final String value) {
        setCookie(name, value, null);
    }

    public void setCookie(final String name, final String value, final Date expires) {
        try {
            cachedCookies.put(name, value);

            final long ID = PonySession.getCurrent().nextID();
            final Create create = new Create(ID, WidgetType.COOKIE);
            create.put(PROPERTY.NAME, name);
            create.put(PROPERTY.VALUE, value);
            if (expires != null) {
                create.put(PROPERTY.COOKIE_EXPIRE, expires.getTime());
            }

            PonySession.getCurrent().stackInstruction(create);
        } catch (final Exception e) {
            throw new RuntimeException("encoding failure", e);
        }
    }

}
