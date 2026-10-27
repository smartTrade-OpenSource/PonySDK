/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.form.dataconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

public class DataConverterTest {

    @Test
    public void testNumberConverters() {
        final IntegerConverter integer = new IntegerConverter();
        assertEquals("3", integer.from(3));
        assertEquals(3, integer.to("3"));
        assertNull(integer.from(null));
        assertNull(integer.to(null));
        assertNull(integer.to(""));

        final LongConverter longConverter = new LongConverter();
        assertEquals("4", longConverter.from(4L));
        assertEquals(4L, longConverter.to("4"));
        assertNull(longConverter.from(null));
        assertNull(longConverter.to(""));
        assertNull(longConverter.to(null));

        final DoubleConverter doubleConverter = new DoubleConverter();
        assertEquals("1.5", doubleConverter.from(1.5));
        assertEquals(1.5, doubleConverter.to("1.5"));
        assertNull(doubleConverter.from(null));
        assertNull(doubleConverter.to(null));
        assertNull(doubleConverter.to(""));

        final FloatConverter floatConverter = new FloatConverter();
        assertEquals("2.5", floatConverter.from(2.5f));
        assertEquals(2.5f, floatConverter.to("2.5"));
        assertNull(floatConverter.from(null));
        assertNull(floatConverter.to(""));
        assertNull(floatConverter.to(null));
    }

    @Test
    public void testIdentityConverter() {
        final Object value = new Object();
        final IdentityConverter<Object> converter = new IdentityConverter<>();
        assertSame(value, converter.from(value));
        assertSame(value, converter.to(value));
        assertEquals("a", IdentityConverter.STRING.to("a"));
    }

    @Test
    public void testDateConverter() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final DateConverter converter = new DateConverter(format);
        assertEquals("1970-01-02", converter.from(new Date(86_400_000L)));
        assertEquals(new Date(86_400_000L), converter.to("1970-01-02"));
        assertNull(converter.from(null));
        // Unparseable input is logged and converted to null
        assertNull(converter.to("not a date"));

        final DateConverter defaultConverter = new DateConverter();
        final Date now = new Date(1_000_000_000_000L);
        assertEquals(DateFormat.getDateInstance().format(now), defaultConverter.from(now));
    }
}
