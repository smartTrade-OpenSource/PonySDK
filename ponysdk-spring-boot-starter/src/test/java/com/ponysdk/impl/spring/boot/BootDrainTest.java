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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import com.ponysdk.core.server.application.Drain;

/** {@code server.shutdown=graceful}: the live pages move first, the web server stops after. */
class BootDrainTest {

    private static ConfigurableApplicationContext start(final String... properties) {
        final ConfigurableApplicationContext context = new SpringApplicationBuilder(BootTestApplication.class)
            .properties("server.port=0", "server.address=127.0.0.1", "ponysdk.application-context-name=app",
                "ponysdk.string-dictionary.persistence=false", "spring.main.banner-mode=off")
            .properties(properties).run();
        return context;
    }

    private static int port(final ConfigurableApplicationContext context) {
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    private static HttpResponse<String> health(final HttpClient http, final int port) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/app/health")).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void theLivePagesMoveWhileTheServerStillServesAndItStopsAfter() throws Exception {
        final ConfigurableApplicationContext context = start("ponysdk.drain-timeout=3s");
        final int port = port(context);
        final PonySDKHealthIndicator indicator = context.getBean(PonySDKHealthIndicator.class);
        try (LingeringScreen lingering = new LingeringScreen(); HttpClient http = HttpClient.newHttpClient()) {
            final ScreenClient client = new ScreenClient("ws://127.0.0.1:" + port + "/app/ws");
            client.assertSeen("Go");
            assertEquals(200, health(http, port).statusCode());
            assertEquals(Status.UP, indicator.health().getStatus());

            final Thread shutdown = Thread.ofVirtual().start(context::close); // what SIGTERM runs
            HttpResponse<String> draining = health(http, port);
            final long deadline = System.currentTimeMillis() + 2_000;
            while (draining.statusCode() != 503 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
                draining = health(http, port);
            }
            assertEquals(503, draining.statusCode(), "the web server still answers while the pages move");
            assertEquals("DRAINING", draining.body());
            assertEquals(Status.OUT_OF_SERVICE, indicator.health().getStatus());
            assertEquals(true, indicator.health().getDetails().get("draining"));

            assertTrue(client.awaitDestroyed(), "the page is told its context is over: it reloads");
            assertTrue(client.awaitClosed());
            shutdown.join();

            assertFalse(context.isActive(), "closed once the drain ended");
            assertFalse(Drain.isDraining());
            assertThrows(IOException.class, () -> health(http, port), "then the web server stopped");
        } finally {
            context.close();
        }
    }

    @Test
    void withoutADrainTimeTheServerStopsAtOnce() throws Exception {
        final ConfigurableApplicationContext context = start("ponysdk.drain-timeout=0");
        try (LingeringScreen lingering = new LingeringScreen()) {
            final long start = System.nanoTime();
            context.close();
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 3, "no window to wait for");
            assertFalse(context.isActive());
        } finally {
            context.close();
        }
    }
}
