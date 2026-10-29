/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.impl.spring.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** The application started for real in Boot's Jetty on a random port: its page over HTTP, its screens over the WebSocket. */
@SpringBootTest(classes = BootTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "ponysdk.application-context-name=app", "ponysdk.string-dictionary.persistence=false",
            "ponysdk.websocket.allowed-origins=https://portal.example.com", "ponysdk.application-name=Boot test" })
class BootRunTest {

    @LocalServerPort
    int port;

    @Autowired
    PonySDKHealthIndicator health;

    private HttpResponse<String> get(final String path, final String... headers) throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            final HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
            for (int i = 0; i < headers.length; i += 2) request.header(headers[i], headers[i + 1]);
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void theBootstrapPageCarriesTheConfigurationAndAPolicy() throws Exception {
        final HttpResponse<String> page = get("/app/");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("<title>Boot test</title>"), page.body());
        assertTrue(page.body().contains("script/ponysdk.js"), page.body());
        assertTrue(page.headers().firstValue("Content-Security-Policy").isPresent(), "the policy of ADR-0015");
        assertEquals("nosniff", page.headers().firstValue("X-Content-Type-Options").orElse(null));
    }

    @Test
    void theApplicationsPathWithoutItsSlashGoesToThePage() throws Exception {
        final HttpResponse<String> redirect = get("/app");
        assertEquals(302, redirect.statusCode());
        assertEquals("/app/", URI.create(redirect.headers().firstValue("Location").orElseThrow()).getPath());
    }

    @Test
    void theTerminalAndTheHealthCheckAreServedAndWhatIsNotTheirsStaysBootsAndTheApplications() throws Exception {
        final HttpResponse<String> terminal = get("/app/script/ponysdk.js");
        assertEquals(200, terminal.statusCode());
        assertTrue(terminal.headers().firstValue("Content-Type").orElse("").contains("javascript"));

        final HttpResponse<String> up = get("/app/health");
        assertEquals(200, up.statusCode());
        assertEquals("UP", up.body());
        assertEquals(Status.UP, health.health().getStatus());

        assertEquals(404, get("/app/script/missing.js").statusCode(), "a file the application does not have");
        assertEquals(404, get("/elsewhere").statusCode(), "outside the application's path, nothing of PonySDK's");
    }

    @Test
    void thePagesAllowedToEmbedTheApplicationAreAnsweredWithCredentials() throws Exception {
        final HttpResponse<String> allowed = get("/app/health", "Origin", "https://portal.example.com");
        assertEquals("https://portal.example.com", allowed.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertEquals("true", allowed.headers().firstValue("Access-Control-Allow-Credentials").orElse(null));

        final HttpResponse<String> other = get("/app/health", "Origin", "https://evil.example.com");
        assertTrue(other.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }

    @Test
    void theWebSocketSendsTheFirstScreenAndRunsTheClicksAndEachScreenHasItsOwnEntryPoint() throws Exception {
        final int createdBefore = BootTestBeans.CREATED.get();
        final int destroyedBefore = BootTestBeans.DESTROYED.get();

        final ScreenClient first = new ScreenClient("ws://127.0.0.1:" + port + "/app/ws");
        final ScreenClient second = new ScreenClient("ws://127.0.0.1:" + port + "/app/ws");
        first.assertSeen("Go");
        second.assertSeen("Go");
        assertEquals("Hello", first.element("LABEL").getText());
        assertEquals(createdBefore + 2, BootTestBeans.CREATED.get(), "one entry point per screen");

        first.click("BUTTON");
        first.assertSeen("Clicked");
        assertEquals("Clicked", first.element("LABEL").getText());
        assertEquals("Hello", second.element("LABEL").getText(), "the other screen is its own");

        first.close();
        assertTrue(first.awaitClosed());
        final long deadline = System.nanoTime() + 10_000_000_000L;
        while (BootTestBeans.DESTROYED.get() < destroyedBefore + 1 && System.nanoTime() < deadline) Thread.sleep(20);
        assertEquals(destroyedBefore + 1, BootTestBeans.DESTROYED.get(), "the screen's beans end with it");
        second.close();
    }
}
