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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnActionListener;
import com.ponysdk.core.ui.datagrid2.column.ColumnController;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.core.util.StringUtils;

/**
 * @author mbagdouri
 */
public class ColumnVisibilitySelectorDataGridView<K, V> extends WidgetDecoratorDataGridView<K, V> {

    private final PWidget layout;
    private final PComplexPanel columnsWidget;
    private final List<ColumnHandler<V>> handlers = new ArrayList<>();

    public ColumnVisibilitySelectorDataGridView(final DataGridView<K, V> view) {
        super(view);
        columnsWidget = createColumnsWidget();

        final PWidget applyWidget = initApplyWidget();

        final PWidget cancelWidget = initCancelWidget();

        final PWidget restoreWidget = initRestoreWidget();

        final PTextBox searchWidget = initSearchWidget();

        layout = createLayout(columnsWidget, applyWidget, cancelWidget, restoreWidget, searchWidget);
    }

    private PTextBox initSearchWidget() {
        final PTextBox searchWidget = createSearchWidget();
        searchWidget.addValueChangeHandler(e -> {
            for (final ColumnHandler<V> handler : handlers) {
                handler.wrapperWidget.setVisible(StringUtils.containsIgnoreCase(handler.column.getId(), e.getData().trim()));
            }
        });
        // only to receive value change events on every key
        searchWidget.addKeyUpHandler(e -> {
            // do nothing
        });
        return searchWidget;
    }

    private PWidget initRestoreWidget() {
        final PWidget restoreWidget = createRestoreWidget();
        restoreWidget.addDomHandler((PClickHandler) e -> {
            clearSelection();
            for (final ColumnHandler<V> handler : handlers) {
                if (!handler.checkBox.isEnabled()) continue;
                final boolean shownByDefault = handler.column.getDefaultState().isShown();
                if (shownByDefault != handler.checkBox.getValue()) {
                    handler.checkBox.setValue(shownByDefault);
                    handler.valueChanged = true;
                }
            }
            onRestore();
        }, PClickEvent.TYPE);
        return restoreWidget;
    }

    private PWidget initCancelWidget() {
        final PWidget cancelWidget = createCancelWidget();
        cancelWidget.addDomHandler((PClickHandler) e -> {
            clearSelection();
            onCancel();
        }, PClickEvent.TYPE);
        return cancelWidget;
    }

    private PWidget initApplyWidget() {
        final PWidget applyWidget = createApplyWidget();
        applyWidget.addDomHandler((PClickHandler) e -> {
            for (final ColumnHandler<V> handler : handlers) {
                if (!handler.valueChanged) continue;
                try {
                    final ColumnController<V> grip = handler.column.getController();
                    if (grip == null) continue;
                    if (handler.checkBox.getValue()) {
                        grip.setState(handler.state.onShow());
                    } else {
                        grip.setState(handler.state.onHide());
                    }
                } finally {
                    handler.valueChanged = false;
                }
            }
            onApply();
        }, PClickEvent.TYPE);
        return applyWidget;
    }

    private void clearSelection() {
        for (final ColumnHandler<V> handler : handlers) {
            if (!handler.valueChanged) continue;
            handler.checkBox.setValue(!handler.checkBox.getValue());
            handler.valueChanged = false;
        }
    }

    protected void onApply() {

    }

    protected void onCancel() {

    }

    protected void onRestore() {

    }

    protected PComplexPanel createColumnSelectorWidget(final ColumnDefinition<V> column) {
        return Element.newLi();
    }

    protected PComplexPanel createColumnsWidget() {
        return Element.newUl();
    }

    protected PWidget createApplyWidget() {
        final PButton button = Element.newPButton();
        button.setEnabledOnRequest(true);
        button.setText("Apply");
        return button;
    }

    protected PWidget createCancelWidget() {
        final PButton button = Element.newPButton();
        button.setEnabledOnRequest(true);
        button.setText("Cancel");
        return button;
    }

    protected PWidget createRestoreWidget() {
        final PButton button = Element.newPButton();
        button.setEnabledOnRequest(true);
        button.setText("Restore default settings");
        return button;
    }

    protected PTextBox createSearchWidget() {
        return Element.newPTextBox();
    }

    protected PWidget createLayout(final PComplexPanel columnsWidget, final PWidget applyWidget, final PWidget cancelWidget,
                                   final PWidget restoreWidget, final PTextBox searchWidget) {
        final PComplexPanel div = Element.newDiv();
        div.add(searchWidget);
        div.add(applyWidget);
        div.add(cancelWidget);
        div.add(restoreWidget);
        div.add(columnsWidget);
        return div;
    }

    @Override
    public final void setAdapter(final DataGridAdapter<K, V> adapter) {
        super.setAdapter(adapter);
        final List<ColumnDefinition<V>> columns = new ArrayList<>(adapter.getColumnDefinitions());
        final Iterator<ColumnDefinition<V>> iterator = columns.iterator();
        while (iterator.hasNext()) {
            final ColumnDefinition<V> column = iterator.next();
            if (!column.isVisibilitySwitchable()) iterator.remove();
        }
        columns.sort(this::compare);
        for (final ColumnDefinition<V> column : columns) {
            final PComplexPanel wrapperWidget = createColumnSelectorWidget(column);
            final ColumnHandler<V> handler = new ColumnHandler<>(wrapperWidget, column);
            handlers.add(handler);
            addColumnActionListener(column, handler);
            columnsWidget.add(wrapperWidget);
        }
    }

    protected int compare(final ColumnDefinition<V> c1, final ColumnDefinition<V> c2) {
        return c1.getId().compareTo(c2.getId());
    }

    private static class ColumnHandler<V> implements ColumnActionListener<V>, PValueChangeHandler<Boolean> {

        private final ColumnDefinition<V> column;
        private final PCheckBox checkBox;
        private final PComplexPanel wrapperWidget;
        private State state;
        private boolean valueChanged = false;

        ColumnHandler(final PComplexPanel wrapperWidget, final ColumnDefinition<V> column) {
            super();
            this.column = column;
            this.wrapperWidget = wrapperWidget;
            this.checkBox = Element.newPCheckBox(column.getId());
            checkBox.addValueChangeHandler(this);
            onStateChanged(column.getDefaultState());
            wrapperWidget.add(checkBox);
        }

        @Override
        public void onSort(final boolean asc) {
        }

        @Override
        public void onClearSort() {
        }

        @Override
        public void onFilter(final Object key, final BiPredicate<V, Supplier<Object>> filter, final boolean reinforcing) {
        }

        @Override
        public void onClearFilter(final Object key) {
        }

        @Override
        public void onClearFilters() {
        }

        @Override
        public void onRedraw(final boolean clearRenderingHelpers) {
        }

        @Override
        public void onValueChange(final PValueChangeEvent<Boolean> event) {
            valueChanged = !valueChanged;
        }

        @Override
        public void onStateChanged(final State state) {
            this.state = state;
            checkBox.setValue(state.isShown());
            checkBox.setEnabled(!state.isPinned());
            valueChanged = false;
        }

        @Override
        public void onResized(final int width) {
        }

    }

    @Override
    public PWidget getDecoratorWidget() {
        return layout;
    }

}