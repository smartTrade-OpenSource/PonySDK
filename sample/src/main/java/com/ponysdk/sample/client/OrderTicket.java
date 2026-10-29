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

package com.ponysdk.sample.client;

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
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * The bean of the Forms page: its rules are annotations, read by the binder (docs/guide/forms.md).
 */
// region constraints
@OrderTicket.LimitNeedsPrice
public class OrderTicket {

    @Pattern(regexp = "[A-Z]{6}", message = "Six capital letters, like EURUSD")
    private String instrument;
    @NotNull(message = "Choose a side")
    private Side side;
    private Type type = Type.MARKET;
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "At least 1")
    @Max(value = 1_000_000, message = "At most 1 000 000")
    private Integer quantity;
    @Positive(message = "Must be greater than 0")
    private BigDecimal limitPrice;
    @NotNull(message = "Value date is required")
    @FutureOrPresent(message = "The value date is in the past")
    private LocalDate valueDate;
    @Size(max = 40, message = "At most 40 characters")
    private String comment;
    @AssertTrue(message = "Confirm the details to send the order")
    private boolean confirmed;
    // ... getters, setters
    // endregion

    public String getInstrument() { return instrument; }
    public void setInstrument(final String instrument) { this.instrument = instrument; }
    public Side getSide() { return side; }
    public void setSide(final Side side) { this.side = side; }
    public Type getType() { return type; }
    public void setType(final Type type) { this.type = type; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(final Integer quantity) { this.quantity = quantity; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public void setLimitPrice(final BigDecimal limitPrice) { this.limitPrice = limitPrice; }
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(final LocalDate valueDate) { this.valueDate = valueDate; }
    public String getComment() { return comment; }
    public void setComment(final String comment) { this.comment = comment; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(final boolean confirmed) { this.confirmed = confirmed; }

    public enum Side {
        BUY, SELL
    }

    public enum Type {
        MARKET, LIMIT
    }

    @Override
    public String toString() {
        return type + " " + side + " " + quantity + " " + instrument + (limitPrice == null ? "" : " @ " + limitPrice.toPlainString())
            + ", value " + valueDate + (comment == null || comment.isBlank() ? "" : ", \"" + comment + "\"");
    }

    // region rule
    /** A rule across fields: a limit order has a limit price. Its error is the bean's, not a field's. */
    @Documented
    @Constraint(validatedBy = LimitNeedsPrice.Check.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LimitNeedsPrice {

        String message() default "A limit order needs a limit price";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        class Check implements ConstraintValidator<LimitNeedsPrice, OrderTicket> {

            @Override
            public boolean isValid(final OrderTicket order, final ConstraintValidatorContext context) {
                return order.getType() != Type.LIMIT || order.getLimitPrice() != null;
            }
        }
    }
    // endregion
}
