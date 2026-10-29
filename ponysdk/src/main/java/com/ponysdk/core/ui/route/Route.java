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

package com.ponysdk.core.ui.route;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes a view class the target of a path of a {@link Router}. The view implements
 * {@link com.ponysdk.core.ui.basic.IsPWidget}; its constructor takes the {@link RouteParameters}, or nothing.
 * <p>
 * The path is a template of segments separated by {@code /}: a literal segment ({@code orders}) or a named parameter
 * ({@code :id}). {@code orders/:id} matches {@code orders/42}; at the same position a literal beats a parameter
 * ({@code orders/new} is chosen over {@code orders/:id}). The empty path is the router's root.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Route {

    /**
     * The path templates, relative to the router's base. A view with several is reached by each; the first is the one
     * {@link Router#navigate(Class, java.util.Map)} writes ({@code @Route({"orders", ""})}: the list is also the root).
     */
    String[] value();
}
