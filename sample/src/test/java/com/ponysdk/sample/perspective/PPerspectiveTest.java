/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.perspective;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.sample.perspective.PPerspective.Column;

/**
 * PPerspective (docs/decisions/0029-perspective.md): what a flush sends to the terminal, what is merged before it, what
 * is refused, and what the terminal reports back. A real context, as the chart's feed test has: the conflator's flush
 * runs on its thread.
 */
class PPerspectiveTest {

    private Application application;
    private UIContext context;
    /** What the terminal receives: the model and its value, in order. */
    private final List<Object[]> written = new ArrayList<>();

    @BeforeEach
    void startContext() {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setStringDictionaryEnabled(false);
        application = new Application("perspective", Mockito.mock(HttpSession.class), config);
        SessionManager.get().registerApplication(application);
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(HttpSession.class)));
        Mockito.doAnswer(call -> {
            synchronized (written) {
                written.add(new Object[] { call.getArgument(0), call.getArgument(1) });
            }
            return null;
        }).when(socket).encode(Mockito.any(), Mockito.any());
        final TxnContext txnContext = new TxnContext(socket);
        txnContext.setApplication(application);
        context = new UIContext(socket, txnContext, config);
        application.registerUIContext(context);
    }

    @AfterEach
    void stopContext() {
        context.destroy();
        SessionManager.get().unregisterApplication(application);
        UIContext.remove();
    }

    /** Runs on the context's thread and waits for the result. */
    private <T> T onUi(final Supplier<T> action) throws InterruptedException {
        final AtomicReference<T> result = new AtomicReference<>();
        final boolean[] done = { false };
        context.execute(() -> {
            result.set(action.get());
            synchronized (done) {
                done[0] = true;
            }
        });
        await(() -> {
            synchronized (done) {
                return done[0];
            }
        });
        return result.get();
    }

    private PPerspective blotter(final PPerspective.Builder builder) throws InterruptedException {
        return onUi(() -> {
            final PPerspective blotter = builder.build();
            blotter.attach(PWindow.getMain());
            return blotter;
        });
    }

    private static PPerspective.Builder quotes() {
        return PPerspective.builder("id", Column.integer("id"), Column.string("symbol"), Column.floating("bid", 4), Column.integer("qty"))
            .interval(Duration.ZERO);
    }

    /** The arguments of the calls to "batch", in order. */
    private List<Object[]> batches() {
        final List<Object[]> batches = new ArrayList<>();
        synchronized (written) {
            for (int i = 0; i < written.size(); i++) {
                if (written.get(i)[0] == ServerToClientModel.PADDON_METHOD && "batch".equals(written.get(i)[1])) {
                    batches.add((Object[]) written.get(i + 1)[1]);
                }
            }
        }
        return batches;
    }

    private List<Object[]> calls(final String method) {
        final List<Object[]> calls = new ArrayList<>();
        synchronized (written) {
            for (int i = 0; i < written.size(); i++) {
                if (written.get(i)[0] == ServerToClientModel.PADDON_METHOD && method.equals(written.get(i)[1])) {
                    final boolean hasArguments = i + 1 < written.size() && written.get(i + 1)[0] == ServerToClientModel.PADDON_ARGUMENTS;
                    calls.add(hasArguments ? (Object[]) written.get(i + 1)[1] : new Object[0]);
                }
            }
        }
        return calls;
    }

    /** Offers on the context's thread: the flush, which waits behind it, sees everything at once. */
    private void offerTogether(final Runnable offers) throws InterruptedException {
        onUi(() -> {
            offers.run();
            return null;
        });
    }

    private void awaitFlushes(final PPerspective blotter, final long flushes) throws InterruptedException {
        await(() -> blotter.getFlushes() >= flushes);
    }

    private static Map<String, Object> row(final Object... keysAndValues) {
        final Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) row.put((String) keysAndValues[i], keysAndValues[i + 1]);
        return row;
    }

    // ---- creation ----

    @Test
    void aViewerIsAnAddOnOfItsClassWhoseCreationArgumentsAreTheSchemaAsATypedArray() throws InterruptedException {
        final PPerspective blotter = blotter(quotes().theme(PPerspective.Theme.DARK).charts(true)
            .config(PerspectiveConfig.builder().plugin("Y Bar").groupBy("symbol").build()));

        synchronized (written) {
            final Object[] factory = written.stream().filter(w -> w[0] == ServerToClientModel.FACTORY).findFirst().orElseThrow();
            assertEquals("com.ponysdk.sample.perspective.PPerspective", factory[1]);
            final Object[] args = (Object[]) written.stream().filter(w -> w[0] == ServerToClientModel.PADDON_CREATION_ARGS).findFirst()
                .orElseThrow()[1];
            assertArrayEquals(new Object[] { "id", "dark", true, "{\"plugin\":\"Y Bar\",\"group_by\":[\"symbol\"]}", 4, "id", "integer", 0,
                "symbol", "string", 0, "bid", "float", 4, "qty", "integer", 0 }, args);
        }
        assertEquals(0, written.stream().filter(w -> w[0] == ServerToClientModel.PADDON_CREATION).count(), "no JSON object form");
        assertEquals(0, blotter.getOffered());
    }

    @Test
    void aViewerWithoutAConfigurationSendsAnEmptyOneAndFollowsThePagesColours() throws InterruptedException {
        blotter(quotes());

        synchronized (written) {
            final Object[] args = (Object[]) written.stream().filter(w -> w[0] == ServerToClientModel.PADDON_CREATION_ARGS).findFirst()
                .orElseThrow()[1];
            assertEquals("auto", args[1]);
            assertEquals(false, args[2]);
            assertEquals("", args[3]);
        }
    }

    @Test
    void theBuilderRefusesATableThatCouldNotBeBuilt() {
        assertThrows(NullPointerException.class, () -> PPerspective.builder(null, Column.integer("id")));
        assertThrows(IllegalArgumentException.class, () -> PPerspective.builder("id"));
        assertThrows(IllegalArgumentException.class, () -> PPerspective.builder("id", Column.integer("id"), Column.string("id")));
        assertThrows(IllegalArgumentException.class, () -> PPerspective.builder("key", Column.integer("id")));
        assertThrows(NullPointerException.class, () -> PPerspective.builder("id", Column.integer("id"), null));
        final Column[] many = new Column[33];
        for (int c = 0; c < many.length; c++) many[c] = Column.integer("c" + c);
        assertThrows(IllegalArgumentException.class, () -> PPerspective.builder("c0", many));
        assertDoesNotThrow(() -> PPerspective.builder("c0", java.util.Arrays.copyOf(many, 32)));
        final PPerspective.Builder builder = PPerspective.builder("id", Column.integer("id"));
        assertThrows(NullPointerException.class, () -> builder.theme(null));
        assertThrows(NullPointerException.class, () -> builder.config(null));
        assertThrows(IllegalArgumentException.class, () -> builder.interval(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> new Column("", PPerspective.Type.STRING, 0));
        assertThrows(IllegalArgumentException.class, () -> Column.floating("x", 10));
        assertThrows(IllegalArgumentException.class, () -> Column.floating("x", -1));
    }

    // ---- the wire ----

    @Test
    void aRowIsItsKeyTheMaskOfItsColumnsThenTheirValuesInTheirSmallestForm() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        assertTrue(onUi(() -> blotter.update(row("id", 7, "bid", 1.08512, "symbol", "EURUSD"))));
        awaitFlushes(blotter, 1);

        // flags 0, no key removed, 1 row: key 7, columns symbol (bit 0) and bid (bit 1), bid as units of 10^-4
        assertEquals(1, batches().size());
        assertArrayEquals(new Object[] { 0, 0, 1, 7, 3, "EURUSD", 10_851L }, batches().get(0));
        assertEquals(1, blotter.getOffered());
        assertEquals(1, blotter.getApplied());
    }

    @Test
    void theChangesOfARowBetweenTwoFlushesMergeAndTheLastValueOfAColumnWins() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        offerTogether(() -> {
            blotter.update(row("id", 1, "bid", 1.0));
            blotter.update(row("id", 2, "bid", 5.0));
            blotter.update(row("id", 1, "qty", 100, "bid", 1.5));
            blotter.update(row("id", 1, "qty", 200));
        });
        awaitFlushes(blotter, 1);

        assertEquals(1, batches().size(), "one batch for the flush");
        // rows in the order their keys first came; row 1 carries bid (bit 1) and qty (bit 2): bid 1.5, qty 200
        assertArrayEquals(new Object[] { 0, 0, 2, 1, 6, 15_000L, 200, 2, 2, 50_000L }, batches().get(0));
        assertEquals(4, blotter.getOffered());
        assertEquals(2, blotter.getApplied(), "four offers, two rows sent");
    }

    @Test
    void aNullSetsTheCellToNullAndStillCostsItsBitInTheMask() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        onUi(() -> blotter.update(row("id", 1, "symbol", null)));
        awaitFlushes(blotter, 1);

        assertArrayEquals(new Object[] { 0, 0, 1, 1, 1, null }, batches().get(0));
    }

    @Test
    void aKeyRemovedGoesBeforeTheRowsAndTakesWhatWasPendingForItAway() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        offerTogether(() -> {
            blotter.update(row("id", 1, "qty", 1));
            blotter.update(row("id", 2, "qty", 2));
            blotter.remove(List.of(1, 3));
        });
        awaitFlushes(blotter, 1);

        // keys 1 and 3 removed (2 of them), then row 2 only
        assertArrayEquals(new Object[] { 0, 2, 1, 3, 1, 2, 4, 2 }, batches().get(0));
    }

    @Test
    void aRowUpdatedAfterItsRemovalComesBackWithTheColumnsOfThatUpdateOnly() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        offerTogether(() -> {
            blotter.remove(List.of(5));
            blotter.update(row("id", 5, "qty", 9));
        });
        awaitFlushes(blotter, 1);

        assertArrayEquals(new Object[] { 0, 1, 5, 1, 5, 4, 9 }, batches().get(0));
    }

    @Test
    void replaceSendsEveryRowWithTheReplaceFlagAndDropsWhatWasOfferedBefore() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        offerTogether(() -> {
            blotter.update(row("id", 1, "qty", 1));
            blotter.remove(List.of(8));
            blotter.replace(List.of(row("id", 2, "qty", 20), row("id", 3, "qty", 30), row("id", 2, "symbol", "A")));
            blotter.update(row("id", 3, "qty", 31));
            blotter.remove(List.of(2));
        });
        awaitFlushes(blotter, 1);

        // the replace, then what came after it: row 2 is gone, row 3 has its last quantity; nothing is removed by key
        assertArrayEquals(new Object[] { 1, 0, 1, 3, 4, 31 }, batches().get(0));
    }

    @Test
    void clearIsAReplaceFlagWithoutRows() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        offerTogether(() -> {
            blotter.update(row("id", 1, "qty", 1));
            blotter.clear();
        });
        awaitFlushes(blotter, 1);

        assertArrayEquals(new Object[] { 1, 0, 0 }, batches().get(0));
    }

    @Test
    void aBigReplaceGoesInSeveralCallsOfBoundedSizeInOneFlush() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) rows.add(row("id", i, "symbol", "S" + i, "bid", i / 100.0, "qty", i));

        onUi(() -> blotter.replace(rows));
        awaitFlushes(blotter, 1);

        final List<Object[]> batches = batches();
        assertTrue(batches.size() > 1, batches.size() + " calls");
        long total = 0;
        for (int b = 0; b < batches.size(); b++) {
            final Object[] batch = batches.get(b);
            assertTrue(batch.length <= 16_384 + 6, "a call of " + batch.length + " values");
            assertEquals(b == 0 ? 1 : 0, batch[0], "the replace flag is on the first call only");
            total += (Integer) batch[2];
        }
        assertEquals(5_000, total);
        assertEquals(1, blotter.getFlushes(), "one flush");
    }

    @Test
    void manyKeysRemovedAlsoGoInCallsOfBoundedSize() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());
        final List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < 40_000; i++) keys.add(i);

        onUi(() -> blotter.remove(keys));
        awaitFlushes(blotter, 1);

        long total = 0;
        for (final Object[] batch : batches()) {
            assertTrue(batch.length <= 16_384 + 6);
            total += (Integer) batch[1];
        }
        assertEquals(40_000, total);
    }

    @Test
    void theValuesOfEachTypeTravelInTheirWireForm() throws InterruptedException {
        final PPerspective blotter = blotter(PPerspective.builder("id", Column.string("id"), Column.bool("open"),
            Column.date("day"), Column.datetime("at"), Column.floating("px", 2), Column.integer("n")).interval(Duration.ZERO));
        final Instant at = Instant.parse("2026-10-30T12:00:00Z");

        final long before = System.currentTimeMillis();
        // each flushed before the next is offered: offered together, the conflator would merge them into fewer flushes
        onUi(() -> blotter.update(row("id", "A", "open", true, "day", LocalDate.of(2026, 10, 30), "at", at, "px", -12.34, "n", 7L)));
        awaitFlushes(blotter, 1);
        onUi(() -> blotter.update(row("id", "B", "at", Date.from(at), "px", 3)));
        awaitFlushes(blotter, 2);
        onUi(() -> blotter.update(row("id", "C", "at", at.toEpochMilli(), "n", Integer.MIN_VALUE)));
        awaitFlushes(blotter, 3);
        final long after = System.currentTimeMillis();

        final List<Object[]> batches = batches();
        // flags, the time the datetimes are relative to, removed, rows
        final Object[] first = batches.get(0);
        final long base = (Long) first[1];
        assertTrue(base >= before && base <= after);
        assertArrayEquals(new Object[] { 0, base, 0, 1, "A", 31, true, (int) LocalDate.of(2026, 10, 30).toEpochDay(), at.toEpochMilli() - base,
            -1_234L, 7 }, first);
        assertArrayEquals(new Object[] { 0, batches.get(1)[1], 0, 1, "B", 12, at.toEpochMilli() - (Long) batches.get(1)[1], 300L },
            batches.get(1));
        assertArrayEquals(new Object[] { 0, batches.get(2)[1], 0, 1, "C", 20, at.toEpochMilli() - (Long) batches.get(2)[1], Integer.MIN_VALUE },
            batches.get(2));
    }

    @Test
    void aDatetimeKeyAndAFloatKeyAreWrittenLikeAnyOtherValue() throws InterruptedException {
        final PPerspective blotter = blotter(PPerspective.builder("at", Column.datetime("at"), Column.integer("n")).interval(Duration.ZERO));

        onUi(() -> blotter.update(row("at", 1_000L, "n", 1)));
        awaitFlushes(blotter, 1);
        onUi(() -> blotter.remove(List.of(1_000L)));
        awaitFlushes(blotter, 2);

        final long base = (Long) batches().get(0)[1];
        assertArrayEquals(new Object[] { 0, base, 0, 1, 1_000L - base, 1, 1 }, batches().get(0));
        assertArrayEquals(new Object[] { 0, batches().get(1)[1], 1, 1_000L - (Long) batches().get(1)[1], 0 }, batches().get(1));
    }

    @Test
    void aFlushWithNothingToSendWritesNothingAndARowRemovedAgainOnlyLeavesItsKey() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        onUi(() -> blotter.remove(List.of()));
        Thread.sleep(100);
        assertEquals(0, batches().size(), "nothing offered, nothing sent");

        offerTogether(() -> {
            blotter.update(row("id", 1, "qty", 1));
            blotter.remove(List.of(1));
        });
        awaitFlushes(blotter, 1);

        assertArrayEquals(new Object[] { 0, 1, 1, 0 }, batches().get(0), "the key to remove, no row");
    }

    // ---- what is refused ----

    @Test
    void aRowThatCouldNotBeSentIsRefusedOnTheCallersThreadAndLeavesTheStateAlone() throws InterruptedException {
        final PPerspective blotter = blotter(PPerspective.builder("id", Column.integer("id"), Column.string("s"), Column.floating("f", 2),
            Column.bool("b"), Column.date("d"), Column.datetime("t"), Column.integer("n")).interval(Duration.ZERO));

        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("s", "no index")));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", null)));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "nope", 1)));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", "x")));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1.5)));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 3_000_000_000L)));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "n", "7")));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "s", 7)));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "f", "1.5")));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "f", Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "f", Double.POSITIVE_INFINITY)));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "f", 1e300)));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "b", "true")));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "d", "2026-10-30")));
        assertThrows(IllegalArgumentException.class, () -> blotter.update(row("id", 1, "t", "now")));
        assertThrows(NullPointerException.class, () -> blotter.remove(java.util.Collections.singletonList(null)));
        assertThrows(IllegalArgumentException.class, () -> blotter.remove(List.of("x")));
        assertThrows(IllegalArgumentException.class, () -> blotter.replace(List.of(row("id", 1), row("n", 2))));
        assertThrows(IllegalArgumentException.class, () -> blotter.replace(List.of(row("id", 1, "nope", 2))));

        assertEquals(0, blotter.getOffered(), "nothing counted");
        Thread.sleep(50);
        assertEquals(0, batches().size(), "nothing sent");
    }

    // ---- what the user does ----

    private static JsonObject fromTerminal(final String json) {
        return Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), Json.createReader(new java.io.StringReader(json)).readObject())
            .build();
    }

    @Test
    void theConfigurationAClickAndASelectionTheTerminalReportsReachTheirListeners() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());
        final List<PerspectiveConfig> configs = new ArrayList<>();
        final List<JsonObject> clicks = new ArrayList<>();
        final List<JsonObject> selections = new ArrayList<>();
        blotter.addConfigListener(configs::add);
        final var clickRegistration = blotter.addClickListener(clicks::add);
        blotter.addSelectListener(selections::add);

        onUi(() -> {
            blotter.onClientData(fromTerminal("{\"type\":\"config\",\"config\":{\"plugin\":\"Y Bar\",\"group_by\":[\"symbol\"],\"split_by\":[],"
                + "\"columns\":[\"bid\",null],\"sort\":[[\"bid\",\"desc\"]],\"filter\":[[\"qty\",\">\",5],[\"symbol\",\"in\",[\"A\",\"B\"]]],"
                + "\"aggregates\":{\"bid\":\"avg\",\"qty\":{\"weighted_mean\":[\"bid\"]}}}}"));
            blotter.onClientData(fromTerminal("{\"type\":\"click\",\"row\":{\"symbol\":\"A\"}}"));
            blotter.onClientData(fromTerminal("{\"type\":\"select\",\"selected\":true,\"row\":{\"symbol\":\"B\"}}"));
            blotter.onClientData(fromTerminal("{\"type\":\"select\",\"selected\":false,\"row\":null}"));
            blotter.onClientData(fromTerminal("{\"type\":\"select\",\"selected\":true,\"row\":null}"));
            blotter.onClientData(fromTerminal("{\"type\":\"ready\"}"));
            clickRegistration.removeHandler();
            blotter.onClientData(fromTerminal("{\"type\":\"click\",\"row\":{\"symbol\":\"C\"}}"));
            return null;
        });

        assertEquals(1, configs.size());
        final PerspectiveConfig config = configs.get(0);
        assertEquals("Y Bar", config.plugin());
        assertEquals(List.of("symbol"), config.groupBy());
        assertEquals(List.of(), config.splitBy());
        assertEquals(java.util.Arrays.asList("bid", null), config.columns());
        assertEquals(List.of(new PerspectiveConfig.Sort("bid", "desc")), config.sort());
        assertEquals(List.of(new PerspectiveConfig.Filter("qty", ">", 5L), new PerspectiveConfig.Filter("symbol", "in", List.of("A", "B"))),
            config.filter());
        assertEquals(Map.of("bid", "avg"), config.aggregates(), "an aggregate that is not a name is left out");
        assertEquals(List.of("{\"symbol\":\"A\"}"), clicks.stream().map(JsonObject::toString).toList());
        assertEquals(java.util.Arrays.asList("{\"symbol\":\"B\"}", null, null),
            selections.stream().map(s -> s == null ? null : s.toString()).toList(), "deselected, or selected without a row: null");
    }

    @Test
    void theConfigurationIsSentAsJsonWithItsSetFieldsOnly() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());
        final PerspectiveConfig pivot = PerspectiveConfig.builder().plugin("Datagrid").groupBy("symbol").splitBy("qty").columns("bid")
            .sort("bid", "asc").filter("symbol", "==", "A").filter("qty", "<", 2.5).filter("qty", "in", List.of(1L, 2L)).filter("symbol", "!=", null)
            .filter("symbol", "==", true).aggregate("bid", "sum")
            .pluginConfig(Json.createObjectBuilder().add("edit_mode", "SELECT_ROW").build()).build();

        onUi(() -> {
            blotter.configure(pivot);
            blotter.configure(PerspectiveConfig.builder().plugin("Y Line").build());
            blotter.reset();
            return null;
        });

        final List<Object[]> configure = calls("configure");
        assertEquals("{\"plugin\":\"Datagrid\",\"group_by\":[\"symbol\"],\"split_by\":[\"qty\"],\"columns\":[\"bid\"],\"sort\":[[\"bid\",\"asc\"]],"
            + "\"filter\":[[\"symbol\",\"==\",\"A\"],[\"qty\",\"<\",2.5],[\"qty\",\"in\",[1,2]],[\"symbol\",\"!=\",null],[\"symbol\",\"==\",true]],"
            + "\"aggregates\":{\"bid\":\"sum\"},\"plugin_config\":{\"edit_mode\":\"SELECT_ROW\"}}", configure.get(0)[0]);
        assertEquals("{\"plugin\":\"Y Line\"}", configure.get(1)[0]);
        assertEquals(1, calls("reset").size());
        assertEquals(pivot, PerspectiveConfig.fromJson(pivot.toJson()), "what is written is what is read");
        assertNull(PerspectiveConfig.fromJson(Json.createObjectBuilder().build()).plugin());
    }

    @Test
    void aFilterValueThatIsNotJsonIsRefused() {
        final PerspectiveConfig config = PerspectiveConfig.builder().filter("a", "==", new Object()).build();
        assertThrows(IllegalArgumentException.class, config::toJson);
        assertThrows(NullPointerException.class, () -> PerspectiveConfig.builder().plugin(null));
        assertThrows(NullPointerException.class, () -> PerspectiveConfig.builder().pluginConfig(null));
    }

    // ---- lifecycle ----

    @Test
    void aDestroyedViewerDropsWhatIsOffered() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());
        onUi(() -> blotter.update(row("id", 1, "qty", 1)));
        awaitFlushes(blotter, 1);

        onUi(() -> {
            blotter.destroy();
            return null;
        });

        assertFalse(blotter.update(row("id", 2, "qty", 2)));
        assertFalse(blotter.remove(List.of(1)));
        assertFalse(blotter.replace(List.of()));
        assertFalse(blotter.clear());
        assertEquals(1, batches().size());
    }

    @Test
    void theContextsDestroyClosesTheFlush() throws InterruptedException {
        final PPerspective blotter = blotter(quotes());

        onUi(() -> {
            blotter.onDestroy();
            return null;
        });

        assertFalse(blotter.update(row("id", 1, "qty", 1)));
    }

    @Test
    void aSlowIntervalSendsOneBatchForAFeedFasterThanIt() throws InterruptedException {
        final PPerspective blotter = blotter(quotes().interval(Duration.ofMillis(200)));

        final Map<String, Object> last = new HashMap<>();
        for (int i = 1; i <= 2_000; i++) {
            last.put("id", i % 10);
            last.put("qty", i);
            blotter.update(last);
        }
        await(() -> blotter.getApplied() >= 1);
        Thread.sleep(500);

        assertEquals(2_000, blotter.getOffered());
        assertTrue(blotter.getFlushes() <= 4, blotter.getFlushes() + " flushes");
        assertTrue(blotter.getApplied() <= 40, blotter.getApplied() + " rows sent");
        // applied in order, each key ends at the last quantity offered for it: the batches' rows are key, mask (qty: 4), qty
        final Map<Object, Object> quantities = new HashMap<>();
        for (final Object[] batch : batches()) {
            for (int rows = (Integer) batch[2], at = 3; rows > 0; rows--, at += 3) quantities.put(batch[at], batch[at + 2]);
        }
        for (int id = 0; id < 10; id++) assertEquals(id == 0 ? 2_000 : 1_990 + id, quantities.get(id), "key " + id);
    }

    private static void await(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < deadline, "not reached in 5 s");
            Thread.sleep(5);
        }
    }
}
