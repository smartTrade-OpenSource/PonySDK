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

import org.junit.jupiter.api.Test;

public class ListBoxConfigurationTest {

    @Test
    public void defaults() {
        final ListBoxConfiguration configuration = new ListBoxConfiguration();

        assertTrue(configuration.isSearchEnabled());
        assertNull(configuration.getNoMatchesLabel());
        assertFalse(configuration.isMultiSelectionEnabled());
        assertTrue(configuration.isSortingEnabled());
        assertTrue(configuration.isSelectionSortingEnabled());
        assertNull(configuration.getSelectionLimit());
        assertNull(configuration.getDisplaySelectionLimit());
        assertNull(configuration.getDisplaySelectionLabel());
        assertFalse(configuration.isGroupEnabled());
    }

    @Test
    public void settersAreFluent() {
        final ListBoxConfiguration configuration = new ListBoxConfiguration();

        assertSame(configuration, configuration.setNoMatchesLabel("none"));
        assertSame(configuration, configuration.setTitleDisplayed(false));
        assertSame(configuration, configuration.disableSearch());
        assertSame(configuration, configuration.setSelectionLimit(3));

        assertEquals("none", configuration.getNoMatchesLabel());
        assertFalse(configuration.isTitleDisplayed());
        assertFalse(configuration.isSearchEnabled());
        assertEquals(3, configuration.getSelectionLimit());
    }
}
