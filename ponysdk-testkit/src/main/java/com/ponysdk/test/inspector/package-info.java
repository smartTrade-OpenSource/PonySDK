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

/**
 * PonySDK UI TestKit - a reflection-free, inspector-based API for testing PonySDK UI
 * components server-side, without a real browser.
 *
 * <h2>Who this is for</h2>
 * <p>Developers writing JUnit tests against PonySDK widgets. The testkit plugs into the
 * existing {@code PSuite} base class (mocked {@code UIContext} and WebSocket) and lets you
 * drive widgets the way a user would: click, type, keypress, focus, blur, select, filter.
 * It never reaches into private state - every query and action goes through the public
 * {@code PWidget} API or through the same {@code onClientData} path a real browser event
 * would take.</p>
 *
 * <h2>Entry point</h2>
 * <p>{@link com.ponysdk.test.inspector.InspectorWidget#of(com.ponysdk.core.ui.basic.PWidget)}
 * wraps any widget and exposes queries ({@code getText}, {@code isVisible}, {@code isEnabled},
 * {@code hasStyle}), traversal ({@code find}, {@code findAll}, {@code has}) and actions
 * ({@code click}, {@code doubleClick}, {@code focus}, {@code blur}, {@code keyDown},
 * {@code keyUp}, {@code type}).</p>
 *
 * <h2>High-level example</h2>
 * <pre>{@code
 * import static com.ponysdk.test.inspector.predicate.Predicates.style;
 * import static com.ponysdk.test.inspector.predicate.Predicates.text;
 * import static com.ponysdk.test.inspector.predicate.Predicates.type;
 *
 * InspectorWidget panel = InspectorWidget.of(myPanel);
 *
 * // Locate a ListBox and select a value
 * InspectorListBox country = panel.find(InspectorListBox.FACTORY);
 * country.select("France");
 * assertEquals(List.of("France"), country.getSelectedLabels());
 *
 * // Drive a text input and assert on the outcome
 * panel.find(type(PTextBox.class)).type("hello").blur();
 * panel.find(style("submit")).click();
 * assertTrue(panel.has(text("Saved")));
 * }</pre>
 *
 * <h2>Built-in inspectors</h2>
 * <ul>
 *   <li>{@link com.ponysdk.test.inspector.InspectorWidget} - the core inspector, works on any
 *       {@code PWidget}.</li>
 *   <li>{@link com.ponysdk.test.inspector.InspectorListBox} - typed helpers for {@code ListBox}
 *       dropdowns (open/close, select, filter, clear, read back selection).</li>
 *   <li>{@link com.ponysdk.test.inspector.InspectorInfiniteScroll} - simulates
 *       {@code InfiniteScrollAddon} render/scroll events for pagination tests.</li>
 * </ul>
 *
 * <h2>Predicates</h2>
 * <p>{@link com.ponysdk.test.inspector.predicate.Predicates} provides composable factory
 * methods - {@code style}, {@code type}, {@code text}, {@code debugId}, {@code attr},
 * {@code position} - returning standard {@link java.util.function.Predicate} instances that
 * combine naturally with {@link java.util.function.Predicate#and(java.util.function.Predicate)
 * and}, {@link java.util.function.Predicate#or(java.util.function.Predicate) or},
 * {@link java.util.function.Predicate#negate() negate}.</p>
 *
 * <h2>Extending with a custom inspector</h2>
 * <p>Project-specific composite widgets get their own inspector by extending
 * {@link com.ponysdk.test.inspector.InspectorWidget} and declaring a public static
 * {@code FACTORY} of type {@link com.ponysdk.test.inspector.InspectorFactory}. See that
 * interface for a copy-pasteable example.</p>
 *
 * <h2>License</h2>
 * <p>Licensed under the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License, Version 2.0</a>.</p>
 */
package com.ponysdk.test.inspector;
