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
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link MethodIntrospector} that uses Java reflection
 * to discover setter methods on component classes.
 * <p>
 * This introspector:
 * <ul>
 *   <li>Finds all public methods starting with "set"</li>
 *   <li>Filters out methods with more than 3 parameters</li>
 *   <li>Extracts complete method signatures with parameter information</li>
 *   <li>Detects Optional parameters and their wrapped types</li>
 *   <li>Sorts methods alphabetically by name</li>
 * </ul>
 * </p>
 */
public class DefaultMethodIntrospector implements MethodIntrospector {

    private static final Logger LOGGER = Logger.getLogger(DefaultMethodIntrospector.class.getName());
    private static final int MAX_PARAMETERS = 3;

    /**
     * Discovers setter methods for the given component class.
     * <p>
     * Scans all public methods, filters those starting with "set" and having
     * 3 or fewer parameters, extracts their complete signatures, and returns
     * them sorted alphabetically by method name.
     * </p>
     *
     * @param componentClass the class to introspect, must not be null
     * @return a list of method signatures sorted alphabetically by method name, never null
     * @throws IllegalArgumentException if componentClass is null
     */
    @Override
    public List<MethodSignature> discoverSetters(final Class<?> componentClass) {
        if (componentClass == null) {
            throw new IllegalArgumentException("componentClass must not be null");
        }

        final List<MethodSignature> signatures = new ArrayList<>();

        try {
            // Get all public methods from the class and its superclasses
            final Method[] methods = componentClass.getMethods();

            for (final Method method : methods) {
                // Check if method is a setter (public, starts with "set")
                if (isSetterMethod(method)) {
                    // Filter out methods with more than 3 parameters
                    if (method.getParameterCount() <= MAX_PARAMETERS) {
                        try {
                            final MethodSignature signature = extractMethodSignature(method);
                            signatures.add(signature);
                        } catch (final Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to extract signature for method: " + method.getName(), e);
                        }
                    }
                }
            }

            // Sort alphabetically by method name
            Collections.sort(signatures, (s1, s2) -> s1.methodName().compareTo(s2.methodName()));

            LOGGER.log(Level.FINE, "Discovered {0} setter methods for class {1}",
                new Object[]{signatures.size(), componentClass.getSimpleName()});

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to discover setters for class: " + componentClass.getName(), e);
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(signatures);
    }

    /**
     * Checks if a method is a public setter method.
     *
     * @param method the method to check
     * @return true if the method is public and starts with "set"
     */
    private boolean isSetterMethod(final Method method) {
        if (!Modifier.isPublic(method.getModifiers()) || !method.getName().startsWith("set")) {
            return false;
        }
        
        // Filter out methods from PWidget and IsPWidget to show only component-specific setters
        final Class<?> declaringClass = method.getDeclaringClass();
        final String className = declaringClass.getName();
        
        // Exclude methods from PWidget, IsPWidget, PObject, and other base classes
        if (className.startsWith("com.ponysdk.core.ui.basic.PWidget") ||
            className.startsWith("com.ponysdk.core.ui.basic.IsPWidget") ||
            className.startsWith("com.ponysdk.core.ui.basic.PObject") ||
            className.startsWith("com.ponysdk.core.ui.basic.HasP") ||
            className.equals("java.lang.Object")) {
            return false;
        }
        
        return true;
    }

    /**
     * Extracts the complete method signature including parameter information.
     *
     * @param method the method to extract signature from
     * @return the method signature with complete parameter information
     */
    private MethodSignature extractMethodSignature(final Method method) {
        final List<ParameterInfo> parameters = new ArrayList<>();

        // Extract parameter information
        final Parameter[] methodParams = method.getParameters();
        for (final Parameter param : methodParams) {
            final ParameterInfo paramInfo = extractParameterInfo(param);
            parameters.add(paramInfo);
        }

        return new MethodSignature(
            method,
            method.getName(),
            method.getReturnType(),
            Collections.unmodifiableList(parameters)
        );
    }

    /**
     * Extracts parameter information including Optional detection.
     *
     * @param param the parameter to extract information from
     * @return the parameter information
     */
    private ParameterInfo extractParameterInfo(final Parameter param) {
        final Class<?> paramType = param.getType();
        final String paramName = param.getName();

        // Check if parameter is Optional
        if (Optional.class.isAssignableFrom(paramType)) {
            final Class<?> wrappedType = extractOptionalWrappedType(param);
            return new ParameterInfo(paramName, paramType, true, wrappedType);
        }

        return new ParameterInfo(paramName, paramType, false, null);
    }

    /**
     * Extracts the type wrapped by an Optional parameter.
     *
     * @param param the Optional parameter
     * @return the wrapped type, or Object.class if unable to determine
     */
    private Class<?> extractOptionalWrappedType(final Parameter param) {
        try {
            final Type genericType = param.getParameterizedType();

            if (genericType instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) genericType;
                final Type[] typeArguments = parameterizedType.getActualTypeArguments();

                if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                    return (Class<?>) typeArguments[0];
                }
            }
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to extract Optional wrapped type for parameter: " + param.getName(), e);
        }

        // Default to Object.class if unable to determine wrapped type
        return Object.class;
    }
}
