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

import org.mockito.Mockito;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.SessionManager;

import jakarta.servlet.http.HttpSession;

/**
 * A live screen that never leaves, as a client that does not go: a drain lasts its whole window. Unregistered when closed.
 */
final class LingeringScreen implements AutoCloseable {

    private final Application application = new Application("lingering", Mockito.mock(HttpSession.class), new ApplicationConfiguration());

    LingeringScreen() {
        final UIContext screen = Mockito.mock(UIContext.class);
        Mockito.when(screen.isAlive()).thenReturn(true);
        application.registerUIContext(screen);
        SessionManager.get().registerApplication(application);
    }

    @Override
    public void close() {
        SessionManager.get().unregisterApplication(application);
    }
}
