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

package com.ponysdk.core.server.stm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.websocket.WebSocket;

/** Unit tests for {@link TxnContext}: the socket it flushes and its delegation to the application. */
public class TxnContextTest {

    private final WebSocket socket = Mockito.mock(WebSocket.class);
    private final TxnContext context = new TxnContext(socket);

    @Test
    public void aCommitFlushesTheCurrentSocket() {
        final WebSocket reconnected = Mockito.mock(WebSocket.class);
        final Txn txn = Txn.get();
        txn.begin(context);
        txn.commit();
        Mockito.verify(socket).flush();

        context.setSocket(reconnected);
        txn.begin(context);
        txn.commit();

        assertSame(reconnected, context.getSocket());
        Mockito.verify(reconnected).flush();
        Mockito.verify(socket, Mockito.times(1)).flush();
    }

    @Test
    public void asAListenerItDoesNothing() {
        context.beforeFlush(context);
        context.afterFlush(context);
        context.beforeRollback();
        Mockito.verifyNoInteractions(socket);
    }

    @Test
    public void withoutApplicationTheApplicationCallsAreIgnored() {
        context.setAttribute("key", "value");
        context.registerUIContext(Mockito.mock(UIContext.class));
        context.deregisterUIContext(1);

        assertNull(context.getApplication());
        assertNull(context.getAttribute("key"));
        assertNull(context.getId());
    }

    @Test
    public void withAnApplicationTheCallsAreDelegated() {
        final Application application = Mockito.mock(Application.class);
        final UIContext uiContext = Mockito.mock(UIContext.class);
        Mockito.when(application.getAttribute("key")).thenReturn("value");
        Mockito.when(application.getId()).thenReturn("app-1");
        context.setApplication(application);

        context.setAttribute("other", 2);
        context.registerUIContext(uiContext);
        context.deregisterUIContext(7);

        assertSame(application, context.getApplication());
        assertEquals("value", context.getAttribute("key"));
        assertEquals("app-1", context.getId());
        Mockito.verify(application).setAttribute("other", 2);
        Mockito.verify(application).registerUIContext(uiContext);
        Mockito.verify(application).deregisterUIContext(7);
    }

    @Test
    public void itNamesItsWriter() {
        assertEquals("TxnContext{modelWriter=" + context.getWriter() + '}', context.toString());
    }
}
