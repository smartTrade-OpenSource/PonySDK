/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.test.PSuite;

/** Numbers and dates as a locale writes them (docs/decisions/0022-locale.md). */
public class FormatsTest extends PSuite {

    private static final Formats EN = Formats.of(Locale.ENGLISH);
    private static final Formats FR = Formats.of(Locale.FRENCH);
    private static final Formats AR = Formats.of(Locale.forLanguageTag("ar"));
    private static final LocalDate DATE = LocalDate.of(2026, 3, 5);

    @AfterEach
    public void resetContext() {
        Mockito.doReturn(Locale.ENGLISH).when(UIContext.get()).getLocale(); // PSuite's
    }

    @Test
    public void aNumberFollowsTheLocale() {
        assertEquals("1,234.5", EN.number(1234.5));
        assertEquals("1\u202f234,5", FR.number(1234.5), "a narrow no-break space, a comma");
        assertEquals("1,234,567", EN.number(1_234_567L));
    }

    @Test
    public void aPercentAndACurrency() {
        assertEquals("26%", EN.percent(0.256));
        assertEquals("26\u00a0%", FR.percent(0.256));
        assertEquals("$1,234.50", EN.currency(1234.5, Currency.getInstance("USD")));
        assertEquals("1\u202f234,50\u00a0\u20ac", FR.currency(1234.5, Currency.getInstance("EUR")));
    }

    @Test
    public void aDateFollowsTheLocale() {
        assertEquals("3/5/26", EN.date(DATE, FormatStyle.SHORT));
        assertEquals("05/03/2026", FR.date(DATE, FormatStyle.SHORT));
        assertEquals("5 mars 2026", FR.date(DATE, FormatStyle.LONG));
        assertEquals("Thursday, March 5, 2026", EN.date(DATE, FormatStyle.FULL));
        assertEquals("05/03/2026 14:07", FR.dateTime(LocalDateTime.of(2026, 3, 5, 14, 7), FormatStyle.SHORT));
    }

    @Test
    public void theFormatsOfTheCurrentContextAreThoseOfItsLocale() {
        Mockito.doReturn(Locale.FRENCH).when(UIContext.get()).getLocale();
        assertEquals(Locale.FRENCH, Formats.current().locale());
        assertEquals("1\u202f234,5", Formats.current().number(1234.5));
    }

    @Test
    public void withoutAContextTheLocaleIsToBeGiven() {
        final UIContext context = UIContext.get();
        UIContext.remove();
        try {
            assertThrows(IllegalStateException.class, Formats::current);
        } finally {
            UIContext.setCurrent(context);
        }
        assertThrows(NullPointerException.class, () -> Formats.of(null));
    }

    @Test
    public void aDateBoxFormatIsTheLocalesAndWritesDigitsOfZeroToNine() throws ParseException {
        final Date date = Date.from(DATE.atStartOfDay().toInstant(ZoneOffset.UTC));
        final java.text.SimpleDateFormat french = FR.dateFormat(FormatStyle.SHORT);
        french.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        assertEquals("05/03/2026", french.format(date));
        assertEquals(date, french.parse("05/03/2026"));

        final java.text.SimpleDateFormat arabic = AR.dateFormat(FormatStyle.LONG);
        arabic.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        assertEquals(date, arabic.parse(arabic.format(date)), "what it writes it reads");
        assertEquals(true, arabic.format(date).chars().noneMatch(c -> c >= '٠' && c <= '٩'), arabic.format(date));

        assertEquals("EEEE d MMMM y", FR.dateFormat(FormatStyle.FULL).toPattern());
        assertEquals("d MMMM y", FR.dateFormat(FormatStyle.LONG).toPattern());
        assertEquals("d MMM y", FR.dateFormat(FormatStyle.MEDIUM).toPattern());
        assertEquals("dd/MM/y", FR.dateFormat(FormatStyle.SHORT).toPattern());
        assertEquals("jeudi 5 mars 2026", withZone(FR.dateFormat("EEEE d MMMM yyyy")).format(date));
        assertEquals("Thursday 5 March 2026", withZone(EN.dateFormat("EEEE d MMMM yyyy")).format(date));
    }

    private static java.text.SimpleDateFormat withZone(final java.text.SimpleDateFormat format) {
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return format;
    }
}
