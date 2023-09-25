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

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ponysdk.core.server.service.query.PResultSet;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnActionListener;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.data.DataGridFilter;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.datasource.DataGridSource;

/**
 * @author mbagdouri
 */
public interface DataGridView<K, V> extends IsPWidget {

    /**
     * @return the {@code DataGridModel} corresponding to this view
     */
    DataGridController<K, V> getController();

    /**
     * Enable draws for this {@link DataGridView}.
     */
    void resume();

    /**
     * Disable draws for this {@link DataGridView}.
     */
    void pause();

    /**
     * Sets a {@link DataGridAdapter} on this view
     */
    void setAdapter(DataGridAdapter<K, V> adapter);

    /**
     * @return the {@link DataGridAdapter} corresponding to this view
     */
    DataGridAdapter<K, V> getAdapter();

    /**
     * Sets the delay (in milliseconds) that will be used between consecutive
     * draws of the view.
     * This delay is only respected for draws triggered by an update in the
     * {@link DataGridSource}. For draws that are triggered by a user action,
     * this delay is not respected and the draw is immediate.
     * This delay can be useful for throttling draw instructions sent to the
     * terminal.
     * This delay can be changed multiple times during the life-cycle of this
     * view.
     * The default value of this delay is 0 (no delay).
     */
    void setPollingDelayMillis(long pollingDelayMillis);

    /**
     * Returns an the number of data that can be shown on the screen for a given
     * filter
     */
    int getLiveDataRowCount();

    /**
     * Returns a {@link PResultSet} of the selected data that is
     * shown to the user (i.e. sorting and filters are taken into account).
     */
    public PResultSet<V> getFilteredData();

    /**
     * Returns a {@link PResultSet} of the selected data that is
     * shown to the user (i.e. sorting and filters are taken into account).
     */
    public PResultSet<V> getLiveSelectedData();

    /**
     *
     * @return the number of selected data
     */
    int getLiveSelectedDataCount();

    /**
     * Returns a {@link Collection} view of data with sorting and filters taken into account.
     *
     * @param from
     *            index starting from which the data is retrieved
     * @param dataSize
     *            size of the data to retrieve
     */
    Collection<V> getLiveData(int from, int dataSize);

    /**
     * Unselects all data of the model.
     */
    void unselectAllData();

    /**
     * Selects all unfiltered data of the model.
     */
    void selectAllLiveData();

    /**
     * Adds/replaces a filter that accepts only data that meet the condition of
     * the {@link Predicate} {@code filter}.
     *
     * @param filter the filter to set
     */
    void setFilter(DataGridFilter<V> filter);

    /**
     * Adds/replaces filters that accepts only data that meet the condition of
     * the {@link Predicate} {@code filter}.
     *
     * @param filters the filters collection to set
     */
    void setFilters(Collection<DataGridFilter<V>> filters);

    /**
     * Adds/replaces a sorting criterion for the view.
     *
     * @param key
     *            an object that can be used to uniquely identify a
     *            sorting criterion, so that it can be replaced or
     *            removed
     * @param comparator
     *            compares two values of the model
     */
    void addSort(Object key, Comparator<V> comparator);

    /**
     * Cancels the sorting criterion, corresponding to {@code key}, from the
     * view
     */
    void clearSort(Object key);

    /**
     * Cancels the filter, corresponding to {@code key}, from the view
     */
    void clearFilter(Object key);

    /**
     * Cancels the filters, corresponding to the list of {@code keys}, from the view
     */
    void clearFilters(Collection<Object> keys);

    /**
     * Cancels all filters from the view
     */
    void clearFilters();

    /**
     * Cancels all sort criteria from the view, except for
     * {@link DataGridAdapter#compareDefault(Object, Object)} and insertion
     * order)
     */
    void clearSorts(boolean notify);

    /**
     * Adds/replaces a {@link RowAction}, identified by {@code key}, that will
     * be applied to all rows that meet its condition
     */
    void addRowAction(Object key, RowAction<V> rowAction);

    /**
     * Cancels the {@link RowAction}, corresponding to {@code key}, from the
     * view
     */
    void clearRowAction(Object key);

    /**
     * Add a listener to column related actions for this {@code column}
     */
    void addColumnActionListener(ColumnDefinition<V> column, ColumnActionListener<V> listener);

    /**
     * Remove a listener to column related actions for this {@code column}
     */
    void removeColumnActionListener(ColumnDefinition<V> column, ColumnActionListener<V> listener);

    /**
     * @return the current configuration of the view
     */
    DataGridConfig<V> getConfig();

    /**
     * Sets a new configuration on the view
     */
    void setConfig(DataGridConfig<V> config);

    /**
     * @return a String representation of a custom value of a
     *         {@link DataGridConfig} identified by {@code key}, that will be
     *         used for the serialization of the configuration, or {@code null}
     *         if it doesn't recognize the {@code key} or fails to encode
     */
    String encodeConfigCustomValue(String key, Object value);

    /**
     * @return a deserialized object built from the String value, or
     *         {@code null} if it doesn't recognize the {@code key}
     * @throws DecodeException
     *             if it fails to decode the value
     */
    Object decodeConfigCustomValue(String key, String value) throws DecodeException;

    /**
     * Adds a {@link DrawListener} that will be called after each time, grid
     * draw instructions are sent to the terminal
     */
    void addDrawListener(DrawListener drawListener);

    /**
     * Scrolls up or down to the row at the provided index.
     * Tries to show the row in the center of visible area.
     *
     * @param index
     *            position of the row to scroll to
     */
    void scrollTo(int index);

    /**
     * Removes a {@link DrawListener}
     */
    void removeDrawListener(DrawListener drawListener);

    void setExceptionHandler(Function<Throwable, String> handler);

    public static class DecodeException extends Exception {

        public DecodeException(final Throwable cause) {
            super(cause);
        }

        public DecodeException() {
            super();
        }

        public DecodeException(final String message, final Throwable cause, final boolean enableSuppression,
                final boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        public DecodeException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public DecodeException(final String message) {
            super(message);
        }

    }

    public interface DrawListener {

        void onDraw(int rowCount);
    }

}
