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

package com.ponysdk.impl.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Redirects keep Jetty 9's 302 with an absolute Location. */
public class PonySDKServerRedirectTest {

    private static final String CONTEXT_PATH = "/app";

    private Server server;

    private ServerConnector connector;

    private ContextHandlerCollection contexts;

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @Before
    public void startServer() throws Exception {
        final ServletContextHandler context = new ServletContextHandler(CONTEXT_PATH);
        context.addServlet(new ServletHolder(new RedirectToContextRootServlet()), "/redirect");

        final PonySDKServer ponySDKServer = new PonySDKServer() {

            @Override
            protected Handler createMainHandler() {
                // Like a product that adds contexts next to the main handler.
                final ServletContextHandler sibling = new ServletContextHandler("/app/ajax");
                return new Handler.Sequence(sibling, super.createMainHandler());
            }

            @Override
            protected Handler addHandlers() {
                contexts = new ContextHandlerCollection(context, new ServletContextHandler("/"));
                return contexts;
            }
        };

        server = new Server();
        connector = new ServerConnector(server, new HttpConnectionFactory(ponySDKServer.createHttpConfiguration()));
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);
        server.setHandler(ponySDKServer.createRootHandler());
        server.start();
    }

    @After
    public void stopServer() throws Exception {
        server.stop();
    }

    @Test
    public void servletRedirectIsAbsolute() throws IOException, InterruptedException {
        final HttpResponse<String> response = get(CONTEXT_PATH + "/redirect");

        assertEquals(302, response.statusCode());
        assertEquals(backend() + CONTEXT_PATH + "/", location(response));
    }

    @Test
    public void contextPathWithoutTrailingSlashRedirectsToAnAbsoluteUrl() throws IOException, InterruptedException {
        final HttpResponse<String> response = get(CONTEXT_PATH);

        assertEquals(302, response.statusCode());
        assertEquals(backend() + CONTEXT_PATH + "/", location(response));
    }

    @Test
    public void postToTheContextPathRedirectsWithA302() throws IOException, InterruptedException {
        final HttpResponse<String> response = client.send(
            HttpRequest.newBuilder(URI.create(backend() + CONTEXT_PATH)).POST(HttpRequest.BodyPublishers.ofString("a=b")).build(),
            HttpResponse.BodyHandlers.ofString());

        assertEquals(302, response.statusCode());
        assertEquals(backend() + CONTEXT_PATH + "/", location(response));
    }

    @Test
    public void contextOutsideTheMainHandlerRedirectsToAnAbsoluteUrl() throws IOException, InterruptedException {
        final HttpResponse<String> response = get(CONTEXT_PATH + "/ajax");

        assertEquals(302, response.statusCode());
        assertEquals(backend() + CONTEXT_PATH + "/ajax/", location(response));
    }

    @Test
    public void contextPathRedirectKeepsTheQueryString() throws IOException, InterruptedException {
        final HttpResponse<String> response = get(CONTEXT_PATH + "?x=1");

        assertEquals(302, response.statusCode());
        assertEquals(backend() + CONTEXT_PATH + "/?x=1", location(response));
    }

    @Test
    public void contextAddedAfterTheStartRedirectsToAnAbsoluteUrl() throws Exception {
        final ServletContextHandler lateContext = new ServletContextHandler("/late");
        contexts.addHandler(lateContext);
        lateContext.start();

        final HttpResponse<String> response = get("/late");

        assertEquals(302, response.statusCode());
        assertEquals(backend() + "/late/", location(response));
    }

    @Test
    public void contextPathWithTrailingSlashIsNotRedirected() throws IOException, InterruptedException {
        final HttpResponse<String> response = get(CONTEXT_PATH + "/");

        assertEquals(404, response.statusCode());
        assertNull(location(response));
    }

    @Test
    public void rootContextIsNotRedirected() throws IOException, InterruptedException {
        final HttpResponse<String> response = get("/");

        assertEquals(404, response.statusCode());
        assertNull(location(response));
    }

    @Test
    public void unknownPathIsNotRedirected() throws IOException, InterruptedException {
        final HttpResponse<String> response = get("/unknown");

        assertEquals(404, response.statusCode());
        assertNull(location(response));
    }

    private String backend() {
        return "http://127.0.0.1:" + connector.getLocalPort();
    }

    private HttpResponse<String> get(final String pathAndQuery) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(backend() + pathAndQuery)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private static String location(final HttpResponse<String> response) {
        return response.headers().firstValue("Location").orElse(null);
    }

    private static final class RedirectToContextRootServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
