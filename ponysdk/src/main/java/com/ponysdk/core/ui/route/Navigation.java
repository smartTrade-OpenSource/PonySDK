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

/**
 * A navigation the {@link Router} is about to make, handed to a {@link BeforeEnter} hook and to a view's
 * {@link BeforeLeave}. What a hook can do depends on which one it is: {@link #redirect(String)} and {@link #deny()}
 * before entering, {@link #postpone()} before leaving; the other calls throw.
 */
public final class Navigation {

    private final Router router;
    private final boolean entering;
    private final String token;
    private final Class<?> view;
    private final RouteParameters parameters;

    String redirect;
    boolean denied;
    Postponed postponed;

    Navigation(final Router router, final boolean entering, final String token, final Class<?> view,
            final RouteParameters parameters) {
        this.router = router;
        this.entering = entering;
        this.token = token;
        this.view = view;
        this.parameters = parameters;
    }

    /** The history token the navigation goes to. */
    public String token() {
        return token;
    }

    /** The view class the navigation goes to; null when no route matches (the router's not-found view is shown). */
    public Class<?> view() {
        return view;
    }

    public RouteParameters parameters() {
        return parameters;
    }

    /**
     * Goes to another path of the router instead, as {@link Router#navigate(String)} would; the redirection goes
     * through the {@link BeforeEnter} hook again. Before entering only.
     */
    public void redirect(final String path) {
        requireEntering("redirect");
        redirect = path;
    }

    /** Shows the router's denied view in place of the view. Before entering only. */
    public void deny() {
        requireEntering("deny");
        denied = true;
    }

    /**
     * Holds the navigation: the current view stays, and the history token goes back to the view's if the browser had
     * moved it. {@link Postponed#resume()} goes on, {@link Postponed#cancel()} gives it up. Before leaving only;
     * calling it again gives the same handle.
     */
    public Postponed postpone() {
        if (entering) throw new IllegalStateException("postpone is for a view's beforeLeave");
        if (postponed == null) postponed = new Postponed(router, token);
        return postponed;
    }

    private void requireEntering(final String call) {
        if (!entering) throw new IllegalStateException(call + " is for a BeforeEnter hook");
    }

    /**
     * A postponed navigation. It is called from a later event of the UI thread (the answer to a confirmation), once
     * the hook has returned; another navigation in the meantime replaces it, and then both calls do nothing.
     */
    public static final class Postponed {

        private final Router router;
        private final String token;

        Postponed(final Router router, final String token) {
            this.router = router;
            this.token = token;
        }

        String token() {
            return token;
        }

        /** Leaves the view without asking it again, and goes where the navigation was going. */
        public void resume() {
            router.resume(this);
        }

        /** Stays on the view. */
        public void cancel() {
            router.cancel(this);
        }
    }
}
