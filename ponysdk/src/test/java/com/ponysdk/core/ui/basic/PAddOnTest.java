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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.WidgetType;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PAddOnTest extends PSuite {

    private final class TestPAddOn extends PAddOn {

        private TestPAddOn(final JsonObject args) {
            super(args);
        }
    }

    @Test
    public void testInit() {
        final PAddOn widget = new PAddOn() {
        };
        assertEquals(WidgetType.ADDON, widget.getWidgetType());
        assertNotNull(widget.toString());
    }

    @Test
    public void testSignature() {
        final PAddOn widget = new TestPAddOn(Json.createObjectBuilder().build());
        assertEquals(TestPAddOn.class.getCanonicalName(), widget.getSignature());
    }

}
