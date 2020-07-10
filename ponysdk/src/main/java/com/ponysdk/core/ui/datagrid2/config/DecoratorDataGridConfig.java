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

package com.ponysdk.core.ui.datagrid2.config;

import java.util.List;
import java.util.Map;

/**
 * @author mbagdouri
 */
public class DecoratorDataGridConfig<V> implements DataGridConfig<V> {

    private final DataGridConfig<V> config;

    protected DecoratorDataGridConfig(final DataGridConfig<V> config) {
        super();
        this.config = config;
    }

    @Override
    public List<Sort<V>> getSorts() {
        return config.getSorts();
    }

    @Override
    public List<ColumnConfig<V>> getColumnConfigs() {
        return config.getColumnConfigs();
    }

    @Override
    public void setCustomValue(final String key, final Object value) {
        config.setCustomValue(key, value);
    }

    @Override
    public Map<String, Object> getReadOnlyCustomValues() {
        return config.getReadOnlyCustomValues();
    }

    @Override
    public <T> T getCustomValue(final String key) {
        return config.getCustomValue(key);
    }
}
