/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

/**
 * Shows in a panel the view that matches the history token: at the start (a deep link, a reload), when the user goes
 * back or forward, and when the application calls {@link #navigate(String)}. A view is a class annotated
 * {@link Route}; the router builds a new instance each time it enters it, and destroys its widget when it leaves it.
 * <p>
 * A router is built with {@link #builder(PSimplePanel)}, on the UI thread of a context, and stays as long as its panel:
 * it stops with the panel's destroy. It only reads and writes the tokens under its base; the tokens of the rest of the
 * application are left alone.
 */
public final class Router {

    private static final int MAX_REDIRECTIONS = 8;

    private final PSimplePanel container;
    private final PHistory history;
    private final String base;
    private final List<Entry> entries;
    private final Map<Class<?>, Entry> byView = new HashMap<>();
    private final ViewFactory notFound;
    private final ViewFactory denied;
    private final Map<Class<?>, Predicate<UIContext>> access;
    private final BeforeEnter beforeEnter;
    private final PValueChangeHandler<String> listener = event -> go(event.getData(), 0, true);

    private Shown shown;
    private Navigation.Postponed postponed;
    private boolean closed;

    private record Entry(Class<?> view, Template template, ViewFactory factory) {
    }

    private record Shown(String token, IsPWidget view, PWidget widget) {
    }

    private Router(final Builder builder) {
        container = builder.container;
        history = UIContext.get().getHistory();
        base = builder.base;
        notFound = new ViewFactory(builder.notFound);
        denied = new ViewFactory(builder.denied);
        access = Map.copyOf(builder.access);
        beforeEnter = builder.beforeEnter;
        entries = new ArrayList<>();
        final Set<String> shapes = new HashSet<>();
        for (final Class<?> view : builder.views) {
            final Route route = view.getAnnotation(Route.class);
            if (route == null) throw new IllegalArgumentException(view.getName() + " has no @Route");
            final Template template = Template.parse(route.value());
            if (!shapes.add(template.shape())) {
                throw new IllegalArgumentException(view.getName() + " has the path of another view: " + route.value());
            }
            final Entry entry = new Entry(view, template, new ViewFactory(view));
            entries.add(entry);
            byView.put(view, entry);
        }
        for (final Class<?> view : access.keySet()) {
            if (!byView.containsKey(view)) throw new IllegalArgumentException("Access to " + view.getName() + ", not a view of this router");
        }
        entries.sort(Comparator.comparing(entry -> entry.template.key())); // the most literal template matches first
    }

    /** A router that fills {@code container}. */
    public static Builder builder(final PSimplePanel container) {
        return new Builder(container);
    }

    /**
     * Goes to a view with its parameters: those the path names fill it, the others go in the query
     * ({@code orders/42?status=open}); a null value leaves its parameter out.
     *
     * @throws IllegalArgumentException when the view is not one of the router's, or a path parameter has no value
     */
    public void navigate(final Class<?> view, final Map<String, ?> parameters) {
        final Entry entry = byView.get(view);
        if (entry == null) throw new IllegalArgumentException(view.getName() + " is not a view of this router");
        final Set<String> used = new HashSet<>();
        final StringBuilder relative = new StringBuilder(entry.template.expand(parameters, used));
        char separator = '?';
        for (final Map.Entry<String, ?> parameter : new TreeMap<String, Object>(parameters).entrySet()) {
            if (used.contains(parameter.getKey()) || parameter.getValue() == null) continue;
            relative.append(separator).append(Tokens.encode(parameter.getKey())).append('=')
                .append(Tokens.encode(parameter.getValue().toString()));
            separator = '&';
        }
        go(token(relative.toString()), 0, true);
    }

    public void navigate(final Class<?> view) {
        navigate(view, Map.of());
    }

    /** Goes to a path of the router, with its query if it has one: {@code orders/42?status=open}. */
    public void navigate(final String path) {
        go(token(path), 0, true);
    }

    private void onClose() {
        closed = true;
        postponed = null;
        shown = null;
        history.removeValueChangeHandler(listener);
    }

    void resume(final Navigation.Postponed navigation) {
        if (navigation == postponed) go(navigation.token(), 0, false);
    }

    void cancel(final Navigation.Postponed navigation) {
        if (navigation == postponed) postponed = null;
    }

    private void start() {
        final String current = currentToken();
        go(local(current) == null ? base : current, 0, false);
        // listening once a view is shown: a router whose first view fails to build is not left on the history
        history.addValueChangeHandler(listener);
        container.addDestroyListener(object -> onClose());
    }

    /**
     * Shows what the token designates.
     *
     * @param leave whether the view shown is asked first; not when it already agreed
     */
    private void go(final String token, final int redirections, final boolean leave) {
        if (closed) return; // a handler of the same event can have destroyed the panel
        final String local = local(token);
        if (local == null) return; // another part of the application's token
        postponed = null; // a navigation replaces the one that waited
        if (shown != null && shown.token.equals(token)) return;

        final String[] parts = local.split("\\?", 2);
        final String[] path = Tokens.segments(parts[0]);
        final Map<String, String> query = Tokens.query(parts.length > 1 ? parts[1] : "");
        Entry matched = null;
        Map<String, String> values = Map.of();
        for (final Entry entry : entries) {
            final Map<String, String> candidate = entry.template.match(path);
            if (candidate != null) {
                matched = entry;
                values = candidate;
                break;
            }
        }
        final RouteParameters parameters = new RouteParameters(token, values, query);

        if (leave && shown.view instanceof BeforeLeave guard) { // a started router always shows a view
            final Navigation navigation = new Navigation(this, false, token, matched == null ? null : matched.view, parameters);
            guard.beforeLeave(navigation);
            if (navigation.postponed != null) {
                postponed = navigation.postponed;
                history.newItem(shown.token, false); // the browser moved on a back or a forward: bring it back
                return;
            }
        }

        if (matched == null) {
            show(token, notFound, parameters);
            return;
        }
        final Navigation navigation = new Navigation(this, true, token, matched.view, parameters);
        if (beforeEnter != null) beforeEnter.beforeEnter(navigation);
        if (navigation.redirect != null) {
            if (redirections >= MAX_REDIRECTIONS) {
                throw new IllegalStateException("More than " + MAX_REDIRECTIONS + " redirections, from " + token);
            }
            go(token(navigation.redirect), redirections + 1, false);
            return;
        }
        final Predicate<UIContext> allowed = access.get(matched.view);
        if (navigation.denied || allowed != null && !allowed.test(UIContext.get())) show(token, denied, parameters);
        else show(token, matched.factory, parameters);
    }

    private void show(final String token, final ViewFactory factory, final RouteParameters parameters) {
        final IsPWidget view = factory.create(parameters);
        final PWidget widget = view.asWidget();
        final Shown previous = shown;
        shown = new Shown(token, view, widget);
        container.setWidget(widget);
        if (previous != null) previous.widget.onDestroy(); // its schedulers and listeners stop with it
        if (!token.equals(currentToken())) history.newItem(token, false);
    }

    private String currentToken() {
        final String token = history.getToken();
        return token == null ? "" : token;
    }

    /** The token without the base; null when the token is not under it. */
    private String local(final String token) {
        if (base.isEmpty()) return token;
        if (!token.startsWith(base)) return null;
        final String rest = token.substring(base.length());
        return rest.isEmpty() || rest.charAt(0) == '/' || rest.charAt(0) == '?' ? rest : null;
    }

    /** The token of a path of the router. */
    private String token(final String relative) {
        final String[] parts = relative.split("\\?", 2);
        final String path = Tokens.normalize(parts[0]);
        final String query = parts.length > 1 ? "?" + parts[1] : "";
        if (base.isEmpty()) return path + query;
        return (path.isEmpty() ? base : base + "/" + path) + query;
    }

    public static final class Builder {

        private final PSimplePanel container;
        private final List<Class<?>> views = new ArrayList<>();
        private final Map<Class<?>, Predicate<UIContext>> access = new HashMap<>();
        private String base = "";
        private Class<?> notFound = DefaultViews.NotFound.class;
        private Class<?> denied = DefaultViews.Denied.class;
        private BeforeEnter beforeEnter;

        private Builder(final PSimplePanel container) {
            this.container = container;
        }

        /**
         * The router's part of the token: with {@code routing} the path {@code orders/42} is the token
         * {@code routing/orders/42}. A router started on a token outside its base goes to its root. Without a base,
         * the router owns the whole token.
         */
        public Builder base(final String base) {
            this.base = Tokens.normalize(base);
            return this;
        }

        /** The views of the router, each with its {@link Route}. */
        public Builder views(final Class<?>... views) {
            this.views.addAll(List.of(views));
            return this;
        }

        /** The view for a path that matches no route; it is built like the others, its parameters carry the token. */
        public Builder notFound(final Class<?> view) {
            this.notFound = view;
            return this;
        }

        /** The view for a navigation denied, by {@link #access} or by {@link Navigation#deny()}. */
        public Builder denied(final Class<?> view) {
            this.denied = view;
            return this;
        }

        /**
         * Lets a view be entered when the predicate is true; it is asked with the context of the router, after the
         * {@link BeforeEnter} hook. A view without a predicate is open.
         */
        public Builder access(final Class<?> view, final Predicate<UIContext> allowed) {
            access.put(view, allowed);
            return this;
        }

        public Builder beforeEnter(final BeforeEnter hook) {
            this.beforeEnter = hook;
            return this;
        }

        /**
         * Builds the router and shows what the current token designates.
         *
         * @throws IllegalArgumentException when a view cannot be one, or two views have the same path
         */
        public Router start() {
            final Router router = new Router(this);
            router.start();
            return router;
        }
    }
}
