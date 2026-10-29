/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.StringDictionary;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;

/**
 * Writes the frames the TypeScript terminal is tested on (docs/decisions/0002-javascript-terminal.md, P1), produced by
 * the real server write path: {@code <name>.bin} holds the bytes, {@code <name>.json} where each frame ends and, for
 * the value fixtures, the models the terminal must read back.
 * <ul>
 * <li>{@code values-plain}, {@code values-dictionary}: every value type and string encoding, one model per block,
 * without then with the string dictionary.</li>
 * <li>{@code page-v1}, {@code page-v2}, {@code page-v3}: the same page built, updated and partly removed by a
 * UIContext, in each protocol version. The terminal must apply the same operations whatever the version.</li>
 * <li>{@code dates.json}: dates formatted by {@link SimpleDateFormat} in English, and texts it parses, which the
 * terminal's date patterns must format and parse alike (ADR-0002, R3); and in other languages, whose names the
 * terminal takes from the browser and the server reads back with the JDK's (ADR-0022).</li>
 * </ul>
 * Argument: the output directory.
 */
public final class TerminalFixtures {

    private TerminalFixtures() {
    }

    public static void main(final String[] args) throws IOException {
        final Path dir = Path.of(args[0]);
        Files.createDirectories(dir);
        values(dir, "values-plain", null);
        final StringDictionary dictionary = new StringDictionary(70_000, 4, 70_000);
        for (int i = 0; i < 40_000; i++) dictionary.internOrGet("filler-" + i); // ids of the next entries above 32767
        values(dir, "values-dictionary", dictionary);
        for (final int version : new int[] {ProtocolVersion.V1, ProtocolVersion.UPDATE_RUNS, ProtocolVersion.CREATE_IN_PARENT}) {
            page(dir, "page-v" + version, version);
        }
        dates(dir);
    }

    // ---- dates ---------------------------------------------------------------------------------------------------

    private static final String[] PATTERNS = {"dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd/MM/yyyy HH:mm:ss", "d MMM yyyy",
        "EEEE d MMMM yyyy", "EEE, MMM d, yy", "h:mm a", "K:mm a", "k:mm", "yyyyMMdd", "dd.MM.yy HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss.SSS", "'Week of' d MMM yyyy"};

    /** Year, month (1-12), day, hours, minutes, seconds, milliseconds: away from daylight saving changes. */
    private static final int[][] DATES = {{2026, 3, 5, 14, 7, 9, 45}, {2026, 1, 1, 0, 0, 0, 0}, {2025, 12, 31, 23, 59, 59, 999},
        {2024, 2, 29, 12, 0, 0, 0}, {1999, 7, 14, 9, 5, 1, 7}, {2045, 10, 20, 13, 30, 0, 500}, {1976, 11, 9, 0, 30, 0, 0}};

    /** Texts a user may type, as the server parses them: the terminal must read the same date. */
    private static final String[][] TYPED = {{"dd/MM/yyyy", "5/3/2026"}, {"dd/MM/yyyy", "32/01/2026"},
        {"MM/dd/yyyy", "13/01/2026"}, {"d MMM yyyy", "5 march 2026"}, {"d MMM yyyy", "5 Mar 2026"}, {"dd/MM/yy", "05/03/26"},
        {"dd/MM/yy", "05/03/70"}, {"dd/MM/yyyy HH:mm", "05/03/2026 7:05"}, {"h:mm a", "12:30 AM"}, {"h:mm a", "1:30 pm"}};

    /** The languages of the showcase, and patterns with names; digits as the terminal writes them. */
    private static final String[] LOCALES = {"fr", "ar"};
    private static final String[] LOCALIZED_PATTERNS = {"EEEE d MMMM yyyy", "EEE d MMM yyyy", "MMMM yyyy", "h:mm a"};

    private static void dates(final Path dir) throws IOException {
        final JsonArrayBuilder formatted = Json.createArrayBuilder();
        for (final String pattern : PATTERNS) {
            final SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            for (final int[] d : DATES) {
                final Calendar calendar = Calendar.getInstance();
                calendar.clear();
                calendar.set(d[0], d[1] - 1, d[2], d[3], d[4], d[5]);
                calendar.set(Calendar.MILLISECOND, d[6]);
                formatted.add(Json.createObjectBuilder().add("pattern", pattern).add("date", fields(d))
                    .add("text", format.format(calendar.getTime())));
            }
        }
        final JsonArrayBuilder typed = Json.createArrayBuilder();
        for (final String[] t : TYPED) {
            final Date date;
            try {
                date = new SimpleDateFormat(t[0], Locale.ENGLISH).parse(t[1]);
            } catch (final ParseException e) {
                throw new IllegalStateException(t[0] + " " + t[1], e);
            }
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            final int[] d = {calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND)};
            typed.add(Json.createObjectBuilder().add("pattern", t[0]).add("text", t[1]).add("date", fields(d)));
        }
        final JsonArrayBuilder localized = Json.createArrayBuilder();
        for (final String tag : LOCALES) {
            final Locale locale = new Locale.Builder().setLanguageTag(tag).setUnicodeLocaleKeyword("nu", "latn").build();
            for (final String pattern : LOCALIZED_PATTERNS) {
                final SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
                for (final int[] d : DATES) {
                    final Calendar calendar = Calendar.getInstance();
                    calendar.clear();
                    calendar.set(d[0], d[1] - 1, d[2], d[3], d[4], d[5]);
                    localized.add(Json.createObjectBuilder().add("locale", tag).add("pattern", pattern).add("date", fields(d))
                        .add("text", format.format(calendar.getTime())));
                }
            }
        }
        Files.writeString(dir.resolve("dates.json"), Json.createObjectBuilder().add("formatted", formatted).add("typed", typed)
            .add("localized", localized).build().toString());
    }

    private static JsonArrayBuilder fields(final int[] date) {
        final JsonArrayBuilder array = Json.createArrayBuilder();
        for (final int field : date) array.add(field);
        return array;
    }

    // ---- values --------------------------------------------------------------------------------------------------

    /** One model per block, written twice: with a dictionary, the second round refers to what the first added. */
    private static void values(final Path dir, final String name, final StringDictionary dictionary) throws IOException {
        final List<Object[]> models = new ArrayList<>();
        final ServerToClientModel nullKey = first(ValueTypeModel.NULL);
        final ServerToClientModel booleanKey = first(ValueTypeModel.BOOLEAN);
        final ServerToClientModel byteKey = first(ValueTypeModel.BYTE);
        final ServerToClientModel intKey = first(ValueTypeModel.INTEGER);
        final ServerToClientModel uint31Key = first(ValueTypeModel.UINT31);
        final ServerToClientModel longKey = first(ValueTypeModel.LONG);
        final ServerToClientModel doubleKey = first(ValueTypeModel.DOUBLE);
        final ServerToClientModel stringKey = ServerToClientModel.TEXT;
        final ServerToClientModel arrayKey = first(ValueTypeModel.ARRAY);

        models.add(new Object[] {nullKey, null});
        for (final boolean b : new boolean[] {true, false}) models.add(new Object[] {booleanKey, b});
        for (final byte b : new byte[] {Byte.MIN_VALUE, -1, 0, Byte.MAX_VALUE}) models.add(new Object[] {byteKey, b});
        for (final int i : new int[] {Integer.MIN_VALUE, -1, 0, 32_768, Integer.MAX_VALUE}) models.add(new Object[] {intKey, i});
        for (final int i : new int[] {0, 1, Short.MAX_VALUE, Short.MAX_VALUE + 1, 1 << 24, Integer.MAX_VALUE})
            models.add(new Object[] {uint31Key, i});
        for (final long l : new long[] {0, -5, 1_700_000_000_000L, (1L << 53) - 1, -(1L << 53) + 1, 1L << 32})
            models.add(new Object[] {longKey, l});
        for (final double d : new double[] {0, 0.1, -1.5e300, Double.MIN_VALUE, 1 / 3.0}) models.add(new Object[] {doubleKey, d});

        for (final String round : new String[] {"first", "second"}) {
            models.add(new Object[] {stringKey, null});
            models.add(new Object[] {stringKey, ""});
            models.add(new Object[] {stringKey, "a"});
            models.add(new Object[] {stringKey, "abc"});
            models.add(new Object[] {stringKey, "sixteen-chars-xx"});
            models.add(new Object[] {stringKey, "seventeen-chars-x"});
            for (final int length : new int[] {247, 248, 249, 250, 251, 255, 256, 300, 70_000})
                models.add(new Object[] {stringKey, "x".repeat(length - 1) + length % 10});
            models.add(new Object[] {stringKey, "été € café 🐴"});
            models.add(new Object[] {stringKey, "é".repeat(200)});
            models.add(new Object[] {stringKey, "é".repeat(40_000)});
            models.add(new Object[] {stringKey, "{\"not\":\"parsed\"}"});
            models.add(new Object[] {stringKey, round + "-round"});
        }

        for (final String round : new String[] {"first", "second"}) {
            models.add(new Object[] {arrayKey, new Object[0]});
            models.add(new Object[] {arrayKey, new Object[] {null, true, false, (byte) -3, (short) 300, (short) 5, 70_000, -5, -5L,
                1L << 40, 0.5, 0.1, 1.25f, "ascii", "", "été", "{\"x\":[1,2]}", "[1,2]", "{bad", round + "-round",
                "y".repeat(300), "z".repeat(70_000)}});
            models.add(new Object[] {arrayKey, range(254)});
            models.add(new Object[] {arrayKey, range(255)});
            models.add(new Object[] {arrayKey, range(1_000)});
        }

        final List<Integer> frames = new ArrayList<>();
        final byte[] bytes = encode(dictionary, frames, pusher -> {
            for (final Object[] model : models) {
                pusher.encode((ServerToClientModel) model[0], model[1]);
                pusher.encode(ServerToClientModel.END, null);
                if (model[0] == nullKey || model[0] == arrayKey) pusher.flush(); // several frames
            }
        });

        final JsonArrayBuilder expected = Json.createArrayBuilder();
        for (final Object[] model : models) {
            expected.add(Json.createObjectBuilder().add("key", ((ServerToClientModel) model[0]).name()).add("value", expected(model[1])));
            expected.add(Json.createObjectBuilder().add("key", ServerToClientModel.END.name()).add("value", JsonValue.NULL));
        }
        write(dir, name, bytes, frames, Json.createObjectBuilder().add("expected", expected));
    }

    private static ServerToClientModel first(final ValueTypeModel type) {
        return Arrays.stream(ServerToClientModel.values()).filter(m -> m.getTypeModel() == type).findFirst().orElseThrow();
    }

    private static Object[] range(final int size) {
        final Object[] array = new Object[size];
        for (int i = 0; i < size; i++) array[i] = i;
        return array;
    }

    /** What the terminal reads for a written value. */
    private static JsonValue expected(final Object value) {
        if (value == null) return JsonValue.NULL;
        if (value instanceof Boolean b) return b ? JsonValue.TRUE : JsonValue.FALSE;
        if (value instanceof Float f) return Json.createValue((double) f);
        if (value instanceof Double d) return Json.createValue(d);
        if (value instanceof Number n) return Json.createValue(n.longValue());
        if (value instanceof String s) return s.isEmpty() ? JsonValue.NULL : Json.createValue(s); // "" and null alike
        final JsonArrayBuilder array = Json.createArrayBuilder();
        for (final Object element : (Object[]) value) array.add(expectedElement(element));
        return array.build();
    }

    /** An array element: an empty string stays "", one holding a JSON object or array arrives parsed. */
    private static JsonValue expectedElement(final Object element) {
        if (element instanceof String s) {
            if (s.startsWith("{") || s.startsWith("[")) {
                try (var reader = Json.createReader(new java.io.StringReader(s))) {
                    return reader.readValue();
                } catch (final RuntimeException notJson) {
                    return Json.createValue(s);
                }
            }
            return Json.createValue(s);
        }
        return expected(element);
    }

    // ---- page ----------------------------------------------------------------------------------------------------

    /** A page built, updated and partly removed by a UIContext writing in the given protocol version. */
    private static void page(final Path dir, final String name, final int version) throws IOException {
        final List<Integer> frames = new ArrayList<>();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final WebSocketPusher pusher = pusher(stream, frames);
        pusher.setStringDictionary(new StringDictionary(70_000, 4, 70_000));
        final WebSocket socket = new WebSocket() {
            @Override
            public void flush() {
                try {
                    pusher.flush();
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        socket.setUpgradeRequestData(new UpgradeRequestData(Map.of(), "fixtures", null));
        final TxnContext txn = new TxnContext(socket);
        txn.getWriter().setEncoder(new WebsocketEncoder() {
            @Override
            public void beginObject() {
            }

            @Override
            public void endObject() {
                encode(ServerToClientModel.END, null);
            }

            @Override
            public int protocolVersion() {
                return version;
            }

            @Override
            public void encode(final ServerToClientModel model, final Object value) {
                try {
                    pusher.encode(model, value);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        final UIContext context = new UIContext(socket, txn, config);

        final PFlowPanel[] rows = new PFlowPanel[3];
        final List<PLabel> labels = new ArrayList<>();
        context.execute(() -> {
            final PFlowPanel root = Element.newPFlowPanel();
            root.addStyleName("root");
            PWindow.getMain().add(root);
            for (int i = 0; i < rows.length; i++) {
                rows[i] = Element.newPFlowPanel();
                rows[i].addStyleName("row");
                for (int j = 0; j < 4; j++) {
                    final PLabel label = Element.newPLabel("cell " + i + "." + j);
                    labels.add(label);
                    rows[i].add(label);
                }
                final PButton button = Element.newPButton("Buy " + i);
                button.addClickHandler(e -> {
                });
                rows[i].add(button);
                root.add(rows[i]);
            }
        });
        context.execute(() -> labels.forEach(label -> label.setText(label.getText() + " updated"))); // update runs
        context.execute(() -> {
            for (int i = 0; i < labels.size(); i += 2) labels.get(i).addStyleName("even");
        });
        context.execute(() -> rows[1].removeFromParent());
        context.execute(() -> rows[2].clear());
        write(dir, name, stream.toByteArray(), frames, Json.createObjectBuilder().add("version", version));
    }

    // ---- writing -------------------------------------------------------------------------------------------------

    private interface Writes {
        void write(WebSocketPusher pusher) throws IOException;
    }

    private static byte[] encode(final StringDictionary dictionary, final List<Integer> frames, final Writes writes) throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final WebSocketPusher pusher = pusher(stream, frames);
        if (dictionary != null) pusher.setStringDictionary(dictionary);
        writes.write(pusher);
        pusher.flush();
        return stream.toByteArray();
    }

    /** A pusher whose frames go to the stream; the end of each frame is added to frames. */
    private static WebSocketPusher pusher(final ByteArrayOutputStream stream, final List<Integer> frames) {
        final Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class<?>[] {Session.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "sendBinary" -> {
                    final ByteBuffer buffer = (ByteBuffer) args[0];
                    final byte[] chunk = new byte[buffer.remaining()];
                    buffer.duplicate().get(chunk);
                    stream.writeBytes(chunk);
                    frames.add(stream.size());
                    ((Callback) args[1]).succeed();
                    yield null;
                }
                case "isOpen" -> true;
                case "hashCode" -> 1;
                case "equals" -> proxy == args[0];
                default -> null;
            });
        return new WebSocketPusher(session, 1 << 20, 1_000, new ByteBufferPool(1 << 20, 8));
    }

    private static void write(final Path dir, final String name, final byte[] bytes, final List<Integer> frames,
                              final JsonObjectBuilder json) throws IOException {
        final JsonArrayBuilder ends = Json.createArrayBuilder();
        frames.forEach(ends::add);
        Files.write(dir.resolve(name + ".bin"), bytes);
        Files.writeString(dir.resolve(name + ".json"), json.add("frames", ends).build().toString());
    }
}
