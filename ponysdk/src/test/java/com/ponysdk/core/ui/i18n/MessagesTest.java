/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.MissingResourceException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.test.PSuite;

/** The texts of an application, in the locale of its context (docs/decisions/0022-locale.md). */
public class MessagesTest extends PSuite {

    private static final Locale FR_CA = Locale.forLanguageTag("fr-CA");
    private static final Locale AR = Locale.forLanguageTag("ar");

    private final Messages messages = Messages.of("i18n/Test");

    @AfterEach
    public void resetContext() {
        Mockito.doReturn(Locale.ENGLISH).when(UIContext.get()).getLocale(); // PSuite's
    }

    @Test
    public void aTextInTheLocalesLanguage() {
        assertEquals("Hello", messages.get(Locale.ENGLISH, "greeting"));
        assertEquals("Bonjour", messages.get(Locale.FRENCH, "greeting"));
    }

    @Test
    public void theChainIsTheLocaleThenItsLanguageThenTheBaseFile() {
        assertEquals("Allo", messages.get(FR_CA, "greeting"), "fr_CA");
        assertEquals("Commande 7", messages.get(FR_CA, "order.title", 7), "fr_CA has none: its language, fr");
        assertEquals("From the base", messages.get(FR_CA, "only.base"), "fr has none either: the base file");
        assertEquals("Hello", messages.get(AR, "greeting"), "a language without a file: the base");
    }

    @Test
    public void theJvmsDefaultLocaleIsNoPartOfTheChain() {
        final Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.FRENCH);
        try {
            assertEquals("Hello", messages.get(AR, "greeting"), "the JVM speaks French, the visitor Arabic: the base file");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void theBundlesAreSearchedInTheOrderTheyAreNamed() {
        final Messages both = Messages.of("i18n/Test", "i18n/Other");
        assertEquals("Hello", both.get(Locale.ENGLISH, "greeting"), "the first has it");
        assertEquals("From the second bundle", both.get(Locale.ENGLISH, "other.key"));
        assertEquals("Shadowed by the first bundle", Messages.of("i18n/Other", "i18n/Test").get(Locale.ENGLISH, "greeting"));
    }

    @Test
    public void argumentsAreFormattedInTheLocale() {
        assertEquals("Order 1,234", messages.get(Locale.ENGLISH, "order.title", 1234));
        assertEquals("Commande 1\u202f234", messages.get(Locale.FRENCH, "order.title", 1234), "a narrow no-break space");
        assertEquals("Total 1,234.50", messages.get(Locale.ENGLISH, "price", 1234.5));
    }

    @Test
    public void aPluralIsAChoice() {
        assertEquals("no order", messages.get(Locale.ENGLISH, "order.count", 0));
        assertEquals("one order", messages.get(Locale.ENGLISH, "order.count", 1));
        assertEquals("3 orders", messages.get(Locale.ENGLISH, "order.count", 3));
        assertEquals("aucune commande", messages.get(Locale.FRENCH, "order.count", 0));
        assertEquals("une commande", messages.get(Locale.FRENCH, "order.count", 1));
        assertEquals("12 commandes", messages.get(Locale.FRENCH, "order.count", 12));
    }

    @Test
    public void aTextWithoutArgumentsIsReturnedAsWrittenAndWithThemAQuoteIsDoubled() {
        assertEquals("It''s {0}", messages.get(Locale.ENGLISH, "quote.text"), "no arguments: as written");
        assertEquals("It's 5", messages.get(Locale.ENGLISH, "quote.text", 5));
        assertEquals("C'est 5", messages.get(Locale.FRENCH, "quote.text", 5));
    }

    @Test
    public void aMissingKeyGivesItsNameBetweenBangs() {
        assertEquals("!no.such.key!", messages.get(Locale.FRENCH, "no.such.key"));
        assertEquals("!no.such.key!", messages.get(Locale.FRENCH, "no.such.key"), "reported once, the same answer");
    }

    @Test
    public void theKeysReportedAreBounded() {
        for (int i = 0; i < 1_100; i++) assertEquals("!k" + i + "!", messages.get(Locale.ENGLISH, "k" + i));
        assertEquals("Broken {0", messages.get(Locale.ENGLISH, "broken", 1), "a pattern in error is not reported past the bound either");
    }

    @Test
    public void aPatternThatIsNotValidGivesTheTextAsWritten() {
        assertEquals("Cassé {0", messages.get(Locale.FRENCH, "broken", 1));
        assertEquals("Broken {0", messages.get(Locale.ENGLISH, "broken", 1));
        assertEquals("Broken {0", messages.get(Locale.ENGLISH, "broken", 1), "reported once");
    }

    @Test
    public void theLocaleIsTheCurrentContexts() {
        Mockito.doReturn(Locale.FRENCH).when(UIContext.get()).getLocale();
        assertEquals("Commande 7", messages.get("order.title", 7));
        Mockito.doReturn(Locale.ENGLISH).when(UIContext.get()).getLocale();
        assertEquals("Order 7", messages.get("order.title", 7));
    }

    @Test
    public void withoutAContextTheLocaleIsToBeGiven() {
        final UIContext context = UIContext.get();
        UIContext.remove();
        try {
            assertThrows(IllegalStateException.class, () -> messages.get("greeting"));
            assertEquals("Bonjour", messages.get(Locale.FRENCH, "greeting"));
        } finally {
            UIContext.setCurrent(context);
        }
    }

    @Test
    public void aBundleThatDoesNotExistIsAStartupError() {
        assertThrows(MissingResourceException.class, () -> Messages.of("i18n/Nothing"));
        assertThrows(IllegalArgumentException.class, Messages::of);
    }
}
