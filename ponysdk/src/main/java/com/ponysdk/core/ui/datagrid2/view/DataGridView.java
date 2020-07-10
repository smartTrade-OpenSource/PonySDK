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
import java.util.function.Predicate;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnActionListener;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
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
     * Sets a {@link DataGridAdapter} on this view
     */
    void setAdapter(DataGridAdapter<K, V> adapter);

    /**
     * Sets the delay (in milliseconds) that will be used between consecutive
     * draws of the view.<br/>
     * This delay is only respected for draws triggered by an update in the
     * {@link DataGridSource}. For draws that are triggered by a user action,
     * this delay is not respected and the draw is immediate.<br/>
     * This delay can be useful for throttling draw instructions sent to the
     * terminal.<br/>
     * This delay can be changed multiple times during the life-cycle of this
     * view.<br/>
     * The default value of this delay is 0 (no delay).<br/>
     */
    void setPollingDelayMillis(long pollingDelayMillis);

    /**
     * Returns an the number of data that can be shown on the screen for a given
     * filter
     */
    int getLiveDataRowCount();

    /**
     * Returns an immutable {@link Collection} view of the selected data that is
     * shown to the user (i.e. sorting and filters are taken into account). The
     * collection is backed by the view, so changes to the view are reflected in
     * the collection.
     */
    Collection<V> getLiveSelectedData();

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
     * @param key an object that can be used to uniquely identify a
     *            filter, so that it can be replaced or removed
     * @param filter a predicate that decides whether a value is accepted
     *            or filtered
     * @param reinforcing {@code true} if the predicate is at least as
     *            intolerant as the replaced predicate of the same key
     *            (i.e. the predicate doesn't accept any value that was
     *            not accepted by the replaced predicate), {@code false}
     *            otherwise. This is an optimization that allows us to
     *            avoid applying the predicate on values that we already
     *            know will not be accepted. If this filter is not
     *            replacing an existing one, the value of the
     *            {@code reinforcing} argument has no impact.
     */
    void setFilter(Object key, String id, Predicate<V> filter, boolean reinforcing);

    /**
     * Adds/replaces a sorting criterion for the view.
     *
     * @param key an object that can be used to uniquely identify a
     *            sorting criterion, so that it can be replaced or
     *            removed
     * @param comparator compares two values of the model
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
     * Cancels all filters from the view
     */
    void clearFilters();

    /**
     * Cancels all sort criteria from the view, except for
     * {@link DataGridAdapter#compareDefault(Object, Object)} and insertion
     * order)
     */
    void clearSorts();

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
     * @throws DecodeException if it fails to decode the value
     */
    Object decodeConfigCustomValue(String key, String value) throws DecodeException;

    /**
     * Adds a {@link DrawListener} that will be called after each time, grid
     * draw instructions are sent to the terminal
     */
    void addDrawListener(DrawListener drawListener);

    /**
     * Removes a {@link DrawListener}
     */
    void removeDrawListener(DrawListener drawListener);

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

        void onDraw();
    }
}
