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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validation;

/**
 * The constraints of a bean class, for a {@link Binder}. The only class of the package that touches
 * {@code jakarta.validation}: {@link Binder} loads it only once it found the API on the classpath, and the
 * provider (Hibernate Validator) working; the jakarta.validation API alone is not enough to validate.
 */
final class BeanValidation {

    private static final Logger log = LoggerFactory.getLogger(BeanValidation.class);

    /** A constraint that failed: on a property of the bean, or on the bean itself (property ""). */
    record Violation(String property, String message) {
    }

    /** The factory is built once, by the first binder to want it: it is the costly part, and shared by every session. */
    private static final class Provider {
        private static final jakarta.validation.Validator VALIDATOR = create();

        private static jakarta.validation.Validator create() {
            try {
                return Validation.buildDefaultValidatorFactory().getValidator(); // lives as long as the JVM
            } catch (final jakarta.validation.ValidationException e) { // no provider, or its expression language missing
                log.warn("Bean Validation is on the classpath but unusable, the constraints are not applied: {}", e.getMessage());
                return null;
            }
        }
    }

    private final jakarta.validation.Validator validator;
    private final Class<?> beanType;

    private BeanValidation(final jakarta.validation.Validator validator, final Class<?> beanType) {
        this.validator = validator;
        this.beanType = beanType;
    }

    /** The validation of {@code beanType}, null when there is no working provider. */
    static BeanValidation of(final Class<?> beanType) {
        final jakarta.validation.Validator validator = Provider.VALIDATOR;
        return validator == null ? null : new BeanValidation(validator, beanType);
    }

    /** Fails for a name that is not a property of the bean. */
    void checkProperty(final String property) {
        validator.validateValue(beanType, property, null);
    }

    /** The messages of the constraints of {@code property} that {@code value} breaks, null when none. */
    String property(final String property, final Object value) {
        return join(validator.validateValue(beanType, property, value));
    }

    /** The constraints that the bean breaks; those of a property are named by the first node of their path. */
    List<Violation> bean(final Object bean) {
        final Set<ConstraintViolation<Object>> violations = validator.validate(bean);
        final List<Violation> out = new ArrayList<>(violations.size());
        for (final ConstraintViolation<Object> v : violations) out.add(new Violation(root(v.getPropertyPath()), v.getMessage()));
        return out;
    }

    private static String root(final Path path) {
        for (final Path.Node node : path) {
            if (node.getName() != null) return node.getName(); // the bean's own node has none
        }
        return "";
    }

    private static String join(final Set<? extends ConstraintViolation<?>> violations) {
        if (violations.isEmpty()) return null;
        return violations.stream().map(ConstraintViolation::getMessage).sorted().collect(Collectors.joining("; "));
    }

}
