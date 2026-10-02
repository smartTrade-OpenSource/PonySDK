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

import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.ReflectionTestUtil;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.mockito.Mockito;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollAddon;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxDataProvider;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.ponysdk.core.ui.listbox.MultiLevelDropDownRenderer.getConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListBoxTest extends PSuite {

    private final static String FIELD_CONFIGURATION = "configuration";
    private final static String METHOD_IS_SELECTION_ALLOWED = "isSelectionAllowed";
    private final static String ITEM1 = "item1";
    private final static String ITEM2 = "item2";

    private ListBoxConfiguration configuration = getConfiguration();
    private final ListBox<String> listBox = new ListBox<>(configuration);


    @After
    public void tearDown() {
        configuration = getConfiguration();
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

    // Bulk selection (select all / unselect all)

    private static final List<String> PAIRS = List.of("USD/CAD", "USD/CLP", "USD/COP", "EUR/USD");

    @Test
    public void bulkSelection_requiresMultiSelection() {
        final ListBoxConfiguration configuration = new ListBoxConfiguration().enableMultiSelectionBulk("Select all", "Unselect all");

        assertThrows(IllegalStateException.class, () -> new ListBox<String>(configuration));
    }

    @Test
    public void bulkSelection_isRefusedWithASelectionLimit() {
        final ListBoxConfiguration configuration = new ListBoxConfiguration().enableMultiSelection().setSelectionLimit(2)
            .enableMultiSelectionBulk("Select all", "Unselect all");

        assertThrows(IllegalStateException.class, () -> new ListBox<String>(configuration));
    }

    // Remote list box (data provider)

    @Test
    public void remote_selectAllWithFilter_selectsOnlyTheMatches() {
        final TestListBox listBox = remoteListBox();
        listBox.filter = "USD/C";

        listBox.selectAllFiltered();

        assertEquals(Set.of("USD/CAD", "USD/CLP", "USD/COP"), Set.copyOf(listBox.getSelectedDataList()));
    }

    @Test
    public void remote_selectAllWithoutFilter_selectsEverything() {
        final TestListBox listBox = remoteListBox();

        listBox.selectAllFiltered();

        assertEquals(Set.copyOf(PAIRS), Set.copyOf(listBox.getSelectedDataList()));
    }

    @Test
    public void remote_selectAll_keepsThePreviousSelectionWithoutDuplicates() {
        final TestListBox listBox = remoteListBox();
        listBox.filter = "USD/CAD";
        listBox.selectAllFiltered();
        listBox.filter = "EUR";
        listBox.selectAllFiltered();

        listBox.filter = "USD";
        listBox.selectAllFiltered();

        assertEquals(PAIRS.size(), listBox.getSelectedDataList().size());
        assertEquals(Set.copyOf(PAIRS), Set.copyOf(listBox.getSelectedDataList()));
    }

    @Test
    public void remote_unselectAllWithFilter_keepsTheItemsOutsideTheFilter() {
        final TestListBox listBox = remoteListBox();
        listBox.selectAllFiltered();

        listBox.filter = "USD/C";
        listBox.unselectAllFiltered();

        assertEquals(List.of("EUR/USD"), new ArrayList<>(listBox.getSelectedDataList()));
    }

    @Test
    public void remote_unselectAllWithoutFilter_clearsTheSelection() {
        final TestListBox listBox = remoteListBox();
        listBox.selectAllFiltered();

        listBox.filter = null;
        listBox.unselectAllFiltered();

        assertEquals(0, listBox.getSelectedDataList().size());
    }

    @Test
    public void remote_selectAllWithAFilterMatchingNothing_changesNothing() {
        final TestListBox listBox = remoteListBox();
        listBox.filter = "XYZ";

        listBox.selectAllFiltered();

        assertEquals(0, listBox.getSelectedDataList().size());
    }

    @Test
    public void remote_selectAll_asksTheProviderForEveryMatchAtOnce() {
        final FakeProvider provider = new FakeProvider();
        final TestListBox listBox = new TestListBox(bulkConfiguration(), provider);
        listBox.filter = "USD/C";

        listBox.selectAllFiltered();

        assertEquals(3, provider.lastRequestedSize);
        assertEquals(0, provider.lastRequestedBegin);
        assertEquals("USD/C", provider.lastRequestedFilter);
    }

    // Scroll position

    @Test
    @SuppressWarnings("unchecked")
    public void selectAll_whenOpen_redrawsWithoutMovingTheScroll() {
        final TestListBox listBox = remoteListBox();
        final InfiniteScrollAddon<ListBoxItem<String>, ListBox<String>.ListBoxItemWidget> itemContainer = Mockito.mock(InfiniteScrollAddon.class);
        setItemContainer(listBox, itemContainer);
        listBox.open = true;

        listBox.selectAllFiltered();

        Mockito.verify(itemContainer).refreshWithoutScrollCorrection();
        Mockito.verify(itemContainer, Mockito.never()).showIndex(Mockito.anyInt());
        Mockito.verify(itemContainer, Mockito.never()).scrollToTop();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void unselectAll_whenOpen_redrawsWithoutMovingTheScroll() {
        final TestListBox listBox = remoteListBox();
        final InfiniteScrollAddon<ListBoxItem<String>, ListBox<String>.ListBoxItemWidget> itemContainer = Mockito.mock(InfiniteScrollAddon.class);
        setItemContainer(listBox, itemContainer);
        listBox.open = true;

        listBox.unselectAllFiltered();

        Mockito.verify(itemContainer).refreshWithoutScrollCorrection();
        Mockito.verify(itemContainer, Mockito.never()).showIndex(Mockito.anyInt());
        Mockito.verify(itemContainer, Mockito.never()).scrollToTop();
    }

    // In memory list box

    @Test
    public void inMemory_selectAllWithFilter_selectsOnlyTheMatches() {
        final TestListBox listBox = inMemoryListBox();
        listBox.filter = "usd/c";

        listBox.selectAllFiltered();

        assertEquals(Set.of("USD/CAD", "USD/CLP", "USD/COP"), Set.copyOf(listBox.getSelectedDataList()));
    }

    @Test
    public void inMemory_unselectAllWithFilter_keepsTheItemsOutsideTheFilter() {
        final TestListBox listBox = inMemoryListBox();
        listBox.selectAllFiltered();

        listBox.filter = "usd/c";
        listBox.unselectAllFiltered();

        assertEquals(List.of("EUR/USD"), new ArrayList<>(listBox.getSelectedDataList()));
    }

    @Test
    public void inMemory_selectAllWithoutFilter_selectsEverything() {
        final TestListBox listBox = inMemoryListBox();

        listBox.selectAllFiltered();

        assertEquals(Set.copyOf(PAIRS), Set.copyOf(listBox.getSelectedDataList()));
    }

    // Helpers

    private static ListBoxConfiguration bulkConfiguration() {
        return new ListBoxConfiguration().enableMultiSelection().enableMultiSelectionBulk("Select all", "Unselect all");
    }

    private static TestListBox remoteListBox() {
        return new TestListBox(bulkConfiguration(), new FakeProvider());
    }

    private static TestListBox inMemoryListBox() {
        final List<ListBoxItem<String>> items = PAIRS.stream().map(pair -> ListBoxItem.of(pair, pair)).collect(Collectors.toList());
        return new TestListBox(bulkConfiguration(), items);
    }

    private static void setItemContainer(final ListBox<?> listBox, final Object itemContainer) {
        try {
            final java.lang.reflect.Field field = ListBox.class.getDeclaredField("itemContainer");
            field.setAccessible(true);
            field.set(listBox, itemContainer);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot set the item container", e);
        }
    }

    /** A list box whose filter text is set by the test instead of typed in the filter widget. */
    private static class TestListBox extends ListBox<String> {

        String filter;
        boolean open;

        TestListBox(final ListBoxConfiguration configuration, final ListBoxDataProvider<String> provider) {
            super(configuration, provider);
        }

        TestListBox(final ListBoxConfiguration configuration, final Collection<? extends ListBoxItem<String>> items) {
            super(configuration, items);
        }

        @Override
        protected String getFilter() {
            return filter;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        protected void selectAllFiltered() {
            super.selectAllFiltered();
        }

        @Override
        protected void unselectAllFiltered() {
            super.unselectAllFiltered();
        }
    }

    private static class FakeProvider implements ListBoxDataProvider<String> {

        int lastRequestedBegin = -1;
        int lastRequestedSize = -1;
        String lastRequestedFilter;

        private List<String> matching(final String filter) {
            return PAIRS.stream().filter(pair -> filter == null || filter.isEmpty() || pair.toLowerCase().contains(filter.toLowerCase()))
                .collect(Collectors.toList());
        }

        @Override
        public int getFullDataSize(final String containsLabelFilter) {
            return matching(containsLabelFilter).size();
        }

        @Override
        public List<ListBoxItem<String>> getDataByIds(final Collection<Object> ids) {
            return PAIRS.stream().filter(ids::contains).map(this::getItemFromData).collect(Collectors.toList());
        }

        @Override
        public List<ListBoxItem<String>> getData(final int beginIndex, final int maxSize, final String containsLabelFilter) {
            lastRequestedBegin = beginIndex;
            lastRequestedSize = maxSize;
            lastRequestedFilter = containsLabelFilter;
            final List<String> matching = matching(containsLabelFilter);
            return matching.subList(Math.min(beginIndex, matching.size()), Math.min(beginIndex + maxSize, matching.size())).stream()
                .map(this::getItemFromData).collect(Collectors.toList());
        }

        @Override
        public ListBoxItem<String> getItemFromData(final String selectedData) {
            return ListBoxItem.of(selectedData, selectedData);
        }
    }
}
