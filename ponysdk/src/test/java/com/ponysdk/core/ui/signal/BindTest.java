/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.signal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;

import jakarta.json.Json;

/**
 * Bindings on real contexts, not {@link com.ponysdk.test.PSuite}: its context is a Mockito spy, whose invocation
 * history keeps every registered object reachable. The drains run when the test says so.
 */
public class BindTest {

    private final ArrayDeque<Runnable> drains = new ArrayDeque<>();
    private UIContext ctx;

    private static UIContext context() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData())
            .thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(jakarta.servlet.http.HttpSession.class)));
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        return new UIContext(socket, new TxnContext(socket), config);
    }

    @BeforeEach
    public void setUp() {
        ctx = context();
        ctx.execute(() -> Bind.configure(Duration.ZERO, drains::add));
    }

    @AfterEach
    public void tearDown() {
        ctx.destroy();
        UIContext.remove();
    }

    /** What the context's drain thread does, here on the test's. */
    private void drain() {
        Runnable drain;
        while ((drain = drains.poll()) != null) drain.run();
    }

    private static <W extends PWidget> W added(final W widget) {
        PWindow.getMain().add(widget);
        return widget;
    }

    /** What an action throws: asserted outside {@code execute}, which logs what a runnable throws and goes on. */
    private static Throwable thrown(final Runnable action) {
        try {
            action.run();
            return null;
        } catch (final RuntimeException e) {
            return e;
        }
    }

    private static boolean eventually(final BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 500 && !condition.getAsBoolean(); i++) Thread.sleep(10);
        return condition.getAsBoolean();
    }

    @Test
    public void aLabelShowsTheSignalAtOnceThenFollowsIt() {
        final MutableSignal<String> signal = Signal.of("a");
        final PLabel[] label = new PLabel[1];
        ctx.execute(() -> Bind.text(label[0] = added(Element.newPLabel()), signal));
        assertEquals("a", label[0].getText());

        signal.set("b");
        assertEquals("a", label[0].getText(), "from the context's UI thread, not the setter's");
        drain();

        assertEquals("b", label[0].getText());
    }

    @Test
    public void aBurstBetweenTwoFlushesIsOneUpdateOfTheProperty() {
        final MutableSignal<Integer> signal = Signal.of(0);
        final AtomicInteger applied = new AtomicInteger();
        final PLabel[] label = new PLabel[1];
        ctx.execute(() -> Bind.to(label[0] = added(Element.newPLabel()), signal, (l, v) -> {
            applied.incrementAndGet();
            l.setText("" + v);
        }));
        assertEquals(1, applied.get());

        for (int i = 1; i <= 1_000; i++) signal.set(i);
        assertEquals(1, drains.size(), "one drain for them all");
        drain();

        assertEquals(2, applied.get());
        assertEquals("1000", label[0].getText());
    }

    @Test
    public void manyBindingsThatChangeTogetherAreOneDrainAndOneTransaction() {
        final List<MutableSignal<String>> signals = new ArrayList<>();
        final List<PLabel> labels = new ArrayList<>();
        ctx.execute(() -> {
            for (int i = 0; i < 200; i++) {
                final MutableSignal<String> signal = Signal.of("a");
                signals.add(signal);
                final PLabel label = added(Element.newPLabel());
                labels.add(label);
                Bind.text(label, signal);
            }
        });

        signals.forEach(signal -> signal.set("b"));
        assertEquals(1, drains.size(), "the first binding asked for it, the others ride it");
        drain();

        assertTrue(labels.stream().allMatch(label -> "b".equals(label.getText())));
        signals.forEach(signal -> signal.set("c"));
        drain();
        assertTrue(labels.stream().allMatch(label -> "c".equals(label.getText())), "and the next drain does the same");
    }

    @Test
    public void aBindingThatFailsDoesNotStopTheOthersOfTheDrain() {
        final MutableSignal<String> signal = Signal.of("a");
        final PLabel[] labels = new PLabel[2];
        ctx.execute(() -> {
            Bind.to(labels[0] = added(Element.newPLabel()), signal, (label, text) -> {
                if (text.equals("b")) throw new IllegalStateException("cannot show " + text);
            });
            Bind.text(labels[1] = added(Element.newPLabel()), signal);
        });

        signal.set("b");
        drain();

        assertEquals("b", labels[1].getText());
    }

    @Test
    public void aComputedSignalIsComputedOnceForTheBurst() {
        final MutableSignal<Integer> price = Signal.of(1);
        final MutableSignal<Integer> quantity = Signal.of(1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<String> total = Signal.computed(() -> {
            computed.incrementAndGet();
            return "" + price.get() * quantity.get();
        });
        final PLabel[] label = new PLabel[1];
        ctx.execute(() -> Bind.text(label[0] = added(Element.newPLabel()), total));
        assertEquals(1, computed.get());

        for (int i = 2; i <= 100; i++) {
            price.set(i);
            quantity.set(i);
        }
        drain();

        assertEquals("10000", label[0].getText());
        assertEquals(2, computed.get());
    }

    @Test
    public void aButtonFollowsItsTextItsVisibilityAndItsState() {
        final MutableSignal<String> text = Signal.of("Buy");
        final MutableSignal<Boolean> on = Signal.of(true);
        final PButton[] button = new PButton[1];
        ctx.execute(() -> {
            button[0] = added(Element.newPButton());
            Bind.text(button[0], text);
            Bind.visible(button[0], on);
            Bind.enabled(button[0], on);
            Bind.styleName(button[0], "live", on);
        });
        assertEquals("Buy", button[0].getText());
        assertTrue(button[0].isVisible() && button[0].isEnabled() && button[0].hasStyleName("live"));

        text.set("Sell");
        on.set(false);
        drain();

        assertEquals("Sell", button[0].getText());
        assertFalse(button[0].isVisible());
        assertFalse(button[0].isEnabled());
        assertFalse(button[0].hasStyleName("live"));
        on.set(true);
        drain();
        assertTrue(button[0].isVisible() && button[0].isEnabled() && button[0].hasStyleName("live"));
    }

    @Test
    public void aTextBoxAndItsSignalFollowEachOther() {
        final MutableSignal<String> signal = Signal.of("EUR");
        final PTextBox[] box = new PTextBox[1];
        final Binding[] binding = new Binding[1];
        ctx.execute(() -> binding[0] = Bind.value(box[0] = added(Element.newPTextBox()), signal));
        assertEquals("EUR", box[0].getValue());

        signal.set("USD");
        drain();
        assertEquals("USD", box[0].getValue());

        ctx.execute(() -> box[0].onClientData(
            Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), "CHF").build()));
        assertEquals("CHF", signal.get(), "the user's input is the signal's value");
        drain();
        assertEquals("CHF", box[0].getValue());
        assertTrue(drains.isEmpty(), "the box's own input does not come back to it");

        binding[0].close();
        assertTrue(box[0].getValueChangeHandlers().isEmpty());
    }

    @Test
    public void aCheckBoxAndItsSignalFollowEachOther() {
        final MutableSignal<Boolean> signal = Signal.of(false);
        final PCheckBox[] box = new PCheckBox[1];
        ctx.execute(() -> Bind.value(box[0] = added(Element.newPCheckBox()), signal));

        signal.set(true);
        drain();
        assertTrue(box[0].getValue());
        ctx.execute(() -> box[0].onClientData(
            Json.createObjectBuilder().add(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue(), false).build()));

        assertFalse(signal.get());
    }

    @Test
    public void aClosedBindingKeepsWhatTheWidgetShowsAndFollowsNoMore() {
        final MutableSignal<String> signal = Signal.of("a");
        final PLabel[] label = new PLabel[1];
        final Binding[] binding = new Binding[1];
        ctx.execute(() -> binding[0] = Bind.text(label[0] = added(Element.newPLabel()), signal));
        signal.set("b"); // asked for, not run yet

        binding[0].close();
        binding[0].close();
        drain();
        signal.set("c");
        drain();

        assertEquals("a", label[0].getText());
        assertTrue(drains.isEmpty());
    }

    @Test
    public void aBindingFollowsTheContextOfItsWidgetWhateverContextsFollowTheSignal() {
        final UIContext other = context();
        final ArrayDeque<Runnable> otherDrains = new ArrayDeque<>();
        final MutableSignal<String> signal = Signal.of("a");
        final PLabel[] labels = new PLabel[2];
        other.execute(() -> {
            Bind.configure(Duration.ZERO, otherDrains::add);
            Bind.text(labels[1] = added(Element.newPLabel()), signal);
        });
        ctx.execute(() -> Bind.text(labels[0] = added(Element.newPLabel()), signal));

        signal.set("b");
        assertEquals(1, drains.size());
        assertEquals(1, otherDrains.size());
        other.destroy(); // one context ends: the other follows still
        otherDrains.forEach(Runnable::run);
        drain();

        assertEquals("b", labels[0].getText());
        assertEquals("a", labels[1].getText());
    }

    @Test
    public void aKeyedListKeepsTheWidgetOfAKeyThatStays() {
        record Row(String id, String name) {
        }
        final MutableSignal<List<Row>> rows = Signal.of(List.of(new Row("a", "A"), new Row("b", "B"), new Row("c", "C")));
        final List<String> made = new ArrayList<>();
        final PFlowPanel[] panel = new PFlowPanel[1];
        ctx.execute(() -> Bind.children(panel[0] = added(Element.newPFlowPanel()), rows, Row::id, row -> {
            made.add(row.id());
            return Element.newPLabel(row.name());
        }));
        final PWidget a = panel[0].getWidget(0), b = panel[0].getWidget(1), c = panel[0].getWidget(2);
        final boolean[] destroyed = {false};
        b.addDestroyListener(o -> destroyed[0] = true);

        rows.set(List.of(new Row("c", "C"), new Row("a", "A2"), new Row("d", "D")));
        drain();

        assertEquals(3, panel[0].getWidgetCount());
        assertSame(c, panel[0].getWidget(0));
        assertSame(a, panel[0].getWidget(1), "the key stays, so does its widget");
        assertEquals("A", ((PLabel) a).getText(), "its item's changes are for the widget to follow");
        assertEquals("D", ((PLabel) panel[0].getWidget(2)).getText());
        assertEquals(List.of("a", "b", "c", "d"), made);
        assertNull(b.getParent());
        assertTrue(destroyed[0], "a widget whose key is gone is destroyed");

        rows.set(List.of());
        drain();
        assertEquals(0, panel[0].getWidgetCount());
    }

    @Test
    public void aKeyedListReordersItsWidgetsWithoutMakingOthers() {
        final MutableSignal<List<Integer>> items = Signal.of(List.of(1, 2, 3, 4));
        final AtomicInteger made = new AtomicInteger();
        final PFlowPanel[] panel = new PFlowPanel[1];
        ctx.execute(() -> Bind.children(panel[0] = added(Element.newPFlowPanel()), items, i -> i, i -> {
            made.incrementAndGet();
            return Element.newPLabel("" + i);
        }));
        final List<PWidget> before = List.of(panel[0].getWidget(0), panel[0].getWidget(1), panel[0].getWidget(2), panel[0].getWidget(3));

        items.set(List.of(4, 3, 2, 1));
        drain();

        assertEquals(4, made.get());
        assertEquals(List.of(before.get(3), before.get(2), before.get(1), before.get(0)),
            List.of(panel[0].getWidget(0), panel[0].getWidget(1), panel[0].getWidget(2), panel[0].getWidget(3)));
    }

    @Test
    public void aListWithTwoItemsOfAKeyIsRefusedAndLeavesNoBinding() {
        final MutableSignal<List<String>> items = Signal.of(List.of("a", "a"));
        final PFlowPanel[] panel = new PFlowPanel[1];
        final Throwable[] refused = new Throwable[1];

        ctx.execute(() -> {
            panel[0] = added(Element.newPFlowPanel());
            refused[0] = thrown(() -> Bind.children(panel[0], items, s -> s, Element::newPLabel));
        });

        assertInstanceOf(IllegalArgumentException.class, refused[0]);
        assertEquals(0, panel[0].getWidgetCount());
        items.set(List.of("a"));
        assertTrue(drains.isEmpty(), "the refused binding does not follow the signal");
    }

    @Test
    public void aRowThatCannotBeMadeLeavesTheOnesMadeBeforeItDestroyed() {
        final MutableSignal<List<Integer>> items = Signal.of(List.of(1));
        final PFlowPanel[] panel = new PFlowPanel[1];
        final List<Integer> destroyed = new ArrayList<>();
        ctx.execute(() -> Bind.children(panel[0] = added(Element.newPFlowPanel()), items, i -> i, i -> {
            if (i == 3) throw new IllegalStateException("no row for " + i);
            final PLabel label = Element.newPLabel("" + i);
            label.addDestroyListener(o -> destroyed.add(i));
            return label;
        }));
        final PWidget one = panel[0].getWidget(0);

        items.set(List.of(1, 2, 3));
        drain(); // the failure is the context's to log: the drain goes on

        assertEquals(1, panel[0].getWidgetCount());
        assertSame(one, panel[0].getWidget(0));
        assertEquals(List.of(2), destroyed, "the row of 2, made before the failure, was destroyed");
        items.set(List.of(1, 2));
        drain();
        assertEquals(2, panel[0].getWidgetCount(), "and the binding goes on");
    }

    @Test
    public void aBindingEndsWithItsWidgetAndNothingKeepsTheWidget() {
        final MutableSignal<Integer> source = Signal.of(1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<String> text = Signal.computed(() -> {
            computed.incrementAndGet();
            return "" + source.get();
        });
        final WeakReference<PLabel> reference = new WeakReference<>(boundThenDestroyed(text));
        assertEquals(1, computed.get());

        source.set(2);
        drain();

        assertEquals(1, computed.get(), "nothing follows the computed signal: it did not compute");
        assertTrue(SignalTest.collected(reference), "the signal, which outlives the widget, does not keep it");
    }

    private PLabel boundThenDestroyed(final Signal<String> text) {
        final PLabel[] label = new PLabel[1];
        ctx.execute(() -> {
            Bind.text(label[0] = added(Element.newPLabel()), text);
            label[0].removeFromParent();
            label[0].onDestroy();
        });
        return label[0];
    }

    @Test
    public void aBindingEndsWithItsContextAndNothingKeepsTheWidget() {
        final MutableSignal<String> signal = Signal.of("a");
        final AtomicInteger computed = new AtomicInteger();
        final Signal<String> text = Signal.computed(() -> {
            computed.incrementAndGet();
            return signal.get();
        });
        final UIContext doomed = context();
        final WeakReference<PButton> reference = new WeakReference<>(boundIn(doomed, text));
        final int before = computed.get();

        doomed.destroy();
        signal.set("b");

        assertEquals(1, before);
        assertEquals("b", text.get());
        assertEquals(2, computed.get(), "only the read above computed: no binding is left on the signal");
        assertTrue(SignalTest.collected(reference), "the signal, which outlives the context, does not keep its widgets");
    }

    private static PButton boundIn(final UIContext doomed, final Signal<String> text) {
        final PButton[] button = new PButton[1];
        doomed.execute(() -> Bind.text(button[0] = added(Element.newPButton()), text));
        return button[0];
    }

    @Test
    public void aBindingOfAWidgetOutsideTheWindowsEndsWithItsContextToo() {
        final MutableSignal<String> signal = Signal.of("a");
        final AtomicInteger computed = new AtomicInteger();
        final Signal<String> text = Signal.computed(() -> {
            computed.incrementAndGet();
            return signal.get();
        });
        final UIContext doomed = context();
        final PLabel[] label = new PLabel[1];
        doomed.execute(() -> Bind.text(label[0] = Element.newPLabel(), text)); // in no window: its context's destroy does not reach it
        final WeakReference<PLabel> reference = new WeakReference<>(label[0]);
        label[0] = null;

        doomed.destroy();
        signal.set("b");

        assertEquals("b", text.get());
        assertEquals(2, computed.get(), "nothing followed the computed signal after the destroy");
        assertTrue(SignalTest.collected(reference));
    }

    @Test
    public void aBindingIsMadeOnTheUiThreadOfAContext() {
        final PLabel[] label = new PLabel[1];
        ctx.execute(() -> label[0] = Element.newPLabel());

        assertThrows(IllegalStateException.class, () -> Bind.text(label[0], Signal.of("a")));
        assertThrows(IllegalStateException.class, () -> Bind.configure(Duration.ZERO, null));
    }

    @Test
    public void aContextIsConfiguredBeforeItsFirstBindingOnly() {
        final Throwable[] refused = new Throwable[1];
        final UIContext fresh = context();
        fresh.execute(() -> {
            Bind.text(Element.newPLabel(), Signal.of("a"));
            refused[0] = thrown(() -> Bind.configure(Duration.ZERO, null));
        });
        final Throwable[] configured = new Throwable[1];
        ctx.execute(() -> configured[0] = thrown(() -> Bind.configure(Duration.ofMillis(50), null)));

        assertInstanceOf(IllegalStateException.class, refused[0]);
        assertInstanceOf(IllegalStateException.class, configured[0], "configured in setUp already");
        fresh.destroy();
    }

    @Test
    public void aFeedThreadSetsAndTheLabelEndsOnTheLastValue() throws InterruptedException {
        final UIContext live = context();
        final MutableSignal<Integer> signal = Signal.of(0);
        final PLabel[] labels = new PLabel[1];
        live.execute(() -> Bind.text(labels[0] = added(Element.newPLabel()), Signal.computed(() -> "" + signal.get())));
        final PLabel label = labels[0];

        final Thread feed = Thread.ofVirtual().start(() -> {
            for (int i = 1; i <= 100_000; i++) signal.set(i);
        });
        feed.join();

        assertTrue(eventually(() -> "100000".equals(label.getText())), "the latest value is shown: " + label.getText());
        live.destroy();
    }
}
