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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Function;

/**
 * The converters of the usual types. A blank text is no value (null), and no value shows as an empty text.
 */
public final class Converters {

    private Converters() {
    }

    public static Converter<String, Integer> toInteger(final String error) {
        return text(Integer::valueOf, error);
    }

    public static Converter<String, Long> toLong(final String error) {
        return text(Long::valueOf, error);
    }

    /** NaN and the infinities are not numbers a field accepts. */
    public static Converter<String, Double> toDouble(final String error) {
        return text(s -> {
            final double d = Double.parseDouble(s);
            if (!Double.isFinite(d)) throw new IllegalArgumentException(s);
            return d;
        }, error);
    }

    /** Shown without an exponent. */
    public static Converter<String, BigDecimal> toBigDecimal(final String error) {
        return Converter.of(s -> s == null || s.isBlank() ? null : new BigDecimal(s.strip()),
            d -> d == null ? "" : d.toPlainString(), error);
    }

    /** An ISO date, 2026-10-29. */
    public static Converter<String, LocalDate> toLocalDate(final String error) {
        return text(LocalDate::parse, error);
    }

    /** A date box's {@link Date} as a {@link LocalDate}: its day in {@code zone}, and back to the start of that day. */
    public static Converter<Date, LocalDate> dateToLocalDate(final ZoneId zone) {
        return Converter.of(d -> d == null ? null : d.toInstant().atZone(zone).toLocalDate(),
            d -> d == null ? null : Date.from(d.atStartOfDay(zone).toInstant()), null);
    }

    private static <M> Converter<String, M> text(final Function<String, M> parse, final String error) {
        return Converter.of(s -> s == null || s.isBlank() ? null : parse.apply(s.strip()),
            m -> m == null ? "" : m.toString(), error);
    }

}
