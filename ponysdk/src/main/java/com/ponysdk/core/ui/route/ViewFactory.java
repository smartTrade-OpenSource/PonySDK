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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import com.ponysdk.core.ui.basic.IsPWidget;

/** Builds the instances of a view class: its constructor takes the {@link RouteParameters}, or nothing. */
final class ViewFactory {

    private final Class<?> view;
    private final Constructor<?> constructor;
    private final boolean withParameters;

    /** @throws IllegalArgumentException when the class cannot be a view */
    ViewFactory(final Class<?> view) {
        if (!IsPWidget.class.isAssignableFrom(view) || Modifier.isAbstract(view.getModifiers())) {
            throw new IllegalArgumentException(view.getName() + " must be a concrete class implementing IsPWidget");
        }
        Constructor<?> found;
        boolean parameters = true;
        try {
            found = view.getDeclaredConstructor(RouteParameters.class);
        } catch (final NoSuchMethodException e) {
            parameters = false;
            try {
                found = view.getDeclaredConstructor();
            } catch (final NoSuchMethodException none) {
                throw new IllegalArgumentException(view.getName() + " needs a constructor taking RouteParameters, or none", none);
            }
        }
        found.setAccessible(true); // a view is often package-private, next to the screen that uses it
        this.view = view;
        this.constructor = found;
        this.withParameters = parameters;
    }

    IsPWidget create(final RouteParameters parameters) {
        try {
            return (IsPWidget) (withParameters ? constructor.newInstance(parameters) : constructor.newInstance());
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create " + view.getName(), e);
        }
    }
}
