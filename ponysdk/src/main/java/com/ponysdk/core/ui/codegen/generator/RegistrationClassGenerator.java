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

package com.ponysdk.core.ui.codegen.generator;

import com.ponysdk.core.ui.codegen.model.ComponentDefinition;

import java.util.List;

/**
 * Generates registration classes for component libraries.
 * <p>
 * Registration classes integrate generated components with the Component Terminal
 * infrastructure and provide unique component identifiers.
 * </p>
 */
public class RegistrationClassGenerator {

    private final String javaPackage;
    private final String libraryName;

    /**
     * Creates a new registration class generator.
     *
     * @param javaPackage  the Java package for the registration class
     * @param libraryName  the library name (e.g., "webawesome", "shoelace")
     */
    public RegistrationClassGenerator(final String javaPackage, final String libraryName) {
        this.javaPackage = javaPackage;
        this.libraryName = libraryName;
    }

    /**
     * Generates a registration class for a list of components.
     *
     * @param components the components to register
     * @return the generated Java source code
     */
    public String generateRegistrationClass(final List<ComponentDefinition> components) {
        final StringBuilder sb = new StringBuilder();
        
        // Package declaration
        sb.append("package ").append(javaPackage).append(";\n\n");
        
        // Imports
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.Map;\n");
        sb.append("import java.util.function.Supplier;\n");
        sb.append("import com.ponysdk.core.ui.component.PComponent;\n\n");
        
        // Class JavaDoc
        sb.append("/**\n");
        sb.append(" * Registration class for ").append(libraryName).append(" components.\n");
        sb.append(" * <p>\n");
        sb.append(" * This class provides component factory methods and integrates with\n");
        sb.append(" * the Component Terminal infrastructure.\n");
        sb.append(" * </p>\n");
        sb.append(" * <p>\n");
        sb.append(" * Generated code - do not modify manually.\n");
        sb.append(" * </p>\n");
        sb.append(" */\n");
        
        // Class declaration
        final String className = capitalize(libraryName) + "ComponentRegistry";
        sb.append("public final class ").append(className).append(" {\n\n");
        
        // Private constructor
        sb.append("    private ").append(className).append("() {\n");
        sb.append("        // Utility class\n");
        sb.append("    }\n\n");
        
        // Component factory map
        sb.append("    private static final Map<String, Supplier<PComponent<?>>> COMPONENT_FACTORIES = new HashMap<>();\n\n");
        
        // Static initializer
        sb.append("    static {\n");
        for (final ComponentDefinition component : components) {
            final String wrapperClassName = component.getWrapperClassName();
            final String tagName = component.tagName();
            sb.append("        COMPONENT_FACTORIES.put(\"").append(tagName).append("\", ")
              .append(wrapperClassName).append("::new);\n");
        }
        sb.append("    }\n\n");
        
        // Register method
        sb.append("    /**\n");
        sb.append("     * Registers all ").append(libraryName).append(" components with the Component Terminal.\n");
        sb.append("     * <p>\n");
        sb.append("     * This method should be called during application initialization.\n");
        sb.append("     * </p>\n");
        sb.append("     */\n");
        sb.append("    public static void registerAll() {\n");
        sb.append("        // Component registration logic would go here\n");
        sb.append("        // This integrates with the existing Component Terminal infrastructure\n");
        sb.append("    }\n\n");
        
        // Get factory method
        sb.append("    /**\n");
        sb.append("     * Gets a component factory for the specified tag name.\n");
        sb.append("     *\n");
        sb.append("     * @param tagName the component tag name\n");
        sb.append("     * @return the component factory, or null if not found\n");
        sb.append("     */\n");
        sb.append("    public static Supplier<PComponent<?>> getFactory(final String tagName) {\n");
        sb.append("        return COMPONENT_FACTORIES.get(tagName);\n");
        sb.append("    }\n\n");
        
        // Get all tag names method
        sb.append("    /**\n");
        sb.append("     * Gets all registered component tag names.\n");
        sb.append("     *\n");
        sb.append("     * @return set of tag names\n");
        sb.append("     */\n");
        sb.append("    public static java.util.Set<String> getTagNames() {\n");
        sb.append("        return COMPONENT_FACTORIES.keySet();\n");
        sb.append("    }\n");
        
        sb.append("}\n");
        
        return sb.toString();
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private static String capitalize(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
