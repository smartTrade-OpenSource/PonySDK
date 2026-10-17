/*
 * Copyright (c) 2024 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.flexlayout;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;

import org.junit.Test;

import com.ponysdk.core.ui.basic.PAddOn;
import com.ponysdk.test.PSuite;

/**
 * Verifies that {@link FlexLayoutAddon} sends its creation arguments through the v3
 * <strong>pure-binary</strong> {@code PAddOn(Object...)} path (ServerToClientModel.PADDON_CREATION_ARGS)
 * rather than the legacy JSON form ({@code PAddOn(JsonObject)} → PADDON_CREATION).
 * <p>
 * The addon stores the binary args in {@code PAddOn.creationArgs} and leaves the JSON {@code args}
 * field {@code null}; they are only flushed (and nulled) when the widget is attached to a window,
 * which does not happen here (the wrapped panel is never attached), so we can observe them directly.
 */
public class FlexLayoutAddonTest extends PSuite {

    @Test
    public void usesBinaryCreationArgsInsteadOfJson() throws Exception {
        final String model = "{\"type\":\"row\",\"children\":[]}";
        final String theme = "fl-theme-nord";
        final FlexLayoutAddon addon = new FlexLayoutAddon(model, theme, null);

        final Field jsonArgs = PAddOn.class.getDeclaredField("args");
        jsonArgs.setAccessible(true);
        final Field binaryArgs = PAddOn.class.getDeclaredField("creationArgs");
        binaryArgs.setAccessible(true);

        assertNull("the legacy JSON creation form must not be used", jsonArgs.get(addon));

        final Object[] creationArgs = (Object[]) binaryArgs.get(addon);
        assertNotNull("creation args must be sent as a binary typed array", creationArgs);
        assertArrayEquals("binary creation args must be [model, theme, borders]",
            new Object[] { model, theme, null }, creationArgs);
    }
}
