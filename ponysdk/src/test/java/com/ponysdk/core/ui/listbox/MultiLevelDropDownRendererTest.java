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
package com.ponysdk.core.ui.listbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.dropdown.DropDownTestEvents;
import com.ponysdk.core.ui.dropdown.RecordingModelWriter;
import com.ponysdk.core.ui.listbox.ListBox.GroupListBoxItem;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

public class MultiLevelDropDownRendererTest extends PSuite {

    private final List<MultiLevelDropDownRenderer<String>> renderers = new ArrayList<>();
    private final List<List<String>> events = new ArrayList<>();
    private ListBox<MultiLevelDropDownNode<String>> root;

    @BeforeEach
    public void setUp() {
        RecordingModelWriter.install();
        root = new ListBox<>(MultiLevelDropDownRenderer.getConfiguration(), List.of(leaf("leaf"), node("node", leaf("child"))));
        root.setItemRendererSupplier(() -> {
            final MultiLevelDropDownRenderer<String> renderer = new MultiLevelDropDownRenderer<>(root);
            renderers.add(renderer);
            return renderer;
        });
        root.addValueChangeHandler(e -> events.add(e.getData().stream().map(ListBoxItem::getLabel).collect(Collectors.toList())));
        PWindow.getMain().add(root.asWidget());
    }

    @AfterEach
    public void tearDown() {
        PWindow.getMain().getPRootPanel().clear();
        RecordingModelWriter.uninstall();
    }

    @Test
    public void configurationIsEventOnlyMultilevel() {
        final ListBoxConfiguration configuration = MultiLevelDropDownRenderer.getConfiguration();

        assertTrue(configuration.isEventOnlyEnabled());
        assertTrue(configuration.isMultilevelEnabled());
        assertTrue(configuration.isStopClickEventEnabled());
        assertFalse(configuration.isClearTitleButtonEnabled());
        assertFalse(configuration.isSortingEnabled());
        assertFalse(configuration.isSearchEnabled());
    }

    @Test
    public void leavesAreLabelsAndNodesAreNestedListBoxes() {
        root.open();

        assertEquals(2, renderers.size());
        final PSimplePanel leafPanel = (PSimplePanel) renderers.get(0).asWidget();
        assertTrue(leafPanel.hasStyleName("is-item-wrapper"));
        assertEquals("leaf", ((PLabel) leafPanel.getWidget()).getText());
        assertEquals("leaf", ((PLabel) leafPanel.getWidget()).getTitle());
        assertNull(renderers.get(0).nestedListBox);

        final ListBox<MultiLevelDropDownNode<String>> nested = renderers.get(1).nestedListBox;
        assertSame(nested.asWidget(), ((PSimplePanel) renderers.get(1).asWidget()).getWidget());
        assertEquals("node", nested.getConfiguration().getTitle());
        assertEquals("node", ((PButton) nested.asWidget().getWidget(0)).getText());
        assertEquals(List.of("child"), nested.getItems().stream().map(ListBoxItem::getLabel).collect(Collectors.toList()));
        assertTrue(nested.getContainer().asWidget().hasStyleName("multi-level-dropdown-container"));
        assertEquals(String.valueOf(root.getContainer().asWidget().getID()),
            nested.getContainer().asWidget().getAttribute("multilvl-parent"));
    }

    @Test
    public void clickOnLeafNotifiesAndClosesTheDropDown() {
        root.open();

        PEmulator.click(((PSimplePanel) renderers.get(0).asWidget()).getWidget());

        assertEquals(List.of(List.of("leaf")), events);
        assertFalse(root.isOpen());
    }

    @Test
    public void disabledLeafIsMarked() {
        root.getItems().get(0).setEnabled(false);
        root.open();

        assertTrue(((PSimplePanel) renderers.get(0).asWidget()).getWidget().hasAttribute("disabled"));
    }

    @Test
    public void rightKeyOpensTheNestedLevelAndChildSelectionClosesAllLevels() {
        root.open();
        final PWidget rootKeys = root.getContainer().asWidget();
        assertEquals(TabindexMode.TABULABLE.getTabIndex(), rootKeys.getTabindex());

        // The first item is a leaf: nothing to open
        DropDownTestEvents.keyDown(rootKeys, PKeyCodes.RIGHT);
        assertNull(renderers.get(0).nestedListBox);

        DropDownTestEvents.keyDown(rootKeys, PKeyCodes.DOWN);
        DropDownTestEvents.keyDown(rootKeys, PKeyCodes.RIGHT);
        final ListBox<MultiLevelDropDownNode<String>> nested = renderers.get(1).nestedListBox;
        assertTrue(nested.isOpen());
        // The parent level stops listening to the arrows while the nested level is open
        assertEquals(TabindexMode.FOCUSABLE.getTabIndex(), rootKeys.getTabindex());

        final PComplexPanel nestedRows = (PComplexPanel) ((PComplexPanel) nested.getItemContainer().asWidget()).getWidget(0);
        final PSimplePanel childPanel = (PSimplePanel) ((PComplexPanel) nestedRows.getWidget(0)).getWidget(0);
        PEmulator.click(childPanel.getWidget());

        assertEquals(List.of(List.of("child")), events);
        assertFalse(nested.isOpen());
        assertFalse(root.isOpen());
    }

    @Test
    public void leftKeyClosesTheLevel() {
        root.open();

        DropDownTestEvents.keyDown(root.getContainer().asWidget(), PKeyCodes.LEFT);

        assertFalse(root.isOpen());
    }

    @Test
    public void renderersAreReusedWhenItemsChange() {
        root.open();

        root.replaceItems(List.of(node("other", leaf("x"), leaf("y")), leaf("last")));

        final ListBox<MultiLevelDropDownNode<String>> nested = renderers.get(0).nestedListBox;
        assertSame(nested.asWidget(), ((PSimplePanel) renderers.get(0).asWidget()).getWidget());
        assertEquals("other", nested.getConfiguration().getTitle());
        assertEquals(List.of("x", "y"), nested.getItems().stream().map(ListBoxItem::getLabel).collect(Collectors.toList()));
        assertEquals("last", ((PLabel) ((PSimplePanel) renderers.get(1).asWidget()).getWidget()).getText());
    }

    @Test
    public void nestedLevelWithGroupsIsBuiltInGroupMode() {
        final GroupListBoxItem<MultiLevelDropDownNode<String>> group = GroupListBoxItem.of("G", List.of(leaf("grouped")));
        root.replaceItems(List.of(node("withGroup", group)));

        root.open();

        final ListBox<MultiLevelDropDownNode<String>> nested = renderers.get(0).nestedListBox;
        assertTrue(nested.getConfiguration().isGroupEnabled());
        assertTrue(nested.asWidget().hasStyleName("dd-listbox-group"));
        assertEquals(List.of("G", "grouped"), nested.getItems().stream().map(ListBoxItem::getLabel).collect(Collectors.toList()));
    }

    @Test
    public void customNestedRendererIsUsedForSubLevels() {
        final List<MultiLevelDropDownRenderer<String>> nestedRenderers = new ArrayList<>();
        root.setItemRendererSupplier(() -> {
            final MultiLevelDropDownRenderer<String> renderer = new MultiLevelDropDownRenderer<>(root);
            assertSame(renderer, renderer.setNestedListBoxRenderer((list, parent) -> {
                final MultiLevelDropDownRenderer<String> nestedRenderer = new MultiLevelDropDownRenderer<>(list, parent);
                nestedRenderers.add(nestedRenderer);
                return nestedRenderer;
            }));
            renderers.add(renderer);
            return renderer;
        });
        root.open();

        DropDownTestEvents.keyDown(root.getContainer().asWidget(), PKeyCodes.DOWN);
        DropDownTestEvents.keyDown(root.getContainer().asWidget(), PKeyCodes.RIGHT);

        assertEquals(1, nestedRenderers.size());
        assertEquals("child", ((PLabel) ((PSimplePanel) nestedRenderers.get(0).asWidget()).getWidget()).getText());
    }

    @Test
    public void closeAllWithoutNestedLevelClosesTheParentContainer() {
        final MultiLevelDropDownRenderer<String> renderer = new MultiLevelDropDownRenderer<>(root);
        root.open();

        renderer.openNested();
        renderer.closeAll();

        assertFalse(root.isOpen());
    }

    private static ListBoxItem<MultiLevelDropDownNode<String>> leaf(final String label) {
        return ListBoxItem.of(label, new MultiLevelDropDownNode<String>(label + "-value"));
    }

    @SafeVarargs
    private static ListBoxItem<MultiLevelDropDownNode<String>> node(final String label,
                                                                   final ListBoxItem<MultiLevelDropDownNode<String>>... children) {
        return ListBoxItem.of(label, new MultiLevelDropDownNode<String>(List.of(children)));
    }
}
