/*
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

package com.ponysdk.core.server.application;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;

public class UIContextTest {

    @Test
    public void testMissingUpgradeRequestDataReadsAsEmpty() {
        final UIContext uiContext = new UIContext(Mockito.mock(WebSocket.class), Mockito.mock(TxnContext.class),
                Mockito.mock(ApplicationConfiguration.class), null);

        assertTrue(uiContext.getParameterMap().isEmpty());
        assertTrue(uiContext.getHeaders().isEmpty());
        assertTrue(uiContext.getRequestCookies().isEmpty());
        assertNull(uiContext.getHistoryToken());
        assertNull(uiContext.getSession());
        assertNull(uiContext.getHost());
        assertNull(uiContext.getRemoteSocketAddress());
        assertFalse(uiContext.isSecure());
    }
}
