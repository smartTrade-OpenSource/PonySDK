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
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.core.Parser;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class PCookies {

    private static final int ID = 0; // reserved

    private final Map<String, String> cachedCookies = new ConcurrentHashMap<>();

    public PCookies() {
    }

    public void cacheCookie(final String name, final String value) {
        cachedCookies.put(name, value);
    }

    public String getCookie(final String name) {
        return cachedCookies.get(name);
    }

    public String removeCookie(final String name) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        parser.parse(ServerToClientModel.TYPE_UPDATE, ID);
        parser.parse(ServerToClientModel.REMOVE_COOKIE, name);
        parser.endObject();

        return cachedCookies.remove(name);
    }

    public void setCookie(final String name, final String value) {
        setCookie(name, value, null);
    }

    public void setCookie(final String name, final String value, final Date expires) {
        cachedCookies.put(name, value);

        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        parser.parse(ServerToClientModel.TYPE_UPDATE, ID);
        parser.parse(ServerToClientModel.ADD_COOKIE, name);
        parser.parse(ServerToClientModel.VALUE, value);
        if (expires != null) {
            parser.parse(ServerToClientModel.COOKIE_EXPIRE, expires.getTime());
        }
        parser.endObject();
    }

}
