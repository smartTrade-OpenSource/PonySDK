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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PSuite;

public class RouterTest extends PSuite {

    /** What the views did, in order: "build orders", "destroy orders". */
    private static final List<String> LOG = new ArrayList<>();
    /** The edit view's form has unsaved changes. */
    private static boolean dirty;
    /** The handle the edit view got when it postponed a navigation. */
    private static Navigation.Postponed held;
    /** The navigation the edit view was asked about. */
    private static Navigation asked;

    abstract static class Page implements IsPWidget {

        final PLabel label = Element.newPLabel();

        Page(final String name, final String text) {
            label.setText(text);
            LOG.add("build " + name);
            label.addDestroyListener(object -> LOG.add("destroy " + name));
        }

        @Override
        public PWidget asWidget() {
            return label;
        }
    }

    @Route("")
    public static class Home extends Page {

        public Home() {
            super("home", "home");
        }
    }

    @Route("orders")
    static class Orders extends Page {

        Orders(final RouteParameters parameters) {
            super("orders", "orders status=" + parameters.get("status", "any") + " page=" + parameters.getInt("page", 1)
                    + " flag=" + parameters.getBoolean("flag", false) + " token=" + parameters.token());
        }
    }

    @Route("orders/:id")
    static class Order extends Page {

        Order(final RouteParameters parameters) {
            super("order", "order " + parameters.get("id") + "/" + parameters.getLong("id", -1));
        }
    }

    @Route("orders/new")
    static class NewOrder extends Page {

        NewOrder() {
            super("new", "new order");
        }
    }

    @Route("orders/:id/edit")
    static class Edit extends Page implements BeforeLeave {

        Edit(final RouteParameters parameters) {
            super("edit", "edit " + parameters.getInt("id"));
        }

        @Override
        public void beforeLeave(final Navigation navigation) {
            asked = navigation;
            if (dirty) held = navigation.postpone();
        }
    }

    @Route("admin")
    static class Admin extends Page {

        Admin() {
            super("admin", "admin");
        }
    }

    @Route("login")
    static class Login extends Page {

        Login() {
            super("login", "login");
        }
    }

    @Route(":section/:id")
    static class Section extends Page {

        Section(final RouteParameters parameters) {
            super("section", "section " + parameters.get("section") + " " + parameters.get("id"));
        }
    }

    @Route(":kind/new")
    static class AnyNew extends Page {

        AnyNew(final RouteParameters parameters) {
            super("anynew", "any new " + parameters.get("kind"));
        }
    }

    @Route("orders/:id/:what")
    static class OrderThing extends Page {

        OrderThing(final RouteParameters parameters) {
            super("thing", "thing " + parameters.get("what"));
        }
    }

    @Route("orders/:other")
    static class OrderOther extends Page {

        OrderOther() {
            super("other", "other");
        }
    }

    /** Postpones and resumes at once: from inside the hook, where it cannot work. */
    @Route("impatient")
    static class Impatient extends Page implements BeforeLeave {

        Impatient() {
            super("impatient", "impatient");
        }

        @Override
        public void beforeLeave(final Navigation navigation) {
            navigation.postpone().resume();
        }
    }

    @Route("broken")
    static class Broken extends Page {

        Broken() {
            super("broken", "broken");
            throw new IllegalStateException("no view today");
        }
    }

    /** A view without a route: for the application's not-found and denied views. */
    static class Missing extends Page {

        Missing(final RouteParameters parameters) {
            super("missing", "missing " + parameters.token());
        }
    }

    static class Forbidden extends Page {

        Forbidden(final RouteParameters parameters) {
            super("forbidden", "forbidden " + parameters.token() + " id=" + parameters.get("id"));
        }
    }

    private final PHistory history = UIContext.get().getHistory();
    private PSimplePanel container;

    @BeforeEach
    public void freshPanel() {
        LOG.clear();
        dirty = false;
        held = null;
        asked = null;
        history.newItem("", false);
        container = Element.newPSimplePanel();
    }

    @AfterEach
    public void closeRouter() {
        container.onDestroy(); // stops the router, which stops listening to the history
        Mockito.doReturn(history).when(UIContext.get()).getHistory();
    }

    private Router.Builder app() {
        return Router.builder(container).views(Home.class, Orders.class, Order.class, NewOrder.class, Edit.class,
            Admin.class, Login.class);
    }

    private String text() {
        return ((PLabel) container.getWidget()).getText();
    }

    private static List<String> log() {
        return new ArrayList<>(LOG);
    }

    /** A history the router reads, to see what it writes to it. */
    private PHistory spiedHistory() {
        final PHistory spied = Mockito.spy(new PHistory());
        Mockito.doReturn(spied).when(UIContext.get()).getHistory();
        return spied;
    }

    // ── matching ──

    @Test
    public void theRootPathShowsItsView() {
        app().start();
        assertEquals("home", text());
        assertEquals("", history.getToken());
    }

    @Test
    public void aPathParameterIsReadByName() {
        history.newItem("orders/42", false);
        app().start();
        assertEquals("order 42/42", text());
    }

    @Test
    public void aLiteralBeatsAParameterWhateverTheOrderOfTheViews() {
        history.newItem("orders/new", false);
        Router.builder(container).views(Order.class, NewOrder.class).start();
        assertEquals("new order", text());
    }

    @Test
    public void theLeftmostLiteralWins() {
        history.newItem("orders/new", false);
        Router.builder(container).views(AnyNew.class, Order.class).start();
        assertEquals("order new/-1", text());
        history.fireHistoryChanged("users/new");
        assertEquals("any new users", text());
    }

    @Test
    public void aLiteralInTheLastSegmentBeatsAParameter() {
        history.newItem("orders/42/edit", false);
        Router.builder(container).views(OrderThing.class, Edit.class).start();
        assertEquals("edit 42", text());
        history.fireHistoryChanged("orders/42/print");
        assertEquals("thing print", text());
    }

    @Test
    public void severalParametersAreReadByName() {
        history.newItem("users/9", false);
        Router.builder(container).views(Section.class).start();
        assertEquals("section users 9", text());
    }

    @Test
    public void aPathNeedsTheSameNumberOfSegments() {
        history.newItem("orders/42/edit", false);
        app().start();
        assertEquals("edit 42", text());
        history.fireHistoryChanged("orders/42/edit/more");
        assertEquals("Not found: orders/42/edit/more", text());
        history.fireHistoryChanged("orders/42/other");
        assertEquals("Not found: orders/42/other", text());
    }

    @Test
    public void aLiteralThatDiffersDoesNotMatch() {
        history.newItem("order/42", false);
        app().start();
        assertEquals("Not found: order/42", text());
    }

    @Test
    public void emptySegmentsAreIgnored() {
        history.newItem("/orders//42/", false);
        app().start();
        assertEquals("order 42/42", text());
    }

    @Test
    public void aSegmentIsPercentDecodedAndAPlusStaysAPlus() {
        history.newItem("orders/a%20b+c", false);
        app().start();
        assertEquals("order a b+c/-1", text());
        history.fireHistoryChanged("orders/50%");
        assertEquals("order 50%/-1", text(), "a malformed escape is kept as written");
    }

    // ── query ──

    @Test
    public void theQueryIsReadTyped() {
        history.newItem("orders?status=open&page=3&flag=TRUE", false);
        app().start();
        assertEquals("orders status=open page=3 flag=true token=orders?status=open&page=3&flag=TRUE", text());
    }

    @Test
    public void aMissingOrMalformedQueryParameterGivesTheFallback() {
        history.newItem("orders?page=many&flag=maybe", false);
        app().start();
        assertEquals("orders status=any page=1 flag=false token=orders?page=many&flag=maybe", text());
    }

    @Test
    public void theQueryIsDecodedAndTheFirstValueOfANameWins() {
        final String token = "orders?status=to%20ship&status=late&&page&=x&flag=false&bad=%zz";
        history.newItem(token, false);
        app().start();
        assertEquals("orders status=to ship page=1 flag=false token=" + token, text());
    }

    @Test
    public void aPathParameterHidesTheQueryParameterOfTheSameName() {
        history.newItem("orders/42?id=7&page=2", false);
        Router.builder(container).views(Order.class).start();
        assertEquals("order 42/42", text());
    }

    // ── the token at the start: a deep link, a reload ──

    @Test
    public void aTokenThatSurvivesAReloadShowsTheSameView() {
        app().start();
        container.onDestroy();
        // the page is loaded again with its token: the server replays it before the entry point starts
        history.newItem("orders/42/edit", false);
        container = Element.newPSimplePanel();
        app().start();
        assertEquals("edit 42", text());
        assertEquals("orders/42/edit", history.getToken());
    }

    @Test
    public void startingWritesNothingWhenTheTokenIsTheViews() {
        final PHistory spied = spiedHistory();
        spied.fireHistoryChanged("orders/42"); // what the server replays before the entry point
        app().start();
        assertEquals("order 42/42", text());
        Mockito.verify(spied, Mockito.never()).newItem(Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void noTokenAtAllIsTheRootAndWritesNothing() {
        final PHistory spied = spiedHistory();
        assertNull(spied.getToken());
        app().start();
        assertEquals("home", text());
        Mockito.verify(spied, Mockito.never()).newItem(Mockito.anyString(), Mockito.anyBoolean());
    }

    // ── navigate ──

    @Test
    public void aNavigationWritesItsTokenOnceAndNeverFiresIt() {
        final PHistory spied = spiedHistory();
        final Router router = app().start();
        router.navigate("orders/42");
        Mockito.verify(spied).newItem("orders/42", false);
        Mockito.verify(spied, Mockito.times(1)).newItem(Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void navigateByViewFillsThePathAndPutsTheRestInTheQuery() {
        final Router router = app().start();
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 42);
        parameters.put("status", "to ship");
        parameters.put("page", 2);
        parameters.put("absent", null);
        router.navigate(Order.class, parameters);
        assertEquals("order 42/42", text());
        assertEquals("orders/42?page=2&status=to+ship", history.getToken());
    }

    @Test
    public void navigateByViewWithoutParameters() {
        final Router router = app().start();
        router.navigate(Orders.class);
        assertEquals("orders", history.getToken());
        assertEquals("orders status=any page=1 flag=false token=orders", text());
        router.navigate(Home.class);
        assertEquals("", history.getToken());
        assertEquals("home", text());
    }

    @Test
    public void aParameterIsEncodedInThePath() {
        final Router router = app().start();
        router.navigate(Order.class, Map.of("id", "a b/c"));
        assertEquals("orders/a%20b%2Fc", history.getToken());
        assertEquals("order a b/c/-1", text());
    }

    @Test
    public void navigateByViewNeedsItsParametersAndAKnownView() {
        final Router router = app().start();
        assertThrows(IllegalArgumentException.class, () -> router.navigate(Order.class));
        assertThrows(IllegalArgumentException.class, () -> router.navigate(Missing.class));
        assertEquals("home", text());
    }

    @Test
    public void navigateByPathTakesItsQueryAsWritten() {
        final Router router = app().start();
        router.navigate("/orders/?status=open");
        assertEquals("orders?status=open", history.getToken());
        assertEquals("orders status=open page=1 flag=false token=orders?status=open", text());
        router.navigate("orders/42");
        assertEquals("order 42/42", text());
    }

    @Test
    public void navigatingToTheTokenAlreadyShownChangesNothing() {
        final Router router = app().start();
        router.navigate("orders/42");
        LOG.clear();
        router.navigate("orders/42");
        history.fireHistoryChanged("orders/42");
        assertEquals(List.of(), log());
    }

    @Test
    public void aViewThatFailsToBuildLeavesTheScreenAndTheTokenAlone() {
        final Router router = Router.builder(container).views(Home.class, Broken.class).start();
        final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> router.navigate("broken"));
        assertTrue(failure.getCause() instanceof InvocationTargetException);
        assertEquals("home", text());
        assertEquals("", history.getToken());
        assertEquals(List.of("build home", "build broken"), log(), "the home view is not destroyed");
    }

    @Test
    public void aRouterWhoseFirstViewFailsIsNotLeftListening() {
        history.newItem("broken", false);
        assertThrows(IllegalStateException.class, () -> Router.builder(container).views(Home.class, Broken.class).start());
        history.fireHistoryChanged("");
        assertEquals(List.of("build broken"), log());
    }

    // ── back and forward ──

    @Test
    public void backAndForwardShowTheViewOfTheToken() {
        final Router router = app().start();
        router.navigate("orders");
        router.navigate("orders/42");
        history.fireHistoryChanged("orders"); // back
        assertEquals("orders status=any page=1 flag=false token=orders", text());
        history.fireHistoryChanged("orders/42"); // forward
        assertEquals("order 42/42", text());
        assertEquals("orders/42", history.getToken());
    }

    // ── a view is destroyed when it is left ──

    @Test
    public void aViewIsDestroyedWhenLeftAndNotBefore() {
        final Router router = app().start();
        router.navigate("orders");
        assertEquals(List.of("build home", "build orders", "destroy home"), log());
        router.navigate("orders/42");
        assertEquals(List.of("build home", "build orders", "destroy home", "build order", "destroy orders"), log());
    }

    @Test
    public void anotherParameterBuildsANewView() {
        final Router router = app().start();
        router.navigate("orders/1");
        final PWidget first = container.getWidget();
        router.navigate("orders/2");
        assertNotSame(first, container.getWidget());
        assertEquals("order 2/2", text());
    }

    // ── not found ──

    @Test
    public void anUnknownPathShowsTheNotFoundViewAndKeepsTheToken() {
        final Router router = app().start();
        router.navigate("nowhere/at/all?x=1");
        assertEquals("Not found: nowhere/at/all?x=1", text());
        assertEquals("nowhere/at/all?x=1", history.getToken());
    }

    @Test
    public void theNotFoundViewOfTheApplicationIsBuiltWithTheToken() {
        history.newItem("nowhere", false);
        Router.builder(container).views(Home.class).notFound(Missing.class).start();
        assertEquals("missing nowhere", text());
    }

    @Test
    public void notFoundIsLeftLikeAnyView() {
        final Router router = Router.builder(container).views(Home.class).notFound(Missing.class).start();
        router.navigate("nowhere");
        router.navigate("");
        assertEquals(List.of("build home", "build missing", "destroy home", "build home", "destroy missing"), log());
    }

    // ── before enter ──

    @Test
    public void aHookThatDoesNothingLetsTheNavigationPass() {
        final List<String> seen = new ArrayList<>();
        final Router router = app().beforeEnter(navigation -> seen.add(navigation.token() + " "
                + navigation.view().getSimpleName() + " " + navigation.parameters().get("id"))).start();
        router.navigate("orders/42");
        router.navigate("nowhere"); // no route: nothing to enter, the hook is not asked
        assertEquals(List.of(" Home null", "orders/42 Order 42"), seen);
        assertEquals("Not found: nowhere", text());
    }

    @Test
    public void aHookCanRedirect() {
        final Router router = app().beforeEnter(navigation -> {
            if (navigation.view() == Admin.class) navigation.redirect("login?from=admin");
        }).start();
        router.navigate("admin");
        assertEquals("login", text());
        assertEquals("login?from=admin", history.getToken());
        assertEquals(List.of("build home", "build login", "destroy home"), log(), "the redirected view is never built");
    }

    @Test
    public void aRedirectionGoesThroughTheHookAgain() {
        final List<String> seen = new ArrayList<>();
        final Router router = app().beforeEnter(navigation -> {
            seen.add(navigation.token());
            if (navigation.view() == Admin.class) navigation.redirect("orders");
            if (navigation.view() == Orders.class) navigation.redirect("login");
        }).start();
        router.navigate("admin");
        assertEquals("login", text());
        assertEquals(List.of("", "admin", "orders", "login"), seen);
    }

    @Test
    public void redirectionsThatNeverEndAreStopped() {
        final Router router = app().beforeEnter(navigation -> {
            if (navigation.view() == Admin.class) navigation.redirect("login");
            if (navigation.view() == Login.class) navigation.redirect("admin");
        }).start();
        final IllegalStateException loop = assertThrows(IllegalStateException.class, () -> router.navigate("admin"));
        assertTrue(loop.getMessage().startsWith("More than 8 redirections"), loop.getMessage());
        assertEquals("home", text());
    }

    @Test
    public void aHookCanDeny() {
        final Router router = app().beforeEnter(navigation -> {
            if (navigation.view() == Admin.class) navigation.deny();
        }).start();
        router.navigate("admin");
        assertEquals("Access denied: admin", text());
        assertEquals("admin", history.getToken());
    }

    @Test
    public void aRedirectionToWhereTheViewAlreadyIsStays() {
        final Router router = app().beforeEnter(navigation -> {
            if (navigation.view() == Admin.class) navigation.redirect("");
        }).start();
        router.navigate("admin");
        assertEquals("home", text());
        assertEquals(List.of("build home"), log());
    }

    // ── access ──

    @Test
    public void aViewIsEnteredWhenItsPredicateAgrees() {
        UIContext.get().setAttribute("roles", Set.of("admin"));
        try {
            final Router router = app().access(Admin.class, ui -> ui.<Set<String>> getAttribute("roles").contains("admin")).start();
            router.navigate("admin");
            assertEquals("admin", text());
        } finally {
            UIContext.get().removeAttribute("roles");
        }
    }

    @Test
    public void aViewIsDeniedWhenItsPredicateDisagreesAndTheRolesAreReadEachTime() {
        UIContext.get().setAttribute("roles", Set.of("trader"));
        try {
            final Router router = app().access(Admin.class, ui -> ui.<Set<String>> getAttribute("roles").contains("admin")).start();
            router.navigate("admin");
            assertEquals("Access denied: admin", text());
            assertEquals("admin", history.getToken());
            router.navigate("orders");
            UIContext.get().setAttribute("roles", Set.of("trader", "admin"));
            router.navigate("admin");
            assertEquals("admin", text());
        } finally {
            UIContext.get().removeAttribute("roles");
        }
    }

    @Test
    public void theDeniedViewOfTheApplicationGetsTheParametersOfTheRoute() {
        history.newItem("orders/42?x=1", false);
        Router.builder(container).views(Order.class).denied(Forbidden.class).access(Order.class, ui -> false).start();
        assertEquals("forbidden orders/42?x=1 id=42", text());
    }

    @Test
    public void aDeepLinkToADeniedViewShowsTheDeniedView() {
        history.newItem("admin", false);
        app().access(Admin.class, ui -> false).start();
        assertEquals("Access denied: admin", text());
        assertEquals(List.of(), log(), "the denied view is not built");
    }

    @Test
    public void theHookIsAskedBeforeTheAccessPredicate() {
        final List<String> calls = new ArrayList<>();
        final Router router = app().beforeEnter(navigation -> calls.add("hook " + navigation.view().getSimpleName()))
            .access(Admin.class, ui -> calls.add("access") && false).start();
        router.navigate("admin");
        assertEquals(List.of("hook Home", "hook Admin", "access"), calls);
        assertEquals("Access denied: admin", text());
    }

    @Test
    public void aRedirectedNavigationIsNotAskedAboutTheAccess() {
        final List<String> calls = new ArrayList<>();
        final Router router = app().beforeEnter(navigation -> {
            if (navigation.view() == Admin.class) navigation.redirect("login");
        }).access(Admin.class, ui -> calls.add("access")).start();
        router.navigate("admin");
        assertEquals("login", text());
        assertEquals(List.of(), calls);
    }

    // ── before leave ──

    @Test
    public void aViewThatDoesNotPostponeIsLeft() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        router.navigate("orders");
        assertTrue(text().startsWith("orders"));
        assertEquals("orders", asked.token());
        assertSame(Orders.class, asked.view());
        assertEquals("orders", asked.parameters().token());
    }

    @Test
    public void aViewThatIsLeftForNothingKnownIsToldSo() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        router.navigate("nowhere");
        assertNull(asked.view());
        assertEquals("Not found: nowhere", text());
    }

    @Test
    public void aPostponedNavigationKeepsTheViewAndWaits() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        assertEquals("edit 42", text());
        assertEquals("orders/42/edit", history.getToken());
        assertEquals(List.of("build home", "build edit", "destroy home"), log());
        assertEquals("orders", held.token());
    }

    @Test
    public void aPostponedNavigationResumesWithoutAskingTheViewAgain() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        asked = null;
        held.resume();
        assertTrue(text().startsWith("orders"));
        assertEquals("orders", history.getToken());
        assertNull(asked, "the view already agreed");
        assertEquals(List.of("build home", "build edit", "destroy home", "build orders", "destroy edit"), log());
    }

    @Test
    public void aPostponedNavigationCanBeCancelled() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        held.cancel();
        held.resume(); // too late
        assertEquals("edit 42", text());
        assertEquals("orders/42/edit", history.getToken());
    }

    @Test
    public void aBackThatIsPostponedBringsTheTokenBack() {
        final Router router = app().start();
        router.navigate("orders");
        router.navigate("orders/42/edit");
        dirty = true;
        history.fireHistoryChanged("orders"); // the browser went back: its token is already "orders"
        assertEquals("edit 42", text());
        assertEquals("orders/42/edit", history.getToken(), "the token is written again: the browser follows");
        held.resume();
        assertTrue(text().startsWith("orders"));
        assertEquals("orders", history.getToken());
    }

    @Test
    public void aPostponedNavigationReplacedByAnotherDoesNothingWhenResumed() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        final Navigation.Postponed first = held;
        router.navigate("admin");
        final Navigation.Postponed second = held;
        assertNotSame(first, second);
        first.resume();
        assertEquals("edit 42", text());
        second.resume();
        assertEquals("admin", text());
    }

    @Test
    public void cancellingAReplacedNavigationKeepsTheCurrentOneWaiting() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        final Navigation.Postponed first = held;
        router.navigate("admin");
        first.cancel();
        held.resume();
        assertEquals("admin", text());
    }

    @Test
    public void goingBackToTheViewItselfCancelsTheWaitingNavigation() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        router.navigate("orders/42/edit"); // where it already is
        held.resume();
        assertEquals("edit 42", text());
    }

    @Test
    public void postponingTwiceGivesTheSameHandle() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        assertSame(held, asked.postpone());
    }

    @Test
    public void aNavigationResumedInsideTheHookDoesNotGo() {
        final Router router = Router.builder(container).views(Home.class, Impatient.class, Orders.class).start();
        router.navigate("impatient");
        router.navigate("orders");
        assertEquals("impatient", text());
    }

    @Test
    public void aCallOfTheWrongKindFails() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        assertThrows(IllegalStateException.class, () -> asked.redirect("login"));
        assertThrows(IllegalStateException.class, () -> asked.deny());
    }

    @Test
    public void postponingIsForLeavingOnly() {
        final Navigation[] entering = new Navigation[1];
        app().beforeEnter(navigation -> entering[0] = navigation).start();
        assertThrows(IllegalStateException.class, () -> entering[0].postpone());
    }

    // ── a base: a router that shares the token with the rest of the application ──

    @Test
    public void aRouterWithABaseReadsAndWritesUnderIt() {
        history.newItem("routing/orders/42?x=1", false);
        final Router router = app().base("/routing/").start();
        assertEquals("order 42/42", text());
        router.navigate("orders");
        assertEquals("routing/orders", history.getToken());
        router.navigate(Order.class, Map.of("id", 7, "status", "open"));
        assertEquals("routing/orders/7?status=open", history.getToken());
        router.navigate("");
        assertEquals("routing", history.getToken());
        assertEquals("home", text());
    }

    @Test
    public void theBaseAloneAndItsQueryAreTheRoot() {
        history.newItem("routing", false);
        app().base("routing").start();
        assertEquals("home", text());
        history.fireHistoryChanged("routing?x=1");
        assertEquals("home", text());
        assertEquals(List.of("build home", "build home", "destroy home"), log(), "another token, another view");
    }

    @Test
    public void aTokenOutsideTheBaseIsLeftAloneWhenItComes() {
        history.newItem("routing/orders", false);
        app().base("routing").start();
        final String shown = text();
        history.fireHistoryChanged("overview");
        history.fireHistoryChanged("routingx");
        history.fireHistoryChanged("routingx/orders");
        assertEquals(shown, text());
        assertEquals("routingx/orders", history.getToken());
    }

    @Test
    public void startingOutsideTheBaseGoesToItsRoot() {
        history.newItem("reconnection", false);
        app().base("routing").start();
        assertEquals("home", text());
        assertEquals("routing", history.getToken());
    }

    @Test
    public void startingOnAWordThatOnlyBeginsWithTheBaseGoesToItsRoot() {
        history.newItem("routingx", false);
        app().base("routing").start();
        assertEquals("routing", history.getToken());
    }

    // ── closing ──

    @Test
    public void aRouterStopsWithItsPanel() {
        final Router router = app().start();
        router.navigate("orders");
        container.onDestroy();
        LOG.clear();
        history.fireHistoryChanged("orders/42");
        router.navigate("orders/43");
        router.navigate(Orders.class);
        assertEquals(List.of(), log());
        assertEquals("orders/42", history.getToken(), "navigate does nothing once the panel is gone");
    }

    @Test
    public void aHandlerOfTheSameEventThatDestroyedThePanelIsNotFollowed() {
        history.addValueChangeHandler(new PValueChangeHandler<String>() { // runs before the router's
            @Override
            public void onValueChange(final PValueChangeEvent<String> event) {
                history.removeValueChangeHandler(this);
                container.onDestroy();
            }
        });
        final Router router = app().start();
        LOG.clear();
        history.fireHistoryChanged("orders");
        assertEquals(List.of("destroy home"), log(), "the panel took its view with it, the router built nothing");
        router.navigate("orders");
        assertEquals(List.of("destroy home"), log());
    }

    @Test
    public void aPostponedNavigationIsForgottenWhenThePanelGoes() {
        final Router router = app().start();
        router.navigate("orders/42/edit");
        dirty = true;
        router.navigate("orders");
        final Navigation.Postponed waiting = held;
        container.onDestroy();
        LOG.clear();
        waiting.resume();
        assertEquals(List.of(), log());
    }

    // ── what the application writes wrong ──

    @Test
    public void aViewNeedsItsRoute() {
        assertThrows(IllegalArgumentException.class, () -> Router.builder(container).views(Missing.class).start());
    }

    @Test
    public void twoViewsCannotShareAPath() {
        assertThrows(IllegalArgumentException.class, () -> Router.builder(container).views(Order.class, Order.class).start());
        assertThrows(IllegalArgumentException.class, () -> Router.builder(container).views(Order.class, OrderOther.class).start());
    }

    @Test
    public void anAccessRuleIsForAViewOfTheRouter() {
        assertThrows(IllegalArgumentException.class, () -> app().access(Missing.class, ui -> true).start());
    }

    @Test
    public void aNavigationIsToAViewOfTheRouter() {
        final Router router = Router.builder(container).views(Home.class).start();
        assertThrows(IllegalArgumentException.class, () -> router.navigate(Order.class, Map.of("id", 1)));
    }

    @Test
    public void theNotFoundAndDeniedViewsMustBeViews() {
        assertThrows(IllegalArgumentException.class, () -> app().notFound(String.class).start());
        assertThrows(IllegalArgumentException.class, () -> app().denied(Object.class).start());
    }
}
