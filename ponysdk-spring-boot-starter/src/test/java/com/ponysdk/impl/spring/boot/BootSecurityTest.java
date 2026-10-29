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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** What the guide says of Spring Security: it is in front of the application's path, as in front of any other. */
class BootSecurityTest {

    /** Spring Security as Boot configures it without a filter chain of the application's: everything needs a login. */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(BootTestBeans.class)
    static class Default {
    }

    /** The application's path open to the browsers, as an application with its own login would; the AJAX and upload posts without a CSRF token. */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(BootTestBeans.class)
    static class Open {

        @Bean
        SecurityFilterChain chain(final HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(requests -> requests.requestMatchers("/app/**").permitAll().anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/app/ajax", "/app/stream")).httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    /** As {@link Open}, with the CSRF protection Spring Security has by default. */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(BootTestBeans.class)
    static class Csrf {

        @Bean
        SecurityFilterChain chain(final HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            return http.build();
        }
    }

    private static ConfigurableApplicationContext start(final Class<?> application) {
        return new SpringApplicationBuilder(application).properties("server.port=0", "server.address=127.0.0.1",
            "ponysdk.application-context-name=app", "ponysdk.string-dictionary.persistence=false", "spring.main.banner-mode=off").run();
    }

    private static int port(final ConfigurableApplicationContext context) {
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    private static HttpResponse<String> send(final int port, final String method, final String path) throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void withoutAFilterChainOfItsOwnSpringSecuritySendsTheBrowserToLogIn() throws Exception {
        try (ConfigurableApplicationContext context = start(Default.class)) {
            final HttpResponse<String> page = send(port(context), "GET", "/app/");
            assertTrue(page.statusCode() == 401 || page.statusCode() == 302, "not served: " + page.statusCode());
            assertNotEquals(200, send(port(context), "GET", "/app/health").statusCode());
        }
    }

    @Test
    void anApplicationThatOpensItsPathServesThePageAndTheWebSocketOfASession() throws Exception {
        try (ConfigurableApplicationContext context = start(Open.class)) {
            final int port = port(context);
            assertEquals(200, send(port, "GET", "/app/").statusCode());
            assertEquals(401, send(port, "GET", "/other").statusCode(), "the rest stays closed");

            final ScreenClient client = new ScreenClient("ws://127.0.0.1:" + port + "/app/ws");
            client.assertSeen("Go");
            client.close();

            assertNotEquals(403, send(port, "POST", "/app/ajax").statusCode(), "the addons' AJAX posts are let through");
            assertNotEquals(403, send(port, "POST", "/app/stream").statusCode(), "and the uploads");
        }
    }

    @Test
    void theCsrfProtectionRefusesThePostsOfTheTerminalThatCarryNoToken() throws Exception {
        try (ConfigurableApplicationContext context = start(Csrf.class)) {
            final int port = port(context);
            assertEquals(200, send(port, "GET", "/app/").statusCode());
            assertEquals(403, send(port, "POST", "/app/ajax").statusCode());
            assertEquals(403, send(port, "POST", "/app/stream").statusCode());
        }
    }
}
