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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.Test;

public class ListBoxConfigurationTest {

    @Test
    public void bulkSelection_isDisabledByDefault() {
        final ListBoxConfiguration configuration = new ListBoxConfiguration().enableMultiSelection();

        assertEquals(false, configuration.isBulkSelectionEnabled());
        assertNull(configuration.getSelectAllLabel());
        assertNull(configuration.getUnselectAllLabel());
    }

    @Test
    public void bulkSelection_keepsTheLabels() {
        final ListBoxConfiguration configuration = new ListBoxConfiguration().enableMultiSelection().enableMultiSelectionBulk("Select all",
            "Unselect all");

        assertEquals(true, configuration.isBulkSelectionEnabled());
        assertEquals("Select all", configuration.getSelectAllLabel());
        assertEquals("Unselect all", configuration.getUnselectAllLabel());
    }
}
