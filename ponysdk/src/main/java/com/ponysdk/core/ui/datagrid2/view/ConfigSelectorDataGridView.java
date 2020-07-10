/*
 * Copyright (c) 2019 PonySDK Owners: Luciano Broussal <luciano.broussal AT
 * gmail.com> Mathieu Barbier <mathieu.barbier AT gmail.com> Nicolas Ciaravola
 * <nicolas.ciaravola.pro AT gmail.com>
 *
 * WebSite: http://code.google.com/p/pony-sdk/
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

package com.ponysdk.core.ui.datagrid2.view;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.Sort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;

/**
 * @author mbagdouri
 */
public class ConfigSelectorDataGridView<K, V> extends WidgetDecoratorDataGridView<K, V> {

    private static final byte COLUMN_SORT = 0;
    private static final byte GENERAL_SORT = 1;
    private final PComplexPanel layout;
    private final String defaultKey;
    private final LinkedHashMap<String, ConfigHandler> handlers = new LinkedHashMap<>();
    private DataGridAdapter<K, V> adapter;
    private ConfigHandler current;

    public ConfigSelectorDataGridView(final DataGridView<K, V> view, final String defaultKey) {
        super(view);
        layout = createLayout();
        this.defaultKey = defaultKey;
    }

    private void checkAdapter() {
        if (adapter == null) throw new IllegalStateException("No " + DataGridAdapter.class + " has been set yet");
    }

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        super.setAdapter(adapter);
        this.adapter = adapter;
        final ConfigHandler defaultHandler = new ConfigHandler(defaultKey, null);
        handlers.put(defaultKey, defaultHandler);
        current = defaultHandler;
        onSelectConfigWidget(defaultKey, current.widget);
    }

    public void setConfigEntries(final List<ConfigEntry<V>> entries) {
        checkAdapter();
        final DataGridConfig<V> defaultConfig = handlers.get(defaultKey).getConfig();
        layout.clear();
        handlers.clear();
        if (entries.size() == 0 || !defaultKey.equals(entries.get(0).getKey())) {
            handlers.put(defaultKey, new ConfigHandler(defaultKey, defaultConfig));
        }
        for (final ConfigEntry<V> entry : entries) {
            addConfigEntry(entry.getKey(), entry.getConfig());
        }
        current = handlers.get(defaultKey);
        onSelectConfigWidget(defaultKey, current.widget);
        ConfigSelectorDataGridView.super.setConfig(current.config);
    }

    public List<ConfigEntry<V>> decodeConfigEntries(final String encodedConf) throws DecodeException {
        checkAdapter();
        try {
            final ByteBuffer buffer = Base64.getDecoder().decode(StandardCharsets.US_ASCII.encode(encodedConf));
            int entriesNumber = buffer.getInt();
            final List<ConfigEntry<V>> entries = new ArrayList<>(entriesNumber);
            while (entriesNumber-- > 0) {
                entries.add(decodeConfigEntry(buffer));
            }
            return entries;
        } catch (final IllegalArgumentException | BufferUnderflowException e) {
            throw new DecodeException(e);
        }
    }

    public String encodeConfigEntries(final List<ConfigEntry<V>> entries) {
        checkAdapter();
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer = encodeInt(buffer, entries.size());
        for (final ConfigEntry<V> entry : entries) {
            buffer = encodeConfigEntry(buffer, entry);
        }
        buffer.flip();
        return StandardCharsets.US_ASCII.decode(Base64.getEncoder().encode(buffer)).toString();
    }

    private ConfigEntry<V> decodeConfigEntry(final ByteBuffer buffer) throws DecodeException {
        final String key = decodeString(buffer);
        final DataGridConfig<V> config = decodeConfig(buffer);
        return e(key, config);
    }

    private ByteBuffer encodeConfigEntry(ByteBuffer buffer, final ConfigEntry<V> entry) {
        buffer = encodeString(buffer, entry.getKey());
        buffer = encodeConfig(buffer, entry.getConfig());
        return buffer;
    }

    private DataGridConfig<V> decodeConfig(final ByteBuffer buffer) throws DecodeException {
        int sortsNumber = buffer.getInt();
        final DataGridConfigBuilder<V> builder = new DataGridConfigBuilder<>();
        while (sortsNumber-- > 0) {
            final Sort<V> sort = decodeSort(buffer);
            builder.addSort(sort);
        }
        int columnConfigsNumber = buffer.getInt();
        while (columnConfigsNumber-- > 0) {
            final ColumnConfig<V> columnConfig = decodeColumnConfig(buffer);
            if (columnConfig == null) continue;
            builder.addColumnConfig(columnConfig);
        }
        int customValuesNumber = buffer.getInt();
        while (customValuesNumber-- > 0) {
            decodeCustomValue(buffer, builder);
        }
        return builder.build();
    }

    private ByteBuffer encodeConfig(ByteBuffer buffer, final DataGridConfig<V> config) {
        final List<Sort<V>> sorts = config.getSorts();
        buffer = encodeInt(buffer, sorts.size());
        for (final Sort<V> sort : sorts) {
            buffer = encodeSort(buffer, sort);
        }
        final List<ColumnConfig<V>> columnConfigs = config.getColumnConfigs();
        buffer = encodeInt(buffer, columnConfigs.size());
        for (final ColumnConfig<V> columnConfig : columnConfigs) {
            buffer = encodeColumnConfig(buffer, columnConfig);
        }
        final Map<String, Object> customValues = config.getReadOnlyCustomValues();
        buffer = encodeInt(buffer, customValues.size());
        for (final Map.Entry<String, Object> entry : customValues.entrySet()) {
            buffer = encodeCustomValue(buffer, entry.getKey(), entry.getValue());
        }
        return buffer;
    }

    private void decodeCustomValue(final ByteBuffer buffer, final DataGridConfigBuilder<V> builder) throws DecodeException {
        final String key = decodeString(buffer);
        final Object value = decodeConfigCustomValue(key, decodeString(buffer));
        if (value == null) return;
        builder.addCustomValue(key, value);
    }

    private ByteBuffer encodeCustomValue(ByteBuffer buffer, final String key, final Object value) {
        buffer = encodeString(buffer, key);
        buffer = encodeString(buffer, encodeConfigCustomValue(key, value));
        return buffer;
    }

    private static <V> ColumnConfig<V> decodeColumnConfig(final ByteBuffer buffer) {
        final String columnId = decodeString(buffer);
        final State state = decodeState(buffer);
        final int width = buffer.getInt();
        return new ColumnConfig<>(columnId, state, width);
    }

    private static <V> ByteBuffer encodeColumnConfig(ByteBuffer buffer, final ColumnConfig<V> columnConfig) {
        buffer = encodeString(buffer, columnConfig.getColumnId());
        buffer = encodeString(buffer, columnConfig.getState().name());
        buffer = encodeInt(buffer, columnConfig.getWidth());
        return buffer;
    }

    private static State decodeState(final ByteBuffer buffer) {
        try {
            return State.valueOf(decodeString(buffer));
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private static <V> Sort<V> decodeSort(final ByteBuffer buffer) {
        final byte b = buffer.get();
        if (b == COLUMN_SORT) return new ColumnSort<>(decodeString(buffer), decodeBoolean(buffer));
        else return null;
    }

    private static <V> ByteBuffer encodeSort(ByteBuffer buffer, final Sort<V> sort) {
        if (sort instanceof ColumnSort) {
            buffer = encodeByte(buffer, COLUMN_SORT);
            final ColumnSort<V> columnSort = (ColumnSort<V>) sort;
            buffer = encodeString(buffer, columnSort.getColumnId());
            buffer = encodeBoolean(buffer, columnSort.isAsc());
        } else { // sort instanceof GeneralSort
            buffer = encodeByte(buffer, GENERAL_SORT);
        }
        return buffer;
    }

    private static boolean decodeBoolean(final ByteBuffer buffer) {
        return buffer.get() != 0;
    }

    private static ByteBuffer encodeBoolean(ByteBuffer buffer, final boolean value) {
        buffer = ensureRemaining(buffer, 1);
        buffer.put(value ? (byte) 1 : (byte) 0);
        return buffer;
    }

    private static ByteBuffer encodeInt(ByteBuffer buffer, final int value) {
        buffer = ensureRemaining(buffer, 4);
        buffer.putInt(value);
        return buffer;
    }

    private static ByteBuffer encodeByte(ByteBuffer buffer, final byte value) {
        buffer = ensureRemaining(buffer, 1);
        buffer.put(value);
        return buffer;
    }

    private static String decodeString(final ByteBuffer buffer) {
        final int length = buffer.getInt();
        final int limit = buffer.limit();
        buffer.limit(buffer.position() + length);
        final String str = StandardCharsets.UTF_8.decode(buffer).toString();
        buffer.limit(limit);
        return str;
    }

    private static ByteBuffer encodeString(ByteBuffer buffer, final String value) {
        final byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        buffer = ensureRemaining(buffer, 4 + encoded.length);
        buffer.putInt(encoded.length);
        buffer.put(encoded);
        return buffer;
    }

    private static ByteBuffer ensureRemaining(final ByteBuffer buffer, final int length) {
        if (buffer.remaining() >= length) return buffer;
        final ByteBuffer b = ByteBuffer.allocate(Math.max(buffer.capacity() * 2, buffer.capacity() + length));
        buffer.flip();
        b.put(buffer);
        return b;
    }

    public boolean addConfigEntry(final String key, final DataGridConfig<V> config) {
        checkAdapter();
        Objects.requireNonNull(config);
        if (handlers.containsKey(key)) return false;
        handlers.put(key, new ConfigHandler(key, config));
        return true;
    }

    public DataGridConfig<V> removeConfigEntry(final String key) {
        checkAdapter();
        final ConfigHandler handler = handlers.get(key);
        if (handler == null) return null;
        final DataGridConfig<V> config = handler.getConfig();
        handler.onRemove();
        return config;
    }

    public DataGridConfig<V> getCurrentConfig() {
        checkAdapter();
        return current.getConfig();
    }

    public void selectConfig(final String key) {
        checkAdapter();
        final ConfigHandler handler = handlers.get(key);
        if (handler == null) return;
        handler.onSelect();
    }

    public List<ConfigEntry<V>> getConfigEntries() {
        checkAdapter();
        final List<ConfigEntry<V>> entries = new ArrayList<>(handlers.size());
        for (final ConfigHandler handler : handlers.values()) {
            entries.add(new SimpleConfigEntry<>(handler.key, handler.getConfig()));
        }
        return entries;
    }

    protected PComplexPanel createLayout() {
        return Element.newDiv();
    }

    protected void addConfigWidget(final Object key, final PComplexPanel layout, final IsPWidget configWidget) {
        layout.add(configWidget);
    }

    @Override
    public PWidget getDecoratorWidget() {
        return layout;
    }

    public static interface ConfigEntry<V> {

        String getKey();

        DataGridConfig<V> getConfig();

    }

    protected IsPWidget createWidget(final Object key, final ConfigSelectorDataGridView<?, V>.ConfigHandler handler) {
        final PComplexPanel span = Element.newSpan();
        final PButton select = Element.newPButton(key.toString());
        select.addClickHandler(e -> handler.onSelect());
        span.add(select);
        if (!key.equals(defaultKey)) {
            final PButton remove = Element.newPButton("X");
            remove.addClickHandler(e -> handler.onRemove());
            span.add(remove);
        }
        return span;
    }

    protected void onSelectConfigWidget(final Object key, final IsPWidget configWidget) {
        ((PButton) ((PComplexPanel) configWidget.asWidget()).getWidget(0)).setEnabled(false);
    }

    protected void onUnselectConfigWidget(final Object key, final IsPWidget configWidget) {
        ((PButton) ((PComplexPanel) configWidget.asWidget()).getWidget(0)).setEnabled(true);
    }

    public static <V> ConfigEntry<V> e(final String key, final DataGridConfig<V> config) {
        return new SimpleConfigEntry<>(key, config);
    }

    protected class ConfigHandler {

        private final String key;
        private final IsPWidget widget;
        private DataGridConfig<V> config;

        private ConfigHandler(final String key, final DataGridConfig<V> config) {
            this.key = key;
            this.config = config;
            widget = createWidget(key, this);
            addConfigWidget(key, layout, widget);
        }

        public void onRemove() {
            layout.remove(widget.asWidget());
            if (current == null || key.equals(defaultKey)) return;
            if (this == current) {
                handlers.get(defaultKey).onSelect();
            }
            handlers.remove(key);
        }

        public void onSelect() {
            if (current == null || this == current) return;
            current.config = getView().getConfig();
            onUnselectConfigWidget(key, current.widget);
            current = this;
            ConfigSelectorDataGridView.super.setConfig(config);
            onSelectConfigWidget(key, widget);
        }

        private DataGridConfig<V> getConfig() {
            if (this == current) {
                config = getView().getConfig();
            }
            return config;
        }

    }

    public static class SimpleConfigEntry<V> implements ConfigEntry<V> {

        private final String key;
        private final DataGridConfig<V> config;

        public SimpleConfigEntry(final String key, final DataGridConfig<V> config) {
            super();
            this.key = key;
            this.config = config;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public DataGridConfig<V> getConfig() {
            return config;
        }

    }
}