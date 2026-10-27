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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PDialogBoxTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    @Test
    public void testInit() {
        final PDialogBox dialog = Element.newPDialogBox();
        assertEquals(WidgetType.DIALOG_BOX, dialog.getWidgetType());
        assertFalse(dialog.isVisible());
        assertNull(dialog.getCaption());
    }

    @Test
    public void testAutoHide() {
        writer.clear();
        PWindow.getMain().add(Element.newPDialogBox(true));
        assertTrue(writer.valuesOf(ServerToClientModel.POPUP_AUTO_HIDE).contains(true));
    }

    @Test
    public void testSetCaption() {
        final PDialogBox dialog = Element.newPDialogBox(false);
        PWindow.getMain().add(dialog);
        writer.clear();
        dialog.setCaption("Caption");
        assertEquals("Caption", dialog.getCaption());
        dialog.setCaption("Caption");
        assertEquals(1, writer.count(ServerToClientModel.POPUP_CAPTION));
        assertTrue(writer.valuesOf(ServerToClientModel.POPUP_CAPTION).contains("Caption"));
    }

}
