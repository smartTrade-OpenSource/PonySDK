/*
 * Copyright (c) 2020 PonySDK
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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.ponysdk.core.ui.datagrid2.data.DataSrcResult;
import com.ponysdk.core.ui.datagrid2.datasource.DataGridSource;

/**
 */
public class DataGetterServiceImpl<K, V> implements DataGetterService<K, V> {

    //    private final DataGridController<K, V> controller;
    //
    //    public DataGetterServiceImpl(final DataGridController<K, V> controller) {
    //        this.controller = controller;
    //    }
    //
    //    @Override
    //    public void prepareData(final int row, final int size, final boolean horizontalScroll, final Consumer<Integer> consumer) {
    //        CompletableFuture.supplyAsync(() -> prepareData(row, size, horizontalScroll)).thenAccept(consumer::accept);
    //    }
    //
    //    private Integer prepareData(final int row, final int size, final boolean horizontalScroll) {
    //        controller.prepareLiveDataOnScreen(row, size, horizontalScroll);
    //        try {
    //            Thread.sleep(2000);
    //        } catch (final InterruptedException e) {
    //            e.printStackTrace();
    //        }
    //        return 1;
    //    }

    private final DataGridSource<K, V> dataSource;

    public DataGetterServiceImpl(final DataGridSource<K, V> dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public synchronized void prepareData(final DataSrcResult<V> dataSrcResult, final Consumer<DataSrcResult<V>> consumer) {
        CompletableFuture.supplyAsync(() -> prepareData(dataSrcResult)).thenAccept(consumer::accept);
    }

    //    private DataSrcResult<V> prepareData(final DataSrcResult<V> dataSrcResult) {
    //
    //        final List<SimpleRow<V>> liveData = dataSource.getRows(dataSrcResult.firstRowIndex, dataSrcResult.size);
    //        dataSrcResult.liveData.addAll(liveData);
    //                    try {
    //                        Thread.sleep(2000);
    //                    } catch (final InterruptedException e) {
    //                        e.printStackTrace();
    //                    }
    //        threadCounter++;
    //        return dataSrcResult;
    //    }
    private synchronized DataSrcResult<V> prepareData(DataSrcResult<V> dataSrcResult) {

        //        try {
        //            Thread.sleep(1000);
        //        } catch (final InterruptedException e) {
        //            e.printStackTrace();
        //        }
        dataSrcResult = dataSource.getRows(dataSrcResult);
        return dataSrcResult;
    }
}
