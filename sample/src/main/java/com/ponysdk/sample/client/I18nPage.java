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

import static com.ponysdk.sample.client.Ui.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PDatePicker;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.i18n.Formats;
import com.ponysdk.core.ui.i18n.Messages;
import com.ponysdk.core.ui.i18n.PString;

/**
 * Internationalisation (docs/decisions/0022-locale.md): the locale of the context, from the browser's languages or
 * the visitor's choice; texts, numbers and dates in that locale; the page's direction for Arabic.
 */
final class I18nPage {

    // region messages
    /** i18n/Showcase.properties, Showcase_fr.properties, Showcase_ar.properties: the application's texts. */
    private static final Messages MESSAGES = Messages.of("i18n/Showcase");
    // endregion

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    private I18nPage() {
    }

    static PFlowPanel build(final UIContext uiContext) {
        final PFlowPanel p = section();
        pageTitle(p, "Internationalisation", "The locale of each page comes from the browser's languages, or the visitor's "
                + "choice, kept in a cookie. Switching reloads the page at its address, in the other language; Arabic "
                + "turns the whole page right to left.");

        // region switch
        final PFlowPanel languages = Element.newPFlowPanel();
        languages.addStyleName("stream-rates");
        for (final Locale locale : uiContext.getConfiguration().getSupportedLocales()) {
            final PButton button = Element.newPButton(nativeName(locale));
            button.addStyleName("stream-rate");
            if (locale.getLanguage().equals(uiContext.getLocale().getLanguage())) button.addStyleName("stream-rate-on");
            // Keeps the choice in a cookie, and reloads the page at its token: it comes back in the new locale
            button.addClickHandler(e -> uiContext.setLocale(locale));
            languages.add(button);
        }
        // endregion
        p.add(languages);

        final PFlowPanel screen = Element.newPFlowPanel();
        screen.addStyleName("i18n-screen");
        screen.add(text("i18n-title", MESSAGES.get("screen.title")));
        // region texts
        screen.add(text("i18n-greeting", MESSAGES.get("screen.greeting", "Nicolas")));
        final PLabel orders = Element.newPLabel(MESSAGES.get("screen.orders", 3)); // a plural: a choice in the text
        // endregion
        orders.addStyleName("i18n-orders");
        final int[] count = { 3 };
        final PFlowPanel stepper = Element.newPFlowPanel();
        stepper.addStyleName("i18n-stepper");
        final PButton less = Element.newPButton("-");
        final PButton more = Element.newPButton("+");
        less.setAttribute("aria-label", "-1");
        more.setAttribute("aria-label", "+1");
        less.addClickHandler(e -> orders.setText(MESSAGES.get("screen.orders", count[0] = Math.max(0, count[0] - 1))));
        more.addClickHandler(e -> orders.setText(MESSAGES.get("screen.orders", ++count[0])));
        stepper.add(less, orders, more);
        screen.add(stepper);

        // region formats
        final Formats formats = Formats.current(); // the context's locale
        screen.add(text("i18n-volume", MESSAGES.get("screen.volume", formats.number(1_234_567.891))));
        screen.add(text("i18n-amount", MESSAGES.get("screen.amount", formats.currency(1234.5, Currency.getInstance("EUR")))));
        screen.add(text("i18n-today", MESSAGES.get("screen.today", formats.date(TODAY, FormatStyle.FULL))));
        // endregion

        // region dates
        // The date box writes and reads its text with this format: the browser puts the names of the page's language
        final PDateBox dateBox = Element.newPDateBox(formats.dateFormat("d MMMM yyyy"));
        final PLabel picked = Element.newPLabel(MESSAGES.get("screen.nopick"));
        picked.addStyleName("i18n-picked");
        dateBox.addValueChangeHandler(e -> picked.setText(e.getData() == null ? MESSAGES.get("screen.nopick")
                : MESSAGES.get("screen.picked", formats.date(toLocalDate(e.getData()), FormatStyle.LONG))));
        final PDatePicker datePicker = Element.newPDatePicker(); // its months, days and first day of the week follow the page
        // endregion
        dateBox.addStyleName("i18n-datebox");
        final PElement dateLabel = text("i18n-date-label", MESSAGES.get("screen.date"));
        screen.add(dateLabel);
        screen.add(dateBox);
        screen.add(picked);
        screen.add(datePicker);

        // The framework's own messages: the same locale
        final PButton ok = Element.newPButton(PString.get("dialog.ok"));
        ok.addStyleName("i18n-ok");
        screen.add(ok);
        p.add(screen);

        p.add(codeToggle("the texts and the switch", Source.code(I18nPage.class, "messages") + Source.code(I18nPage.class, "switch")
                + Source.code(I18nPage.class, "texts")));
        p.add(codeToggle("numbers and dates", Source.code(I18nPage.class, "formats") + Source.code(I18nPage.class, "dates")));
        return p;
    }

    private static PElement text(final String cssClass, final String text) {
        return el(cssClass, text);
    }

    /** The language's name in that language, as a switch shows it: Français. */
    private static String nativeName(final Locale locale) {
        final String name = locale.getDisplayLanguage(locale);
        return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
    }

    /** A date box's date is midnight in its format's zone: the JVM's. */
    private static LocalDate toLocalDate(final Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
