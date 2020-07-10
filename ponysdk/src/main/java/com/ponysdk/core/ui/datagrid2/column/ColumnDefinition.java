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

package com.ponysdk.core.ui.datagrid2.column;

import java.util.function.Supplier;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;

/**
 * Used to define and manage a column in a {@link DataGridView}. The same
 * instance of a {@code ColumnDefinition} cannot be used for multiple
 * {@link DataGridView} instances.
 *
 * @author mbagdouri
 */
public interface ColumnDefinition<V> extends ColumnActionListener<V> {

    /**
     * Must always return the same instance.
     *
     * @return the widget that will be used for the header of this column, or
     *         {@code null} if the corresponding
     *         {@link DataGridAdapter#hasHeader()} is {@code false}
     */
    IsPWidget getHeader();

    /**
     * Must always return the same instance.<br/>
     * Returns the same instance as {@link #getHeader()} if the whole header is
     * draggable
     *
     * @return the sub-widget of the header that will be draggable, or
     *         {@code null} if the corresponding
     *         {@link DataGridAdapter#hasHeader()} is {@code false} or this
     *         column is not draggable.
     */
    IsPWidget getDraggableHeaderElement();

    /**
     * Must always return the same instance.
     *
     * @return the widget that will be used for the footer of this column, or
     *         {@code null} if the corresponding
     *         {@link DataGridAdapter#hasFooter()} is {@code false}
     */
    IsPWidget getFooter();

    /**
     * Must always return a new instance but of the same kind and dimensions.
     *
     * @return a new widget that will be used as a cell in the body of the
     *         {@link DataGridView} for this column.
     */
    Cell<V> createCell();

    /**
     * Returns an intermediate object that will be cached in order to be used
     * when rendering {@code data} in a {@link Cell}.<br/>
     * <br/>
     * The returned object will be handed to
     * {@link Cell#render(Object, Object)}.<br/>
     * It can also be retrieved via a {@link Supplier} either in
     * {@link ColumnDefinition#compare(Object, Supplier, Object, Supplier)} to
     * be used for sorting purposes, or inside the {@code BiPredicate} argument
     * of
     * {@link ColumnController#filter(Object, java.util.function.BiPredicate, boolean)}
     * to be used for filtering purposes.<br/>
     * <br/>
     * For example: for a date column, this method can return a formatted
     * {@code String} of the date value, to avoid recalculating the
     * {@code String} each time the value is rendered in a {@link Cell}.<br/>
     * <br/>
     * Another example: for an {@code enum}-based column, whose values are
     * rendered differently depending on the user's language, this method can
     * return an i18n {@code String} value of the {@code enum} value that can be
     * used for rendering, but can also be used for sorting and filtering on
     * this column.<br/>
     * <br/>
     * Can return {@code null} if no intermediate object is necessary.
     */
    Object getRenderingHelper(V data);

    /**
     * Compares {@code v1} and {@code v2} from the perspective of this
     * column.<br/>
     * Rendering helpers corresponding to both values can be retrieved via the
     * {@link Supplier} arguments to be used for the comparison.
     * {@code renderingHelper1} corresponds to {@code v1} while
     * {@code renderingHelper2} corresponds to {@code v2}.<br/>
     * Returns a negative integer, zero, or a positive integer as {@code v1} is
     * less than, equal to, or greater than {@code v2}.<br/>
     * Returns {@code 0} or any other arbitrary value when this column is not
     * sortable.<br/>
     *
     * @see ColumnDefinition#isSortable()
     * @see ColumnDefinition#getRenderingHelper(Object)
     */
    int compare(V v1, Supplier<Object> renderingHelper1, V v2, Supplier<Object> renderingHelper2);

    /**
     * Sets a {@link ColumnController} that can be used to make column related
     * actions. It will be set as soon as the {@link DataGridAdapter} is set on
     * the {@link DataGridView}. This {@link ColumnController} must be made
     * available via the {@link ColumnDefinition#getController()} method.
     */
    void setController(ColumnController<V> columnController);

    /**
     * Returns the {@link ColumnController} that can be used to make column
     * related actions. This {@link ColumnController} will be available
     * immediately after that the {@link DataGridAdapter} is set on the
     * {@link DataGridView}.
     */
    ColumnController<V> getController();

    /**
     * @return the default {@link State} for this column
     */
    State getDefaultState();

    /**
     * @return {@code true} if the visibility of this column can be switched,
     *         {@code false} otherwise
     * @see #getDefaultState()
     */
    boolean isVisibilitySwitchable();

    /**
     * @return {@code true} if this column can be pinned and unpinned,
     *         {@code false} if it must remain in its default pin state
     * @see #getDefaultState()
     */
    boolean isPinSwitchable();

    /**
     * @return {@code true} if data can be filtered based on this column,
     *         {@code false} otherwise
     * @see ColumnController#filter(Object, java.util.function.BiPredicate,
     *      boolean)
     */
    boolean isFilterable();

    /**
     * @return {@code true} if data can be sorted based on this column,
     *         {@code false} otherwise
     * @see #compare(Object, Supplier, Object, Supplier)
     */
    boolean isSortable();

    /**
     * @return {@code true} if the width of this column can be changed,
     *         {@code false} otherwise
     * @see #getDefaultWidth()
     */
    boolean isResizable();

    /**
     * @return an ID that must be unique within the corresponding
     *         {@link DataGridAdapter} and persistent
     */
    String getId();

    /**
     * @return the default width for this column
     */
    int getDefaultWidth();

    /**
     * @return the minimum width for this column
     */
    int getMinWidth();

    /**
     * @return the maximum width for this column
     */
    int getMaxWidth();

    /**
     * The state of a column regarding its visibility and pinning.<br/>
     * <br/>
     * There are only 3 possible states : {@link #UNPINNED_SHOWN},
     * {@link #UNPINNED_HIDDEN} and {@link #PINNED_SHOWN}. A column cannot be
     * pinned and hidden.
     */
    public static enum State {

        UNPINNED_SHOWN {

            @Override
            public State onHide() {
                return UNPINNED_HIDDEN;
            }

            @Override
            public State onShow() {
                return UNPINNED_SHOWN;
            }

            @Override
            public State onPin() {
                return PINNED_SHOWN;
            }

            @Override
            public State onUnpin() {
                return UNPINNED_SHOWN;
            }

            @Override
            public boolean isPinned() {
                return false;
            }

            @Override
            public boolean isShown() {
                return true;
            }
        },
        PINNED_SHOWN {

            @Override
            public State onHide() {
                return PINNED_SHOWN;
            }

            @Override
            public State onShow() {
                return PINNED_SHOWN;
            }

            @Override
            public State onPin() {
                return PINNED_SHOWN;
            }

            @Override
            public State onUnpin() {
                return UNPINNED_SHOWN;
            }

            @Override
            public boolean isPinned() {
                return true;
            }

            @Override
            public boolean isShown() {
                return true;
            }
        },
        UNPINNED_HIDDEN {

            @Override
            public State onHide() {
                return UNPINNED_HIDDEN;
            }

            @Override
            public State onShow() {
                return UNPINNED_SHOWN;
            }

            @Override
            public State onPin() {
                return UNPINNED_HIDDEN;
            }

            @Override
            public State onUnpin() {
                return UNPINNED_HIDDEN;
            }

            @Override
            public boolean isPinned() {
                return false;
            }

            @Override
            public boolean isShown() {
                return false;
            }
        };

        public abstract State onHide();

        public abstract State onShow();

        public abstract State onPin();

        public abstract State onUnpin();

        public abstract boolean isPinned();

        public abstract boolean isShown();
    }
}