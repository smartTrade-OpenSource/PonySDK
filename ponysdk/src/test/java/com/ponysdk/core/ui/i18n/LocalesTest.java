/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/** Which of the application's locales a visitor gets (docs/decisions/0022-locale.md). */
public class LocalesTest {

    private static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, Locale.FRENCH, Locale.forLanguageTag("ar"));

    @Test
    public void theCookieIsTheVisitorsChoiceAndComesFirst() {
        assertEquals(Locale.FRENCH, Locales.negotiate(SUPPORTED, "fr", "ar,en;q=0.8"));
    }

    @Test
    public void withoutACookieTheBestOfAcceptLanguageDecides() {
        assertEquals(Locale.FRENCH, Locales.negotiate(SUPPORTED, null, "de;q=0.9, fr;q=0.8, en;q=0.5"));
        assertEquals(Locale.ENGLISH, Locales.negotiate(SUPPORTED, null, "de, en;q=0.5, fr;q=0.2"));
    }

    @Test
    public void aRangeMatchesTheLocalesItIsAPrefixOf() {
        assertEquals(Locale.FRENCH, Locales.negotiate(SUPPORTED, null, "fr-CH, fr;q=0.9, en;q=0.8"), "fr-CH gets fr");
        assertEquals(Locale.ENGLISH, Locales.negotiate(SUPPORTED, null, "en-US,en;q=0.9"));
    }

    @Test
    public void aRangeWithoutPrefixMatchGetsTheLocaleOfItsLanguage() {
        final List<Locale> swiss = List.of(Locale.ENGLISH, Locale.forLanguageTag("de-CH"));
        assertEquals(Locale.forLanguageTag("de-CH"), Locales.negotiate(swiss, null, "de"));
        assertEquals(Locale.forLanguageTag("de-CH"), Locales.negotiate(swiss, null, "de-AT"));
    }

    @Test
    public void nothingThatFitsGivesTheFirstSupportedLocale() {
        assertEquals(Locale.ENGLISH, Locales.negotiate(SUPPORTED, null, "de, ja;q=0.5"));
        assertEquals(Locale.ENGLISH, Locales.negotiate(SUPPORTED, null, "*"));
        assertEquals(Locale.ENGLISH, Locales.negotiate(SUPPORTED, null, null));
        assertEquals(Locale.ENGLISH, Locales.negotiate(SUPPORTED, "de", "ja"));
    }

    @Test
    public void aValueThatIsMalformedOrNotSupportedIsIgnored() {
        assertEquals(Locale.FRENCH, Locales.negotiate(SUPPORTED, "not a locale!!", "fr"), "the header decides");
        assertEquals(Locale.FRENCH, Locales.negotiate(SUPPORTED, "de", "fr"), "a cookie of a locale no longer supported");
        assertEquals(Locale.ENGLISH, Locales.negotiate(SUPPORTED, null, "fr;q=nonsense"));
        assertNull(Locales.match(SUPPORTED, "!!"));
    }

    @Test
    public void rightToLeftLanguages() {
        assertTrue(Locales.isRightToLeft(Locale.forLanguageTag("ar")));
        assertTrue(Locales.isRightToLeft(Locale.forLanguageTag("ar-EG")));
        assertTrue(Locales.isRightToLeft(Locale.forLanguageTag("he-IL")));
        assertTrue(Locales.isRightToLeft(Locale.forLanguageTag("fa")));
        assertFalse(Locales.isRightToLeft(Locale.FRENCH));
        assertFalse(Locales.isRightToLeft(Locale.ENGLISH));
        assertTrue(Locales.isRightToLeft(Locale.forLanguageTag("ku-Arab")), "the script decides when there is one");
        assertFalse(Locales.isRightToLeft(Locale.forLanguageTag("ar-Latn")));
    }
}
