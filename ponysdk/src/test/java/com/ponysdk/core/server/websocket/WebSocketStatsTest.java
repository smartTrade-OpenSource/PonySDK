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
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ponysdk.core.server.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;
import com.ponysdk.core.server.websocket.WebSocketStats.Bandwidth;
import com.ponysdk.core.server.websocket.WebSocketStats.DecodeException;
import com.ponysdk.core.server.websocket.WebSocketStats.Record;
import com.ponysdk.core.util.Pair;

/**
 * Non-regression tests for {@link WebSocketStats} — the protocol bandwidth statistics model:
 * summary aggregation, the per-model / per-value-type / per-group bandwidth views, the binary
 * encode/decode round-trip (grouped, the symmetric format), CSV export, and the {@link Bandwidth}
 * / {@link Record} value classes.
 */
public class WebSocketStatsTest {

    // Two models with distinct, non-null value types (avoids null keys in the EnumMap views).
    private static final ServerToClientModel M_INT = ServerToClientModel.HEARTBEAT_PERIOD; // INTEGER
    private static final ServerToClientModel M_STR = ServerToClientModel.TYPE_HISTORY; // STRING

    private static Record rec(final int meta, final int data, final int count) {
        return new Record(meta, data) {
            @Override
            public int getCount() {
                return count;
            }
        };
    }

    private static WebSocketStats ungrouped() {
        final Map<Object, Record> ints = new HashMap<>();
        ints.put(100, rec(1, 4, 3)); // meta 3, data 12
        ints.put(200, rec(2, 6, 1)); // meta 2, data 6
        final Map<Object, Record> strs = new HashMap<>();
        strs.put("hello", rec(1, 10, 2)); // meta 2, data 20
        final Map<ServerToClientModel, Map<Object, Record>> stats = new EnumMap<>(ServerToClientModel.class);
        stats.put(M_INT, ints);
        stats.put(M_STR, strs);
        return new WebSocketStats(stats, LocalDateTime.now().minusMinutes(1), LocalDateTime.now(), false);
    }

    @Test
    public void testSummaryBandwidthAndCount() {
        final WebSocketStats ws = ungrouped();
        assertFalse(ws.isGrouped());
        assertEquals(6, ws.getSummaryCount()); // 3 + 1 + 2
        final Bandwidth summary = ws.getSummaryBandwidth();
        assertEquals(7, summary.getMeta()); // 1*3 + 2*1 + 1*2
        assertEquals(38, summary.getData()); // 4*3 + 6*1 + 10*2
        assertEquals(45, summary.getTotal());
        assertNotNull(ws.getStats());
    }

    @Test
    public void testGetBandwidthPerServerToClientModel() {
        final Map<ServerToClientModel, Bandwidth> perModel = ungrouped().getBandwidthPerServerToClientModel();
        assertEquals(5, perModel.get(M_INT).getMeta()); // 1*3 + 2*1
        assertEquals(18, perModel.get(M_INT).getData()); // 4*3 + 6*1
        assertEquals(2, perModel.get(M_STR).getMeta());
        assertEquals(20, perModel.get(M_STR).getData());
    }

    @Test
    public void testGetBandwidthPerValueType() {
        final Map<ValueTypeModel, Bandwidth> perType = ungrouped().getBandwidthPerValueType();
        assertEquals(18, perType.get(ValueTypeModel.INTEGER).getData());
        assertEquals(20, perType.get(ValueTypeModel.STRING).getData());
    }

    @Test
    public void testGetBandwidthFor_appliesValueAndModelFilters() {
        final Bandwidth only100 = ungrouped().getBandwidthFor(v -> Integer.valueOf(100).equals(v), null);
        assertEquals(3, only100.getMeta());
        assertEquals(12, only100.getData());

        final Bandwidth onlyStrModel = ungrouped().getBandwidthFor(null, m -> m == M_STR);
        assertEquals(20, onlyStrModel.getData());
    }

    @Test
    public void testGetBandwidthForPattern() {
        final Bandwidth hello = ungrouped().getBandwidthForPattern(Pattern.compile("hell"), null);
        assertEquals(20, hello.getData());
        assertEquals(2, hello.getMeta());
    }

    @Test
    public void testToCsv_writesHeaderAndOneLinePerRecord() throws Exception {
        final File file = File.createTempFile("wsstats", ".csv");
        file.deleteOnExit();
        ungrouped().toCsv(file, ';', null, null);

        final List<String> lines = Files.readAllLines(file.toPath());
        assertTrue("header must be present", lines.get(0).startsWith("MODEL;"));
        assertTrue(lines.get(0).contains("BANDWIDTH"));
        // header + 3 records (the toCsv writer starts each record with '\n')
        final long dataLines = lines.stream().filter(l -> l.contains(";")).count();
        assertEquals(4, dataLines); // header + 3
    }

    @Test
    public void testToString_containsSummary() {
        assertTrue(ungrouped().toString().contains("Web socket stats recording"));
    }

    // ---- Grouped encode/decode round-trip (the symmetric on-disk format) ----

    @Test
    public void testEncodeDecodeRoundTrip_groupedCoversAllValueTypes() throws Exception {
        final Map<Object, Record> records = new HashMap<>();
        records.put(new Pair<Object, String>(null, "g"), rec(1, 1, 1));
        records.put(new Pair<Object, String>(Boolean.TRUE, "g"), rec(1, 2, 2));
        records.put(new Pair<Object, String>((byte) 7, "g"), rec(1, 3, 3));
        records.put(new Pair<Object, String>((short) 9, "g"), rec(1, 4, 4));
        records.put(new Pair<Object, String>(123, "g"), rec(1, 5, 5));
        records.put(new Pair<Object, String>(1.5f, "g"), rec(1, 6, 6));
        records.put(new Pair<Object, String>(2.5d, "g"), rec(1, 7, 7));
        records.put(new Pair<Object, String>("text", "h"), rec(1, 8, 8));
        final Map<ServerToClientModel, Map<Object, Record>> stats = new EnumMap<>(ServerToClientModel.class);
        stats.put(M_INT, records);

        final WebSocketStats original = new WebSocketStats(stats, LocalDateTime.now().minusMinutes(2),
            LocalDateTime.now(), true);

        final File file = File.createTempFile("wsstats", ".bin");
        file.deleteOnExit();
        original.encode(file);
        final WebSocketStats decoded = WebSocketStats.decode(file);

        assertTrue(decoded.isGrouped());
        final Map<Object, ? extends Record> decodedRecords = decoded.getStats().get(M_INT);
        assertEquals(records.size(), decodedRecords.size());
        // Every original (value, group) pair must survive the round-trip with its byte counts.
        for (final Map.Entry<Object, Record> e : records.entrySet()) {
            final Record d = decodedRecords.get(e.getKey());
            assertNotNull("missing key after round-trip: " + e.getKey(), d);
            assertEquals(e.getValue().getCount(), d.getCount());
            assertEquals(e.getValue().getDataBytes(), d.getDataBytes());
        }
    }

    @Test
    public void testGroupedBandwidthViews() {
        final Map<Object, Record> records = new HashMap<>();
        records.put(new Pair<Object, String>(1, "alpha"), rec(2, 3, 4));
        records.put(new Pair<Object, String>(2, "beta"), rec(1, 1, 1));
        final Map<ServerToClientModel, Map<Object, Record>> stats = new EnumMap<>(ServerToClientModel.class);
        stats.put(M_INT, records);
        final WebSocketStats ws = new WebSocketStats(stats, LocalDateTime.now(), LocalDateTime.now(), true);

        assertNotNull(ws.getBandwidthPerGroup());
        assertEquals(12, ws.getBandwidthPerGroup().get("alpha").getData()); // 3*4
        assertNotNull(ws.getBandwidthPerValueTypePerGroup());
        assertNotNull(ws.getBandwidthPerServerToClientModelPerGroup());
    }

    @Test
    public void testGroupedViewsReturnNullWhenUngrouped() {
        final WebSocketStats ws = ungrouped();
        assertNull(ws.getBandwidthPerGroup());
        assertNull(ws.getBandwidthPerValueTypePerGroup());
        assertNull(ws.getBandwidthPerServerToClientModelPerGroup());
    }

    @Test
    public void testDecode_truncatedFileThrowsDecodeException() throws Exception {
        final File empty = File.createTempFile("wsstats-bad", ".bin");
        empty.deleteOnExit();
        assertThrows(DecodeException.class, () -> WebSocketStats.decode(empty));
    }

    // ---- Bandwidth / Record value classes ----

    @Test
    public void testBandwidth_arithmeticRatiosCompareAndToString() {
        final Bandwidth b = new Bandwidth(10, 30);
        assertEquals(40, b.getTotal());
        assertEquals(0.25f, b.getMetaRatio(), 0.0001f);
        assertEquals(0.75f, b.getDataRatio(), 0.0001f);
        b.add(10, 10);
        assertEquals(60, b.getTotal());
        assertTrue(new Bandwidth(100, 0).compareTo(new Bandwidth(10, 0)) > 0);
        assertTrue(new Bandwidth(1, 0).compareTo(new Bandwidth(100, 0)) < 0);
        assertTrue(b.toString().contains("meta="));
    }

    @Test
    public void testRecord_compareToByTotalBandwidth() {
        assertTrue(rec(10, 10, 5).compareTo(rec(1, 1, 1)) > 0);
        assertTrue(rec(1, 1, 1).compareTo(rec(10, 10, 5)) < 0);
    }
}
