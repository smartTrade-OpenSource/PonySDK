/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpCookie;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.Subprotocols;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.i18n.Locales;

import jakarta.servlet.http.HttpSession;

/** The locale of a context: negotiated at the handshake, changed by the application (docs/decisions/0022-locale.md). */
public class UIContextLocaleTest {

    private static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, Locale.FRENCH, Locale.forLanguageTag("ar"));

    private Application application;
    private final WebSocket socket = Mockito.mock(WebSocket.class);

    @AfterEach
    public void tearDown() {
        if (application != null) SessionManager.get().unregisterApplication(application);
        UIContext.remove();
    }

    private UIContext context(final List<Locale> supported, final String acceptLanguage, final String cookie) {
        return context(supported, acceptLanguage, cookie, false);
    }

    private UIContext context(final List<Locale> supported, final String acceptLanguage, final String cookie, final boolean embedded) {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setStringDictionaryEnabled(false);
        config.setSupportedLocales(supported);
        application = new Application("locale-test", Mockito.mock(HttpSession.class), config);
        SessionManager.get().registerApplication(application);
        final Map<String, List<String>> headers = new java.util.HashMap<>();
        if (acceptLanguage != null) headers.put("Accept-Language", List.of(acceptLanguage));
        if (embedded) headers.put(Subprotocols.HEADER, List.of(Subprotocols.PONYSDK));
        final Map<String, List<String>> parameters = embedded ? Map.of(ClientToServerModel.HOST_ATTRIBUTES.toStringValue(), List.of("{}")) : Map.of();
        final List<HttpCookie> cookies = cookie == null ? List.of() : List.of(new HttpCookie("theme", "dark"), new HttpCookie(Locales.COOKIE, cookie));
        final UpgradeRequestData upgrade = new UpgradeRequestData(parameters, headers, cookies, null, false, null, "Mozilla/5.0", Mockito.mock(HttpSession.class));
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(upgrade);
        final TxnContext txnContext = new TxnContext(socket);
        txnContext.setApplication(application);
        final UIContext uiContext = new UIContext(socket, txnContext, config, upgrade);
        application.registerUIContext(uiContext);
        return uiContext;
    }

    @Test
    public void withoutSupportedLocalesTheContextHasTheJvmsAndTheRootIsLeftAlone() {
        final UIContext uiContext = context(List.of(), "fr", null);

        assertEquals(Locale.getDefault(), uiContext.getLocale());
        uiContext.execute(() -> PWindow.getMain().getPRootPanel());
        uiContext.flush();
        Mockito.verify(socket, Mockito.never()).encode(Mockito.eq(ServerToClientModel.PUT_ATTRIBUTE_KEY), Mockito.any());
    }

    @Test
    public void theLocaleIsNegotiatedAtTheHandshake() {
        assertEquals(Locale.FRENCH, context(SUPPORTED, "fr-CH,fr;q=0.9,en;q=0.8", null).getLocale());
        assertEquals(Locale.ENGLISH, context(SUPPORTED, "de", null).getLocale(), "the first supported");
        assertEquals(Locale.ENGLISH, context(SUPPORTED, null, null).getLocale());
    }

    @Test
    public void theVisitorsChoiceInTheCookieComesBeforeTheBrowsersLanguage() {
        assertEquals(Locale.forLanguageTag("ar"), context(SUPPORTED, "fr", "ar").getLocale());
    }

    @Test
    public void thePageRootIsCreatedWithTheLanguageAndTheDirection() {
        final UIContext uiContext = context(SUPPORTED, "ar", null);

        uiContext.execute(() -> PWindow.getMain().getPRootPanel());
        uiContext.flush();

        final ArgumentCaptor<Object> names = ArgumentCaptor.forClass(Object.class);
        final ArgumentCaptor<Object> values = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(socket, Mockito.atLeastOnce()).encode(Mockito.eq(ServerToClientModel.PUT_ATTRIBUTE_KEY), names.capture());
        Mockito.verify(socket, Mockito.atLeastOnce()).encode(Mockito.eq(ServerToClientModel.ATTRIBUTE_VALUE), values.capture());
        assertEquals(List.of("lang", "dir"), names.getAllValues());
        assertEquals(List.of("ar", "rtl"), values.getAllValues());
    }

    @Test
    public void aLeftToRightLanguageIsWrittenLtrAndAZoneIsLeftAlone() {
        final UIContext uiContext = context(SUPPORTED, "fr", null);

        uiContext.execute(() -> {
            PWindow.getMain().getPRootPanel("zone");
            PWindow.getMain().getPRootPanel();
        });
        uiContext.flush();

        final ArgumentCaptor<Object> values = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(socket, Mockito.atLeastOnce()).encode(Mockito.eq(ServerToClientModel.ATTRIBUTE_VALUE), values.capture());
        assertEquals(List.of("fr", "ltr"), values.getAllValues(), "the page's root, not the element of a zone");
    }

    @Test
    public void changingTheLocaleKeepsTheChoiceInACookieAndReloadsThePage() throws Exception {
        final UIContext uiContext = context(SUPPORTED, "en", null);
        final CountDownLatch destroyed = new CountDownLatch(1);
        uiContext.addContextDestroyListener(context -> destroyed.countDown());

        uiContext.setLocale(Locale.FRENCH);

        assertEquals(Locale.FRENCH, uiContext.getLocale());
        assertTrue(destroyed.await(5, TimeUnit.SECONDS), "the context ends: the page reloads at its token");
        final InOrder order = Mockito.inOrder(socket);
        order.verify(socket).encode(ServerToClientModel.ADD_COOKIE, Locales.COOKIE);
        order.verify(socket).encode(ServerToClientModel.VALUE, "fr");
        order.verify(socket).encode(ServerToClientModel.DESTROY_CONTEXT, null);
        Mockito.verify(socket).encode(ServerToClientModel.COOKIE_PATH, "/");
        Mockito.verify(socket).encode(ServerToClientModel.COOKIE_SAMESITE, "Lax");
    }

    @Test
    public void aLocaleThatFitsIsTakenInItsSupportedForm() throws Exception {
        final UIContext uiContext = context(SUPPORTED, "en", null);
        final CountDownLatch destroyed = new CountDownLatch(1);
        uiContext.addContextDestroyListener(context -> destroyed.countDown());

        uiContext.setLocale(Locale.CANADA_FRENCH);

        assertEquals(Locale.FRENCH, uiContext.getLocale());
        assertTrue(destroyed.await(5, TimeUnit.SECONDS));
        Mockito.verify(socket).encode(ServerToClientModel.VALUE, "fr");
    }

    @Test
    public void theCurrentLocaleChangesNothing() throws Exception {
        final UIContext uiContext = context(SUPPORTED, "fr", null);

        uiContext.setLocale(Locale.FRENCH);
        uiContext.setLocale(Locale.CANADA_FRENCH);
        Thread.sleep(200); // a reload would have started

        assertTrue(uiContext.isAlive());
        Mockito.verify(socket, Mockito.never()).encode(Mockito.eq(ServerToClientModel.ADD_COOKIE), Mockito.any());
        Mockito.verify(socket, Mockito.never()).encode(Mockito.eq(ServerToClientModel.DESTROY_CONTEXT), Mockito.any());
    }

    @Test
    public void aLocaleTheApplicationDoesNotSupportIsRefused() {
        final UIContext uiContext = context(SUPPORTED, "en", null);

        assertThrows(IllegalArgumentException.class, () -> uiContext.setLocale(Locale.GERMAN));
        assertEquals(Locale.ENGLISH, uiContext.getLocale());
        assertTrue(uiContext.isAlive());
    }

    @Test
    public void withoutSupportedLocalesThereIsNothingToChange() {
        assertThrows(IllegalStateException.class, () -> context(List.of(), null, null).setLocale(Locale.FRENCH));
    }

    @Test
    public void anEmbeddedContextWaitsForItsTerminalsCookieRequestBeforeTheReload() throws Exception {
        final UIContext uiContext = context(SUPPORTED, "en", null, true);
        assertTrue(uiContext.isEmbedded());
        final CountDownLatch destroyed = new CountDownLatch(1);
        uiContext.addContextDestroyListener(context -> destroyed.countDown());

        uiContext.setLocale(Locale.FRENCH);

        Thread.sleep(150);
        assertEquals(1, destroyed.getCount(), "the context is there for the terminal's request");
        Mockito.verify(socket).encode(ServerToClientModel.HANDLER_TYPE, com.ponysdk.core.model.HandlerModel.HANDLER_COOKIE_REQUEST.getValue());
        assertTrue(destroyed.await(5, TimeUnit.SECONDS), "then it ends and the element starts again");
    }
}
