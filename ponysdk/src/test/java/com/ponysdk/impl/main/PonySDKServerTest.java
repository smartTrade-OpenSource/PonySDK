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
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ponysdk.impl.main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.eclipse.jetty.compression.brotli.BrotliCompression;
import org.eclipse.jetty.compression.gzip.GzipCompression;
import org.eclipse.jetty.compression.server.CompressionConfig;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.ee11.servlet.ErrorHandler;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.Encoder;

/**
 * Non-regression tests for the Jakarta EE11 / Jetty 12.1 migration of {@link PonySDKServer}.
 *
 * <ul>
 * <li>the main handler must be a Jetty 12.1 {@link CompressionHandler} (the EE10 {@code GzipHandler}
 * is deprecated for removal); it registers gzip (and Brotli when its native lib is available),
 * wraps the application handler, and applies a default {@link CompressionConfig} that compresses
 * text but skips already-compressed content;</li>
 * <li>the error handler must keep hiding stack traces and the message-in-title
 * ({@code setShowServlet} was removed in EE11, the rest must stay).</li>
 * </ul>
 */
public class PonySDKServerTest {

    /** Exposes the protected factory methods and stubs the app handler so no full context is needed. */
    private static final class TestServer extends PonySDKServer {

        final Handler appHandler = Mockito.mock(Handler.class);

        @Override
        protected Handler addHandlers() {
            return appHandler;
        }

        Handler mainHandler() {
            return createMainHandler();
        }

        ErrorHandler errorHandler() {
            return createErrorHandler();
        }

        void httpsConnector() {
            createHttpsConnector();
        }
    }

    @Test
    public void httpsWithoutAKeyStoreSaysWhatToSet() {
        // HTTPS is on by default: a server started without a key store must say how to fix it, not throw an NPE
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> new TestServer().httpsConnector());
        assertEquals("HTTPS is on (setUseSSL) and no key store is set: call setSslKeyStoreFile, or setUseSSL(false) "
                + "for plain HTTP", e.getMessage());
    }

    private static org.eclipse.jetty.ee11.servlet.ServletContextHandler webApp(final PonySDKServer server) {
        final com.ponysdk.core.server.application.ApplicationConfiguration configuration =
                new com.ponysdk.core.server.application.ApplicationConfiguration();
        configuration.setApplicationContextName("app");
        final com.ponysdk.core.server.application.ApplicationManager manager =
                Mockito.mock(com.ponysdk.core.server.application.ApplicationManager.class);
        Mockito.when(manager.getConfiguration()).thenReturn(configuration);
        server.setApplicationManager(manager);
        return server.createWebApp();
    }

    private static int deflateLevelOf(final Object pool) throws Exception {
        final java.lang.reflect.Field f = org.eclipse.jetty.util.compression.DeflaterPool.class.getDeclaredField("compressionLevel");
        f.setAccessible(true);
        return f.getInt(pool);
    }

    @Test
    public void testWebSocketDeflaterPoolUsesTheConfiguredLevel() throws Exception {
        final Object pool = webApp(new PonySDKServer()).getAttribute(PonySDKServer.WEBSOCKET_DEFLATER_POOL_ATTRIBUTE);
        assertTrue(pool instanceof org.eclipse.jetty.util.compression.DeflaterPool);
        Assertions.assertEquals(3, deflateLevelOf(pool), "default level");
    }

    @Test
    public void testJettyDefaultDeflaterWhenLevelIsMinusOne() {
        final PonySDKServer server = new PonySDKServer();
        final com.ponysdk.core.server.application.ApplicationConfiguration configuration =
                new com.ponysdk.core.server.application.ApplicationConfiguration();
        configuration.setApplicationContextName("app");
        configuration.setWsDeflateLevel(-1);
        final com.ponysdk.core.server.application.ApplicationManager manager =
                Mockito.mock(com.ponysdk.core.server.application.ApplicationManager.class);
        Mockito.when(manager.getConfiguration()).thenReturn(configuration);
        server.setApplicationManager(manager);
        Assertions.assertNull(server.createWebApp().getAttribute(PonySDKServer.WEBSOCKET_DEFLATER_POOL_ATTRIBUTE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> configuration.setWsDeflateLevel(10));
    }

    @Test
    public void testSessionCookieIsSecureAndHttpOnlyByDefault() {
        final PonySDKServer server = new PonySDKServer();
        assertTrue(server.isSessionCookieSecure());
        final var cookie = webApp(server).getSessionHandler().getSessionCookieConfig();
        assertTrue(cookie.isSecure(), "Secure by default (also behind a TLS-terminating proxy)");
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    public void testSessionCookieSecureCanBeDisabledForPlainHttpDevelopment() {
        final PonySDKServer server = new PonySDKServer();
        server.setSessionCookieSecure(false);
        final var cookie = webApp(server).getSessionHandler().getSessionCookieConfig();
        assertFalse(cookie.isSecure());
        assertTrue(cookie.isHttpOnly(), "HttpOnly is kept");
    }

    @Test
    public void testCreateMainHandler_usesCompressionHandlerWrappingAppWithDefaults() {
        final TestServer server = new TestServer();

        final Handler handler = server.mainHandler();

        assertTrue(handler instanceof CompressionHandler, "main handler must be a Jetty 12.1 CompressionHandler");
        final CompressionHandler compression = (CompressionHandler) handler;
        assertSame(server.appHandler, compression.getHandler(), "compression handler must wrap the application handler");

        // An explicit default config must be set (not the empty "compress everything" config):
        // text is compressed, already-compressed content is skipped.
        final CompressionConfig config = compression.getConfiguration("/");
        assertNotNull(config, "a compression config must be set for '/'");
        assertTrue(config.isCompressMimeTypeSupported("text/html"), "text/html must be compressed");
        assertFalse(config.isCompressMimeTypeSupported("image/png"),
                "already-compressed image/png must NOT be re-compressed");

        // removeCompression returns the registered Compression (non-null) → proves gzip was registered.
        // CompressionHandler keys by the HTTP content-coding token (getEncodingName()).
        assertNotNull(compression.removeCompression(new GzipCompression().getEncodingName()),
                "gzip compression must be registered on the handler");
        // Browsers list gzip first ("gzip, deflate, br, zstd"): without a server preference Jetty picks gzip
        assertEquals(List.of("br", "gzip"), config.getCompressPreferredEncodings(),
                "Brotli preferred when the browser accepts both");
    }

    @Test
    public void testCreateMainHandler_registersBrotliWhenNativeAvailable() {
        // Brotli is a progressive enhancement gated on the native lib: only assert it where loadable.
        Assumptions.assumeTrue(Brotli4jLoader.isAvailable(), "Brotli native library not available on this platform");

        final CompressionHandler compression = (CompressionHandler) new TestServer().mainHandler();

        final BrotliCompression brotli = (BrotliCompression) compression.removeCompression(new BrotliCompression().getEncodingName());
        assertNotNull(brotli, "Brotli must be registered when its native library is available");
        assertEquals(PonySDKServer.BROTLI_LEVEL,
                brotli.getDefaultEncoderConfig().getCompressionLevel(), "a level that compresses each response in milliseconds, not hundreds");
    }

    @Test
    public void testCreateErrorHandler_hidesStacksAndMessageInTitle() {
        final ErrorHandler errorHandler = new TestServer().errorHandler();

        assertNotNull(errorHandler);
        assertFalse(errorHandler.isShowStacks(), "error handler must not show stack traces");
        assertFalse(errorHandler.isShowMessageInTitle(), "error handler must not show the message in the title");
    }

    /** Answers the number of bytes of the request body the application receives. */
    private static final class BodySizeServer extends PonySDKServer {

        @Override
        protected Handler addHandlers() {
            return new Handler.Abstract() {

                @Override
                public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
                    final int size = Content.Source.asByteBuffer(request).remaining();
                    Content.Sink.write(response, true, String.valueOf(size), callback);
                    return true;
                }
            };
        }
    }

    /** The size of a compressed body as the application receives it, through the server's main handler. */
    private static int receivedSize(final String encoding, final byte[] body) throws Exception {
        final Server jetty = new Server();
        final ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("127.0.0.1");
        jetty.addConnector(connector);
        jetty.setHandler(new BodySizeServer().createMainHandler());
        jetty.start();
        try (HttpClient client = HttpClient.newHttpClient()) {
            final HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + connector.getLocalPort() + "/"))
                .header("Content-Encoding", encoding)
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
            return Integer.parseInt(client.send(request, HttpResponse.BodyHandlers.ofString()).body());
        } finally {
            jetty.stop();
        }
    }

    private static final byte[] PLAIN = "a request body ".repeat(100).getBytes(StandardCharsets.US_ASCII);

    @Test
    public void aBrotliRequestBodyIsNotDecodedByTheServer() throws Exception {
        // CVE-2026-111497: the native Brotli decoder writes out of bounds on a crafted stream. PonySDK only
        // compresses responses; a request body must never reach the decoder.
        Assumptions.assumeTrue(Brotli4jLoader.isAvailable(), "Brotli native library not available on this platform");
        final byte[] compressed = Encoder.compress(PLAIN);
        assertEquals(compressed.length, receivedSize("br", compressed), "the body reaches the application as it was sent");
    }

    @Test
    public void aGzipRequestBodyIsNotDecodedByTheServer() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(PLAIN);
        }
        final byte[] compressed = out.toByteArray();
        assertEquals(compressed.length, receivedSize("gzip", compressed), "the body reaches the application as it was sent");
    }
}
