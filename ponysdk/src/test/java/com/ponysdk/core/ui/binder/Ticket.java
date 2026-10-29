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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * The bean of the binder's tests: constraints on its properties, and two on the class. The messages are written out,
 * not the provider's defaults: they depend on the locale of the machine.
 */
@Ticket.LimitNeedsPrice
@Ticket.LargeNeedsPrice
@Ticket.NoteMustBeShort
public class Ticket {

    public enum Kind {
        MARKET, LIMIT
    }

    @NotNull(message = "Instrument required")
    @Size(min = 3, message = "Too short")
    @Pattern(regexp = "[A-Z]*", message = "Upper case only")
    private String instrument;
    @NotNull(message = "Quantity required")
    @Min(value = 1, message = "At least 1")
    @Max(value = 1000, message = "At most 1000")
    private Integer quantity;
    @Positive(message = "Positive only")
    private BigDecimal limit;
    private Kind kind = Kind.MARKET;
    @AssertTrue(message = "Confirm")
    private boolean confirmed;
    private LocalDate date;
    private String note;

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(final String instrument) {
        this.instrument = instrument;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(final BigDecimal limit) {
        this.limit = limit;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(final Kind kind) {
        this.kind = kind;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(final boolean confirmed) {
        this.confirmed = confirmed;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(final LocalDate date) {
        this.date = date;
    }

    /** A property of the bean that no test binds: its constraints are not the form's. */
    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    /** The bean's own error: a limit order has a limit price. */
    @Documented
    @Constraint(validatedBy = LimitNeedsPriceValidator.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LimitNeedsPrice {

        String message() default "A limit order needs a limit price";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class LimitNeedsPriceValidator implements ConstraintValidator<LimitNeedsPrice, Ticket> {

        @Override
        public boolean isValid(final Ticket ticket, final ConstraintValidatorContext context) {
            return ticket.kind != Kind.LIMIT || ticket.limit != null;
        }
    }

    /** An error of the bean that belongs to a field: a large order has a limit price, said on the limit. */
    @Documented
    @Constraint(validatedBy = LargeNeedsPriceValidator.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LargeNeedsPrice {

        String message() default "A large order needs a limit price";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class LargeNeedsPriceValidator implements ConstraintValidator<LargeNeedsPrice, Ticket> {

        @Override
        public boolean isValid(final Ticket ticket, final ConstraintValidatorContext context) {
            if (ticket.quantity == null || ticket.quantity <= 900 || ticket.limit != null) return true;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("limit").addConstraintViolation();
            return false;
        }
    }

    /** A class-level error on a property no form binds: not the form's to show. */
    @Documented
    @Constraint(validatedBy = NoteValidator.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NoteMustBeShort {

        String message() default "A note is short";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class NoteValidator implements ConstraintValidator<NoteMustBeShort, Ticket> {

        @Override
        public boolean isValid(final Ticket ticket, final ConstraintValidatorContext context) {
            if (ticket.note == null || ticket.note.length() < 10) return true;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("note").addConstraintViolation();
            return false;
        }
    }
}
