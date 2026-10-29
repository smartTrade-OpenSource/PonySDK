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

package com.ponysdk.core.ui.binder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.listbox.ListBox;

/**
 * The selection of the list boxes as fields to bind: they hold a selection, not a value
 * ({@code binder.forField(Fields.of(sideBox, Side.class))}).
 */
public final class Fields {

    private Fields() {
    }

    /** The value of the selected item of a list box ({@code addItem(label, value)}); null is the empty item, if it has one. */
    public static <V> HasPValue<V> of(final PListBox box, final Class<V> type) {
        return new Selection<>(box, () -> type.cast(box.getSelectedValue()), box::setSelectedValue,
            changed -> box.addChangeHandler(event -> changed.run()));
    }

    /** The data of the selected item of a list box with a single selection. */
    public static <D> HasPValue<D> of(final ListBox<D> box) {
        return new Selection<>(box, box::getSelectedData, box::setSelected,
            changed -> box.addValueChangeHandler(event -> changed.run()));
    }

    private static final class Selection<V> implements HasPValue<V>, IsPWidget {

        private final IsPWidget widget;
        private final Supplier<V> read;
        private final Consumer<V> write;
        private final List<PValueChangeHandler<V>> handlers = new ArrayList<>();

        private Selection(final IsPWidget widget, final Supplier<V> read, final Consumer<V> write,
                          final Consumer<Runnable> onChange) {
            this.widget = widget;
            this.read = read;
            this.write = write;
            onChange.accept(this::fire);
        }

        private void fire() {
            final PValueChangeEvent<V> event = new PValueChangeEvent<>(widget, read.get());
            for (final PValueChangeHandler<V> handler : handlers) handler.onValueChange(event);
        }

        @Override
        public V getValue() {
            return read.get();
        }

        @Override
        public void setValue(final V value) {
            write.accept(value);
        }

        @Override
        public void addValueChangeHandler(final PValueChangeHandler<V> handler) {
            handlers.add(handler);
        }

        @Override
        public boolean removeValueChangeHandler(final PValueChangeHandler<V> handler) {
            return handlers.remove(handler);
        }

        @Override
        public Collection<PValueChangeHandler<V>> getValueChangeHandlers() {
            return Collections.unmodifiableList(handlers);
        }

        @Override
        public PWidget asWidget() {
            return widget.asWidget();
        }
    }

}
