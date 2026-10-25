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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.writer.ModelWriter;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The cookies of the page. In a {@code <pony-app>} element of another page, those of the application's origin
 * (docs/decisions/0009-embedding-limits.md, decision 7): read from the WebSocket upgrade, the session cookie aside, and
 * written by a request the terminal makes to the application's origin, answered with {@code Set-Cookie}. From a page of
 * another site, the browser's rules for third-party cookies apply.
 */
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
        final UIContext uiContext = UIContext.get();
        if (uiContext.isEmbedded()) {
            final Cookie cookie = cookie(name, "", path);
            cookie.setMaxAge(0);
            uiContext.stackCookieRequest(setting(cookie));
            return cachedCookies.remove(name);
        }
        final ModelWriter writer = uiContext.getWriter();
        writer.beginObject(PWindow.getMain());
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
        setCookie(name, value, expires, path, null, false, null);
    }

    public void setCookie(final String name, final String value, final Date expires, final String domain, final String path,
                          final boolean secure, String sameSite) {
        cachedCookies.put(name, value);

        final UIContext uiContext = UIContext.get();
        if (uiContext.isEmbedded()) {
            final Cookie cookie = cookie(name, value, path);
            if (expires != null) cookie.setMaxAge((int) Math.max(0, (expires.getTime() - System.currentTimeMillis()) / 1000));
            if (domain != null) cookie.setDomain(domain);
            cookie.setSecure(secure);
            if (sameSite != null) cookie.setAttribute("SameSite", sameSite);
            uiContext.stackCookieRequest(setting(cookie));
            return;
        }
        final ModelWriter writer = uiContext.getWriter();
        writer.beginObject(PWindow.getMain());
        writer.write(ServerToClientModel.TYPE_UPDATE, ID);
        writer.write(ServerToClientModel.ADD_COOKIE, name);
        writer.write(ServerToClientModel.VALUE, value);
        if (expires != null) writer.write(ServerToClientModel.COOKIE_EXPIRE, expires.getTime());
        if (domain != null) writer.write(ServerToClientModel.COOKIE_DOMAIN, domain);
        if (path != null) writer.write(ServerToClientModel.COOKIE_PATH, path);
        if (secure) writer.write(ServerToClientModel.COOKIE_SECURE, true);
        if (sameSite != null) writer.write(ServerToClientModel.COOKIE_SAMESITE, sameSite);
        writer.endObject();
    }

    public void onClientData(final JsonObject event) {
        final JsonArray cookies = event.getJsonArray(ClientToServerModel.COOKIES.toStringValue());
        final UIContext uiContext = UIContext.get();
        // An embedded terminal sends none: document.cookie is the host page's
        if (uiContext != null && uiContext.isEmbedded()) {
            final String session = sessionCookieName(uiContext.getSession());
            for (final java.net.HttpCookie cookie : uiContext.getRequestCookies()) {
                if (!cookie.getName().equals(session)) cachedCookies.put(decode(cookie.getName()), decode(cookie.getValue()));
            }
        }

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

    /** As the terminal writes them in a page: name and value URI-encoded. */
    private static Cookie cookie(final String name, final String value, final String path) {
        final Cookie cookie = new Cookie(encode(name), encode(value));
        if (path != null) cookie.setPath(path);
        return cookie;
    }

    /** What the terminal's request gets: the cookie, and nothing else. */
    private static com.ponysdk.core.ui.eventbus.StreamHandler setting(final Cookie cookie) {
        return (request, response, uiContext) -> {
            response.addCookie(cookie);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        };
    }

    private static String sessionCookieName(final HttpSession session) {
        final String name = session != null ? session.getServletContext().getSessionCookieConfig().getName() : null;
        return name != null ? name : "JSESSIONID";
    }

    /** As encodeURIComponent: a space is %20, not +. */
    private static String encode(final String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** As decodeURIComponent: a + stays. */
    private static String decode(final String text) {
        try {
            return URLDecoder.decode(text.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException e) {
            return text;
        }
    }
}
