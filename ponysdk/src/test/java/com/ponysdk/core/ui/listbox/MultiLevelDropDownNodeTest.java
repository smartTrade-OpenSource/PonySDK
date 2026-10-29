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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;

public class MultiLevelDropDownNodeTest {

    @Test
    public void leafWrapsAValue() {
        final MultiLevelDropDownNode<String> leaf = new MultiLevelDropDownNode<String>("value");

        assertTrue(leaf.isLeaf());
        assertEquals("value", leaf.getValue());
        assertNull(leaf.getItems());
    }

    @Test
    public void nodeWrapsItems() {
        final List<ListBoxItem<MultiLevelDropDownNode<String>>> items = List.of(ListBoxItem.of("a", new MultiLevelDropDownNode<String>("a")));
        final MultiLevelDropDownNode<String> node = new MultiLevelDropDownNode<String>(items);

        assertFalse(node.isLeaf());
        assertNull(node.getValue());
        assertSame(items, node.getItems());
    }

    @Test
    public void nodeRequiresItems() {
        assertThrows(IllegalArgumentException.class,
            () -> new MultiLevelDropDownNode<String>((Collection<ListBoxItem<MultiLevelDropDownNode<String>>>) null));
        assertThrows(IllegalArgumentException.class, () -> new MultiLevelDropDownNode<String>(List.of()));
    }
}
