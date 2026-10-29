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

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.route.BeforeLeave;
import com.ponysdk.core.ui.route.Navigation;
import com.ponysdk.core.ui.route.Route;
import com.ponysdk.core.ui.route.RouteParameters;
import com.ponysdk.core.ui.route.Router;

/**
 * Routing (docs/decisions/0020-routing.md): a small application inside this page, whose screens are classes annotated
 * {@link Route}. Its tokens sit under the page's own ({@code #routing/orders/42}): the showcase keeps the first
 * segment, the router the rest.
 */
final class RoutingPage {

    /** The page's token, and the router's base. */
    static final String TOKEN = "routing";
    /** The UIContext attribute that holds the roles of the user: the application's, the router never looks at it. */
    private static final String ROLES = "roles";

    private record Order(int id, String customer, String status, int quantity) {
    }

    private static final List<Order> ORDERS = List.of(new Order(1, "Acme Capital", "open", 250),
        new Order(2, "Borealis Fund", "shipped", 40), new Order(3, "Cobalt Bank", "open", 1_200),
        new Order(4, "Delta Treasury", "shipped", 75), new Order(5, "Equinox Partners", "open", 18));

    private RoutingPage() {
    }

    static PFlowPanel build(final UIContext uiContext) {
        final PFlowPanel p = section();
        pageTitle(p, "Routing", "The screens of a small application are classes annotated @Route; a router shows the one "
                + "that matches the history token: at the start (reload this page on an order), on back and forward, "
                + "and when a screen asks for another. Watch the address bar.");

        uiContext.setAttribute(ROLES, Set.of("trader"));
        final PSimplePanel body = Element.newPSimplePanel();
        body.addStyleName("route-body");

        // region router
        final Router router = Router.builder(body)
            .base(TOKEN) // this page's part of the token: routing/orders/42; the showcase keeps the first segment
            .views(OrdersView.class, OrderView.class, EditView.class, AdminView.class)
            .notFound(NotFoundView.class)
            // the roles are the application's: here an attribute of the UIContext
            .access(AdminView.class, ui -> ui.<Set<String>> getAttribute(ROLES).contains("admin"))
            .start();
        // endregion

        final PFlowPanel nav = Element.newPFlowPanel();
        nav.addStyleName("route-nav");
        final PButton orders = Element.newPButton("Orders");
        orders.addClickHandler(e -> router.navigate(OrdersView.class));
        final PButton admin = Element.newPButton("Admin");
        admin.addClickHandler(e -> router.navigate(AdminView.class));
        // a plain link works as well: the browser changes the fragment, the router follows
        final PAnchor lost = Element.newPAnchor("A page that does not exist", "#" + TOKEN + "/nowhere");
        final PButton role = Element.newPButton("Role: trader");
        role.addStyleName("route-role");
        role.addClickHandler(e -> {
            final boolean granted = uiContext.<Set<String>> getAttribute(ROLES).contains("admin");
            uiContext.setAttribute(ROLES, granted ? Set.of("trader") : Set.of("trader", "admin"));
            role.setText(granted ? "Role: trader" : "Role: admin");
        });
        nav.add(orders, admin, lost, role);

        final PFlowPanel app = Element.newPFlowPanel();
        app.addStyleName("route-app");
        app.add(nav, body);
        p.add(app);
        p.add(codeToggle("the router", Source.code(RoutingPage.class, "router")));
        p.add(codeToggle("a list read from the query", Source.code(RoutingPage.class, "list")));
        p.add(codeToggle("a detail read from the path, destroyed when left", Source.code(RoutingPage.class, "detail")));
        p.add(codeToggle("a view that postpones the navigation", Source.code(RoutingPage.class, "guard")));
        return p;
    }

    private static PButton button(final String text, final Runnable onClick) {
        final PButton button = Element.newPButton(text);
        button.addClickHandler(e -> onClick.run());
        return button;
    }

    // region list
    /** The list, also the root of the page: the query {@code orders?status=open} filters it. */
    @Route({ "orders", "" })
    static final class OrdersView implements IsPWidget {

        private final PFlowPanel root = Element.newPFlowPanel();

        OrdersView(final RouteParameters parameters) {
            final Router router = parameters.router();
            final String status = parameters.get("status", "all");
            root.addStyleName("route-view");
            root.add(el("route-title", "Orders"));
            final PFlowPanel filters = Element.newPFlowPanel();
            filters.addStyleName("route-filters");
            for (final String filter : List.of("all", "open", "shipped")) {
                final PButton button = button(filter, () -> {
                    if (filter.equals("all")) router.navigate(OrdersView.class);
                    else router.navigate(OrdersView.class, Map.of("status", filter));
                });
                button.addStyleName("route-filter");
                if (filter.equals(status)) button.addStyleName("route-filter-on");
                filters.add(button);
            }
            root.add(filters);
            for (final Order order : ORDERS) {
                if (!status.equals("all") && !status.equals(order.status())) continue;
                final PFlowPanel row = Element.newPFlowPanel();
                row.addStyleName("route-row");
                row.add(el("route-cell", "#" + order.id() + "  " + order.customer()),
                    el("route-cell route-status", order.status()),
                    button("View", () -> router.navigate(OrderView.class, Map.of("id", order.id()))));
                root.add(row);
            }
        }

        @Override
        public PWidget asWidget() {
            return root;
        }
    }
    // endregion

    // region detail
    /** The detail: {@code :id} is a named parameter of the path. */
    @Route("orders/:id")
    static final class OrderView implements IsPWidget {

        private final PFlowPanel root = Element.newPFlowPanel();

        OrderView(final RouteParameters parameters) {
            final Router router = parameters.router();
            final int id = parameters.getInt("id", -1); // typed; "orders/abc" is the view's own "no such order"
            final Optional<Order> found = ORDERS.stream().filter(o -> o.id() == id).findFirst();
            root.addStyleName("route-view");
            if (found.isEmpty()) {
                root.add(el("route-title", "No order " + parameters.get("id")));
            } else {
                final Order order = found.get();
                root.add(el("route-title", "Order #" + order.id() + ", " + order.customer()),
                    el("route-note", order.quantity() + " lots, " + order.status()));
                final PElement refreshed = el("route-note", "Refreshed 0 times");
                root.add(refreshed);
                // A scheduler of the view: the router destroys the view's widget when it is left, which stops it
                final int[] count = { 0 };
                final PScheduler.UIRunnable refresh = PScheduler.scheduleAtFixedRate(() -> {
                    if (!idle()) refreshed.setInnerText("Refreshed " + (++count[0]) + " times");
                }, Duration.ofSeconds(1));
                root.addDestroyListener(e -> refresh.cancel());
                root.add(button("Edit", () -> router.navigate(EditView.class, Map.of("id", id))));
            }
            root.add(button("All orders", () -> router.navigate(OrdersView.class)));
        }

        @Override
        public PWidget asWidget() {
            return root;
        }
    }
    // endregion

    // region guard
    /** The edit form: leaving it with unsaved changes asks first. */
    @Route("orders/:id/edit")
    static final class EditView implements IsPWidget, BeforeLeave {

        private final PFlowPanel root = Element.newPFlowPanel();
        private final PFlowPanel confirm = Element.newPFlowPanel();
        private final PElement confirmText = el("route-note", "");
        private final PElement saved = el("route-note", "No change");
        private boolean dirty;
        private Navigation.Postponed waiting;

        EditView(final RouteParameters parameters) {
            final Router router = parameters.router();
            final int id = parameters.getInt("id");
            root.addStyleName("route-view");
            root.add(el("route-title", "Edit order #" + id));
            final PTextBox note = Element.newPTextBox();
            note.setPlaceholder("A note for this order");
            note.addValueChangeHandler(e -> {
                dirty = true;
                saved.setInnerText("Unsaved changes");
            });
            root.add(note, saved);
            root.add(button("Save", () -> {
                dirty = false;
                saved.setInnerText("Saved");
            }));
            root.add(button("Back to the order", () -> router.navigate(OrderView.class, Map.of("id", id))));

            confirm.addStyleName("route-confirm");
            confirm.setVisible(false);
            confirm.add(confirmText, button("Leave", () -> waiting.resume()), button("Stay", () -> {
                waiting.cancel();
                confirm.setVisible(false);
            }));
            root.add(confirm);
        }

        @Override
        public void beforeLeave(final Navigation navigation) {
            if (!dirty) return; // nothing to lose: the navigation goes on
            waiting = navigation.postpone(); // the view stays, the token is put back; "Leave" resumes, "Stay" cancels
            confirmText.setInnerText("Unsaved changes. Leave for " + navigation.token() + "?");
            confirm.setVisible(true);
        }

        @Override
        public PWidget asWidget() {
            return root;
        }
    }
    // endregion

    /** Only for the admin role: the router's access rule keeps the others out. */
    @Route("admin")
    static final class AdminView implements IsPWidget {

        private final PFlowPanel root = Element.newPFlowPanel();

        AdminView() {
            root.addStyleName("route-view");
            root.add(el("route-title", "Admin console"), el("route-note", "You have the admin role."));
        }

        @Override
        public PWidget asWidget() {
            return root;
        }
    }

    /** Shown for a path no route matches; its parameters carry the token. */
    static final class NotFoundView implements IsPWidget {

        private final PFlowPanel root = Element.newPFlowPanel();

        NotFoundView(final RouteParameters parameters) {
            root.addStyleName("route-view");
            root.add(el("route-title", "Page not found"), el("route-note", "Nothing at #" + parameters.token()),
                button("Orders", () -> parameters.router().navigate(OrdersView.class)));
        }

        @Override
        public PWidget asWidget() {
            return root;
        }
    }
}
