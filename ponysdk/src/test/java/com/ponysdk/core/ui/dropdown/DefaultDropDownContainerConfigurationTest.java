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
package com.ponysdk.core.ui.dropdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.dropdown.DropDownContainerConfiguration.DropDownCloseOnClickMode;

public class DefaultDropDownContainerConfigurationTest {

    @Test
    public void defaults() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();

        assertTrue(configuration.isTitleDisplayed());
        assertTrue(configuration.isTitlePlaceHolder());
        assertNull(configuration.getTitle());
        assertEquals(DropDownContainerConfiguration.DEFAULT_TITLE_SEPARATOR, configuration.getTitleSeparator());
        assertNull(configuration.getPlaceholder());
        assertNull(configuration.getAllLabel());
        assertNull(configuration.getClearLabel());
        assertTrue(configuration.isSelectionDisplayed());
        assertEquals(DropDownCloseOnClickMode.DEFAULT, configuration.getCloseOnClickMode());
        assertTrue(configuration.isClearTitleButtonEnabled());
        assertFalse(configuration.isEventOnlyEnabled());
        assertFalse(configuration.isStopClickEventEnabled());
        assertFalse(configuration.isPositionDropRight());
        assertFalse(configuration.isMultilevelEnabled());
    }

    @Test
    public void settersAreFluentAndStoreValues() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();

        assertSame(configuration, configuration.setTitleDisplayed(false));
        assertSame(configuration, configuration.disableTitlePlaceHolder());
        assertSame(configuration, configuration.setTitle("title"));
        assertSame(configuration, configuration.setTitleSeparator("|"));
        assertSame(configuration, configuration.setPlaceholder("placeholder"));
        assertSame(configuration, configuration.setAllLabel("all"));
        assertSame(configuration, configuration.enableMultiSelectionClear("clear"));
        assertSame(configuration, configuration.setSelectionDisplayed(false));
        assertSame(configuration, configuration.setCloseOnClickMode(DropDownCloseOnClickMode.FALSE));
        assertSame(configuration, configuration.disableClearTitleButton());
        assertSame(configuration, configuration.enabledEventOnly());
        assertSame(configuration, configuration.enableStopClickEvent());
        assertSame(configuration, configuration.setPositionDropRight(true));

        assertFalse(configuration.isTitleDisplayed());
        assertFalse(configuration.isTitlePlaceHolder());
        assertEquals("title", configuration.getTitle());
        assertEquals("|", configuration.getTitleSeparator());
        assertEquals("placeholder", configuration.getPlaceholder());
        assertEquals("all", configuration.getAllLabel());
        assertEquals("clear", configuration.getClearLabel());
        assertFalse(configuration.isSelectionDisplayed());
        assertEquals(DropDownCloseOnClickMode.FALSE, configuration.getCloseOnClickMode());
        assertFalse(configuration.isClearTitleButtonEnabled());
        assertTrue(configuration.isEventOnlyEnabled());
        assertTrue(configuration.isStopClickEventEnabled());
        assertTrue(configuration.isPositionDropRight());
    }

    @Test
    public void multilevelForcesDropRight() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();

        assertSame(configuration, configuration.enableMultilevel());

        assertTrue(configuration.isMultilevelEnabled());
        assertTrue(configuration.isPositionDropRight());
    }
}
