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

package com.ponysdk.sample.client.wa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PEvent;
import com.ponysdk.core.ui.basic.PEventOptions;
import com.ponysdk.core.ui.basic.PWebComponent;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

import jakarta.json.Json;

/**
 * Web Awesome's form controls as fields of a {@link com.ponysdk.core.ui.binder.Binder}
 * (docs/decisions/0028-web-awesome.md): {@code binder.forField(WaFields.of(input)).bind(...)}, {@code carry} included.
 * <p>
 * A control keeps its value in the browser: the server reads it from the control's {@code change} event, the field's
 * {@code target.value}, which the manifest of the library does not say (the property is an accessor, untyped there), and
 * writes it as a property. That property is {@code stateless}: written every time, never compared with what the server
 * wrote last, because the user has changed it since (a reset to the value the server wrote before would be skipped).
 */
// region adapters
public final class WaFields {

    private WaFields() {
    }

    public static HasPValue<String> of(final PWaInput input) {
        return new Field<>(input, "value", "target.value", WaFields::value, WaFields::text);
    }

    public static HasPValue<String> of(final PWaTextarea textarea) {
        return new Field<>(textarea, "value", "target.value", WaFields::value, WaFields::text);
    }

    /** The value of the chosen option; a select with {@code multiple} gives its values as a JSON array. */
    public static HasPValue<String> of(final PWaSelect select) {
        return new Field<>(select, "value", "target.value", WaFields::value, WaFields::text);
    }

    public static HasPValue<String> of(final PWaRadioGroup group) {
        return new Field<>(group, "value", "target.value", WaFields::value, WaFields::text);
    }

    public static HasPValue<String> of(final PWaColorPicker picker) {
        return new Field<>(picker, "value", "target.value", WaFields::value, WaFields::text);
    }

    public static HasPValue<Boolean> of(final PWaCheckbox checkbox) {
        return new Field<>(checkbox, "checked", "target.checked", e -> Boolean.TRUE.equals(e.getBoolean("target.checked")),
            value -> String.valueOf(Boolean.TRUE.equals(value)));
    }

    public static HasPValue<Boolean> of(final PWaSwitch toggle) {
        return new Field<>(toggle, "checked", "target.checked", e -> Boolean.TRUE.equals(e.getBoolean("target.checked")),
            value -> String.valueOf(Boolean.TRUE.equals(value)));
    }

    public static HasPValue<Double> of(final PWaSlider slider) {
        return new Field<>(slider, "value", "target.value", e -> e.getNumber("target.value"), WaFields::number);
    }

    public static HasPValue<Double> of(final PWaRating rating) {
        return new Field<>(rating, "value", "target.value", e -> e.getNumber("target.value"), WaFields::number);
    }

    private static String value(final PEvent event) {
        return event.getString("target.value");
    }

    private static String text(final String value) {
        return Json.createValue(value == null ? "" : value).toString();
    }

    private static String number(final Double value) {
        return value == null ? "0" : value % 1 == 0 ? String.valueOf(value.longValue()) : String.valueOf(value);
    }

    /** A control as a field: the value it last reported or was given, and the handlers of its changes. */
    private static final class Field<V> implements HasPValue<V>, IsPWidget {

        private final PWebComponent element;
        private final PWebComponent.PropertyHandle property;
        private final Function<V, String> json;
        private final List<PValueChangeHandler<V>> handlers = new ArrayList<>();
        private V value;

        Field(final PWebComponent element, final String property, final String path, final Function<PEvent, V> read,
              final Function<V, String> json) {
            this.element = element;
            this.property = element.property(property).stateless();
            this.json = json;
            element.addEventListener("change", PEventOptions.fields(path), event -> {
                value = read.apply(event);
                final PValueChangeEvent<V> change = new PValueChangeEvent<>(element, value);
                for (final PValueChangeHandler<V> handler : List.copyOf(handlers)) handler.onValueChange(change);
            });
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public void setValue(final V value) {
            this.value = value;
            property.set(json.apply(value));
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
            return element;
        }
    }
}
// endregion
