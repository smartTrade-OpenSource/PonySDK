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

package com.ponysdk.core.ui.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import com.ponysdk.test.PSuite;

public class PStringTest extends PSuite {

    @Test
    public void testMessages() {
        assertEquals("Ok", PString.get("dialog.ok"));
        assertEquals("3 chars minimum", PString.get("validator.error.minlength", 3));

        final ResourceBundle bundle = PString.switchLocal(Locale.ENGLISH);
        assertNotNull(bundle);
        assertEquals("Cancel", bundle.getString("dialog.cancel"));
        assertEquals("Cancel", PString.get("dialog.cancel"));
    }
}
