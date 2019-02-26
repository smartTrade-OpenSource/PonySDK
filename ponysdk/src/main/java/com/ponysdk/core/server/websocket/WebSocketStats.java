/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.server.websocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;
import com.ponysdk.core.util.Pair;

public class WebSocketStats {

    private static final int MAX_STRING_SIZE = 65535;
    private static final byte NULL_VALUE = 0;
    private static final byte FALSE_VALUE = 1;
    private static final byte TRUE_VALUE = 2;
    private static final byte BYTE_VALUE = 3;
    private static final byte SHORT_VALUE = 4;
    private static final byte INT_VALUE = 5;
    private static final byte FLOAT_VALUE = 6;
    private static final byte DOUBLE_VALUE = 7;
    private static final byte STRING_VALUE = 8;

    private static final Logger log = LoggerFactory.getLogger(WebSocketStats.class);
    private final Map<ServerToClientModel, ? extends Map<Object, ? extends Record>> stats;
    private final Bandwidth summaryBandwidth;
    private final long summaryCount;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final boolean grouped;

    public WebSocketStats(final Map<ServerToClientModel, ? extends Map<Object, ? extends Record>> stats, final LocalDateTime startTime,
            final LocalDateTime endTime, final boolean grouped) {
        super();
        this.startTime = startTime;
        this.endTime = endTime;
        this.stats = stats;
        this.grouped = grouped;
        long summaryMeta = 0L;
        long summaryData = 0L;
        long count = 0L;
        for (final Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>> entry : stats.entrySet()) {
            for (final Record record : entry.getValue().values()) {
                count += record.getCount();
                summaryMeta += (long) record.getMetaBytes() * record.getCount();
                summaryData += (long) record.getDataBytes() * record.getCount();
            }
        }
        this.summaryBandwidth = new Bandwidth(Math.max(1L, summaryMeta), Math.max(1L, summaryData));
        this.summaryCount = count;
    }

    public void toCsv(final File file, final char separator, final Predicate<Object> valueFilter,
                      final Predicate<ServerToClientModel> modelFilter)
            throws IOException {
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writeCsvHeader(separator, writer);
            Stream<? extends Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>>> s = stats.entrySet().stream();
            if (modelFilter != null) s = s.filter(e -> modelFilter.test(e.getKey()));

            Stream<Pair<ServerToClientModel, ? extends Entry<Object, ? extends Record>>> s2 = s
                .flatMap(e -> e.getValue().entrySet().stream().map(ee -> new Pair<>(e.getKey(), ee)));
            if (valueFilter != null) s2 = s2.filter(p -> valueFilter.test(p.getSecond().getKey()));

            s2.sorted((p1, p2) -> p2.getSecond().getValue().compareTo(p1.getSecond().getValue())).forEach(p -> {
                try {
                    writeCsvLine(separator, writer, p);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (final IllegalArgumentException e) {
            final Throwable t = e.getCause();
            if (t instanceof IOException) throw (IOException) t;
            throw e;
        }
    }

    private void writeCsvLine(final char separator, final BufferedWriter writer,
                              final Pair<ServerToClientModel, ? extends Entry<Object, ? extends Record>> p)
            throws IOException {
        final NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(4);

        final Record record = p.getSecond().getValue();
        writer.write('\n');
        writer.write(Objects.toString(p.getFirst()));
        writer.write(separator);
        writer.write(Objects.toString(modelToValueType(p.getFirst())));
        writer.write(separator);
        Object v = p.getSecond().getKey();
        if (isGrouped()) {
            final Pair<?, ?> pair = (Pair<?, ?>) v;
            writer.write(Objects.toString(pair.getSecond()));
            writer.write(separator);
            v = pair.getFirst();
        }
        writer.write(writeableValue(Objects.toString(v), separator));
        writer.write(separator);
        writer.write(Integer.toString(record.getCount()));
        writer.write(separator);
        writer.write(Integer.toString(record.getMetaBytes()));
        writer.write(separator);
        writer.write(Integer.toString(record.getDataBytes()));
        writer.write(separator);
        final long bandwidth = record.getCount() * ((long) record.getMetaBytes() + record.getDataBytes());
        writer.write(Long.toString(bandwidth));
        writer.write(separator);
        writer.write(format.format((double) bandwidth / summaryBandwidth.getTotal()));
    }

    private String writeableValue(String s, final char separator) {
        if (s.length() > 512) {
            s = s.substring(0, 512);
        }
        return s.replace('\n', ' ').replace('\r', ' ').replace(separator, ' ');
    }

    private void writeCsvHeader(final char separator, final BufferedWriter writer) throws IOException {
        writer.write("MODEL");
        writer.write(separator);
        writer.write("VALUE TYPE");
        writer.write(separator);
        if (isGrouped()) {
            writer.write("GROUP");
            writer.write(separator);
        }
        writer.write("VALUE");
        writer.write(separator);
        writer.write("COUNT");
        writer.write(separator);
        writer.write("META SIZE");
        writer.write(separator);
        writer.write("DATA SIZE");
        writer.write(separator);
        writer.write("BANDWIDTH");
        writer.write(separator);
        writer.write("BANDWIDTH %");
    }

    public void encode(final File file) throws IOException {
        try (DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            outputStream.writeBoolean(grouped);
            outputStream.writeLong(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            outputStream.writeLong(endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            outputStream.writeInt(stats.size());
            for (final Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>> entry : stats.entrySet()) {
                outputStream.writeUTF(entry.getKey().name());
                final Map<Object, ? extends Record> records = entry.getValue();
                outputStream.writeInt(records.size());
                for (final Map.Entry<Object, ? extends Record> recordEntry : records.entrySet()) {
                    final Object value = recordEntry.getKey();
                    if (grouped) {
                        final Pair<Object, String> pair = (Pair<Object, String>) value;
                        encodeValue(outputStream, pair.getFirst());
                        writeUTF(outputStream, pair.getSecond());
                    }
                    final Record record = recordEntry.getValue();
                    outputStream.writeInt(record.getMetaBytes());
                    outputStream.writeInt(record.getDataBytes());
                    outputStream.writeInt(record.getCount());
                }
            }
        }
    }

    private static void encodeValue(final DataOutputStream outputStream, final Object v) throws IOException {
        if (v == null) {
            outputStream.writeByte(NULL_VALUE);
        } else if (v instanceof Boolean) {
            outputStream.write(v.equals(Boolean.TRUE) ? TRUE_VALUE : FALSE_VALUE);
        } else if (v instanceof Byte) {
            outputStream.write(BYTE_VALUE);
            outputStream.writeByte((byte) v);
        } else if (v instanceof Short) {
            outputStream.write(SHORT_VALUE);
            outputStream.writeShort((short) v);
        } else if (v instanceof Integer) {
            outputStream.write(INT_VALUE);
            outputStream.writeInt((int) v);
        } else if (v instanceof Float) {
            outputStream.write(FLOAT_VALUE);
            outputStream.writeFloat((float) v);
        } else if (v instanceof Double) {
            outputStream.write(DOUBLE_VALUE);
            outputStream.writeDouble((double) v);
        } else {
            outputStream.write(STRING_VALUE);
            writeUTF(outputStream, v.toString());
        }
    }

    private static void writeUTF(final DataOutputStream outputStream, String s) throws IOException {
        if (s == null) s = "";
        else if (s.length() >= MAX_STRING_SIZE) {
            s = s.substring(0, MAX_STRING_SIZE);
        }
        try {
            outputStream.writeUTF(s);
        } catch (final UTFDataFormatException e) {
            outputStream.writeUTF(s.substring(0, MAX_STRING_SIZE / 3));
        }
    }

    public static class DecodeException extends Exception {

        private DecodeException(final String message) {
            super(message);
        }

        private DecodeException(final Throwable cause) {
            super(cause);
        }

    }

    public static WebSocketStats decode(final File file) throws FileNotFoundException, IOException, DecodeException {
        try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final boolean grouped = inputStream.readBoolean();
            final LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(inputStream.readLong()),
                ZoneId.systemDefault());
            final LocalDateTime endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(inputStream.readLong()),
                ZoneId.systemDefault());
            final Map<ServerToClientModel, Map<Object, Record>> stats = new EnumMap<>(ServerToClientModel.class);
            final int nbModels = inputStream.readInt();
            for (int i = 0; i < nbModels; i++) {
                final String modelName = inputStream.readUTF();
                ServerToClientModel model = null;
                try {
                    model = ServerToClientModel.valueOf(modelName);
                } catch (final IllegalArgumentException e) {
                    log.warn("Unrecognized ServerToClientModel value {} (possibly deprecated)", modelName);
                }
                final int nbRecords = inputStream.readInt();
                final Map<Object, Record> records = stats.computeIfAbsent(model, m -> new HashMap<>(nbRecords));
                for (int j = 0; j < nbRecords; j++) {
                    Object value = decodeValue(inputStream);
                    if (grouped) {
                        value = new Pair<>(value, inputStream.readUTF());
                    }
                    records.put(value, new ImmutableRecord(inputStream.readInt(), inputStream.readInt(), inputStream.readInt()));
                }
            }
            return new WebSocketStats(stats, startTime, endTime, grouped);
        } catch (final EOFException e) {
            throw new DecodeException(e);
        }
    }

    private static Object decodeValue(final DataInputStream inputStream) throws IOException, DecodeException {
        final byte valueType = inputStream.readByte();
        switch (valueType) {
            case NULL_VALUE:
                return null;
            case FALSE_VALUE:
                return false;
            case TRUE_VALUE:
                return true;
            case BYTE_VALUE:
                return inputStream.readByte();
            case SHORT_VALUE:
                return inputStream.readShort();
            case INT_VALUE:
                return inputStream.readInt();
            case FLOAT_VALUE:
                return inputStream.readFloat();
            case DOUBLE_VALUE:
                return inputStream.readDouble();
            case STRING_VALUE:
                return inputStream.readUTF();
        }
        throw new DecodeException("Unrecognized value type " + valueType);
    }

    public Bandwidth getBandwidthForPattern(final Pattern pattern, final Predicate<ServerToClientModel> modelFilter) {
        return getBandwidthFor(v -> pattern.matcher(Objects.toString(isGrouped() ? ((Pair<?, ?>) v).getFirst() : v)).find(),
            modelFilter);
    }

    public Bandwidth getBandwidthFor(final Predicate<Object> valueFilter, final Predicate<ServerToClientModel> modelFilter) {
        long meta = 0L;
        long data = 0L;
        for (final Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>> statsEntry : stats.entrySet()) {
            if (modelFilter == null || modelFilter.test(statsEntry.getKey())) {
                for (final Entry<Object, ? extends Record> entry : statsEntry.getValue().entrySet()) {
                    if (valueFilter == null || valueFilter.test(entry.getKey())) {
                        final Record record = entry.getValue();
                        meta += (long) record.getMetaBytes() * record.getCount();
                        data += (long) record.getDataBytes() * record.getCount();
                    }
                }
            }
        }
        return new Bandwidth(meta, data);
    }

    public Map<ValueTypeModel, Bandwidth> getBandwidthPerValueType() {
        final Map<ValueTypeModel, Bandwidth> map = new EnumMap<>(ValueTypeModel.class);
        for (final Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>> entry : stats.entrySet()) {
            for (final Entry<Object, ? extends Record> recordEntry : entry.getValue().entrySet()) {
                final Record record = recordEntry.getValue();
                final ValueTypeModel valueType = modelToValueType(entry.getKey());
                map.computeIfAbsent(valueType, vt -> new Bandwidth(0L, 0L)).add((long) record.getCount() * record.getMetaBytes(),
                    (long) record.getCount() * record.getDataBytes());
            }
        }
        return map;
    }

    public Map<Pair<ValueTypeModel, String>, Bandwidth> getBandwidthPerValueTypePerGroup() {
        if (!grouped) return null;
        final Map<Pair<ValueTypeModel, String>, Bandwidth> map = new HashMap<>();
        for (final Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>> entry : stats.entrySet()) {
            for (final Entry<Object, ? extends Record> recordEntry : entry.getValue().entrySet()) {
                final Record record = recordEntry.getValue();
                final String groupName = ((Pair<Object, String>) recordEntry.getKey()).getSecond();
                final Pair<ValueTypeModel, String> pair = new Pair<>(modelToValueType(entry.getKey()), groupName);
                map.computeIfAbsent(pair, p -> new Bandwidth(0L, 0L)).add((long) record.getCount() * record.getMetaBytes(),
                    (long) record.getCount() * record.getDataBytes());
            }
        }
        return map;
    }

    public Map<ServerToClientModel, Bandwidth> getBandwidthPerServerToClientModel() {
        final Map<ServerToClientModel, Bandwidth> map = new EnumMap<>(ServerToClientModel.class);
        for (final Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>> entry : stats.entrySet()) {
            long meta = 0L;
            long data = 0L;
            for (final Entry<Object, ? extends Record> recordEntry : entry.getValue().entrySet()) {
                final Record record = recordEntry.getValue();
                meta += (long) record.getMetaBytes() * record.getCount();
                data += (long) record.getDataBytes() * record.getCount();
            }
            map.put(entry.getKey(), new Bandwidth(meta, data));
        }
        return map;
    }

    public Map<Pair<ServerToClientModel, String>, Bandwidth> getBandwidthPerServerToClientModelPerGroup() {
        if (!grouped) return null;
        final Map<Pair<ServerToClientModel, String>, Bandwidth> map = new HashMap<>();
        for (final Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>> entry : stats.entrySet()) {
            for (final Entry<Object, ? extends Record> recordEntry : entry.getValue().entrySet()) {
                final Record record = recordEntry.getValue();
                final String groupName = ((Pair<Object, String>) recordEntry.getKey()).getSecond();
                final Pair<ServerToClientModel, String> pair = new Pair<>(entry.getKey(), groupName);
                map.computeIfAbsent(pair, p -> new Bandwidth(0L, 0L)).add((long) record.getCount() * record.getMetaBytes(),
                    (long) record.getCount() * record.getDataBytes());
            }
        }
        return map;
    }

    public Map<String, Bandwidth> getBandwidthPerGroup() {
        if (!grouped) return null;
        final Map<String, Bandwidth> map = new HashMap<>();
        for (final Entry<ServerToClientModel, ? extends Map<Object, ? extends Record>> entry : stats.entrySet()) {
            for (final Entry<Object, ? extends Record> recordEntry : entry.getValue().entrySet()) {
                final Record record = recordEntry.getValue();
                final String groupName = ((Pair<Object, String>) recordEntry.getKey()).getSecond();
                map.computeIfAbsent(groupName, g -> new Bandwidth(0L, 0L)).add((long) record.getCount() * record.getMetaBytes(),
                    (long) record.getCount() * record.getDataBytes());
            }
        }
        return map;
    }

    private static ValueTypeModel modelToValueType(final ServerToClientModel model) {
        return model == null ? null : model.getTypeModel();
    }

    public boolean isGrouped() {
        return grouped;
    }

    public Map<ServerToClientModel, ? extends Map<Object, ? extends Record>> getStats() {
        return stats;
    }

    public Bandwidth getSummaryBandwidth() {
        return summaryBandwidth;
    }

    public long getSummaryCount() {
        return summaryCount;
    }

    public static class Bandwidth implements Comparable<Bandwidth> {

        protected long meta;
        protected long data;

        public Bandwidth(final long meta, final long data) {
            super();
            this.meta = meta;
            this.data = data;
        }

        void add(final long meta, final long data) {
            this.meta += meta;
            this.data += data;
        }

        public long getTotal() {
            return meta + data;
        }

        public long getMeta() {
            return meta;
        }

        public long getData() {
            return data;
        }

        public float getMetaRatio() {
            return (float) meta / getTotal();
        }

        public float getDataRatio() {
            return (float) data / getTotal();
        }

        @Override
        public String toString() {
            return "[meta=" + meta + ", data=" + data + "]";
        }

        @Override
        public int compareTo(final Bandwidth o) {
            final long difference = meta + data - (o == null ? 0 : o.meta + o.data);
            if (difference > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            else if (difference < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            else return (int) difference;
        }
    }

    public abstract static class Record implements Comparable<Record> {

        private final int metaBytes;
        private final int dataBytes;

        public Record(final int metaBytes, final int dataBytes) {
            super();
            this.metaBytes = metaBytes;
            this.dataBytes = dataBytes;
        }

        public int getMetaBytes() {
            return metaBytes;
        }

        public int getDataBytes() {
            return dataBytes;
        }

        public abstract int getCount();

        @Override
        public int compareTo(final Record o) {
            final long otherValue = o == null ? 0L : o.getCount() * ((long) o.getMetaBytes() + o.getDataBytes());
            final long difference = getCount() * ((long) getMetaBytes() + getDataBytes()) - otherValue;
            if (difference > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            else if (difference < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            else return (int) difference;
        }
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final NumberFormat numberFormat = NumberFormat.getNumberInstance();
        sb.append("Web socket stats recording from ").append(startTime).append(" to ").append(endTime);
        sb.append(": Total bandwidth=").append(numberFormat.format(summaryBandwidth.getTotal()));
        sb.append(", Meta bandwidth=").append(numberFormat.format(summaryBandwidth.meta));
        sb.append(" (").append(String.format("%.2f", 100.0 * summaryBandwidth.meta / summaryBandwidth.getTotal())).append("%)");
        sb.append(", Data bandwidth=").append(numberFormat.format(summaryBandwidth.data));
        sb.append(" (").append(String.format("%.2f", 100.0 * summaryBandwidth.data / summaryBandwidth.getTotal())).append("%)");
        sb.append(", Frame count=").append(numberFormat.format(summaryCount));
        return sb.toString();
    }

    private static class ImmutableRecord extends Record {

        private final int count;

        private ImmutableRecord(final int metaBytes, final int dataBytes, final int count) {
            super(metaBytes, dataBytes);
            this.count = count;
        }

        @Override
        public int getCount() {
            return count;
        }

    }

}
