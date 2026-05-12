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

package com.ponysdk.test.inspector;

import java.util.function.Predicate;

import com.ponysdk.core.ui.basic.PWidget;

/**
 * Contract for typed inspector lookup.
 *
 * <p>Every specialized inspector (a subclass of {@link InspectorWidget}) exposes a
 * {@code public static final InspectorFactory<...> FACTORY} instance implementing this
 * interface. The factory pairs the two pieces of information needed to find and wrap
 * a widget:
 *
 * <ul>
 *   <li>{@link #basePredicate()} — how to recognize widgets this inspector can wrap
 *       (for example, {@code InspectorListBox} matches widgets carrying the
 *       {@code dd-listbox} CSS class).</li>
 *   <li>{@link #create(PWidget)} — how to construct the inspector around a matched
 *       widget.</li>
 * </ul>
 *
 * <p>This pair is consumed by the typed lookup methods on {@link InspectorWidget}:
 *
 * <pre>{@code
 * InspectorListBox lb     = panel.find(InspectorListBox.FACTORY);
 * InspectorListBox byAttr = panel.find(InspectorListBox.FACTORY, attr("aria-label", "account"));
 * List<InspectorListBox> all = panel.findAll(InspectorListBox.FACTORY);
 * }</pre>
 *
 * <p>Extra predicates passed to {@code find}/{@code findAll} are AND-combined with the
 * factory's base predicate, so callers can narrow down the match without losing the
 * typed return.
 *
 * <h2>Extension pattern</h2>
 *
 * <p>To create a custom inspector for a project-specific composite widget:
 *
 * <ol>
 *   <li>Extend {@link InspectorWidget}.</li>
 *   <li>Declare a {@code public static final InspectorFactory<YourInspector> FACTORY}
 *       field that returns a base predicate identifying your widget and a
 *       {@code create} method that wraps it.</li>
 *   <li>Expose domain-specific methods that delegate to {@link InspectorWidget#find}
 *       and the protected event-firing helpers.</li>
 * </ol>
 *
 * <p>Example — an inspector for a price-spinner composite widget:
 *
 * <pre>{@code
 * public class InspectorPriceSpinner extends InspectorWidget {
 *
 *     public static final InspectorFactory<InspectorPriceSpinner> FACTORY =
 *             new InspectorFactory<InspectorPriceSpinner>() {
 *                 public Predicate<PWidget> basePredicate() {
 *                     return Predicates.style("price-spinner");
 *                 }
 *                 public InspectorPriceSpinner create(PWidget widget) {
 *                     return new InspectorPriceSpinner(widget);
 *                 }
 *             };
 *
 *     public InspectorPriceSpinner(PWidget widget) {
 *         super(widget);
 *     }
 *
 *     public InspectorPriceSpinner increment() {
 *         find(Predicates.style("btn-up")).click();
 *         return this;
 *     }
 *
 *     public String getValue() {
 *         return find(Predicates.style("value-display")).getText();
 *     }
 * }
 * }</pre>
 *
 * <p>Once the factory is declared, the custom inspector participates in typed lookups
 * exactly like the built-in ones:
 *
 * <pre>{@code
 * InspectorPriceSpinner spinner = panel.find(InspectorPriceSpinner.FACTORY);
 * spinner.increment();
 * }</pre>
 *
 * @param <I> the inspector type produced by this factory
 */
public interface InspectorFactory<I extends InspectorWidget> {

    /**
     * The base predicate that identifies widgets this factory can wrap.
     *
     * <p>Typed lookups combine this predicate with any extra predicates passed by the
     * caller using logical AND. The predicate should be as specific as needed to
     * uniquely identify the target widget family (typically a CSS class, widget type,
     * or a combination thereof).
     *
     * @return a non-null predicate matching widgets this factory's inspector can wrap
     */
    Predicate<PWidget> basePredicate();

    /**
     * Creates an inspector instance wrapping the given widget.
     *
     * <p>The widget is guaranteed to have been pre-validated against
     * {@link #basePredicate()} (and any extra predicates) by the caller, so
     * implementations may assume it is of a compatible shape.
     *
     * @param widget the widget to wrap; never {@code null}
     * @return a new inspector wrapping {@code widget}
     */
    I create(PWidget widget);
}
