
package com.ponysdk.core.ui.datagrid2.controller;

import java.util.function.Consumer;

public class DataGridControllerWrapper<K, V> extends SpyDataGridController<K, V> {

    private final Runnable dataUpdateHandler;
    private final Consumer<K> dataRemoveHandler;

    public DataGridControllerWrapper(final DataGridController<K, V> controller, final Runnable dataUpdateHandler,
            final Consumer<K> dataRemoveHandler) {
        super(controller);
        this.dataUpdateHandler = dataUpdateHandler;
        this.dataRemoveHandler = dataRemoveHandler;
    }

    @Override
    protected void onDataUpdate() {
        dataUpdateHandler.run();
    }

    @Override
    public V removeData(final K k) {
        final V v = super.removeData(k);
        dataRemoveHandler.accept(k);
        return v;
    }
}