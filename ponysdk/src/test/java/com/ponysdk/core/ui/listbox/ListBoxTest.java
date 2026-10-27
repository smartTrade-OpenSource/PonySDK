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

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.dropdown.DropDownContainerConfiguration.DropDownCloseOnClickMode;
import com.ponysdk.core.ui.dropdown.DropDownTestEvents;
import com.ponysdk.core.ui.dropdown.RecordingModelWriter;
import com.ponysdk.core.ui.listbox.ListBox.GroupListBoxItem;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxDataProvider;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.ReflectionTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.ponysdk.core.ui.listbox.MultiLevelDropDownRenderer.getConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListBoxTest extends PSuite {

    private final static String FIELD_CONFIGURATION = "configuration";
    private final static String METHOD_IS_SELECTION_ALLOWED = "isSelectionAllowed";
    private final static String ITEM1 = "item1";
    private final static String ITEM2 = "item2";

    private ListBoxConfiguration configuration = getConfiguration();
    private final ListBox<String> listBox = new ListBox<>(configuration);
    private RecordingModelWriter writer;

    @BeforeEach
    public void setUp() {
        writer = RecordingModelWriter.install();
    }

    @AfterEach
    public void tearDown() {
        configuration = getConfiguration();
        PWindow.getMain().getPRootPanel().clear();
        RecordingModelWriter.uninstall();
    }

    @Test
    public void shouldAllowSelection_WhenLimitNotReached() throws Exception {
        // GIVEN
        configuration.enableMultiSelection();
        configuration.setSelectionLimit(5);
        Collection<String> items = Arrays.asList(ITEM1, ITEM2);
        setConfigurationInListBox(listBox, FIELD_CONFIGURATION, configuration);
        // WHEN
        boolean result = invokeIsSelectionAllowed(items);
        // THEN
        assertTrue(result);
    }

    @Test
    public void shouldDenySelection_WhenLimitReachedAndSelectedItemsNull() throws Exception {
        // GIVEN
        configuration.enableMultiSelection();
        configuration.setSelectionLimit(2);
        List<ListBoxItem<String>> items = List.of(ListBoxItem.of(ITEM1, ITEM1, true), ListBoxItem.of(ITEM2, ITEM2, true));
        ReflectionTestUtil.setField(listBox, "items", items);
        setConfigurationInListBox(listBox, FIELD_CONFIGURATION, configuration);
        // WHEN
        boolean result = invokeIsSelectionAllowed(null);
        // THEN
        assertFalse(result);
    }

    @Test
    public void shouldAllowSelection_WhenLimitReachedAndSelectedItemsNull() throws Exception {
        // GIVEN
        configuration.enableMultiSelection();
        configuration.setSelectionLimit(2);
        List<ListBoxItem<String>> items = List.of(ListBoxItem.of(ITEM1, ITEM1, false), ListBoxItem.of(ITEM2, ITEM2, true));
        ReflectionTestUtil.setField(listBox, "items", items);
        setConfigurationInListBox(listBox, FIELD_CONFIGURATION, configuration);
        // WHEN
        boolean result = invokeIsSelectionAllowed(null);
        // THEN
        assertTrue(result);
    }

    @Test
    public void shouldAllowSelection_WhenMultiSelectionDisabled() throws Exception {
        // GIVEN
        ReflectionTestUtil.setField(configuration, "multiSelectionEnabled", false);
        configuration.setSelectionLimit(1);
        setConfigurationInListBox(listBox, FIELD_CONFIGURATION, configuration);
        Collection<String> items = Arrays.asList(ITEM1, ITEM2);
        // WHEN
        boolean result = invokeIsSelectionAllowed(items);
        // THEN
        assertTrue(result);
    }

    private boolean invokeIsSelectionAllowed(Collection<?> items) throws Exception {
        Method method = ListBox.class.getDeclaredMethod(METHOD_IS_SELECTION_ALLOWED, Collection.class);
        method.setAccessible(true);
        return (boolean) method.invoke(listBox, items);
    }

    private void setConfigurationInListBox(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Construction and item management

    @Test
    public void constructorRejectsIncompatibleModes() {
        assertThrows(IllegalStateException.class,
            () -> new ListBox<String>(new ListBoxConfiguration().enableMultiSelection().enableGroup()));
        final ListBoxConfiguration eventOnly = new ListBoxConfiguration().enableMultiSelection();
        eventOnly.enabledEventOnly();
        assertThrows(IllegalStateException.class, () -> new ListBox<String>(eventOnly));
    }

    @Test
    public void itemsAreExposedReadOnly() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("b", "a"));

        assertEquals(2, box.getSize());
        assertEquals(List.of("b", "a"), labels(box.getItems()));
        assertThrows(UnsupportedOperationException.class, () -> box.getItems().add(ListBoxItem.of("c")));
    }

    @Test
    public void addItemsSortsAndRemoveItemClearsSelection() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("c", "a"));

        box.addItem(ListBoxItem.of("d"));
        assertEquals(List.of("c", "a", "d"), labels(box.getItems()));
        box.addItems(List.of(ListBoxItem.of("b")));
        assertEquals(List.of("a", "b", "c", "d"), labels(box.getItems()));

        box.setSelected("a");
        box.removeItem("a");
        assertEquals(List.of("b", "c", "d"), labels(box.getItems()));
        assertNull(box.getSelectedItem());

        box.setSelected("b");
        box.removeItem("c");
        assertEquals("b", box.getSelectedData());

        box.removeItem("missing");
        assertEquals(2, box.getSize());
    }

    @Test
    public void removeItemInMultiSelectionKeepsOtherSelections() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), items("a", "b", "c"));
        box.setMultiSelected(List.of("a", "b"));

        box.removeItem("a");

        assertEquals(List.of("b"), box.getSelectedDataList());
    }

    @Test
    public void renameItemUpdatesLabelAndTitle() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), items("a", "b")));
        box.setSelected("a");

        box.renameItem("a", "alpha");
        box.renameItem("missing", "other");

        assertEquals(List.of("alpha", "b"), labels(box.getItems()));
        assertEquals("alpha", mainButton(box).getText());
        assertThrows(IllegalCallerException.class,
            () -> new ListBox<String>(new ListBoxConfiguration().enableGroup()).renameItem("a", "b"));
    }

    @Test
    public void clearRemovesItemsAndSelection() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().setTitle("Title"), items("a", "b")));
        box.setSelected("a");
        assertEquals("a", mainButton(box).getText());

        box.clear();

        assertEquals(0, box.getSize());
        assertNull(box.getSelectedData());
        assertEquals("Title", mainButton(box).getText());
    }

    @Test
    public void replaceItemsResetsSelection() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("a", "b"));
        box.setSelected("a");

        box.replaceItems(List.of(ListBoxItem.of("x"), ListBoxItem.of("y", true)));

        assertEquals(List.of("x", "y"), labels(box.getItems()));
        assertEquals(List.of("y"), box.getSelectedDataList());
    }

    @Test
    public void setDataEnabled() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("a", "b"));

        box.setDataEnabled("a", false);
        box.setDataEnabled("missing", false);

        assertFalse(item(box, "a").isEnabled());
        assertTrue(item(box, "b").isEnabled());
        assertThrows(NullPointerException.class, () -> box.setDataEnabled(null, true));
    }

    // Selection without data provider

    @Test
    public void singleSelection() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("b", "a", "c"));
        assertNull(box.getSelectedData());
        assertNull(box.getSelectedItem());
        assertTrue(box.getSelectedItems().isEmpty());

        box.setSelected("a");
        assertEquals("a", box.getSelectedData());
        assertEquals("a", box.getSelectedItem().getLabel());
        assertEquals(List.of("a"), box.getSelectedDataList());
        assertEquals(List.of("a"), labels(box.getValue()));

        box.setSelected("b");
        assertEquals(List.of("b"), box.getSelectedDataList());

        box.setSelected(null);
        assertTrue(box.getSelectedDataList().isEmpty());

        box.setValue(List.of(item(box, "c")));
        assertEquals(List.of("c"), box.getSelectedDataList());
        box.setValue(null);
        assertTrue(box.getSelectedDataList().isEmpty());

        box.addToSelection("b");
        assertEquals(List.of("b"), box.getSelectedDataList());
    }

    @Test
    public void multiSelection() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), items("a", "b", "c", "d"));

        box.setMultiSelected(List.of("a", "c"));
        assertEquals(List.of("a", "c"), box.getSelectedDataList());
        assertEquals("a", box.getSelectedData());

        box.addToSelection("d");
        assertEquals(List.of("a", "c", "d"), box.getSelectedDataList());

        box.removeFromSelection("a");
        assertEquals(List.of("c", "d"), box.getSelectedDataList());

        box.addToSelection(null);
        box.removeFromSelection(null);
        assertEquals(List.of("c", "d"), box.getSelectedDataList());

        box.setMultiSelected(null);
        assertTrue(box.getSelectedDataList().isEmpty());
        box.setMultiSelected(List.of("b"));
        box.setMultiSelected(List.of());
        assertTrue(box.getSelectedDataList().isEmpty());
    }

    @Test
    public void multiSelectionRespectsSelectionLimit() {
        final ListBoxConfiguration limited = new ListBoxConfiguration().enableMultiSelection().setSelectionLimit(2);
        final ListBox<String> box = new ListBox<>(limited, items("a", "b", "c"));

        box.setMultiSelected(List.of("a", "b", "c"));
        assertTrue(box.getSelectedDataList().isEmpty());

        box.setMultiSelected(List.of("a", "b"));
        box.addToSelection("c");
        assertEquals(List.of("a", "b"), box.getSelectedDataList());
    }

    @Test
    public void setSelectedIndexFollowsSortedOrder() {
        final ListBox<String> single = new ListBox<>(new ListBoxConfiguration(), items("b", "a", "c"));
        single.setSelectedIndex(0);
        assertEquals("a", single.getSelectedData());
        single.setSelectedIndex(10);
        assertNull(single.getSelectedData());

        final ListBox<String> multi = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), items("b", "a", "c"));
        multi.setMultiSelected(List.of("a"));
        multi.setSelectedIndex(2);
        assertEquals(List.of("c"), multi.getSelectedDataList());
    }

    @Test
    public void selectionModeGuards() {
        final ListBox<String> single = new ListBox<>(new ListBoxConfiguration(), items("a"));
        assertThrows(IllegalArgumentException.class, () -> single.setMultiSelected(List.of("a")));
        assertThrows(IllegalArgumentException.class, () -> single.removeFromSelection("a"));
        assertThrows(IllegalArgumentException.class, () -> single.setSelectedId("a"));
        assertThrows(IllegalArgumentException.class, () -> single.setSelectedIds(List.of("a")));

        final ListBox<String> multi = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), items("a"));
        assertThrows(IllegalArgumentException.class, () -> multi.setSelected("a"));

        final ListBoxConfiguration eventOnlyConfiguration = new ListBoxConfiguration();
        eventOnlyConfiguration.enabledEventOnly();
        final ListBox<String> eventOnly = new ListBox<>(eventOnlyConfiguration, items("a"));
        assertThrows(UnsupportedOperationException.class, eventOnly::getValue);
        assertThrows(UnsupportedOperationException.class, () -> eventOnly.setValue(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> eventOnly.setSelected("a"));
        assertThrows(UnsupportedOperationException.class, () -> eventOnly.addToSelection("a"));
    }

    // Sorting

    @Test
    public void comparatorOrdersByLabelThenData() {
        final ListBox<Integer> box = new ListBox<>(new ListBoxConfiguration());
        final Comparator<ListBoxItem<Integer>> comparator = box.getComparator();
        final ListBoxItem<Integer> noData = ListBoxItem.of("x", null);

        assertTrue(comparator.compare(noData, ListBoxItem.of("y", 1)) < 0);
        assertTrue(comparator.compare(ListBoxItem.of("y", 1), noData) > 0);
        assertTrue(comparator.compare(ListBoxItem.of("B", 1), ListBoxItem.of("a", 2)) > 0);
        assertEquals(0, comparator.compare(ListBoxItem.of(null, 1), ListBoxItem.of(null, 2)));
        assertTrue(comparator.compare(ListBoxItem.of(null, 1), ListBoxItem.of("a", 2)) < 0);
        assertTrue(comparator.compare(ListBoxItem.of("a", 1), ListBoxItem.of(null, 2)) > 0);
    }

    @Test
    public void dataComparatorTakesPrecedenceAndSortsItems() {
        final ListBox<Integer> box = new ListBox<>(new ListBoxConfiguration(),
            List.of(ListBoxItem.of("one", 1), ListBoxItem.of("two", 2), ListBoxItem.of("three", 3)));

        box.setComparator(Comparator.reverseOrder());
        assertEquals(List.of("three", "two", "one"), labels(box.getItems()));

        box.setComparator((d1, d2) -> {
            throw new IllegalStateException("broken comparator");
        });
        assertEquals(0, box.getComparator().compare(ListBoxItem.of("a", 1), ListBoxItem.of("b", 2)));
    }

    @Test
    public void disabledSortingKeepsInsertionOrder() {
        final ListBox<Integer> box = new ListBox<>(new ListBoxConfiguration().disableSorting(),
            List.of(ListBoxItem.of("one", 1), ListBoxItem.of("two", 2)));

        box.setComparator(Comparator.reverseOrder());

        assertEquals(List.of("one", "two"), labels(box.getItems()));
    }

    @Test
    public void groupComparator() {
        final ListBox<Integer> box = new ListBox<>(new ListBoxConfiguration());
        final ListBoxItem<Integer> second = ListBoxItem.of("a", 1);
        second.setGroupName("G2");
        final ListBoxItem<Integer> first = ListBoxItem.of("b", 2);
        first.setGroupName("g1");

        assertTrue(box.getComparator().compare(second, first) > 0);
        assertTrue(box.getComparator().compare(second, ListBoxItem.of("c", 3)) < 0);

        box.setGroupComparator((i1, i2) -> -i1.getGroupName().compareToIgnoreCase(i2.getGroupName()));
        assertTrue(box.getComparator().compare(second, first) < 0);
    }

    // Groups

    @Test
    public void groupModeFlattensGroups() {
        final ListBox<String> box = groupListBox();

        assertEquals(List.of("Fruits", "pear", "apple", "Vegetables", "leek"), labels(box.getItems()));
        assertEquals("Fruits", item(box, "apple").getGroupName());

        box.setSelectedIndex(0);
        assertEquals("apple", box.getSelectedData());
    }

    @Test
    public void groupItemsCanBeAddedRenamedAndRemoved() {
        final ListBox<String> box = groupListBox();

        box.addItemInGroup("Vegetables", ListBoxItem.of("carrot"));
        box.addItemInGroup("Unknown", ListBoxItem.of("ignored"));
        assertEquals("Vegetables", item(box, "carrot").getGroupName());
        assertEquals(6, box.getSize());

        box.renameItemInGroup("Vegetables", "leek", "Leek");
        box.renameItemInGroup("Vegetables", "missing", "other");
        box.renameItemInGroup("Unknown", "leek", "other");
        assertEquals("leek", item(box, "Leek").getData());

        box.setSelected("apple");
        box.removeItemFromGroup("Fruits", "apple");
        box.removeItemFromGroup("Fruits", "missing");
        box.removeItemFromGroup("Unknown", "pear");
        assertNull(box.getSelectedData());
        assertEquals(List.of("Fruits", "pear", "Vegetables", "Leek", "carrot"), labels(box.getItems()));

        box.addItem(GroupListBoxItem.of("Herbs", items("basil")));
        box.addItems(List.of(GroupListBoxItem.of("Nuts", items("pecan")), ListBoxItem.of("ignored")));
        assertTrue(labels(box.getItems()).containsAll(List.of("Herbs", "basil", "Nuts", "pecan")));
        assertFalse(labels(box.getItems()).contains("ignored"));

        box.setSelected("leek");
        box.removeGroup("Vegetables");
        box.removeGroup("Unknown");
        assertNull(box.getSelectedData());
        assertFalse(labels(box.getItems()).contains("Leek"));
        assertFalse(labels(box.getItems()).contains("Vegetables"));
    }

    @Test
    public void groupOperationsRequireGroupMode() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("a"));

        assertThrows(IllegalCallerException.class, () -> box.addItemInGroup("g", ListBoxItem.of("b")));
        assertThrows(IllegalCallerException.class, () -> box.removeItemFromGroup("g", "a"));
        assertThrows(IllegalCallerException.class, () -> box.removeGroup("g"));
        assertThrows(IllegalCallerException.class, () -> box.renameItemInGroup("g", "a", "b"));
    }

    @Test
    public void groupItem() {
        final GroupListBoxItem<String> group = GroupListBoxItem.of("G", items("a"));
        group.addItem(ListBoxItem.of("b"));

        assertEquals(ListBoxItem.ListBoxItemType.GROUP, group.getType());
        assertNull(group.getData());
        assertEquals("G", group.getGroupItems().get(1).getGroupName());
        assertEquals("GroupListBoxItem [groupName=G]", group.toString());
    }

    @Test
    public void listBoxItem() {
        final ListBoxItem<Integer> item = ListBoxItem.of("one", 1, true);

        assertEquals(ListBoxItem.ListBoxItemType.ITEM, item.getType());
        assertEquals("ListBoxItem [label=one, data=1, selected=true]", item.toString());
        assertEquals(ListBoxItem.of("one", 2), item);
        assertEquals(ListBoxItem.of("one", 2).hashCode(), item.hashCode());
        assertFalse(item.equals(ListBoxItem.of("two", 1)));
        assertFalse(item.equals(null));
        assertFalse(item.equals("one"));
        assertTrue(item.equals(item));

        final ListBoxItem<Integer> grouped = ListBoxItem.of("one", 1);
        grouped.setGroupName("g");
        assertFalse(item.equals(grouped));
        assertFalse(grouped.equals(item));
        assertFalse(grouped.equals(ListBoxItem.of("one", 1)));
        assertFalse(ListBoxItem.of(null, 1).equals(item));
        assertTrue(ListBoxItem.of(null, 1).equals(ListBoxItem.of(null, 2)));

        assertTrue(ListBoxItem.of("s", true).isSelected());
        assertEquals("s", ListBoxItem.of("s").getData());
        assertEquals(List.of("#1", "#2"), labels(ListBoxItem.of(List.of(1, 2), i -> "#" + i)));
    }

    // Data provider

    @Test
    public void dataProviderSingleSelection() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), new ListDataProvider("a", "b", "c"));
        assertEquals(3, box.getSize());
        assertNull(box.getSelectedData());
        assertNull(box.getSelectedItem());
        assertTrue(box.getSelectedDataList().isEmpty());

        box.setSelected("b");
        assertEquals("b", box.getSelectedData());
        assertEquals("b", box.getSelectedItem().getLabel());
        assertEquals(List.of("b"), labels(box.getSelectedItems()));
        assertEquals(List.of("b"), box.getSelectedDataList());

        box.setSelected("unknown");
        assertNull(box.getSelectedData());

        box.setSelectedId("c");
        assertEquals("c", box.getSelectedData());
        box.setSelectedId(null);
        assertNull(box.getSelectedData());

        assertThrows(IllegalArgumentException.class, () -> box.setSelectedIds(List.of("a")));
    }

    @Test
    public void dataProviderMultiSelection() {
        final ListBoxConfiguration multi = new ListBoxConfiguration().enableMultiSelection().setSelectionLimit(2);
        final ListBox<String> box = new ListBox<>(multi, new ListDataProvider("a", "b", "c"));

        box.setSelectedIds(List.of("a", "c"));
        assertEquals(List.of("a", "c"), box.getSelectedDataList());
        box.setSelectedIds(List.of("a", "b", "c"));
        assertTrue(box.getSelectedDataList().isEmpty());
        box.setSelectedIds(List.of("b"));
        box.setSelectedIds(null);
        assertTrue(box.getSelectedDataList().isEmpty());

        box.setMultiSelected(List.of("b", "unknown"));
        assertEquals(List.of("b"), box.getSelectedDataList());
        box.setMultiSelected(List.of("a", "b", "c"));
        assertTrue(box.getSelectedDataList().isEmpty());
        box.setMultiSelected(null);
        assertTrue(box.getSelectedDataList().isEmpty());

        assertThrows(IllegalArgumentException.class, () -> box.setSelectedId("a"));
    }

    @Test
    public void dataProviderRejectsLocalItemOperations() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), new ListDataProvider("a"));

        assertThrows(IllegalCallerException.class, () -> box.addItem(ListBoxItem.of("b")));
        assertThrows(IllegalCallerException.class, () -> box.addItems(List.of(ListBoxItem.of("b"))));
        assertThrows(IllegalCallerException.class, () -> box.addItemInGroup("g", ListBoxItem.of("b")));
        assertThrows(IllegalCallerException.class, () -> box.removeItem("a"));
        assertThrows(IllegalCallerException.class, () -> box.removeItemFromGroup("g", "a"));
        assertThrows(IllegalCallerException.class, () -> box.removeGroup("g"));
        assertThrows(IllegalCallerException.class, () -> box.renameItem("a", "b"));
        assertThrows(IllegalCallerException.class, () -> box.renameItemInGroup("g", "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> box.addToSelection("a"));
        assertThrows(IllegalArgumentException.class, () -> box.replaceItems(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> box.setDataEnabled("a", false));

        final ListBox<String> multi = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), new ListDataProvider("a"));
        assertThrows(IllegalArgumentException.class, () -> multi.removeFromSelection("a"));
    }

    @Test
    public void dataProviderSetSelectedIndex() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), new ListDataProvider("a", "b", "c"));

        box.setSelectedIndex(1);

        assertEquals("b", box.getSelectedData());
    }

    @Test
    public void dataProviderRendersAndSelectsFromProvider() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), new ListDataProvider("c", "a", "b")));

        box.open();
        assertEquals(List.of("c", "a", "b"), renderedLabels(box));

        clickItem(box, "a");
        assertEquals("a", box.getSelectedData());
        box.open();
        assertTrue(row(box, "a").hasStyleName("dd-listbox-item-selected"));
        assertFalse(row(box, "c").hasStyleName("dd-listbox-item-selected"));
    }

    @Test
    public void dataProviderMultiSelectionByClick() {
        final ListBox<String> box = attach(
            new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), new ListDataProvider("a", "b", "c")));
        box.open();

        clickItem(box, "a");
        clickItem(box, "b");
        assertEquals(List.of("a", "b"), box.getSelectedDataList());
        clickItem(box, "a");
        assertEquals(List.of("b"), box.getSelectedDataList());

        box.close();
        box.open();
        assertTrue(checkBox(box, "b").getValue());
        assertFalse(checkBox(box, "a").getValue());
    }

    @Test
    public void dataProviderReceivesTheFilter() {
        final TextListBoxFilterWidget filter = new TextListBoxFilterWidget();
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), new ListDataProvider("ab", "b", "c"));
        box.setFilterWidgetSupplier(() -> filter);
        attach(box);
        box.open();

        filter.setFilter("b");
        DropDownTestEvents.keyUp(filter.asWidget(), 66);

        assertEquals(List.of("ab", "b"), renderedLabels(box));
    }

    // Rendering and interaction

    @Test
    public void openRendersSortedItems() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().setTitle("Fruits"), items("pear", "apple", "fig")));
        assertEquals("Fruits", mainButton(box).getText());
        assertTrue(box.asWidget().hasStyleName("dd-listbox"));
        assertTrue(box.getContainer().asWidget().hasStyleName("dd-listbox-container"));

        box.open();

        assertEquals(List.of("apple", "fig", "pear"), renderedLabels(box));
        assertEquals("apple", currentLabel(box));
    }

    @Test
    public void clickSelectsItemClosesAndNotifies() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().setTitle("Fruits"), items("pear", "apple", "fig")));
        final List<List<String>> events = new ArrayList<>();
        box.addValueChangeHandler(e -> events.add(labels(e.getData())));
        box.open();

        clickItem(box, "fig");

        assertEquals("fig", box.getSelectedData());
        assertFalse(box.isOpen());
        assertEquals(List.of(List.of("fig")), events);
        assertEquals("fig", mainButton(box).getText());
        assertEquals("fig", mainButton(box).getTitle());
        assertTrue(box.asWidget().hasStyleName("dd-container-selected"));
        assertTrue(clearButton(box).isVisible());

        box.open();
        assertTrue(row(box, "fig").hasStyleName("dd-listbox-item-selected"));
        assertFalse(row(box, "apple").hasStyleName("dd-listbox-item-selected"));

        PEmulator.click(clearButton(box));
        assertNull(box.getSelectedData());
        assertEquals(List.of(List.of("fig"), List.of()), events);
        assertEquals("Fruits", mainButton(box).getText());
        assertFalse(clearButton(box).isVisible());
    }

    @Test
    public void disabledItemCannotBeClicked() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), items("a", "b")));
        box.setDataEnabled("a", false);
        box.open();

        assertTrue(itemLabel(box, "a").hasAttribute("disabled"));
        clickItem(box, "a");

        assertNull(box.getSelectedData());
        assertTrue(box.isOpen());
    }

    @Test
    public void closeOnClickMode() {
        final ListBox<String> keepOpen = attach(
            new ListBox<>(new ListBoxConfiguration().setCloseOnClickMode(DropDownCloseOnClickMode.FALSE), items("a", "b")));
        keepOpen.open();
        clickItem(keepOpen, "a");
        assertTrue(keepOpen.isOpen());
        assertEquals("a", keepOpen.getSelectedData());

        final ListBox<String> close = attach(new ListBox<>(
            new ListBoxConfiguration().enableMultiSelection().setCloseOnClickMode(DropDownCloseOnClickMode.TRUE), items("a", "b")));
        close.open();
        clickItem(close, "a");
        assertFalse(close.isOpen());
    }

    @Test
    public void multiSelectionByClick() {
        final ListBoxConfiguration multi = new ListBoxConfiguration().enableMultiSelection().enableMultiSelectionClear("Clear");
        final ListBox<String> box = attach(new ListBox<>(multi, items("a", "b", "c")));
        final List<List<String>> events = new ArrayList<>();
        box.addValueChangeHandler(e -> events.add(labels(e.getData())));
        assertTrue(box.asWidget().hasStyleName("dd-listbox-multi"));
        box.open();
        final PButton clear = clearMultiButton(box);
        assertEquals("Clear", clear.getText());
        assertFalse(clear.isEnabled());

        clickItem(box, "a");
        clickItem(box, "c");
        assertTrue(box.isOpen());
        assertEquals(List.of("a", "c"), box.getSelectedDataList());
        assertTrue(checkBox(box, "a").getValue());
        assertEquals("a, c", mainButton(box).getText());
        assertTrue(clear.isEnabled());

        clickItem(box, "a");
        assertEquals(List.of("c"), box.getSelectedDataList());
        assertFalse(checkBox(box, "a").getValue());

        PEmulator.click(clear);
        assertTrue(box.getSelectedDataList().isEmpty());
        assertFalse(clear.isEnabled());
        assertEquals(List.of(List.of("a"), List.of("a", "c"), List.of("c"), List.of()), events);
    }

    @Test
    public void selectionLimitBlocksClickOnNewItem() {
        final ListBox<String> box = attach(
            new ListBox<>(new ListBoxConfiguration().enableMultiSelection().setSelectionLimit(1), items("a", "b")));
        box.open();

        clickItem(box, "a");
        clickItem(box, "b");

        assertEquals(List.of("a"), box.getSelectedDataList());
        assertFalse(checkBox(box, "b").getValue());
    }

    @Test
    public void selectedItemsAreSortedFirst() {
        final ListBox<String> sorted = attach(new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), items("a", "b", "c")));
        sorted.setMultiSelected(List.of("c"));
        sorted.open();
        assertEquals(List.of("c", "a", "b"), renderedLabels(sorted));
        assertTrue(row(sorted, "c").hasStyleName("dd-listbox-item-last-selected"));
        assertFalse(row(sorted, "a").hasStyleName("dd-listbox-item-last-selected"));

        final ListBox<String> unsorted = attach(new ListBox<>(
            new ListBoxConfiguration().enableMultiSelection().disableSelectionSortingEnabled(), items("a", "b", "c")));
        unsorted.setMultiSelected(List.of("c"));
        unsorted.open();
        assertEquals(List.of("a", "b", "c"), renderedLabels(unsorted));
    }

    @Test
    public void titleWithSeparatorAndSelectionCount() {
        final ListBoxConfiguration multi = new ListBoxConfiguration().enableMultiSelection().setTitle("T").disableTitlePlaceHolder()
            .setDisplaySelectionLimit(1).setDisplaySelectionLabel("selected");
        final ListBox<String> box = attach(new ListBox<>(multi, items("a", "b")));
        assertEquals("T : ", mainButton(box).getText());

        multi.setAllLabel("All");
        box.setMultiSelected(List.of());
        assertEquals("T : All", mainButton(box).getText());

        box.setMultiSelected(List.of("a", "b"));
        assertEquals("T : 2 selected", mainButton(box).getText());
        assertEquals("a, b", mainButton(box).getTitle());

        box.setMultiSelected(List.of("a"));
        assertEquals("T : a", mainButton(box).getText());
    }

    @Test
    public void titleWithoutSelectionDisplayed() {
        final ListBox<String> box = attach(
            new ListBox<>(new ListBoxConfiguration().setTitle("T").setSelectionDisplayed(false), items("a")));

        box.setSelected("a");

        assertEquals("", mainButton(box).getText());
        assertEquals("", mainButton(box).getTitle());
        assertTrue(box.asWidget().hasStyleName("dd-container-selected"));
    }

    @Test
    public void addToSelectionInSingleModeShowsItemOnce() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), items("a", "b")));

        box.addToSelection("a");

        assertEquals("a", mainButton(box).getText());
    }

    @Test
    public void eventOnlyClickNotifiesWithoutSelecting() {
        final ListBoxConfiguration eventOnly = new ListBoxConfiguration();
        eventOnly.enabledEventOnly();
        final ListBox<String> box = attach(new ListBox<>(eventOnly, items("a", "b")));
        final List<List<String>> events = new ArrayList<>();
        box.addValueChangeHandler(e -> events.add(labels(e.getData())));
        box.open();

        clickItem(box, "b");

        assertEquals(List.of(List.of("b")), events);
        assertTrue(box.getSelectedItems().isEmpty());
        assertFalse(box.isOpen());
    }

    @Test
    public void keyboardNavigationSkipsDisabledItems() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().disableSearch(), items("a", "b", "c", "d")));
        box.setDataEnabled("c", false);
        box.open();
        final PWidget keys = box.getContainer().asWidget();
        assertEquals(TabindexMode.TABULABLE.getTabIndex(), keys.getTabindex());
        assertTrue(writer.addonMethods(box.getContainer()).contains("disableSpaceWhenOpened"));
        assertEquals("a", currentLabel(box));

        DropDownTestEvents.keyDown(keys, PKeyCodes.DOWN);
        assertEquals("b", currentLabel(box));
        DropDownTestEvents.keyDown(keys, PKeyCodes.DOWN);
        assertEquals("d", currentLabel(box));
        DropDownTestEvents.keyDown(keys, PKeyCodes.DOWN);
        assertEquals("d", currentLabel(box));
        DropDownTestEvents.keyDown(keys, PKeyCodes.UP);
        assertEquals("b", currentLabel(box));
        DropDownTestEvents.keyDown(keys, PKeyCodes.UP);
        DropDownTestEvents.keyDown(keys, PKeyCodes.UP);
        assertEquals("a", currentLabel(box));

        DropDownTestEvents.keyDown(keys, PKeyCodes.RIGHT);
        DropDownTestEvents.keyDown(keys, PKeyCodes.LEFT);
        DropDownTestEvents.keyDown(keys, PKeyCodes.HOME);
        assertTrue(box.isOpen());
        assertEquals("a", currentLabel(box));

        DropDownTestEvents.keyDown(keys, PKeyCodes.DOWN);
        DropDownTestEvents.keyDown(keys, PKeyCodes.ENTER);
        assertEquals("b", box.getSelectedData());
        assertFalse(box.isOpen());
        assertEquals(TabindexMode.FOCUSABLE.getTabIndex(), keys.getTabindex());

        box.open();
        writer.clear();
        DropDownTestEvents.keyDown(keys, PKeyCodes.ESCAPE);
        assertFalse(box.isOpen());
        assertEquals(List.of(Boolean.TRUE), writer.values(box.asWidget(), ServerToClientModel.FOCUS));
    }

    @Test
    public void scrollKeepsTheCurrentItem() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), items("a", "b", "c")));
        box.open();
        DropDownTestEvents.keyDown(box.getContainer().asWidget(), PKeyCodes.DOWN);

        scroll(box);
        assertEquals("b", currentLabel(box));
        scroll(box);
        assertEquals("b", currentLabel(box));
    }

    @Test
    public void groupsAreRenderedAsHeaders() {
        final ListBox<String> box = attach(groupListBox());
        assertTrue(box.asWidget().hasStyleName("dd-listbox-group"));
        box.open();

        assertEquals(List.of("Fruits", "apple", "pear", "Vegetables", "leek"), renderedLabels(box));
        assertTrue(row(box, "Fruits").hasStyleName("dd-listbox-item-group"));
        assertFalse(row(box, "apple").hasStyleName("dd-listbox-item-group"));
        assertEquals("apple", currentLabel(box));

        DropDownTestEvents.keyDown(box.getContainer().asWidget(), PKeyCodes.DOWN);
        DropDownTestEvents.keyDown(box.getContainer().asWidget(), PKeyCodes.DOWN);
        assertEquals("leek", currentLabel(box));

        clickItem(box, "Fruits");
        assertNull(box.getSelectedData());
        assertTrue(box.isOpen());
    }

    @Test
    public void filterInGroupModeKeepsGroupHeaders() {
        final TextListBoxFilterWidget filter = new TextListBoxFilterWidget();
        final ListBox<String> box = groupListBox();
        box.setFilterWidgetSupplier(() -> filter);
        attach(box);
        box.open();

        filter.setFilter("LE");
        DropDownTestEvents.keyUp(filter.asWidget(), 76);

        assertEquals(List.of("Fruits", "apple", "Vegetables", "leek"), renderedLabels(box));
    }

    @Test
    public void filterWidgetNarrowsVisibleItems() {
        final TextListBoxFilterWidget filter = new TextListBoxFilterWidget();
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration().setPlaceholder("Search"), items("pear", "apple", "fig"));
        box.setFilterWidgetSupplier(() -> filter);
        attach(box);
        box.open();
        final PTextBox textBox = (PTextBox) filter.asWidget();
        assertEquals("Search", textBox.getPlaceholder());
        assertEquals(TabindexMode.TABULABLE.getTabIndex(), textBox.getTabindex());
        assertEquals(List.of(Boolean.TRUE), writer.values(textBox, ServerToClientModel.FOCUS));

        filter.setFilter("P");
        DropDownTestEvents.keyUp(textBox, PKeyCodes.TAB.getCode());
        assertEquals(List.of("apple", "fig", "pear"), renderedLabels(box));
        DropDownTestEvents.keyUp(textBox, 80);
        assertEquals(List.of("apple", "pear"), renderedLabels(box));

        box.setFilterPredicate((item, value) -> item.getLabel().startsWith(value));
        DropDownTestEvents.keyUp(textBox, 80);
        assertEquals(List.of("pear"), renderedLabels(box));

        box.addItem(ListBoxItem.of("plum"));
        assertEquals(List.of("pear", "plum"), renderedLabels(box));

        box.close();
        box.open();
        assertEquals(List.of("apple", "fig", "pear", "plum"), renderedLabels(box));
    }

    @Test
    public void shiftTabInFilterGivesFocusBackToTheListBox() {
        final TextListBoxFilterWidget filter = new TextListBoxFilterWidget();
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("a"));
        box.setFilterWidgetSupplier(() -> filter);
        attach(box);
        box.open();
        writer.clear();

        DropDownTestEvents.keyDown(filter.asWidget(), PKeyCodes.TAB);
        assertTrue(writer.values(box.asWidget(), ServerToClientModel.FOCUS).isEmpty());

        DropDownTestEvents.keyDown(filter.asWidget(), PKeyCodes.SHIFT);
        DropDownTestEvents.keyDown(filter.asWidget(), PKeyCodes.TAB);
        assertEquals(List.of(Boolean.TRUE), writer.values(box.asWidget(), ServerToClientModel.FOCUS));

        DropDownTestEvents.keyUp(filter.asWidget(), PKeyCodes.SHIFT.getCode());
        DropDownTestEvents.keyDown(filter.asWidget(), PKeyCodes.TAB);
        assertEquals(1, writer.values(box.asWidget(), ServerToClientModel.FOCUS).size());
    }

    @Test
    public void typingOnClosedListBoxOpensItWithTheFilter() {
        final TextListBoxFilterWidget filter = new TextListBoxFilterWidget();
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("a"));
        box.setFilterWidgetSupplier(() -> filter);
        attach(box);

        DropDownTestEvents.keyDown(box.asWidget(), 65);

        assertTrue(box.isOpen());
        assertEquals("A", filter.getFilter());

        final ListBox<String> noSearch = attach(new ListBox<>(new ListBoxConfiguration().disableSearch(), items("a")));
        DropDownTestEvents.keyDown(noSearch.asWidget(), 65);
        assertFalse(noSearch.isOpen());
    }

    @Test
    public void refreshFollowsItemChangesWhileOpen() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), items("a", "b")));
        box.open();

        box.addItem(ListBoxItem.of("c"));
        assertEquals(List.of("a", "b", "c"), renderedLabels(box));
        box.removeItem("a");
        assertEquals(List.of("b", "c"), renderedLabels(box));
        box.renameItem("b", "beta");
        assertEquals(List.of("beta", "c"), renderedLabels(box));
        box.clear();
        assertTrue(renderedLabels(box).isEmpty());
    }

    @Test
    public void shouldAllowSelection_WhenMultiSelectionDisabledAndSelectedItemsNull() throws Exception {
        ReflectionTestUtil.setField(configuration, "multiSelectionEnabled", false);
        configuration.setSelectionLimit(0);
        setConfigurationInListBox(listBox, FIELD_CONFIGURATION, configuration);

        assertTrue(invokeIsSelectionAllowed(null));
    }

    @Test
    public void allSelectedItemsAreSortedFirst() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), items("d", "c", "b", "a")));
        box.setMultiSelected(List.of("b", "d"));

        box.open();

        assertEquals(List.of("b", "d", "a", "c"), renderedLabels(box));
        assertTrue(row(box, "d").hasStyleName("dd-listbox-item-last-selected"));
        assertFalse(row(box, "b").hasStyleName("dd-listbox-item-last-selected"));
    }

    @Test
    public void plainItemAddedInGroupModeIsAppended() {
        final ListBox<String> box = groupListBox();

        box.addItem(ListBoxItem.of("loose"));

        assertEquals(List.of("Fruits", "pear", "apple", "Vegetables", "leek", "loose"), labels(box.getItems()));
        assertNull(item(box, "loose").getGroupName());
    }

    @Test
    public void groupChangesWhileOpenAreRendered() {
        final ListBox<String> box = attach(groupListBox());
        box.setSelected("leek");
        box.open();

        box.addItemInGroup("Vegetables", ListBoxItem.of("carrot"));
        assertEquals(List.of("Fruits", "apple", "pear", "Vegetables", "leek", "carrot"), renderedLabels(box));

        box.renameItemInGroup("Vegetables", "leek", "Leek");
        assertEquals(List.of("Fruits", "apple", "pear", "Vegetables", "Leek", "carrot"), renderedLabels(box));
        assertEquals("Leek", mainButton(box).getText());
    }

    @Test
    public void addItemsWhileOpenAreRenderedSorted() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), items("b", "c")));
        box.open();

        box.addItems(List.of(ListBoxItem.of("a")));

        assertEquals(List.of("a", "b", "c"), renderedLabels(box));
    }

    @Test
    public void removingUnselectedGroupItemsKeepsTheSelection() {
        final ListBox<String> box = groupListBox();
        box.setSelected("leek");

        box.removeItemFromGroup("Fruits", "apple");
        assertEquals("leek", box.getSelectedData());

        box.removeGroup("Fruits");
        assertEquals("leek", box.getSelectedData());
        assertEquals(List.of("Vegetables", "leek"), labels(box.getItems()));
    }

    @Test
    public void renameItemBeforeAttach() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("a"));

        box.renameItem("a", "alpha");

        assertEquals(List.of("alpha"), labels(box.getItems()));
    }

    @Test
    public void selectedItemWithoutDataIsSkippedBySelectedData() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(),
            List.of(ListBoxItem.of("none", null, true), ListBoxItem.of("a", "a", true)));

        assertEquals("a", box.getSelectedData());
        assertEquals("none", box.getSelectedItem().getLabel());
    }

    @Test
    public void unknownDataSelectsNothing() {
        final ListBox<String> single = new ListBox<>(new ListBoxConfiguration(),
            List.of(ListBoxItem.of("none", null), ListBoxItem.of("a", "a")));
        single.setSelected("zz");
        assertNull(single.getSelectedItem());

        final ListBox<String> multi = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(),
            List.of(ListBoxItem.of("none", null), ListBoxItem.of("a", "a")));
        multi.setMultiSelected(List.of("zz"));
        assertTrue(multi.getSelectedItems().isEmpty());

        multi.addToSelection("a");
        multi.addToSelection("a");
        multi.addToSelection("zz");
        assertEquals(List.of("a"), multi.getSelectedDataList());
    }

    @Test
    public void removeFromSelectionOfUnknownDataKeepsTheSelection() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(),
            List.of(ListBoxItem.of("none", null, true), ListBoxItem.of("a", "a", true)));

        box.removeFromSelection("zz");

        assertEquals(List.of("none", "a"), labels(box.getSelectedItems()));
    }

    @Test
    public void eventOnlyRejectsMultiSelectionChanges() {
        final ListBoxConfiguration multi = new ListBoxConfiguration().enableMultiSelection();
        final ListBox<String> box = new ListBox<>(multi, items("a"));
        multi.enabledEventOnly();

        assertThrows(UnsupportedOperationException.class, () -> box.setMultiSelected(List.of("a")));
        assertThrows(UnsupportedOperationException.class, () -> box.removeFromSelection("a"));
    }

    @Test
    public void dataProviderEventOnlyRejectsSelectionById() {
        final ListBoxConfiguration single = new ListBoxConfiguration();
        final ListBox<String> singleBox = new ListBox<>(single, new ListDataProvider("a"));
        single.enabledEventOnly();
        assertThrows(UnsupportedOperationException.class, () -> singleBox.setSelectedId("a"));

        final ListBoxConfiguration multi = new ListBoxConfiguration().enableMultiSelection();
        final ListBox<String> multiBox = new ListBox<>(multi, new ListDataProvider("a"));
        multi.enabledEventOnly();
        assertThrows(UnsupportedOperationException.class, () -> multiBox.setSelectedIds(List.of("a")));
    }

    @Test
    public void dataProviderEmptySelectionClearsIt() {
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), new ListDataProvider("a", "b"));
        box.setSelectedIds(List.of("a"));

        box.setSelectedIds(List.of());
        assertTrue(box.getSelectedDataList().isEmpty());

        box.setMultiSelected(List.of("b"));
        box.setMultiSelected(List.of());
        assertTrue(box.getSelectedDataList().isEmpty());
    }

    @Test
    public void dataProviderSetSelectedIndex_MultiSelectionAndOutOfRange() {
        final ListBox<String> multi = new ListBox<>(new ListBoxConfiguration().enableMultiSelection(), new ListDataProvider("a", "b"));
        multi.setSelectedIndex(1);
        assertEquals(List.of("b"), multi.getSelectedDataList());

        multi.setSelectedIndex(5);
        assertTrue(multi.getSelectedDataList().isEmpty());
    }

    @Test
    public void navigationKeysInFilterDoNotFilter() {
        final TextListBoxFilterWidget filter = new TextListBoxFilterWidget();
        final ListBox<String> box = new ListBox<>(new ListBoxConfiguration(), items("pear", "apple", "fig"));
        box.setFilterWidgetSupplier(() -> filter);
        attach(box);
        box.open();
        filter.setFilter("p");

        for (final PKeyCodes key : List.of(PKeyCodes.CTRL, PKeyCodes.ALT, PKeyCodes.PAGE_UP, PKeyCodes.PAGE_DOWN, PKeyCodes.END,
            PKeyCodes.ENTER, PKeyCodes.HOME, PKeyCodes.LEFT, PKeyCodes.UP, PKeyCodes.RIGHT, PKeyCodes.DOWN)) {
            DropDownTestEvents.keyUp(filter.asWidget(), key.getCode());
            assertEquals(List.of("apple", "fig", "pear"), renderedLabels(box), key.name());
        }

        DropDownTestEvents.keyUp(filter.asWidget(), 80);
        assertEquals(List.of("apple", "pear"), renderedLabels(box));
    }

    @Test
    public void filterInGroupModeShowsEachGroupHeaderOnce() {
        final TextListBoxFilterWidget filter = new TextListBoxFilterWidget();
        final ListBox<String> box = groupListBox();
        box.setFilterWidgetSupplier(() -> filter);
        attach(box);
        box.open();

        filter.setFilter("p");
        DropDownTestEvents.keyUp(filter.asWidget(), 80);

        assertEquals(List.of("Fruits", "apple", "pear"), renderedLabels(box));
    }

    @Test
    public void titleNotDisplayedShowsOnlyTheSelection() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().setTitle("T").setTitleDisplayed(false), items("a")));
        assertEquals("", mainButton(box).getText());

        box.setSelected("a");

        assertEquals("a", mainButton(box).getText());
    }

    @Test
    public void allLabelWithoutTitle() {
        final ListBox<String> box = attach(
            new ListBox<>(new ListBoxConfiguration().enableMultiSelection().disableTitlePlaceHolder().setAllLabel("All"), items("a")));

        assertEquals("All", mainButton(box).getText());
    }

    @Test
    public void clearMultiButtonFollowsTheSelection() {
        final ListBoxConfiguration multi = new ListBoxConfiguration().enableMultiSelection().enableMultiSelectionClear("Clear");
        final ListBox<String> box = attach(new ListBox<>(multi, items("a", "b")));
        box.setMultiSelected(List.of("a"));

        box.open();
        final PButton clear = clearMultiButton(box);
        assertTrue(clear.isEnabled());

        clickItem(box, "a");
        assertTrue(box.getSelectedItems().isEmpty());
        assertFalse(clear.isEnabled());
        assertFalse(clearButton(box).isVisible());
    }

    @Test
    public void closeOnClickFalseInMultiSelectionKeepsItOpen() {
        final ListBox<String> box = attach(new ListBox<>(
            new ListBoxConfiguration().enableMultiSelection().setCloseOnClickMode(DropDownCloseOnClickMode.FALSE), items("a", "b")));
        box.open();

        clickItem(box, "a");

        assertTrue(box.isOpen());
        assertEquals(List.of("a"), box.getSelectedDataList());
    }

    @Test
    public void enterInMultiSelectionKeepsItOpenWithoutFocus() {
        final ListBox<String> box = attach(
            new ListBox<>(new ListBoxConfiguration().enableMultiSelection().disableSearch(), items("a", "b")));
        box.open();
        writer.clear();

        DropDownTestEvents.keyDown(box.getContainer().asWidget(), PKeyCodes.ENTER);

        assertEquals(List.of("a"), box.getSelectedDataList());
        assertTrue(box.isOpen());
        assertFalse(writer.values(box.asWidget(), ServerToClientModel.FOCUS).contains(Boolean.TRUE));
    }

    @Test
    public void keysOnEmptyListDoNothing() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().disableSearch()));
        box.open();
        final PWidget keys = box.getContainer().asWidget();

        box.showIndex(ListBoxItem.of("unknown"));
        DropDownTestEvents.keyDown(keys, PKeyCodes.DOWN);
        DropDownTestEvents.keyDown(keys, PKeyCodes.ENTER);

        assertTrue(box.isOpen());
        assertNull(box.getItemContainer().getCurrentItemIndex());
        assertNull(box.getSelectedItem());
    }

    @Test
    public void keysFromAnUnknownIndexRestartFromTheFirstEnabledItem() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().disableSearch(), items("a", "b", "c")));
        box.setDataEnabled("a", false);
        box.open();
        final PWidget keys = box.getContainer().asWidget();

        box.showIndex(ListBoxItem.of("unknown"));
        DropDownTestEvents.keyDown(keys, PKeyCodes.DOWN);
        assertEquals("b", currentLabel(box));

        box.setDataEnabled("a", true);
        box.showIndex(ListBoxItem.of("unknown"));
        DropDownTestEvents.keyDown(keys, PKeyCodes.DOWN);
        assertEquals("a", currentLabel(box));

        box.showIndex(ListBoxItem.of("unknown"));
        DropDownTestEvents.keyDown(keys, PKeyCodes.UP);
        assertEquals("a", currentLabel(box));
    }

    @Test
    public void keysFromAnIndexPastTheEndMoveFromTheLastItem() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().disableSearch(), items("a", "b", "c")));
        box.open();
        final PWidget keys = box.getContainer().asWidget();

        ReflectionTestUtil.setField(box, "index", 10);
        DropDownTestEvents.keyDown(keys, PKeyCodes.UP);
        assertEquals("b", currentLabel(box));

        ReflectionTestUtil.setField(box, "index", 10);
        DropDownTestEvents.keyDown(keys, PKeyCodes.DOWN);
        assertEquals("c", currentLabel(box));
    }

    @Test
    public void rightKeyInMultilevelModeWithoutNestedRenderer() {
        final ListBoxConfiguration multilevel = new ListBoxConfiguration().disableSearch();
        multilevel.enableMultilevel();
        final ListBox<String> box = attach(new ListBox<>(multilevel, items("a")));
        box.open();

        DropDownTestEvents.keyDown(box.getContainer().asWidget(), PKeyCodes.RIGHT);
        assertTrue(box.isOpen());

        final ListBox<String> empty = attach(new ListBox<>(multilevel));
        empty.open();
        DropDownTestEvents.keyDown(empty.getContainer().asWidget(), PKeyCodes.RIGHT);
        assertTrue(empty.isOpen());
    }

    @Test
    public void blurOfOpenMultilevelListBoxFocusesTheFilter() {
        final TextListBoxFilterWidget filter = new TextListBoxFilterWidget();
        final ListBoxConfiguration multilevel = new ListBoxConfiguration();
        multilevel.enableMultilevel();
        final ListBox<String> box = new ListBox<>(multilevel, items("a"));
        box.setFilterWidgetSupplier(() -> filter);
        attach(box);
        DropDownTestEvents.focus(box.asWidget());
        box.open();
        writer.clear();

        DropDownTestEvents.blur(box.asWidget());

        assertTrue(box.isOpen());
        assertEquals(List.of(Boolean.TRUE), writer.values(filter.asWidget(), ServerToClientModel.FOCUS));

        // Without a filter there is nothing to focus: the multilevel list box stays open
        final ListBoxConfiguration noSearch = new ListBoxConfiguration().disableSearch();
        noSearch.enableMultilevel();
        final ListBox<String> other = attach(new ListBox<>(noSearch, items("a")));
        DropDownTestEvents.focus(other.asWidget());
        other.open();
        DropDownTestEvents.blur(other.asWidget());
        assertTrue(other.isOpen());
    }

    @Test
    public void nonCharacterKeyOnClosedListBoxDoesNotOpenIt() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), items("a")));

        DropDownTestEvents.keyDown(box.asWidget(), PKeyCodes.DOWN);

        assertFalse(box.isOpen());
    }

    @Test
    public void removeUpDownKeyHandlerBeforeOpening() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration().disableSearch(), items("a")));

        box.removeUpDownKeyHandler();

        assertEquals(TabindexMode.FOCUSABLE.getTabIndex(), box.getContainer().asWidget().getTabindex());
    }

    @Test
    public void isValueEmpty() {
        assertTrue(listBox.isValueEmpty(List.of()));
        assertFalse(listBox.isValueEmpty(List.of(ListBoxItem.of("a"))));
    }

    @Test
    public void hashCodeWithNullLabelAndGroup() {
        assertEquals(ListBoxItem.of(null, 1).hashCode(), ListBoxItem.of(null, 2).hashCode());
        final ListBoxItem<Integer> first = ListBoxItem.of("a", 1);
        first.setGroupName("g");
        final ListBoxItem<Integer> second = ListBoxItem.of("a", 2);
        second.setGroupName("g");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void scrollOnAnEmptyListKeepsNoCurrentItem() {
        final ListBox<String> box = attach(new ListBox<>(new ListBoxConfiguration(), items()));
        box.open();

        scroll(box);

        assertNull(box.getItemContainer().getCurrentItemIndex());
    }

    // Helpers

    private static List<ListBoxItem<String>> items(final String... labels) {
        final List<ListBoxItem<String>> items = new ArrayList<>();
        for (final String label : labels) {
            items.add(ListBoxItem.of(label));
        }
        return items;
    }

    private static List<String> labels(final Collection<? extends ListBoxItem<?>> items) {
        return items.stream().map(ListBoxItem::getLabel).collect(Collectors.toList());
    }

    private static <D> ListBoxItem<D> item(final ListBox<D> box, final String label) {
        return box.getItems().stream().filter(i -> label.equals(i.getLabel())).findFirst().orElseThrow();
    }

    private static ListBox<String> groupListBox() {
        return new ListBox<>(new ListBoxConfiguration().enableGroup(),
            List.of(GroupListBoxItem.of("Fruits", items("pear", "apple")), GroupListBoxItem.of("Vegetables", items("leek")),
                ListBoxItem.of("orphan")));
    }

    private static <D> ListBox<D> attach(final ListBox<D> box) {
        PWindow.getMain().add(box.asWidget());
        return box;
    }

    private static PButton mainButton(final ListBox<?> box) {
        return (PButton) box.asWidget().getWidget(0);
    }

    private static PButton clearButton(final ListBox<?> box) {
        return (PButton) box.asWidget().getWidget(2);
    }

    private static PButton clearMultiButton(final ListBox<?> box) {
        final PComplexPanel defaultContainer = (PComplexPanel) ((PComplexPanel) box.getContainer().asWidget()).getWidget(0);
        return (PButton) defaultContainer.getWidget(1);
    }

    private static PComplexPanel rows(final ListBox<?> box) {
        return (PComplexPanel) ((PComplexPanel) box.getItemContainer().asWidget()).getWidget(0);
    }

    private static PLabel labelOf(final PWidget row) {
        final PComplexPanel panel = (PComplexPanel) row;
        return (PLabel) panel.getWidget(panel.getWidgetCount() - 1);
    }

    private static List<String> renderedLabels(final ListBox<?> box) {
        final List<String> labels = new ArrayList<>();
        final PComplexPanel rows = rows(box);
        for (int i = 0; i < rows.getWidgetCount(); i++) {
            labels.add(labelOf(rows.getWidget(i)).getText());
        }
        return labels;
    }

    private static PComplexPanel row(final ListBox<?> box, final String label) {
        final PComplexPanel rows = rows(box);
        for (int i = 0; i < rows.getWidgetCount(); i++) {
            if (label.equals(labelOf(rows.getWidget(i)).getText())) return (PComplexPanel) rows.getWidget(i);
        }
        throw new AssertionError("No rendered item " + label);
    }

    private static PLabel itemLabel(final ListBox<?> box, final String label) {
        return labelOf(row(box, label));
    }

    private static PCheckBox checkBox(final ListBox<?> box, final String label) {
        return (PCheckBox) row(box, label).getWidget(0);
    }

    private static void clickItem(final ListBox<?> box, final String label) {
        PEmulator.click(itemLabel(box, label));
    }

    private static String currentLabel(final ListBox<?> box) {
        return labelOf(box.getItemContainer().getCurrentItemIndex().asWidget()).getText();
    }

    private static void scroll(final ListBox<?> box) {
        box.getItemContainer().onClientData(jakarta.json.Json.createObjectBuilder()
            .add(com.ponysdk.core.model.ClientToServerModel.NATIVE.toStringValue(),
                jakarta.json.Json.createObjectBuilder().add("beginIndex", 0).add("maxVisibleItem", 10))
            .build());
    }

    /**
     * In-memory provider honouring the begin index, the page size and a "contains" label filter.
     */
    private static final class ListDataProvider implements ListBoxDataProvider<String> {

        private final List<ListBoxItem<String>> all;

        ListDataProvider(final String... labels) {
            this.all = items(labels);
        }

        private List<ListBoxItem<String>> filtered(final String filter) {
            if (filter == null) return all;
            return all.stream().filter(i -> i.getLabel().toLowerCase().contains(filter.toLowerCase())).collect(Collectors.toList());
        }

        @Override
        public int getFullDataSize(final String containsLabelFilter) {
            return filtered(containsLabelFilter).size();
        }

        @Override
        public List<ListBoxItem<String>> getDataByIds(final Collection<Object> ids) {
            return all.stream().filter(i -> ids.contains(i.getData())).collect(Collectors.toList());
        }

        @Override
        public List<ListBoxItem<String>> getData(final int beginIndex, final int maxSize, final String containsLabelFilter) {
            final List<ListBoxItem<String>> filtered = filtered(containsLabelFilter);
            return new ArrayList<>(filtered.subList(Math.min(beginIndex, filtered.size()), Math.min(filtered.size(), beginIndex + maxSize)));
        }

        @Override
        public ListBoxItem<String> getItemFromData(final String selectedData) {
            return all.stream().filter(i -> i.getData().equals(selectedData)).findFirst().orElse(null);
        }
    }
}