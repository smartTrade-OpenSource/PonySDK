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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;
import static org.junit.jupiter.api.Assertions.*;

public class PPushButtonTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    @Test
    public void theImageIsCreatedFirstThenAddedIntoTheButton() {
        final PImage image = Element.newPImage("icon.png");
        final PPushButton button = Element.newPPushButton(image);
        assertEquals(WidgetType.PUSH_BUTTON, button.getWidgetType());

        PWindow.getMain().add(button);

        assertTrue(image.isInitialized());
        assertSame(PWindow.getMain(), image.getWindow());
        final List<Object> created = writer.valuesOf(ServerToClientModel.TYPE_CREATE);
        assertTrue(created.indexOf(image.getID()) < created.indexOf(button.getID()), "image created before the button");
        assertEquals(List.of(image.getID()), writer.valuesOf(ServerToClientModel.WIDGET_ID));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(image.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID).contains(button.getID()));
    }

    @Test
    public void attachingTwiceAddsTheImageOnce() {
        final PPushButton button = Element.newPPushButton(Element.newPImage("u"));
        PWindow.getMain().add(button);
        writer.clear();
        assertFalse(button.attach(PWindow.getMain(), null));
        assertEquals(0, writer.count(ServerToClientModel.TYPE_ADD));
    }
}
