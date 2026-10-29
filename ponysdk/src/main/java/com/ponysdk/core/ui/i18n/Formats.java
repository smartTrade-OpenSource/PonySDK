/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.i18n;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

import com.ponysdk.core.server.application.UIContext;

/**
 * Numbers and dates written as a locale does (docs/decisions/0022-locale.md): {@code Formats.current()} for the
 * current {@link UIContext}'s locale, {@code Formats.of(locale)} for another. Nothing is cached or shared: the JDK's
 * formats are not thread-safe, and each call asks for its own.
 *
 * <p>A {@link com.ponysdk.core.ui.basic.PDateBox} writes and reads its text with a {@link SimpleDateFormat}: give it
 * {@link #dateFormat(FormatStyle)}, whose pattern the browser applies with the page's month and day names, and which
 * reads them back in the same locale.
 */
public final class Formats {

    private final Locale locale;

    private Formats(final Locale locale) {
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    /** The formats of the current context's locale. */
    public static Formats current() {
        final UIContext context = UIContext.get();
        if (context == null) throw new IllegalStateException("No UIContext on this thread: Formats.of(locale)");
        return new Formats(context.getLocale());
    }

    public static Formats of(final Locale locale) {
        return new Formats(locale);
    }

    public Locale locale() {
        return locale;
    }

    /** {@code 1234.5}: {@code 1,234.5}, {@code 1 234,5} (a narrow no-break space in French). */
    public String number(final Number value) {
        return NumberFormat.getInstance(locale).format(value);
    }

    /** {@code 0.256}: {@code 26%}, {@code 26 %} in French. */
    public String percent(final double ratio) {
        return NumberFormat.getPercentInstance(locale).format(ratio);
    }

    public String currency(final Number amount, final Currency currency) {
        final NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(currency);
        return format.format(amount);
    }

    /** The date of a {@code LocalDate}, {@code LocalDateTime} or {@code ZonedDateTime}, in the style of the locale. */
    public String date(final TemporalAccessor date, final FormatStyle style) {
        return DateTimeFormatter.ofLocalizedDate(style).withLocale(locale).format(date);
    }

    /** The date and time of a {@code LocalDateTime} or a {@code ZonedDateTime}. */
    public String dateTime(final TemporalAccessor dateTime, final FormatStyle style) {
        return DateTimeFormatter.ofLocalizedDateTime(style).withLocale(locale).format(dateTime);
    }

    /** The date format of the locale in that style, in digits 0-9, for a {@code PDateBox}: {@code dd/MM/y} in French. */
    public SimpleDateFormat dateFormat(final FormatStyle style) {
        final int jdk = switch (style) {
            case FULL -> DateFormat.FULL;
            case LONG -> DateFormat.LONG;
            case MEDIUM -> DateFormat.MEDIUM;
            case SHORT -> DateFormat.SHORT;
        };
        return (SimpleDateFormat) DateFormat.getDateInstance(jdk, withLatinDigits());
    }

    /** A date format of this pattern in the locale, in digits 0-9: the names of its months and days are the locale's. */
    public SimpleDateFormat dateFormat(final String pattern) {
        return new SimpleDateFormat(pattern, withLatinDigits());
    }

    /** The browser writes the digits of a date box as 0-9, whatever the language: Arabic's would not be read back. */
    private Locale withLatinDigits() {
        return new Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("nu", "latn").build();
    }
}
