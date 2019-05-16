/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.ui.datagrid2;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author mbagdouri
 */
public abstract class SpyDataGridModel<K, V> implements DataGridModel<K, V> {

    private final DataGridModel<K, V> model;

    public SpyDataGridModel(final DataGridModel<K, V> model) {
        super();
        this.model = model;
    }

    @Override
    public void setData(final V v) {
        model.setData(v);
        if (model.getBound()) onDataUpdate();
    }

    @Override
    public void setData(final Collection<V> c) {
        model.setData(c);
        if (model.getBound()) onDataUpdate();
    }

    @Override
    public void updateData(final K k, final Consumer<V> updater) {
        model.updateData(k, updater);
        if (model.getBound()) onDataUpdate();
    }

    @Override
    public void updateData(final Map<K, Consumer<V>> updaters) {
        model.updateData(updaters);
        if (model.getBound()) onDataUpdate();
    }

    @Override
    public V getData(final K k) {
        return model.getData(k);
    }

    @Override
    public V removeData(final K k) {
        final V v = model.removeData(k);
        if (model.getBound()) onDataUpdate();
        return v;
    }

    @Override
    public void forEach(final BiConsumer<K, V> action) {
        model.forEach(action);
    }

    @Override
    public int size() {
        return model.size();
    }

    @Override
    public void setBound(final boolean bound) {
        final boolean wasBound = model.getBound();
        model.setBound(bound);
        if (!wasBound && model.getBound()) onDataUpdate();
    }

    @Override
    public boolean getBound() {
        return model.getBound();
    }

    protected abstract void onDataUpdate();
}
