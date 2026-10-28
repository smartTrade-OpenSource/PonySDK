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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PFrameTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static JsonObject opened(final String url) {
        return Json.createObjectBuilder().add(ClientToServerModel.HANDLER_OPEN.toStringValue(), url).build();
    }

    @Test
    public void testInit() {
        final PFrame frame = Element.newPFrame("http://host/page");
        assertEquals(WidgetType.FRAME, frame.getWidgetType());
        assertEquals("http://host/page", frame.getURL());
        assertNull(Element.newPFrame().getURL());
    }

    @Test
    public void testUrlSentOnCreationOnlyWhenNotEmpty() {
        writer.clear();
        PWindow.getMain().add(Element.newPFrame("http://host/page"));
        assertEquals(List.of("http://host/page"), writer.valuesOf(ServerToClientModel.URL));

        writer.clear();
        PWindow.getMain().add(Element.newPFrame(""));
        PWindow.getMain().add(Element.newPFrame());
        assertEquals(0, writer.count(ServerToClientModel.URL));
    }

    @Test
    public void testChildrenCreatedOnceTheFrameIsOpened() {
        final PFrame frame = Element.newPFrame("a");
        PWindow.getMain().add(frame);
        final PLabel label = Element.newPLabel("in frame");
        frame.add(label);
        assertFalse(label.isInitialized());

        writer.clear();
        frame.onClientData(opened("http://host/loaded"));
        assertEquals("http://host/loaded", frame.getURL());
        assertTrue(label.isInitialized());
        assertSame(frame, label.getFrame());
        assertTrue(writer.valuesOf(ServerToClientModel.FRAME_ID).contains(frame.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(label.getID()));
    }

    @Test
    public void testZoneAddedAfterOpenIsAttachedImmediately() {
        final PFrame frame = Element.newPFrame("a");
        PWindow.getMain().add(frame);
        frame.onClientData(opened("a"));
        writer.clear();
        final PLabel label = Element.newPLabel();
        frame.add("zone", label);
        assertTrue(label.isInitialized());
        assertEquals(List.of("zone"), writer.valuesOf(ServerToClientModel.ROOT_ID));
    }

    @Test
    public void testClientDataIgnoredWhenHidden() {
        final PFrame frame = Element.newPFrame("a");
        PWindow.getMain().add(frame);
        final PLabel label = Element.newPLabel();
        frame.add(label);
        frame.setVisible(false);
        frame.onClientData(opened("b"));
        assertEquals("a", frame.getURL());
        assertFalse(label.isInitialized());
    }

    @Test
    public void testOtherClientDataDelegated() {
        final PFrame frame = Element.newPFrame("a");
        PWindow.getMain().add(frame);
        frame.onClientData(Json.createObjectBuilder().add("unknown", 1).build());
        assertEquals("a", frame.getURL());
    }

    @Test
    public void testDumpDOM() {
        final PFrame frame = Element.newPFrame("a");
        assertEquals("<frame pid=\"" + frame.getID() + "\"></frame>", frame.dumpDOM());
        final PMenuItemSeparator child = Element.newPMenuItemSeparator();
        frame.add(child);
        final String dom = frame.dumpDOM();
        assertTrue(dom.startsWith("<frame pid=\"" + frame.getID() + "\"><div pid=\""), dom);
        assertTrue(dom.endsWith("<li pid=\"" + child.getID() + "\" separator></li></div></frame>"), dom);
    }

    @Test
    public void aZoneIsAttachedOnceTheFrameIsOpenAndReusedAfter() {
        final PFrame frame = Element.newPFrame("url");
        PWindow.getMain().add(frame);
        frame.onClientData(opened("url"));
        final PLabel first = Element.newPLabel("1");
        final PLabel second = Element.newPLabel("2");
        frame.add("zone", first);
        frame.add("zone", second);
        assertTrue(first.isInitialized());
        assertTrue(second.isInitialized());
        assertSame(first.getParent(), second.getParent());
    }

    @Test
    public void aFrameOpenedBeforeItsCreationAttachesItsZonesLater() {
        final PFrame frame = Element.newPFrame("url");
        frame.onClientData(opened("url"));
        final PLabel label = Element.newPLabel("1");
        frame.add(label);
        assertFalse(label.isInitialized());
    }
}
