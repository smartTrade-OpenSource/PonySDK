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

import java.util.Map;

/**
 * Interface for generating Java and TypeScript source code from web component definitions.
 * <p>
 * This interface defines the contract for code generation from {@link ComponentDefinition} objects
 * parsed from Custom Elements Manifest (CEM) files. Implementations generate:
 * </p>
 * <ul>
 *   <li>Java wrapper classes extending {@code PWebComponent}</li>
 *   <li>Java Props records for type-safe component configuration</li>
 *   <li>TypeScript interfaces for browser-side type safety</li>
 *   <li>Java event detail classes for custom event types</li>
 * </ul>
 * <p>
 * The generated code integrates with the existing PonySDK infrastructure including
 * {@code PWebComponent}, {@code PropsDiffer}, {@code Event Bridge}, and {@code Component Terminal}.
 * </p>
 *
 * @see ComponentDefinition
 */
public interface CodeGenerator {

    /**
     * Generates a Java wrapper class for a web component.
     * <p>
     * The generated class extends {@code PWebComponent} and includes:
     * </p>
     * <ul>
     *   <li>Tag name constant</li>
     *   <li>Constructors accepting Props record</li>
     *   <li>Event listener registration methods</li>
     *   <li>Slot management methods</li>
     *   <li>Component method proxies</li>
     *   <li>CSS custom property setters</li>
     * </ul>
     * <p>
     * Example output for a button component:
     * </p>
     * <pre>{@code
     * public class PButton extends PWebComponent<ButtonProps> {
     *     private static final String TAG_NAME = "wa-button";
     *     
     *     public PButton() {
     *         this(ButtonProps.defaults());
     *     }
     *     
     *     public PButton(final ButtonProps initialProps) {
     *         super(initialProps, DECLARED_SLOTS);
     *     }
     *     
     *     public void addClickListener(final Consumer<JsonObject> handler) {
     *         onEvent("wa-click", handler);
     *     }
     * }
     * }</pre>
     *
     * @param def the component definition containing metadata from the CEM
     * @return generated Java source code as a string
     * @throws IllegalArgumentException if the component definition is invalid
     */
    String generateWrapperClass(ComponentDefinition def);

    /**
     * Generates a Java Props record for a web component.
     * <p>
     * The generated record contains all public properties with:
     * </p>
     * <ul>
     *   <li>Properly mapped Java types</li>
     *   <li>JavaDoc comments from CEM descriptions</li>
     *   <li>Default values factory method</li>
     *   <li>Builder methods for fluent API</li>
     * </ul>
     * <p>
     * Example output:
     * </p>
     * <pre>{@code
     * public record ButtonProps(
     *     String variant,
     *     String size,
     *     boolean disabled
     * ) {
     *     public static ButtonProps defaults() {
     *         return new ButtonProps("neutral", "medium", false);
     *     }
     *     
     *     public ButtonProps withVariant(String variant) {
     *         return new ButtonProps(variant, size, disabled);
     *     }
     * }
     * }</pre>
     *
     * @param def the component definition containing property metadata
     * @return generated Java source code as a string
     * @throws IllegalArgumentException if the component definition is invalid
     */
    String generatePropsRecord(ComponentDefinition def);

    /**
     * Generates TypeScript interfaces for a web component.
     * <p>
     * The generated TypeScript includes:
     * </p>
     * <ul>
     *   <li>Props interface with properly mapped TypeScript types</li>
     *   <li>JSDoc comments from CEM descriptions</li>
     *   <li>Type guard function for runtime validation</li>
     *   <li>Event detail interfaces for custom events</li>
     * </ul>
     * <p>
     * Example output:
     * </p>
     * <pre>{@code
     * export interface ButtonProps {
     *     variant?: 'primary' | 'success' | 'neutral';
     *     size?: 'small' | 'medium' | 'large';
     *     disabled?: boolean;
     * }
     * 
     * export function isButtonProps(obj: unknown): obj is ButtonProps {
     *     if (typeof obj !== 'object' || obj === null) return false;
     *     // ... type checks
     *     return true;
     * }
     * }</pre>
     *
     * @param def the component definition containing property and event metadata
     * @return generated TypeScript source code as a string
     * @throws IllegalArgumentException if the component definition is invalid
     */
    String generateTypeScriptInterface(ComponentDefinition def);

    /**
     * Generates Java event detail classes for custom event types.
     * <p>
     * For each event with a custom detail type (not void or empty), generates a Java class
     * or record representing the event payload structure. These classes are used for
     * type-safe event handling.
     * </p>
     * <p>
     * Example: For an event with detail type {@code { x: number, y: number }}, generates:
     * </p>
     * <pre>{@code
     * public record ClickEventDetail(double x, double y) {}
     * }</pre>
     *
     * @param def the component definition containing event metadata
     * @return map of class name to generated Java source code for each event detail class
     * @throws IllegalArgumentException if the component definition is invalid
     */
    Map<String, String> generateEventDetailClasses(ComponentDefinition def);
}
