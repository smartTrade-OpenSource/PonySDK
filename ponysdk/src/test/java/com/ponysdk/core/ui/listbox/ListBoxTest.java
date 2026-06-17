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
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.ponysdk.core.ui.listbox.MultiLevelDropDownRenderer.getConfiguration;
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
}