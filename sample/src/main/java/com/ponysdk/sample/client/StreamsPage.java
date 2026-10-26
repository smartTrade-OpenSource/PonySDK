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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.Conflator;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;

/**
 * Streams (docs/decisions/0013-conflated-streams.md): a feed thread offers quotes faster than the screen can show
 * them, a {@link Conflator} applies the latest per instrument, one flush at a time.
 */
final class StreamsPage {

    private static final String[] INSTRUMENTS = { "EURUSD", "USDJPY", "GBPUSD", "AUDUSD", "USDCAD", "USDCHF", "NZDUSD",
            "EURGBP", "EURJPY", "GBPJPY", "EURCHF", "AUDJPY", "EURAUD", "EURCAD", "GBPCHF", "CADJPY", "AUDNZD", "USDSEK",
            "USDNOK", "USDSGD", "USDHKD", "USDMXN", "USDZAR", "USDTRY" };
    private static final int[] RATES = { 1_000, 10_000, 100_000 };

    private record Quote(String instrument, double bid) {
    }

    private StreamsPage() {
    }

    static PFlowPanel build(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Streams", "A feed thread offers quotes faster than the screen can show them: the latest quote of "
                + "each instrument is applied, one flush at a time, and the feed never waits for the browser.");

        final PFlowPanel board = Element.newPFlowPanel();
        board.addStyleName("stream-board");
        final Map<String, PLabel> prices = new HashMap<>();
        for (final String instrument : INSTRUMENTS) {
            final PFlowPanel tile = Element.newPFlowPanel();
            tile.addStyleName("stream-tile");
            final PLabel name = Element.newPLabel(instrument);
            name.addStyleName("stream-name");
            final PLabel price = Element.newPLabel("—");
            price.addStyleName("stream-price");
            tile.add(name, price);
            board.add(tile);
            prices.put(instrument, price);
        }

        // region conflator
        // Applied on this context's UI thread: the latest quote of each instrument, at most 20 flushes a second
        final Conflator<String, Quote> quotes = Conflator.<String, Quote> builder(uiContext,
            (instrument, quote) -> prices.get(instrument).setText(String.format(Locale.US, "%.5f", quote.bid())))
            .minInterval(Duration.ofMillis(50))
            .build();
        // The feed's thread offers each quote; offer() never blocks, whatever the browser's pace
        final AtomicInteger rate = new AtomicInteger(RATES[1]);
        Thread.ofVirtual().name("showcase-feed").start(() -> {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            long last = System.nanoTime();
            while (!quotes.isClosed()) {
                final long now = System.nanoTime();
                final long count = rate.get() * (now - last) / 1_000_000_000L; // the rate, whatever the sleep took
                last = now;
                for (long i = 0; i < count; i++) {
                    final String instrument = INSTRUMENTS[random.nextInt(INSTRUMENTS.length)];
                    quotes.offer(instrument, new Quote(instrument, 1 + random.nextDouble()));
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (final InterruptedException e) {
                    return;
                }
            }
        });
        p.addDestroyListener(event -> quotes.close()); // the page left: the feed stops with it
        // endregion

        final PFlowPanel rates = Element.newPFlowPanel();
        rates.addStyleName("stream-rates");
        final PButton[] rateButtons = new PButton[RATES.length];
        for (int r = 0; r < RATES.length; r++) {
            final int value = RATES[r];
            final PButton button = Element.newPButton(String.format(Locale.US, "%,d quotes/s", value));
            button.addStyleName("stream-rate");
            if (value == rate.get()) button.addStyleName("stream-rate-on");
            button.addClickHandler(e -> {
                rate.set(value);
                for (final PButton b : rateButtons) b.removeStyleName("stream-rate-on");
                button.addStyleName("stream-rate-on");
            });
            rateButtons[r] = button;
            rates.add(button);
        }

        final PElement offeredVal = el("perf-value", "—");
        final PElement appliedVal = el("perf-value", "—");
        final PElement conflatedVal = el("perf-value", "—");
        final PFlowPanel counters = Element.newPFlowPanel();
        counters.addStyleName("perf-grid");
        counters.add(perfCard(offeredVal, el("perf-unit", "quotes/s"), el("perf-label", "Offered by the feed")),
            perfCard(appliedVal, el("perf-unit", "values/s"), el("perf-label", "Applied to the screen")),
            perfCard(conflatedVal, el("perf-unit", "%"), el("perf-label", "Replaced before being shown")));

        p.add(rates, counters, board);
        p.add(codeToggle("the conflator and its feed", Source.code(StreamsPage.class, "conflator")));

        final long[] previous = { 0, 0 };
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            final long offered = quotes.getOffered();
            final long applied = quotes.getApplied();
            final long offeredPerSecond = offered - previous[0];
            final long appliedPerSecond = applied - previous[1];
            previous[0] = offered;
            previous[1] = applied;
            offeredVal.setInnerText(String.format(Locale.US, "%,d", offeredPerSecond));
            appliedVal.setInnerText(String.format(Locale.US, "%,d", appliedPerSecond));
            conflatedVal.setInnerText(offeredPerSecond == 0 ? "—"
                    : String.format(Locale.US, "%.1f", 100.0 * (offeredPerSecond - appliedPerSecond) / offeredPerSecond));
        }, Duration.ofSeconds(1)));
        return p;
    }
}
