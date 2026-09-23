/*
 * Copyright (c) 2019 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
import static org.junit.Assert.assertFalse;

import org.eclipse.jetty.server.HttpConfiguration;
import org.junit.Test;

public class PonySDKServerTest {

    @Test
    public void createHttpConfiguration_defaultsToJettyRequestHeaderSize() {
        final PonySDKServer server = new PonySDKServer();
        final HttpConfiguration config = server.createHttpConfiguration();

        // When no requestHeaderSize is set, Jetty's default (8192) is preserved
        assertEquals(8192, config.getRequestHeaderSize());
    }

    // ST-58470: requestHeaderSize must be injectable from subclasses (CoreUI/SMS)
    @Test
    public void createHttpConfiguration_appliesCustomRequestHeaderSize() {
        final PonySDKServer server = new PonySDKServer();
        server.setRequestHeaderSize(16384);
        final HttpConfiguration config = server.createHttpConfiguration();

        assertEquals(16384, config.getRequestHeaderSize());
    }

    @Test
    public void createHttpConfiguration_doesNotSendServerVersion() {
        final PonySDKServer server = new PonySDKServer();
        final HttpConfiguration config = server.createHttpConfiguration();

        assertFalse(config.getSendServerVersion());
    }

    @Test
    public void createHttpConfiguration_doesNotSendDateHeader() {
        final PonySDKServer server = new PonySDKServer();
        final HttpConfiguration config = server.createHttpConfiguration();

        assertFalse(config.getSendDateHeader());
    }
}
