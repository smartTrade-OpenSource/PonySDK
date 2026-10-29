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

package com.ponysdk.core.ui.infinitescroll;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;

public class InfiniteScrollAddonTest extends PSuite {

    private static class ListProvider implements InfiniteScrollProvider<String, PLabel> {

        private final List<String> data = new ArrayList<>();
        private final List<Runnable> handlers = new ArrayList<>();
        private int created;
        private boolean wrongSize;

        ListProvider(final int size) {
            resize(size);
        }

        void resize(final int size) {
            data.clear();
            for (int i = 0; i < size; i++) {
                data.add("item" + i);
            }
        }

        @Override
        public void getData(final int beginIndex, final int maxSize, final Consumer<List<String>> callback) {
            final List<String> page = new ArrayList<>(data.subList(beginIndex, beginIndex + maxSize));
            if (wrongSize) page.remove(0);
            callback.accept(page);
        }

        @Override
        public void getFullSize(final IntConsumer callback) {
            callback.accept(data.size());
        }

        @Override
        public PLabel handleUI(final int index, final String value, final PLabel widget) {
            // Values starting with "new" need a fresh widget
            if (widget == null || value.startsWith("new")) {
                created++;
                return Element.newPLabel(value);
            }
            widget.setText(value);
            return widget;
        }

        @Override
        public void addHandler(final Runnable handler) {
            handlers.add(handler);
        }
    }

    private CapturingModelWriter writer;
    private InfiniteScrollAddon<String, PLabel> addon;

    @AfterEach
    public void tearDown() {
        if (writer != null) writer.uninstall();
        if (addon != null) addon.asWidget().removeFromParent();
    }

    private InfiniteScrollAddon<String, PLabel> newAddon(final ListProvider provider) {
        addon = new InfiniteScrollAddon<>(provider);
        PWindow.getMain().add(addon);
        return addon;
    }

    private PFlowPanel container() {
        return (PFlowPanel) ((PFlowPanel) addon.asWidget()).getWidget(0);
    }

    private List<String> texts() {
        final List<String> texts = new ArrayList<>();
        for (int i = 0; i < container().getWidgetCount(); i++) {
            texts.add(((PLabel) container().getWidget(i)).getText());
        }
        return texts;
    }

    private void scrollTo(final int beginIndex, final int maxVisibleItem) {
        PEmulator.addonData(addon, Json.createObjectBuilder().add("beginIndex", beginIndex).add("maxVisibleItem", maxVisibleItem).build());
    }

    @Test
    public void testFirstRefreshDrawsTheFirstPage() {
        final ListProvider provider = new ListProvider(25);
        newAddon(provider);
        assertTrue(addon.asWidget().hasStyleName("is-viewport"));
        assertTrue(container().hasStyleName("is-container"));

        writer = CapturingModelWriter.install();
        addon.refresh();
        assertEquals(25, addon.getFullSize());
        assertEquals(0, addon.getBeginIndex());
        assertEquals(10, container().getWidgetCount());
        assertEquals("item0", texts().get(0));
        assertTrue(container().getWidget(3).hasStyleName("is-item"));
        assertFalse(addon.asWidget().hasStyleName("is-loading"));

        assertEquals(List.of("beforeDraw", "onDraw", "setSize"), writer.valuesOf(ServerToClientModel.PADDON_METHOD));
        assertArrayEquals(new Object[] { 25 }, (Object[]) writer.valuesOf(ServerToClientModel.PADDON_ARGUMENTS).get(0));

        // Same size: the current window is redrawn, widgets are reused
        writer.clear();
        addon.refresh();
        assertEquals(10, provider.created);
        assertEquals(List.of("beforeDraw", "onDraw"), writer.valuesOf(ServerToClientModel.PADDON_METHOD));
        assertSame(container().getWidget(0), addon.getCurrentItemIndex());
    }

    @Test
    public void testFirstDrawMarksTheCurrentItem() {
        newAddon(new ListProvider(25));
        addon.refresh();
        assertSame(container().getWidget(0), addon.getCurrentItemIndex());
    }

    @Test
    public void testScrollingReusesAndTrimsWidgets() {
        final ListProvider provider = new ListProvider(25);
        newAddon(provider);
        addon.refresh();

        final List<Integer> beginIndexes = new ArrayList<>();
        final InfiniteScrollAddon.InfiniteScrollAddonListener listener = beginIndexes::add;
        addon.addListener(listener);

        scrollTo(20, 10);
        assertEquals(20, addon.getBeginIndex());
        assertEquals(List.of("item20", "item21", "item22", "item23", "item24"), texts());
        assertEquals(10, provider.created);
        assertEquals(List.of(20), beginIndexes);

        addon.removeListener(listener);
        scrollTo(0, 3);
        assertEquals(List.of("item0", "item1", "item2"), texts());
        assertEquals(List.of(20), beginIndexes);
    }

    @Test
    public void testShowIndex() {
        newAddon(new ListProvider(25));
        addon.refresh();
        scrollTo(10, 10);

        writer = CapturingModelWriter.install();
        addon.showIndex(13);
        assertEquals(List.of("showIndex"), writer.valuesOf(ServerToClientModel.PADDON_METHOD));
        assertSame(container().getWidget(3), addon.getCurrentItemIndex());
        assertFalse(container().getWidget(0).hasStyleName("is-item-index"));

        writer.clear();
        addon.showIndex(-1);
        addon.showIndex(25);
        assertTrue(writer.valuesOf(ServerToClientModel.PADDON_METHOD).isEmpty());

        // An index after the visible window marks the last visible item
        addon.showIndex(24);
        assertSame(container().getWidget(9), addon.getCurrentItemIndex());
    }

    @Test
    public void testProviderHandlerForcesARedrawFromTheTop() {
        final ListProvider provider = new ListProvider(25);
        newAddon(provider);
        addon.refresh();
        scrollTo(15, 10);

        provider.resize(30);
        provider.data.set(0, "new0");
        provider.handlers.forEach(Runnable::run);
        assertEquals(30, addon.getFullSize());
        assertEquals(0, addon.getBeginIndex());
        assertEquals("new0", texts().get(0));
        assertEquals(10, container().getWidgetCount());
        assertEquals(11, provider.created);

        // A size change once filled does not move the window
        scrollTo(5, 10);
        provider.resize(40);
        addon.refresh();
        assertEquals(40, addon.getFullSize());
        assertEquals(5, addon.getBeginIndex());

        provider.resize(0);
        addon.refresh();
        assertEquals(0, addon.getFullSize());
    }

    @Test
    public void testEmptyProvider() {
        newAddon(new ListProvider(0));
        addon.refresh();
        assertEquals(0, container().getWidgetCount());
        assertNull(addon.getCurrentItemIndex());
    }

    @Test
    public void testWrongDataSizeIsRejected() {
        final ListProvider provider = new ListProvider(25);
        provider.wrongSize = true;
        newAddon(provider);
        assertThrows(IllegalStateException.class, addon::refresh);
        assertFalse(addon.asWidget().hasStyleName("is-loading"));
    }

    @Test
    public void testTerminalCallsAndWidgetDelegation() {
        newAddon(new ListProvider(5));
        writer = CapturingModelWriter.install();
        addon.scrollToTop();
        addon.refreshWithoutScrollCorrection();
        final List<Object> methods = writer.valuesOf(ServerToClientModel.PADDON_METHOD);
        assertEquals("setScrollTop", methods.get(0));
        assertEquals("prepareUpdate", methods.get(1));

        addon.setVisible(false);
        assertFalse(addon.isVisible());
        assertFalse(addon.asWidget().isVisible());
        addon.setVisible(true);
        assertTrue(addon.isVisible());

        writer.clear();
        addon.setStyleProperty("height", "200px");
        assertEquals(List.of("200px"), writer.valuesAfter(ServerToClientModel.PUT_STYLE_KEY, "height"));

        addon.setMaxItemVisible(2);
        addon.refresh();
        assertEquals(2, container().getWidgetCount());

        addon.addListener(i -> {
        });
        addon.onDestroy();
    }
}
