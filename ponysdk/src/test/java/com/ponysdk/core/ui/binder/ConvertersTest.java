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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

public class ConvertersTest {

    private static <M> void assertConverts(final Converter<String, M> converter, final String text, final M model) {
        final Result<M> result = converter.toModel(text);
        assertFalse(result.isError(), text);
        assertEquals(model, result.getValue());
    }

    private static void assertRefuses(final Converter<String, ?> converter, final String text) {
        final Result<?> result = converter.toModel(text);
        assertTrue(result.isError(), text);
        assertEquals("bad", result.getMessage());
        assertNull(result.getValue());
    }

    @Test
    public void aResultIsAValueOrAMessage() {
        final Result<String> ok = Result.ok("a");
        assertFalse(ok.isError());
        assertEquals("a", ok.getValue());
        assertNull(ok.getMessage());
        assertFalse(Result.ok(null).isError());
        assertNull(Result.ok(null).getValue());
        final Result<String> error = Result.error("no");
        assertTrue(error.isError());
        assertEquals("no", error.getMessage());
    }

    @Test
    public void aValidatorFromATestFailsWithItsMessage() {
        final Validator<Integer> positive = Validator.of(v -> v > 0, "Positive only");
        assertEquals(3, positive.validate(3).getValue());
        assertEquals("Positive only", positive.validate(-1).getMessage());
    }

    @Test
    public void integersAndLongs() {
        final Converter<String, Integer> integer = Converters.toInteger("bad");
        assertConverts(integer, "42", 42);
        assertConverts(integer, " 42 ", 42);
        assertConverts(integer, "-7", -7);
        assertConverts(integer, "", null);
        assertConverts(integer, "  ", null);
        assertConverts(integer, null, null);
        assertRefuses(integer, "4.5");
        assertRefuses(integer, "abc");
        assertRefuses(integer, "99999999999");
        assertEquals("42", integer.toPresentation(42));
        assertEquals("", integer.toPresentation(null));

        final Converter<String, Long> number = Converters.toLong("bad");
        assertConverts(number, "99999999999", 99999999999L);
        assertConverts(number, "", null);
        assertRefuses(number, "1e3");
        assertEquals("5", number.toPresentation(5L));
    }

    @Test
    public void doublesAreFiniteNumbers() {
        final Converter<String, Double> converter = Converters.toDouble("bad");
        assertConverts(converter, "1.25", 1.25);
        assertConverts(converter, "1e3", 1000.0);
        assertConverts(converter, "", null);
        assertRefuses(converter, "abc");
        assertRefuses(converter, "NaN");
        assertRefuses(converter, "Infinity");
        assertEquals("1.25", converter.toPresentation(1.25));
    }

    @Test
    public void bigDecimalsKeepTheirScaleAndShowWithoutExponent() {
        final Converter<String, BigDecimal> converter = Converters.toBigDecimal("bad");
        assertConverts(converter, "1.2500", new BigDecimal("1.2500"));
        assertConverts(converter, " 3 ", new BigDecimal("3"));
        assertConverts(converter, "", null);
        assertConverts(converter, null, null);
        assertRefuses(converter, "1,5");
        assertRefuses(converter, "x");
        assertEquals("1000", converter.toPresentation(new BigDecimal("1E+3")));
        assertEquals("", converter.toPresentation(null));
    }

    @Test
    public void localDatesAreIsoText() {
        final Converter<String, LocalDate> converter = Converters.toLocalDate("bad");
        assertConverts(converter, "2026-10-29", LocalDate.of(2026, 10, 29));
        assertConverts(converter, "", null);
        assertRefuses(converter, "29/10/2026");
        assertRefuses(converter, "2026-02-30"); // a DateTimeException, not an IllegalArgumentException
        assertEquals("2026-10-29", converter.toPresentation(LocalDate.of(2026, 10, 29)));
        assertEquals("", converter.toPresentation(null));
    }

    @Test
    public void aDateBoxsDateIsADayInItsZone() {
        final ZoneId paris = ZoneId.of("Europe/Paris");
        final Converter<Date, LocalDate> converter = Converters.dateToLocalDate(paris);
        final Date lateEvening = Date.from(LocalDate.of(2026, 10, 29).atTime(23, 30).atZone(paris).toInstant());
        assertEquals(LocalDate.of(2026, 10, 29), converter.toModel(lateEvening).getValue());
        assertNull(converter.toModel(null).getValue());
        assertFalse(converter.toModel(null).isError());
        assertEquals(Date.from(LocalDate.of(2026, 10, 29).atStartOfDay(paris).toInstant()),
            converter.toPresentation(LocalDate.of(2026, 10, 29)));
        assertNull(converter.toPresentation(null));
    }

    @Test
    public void aConverterFromFunctionsLetsOtherExceptionsThrough() {
        final Converter<String, Integer> converter = Converter.of(s -> {
            throw new IllegalStateException("a bug, not a bad input");
        }, String::valueOf, "bad");
        assertEquals("a bug, not a bad input",
            assertThrows(IllegalStateException.class, () -> converter.toModel("1")).getMessage());
    }
}
