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

package com.ponysdk.core.ui.wa.codegen;

import java.util.List;

/**
 * Internal model representing a parsed Web Awesome component from the
 * Custom Elements Manifest ({@code custom-elements.json}).
 * <p>
 * The Code Generator populates instances of this record after parsing
 * the manifest, then uses them to drive Java wrapper, props record,
 * TypeScript interface, and Javadoc generation.
 * </p>
 *
 * @param tagName       the HTML custom element tag (e.g. {@code "wa-input"})
 * @param className     the class name from the manifest (e.g. {@code "WaInput"})
 * @param summary       short description of the component
 * @param jsDoc         full documentation extracted from the manifest
 * @param properties    list of the component's public properties
 * @param events        list of events emitted by the component
 * @param slots         list of named slots for content composition
 * @param cssParts      list of CSS shadow parts exposed by the component
 * @param cssProperties list of CSS custom properties supported by the component
 * @param status        component maturity: {@code "stable"}, {@code "experimental"}, or {@code "deprecated"}
 */
public record ComponentDefinition(
    String tagName,
    String className,
    String summary,
    String jsDoc,
    List<PropertyDef> properties,
    List<EventDef> events,
    List<SlotDef> slots,
    List<CssPartDef> cssParts,
    List<CssPropertyDef> cssProperties,
    String status
) {}
