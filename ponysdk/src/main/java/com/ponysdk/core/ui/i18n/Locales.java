/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Which of the application's locales a visitor gets (docs/decisions/0022-locale.md): the choice kept in the
 * {@link #COOKIE} first, then the best match of the browser's {@code Accept-Language}, then the first supported
 * locale.
 */
public final class Locales {

    /** The cookie that keeps the visitor's choice: a reload, or a server that stops (ADR-0017), finds it. */
    public static final String COOKIE = "ponysdk-locale";

    private static final Set<String> RIGHT_TO_LEFT = Set.of("ar", "fa", "he", "iw", "ur", "ps", "sd", "ug", "yi", "dv", "ckb");
    private static final Set<String> RIGHT_TO_LEFT_SCRIPTS = Set.of("Arab", "Hebr", "Thaa", "Syrc", "Nkoo");

    private Locales() {
    }

    /**
     * The supported locale a visitor gets: that of the cookie, else the best match of the Accept-Language header, else
     * the first supported one. A value that is absent, malformed or not supported is ignored.
     *
     * @param supported      the application's locales, not empty
     * @param cookie         the value of {@link #COOKIE}, or null
     * @param acceptLanguage the Accept-Language header, or null
     */
    public static Locale negotiate(final List<Locale> supported, final String cookie, final String acceptLanguage) {
        Locale match = cookie == null ? null : match(supported, cookie);
        if (match == null && acceptLanguage != null) match = match(supported, acceptLanguage);
        return match != null ? match : supported.get(0);
    }

    /**
     * The supported locale that fits the language ranges ({@code fr-CH, fr;q=0.9, en;q=0.8}), or null. A range
     * matches the locales it is a prefix of, as RFC 4647's lookup does ({@code fr-CH} gets {@code fr}); failing that,
     * a locale of the same language ({@code de} gets {@code de-CH}).
     */
    public static Locale match(final List<Locale> supported, final String ranges) {
        final List<Locale.LanguageRange> parsed;
        try {
            parsed = Locale.LanguageRange.parse(ranges);
        } catch (final IllegalArgumentException e) {
            return null; // a header or a cookie of no use: another source decides
        }
        final Locale exact = Locale.lookup(parsed, supported);
        if (exact != null) return exact;
        for (final Locale.LanguageRange range : parsed) {
            final String language = Locale.forLanguageTag(range.getRange()).getLanguage();
            for (final Locale locale : supported) {
                if (locale.getLanguage().equals(language)) return locale;
            }
        }
        return null;
    }

    /** True for a language written from right to left: Arabic, Hebrew, Persian, Urdu... */
    public static boolean isRightToLeft(final Locale locale) {
        final String script = locale.getScript();
        return script.isEmpty() ? RIGHT_TO_LEFT.contains(locale.getLanguage()) : RIGHT_TO_LEFT_SCRIPTS.contains(script);
    }
}
