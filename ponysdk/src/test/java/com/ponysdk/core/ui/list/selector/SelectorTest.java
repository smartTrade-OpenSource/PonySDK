/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.list.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

public class SelectorTest extends PSuite {

    private static class RecordingSelectorView implements SelectorView {

        private final PWidget widget = Element.newPLabel();
        private final List<SelectorViewListener> listeners = new ArrayList<>();
        private SelectionMode lastMode;
        private int lastSelected = -1;

        @Override
        public PWidget asWidget() {
            return widget;
        }

        @Override
        public void addSelectorViewListener(final SelectorViewListener listener) {
            listeners.add(listener);
        }

        @Override
        public void update(final SelectionMode selectionMode, final int numberOfSelectedItems, final int fullSize, final int pageSize) {
            lastMode = selectionMode;
            lastSelected = numberOfSelectedItems;
        }
    }

    private static SelectorCheckBox<String> newCheckBox(final String data) {
        final SelectorCheckBox<String> checkBox = new SelectorCheckBox<>();
        checkBox.setData(data);
        return checkBox;
    }

    @Test
    public void testSelectionModes() {
        final RecordingSelectorView view = new RecordingSelectorView();
        final Selector<String> selector = new Selector<>(view, 3);
        assertEquals(1, view.listeners.size());
        assertSame(view.widget, selector.getSelectorView());
        selector.setFullSize(10);

        final List<SelectorCheckBox<String>> checkBoxes = Arrays.asList(newCheckBox("a"), newCheckBox("b"), newCheckBox("c"));
        checkBoxes.forEach(selector::registerSelectable);
        assertEquals(SelectionMode.NONE, selector.getSelectionMode());

        selector.onSelect();
        assertEquals(SelectionMode.PARTIAL, selector.getSelectionMode());
        assertEquals(SelectionMode.PARTIAL, view.lastMode);
        assertEquals(1, view.lastSelected);

        selector.onSelectionChange(SelectionMode.PAGE);
        assertEquals(SelectionMode.PAGE, selector.getSelectionMode());
        assertEquals(3, view.lastSelected);
        assertEquals(Arrays.asList("a", "b", "c"), selector.getSelectedData());

        selector.onSelectionChange(SelectionMode.FULL);
        assertEquals(SelectionMode.FULL, selector.getSelectionMode());
        assertEquals(10, view.lastSelected);

        // Unselecting from a full selection falls back to the page size minus one
        selector.onUnselect();
        assertEquals(SelectionMode.PARTIAL, selector.getSelectionMode());
        assertEquals(2, view.lastSelected);

        selector.onSelectionChange(SelectionMode.PARTIAL);
        assertEquals(SelectionMode.PARTIAL, selector.getSelectionMode());

        selector.onSelectionChange(SelectionMode.NONE);
        assertEquals(SelectionMode.NONE, selector.getSelectionMode());
        assertEquals(0, view.lastSelected);
        assertTrue(selector.getSelectedData().isEmpty());
    }

    @Test
    public void testUnselectWithinAPage() {
        final RecordingSelectorView view = new RecordingSelectorView();
        final Selector<String> selector = new Selector<>(view, 3);
        selector.setFullSize(10);
        selector.onSelect();
        selector.onSelect();
        selector.onUnselect();
        assertEquals(SelectionMode.PARTIAL, selector.getSelectionMode());
        assertEquals(1, view.lastSelected);
    }

    @Test
    public void testFullWhenEverythingIsSelected() {
        final RecordingSelectorView view = new RecordingSelectorView();
        final Selector<String> selector = new Selector<>(view);
        selector.setFullSize(2);
        selector.onSelect();
        assertEquals(SelectionMode.PARTIAL, selector.getSelectionMode());
        selector.onSelect();
        assertEquals(SelectionMode.FULL, selector.getSelectionMode());

        selector.setPageSize(1);
        selector.onSelect();
        assertEquals(SelectionMode.FULL, selector.getSelectionMode());

        selector.registerSelectable(newCheckBox("a"));
        selector.reset();
        assertEquals(SelectionMode.NONE, selector.getSelectionMode());
        assertEquals(SelectionMode.NONE, view.lastMode);
        assertEquals(0, view.lastSelected);
        selector.onSelectionChange(SelectionMode.PAGE);
        // The registered selectables were dropped by reset
        assertEquals(SelectionMode.NONE, selector.getSelectionMode());
    }

    @Test
    public void testSelectorCheckBox() {
        final SelectorCheckBox<String> checkBox = newCheckBox("data");
        assertNull(checkBox.getSelectedData());
        checkBox.select();
        assertTrue(checkBox.getValue());
        assertEquals("data", checkBox.getSelectedData());
        checkBox.unselect();
        assertFalse(checkBox.getValue());
        assertNull(checkBox.getSelectedData());

        final int[] counts = new int[2];
        checkBox.addSelectableListener(new SelectableListener() {

            @Override
            public void onSelect() {
                counts[0]++;
            }

            @Override
            public void onUnselect() {
                counts[1]++;
            }
        });
        checkBox.onCheck();
        checkBox.onUncheck();
        checkBox.onUncheck();
        assertEquals(1, counts[0]);
        assertEquals(2, counts[1]);
    }

    @Test
    public void testSelectionResult() {
        final SelectionResult<String> result = new SelectionResult<>(SelectionMode.PAGE, List.of("a"));
        assertEquals(SelectionMode.PAGE, result.getSelectionMode());
        assertEquals(List.of("a"), result.getSelectedData());
    }

    @Test
    public void testCompositeSelectorView() {
        final RecordingSelectorView view1 = new RecordingSelectorView();
        final RecordingSelectorView view2 = new RecordingSelectorView();
        final CompositeSelectorView composite = new CompositeSelectorView(view1, view2);
        assertSame(view1.widget, composite.asWidget());

        final SelectorViewListener listener = mode -> {
        };
        composite.addSelectorViewListener(listener);
        assertEquals(List.of(listener), view1.listeners);
        assertEquals(List.of(listener), view2.listeners);

        composite.update(SelectionMode.PAGE, 4, 10, 4);
        assertEquals(SelectionMode.PAGE, view1.lastMode);
        assertEquals(4, view2.lastSelected);
    }

    @Test
    public void testDefaultInfoSelectorView() {
        final DefaultInfoSelectorView view = new DefaultInfoSelectorView();
        assertSame(view, view.asWidget());
        assertFalse(view.isVisible());

        final List<SelectionMode> requested = new ArrayList<>();
        view.addSelectorViewListener(requested::add);

        view.update(SelectionMode.PAGE, 20, 50, 20);
        assertTrue(view.isVisible());
        assertEquals(2, view.getWidgetCount());
        assertEquals("All 20 items on this page are selected.", ((PLabel) view.getWidget(0)).getText());
        assertEquals("Select all 30 remaining items", ((PAnchor) view.getWidget(1)).getText());
        PEmulator.click(view.getWidget(1));
        assertEquals(List.of(SelectionMode.FULL), requested);

        view.update(SelectionMode.FULL, 50, 50, 20);
        assertTrue(view.isVisible());
        assertEquals("All 50 items are selected.", ((PLabel) view.getWidget(0)).getText());
        assertEquals("Clear selection", ((PAnchor) view.getWidget(1)).getText());
        PEmulator.click(view.getWidget(1));
        assertEquals(List.of(SelectionMode.FULL, SelectionMode.NONE), requested);

        // Everything fits in one page: nothing to clear
        view.update(SelectionMode.FULL, 10, 10, 20);
        assertEquals(1, view.getWidgetCount());

        view.update(SelectionMode.PARTIAL, 3, 10, 20);
        assertFalse(view.isVisible());
        view.update(SelectionMode.PARTIAL, 3, 10, 20);
        assertFalse(view.isVisible());
        view.update(SelectionMode.PAGE, 3, 10, 20);
        view.update(SelectionMode.NONE, 0, 10, 20);
        assertFalse(view.isVisible());
        view.update(SelectionMode.NONE, 0, 10, 20);
        assertFalse(view.isVisible());
    }
}
