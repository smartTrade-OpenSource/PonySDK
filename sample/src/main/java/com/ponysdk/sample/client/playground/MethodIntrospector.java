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

package com.ponysdk.sample.client.playground;

import java.util.List;

/**
 * Introspects a component class to discover setter methods.
 * <p>
 * Discovers all public methods starting with "set", extracts their complete
 * signatures including parameter information, and filters out methods with
 * more than 3 parameters.
 * </p>
 */
public interface MethodIntrospector {

    /**
     * Discovers setter methods for the given component class.
     *
     * @param componentClass the class to introspect, must not be null
     * @return a list of method signatures sorted alphabetically by method name, never null
     */
    List<MethodSignature> discoverSetters(Class<?> componentClass);
}
