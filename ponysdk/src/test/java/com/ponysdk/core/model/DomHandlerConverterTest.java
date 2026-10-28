/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DomHandlerConverterTest {

    /**
     * Test method for
     * {@link com.ponysdk.core.model.DomHandlerConverter#convert(com.ponysdk.core.model.DomHandlerType)}.
     */
    @Test
    public void testConvertDomHandlerType() {
        final DomHandlerType type = DomHandlerConverter.convert(HandlerModel.HANDLER_DOM_MOUSE_WHELL);
        assertEquals(DomHandlerType.MOUSE_WHELL, type);
    }

    /**
     * Test method for {@link com.ponysdk.core.model.DomHandlerConverter#convert(com.ponysdk.core.model.HandlerModel)}.
     */
    @Test
    public void testConvertHandlerModel() {
        final HandlerModel type = DomHandlerConverter.convert(DomHandlerType.MOUSE_WHELL);
        assertEquals(HandlerModel.HANDLER_DOM_MOUSE_WHELL, type);
    }

    @Test
    public void everyDomHandlerModelRoundTrips() {
        int mapped = 0;
        for (final HandlerModel model : HandlerModel.values()) {
            if (!model.name().startsWith("HANDLER_DOM_")) {
                assertThrows(IllegalArgumentException.class, () -> DomHandlerConverter.convert(model), model.name());
                continue;
            }
            final DomHandlerType type = DomHandlerConverter.convert(model);
            assertEquals(model, DomHandlerConverter.convert(type), model.name());
            assertEquals(model.name(), "HANDLER_DOM_" + type.name());
            mapped++;
        }
        assertEquals(20, mapped);
    }

    @Test
    public void aDomHandlerTypeWithoutModelIsRejected() {
        for (final DomHandlerType type : DomHandlerType.values()) {
            final boolean hasModel = java.util.Arrays.stream(HandlerModel.values()).anyMatch(m -> m.name().equals("HANDLER_DOM_" + type.name()));
            if (hasModel) assertEquals(type, DomHandlerConverter.convert(DomHandlerConverter.convert(type)));
            else assertThrows(IllegalArgumentException.class, () -> DomHandlerConverter.convert(type), type.name());
        }
    }

    @Test
    public void noDomHandlerTypeHasNoModel() {
        assertThrows(IllegalArgumentException.class, () -> DomHandlerConverter.convert((DomHandlerType) null));
    }
}
