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

package com.ponysdk.core.ui.codegen.dependency;

/**
 * Exception thrown when circular dependencies are detected in component definitions.
 * <p>
 * Circular dependencies prevent proper code generation because components would
 * reference each other in a way that makes compilation impossible.
 * </p>
 * <p>
 * Example: If component A depends on component B, and component B depends on component A,
 * neither can be generated first.
 * </p>
 */
public class CircularDependencyException extends RuntimeException {

    /**
     * Creates a new circular dependency exception with the specified message.
     *
     * @param message the detail message describing the circular dependencies
     */
    public CircularDependencyException(final String message) {
        super(message);
    }

    /**
     * Creates a new circular dependency exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public CircularDependencyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
