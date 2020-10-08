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

package com.ponysdk.core.server.concurrent;

import com.ponysdk.core.server.application.UIContext;

import java.util.function.Consumer;

public final class UIDelegate<R> implements Consumer<R> {

    private final Consumer<R> consumer;
    private final UIContext uiContext;

    public UIDelegate(final Consumer<R> callback) {
        this.uiContext = UIContext.get();
        this.consumer = callback;
    }

    public UIDelegate(final Consumer<R> callback, final UIContext uiContext) {
        this.uiContext = uiContext;
        this.consumer = callback;
    }

    @Override
    public void accept(final R t) {
        uiContext.execute(() -> consumer.accept(t));
    }

}
