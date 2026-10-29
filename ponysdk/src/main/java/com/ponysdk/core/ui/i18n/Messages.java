/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.i18n;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.UIContext;

/**
 * The texts of an application, from the {@link ResourceBundle}s it names, in the locale of the current
 * {@link UIContext} (docs/decisions/0022-locale.md):
 *
 * <pre>
 * static final Messages MESSAGES = Messages.of("i18n/Showcase");   // i18n/Showcase.properties, Showcase_fr.properties...
 * label.setText(MESSAGES.get("order.title", order.id()));          // "Order {0}"
 * </pre>
 *
 * A key is looked up in the bundles in the order they are named; in each, the bundle of the locale, then of its
 * language, then the base one ({@code fr_CA}, {@code fr}, base), never the JVM's default locale. A text with
 * arguments is a {@link MessageFormat} pattern, in the locale: numbers and dates follow it, a quote is written
 * twice ({@code l''été}), and a plural is a choice ({@code {0,choice,0#no order|1#one order|1<{0} orders}}). A text
 * without arguments is returned as written. A key no bundle has gives {@code !key!} and one warning.
 */
public final class Messages {

    private static final Logger log = LoggerFactory.getLogger(Messages.class);

    /** The bundles of a locale and its language, then the base one: the locale of the JVM is no part of it. */
    private static final ResourceBundle.Control CONTROL = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);
    /** The keys already reported, up to a bound: a key made from a visitor's input must not grow it. */
    private static final int MAX_REPORTED = 1_000;

    private record Pattern(Locale locale, String text) {
    }

    private final List<String> baseNames;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();
    /** The patterns of the bundles, parsed once: a MessageFormat is not thread-safe, each call formats with a copy. */
    private final Map<Pattern, MessageFormat> parsed = new ConcurrentHashMap<>();

    private Messages(final List<String> baseNames) {
        this.baseNames = baseNames;
    }

    /**
     * The messages of these bundles, e.g. {@code "i18n/App"} for {@code i18n/App.properties}.
     *
     * @throws MissingResourceException when a bundle has no base file: a name that is wrong shows at startup
     */
    public static Messages of(final String... baseNames) {
        if (baseNames.length == 0) throw new IllegalArgumentException("no bundle");
        for (final String baseName : baseNames) bundle(baseName, Locale.ROOT);
        return new Messages(List.of(baseNames));
    }

    /** The bundle of the locale, its language and the base file, under the JVM-independent chain. */
    static ResourceBundle bundle(final String baseName, final Locale locale) {
        return ResourceBundle.getBundle(baseName, locale, CONTROL);
    }

    /** The text of the key in the locale of the current context, its arguments formatted in that locale. */
    public String get(final String key, final Object... args) {
        final UIContext context = UIContext.get();
        if (context == null) throw new IllegalStateException("No UIContext on this thread: pass the locale, get(locale, key, ...)");
        return get(context.getLocale(), key, args);
    }

    /** The text of the key in that locale: from a thread that is not the context's, with {@code uiContext.getLocale()}. */
    public String get(final Locale locale, final String key, final Object... args) {
        for (final String baseName : baseNames) {
            final ResourceBundle bundle = bundle(baseName, locale);
            if (bundle.containsKey(key)) return format(locale, key, bundle.getString(key), args);
        }
        if (reported.size() < MAX_REPORTED && reported.add(locale.toLanguageTag() + ' ' + key)) {
            log.warn("No message {} for {} in {}", key, locale.toLanguageTag(), baseNames);
        }
        return "!" + key + "!";
    }

    private String format(final Locale locale, final String key, final String pattern, final Object[] args) {
        if (args.length == 0) return pattern;
        try {
            return ((MessageFormat) parsed.computeIfAbsent(new Pattern(locale, pattern), p -> new MessageFormat(pattern, locale)).clone())
                .format(args);
        } catch (final IllegalArgumentException e) {
            if (reported.size() < MAX_REPORTED && reported.add(locale.toLanguageTag() + ' ' + key)) {
                log.error("The message {} for {} is not a valid pattern: {}", key, locale.toLanguageTag(), e.getMessage());
            }
            return pattern; // a translation file's error does not take a page down
        }
    }
}
