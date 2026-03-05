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

package com.ponysdk.sample.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.sample.client.playground.ComponentPlayground;

/**
 * EntryPoint for the Component Playground.
 * <p>
 * Provides an interactive interface for testing Web Awesome components with
 * real-time property manipulation.
 * </p>
 */
public class ComponentPlaygroundEntryPoint implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(ComponentPlaygroundEntryPoint.class);

    @Override
    public void start(final UIContext uiContext) {
        log.info("=== Component Playground EntryPoint ===");
        Txn.get().flush();

    // Wait a bit for the registry to load, then create the playground
        log.info("Creating Component Playground...");

        final ComponentPlayground playground = new ComponentPlayground();
        PWindow.getMain().add(playground);

        log.info("Component Playground initialized successfully");
    }
}
