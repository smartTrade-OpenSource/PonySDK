/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.eventbus.StreamHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link PCookies} of an application in a {@code <pony-app>} element: those of the application's origin, not the host
 * page's document.cookie (docs/decisions/0009-embedding-limits.md, decision 7).
 */
public class PCookiesEmbeddedTest extends PSuite {

    private CapturingModelWriter writer;
    /** The cookie requests the cookies ask of their UIContext. */
    private final List<StreamHandler> requests = new ArrayList<>();

    @BeforeEach
    public void embedded() {
        writer = CapturingModelWriter.install();
        Mockito.doReturn(true).when(UIContext.get()).isEmbedded();
    }

    @AfterEach
    public void reset() {
        writer.uninstall();
        Mockito.doReturn(false).when(UIContext.get()).isEmbedded();
        Mockito.doReturn(List.of()).when(UIContext.get()).getRequestCookies();
        Mockito.clearInvocations(UIContext.get());
    }

    @Test
    public void theCookiesAreThoseOfTheUpgradeTheSessionCookieAside() {
        Mockito.doReturn(List.of(new HttpCookie("JSESSIONID", "node0x"), new HttpCookie("theme", "dark%20blue"),
            new HttpCookie("a%2Bb", "1+1"))).when(UIContext.get()).getRequestCookies();
        final PCookies cookies = new PCookies(requests::add);

        cookies.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.COOKIES.toStringValue(), Json.createArrayBuilder()).build());

        assertTrue(cookies.isInitialized());
        assertEquals("dark blue", cookies.getCookie("theme"), "URI-decoded, as the terminal reads them in a page");
        assertEquals("1+1", cookies.getCookie("a+b"));
        assertNull(cookies.getCookie("JSESSIONID"), "the session cookie is not the application's to read");
    }

    @Test
    public void aWriteIsARequestToTheApplicationsOriginAnsweredWithSetCookie() {
        final PCookies cookies = new PCookies(requests::add);
        cookies.setCookie("last quantity", "1 000", PCookies.CookieOptions.path("/sample").secure().sameSite(PCookies.SameSite.STRICT));

        assertEquals("1 000", cookies.getCookie("last quantity"));
        assertTrue(writer.valuesOf(ServerToClientModel.ADD_COOKIE).isEmpty(), "nothing for document.cookie, the host's");
        final Cookie cookie = answered();
        assertEquals("last%20quantity", cookie.getName());
        assertEquals("1%20000", cookie.getValue());
        assertEquals("/sample", cookie.getPath());
        assertTrue(cookie.getSecure());
        assertEquals("Strict", cookie.getAttribute("SameSite"));
    }

    @Test
    public void aRemovalIsACookieThatExpiresAtOnce() {
        final PCookies cookies = new PCookies(requests::add);
        cookies.setCookie("theme", "dark");
        requests.clear();

        assertEquals("dark", cookies.removeCookie("theme"));
        assertEquals(0, answered().getMaxAge());
        assertTrue(writer.valuesOf(ServerToClientModel.REMOVE_COOKIE).isEmpty());
    }

    /** The cookie the response to the terminal's request sets. */
    private Cookie answered() {
        assertEquals(1, requests.size(), "one request to the application's origin");
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        requests.get(0).onStream(null, response, UIContext.get());
        final ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
        Mockito.verify(response).addCookie(cookie.capture());
        Mockito.verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
        return cookie.getValue();
    }
}
