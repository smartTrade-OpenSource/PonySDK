/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.signal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PFocusWidget;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

/**
 * Widgets that follow signals (docs/decisions/0019-signals.md). Each method is called on the UI thread of the
 * widget's context, shows the signal's current value at once, then follows it: a change, from any thread, reaches
 * the widget on its context's UI thread, at most once per flush however many changes came before it. A binding ends
 * with its widget.
 * <pre>
 * Bind.text(price, bidText);
 * Bind.styleName(price, "up", rising);
 * Bind.value(quantity, quantitySignal); // the user's input sets the signal
 * </pre>
 */
public final class Bind {

    private Bind() {
    }

    /**
     * Sets how this context's bindings are applied: at most one flush per {@code minInterval}, the drains run by
     * {@code executor} (a virtual thread each if null), as {@link com.ponysdk.core.server.concurrent.Conflator} does.
     * Called on the UI thread, before the context's first binding.
     */
    public static void configure(final Duration minInterval, final Executor executor) {
        Bindings.configure(context(), minInterval, executor);
    }

    /** Any property of a widget: {@code Bind.to(label, signal, PLabel::setTitle)}. */
    public static <W extends PObject, T> Binding to(final W widget, final Signal<T> signal, final BiConsumer<? super W, ? super T> apply) {
        return bind(widget, signal, value -> apply.accept(widget, value), null);
    }

    public static Binding text(final PLabel label, final Signal<String> text) {
        return to(label, text, PLabel::setText);
    }

    public static Binding text(final PButton button, final Signal<String> text) {
        return to(button, text, PButton::setText);
    }

    public static Binding visible(final PWidget widget, final Signal<Boolean> visible) {
        return to(widget, visible, PWidget::setVisible);
    }

    public static Binding enabled(final PFocusWidget widget, final Signal<Boolean> enabled) {
        return to(widget, enabled, PFocusWidget::setEnabled);
    }

    /** The style name is on the widget while the signal is true. */
    public static Binding styleName(final PWidget widget, final String styleName, final Signal<Boolean> on) {
        return to(widget, on, (w, value) -> {
            if (Boolean.TRUE.equals(value)) w.addStyleName(styleName);
            else w.removeStyleName(styleName);
        });
    }

    /** The box shows the signal, and the user's input sets it. */
    public static <T, W extends PWidget & HasPValue<T>> Binding value(final W box, final MutableSignal<T> signal) {
        final PValueChangeHandler<T> handler = event -> signal.set(event.getData());
        final Binding binding = bind(box, signal, box::setValue, () -> box.removeValueChangeHandler(handler));
        box.addValueChangeHandler(handler); // once the box shows the signal: its own setValue is not an input
        return binding;
    }

    /**
     * The panel's children are the widgets of the list's items, in its order: a widget is made by {@code row} once
     * for a key, kept for as long as an item has that key, and destroyed with its item. A widget does not follow its
     * item's changes: it follows signals the item holds. The list is replaced, never changed in place; the panel is
     * the binding's.
     */
    public static <T, K> Binding children(final PComplexPanel panel, final Signal<? extends List<T>> items,
                                          final Function<? super T, ? extends K> key, final Function<? super T, ? extends PWidget> row) {
        final Map<K, PWidget> rows = new HashMap<>();
        return bind(panel, items, list -> reconcile(panel, rows, list, key, row), null);
    }

    private static <T, K> void reconcile(final PComplexPanel panel, final Map<K, PWidget> rows, final List<T> items,
                                         final Function<? super T, ? extends K> key, final Function<? super T, ? extends PWidget> row) {
        final Map<K, PWidget> next = new HashMap<>();
        final List<PWidget> order = new ArrayList<>(items.size());
        final List<PWidget> made = new ArrayList<>();
        try {
            for (final T item : items) {
                final K k = key.apply(item);
                if (next.containsKey(k)) throw new IllegalArgumentException("Two items have the key " + k);
                PWidget widget = rows.get(k);
                if (widget == null) {
                    widget = row.apply(item);
                    made.add(widget);
                }
                next.put(k, widget);
                order.add(widget);
            }
        } catch (final RuntimeException e) {
            made.forEach(PObject::onDestroy); // not in the panel: nothing else would end their bindings
            throw e;
        }
        rows.forEach((k, widget) -> {
            if (next.containsKey(k)) return;
            panel.remove(widget);
            widget.onDestroy();
        });
        rows.clear();
        rows.putAll(next);
        for (int i = 0; i < order.size(); i++) {
            final PWidget widget = order.get(i);
            if (i >= panel.getWidgetCount() || panel.getWidget(i) != widget) panel.insert(widget, i); // moves a child already there
        }
    }

    private static <T> Binding bind(final PObject widget, final Signal<T> signal, final Consumer<? super T> apply,
                                    final Runnable onClose) {
        return Bindings.of(context()).bind(widget, signal, apply, onClose);
    }

    private static UIContext context() {
        final UIContext uiContext = UIContext.get();
        if (uiContext == null) throw new IllegalStateException("A binding is made on the UI thread of its widget's context");
        return uiContext;
    }
}
