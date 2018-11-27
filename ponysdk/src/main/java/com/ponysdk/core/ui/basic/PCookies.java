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

package com.ponysdk.core.ui.basic;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.writer.ModelWriter;

public class PCookies {

    private static final int ID = 0; // reserved

    private final Map<String, String> cachedCookies = new HashMap<>(4);

    private boolean isInitialized = false;

    public interface CookiesListener {

        void onInitialized();
    }

    private CookiesListener listener;

    public String getCookie(final String name) {
        return cachedCookies.get(name);
    }

    public String removeCookie(final String name) {
        return removeCookie(name, null);
    }

    public String removeCookie(final String name, final String path) {
        final ModelWriter writer = Txn.get().getWriter();
        writer.beginObject(PWindow.getMain().getID());
        writer.write(ServerToClientModel.TYPE_UPDATE, ID);
        writer.write(ServerToClientModel.REMOVE_COOKIE, name);
        if (path != null) writer.write(ServerToClientModel.COOKIE_PATH, path);
        writer.endObject();

        return cachedCookies.remove(name);
    }

    public void setCookie(final String name, final String value) {
        setCookie(name, value, null, null);
    }

    public void setCookie(final String name, final String value, final String path) {
        setCookie(name, value, null, path);
    }

    public void setCookie(final String name, final String value, final Date expires) {
        setCookie(name, value, expires, null);
    }

    public void setCookie(final String name, final String value, final Date expires, final String path) {
        setCookie(name, value, expires, path, null, false);
    }

    public void setCookie(final String name, final String value, final Date expires, final String domain, final String path,
                          final boolean secure) {
        cachedCookies.put(name, value);

        final ModelWriter writer = Txn.get().getWriter();
        writer.beginObject(PWindow.getMain().getID());
        writer.write(ServerToClientModel.TYPE_UPDATE, ID);
        writer.write(ServerToClientModel.ADD_COOKIE, name);
        writer.write(ServerToClientModel.VALUE, value);
        if (expires != null) writer.write(ServerToClientModel.COOKIE_EXPIRE, expires.getTime());
        if (domain != null) writer.write(ServerToClientModel.COOKIE_DOMAIN, domain);
        if (path != null) writer.write(ServerToClientModel.COOKIE_PATH, path);
        if (secure) writer.write(ServerToClientModel.COOKIE_SECURE, secure);
        writer.endObject();
    }

    public void onClientData(final JsonObject event) {
        final JsonArray cookies = event.getJsonArray(ClientToServerModel.COOKIES.toStringValue());

        for (int i = 0; i < cookies.size(); i++) {
            final JsonObject object = cookies.getJsonObject(i);

            final String key = object.getString(ClientToServerModel.COOKIE_NAME.toStringValue());
            final String value = object.getString(ClientToServerModel.COOKIE_VALUE.toStringValue());

            cachedCookies.put(key, value);
        }

        if (!isInitialized) {
            isInitialized = true;
            if (listener != null) listener.onInitialized();
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setListener(final CookiesListener listener) {
        this.listener = listener;
    }

    public Collection<String> getNames() {
        return cachedCookies.keySet();
    }
}
