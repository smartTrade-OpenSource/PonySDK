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

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

/**
 * Binds the fields of a screen to the properties of a bean (docs/decisions/0021-forms-binder.md).
 * <pre>
 * final Binder&lt;Order&gt; binder = new Binder&lt;&gt;(Order.class);
 * binder.forField(quantity).withConverter(Converters.toInteger("Not a number")).bind(Order::getQuantity, Order::setQuantity);
 * binder.readBean(order);                          // the fields show the bean
 * if (binder.writeBean(order)) save(order);        // all valid: the bean takes the fields; else the errors show
 * </pre>
 * A field's value goes through its converters and validators on each change the widget reports. An error shows on
 * the field, for the eyes and for a screen reader: {@code aria-invalid}, {@code aria-describedby} and a message
 * element after the field, which must be in a panel ({@code PFlowPanel}...). With {@code new Binder<>(Order.class)}
 * and Bean Validation (Jakarta, a provider such as Hibernate Validator) on the classpath, the constraints of the
 * bound properties apply to their fields, and those of the class are errors of the bean; without it nothing changes.
 * <p>
 * Buffered, with {@link #readBean} and {@link #writeBean}: the bean is written when everything is valid, all or
 * nothing. Unbuffered, with {@link #setBean}: each valid change is written to the bean at once. A binder is used on
 * the UI thread of its context, like the widgets it binds.
 */
public final class Binder<B> {

    private static final boolean BEAN_VALIDATION = beanValidationPresent();

    /** A getter that can tell its property: a method reference, {@code Order::getQuantity}. */
    @FunctionalInterface
    public interface Getter<B, V> extends Function<B, V>, Serializable {
    }

    /** A binding of a field to a property. */
    public interface Binding<B, V> {

        /** Validates the field now and shows its error; true when it is valid. */
        boolean validate();
    }

    /**
     * What the form is: whether a field differs from the bean last read or written, whether every field and the bean
     * are valid, and the messages of the bean's own errors.
     */
    public record Status(boolean hasChanges, boolean valid, List<String> beanErrors) {
    }

    private final BeanValidation beanValidation;
    private final List<BindingImpl<?>> bindings = new ArrayList<>();
    private final List<Validator<B>> beanValidators = new ArrayList<>();
    private final List<Consumer<Status>> listeners = new CopyOnWriteArrayList<>();
    private B bean;
    private boolean loading;
    private List<String> beanErrors = List.of();
    private Status status = new Status(false, true, List.of());

    /** A binder of fields and validators of your own. */
    public Binder() {
        this(null);
    }

    /** A binder that applies the constraints of {@code beanType} when Bean Validation is there to do it. */
    public Binder(final Class<B> beanType) {
        beanValidation = beanType != null && BEAN_VALIDATION ? BeanValidation.of(beanType) : null;
    }

    private static boolean beanValidationPresent() {
        try {
            Class.forName("jakarta.validation.Validation", false, Binder.class.getClassLoader());
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    // ---- the bindings -----------------------------------------------------------------------------------------

    /** Starts the binding of a field: its converters and validators, then {@code bind}. */
    @SuppressWarnings("unchecked")
    public <V> BindingBuilder<B, V> forField(final HasPValue<V> field) {
        return new BindingBuilder<>(this, (HasPValue<Object>) field, (Converter<Object, V>) IDENTITY, false);
    }

    /** A validator of the bean, run after the bean took the fields' values: for rules across fields. */
    public Binder<B> withValidator(final Validator<B> validator) {
        beanValidators.add(validator);
        return this;
    }

    private <V> Binding<B, V> add(final BindingBuilder<B, V> builder, final Getter<B, V> getter,
                                  final BiConsumer<B, V> setter, final String name) {
        final String property = beanValidation == null ? null : property(getter, name);
        final IsPWidget widget = builder.field instanceof IsPWidget w ? w : null;
        final BindingImpl<V> binding = new BindingImpl<>(builder.field, builder.converter, getter, setter, property,
            widget == null ? null : new FieldDisplay(widget.asWidget()));
        if (builder.required && binding.display != null) binding.display.required();
        bindings.add(binding);
        builder.field.addValueChangeHandler(event -> changed(binding));
        return binding;
    }

    private String property(final Getter<B, ?> getter, final String name) {
        final String property = name != null ? name : propertyName(getter);
        if (property == null) {
            throw new IllegalArgumentException("The property of this getter is unknown, it is not a method reference: "
                + "give its name to bind(getter, setter, name)");
        }
        beanValidation.checkProperty(property);
        return property;
    }

    /** The property a getter reads, from its method reference ({@code getX}, {@code isX} or {@code x}), else null. */
    private static String propertyName(final Getter<?, ?> getter) {
        final String method;
        try {
            final Method writeReplace = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            method = ((SerializedLambda) writeReplace.invoke(getter)).getImplMethodName();
        } catch (final ReflectiveOperationException | RuntimeException e) { // not a lambda, or its module is closed
            return null;
        }
        if (method.startsWith("lambda$")) return null; // a lambda, not a method reference
        String property = strip(method, "get");
        if (property == null) property = strip(method, "is");
        return property == null ? method : property;
    }

    /** What follows a prefix of a JavaBeans accessor, decapitalized, null when the name is not one. */
    private static String strip(final String method, final String prefix) {
        if (method.length() <= prefix.length() || !method.startsWith(prefix) || !Character.isUpperCase(method.charAt(prefix.length()))) {
            return null;
        }
        final String name = method.substring(prefix.length());
        final boolean acronym = name.length() > 1 && Character.isUpperCase(name.charAt(1));
        return acronym ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    // ---- the bean ---------------------------------------------------------------------------------------------

    /**
     * Buffered: shows the bean in the fields, and from now on holds nothing of it. A null bean empties the fields.
     * Errors are cleared; the status tells what the fields hold.
     */
    public void readBean(final B bean) {
        this.bean = null;
        load(bean);
    }

    /**
     * Unbuffered: shows the bean in the fields and binds to it: each change of a field that is valid is written to
     * the bean at once, then the bean's validators run. A null bean unbinds and empties the fields.
     */
    public void setBean(final B bean) {
        this.bean = bean;
        load(bean);
    }

    public B getBean() {
        return bean;
    }

    private void load(final B source) {
        loading = true;
        try {
            for (final BindingImpl<?> binding : bindings) binding.load(source);
        } finally {
            loading = false;
        }
        beanErrors = List.of();
        fire();
    }

    /**
     * Buffered: writes the fields to {@code target}, all or nothing. Every field is validated and shows its error; if
     * all are valid the bean takes the values and its own validators and constraints run: if one fails the bean is put
     * back as it was and its errors are in the status. True when the bean was written.
     */
    public boolean writeBean(final B target) {
        if (!validateFields()) {
            beanErrors = List.of();
            fire();
            return false;
        }
        for (final BindingImpl<?> binding : bindings) binding.write(target);
        final boolean valid = validateBean(target);
        if (valid) {
            for (final BindingImpl<?> binding : bindings) binding.baseline = binding.field.getValue();
        } else {
            for (final BindingImpl<?> binding : bindings) binding.revert(target);
        }
        fire();
        return valid;
    }

    /**
     * Validates every field, showing the errors, and, for a bean bound with {@link #setBean}, the bean as well. True
     * when everything is valid. A buffered binder has no bean to validate until {@link #writeBean}.
     */
    public boolean validate() {
        boolean valid = validateFields();
        if (bean != null && valid) valid = validateBean(bean);
        else if (bean == null) beanErrors = List.of();
        fire();
        return valid;
    }

    private boolean validateFields() {
        boolean valid = true;
        for (final BindingImpl<?> binding : bindings) valid &= binding.validate0();
        return valid;
    }

    /** Runs the bean's validators and constraints; the bean's own errors are kept, the others show on their field. */
    private boolean validateBean(final B target) {
        final List<String> messages = new ArrayList<>();
        for (final Validator<B> validator : beanValidators) {
            final Result<B> result = validator.validate(target);
            if (result.isError()) messages.add(result.getMessage());
        }
        for (final BindingImpl<?> binding : bindings) binding.external = null;
        if (beanValidation != null) {
            for (final BeanValidation.Violation violation : beanValidation.bean(target)) {
                if (violation.property().isEmpty()) {
                    messages.add(violation.message());
                } else {
                    for (final BindingImpl<?> binding : bindings) {
                        if (violation.property().equals(binding.property)) binding.external = violation.message();
                    }
                }
            }
        }
        for (final BindingImpl<?> binding : bindings) binding.show();
        beanErrors = List.copyOf(messages);
        return messages.isEmpty() && bindings.stream().noneMatch(b -> b.external != null);
    }

    // ---- the status -------------------------------------------------------------------------------------------

    public Status getStatus() {
        boolean changes = false;
        boolean valid = beanErrors.isEmpty();
        for (final BindingImpl<?> binding : bindings) {
            changes |= binding.hasChanged();
            valid &= binding.error == null && binding.external == null;
        }
        return new Status(changes, valid, beanErrors);
    }

    /** Called when the status changes: a field edited, the bean read or written, a validation. */
    public HandlerRegistration addStatusChangeListener(final Consumer<Status> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void fire() {
        final Status current = getStatus();
        if (current.equals(status)) return;
        status = current;
        for (final Consumer<Status> listener : listeners) listener.accept(current);
    }

    /** A field changed: validated, shown, and written to the bean when there is one bound. */
    private void changed(final BindingImpl<?> binding) {
        if (loading) return;
        binding.validate0();
        if (bean != null) {
            if (binding.error == null) {
                binding.write(bean);
                binding.baseline = binding.field.getValue();
                validateBean(bean);
            }
        } else {
            clearBeanErrors();
        }
        fire();
    }

    /** What the last write found of the bean is stale as soon as a field changes. */
    private void clearBeanErrors() {
        beanErrors = List.of();
        for (final BindingImpl<?> binding : bindings) {
            if (binding.external != null) {
                binding.external = null;
                binding.show();
            }
        }
    }

    // ---- the builder ------------------------------------------------------------------------------------------

    private static final Converter<Object, Object> IDENTITY = new Converter<>() {

        @Override
        public Result<Object> toModel(final Object presentation) {
            return Result.ok(presentation);
        }

        @Override
        public Object toPresentation(final Object model) {
            return model;
        }
    };

    /** The converters and validators of a field, from what it holds to what the bean's property holds. */
    public static final class BindingBuilder<B, V> {

        private final Binder<B> binder;
        private final HasPValue<Object> field;
        private final Converter<Object, V> converter;
        private final boolean required;

        private BindingBuilder(final Binder<B> binder, final HasPValue<Object> field, final Converter<Object, V> converter,
                               final boolean required) {
            this.binder = binder;
            this.field = field;
            this.converter = converter;
            this.required = required;
        }

        /** The value so far goes through {@code next}; what is bound is what {@code next} gives. */
        public <T> BindingBuilder<B, T> withConverter(final Converter<V, T> next) {
            return new BindingBuilder<>(binder, field, new Converter<Object, T>() {

                @Override
                public Result<T> toModel(final Object presentation) {
                    final Result<V> before = converter.toModel(presentation);
                    return before.isError() ? Result.error(before.getMessage()) : next.toModel(before.getValue());
                }

                @Override
                public Object toPresentation(final T model) {
                    return converter.toPresentation(next.toPresentation(model));
                }
            }, required);
        }

        /** Checks the value so far; a field that is empty gives null, which a validator has to accept or refuse. */
        public BindingBuilder<B, V> withValidator(final Validator<V> validator) {
            return new BindingBuilder<>(binder, field, new Converter<Object, V>() {

                @Override
                public Result<V> toModel(final Object presentation) {
                    final Result<V> before = converter.toModel(presentation);
                    return before.isError() ? before : validator.validate(before.getValue());
                }

                @Override
                public Object toPresentation(final V model) {
                    return converter.toPresentation(model);
                }
            }, required);
        }

        public BindingBuilder<B, V> withValidator(final Predicate<? super V> test, final String message) {
            return withValidator(Validator.of(test, message));
        }

        /**
         * Refuses an empty field (no value, or a blank text) with {@code message}, and marks it {@code aria-required}.
         * Put it first, before the converters, to judge the text.
         */
        public BindingBuilder<B, V> asRequired(final String message) {
            final BindingBuilder<B, V> checked = withValidator(
                v -> v == null || v instanceof String s && s.isBlank() ? Result.error(message) : Result.ok(v));
            return new BindingBuilder<>(binder, field, checked.converter, true);
        }

        /**
         * Binds the field to the bean's property. A null setter makes the binding read only. With Bean Validation, the
         * property is that of the method reference ({@code Order::getQuantity} is {@code quantity}).
         */
        public Binding<B, V> bind(final Getter<B, V> getter, final BiConsumer<B, V> setter) {
            return binder.add(this, getter, setter, null);
        }

        /** As {@link #bind(Getter, BiConsumer)}, for a getter that is not a method reference, with its property's name. */
        public Binding<B, V> bind(final Getter<B, V> getter, final BiConsumer<B, V> setter, final String property) {
            return binder.add(this, getter, setter, Objects.requireNonNull(property));
        }
    }

    // ---- one binding ------------------------------------------------------------------------------------------

    private final class BindingImpl<V> implements Binding<B, V> {

        private final HasPValue<Object> field;
        private final Converter<Object, V> converter;
        private final Getter<B, V> getter;
        private final BiConsumer<B, V> setter;
        private final String property;
        private final FieldDisplay display;
        /** What the field held when the bean was last read or written. */
        private Object baseline;
        /** The field's value past its converters, when it has no error. */
        private V value;
        /** The field's own error, shown or not yet. */
        private String error;
        /** An error of the bean on this field, from the last write. */
        private String external;
        private V previous;

        private BindingImpl(final HasPValue<Object> field, final Converter<Object, V> converter, final Getter<B, V> getter,
                            final BiConsumer<B, V> setter, final String property, final FieldDisplay display) {
            this.field = field;
            this.converter = converter;
            this.getter = getter;
            this.setter = setter;
            this.property = property;
            this.display = display;
        }

        /** Shows the bean's value, and what the field holds of it as the baseline; no error shows. */
        private void load(final B source) {
            field.setValue(converter.toPresentation(source == null ? null : getter.apply(source)));
            baseline = field.getValue();
            external = null;
            evaluate();
            if (display != null) display.show(null);
        }

        private void evaluate() {
            final Result<V> result = converter.toModel(field.getValue());
            if (result.isError()) {
                error = result.getMessage();
                return;
            }
            value = result.getValue();
            error = property == null ? null : beanValidation.property(property, value);
        }

        private boolean validate0() {
            external = null;
            evaluate();
            show();
            return error == null;
        }

        private void show() {
            if (display != null) display.show(error != null ? error : external);
        }

        private void write(final B target) {
            if (setter == null) return;
            previous = getter.apply(target);
            setter.accept(target, value);
        }

        private void revert(final B target) {
            if (setter != null) setter.accept(target, previous);
        }

        private boolean hasChanged() {
            return !Objects.equals(field.getValue(), baseline);
        }

        @Override
        public boolean validate() {
            final boolean valid = validate0();
            fire();
            return valid;
        }
    }

}
