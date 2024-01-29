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

package com.ponysdk.core.server.application;

import com.ponysdk.core.server.context.UIContext;
import com.ponysdk.core.server.context.UIContextFactory;
import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.ui.main.EntryPoint;

public class JavaApplicationManager extends ApplicationManager {
    private final UIContextFactory uiContextFactory;

    public JavaApplicationManager() {
        uiContextFactory = new UIContextFactory() {
            @Override
            public UIContext create() {
                return new UIContextImpl(this);
            }
        };
    }

    @Override
    public UIContextFactory getUIContextFactory() {
        return uiContextFactory;
    }

    @Override
    protected EntryPoint initializeEntryPoint() {
        final Class<? extends EntryPoint> entryPointClassName = configuration.getEntryPointClass();
        try {
            return entryPointClassName.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}