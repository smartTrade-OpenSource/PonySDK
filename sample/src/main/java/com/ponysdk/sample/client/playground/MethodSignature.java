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

import java.lang.reflect.Method;
import java.util.List;

/**
 * Represents a method signature with parameter information.
 * <p>
 * Immutable value object containing the complete signature of a method including
 * its name, return type, and parameter details.
 * </p>
 *
 * @param method     the reflected method, must not be null
 * @param methodName the method name, must not be null
 * @param returnType the return type class, must not be null
 * @param parameters the list of parameter information, must not be null
 */
public record MethodSignature(
    Method method,
    String methodName,
    Class<?> returnType,
    List<ParameterInfo> parameters
) {
    public MethodSignature {
        if (method == null) throw new IllegalArgumentException("method must not be null");
        if (methodName == null) throw new IllegalArgumentException("methodName must not be null");
        if (returnType == null) throw new IllegalArgumentException("returnType must not be null");
        if (parameters == null) throw new IllegalArgumentException("parameters must not be null");
    }
}
