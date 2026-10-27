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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;

/** {@link PCookies} of an application in its own page: the terminal writes document.cookie. */
public class PCookiesTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    private static void initialize(final PCookies cookies, final String name, final String value) {
        cookies.onClientData(Json.createObjectBuilder().add(ClientToServerModel.COOKIES.toStringValue(), Json.createArrayBuilder()
            .add(Json.createObjectBuilder().add(ClientToServerModel.COOKIE_NAME.toStringValue(), name)
                .add(ClientToServerModel.COOKIE_VALUE.toStringValue(), value))).build());
    }

    @Test
    public void aCookieSetWithADomainIsRemovedWithIt() {
        final PCookies cookies = new PCookies(request -> {
        });
        final PCookies.CookieOptions where = PCookies.CookieOptions.path("/").withDomain("example.com");
        cookies.setCookie("theme", "dark", where);
        writer.clear();

        assertEquals("dark", cookies.removeCookie("theme", where));

        assertEquals(List.of("theme"), writer.valuesOf(ServerToClientModel.REMOVE_COOKIE));
        assertEquals(List.of("example.com"), writer.valuesOf(ServerToClientModel.COOKIE_DOMAIN));
        assertEquals(List.of("/"), writer.valuesOf(ServerToClientModel.COOKIE_PATH));
        // the terminal reads the domain, then the path: the order of ADD_COOKIE
        final List<ServerToClientModel> models = writer.models();
        assertEquals(models.indexOf(ServerToClientModel.COOKIE_DOMAIN) + 1, models.indexOf(ServerToClientModel.COOKIE_PATH));
    }

    @Test
    public void aListenerSetOnceTheCookiesAreKnownIsCalledAtOnce() {
        final PCookies cookies = new PCookies(request -> {
        });
        initialize(cookies, "visits", "3");
        final int[] calls = { 0 };
        cookies.addInitializedListener(() -> calls[0]++);
        assertEquals(1, calls[0], "the cookies are known: no later call would come");
    }

    @Test
    public void everyListenerIsCalledOnceAndARemovedOneIsNot() {
        final PCookies cookies = new PCookies(request -> {
        });
        final List<String> calls = new ArrayList<>();
        cookies.addInitializedListener(() -> calls.add("first"));
        cookies.addInitializedListener(() -> calls.add("removed")).removeHandler();
        cookies.addInitializedListener(() -> calls.add("second"));
        initialize(cookies, "visits", "3");
        initialize(cookies, "visits", "4");
        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    public void allTheAttributesAreWrittenNamed() {
        final PCookies cookies = new PCookies(request -> {
        });
        cookies.setCookie("theme", "dark", PCookies.CookieOptions.expires(java.time.Instant.ofEpochMilli(1_000))
            .withPath("/app").withDomain("example.com").secure().sameSite(PCookies.SameSite.LAX));
        assertEquals(List.of(1_000L), writer.valuesOf(ServerToClientModel.COOKIE_EXPIRE));
        assertEquals(List.of("/app"), writer.valuesOf(ServerToClientModel.COOKIE_PATH));
        assertEquals(List.of("example.com"), writer.valuesOf(ServerToClientModel.COOKIE_DOMAIN));
        assertEquals(List.of(true), writer.valuesOf(ServerToClientModel.COOKIE_SECURE));
        assertEquals(List.of("Lax"), writer.valuesOf(ServerToClientModel.COOKIE_SAMESITE));
    }

    @Test
    public void theNamesAreReadOnly() {
        final PCookies cookies = new PCookies(request -> {
        });
        initialize(cookies, "visits", "3");
        assertEquals(List.of("visits"), List.copyOf(cookies.getNames()));
        assertThrows(UnsupportedOperationException.class, () -> cookies.getNames().clear());
    }

    @Test
    public void theShortOverloadsWriteOnlyTheGivenAttributes() {
        final PCookies cookies = new PCookies(request -> {
        });
        cookies.setCookie("a", "1");
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.ADD_COOKIE));
        assertEquals(List.of("1"), writer.valuesOf(ServerToClientModel.VALUE));
        assertEquals(List.of(0), writer.valuesOf(ServerToClientModel.TYPE_UPDATE), "cookies use the reserved object 0");
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_PATH));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_EXPIRE));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_DOMAIN));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_SECURE));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_SAMESITE));

        writer.clear();
        cookies.setCookie("b", "2", "/b");
        assertEquals(List.of("/b"), writer.valuesOf(ServerToClientModel.COOKIE_PATH));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_DOMAIN));

        writer.clear();
        cookies.setCookie("c", "3", new Date(42L));
        assertEquals(List.of(42L), writer.valuesOf(ServerToClientModel.COOKIE_EXPIRE));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_PATH));

        writer.clear();
        cookies.setCookie("d", "4", new Date(43L), "/d");
        assertEquals(List.of(43L), writer.valuesOf(ServerToClientModel.COOKIE_EXPIRE));
        assertEquals(List.of("/d"), writer.valuesOf(ServerToClientModel.COOKIE_PATH));

        writer.clear();
        cookies.setCookie("e", "5", (Date) null);
        cookies.setCookie("f", "6", PCookies.CookieOptions.domain("example.com"));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_EXPIRE));
        assertEquals(List.of("example.com"), writer.valuesOf(ServerToClientModel.COOKIE_DOMAIN));

        assertEquals("4", cookies.getCookie("d"));
        assertEquals(Set.of("a", "b", "c", "d", "e", "f"), Set.copyOf(cookies.getNames()));
    }

    @Test
    public void removeWritesThePathOnlyWhenGiven() {
        final PCookies cookies = new PCookies(request -> {
        });
        cookies.setCookie("a", "1");
        cookies.setCookie("b", "2");
        writer.clear();

        assertEquals("1", cookies.removeCookie("a"));
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.REMOVE_COOKIE));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_PATH));
        assertEquals(0, writer.count(ServerToClientModel.COOKIE_DOMAIN));
        assertNull(cookies.getCookie("a"));

        assertEquals("2", cookies.removeCookie("b", "/p"));
        assertEquals(List.of("/p"), writer.valuesOf(ServerToClientModel.COOKIE_PATH));
        assertNull(cookies.removeCookie("unknown"));
    }

    @Test
    public void theClientCookiesInitializeOnceAndUpdateTheCache() {
        final PCookies cookies = new PCookies(request -> {
        });
        assertFalse(cookies.isInitialized());
        initialize(cookies, "k", "v1");
        assertTrue(cookies.isInitialized());
        assertEquals("v1", cookies.getCookie("k"));
        initialize(cookies, "k", "v2");
        assertEquals("v2", cookies.getCookie("k"));
    }

    @Test
    public void aCookiesObjectNeedsItsRequestSink() {
        assertThrows(NullPointerException.class, () -> new PCookies(null));
        final PCookies cookies = new PCookies(request -> {
        });
        assertThrows(NullPointerException.class, () -> cookies.addInitializedListener(null));
    }
}
