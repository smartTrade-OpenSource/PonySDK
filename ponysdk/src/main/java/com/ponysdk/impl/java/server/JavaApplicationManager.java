/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.impl.java.server;

import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.HotReload;
import com.ponysdk.core.ui.main.EntryPoint;

public class JavaApplicationManager extends ApplicationManager {

    /** With {@code ApplicationConfiguration.setHotReload(true)} and the classes in a directory; null otherwise. */
    private HotReload hotReload;

    @Override
    public void start() {
        if (configuration.isHotReload()) hotReload = HotReload.of(configuration.getEntryPointClass(), HotReload::reloadPages);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (hotReload != null) hotReload.close();
    }

    @Override
    protected EntryPoint initializeEntryPoint() {
        final Class<? extends EntryPoint> entryPointClass = hotReload != null ? hotReload.current(EntryPoint.class, configuration.getEntryPointClass())
                : configuration.getEntryPointClass();
        try {
            return entryPointClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}
