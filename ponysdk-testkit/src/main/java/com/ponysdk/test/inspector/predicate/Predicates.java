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

package com.ponysdk.test.inspector.predicate;

import java.util.Objects;
import java.util.function.Predicate;

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBoxBase;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.test.inspector.WidgetText;

/**
 * Factory methods for building composable widget predicates used during testkit traversal.
 * <p>
 * All factories return standard {@link Predicate} instances so callers can combine them with
 * {@link Predicate#and(Predicate)}, {@link Predicate#or(Predicate)} and {@link Predicate#negate()}.
 * Each returned predicate overrides {@link Object#toString()} to produce a human readable
 * description that is used in failure messages when a widget lookup misses.
 * </p>
 * <p>
 * Typical usage:
 * </p>
 * <pre>
 *     inspector.find(style("btn", "primary"));
 *     inspector.find(type(PButton.class).and(text("Save")));
 *     inspector.find(debugId("submit"));
 *     inspector.find(attr("aria-label", "account"));
 *     inspector.findAll(style("dd-listbox").and(position(2))); // third ListBox
 * </pre>
 */
public final class Predicates {

    private Predicates() {
        // utility class
    }

    /**
     * Matches widgets that carry every one of the given style names.
     * <p>
     * An empty {@code styleNames} array produces a predicate that matches every widget. Each
     * style name is checked through {@link PWidget#hasStyleName(String)}.
     * </p>
     *
     * @param styleNames the style names the widget must have (all of them)
     * @return a predicate matching widgets that have all supplied style names
     */
    public static Predicate<PWidget> style(final String... styleNames) {
        final String[] names = styleNames == null ? new String[0] : styleNames.clone();
        return new Predicate<PWidget>() {
            @Override
            public boolean test(final PWidget widget) {
                if (widget == null) return false;
                for (final String name : names) {
                    if (name == null || !widget.hasStyleName(name)) return false;
                }
                return true;
            }

            @Override
            public String toString() {
                return "style(" + String.join(", ", names) + ")";
            }
        };
    }

    /**
     * Matches widgets that are instances of the supplied class.
     *
     * @param widgetClass the class (or superclass/interface) the widget must be an instance of
     * @param <T>         the widget sub-type
     * @return a predicate matching widgets assignable to {@code widgetClass}
     */
    public static <T extends PWidget> Predicate<PWidget> type(final Class<T> widgetClass) {
        Objects.requireNonNull(widgetClass, "widgetClass");
        return new Predicate<PWidget>() {
            @Override
            public boolean test(final PWidget widget) {
                return widgetClass.isInstance(widget);
            }

            @Override
            public String toString() {
                return "type(" + widgetClass.getSimpleName() + ")";
            }
        };
    }

    /**
     * Matches widgets whose displayed text equals the given string.
     * <p>
     * Text is resolved following the same rules as {@code InspectorWidget.getText()}:
     * </p>
     * <ul>
     *     <li>{@link PHTML} &rarr; {@code getHTML()}</li>
     *     <li>{@link PLabel} &rarr; {@code getText()}</li>
     *     <li>{@link PButton} / {@link PCheckBox} &rarr; {@code getText()}</li>
     *     <li>{@link PTextBoxBase} &rarr; {@code getText()}</li>
     *     <li>{@link PElement} &rarr; {@code getInnerText()}</li>
     *     <li>Any other widget type &rarr; {@code null} (never matches)</li>
     * </ul>
     *
     * @param expectedText the expected text value (may be {@code null} to match widgets with no text)
     * @return a predicate matching widgets whose text equals {@code expectedText}
     */
    public static Predicate<PWidget> text(final String expectedText) {
        return new Predicate<PWidget>() {
            @Override
            public boolean test(final PWidget widget) {
                if (widget == null) return false;
                return Objects.equals(expectedText, extractText(widget));
            }

            @Override
            public String toString() {
                return "text(" + expectedText + ")";
            }
        };
    }

    /**
     * Matches widgets whose debug ID equals the given value.
     *
     * @param id the expected debug ID
     * @return a predicate matching widgets whose {@link PWidget#getDebugID()} equals {@code id}
     */
    public static Predicate<PWidget> debugId(final String id) {
        return new Predicate<PWidget>() {
            @Override
            public boolean test(final PWidget widget) {
                if (widget == null) return false;
                return Objects.equals(id, widget.getDebugID());
            }

            @Override
            public String toString() {
                return "debugId(" + id + ")";
            }
        };
    }

    /**
     * Matches widgets whose attribute with the given name equals the supplied value.
     *
     * @param name  the attribute name (must not be {@code null})
     * @param value the expected attribute value (may be {@code null} to match missing attributes)
     * @return a predicate matching widgets whose attribute {@code name} equals {@code value}
     */
    public static Predicate<PWidget> attr(final String name, final String value) {
        Objects.requireNonNull(name, "name");
        return new Predicate<PWidget>() {
            @Override
            public boolean test(final PWidget widget) {
                if (widget == null) return false;
                return Objects.equals(value, widget.getAttribute(name));
            }

            @Override
            public String toString() {
                return "attr(" + name + "=" + value + ")";
            }
        };
    }

    /**
     * Matches the N-th widget (0-based) among the widgets satisfying any predicate this one is
     * combined with through {@link Predicate#and(Predicate)}.
     * <p>
     * The returned predicate is <strong>stateful</strong>: each invocation of
     * {@link Predicate#test(Object)} increments an internal counter. Because
     * {@link Predicate#and(Predicate)} short-circuits, the counter only advances for widgets that
     * have already matched the other predicates, which gives the natural "the N-th widget matching
     * the rest of the criteria" semantics.
     * </p>
     * <p>
     * The counter must be reset before every traversal. Traversal code is expected to detect
     * {@link PositionPredicate} instances (directly or through the public {@link #reset(Predicate)}
     * helper) and call {@link PositionPredicate#reset()} before iterating the widget tree.
     * </p>
     *
     * @param index the 0-based position of the widget to match
     * @return a stateful predicate matching only the widget at the requested position
     * @throws IllegalArgumentException if {@code index} is negative
     */
    public static Predicate<PWidget> position(final int index) {
        if (index < 0) throw new IllegalArgumentException("index must be >= 0, got " + index);
        return new PositionPredicate(index);
    }

    /**
     * Resets any {@link PositionPredicate} state carried by the supplied predicate so it can be
     * re-used for a new traversal. Safe to call on any predicate - unrelated predicates are left
     * untouched. Traversal code should call this before iterating the widget tree.
     *
     * @param predicate the predicate to reset (may be {@code null})
     */
    public static void reset(final Predicate<? super PWidget> predicate) {
        if (predicate instanceof PositionPredicate) {
            ((PositionPredicate) predicate).reset();
        }
    }

    /**
     * Stateful predicate backing {@link Predicates#position(int)}. Exposed so traversal code can
     * detect it via {@code instanceof} and call {@link #reset()} before each walk.
     */
    public static final class PositionPredicate implements Predicate<PWidget> {

        private final int index;
        private int counter;

        PositionPredicate(final int index) {
            this.index = index;
            this.counter = 0;
        }

        @Override
        public boolean test(final PWidget widget) {
            final int current = counter;
            counter++;
            return current == index;
        }

        /** Resets the internal counter so this predicate can be reused for another traversal. */
        public void reset() {
            counter = 0;
        }

        /**
         * @return the 0-based position this predicate matches
         */
        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return "position(" + index + ")";
        }
    }

    /**
     * Extracts the text associated with a widget following the same rules as
     * {@code InspectorWidget.getText()}. Delegates to {@link WidgetText#extract(PWidget)} so that
     * the predicate and the inspector always agree on the text resolution. Returns {@code null}
     * for widget types that have no text concept.
     */
    private static String extractText(final PWidget widget) {
        return WidgetText.extract(widget);
    }
}
