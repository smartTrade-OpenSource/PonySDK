/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.chart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/** PChart (docs/decisions/0026-chart.md): what it sends to the terminal, and what it refuses. */
public class PChartTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private PChart chart(final PChart.Builder builder) {
        final PChart chart = builder.build();
        chart.attach(PWindow.getMain());
        writer.clear();
        return chart;
    }

    private Object[] lastPoint() {
        final List<Object> arguments = writer.valuesOf(ServerToClientModel.PADDON_ARGUMENTS);
        return (Object[]) arguments.get(arguments.size() - 1);
    }

    // ---- creation ----

    @Test
    public void aChartIsAnAddOnOfItsClassWithItsSettingsAsATypedArray() {
        final PChart chart = PChart.builder(PChart.Type.LINE, "EURUSD", "GBPUSD").title("Prices").decimals(5)
            .window(Duration.ofSeconds(60)).maxPoints(1000).dataTable(10).build();
        chart.attach(PWindow.getMain());

        assertEquals(List.of(WidgetType.ADDON_COMPOSITE.getValue()), writer.valuesOf(ServerToClientModel.WIDGET_TYPE));
        assertEquals(List.of("com.ponysdk.core.ui.chart.PChart"), writer.valuesOf(ServerToClientModel.FACTORY));
        assertArrayEquals(new Object[] { "line", 5, 60_000L, 1000, "Prices", 10, "EURUSD", "GBPUSD" },
            (Object[]) writer.valuesOf(ServerToClientModel.PADDON_CREATION_ARGS).get(0));
        assertEquals(0, writer.count(ServerToClientModel.PADDON_CREATION), "no JSON");
        assertNotNull(chart.asWidget());
    }

    @Test
    public void aChartKeepsTheLast3600PointsByDefaultAndHasNoTable() {
        final PChart chart = PChart.builder(PChart.Type.BAR, "Volume").build();
        chart.attach(PWindow.getMain());

        assertArrayEquals(new Object[] { "bar", 2, 0L, 3600, "Chart", 0, "Volume" },
            (Object[]) writer.valuesOf(ServerToClientModel.PADDON_CREATION_ARGS).get(0));
    }

    @Test
    public void theSeriesAreCopiedWhenTheBuilderTakesThem() {
        final String[] names = { "A", "B" };
        final PChart.Builder builder = PChart.builder(PChart.Type.LINE, names);
        names[0] = "changed";
        builder.build().attach(PWindow.getMain());

        assertArrayEquals(new Object[] { "line", 2, 0L, 3600, "Chart", 0, "A", "B" },
            (Object[]) writer.valuesOf(ServerToClientModel.PADDON_CREATION_ARGS).get(0));
    }

    @Test
    public void theBuilderRefusesWhatTheTerminalCouldNotDraw() {
        assertThrows(NullPointerException.class, () -> PChart.builder(null, "A"));
        assertThrows(IllegalArgumentException.class, () -> PChart.builder(PChart.Type.LINE));
        assertThrows(NullPointerException.class, () -> PChart.builder(PChart.Type.LINE, "A", null));
        final PChart.Builder builder = PChart.builder(PChart.Type.LINE, "A");
        assertThrows(NullPointerException.class, () -> builder.title(null));
        assertThrows(IllegalArgumentException.class, () -> builder.decimals(-1));
        assertThrows(IllegalArgumentException.class, () -> builder.decimals(10));
        assertThrows(IllegalArgumentException.class, () -> builder.window(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> builder.window(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> builder.maxPoints(1));
        assertThrows(IllegalArgumentException.class, () -> builder.maxPoints(100_001));
        assertThrows(IllegalArgumentException.class, () -> builder.interval(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> builder.dataTable(-1));
        assertThrows(IllegalArgumentException.class, () -> builder.dataTable(101));
        builder.decimals(0).decimals(9).maxPoints(2).maxPoints(100_000).interval(Duration.ZERO).dataTable(0).dataTable(100);
    }

    // ---- points ----

    @Test
    public void aPointIsTheTimeSinceThePreviousOneThenEachSeriesChangeInUnits() {
        final PChart chart = chart(PChart.builder(PChart.Type.LINE, "EURUSD", "GBPUSD").decimals(4));

        chart.append(1_700_000_000_000L, 1.0851, 1.2702);
        assertEquals(List.of("append"), writer.valuesOf(ServerToClientModel.PADDON_METHOD));
        assertArrayEquals(new Object[] { 1_700_000_000_000L, 10_851L, 12_702L }, lastPoint());

        chart.append(1_700_000_000_100L, 1.0853, 1.2702);
        assertArrayEquals(new Object[] { 100L, 2L, 0L }, lastPoint());

        chart.append(1_700_000_000_100L, 1.0850, 1.2710);
        assertArrayEquals(new Object[] { 0L, -3L, 8L }, lastPoint(), "the same time, a change down");
        assertEquals(3, chart.getRows());
    }

    @Test
    public void aNaNLeavesItsSeriesWhereItWasAndNothingIsSentForIt() {
        final PChart chart = chart(PChart.builder(PChart.Type.LINE, "A", "B").decimals(2));

        chart.append(1_000, 1.5, Double.NaN);
        assertArrayEquals(new Object[] { 1_000L, 150L, null }, lastPoint());
        chart.append(1_100, Double.NaN, 2.0);
        assertArrayEquals(new Object[] { 100L, null, 200L }, lastPoint());
        chart.append(1_200, 1.6, Double.NaN);
        assertArrayEquals(new Object[] { 100L, 10L, null }, lastPoint(), "A changes from 1.5, B is unchanged");
    }

    @Test
    public void aPointRefusedLeavesTheNextOneAChangeFromTheLastAccepted() {
        final PChart chart = chart(PChart.builder(PChart.Type.LINE, "A", "B").decimals(2));
        chart.append(1_000, 1.0, 2.0);

        assertThrows(IllegalArgumentException.class, () -> chart.append(1_100, 1.0), "one value for two series");
        assertThrows(IllegalArgumentException.class, () -> chart.append(1_100, 1.0, 2.0, 3.0));
        assertThrows(IllegalArgumentException.class, () -> chart.append(999, 1.0, 2.0), "before the previous point");
        assertThrows(IllegalArgumentException.class, () -> chart.append(1_100, 5.0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> chart.append(1_100, 5.0, 1e300), "beyond what a double holds exactly");

        writer.clear();
        chart.append(1_100, 1.5, 2.0);
        assertArrayEquals(new Object[] { 100L, 50L, 0L }, lastPoint());
        assertEquals(2, chart.getRows());
    }

    @Test
    public void aValueIsRoundedToTheDecimalsOfTheChart() {
        final PChart chart = chart(PChart.builder(PChart.Type.LINE, "A").decimals(2));
        chart.append(1, 1.006);
        assertArrayEquals(new Object[] { 1L, 101L }, lastPoint());
        chart.append(2, -3.14159);
        assertArrayEquals(new Object[] { 1L, -314L - 101L }, lastPoint());
    }

    @Test
    public void clearStartsTheChartAgainFromZero() {
        final PChart chart = chart(PChart.builder(PChart.Type.LINE, "A").decimals(1));
        chart.append(5_000, 10.0);
        writer.clear();

        chart.clear();
        assertEquals(List.of("clear"), writer.valuesOf(ServerToClientModel.PADDON_METHOD));
        assertEquals(0, writer.count(ServerToClientModel.PADDON_ARGUMENTS));

        chart.append(1_000, 11.0); // before the cleared point's time: allowed again
        assertArrayEquals(new Object[] { 1_000L, 110L }, lastPoint());
    }

    @Test
    public void aPointCallMadeBeforeTheChartIsCreatedIsSentAfterItsCreation() {
        final PChart chart = PChart.builder(PChart.Type.LINE, "A").decimals(0).build();
        chart.append(1_000, 7.0);
        assertEquals(0, writer.count(ServerToClientModel.PADDON_METHOD));

        chart.attach(PWindow.getMain());

        final List<ServerToClientModel> models = writer.models();
        assertTrue(models.indexOf(ServerToClientModel.TYPE_CREATE) < models.indexOf(ServerToClientModel.PADDON_METHOD));
        assertArrayEquals(new Object[] { 1_000L, 7L }, lastPoint());
    }

    @Test
    public void aDestroyedChartDropsTheValuesOfTheFeed() {
        final PChart chart = chart(PChart.builder(PChart.Type.LINE, "A", "B"));
        assertThrows(IllegalArgumentException.class, () -> chart.offer(0, Double.NaN));
        assertThrows(IndexOutOfBoundsException.class, () -> chart.offer(2, 1.0));

        chart.onDestroy();
        assertFalse(chart.offer(0, 1.0));

        final PChart another = chart(PChart.builder(PChart.Type.LINE, "A"));
        another.destroy();
        assertFalse(another.offer(0, 1.0));
        assertEquals(1, writer.count(ServerToClientModel.DESTROY));
    }
}
