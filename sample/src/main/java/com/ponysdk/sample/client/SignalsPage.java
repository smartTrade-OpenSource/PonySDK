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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.signal.Bind;
import com.ponysdk.core.ui.signal.MutableSignal;
import com.ponysdk.core.ui.signal.Signal;

/**
 * Signals (docs/decisions/0019-signals.md): a feed thread sets signals, widgets bound to them follow, on the UI thread,
 * once per flush. A computed total, a text box bound both ways, a list of rows kept by key.
 */
final class SignalsPage {

    private static final String[] INSTRUMENTS = { "EURUSD", "USDJPY", "GBPUSD", "AUDUSD", "USDCAD", "USDCHF", "NZDUSD", "EURGBP" };
    private static final int FEED_RATE = 5_000; // signals set per second, by the feed

    private SignalsPage() {
    }

    /** Bound to the signals of one instrument: what a tile and a row of the list show. */
    private record Quote(Signal<String> text, MutableSignal<Boolean> rising) {
    }

    static PFlowPanel build(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Signals", "A feed thread sets values; the widgets bound to them follow, on the UI thread, once per "
                + "flush however many values came before it. A computed total, a text box bound both ways, a list that keeps "
                + "the widget of a key.");

        final AtomicLong sets = new AtomicLong();
        final AtomicLong recomputed = new AtomicLong();
        final PFlowPanel board = Element.newPFlowPanel();
        board.addStyleName("stream-board");
        final Map<String, MutableSignal<Double>> bids = new LinkedHashMap<>();
        final Map<String, Quote> quotes = new LinkedHashMap<>();

        // region ticker
        // A signal per value: the feed's thread sets it, whatever the browser's pace
        for (final String instrument : INSTRUMENTS) {
            final MutableSignal<Double> bid = Signal.of(1.0);
            final MutableSignal<Boolean> rising = Signal.of(true);
            bids.put(instrument, bid);
            // Computed from the bid: once per flush, not once per set
            final Signal<String> text = Signal.computed(() -> {
                recomputed.incrementAndGet();
                return String.format(Locale.US, "%.5f", bid.get());
            });
            quotes.put(instrument, new Quote(text, rising));
            // Bound to the widgets: the text, and a style name while the bid rises
            final PLabel price = Element.newPLabel();
            price.addStyleName("stream-price");
            price.addStyleName("signal-price");
            Bind.text(price, text);
            Bind.styleName(price, "signal-up", rising);
            board.add(tile(instrument, price));
        }
        final AtomicBoolean stopped = new AtomicBoolean();
        Thread.ofVirtual().name("showcase-signals-feed").start(() -> {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            long last = System.nanoTime();
            while (!stopped.get() && uiContext.isAlive()) {
                final long now = System.nanoTime();
                final long count = FEED_RATE * (now - last) / 1_000_000_000L;
                last = now;
                for (long i = 0; i < count; i++) {
                    final String instrument = INSTRUMENTS[random.nextInt(INSTRUMENTS.length)];
                    final MutableSignal<Double> bid = bids.get(instrument);
                    final double previous = bid.get();
                    final double next = Math.max(0.5, previous + random.nextDouble(-0.002, 0.002));
                    quotes.get(instrument).rising().set(next >= previous);
                    bid.set(next);
                    sets.addAndGet(2);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (final InterruptedException e) {
                    return;
                }
            }
        });
        p.addDestroyListener(event -> stopped.set(true)); // the page left: the feed stops with it
        // endregion

        // region twoway
        // The box shows the signal, and what the user types sets it: the other bindings follow
        final MutableSignal<String> quantity = Signal.of("100");
        final Signal<Integer> units = Signal.computed(() -> parse(quantity.get()));
        final PTextBox box = Element.newPTextBox();
        box.addStyleName("signal-quantity");
        box.setAttribute("aria-label", "Quantity");
        Bind.value(box, quantity);
        final PLabel invalid = Element.newPLabel("Not a whole number");
        invalid.addStyleName("signal-invalid");
        Bind.visible(invalid, Signal.computed(() -> units.get() == null));
        final PButton reset = Element.newPButton("Back to 100");
        Bind.enabled(reset, Signal.computed(() -> !"100".equals(quantity.get())));
        reset.addClickHandler(e -> quantity.set("100"));
        final PLabel held = Element.newPLabel();
        held.addStyleName("signal-held");
        Bind.text(held, Signal.computed(() -> "The signal holds \"" + quantity.get() + "\""));
        // A computed signal of computed signals: the total follows the 8 bids and the quantity
        final Signal<String> total = Signal.computed(() -> {
            final Integer n = units.get();
            if (n == null) return "—";
            double sum = 0;
            for (final MutableSignal<Double> bid : bids.values()) sum += bid.get();
            return String.format(Locale.US, "%,.2f", n * sum);
        });
        final PLabel totalLabel = Element.newPLabel();
        totalLabel.addStyleName("signal-total");
        Bind.text(totalLabel, total);
        // endregion

        // region list
        // The panel's children follow a list of keys: a key that stays keeps its widget, even moved
        final MutableSignal<List<String>> watched = Signal.of(List.of(INSTRUMENTS[0], INSTRUMENTS[1], INSTRUMENTS[2]));
        final MutableSignal<Integer> made = Signal.of(0);
        final PFlowPanel rows = Element.newPFlowPanel();
        rows.addStyleName("signal-rows");
        Bind.children(rows, watched, instrument -> instrument, instrument -> {
            made.update(n -> n + 1);
            final Quote quote = quotes.get(instrument);
            final PFlowPanel row = Element.newPFlowPanel();
            row.addStyleName("signal-row");
            final PLabel price = Element.newPLabel();
            price.addStyleName("signal-price");
            Bind.text(price, quote.text());
            Bind.styleName(price, "signal-up", quote.rising());
            row.add(el("signal-row-name", instrument), price);
            return row;
        });
        final PLabel madeLabel = Element.newPLabel();
        madeLabel.addStyleName("signal-made");
        Bind.text(madeLabel, Signal.computed(() -> watched.get().size() + " rows on screen, " + made.get() + " made"));
        // endregion

        final PButton add = Element.newPButton("Add a row");
        add.addClickHandler(e -> watched.update(list -> {
            final List<String> next = new ArrayList<>(list);
            for (final String instrument : INSTRUMENTS) {
                if (!next.contains(instrument)) {
                    next.add(instrument);
                    break;
                }
            }
            return List.copyOf(next);
        }));
        final PButton removeFirst = Element.newPButton("Remove the first");
        removeFirst.addClickHandler(e -> watched.update(list -> list.isEmpty() ? list : List.copyOf(list.subList(1, list.size()))));
        final PButton reverse = Element.newPButton("Reverse");
        reverse.addClickHandler(e -> watched.update(list -> {
            final List<String> next = new ArrayList<>(list);
            Collections.reverse(next);
            return List.copyOf(next);
        }));
        final PFlowPanel actions = Element.newPFlowPanel();
        actions.addStyleName("stream-rates");
        actions.add(add, removeFirst, reverse);

        final PElement setsVal = el("perf-value", "—");
        final PElement recomputedVal = el("perf-value", "—");
        final PElement replacedVal = el("perf-value", "—");
        final PFlowPanel counters = Element.newPFlowPanel();
        counters.addStyleName("perf-grid");
        counters.add(perfCard(setsVal, el("perf-unit", "sets/s"), el("perf-label", "Signals set by the feed")),
            perfCard(recomputedVal, el("perf-unit", "values/s"), el("perf-label", "Computed and shown")),
            perfCard(replacedVal, el("perf-unit", "%"), el("perf-label", "Replaced before being shown")));

        final PFlowPanel position = Element.newPFlowPanel();
        position.addStyleName("signal-position");
        position.add(field("Quantity", box), invalid, field("", reset), held, field("Total", totalLabel));

        p.add(counters, board, sectionTitle("A text box bound both ways, a computed total"), position,
            codeToggle("the ticker and its feed", Source.code(SignalsPage.class, "ticker")),
            codeToggle("the text box and the total", Source.code(SignalsPage.class, "twoway")),
            sectionTitle("A list that keeps the widget of a key"), actions, rows, madeLabel,
            codeToggle("the list", Source.code(SignalsPage.class, "list")));

        final long[] previous = { 0, 0 };
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            final long setsNow = sets.get();
            final long recomputedNow = recomputed.get();
            final long setsPerSecond = setsNow - previous[0];
            final long shownPerSecond = recomputedNow - previous[1];
            previous[0] = setsNow;
            previous[1] = recomputedNow;
            setsVal.setInnerText(String.format(Locale.US, "%,d", setsPerSecond));
            recomputedVal.setInnerText(String.format(Locale.US, "%,d", shownPerSecond));
            replacedVal.setInnerText(setsPerSecond == 0 ? "—"
                    : String.format(Locale.US, "%.1f", 100.0 * Math.max(0, setsPerSecond - shownPerSecond) / setsPerSecond));
        }, Duration.ofSeconds(1)));
        return p;
    }

    private static PFlowPanel field(final String label, final PWidget widget) {
        final PFlowPanel field = Element.newPFlowPanel();
        field.addStyleName("signal-field");
        field.add(el("signal-field-label", label), widget);
        return field;
    }

    private static PFlowPanel tile(final String instrument, final PLabel price) {
        final PFlowPanel tile = Element.newPFlowPanel();
        tile.addStyleName("stream-tile");
        final PLabel name = Element.newPLabel(instrument);
        name.addStyleName("stream-name");
        tile.add(name, price);
        return tile;
    }

    /** The quantity typed, or null when it is not a whole number. */
    private static Integer parse(final String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
