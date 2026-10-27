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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PHorizontalPanelTest extends PSuite {

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
        final PHorizontalPanel panel = Element.newPHorizontalPanel();
        assertEquals(WidgetType.HORIZONTAL_PANEL, panel.getWidgetType());
        assertEquals(PHorizontalAlignment.ALIGN_LEFT, panel.getHorizontalAlignment());
        assertEquals(PVerticalAlignment.ALIGN_TOP, panel.getVerticalAlignment());
    }

    @Test
    public void testAlignmentsSentOnlyWhenChanged() {
        final PHorizontalPanel panel = Element.newPHorizontalPanel();
        PWindow.getMain().add(panel);
        writer.clear();

        panel.setHorizontalAlignment(PHorizontalAlignment.ALIGN_LEFT);
        panel.setVerticalAlignment(PVerticalAlignment.ALIGN_TOP);
        assertTrue(writer.isEmpty());

        panel.setHorizontalAlignment(PHorizontalAlignment.ALIGN_RIGHT);
        panel.setVerticalAlignment(PVerticalAlignment.ALIGN_BOTTOM);
        assertEquals(PHorizontalAlignment.ALIGN_RIGHT, panel.getHorizontalAlignment());
        assertEquals(PVerticalAlignment.ALIGN_BOTTOM, panel.getVerticalAlignment());
        assertEquals(List.of(PHorizontalAlignment.ALIGN_RIGHT.getValue()), writer.valuesOf(ServerToClientModel.HORIZONTAL_ALIGNMENT));
        assertEquals(List.of(PVerticalAlignment.ALIGN_BOTTOM.getValue()), writer.valuesOf(ServerToClientModel.VERTICAL_ALIGNMENT));
    }

}
