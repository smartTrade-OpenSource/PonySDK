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
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.eventbus.StreamHandler;
import com.ponysdk.core.writer.ModelWriter;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The cookies of the page. In a {@code <pony-app>} element of another page, those of the application's origin
 * (docs/decisions/0009-embedding-limits.md, decision 7): read from the WebSocket upgrade, the session cookie aside, and
 * written by a request the terminal makes to the application's origin, answered with {@code Set-Cookie}. From a page of
 * another site, the browser's rules for third-party cookies apply.
 */
public class PCookies {

    private static final int ID = 0; // reserved

    /** The SameSite attribute of a cookie. */
    public enum SameSite {
        STRICT("Strict"), LAX("Lax"), NONE("None");

        private final String value;

        SameSite(final String value) {
            this.value = value;
        }

        /** As written in the cookie: {@code Strict}, {@code Lax}, {@code None}. */
        public String value() {
            return value;
        }
    }

    /**
     * The attributes of a cookie, each named: {@code CookieOptions.path("/").domain("example.com").secure()}. Immutable:
     * each method returns a new one.
     */
    public static final class CookieOptions {

        /** No attribute: a session cookie of the page's path. */
        public static final CookieOptions NONE = new CookieOptions(null, null, null, false, null);

        private final Instant expires;
        private final String path;
        private final String domain;
        private final boolean secure;
        private final SameSite sameSite;

        private CookieOptions(final Instant expires, final String path, final String domain, final boolean secure,
                              final SameSite sameSite) {
            this.expires = expires;
            this.path = path;
            this.domain = domain;
            this.secure = secure;
            this.sameSite = sameSite;
        }

        public static CookieOptions expires(final Instant expires) {
            return NONE.withExpires(expires);
        }

        public static CookieOptions path(final String path) {
            return NONE.withPath(path);
        }

        public static CookieOptions domain(final String domain) {
            return NONE.withDomain(domain);
        }

        public CookieOptions withExpires(final Instant expires) {
            return new CookieOptions(expires, path, domain, secure, sameSite);
        }

        public CookieOptions withPath(final String path) {
            return new CookieOptions(expires, path, domain, secure, sameSite);
        }

        public CookieOptions withDomain(final String domain) {
            return new CookieOptions(expires, path, domain, secure, sameSite);
        }

        /** Sent over HTTPS only. */
        public CookieOptions secure() {
            return new CookieOptions(expires, path, domain, true, sameSite);
        }

        public CookieOptions sameSite(final SameSite sameSite) {
            return new CookieOptions(expires, path, domain, secure, sameSite);
        }
    }

    private final Map<String, String> cachedCookies = new HashMap<>(4);
    /** How an embedded application's cookie is written: a request the terminal makes, given by UIContext. */
    private final Consumer<StreamHandler> cookieRequests;
    private final List<Runnable> initializedListeners = new ArrayList<>(1);

    private boolean isInitialized = false;

    /**
     * Created by {@link UIContext}, which gives the channel of an embedded application's cookie requests: an application
     * gets its cookies from {@link UIContext#getCookies()}.
     */
    public PCookies(final Consumer<StreamHandler> cookieRequests) {
        this.cookieRequests = Objects.requireNonNull(cookieRequests, "cookieRequests");
    }

    /** The cookie's value, null when the browser has none of that name or the cookies are not known yet. */
    public String getCookie(final String name) {
        return cachedCookies.get(name);
    }

    /** Removes the cookie of that name, without a path or a domain. @return its last value, null for none */
    public String removeCookie(final String name) {
        return removeCookie(name, CookieOptions.NONE);
    }

    /** Removes the cookie of that name and path. @return its last value, null for none */
    public String removeCookie(final String name, final String path) {
        return removeCookie(name, CookieOptions.path(path));
    }

    /**
     * Removes the cookie of that name with the path and the domain of the options (the others are ignored): a cookie
     * set with a domain is removed only with it.
     *
     * @return its last value, null for none
     */
    public String removeCookie(final String name, final CookieOptions options) {
        final UIContext uiContext = UIContext.get();
        if (uiContext.isEmbedded()) {
            final Cookie cookie = cookie(name, "", options.path);
            if (options.domain != null) cookie.setDomain(options.domain);
            cookie.setMaxAge(0);
            cookieRequests.accept(setting(cookie));
            return cachedCookies.remove(name);
        }
        final ModelWriter writer = uiContext.getWriter();
        writer.beginObject(PWindow.getMain());
        writer.write(ServerToClientModel.TYPE_UPDATE, ID);
        writer.write(ServerToClientModel.REMOVE_COOKIE, name);
        // the order of ADD_COOKIE: the domain, then the path
        if (options.domain != null) writer.write(ServerToClientModel.COOKIE_DOMAIN, options.domain);
        if (options.path != null) writer.write(ServerToClientModel.COOKIE_PATH, options.path);
        writer.endObject();

        return cachedCookies.remove(name);
    }

    /** Sets a session cookie, for the page's path. */
    public void setCookie(final String name, final String value) {
        setCookie(name, value, CookieOptions.NONE);
    }

    /** Sets a session cookie for that path. */
    public void setCookie(final String name, final String value, final String path) {
        setCookie(name, value, CookieOptions.path(path));
    }

    /** Sets a cookie that expires then. */
    public void setCookie(final String name, final String value, final Date expires) {
        setCookie(name, value, CookieOptions.expires(expires == null ? null : expires.toInstant()));
    }

    /** Sets a cookie that expires then, for that path. */
    public void setCookie(final String name, final String value, final Date expires, final String path) {
        setCookie(name, value, CookieOptions.path(path).withExpires(expires == null ? null : expires.toInstant()));
    }

    /** Sets a cookie with these attributes: {@code setCookie("theme", "dark", CookieOptions.path("/").secure())}. */
    public void setCookie(final String name, final String value, final CookieOptions options) {
        cachedCookies.put(name, value);

        final UIContext uiContext = UIContext.get();
        if (uiContext.isEmbedded()) {
            final Cookie cookie = cookie(name, value, options.path);
            if (options.expires != null) cookie.setMaxAge((int) Math.max(0, (options.expires.toEpochMilli() - System.currentTimeMillis()) / 1000));
            if (options.domain != null) cookie.setDomain(options.domain);
            cookie.setSecure(options.secure);
            if (options.sameSite != null) cookie.setAttribute("SameSite", options.sameSite.value());
            cookieRequests.accept(setting(cookie));
            return;
        }
        final ModelWriter writer = uiContext.getWriter();
        writer.beginObject(PWindow.getMain());
        writer.write(ServerToClientModel.TYPE_UPDATE, ID);
        writer.write(ServerToClientModel.ADD_COOKIE, name);
        writer.write(ServerToClientModel.VALUE, value);
        if (options.expires != null) writer.write(ServerToClientModel.COOKIE_EXPIRE, options.expires.toEpochMilli());
        if (options.domain != null) writer.write(ServerToClientModel.COOKIE_DOMAIN, options.domain);
        if (options.path != null) writer.write(ServerToClientModel.COOKIE_PATH, options.path);
        if (options.secure) writer.write(ServerToClientModel.COOKIE_SECURE, true);
        if (options.sameSite != null) writer.write(ServerToClientModel.COOKIE_SAMESITE, options.sameSite.value());
        writer.endObject();
    }

    /** The browser's cookies, sent by the terminal at its start: called by the framework. */
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
            for (final Runnable listener : List.copyOf(initializedListeners)) listener.run();
        }
    }

    /** True once the browser's cookies are known. */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Called once the cookies are known; at once when they already are.
     *
     * @return removes the listener
     */
    public HandlerRegistration addInitializedListener(final Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        if (isInitialized) {
            listener.run();
            return () -> {
            };
        }
        initializedListeners.add(listener);
        return () -> initializedListeners.remove(listener);
    }

    /** The names of the cookies known, read-only. */
    public Collection<String> getNames() {
        return Collections.unmodifiableSet(cachedCookies.keySet());
    }

    /** As the terminal writes them in a page: name and value URI-encoded. */
    private static Cookie cookie(final String name, final String value, final String path) {
        final Cookie cookie = new Cookie(encode(name), encode(value));
        if (path != null) cookie.setPath(path);
        return cookie;
    }

    /** What the terminal's request gets: the cookie, Secure over HTTPS (it only goes back to that origin), and nothing else. */
    private static StreamHandler setting(final Cookie cookie) {
        return (request, response, uiContext) -> {
            if (request.isSecure()) cookie.setSecure(true);
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
