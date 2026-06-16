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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.compression.gzip.GzipCompression;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.ee11.servlet.ErrorHandler;
import org.eclipse.jetty.server.Handler;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Non-regression tests for the Jakarta EE11 / Jetty 12.1 migration of {@link PonySDKServer}.
 *
 * <ul>
 * <li>the main handler must be a Jetty 12.1 {@link CompressionHandler} (the EE10 {@code GzipHandler}
 * is deprecated for removal), it must register gzip and wrap the application handler;</li>
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
    }

    @Test
    public void testCreateMainHandler_usesGzipCompressionHandlerWrappingApp() {
        final TestServer server = new TestServer();

        final Handler handler = server.mainHandler();

        assertTrue("main handler must be a Jetty 12.1 CompressionHandler", handler instanceof CompressionHandler);
        final CompressionHandler compression = (CompressionHandler) handler;
        assertSame("compression handler must wrap the application handler", server.appHandler, compression.getHandler());

        // removeCompression returns the registered Compression (non-null) → proves gzip was registered
        final String gzipName = new GzipCompression().getName();
        assertNotNull("gzip compression must be registered on the handler", compression.removeCompression(gzipName));
    }

    @Test
    public void testCreateErrorHandler_hidesStacksAndMessageInTitle() {
        final ErrorHandler errorHandler = new TestServer().errorHandler();

        assertNotNull(errorHandler);
        assertFalse("error handler must not show stack traces", errorHandler.isShowStacks());
        assertFalse("error handler must not show the message in the title", errorHandler.isShowMessageInTitle());
    }
}
