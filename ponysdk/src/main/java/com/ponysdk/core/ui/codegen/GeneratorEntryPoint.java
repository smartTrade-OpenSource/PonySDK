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

package com.ponysdk.core.ui.codegen;

import com.ponysdk.core.ui.codegen.model.*;
import com.ponysdk.core.ui.codegen.parser.CEMParser;
import com.ponysdk.core.ui.codegen.parser.DefaultCEMParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Entry point to view all components from a CEM file.
 * <p>
 * Usage: java GeneratorEntryPoint [cemFile]
 * </p>
 */
public class GeneratorEntryPoint {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java GeneratorEntryPoint <cemFile>");
            System.err.println("Example: java GeneratorEntryPoint ponysdk/src/main/data/custom-elements.json");
            System.exit(1);
        }

        final Path cemFile = Paths.get(args[0]);

        System.out.println("═".repeat(100));
        System.out.println("                        WEB COMPONENT VIEWER");
        System.out.println("═".repeat(100));
        System.out.println("CEM File: " + cemFile);
        System.out.println("═".repeat(100));
        System.out.println();

        try {
            // Parse CEM file
            final CEMParser parser = new DefaultCEMParser();
            final List<ComponentDefinition> components = parser.parse(cemFile);

            System.out.println("Found " + components.size() + " components\n");

            // Display each component
            for (int i = 0; i < components.size(); i++) {
                final ComponentDefinition component = components.get(i);
                printComponent(i + 1, component);
            }

            // Print summary
            printSummary(components);

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printComponent(int index, ComponentDefinition component) {
        System.out.println("┌─ Component #" + index + " " + "─".repeat(90));
        System.out.println("│");
        System.out.println("│ Tag Name:     " + component.tagName());
        System.out.println("│ Class Name:   " + component.className());
        System.out.println("│ Wrapper:      P" + component.getWrapperClassName());
        System.out.println("│ Props:        " + component.getPropsClassName());
        System.out.println("│ Status:       " + component.status());
        
        if (component.summary() != null && !component.summary().isEmpty()) {
            System.out.println("│ Summary:      " + component.summary());
        }
        
        System.out.println("│");

        // Properties
        if (!component.properties().isEmpty()) {
            System.out.println("│ ┌─ Properties (" + component.properties().size() + ")");
            for (PropertyDef prop : component.properties()) {
                final String required = prop.required() ? " [REQUIRED]" : "";
                final String defaultVal = prop.defaultValue() != null ? " = " + prop.defaultValue() : "";
                System.out.println("│ │  • " + prop.name() + ": " + prop.cemType() + 
                                 " → " + prop.javaType() + required + defaultVal);
                if (prop.description() != null && !prop.description().isEmpty()) {
                    System.out.println("│ │    " + prop.description());
                }
            }
            System.out.println("│ └─");
            System.out.println("│");
        }

        // Events
        if (!component.events().isEmpty()) {
            System.out.println("│ ┌─ Events (" + component.events().size() + ")");
            for (EventDef event : component.events()) {
                final String bubbles = event.bubbles() ? " [BUBBLES]" : "";
                final String cancelable = event.cancelable() ? " [CANCELABLE]" : "";
                System.out.println("│ │  • " + event.name() + bubbles + cancelable);
                if (event.description() != null && !event.description().isEmpty()) {
                    System.out.println("│ │    " + event.description());
                }
                if (event.detailType() != null) {
                    System.out.println("│ │    Detail: " + event.detailType());
                }
            }
            System.out.println("│ └─");
            System.out.println("│");
        }

        // Slots
        if (!component.slots().isEmpty()) {
            System.out.println("│ ┌─ Slots (" + component.slots().size() + ")");
            for (SlotDef slot : component.slots()) {
                final String slotName = slot.name().isEmpty() ? "(default)" : slot.name();
                System.out.println("│ │  • " + slotName);
                if (slot.description() != null && !slot.description().isEmpty()) {
                    System.out.println("│ │    " + slot.description());
                }
            }
            System.out.println("│ └─");
            System.out.println("│");
        }

        // Methods
        if (!component.methods().isEmpty()) {
            System.out.println("│ ┌─ Methods (" + component.methods().size() + ")");
            for (MethodDef method : component.methods()) {
                final String async = method.async() ? " [ASYNC]" : "";
                final String params = method.parameters().isEmpty() ? "()" : 
                    "(" + method.parameters().stream()
                        .map(p -> p.name() + ": " + p.cemType())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("") + ")";
                System.out.println("│ │  • " + method.name() + params + ": " + method.returnType() + async);
                if (method.description() != null && !method.description().isEmpty()) {
                    System.out.println("│ │    " + method.description());
                }
            }
            System.out.println("│ └─");
            System.out.println("│");
        }

        // CSS Properties
        if (!component.cssProperties().isEmpty()) {
            System.out.println("│ ┌─ CSS Properties (" + component.cssProperties().size() + ")");
            for (CssPropertyDef css : component.cssProperties()) {
                final String defaultVal = css.defaultValue() != null ? " = " + css.defaultValue() : "";
                System.out.println("│ │  • " + css.name() + defaultVal);
                if (css.description() != null && !css.description().isEmpty()) {
                    System.out.println("│ │    " + css.description());
                }
            }
            System.out.println("│ └─");
            System.out.println("│");
        }

        System.out.println("└" + "─".repeat(99));
        System.out.println();
    }

    private static void printSummary(List<ComponentDefinition> components) {
        int totalProperties = 0;
        int totalEvents = 0;
        int totalSlots = 0;
        int totalMethods = 0;
        int totalCssProps = 0;

        for (ComponentDefinition component : components) {
            totalProperties += component.properties().size();
            totalEvents += component.events().size();
            totalSlots += component.slots().size();
            totalMethods += component.methods().size();
            totalCssProps += component.cssProperties().size();
        }

        System.out.println("═".repeat(100));
        System.out.println("                                 SUMMARY");
        System.out.println("═".repeat(100));
        System.out.println();
        System.out.println("  Total Components:     " + components.size());
        System.out.println("  Total Properties:     " + totalProperties);
        System.out.println("  Total Events:         " + totalEvents);
        System.out.println("  Total Slots:          " + totalSlots);
        System.out.println("  Total Methods:        " + totalMethods);
        System.out.println("  Total CSS Properties: " + totalCssProps);
        System.out.println();
        System.out.println("═".repeat(100));
    }
}
