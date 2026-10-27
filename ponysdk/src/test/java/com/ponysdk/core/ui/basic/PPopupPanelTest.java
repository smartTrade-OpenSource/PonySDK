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

import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PPopupPanelTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PPopupPanel attachedPopup(final boolean autoHide) {
        final PPopupPanel popup = Element.newPPopupPanel(autoHide);
        PWindow.getMain().add(popup);
        return popup;
    }

    @Test
    public void testInit() {
        final PPopupPanel popup = Element.newPPopupPanel();
        assertEquals(WidgetType.POPUP_PANEL, popup.getWidgetType());
        assertFalse(popup.isVisible());
        assertFalse(popup.isShowing());
        assertFalse(popup.isAnimationEnabled());
    }

    @Test
    public void testAutoHideWrittenOnCreation() {
        writer.clear();
        attachedPopup(true);
        assertTrue(writer.valuesOf(ServerToClientModel.POPUP_AUTO_HIDE).contains(true));
    }

    @Test
    public void testNoAutoHideNotWritten() {
        writer.clear();
        attachedPopup(false);
        assertFalse((writer.count(ServerToClientModel.POPUP_AUTO_HIDE) > 0));
    }

    @Test
    public void testShowAndHide() {
        final PPopupPanel popup = attachedPopup(false);
        writer.clear();

        popup.show();
        assertTrue(popup.isVisible());
        assertEquals(1, writer.count(ServerToClientModel.OPEN));

        popup.show();
        assertEquals(1, writer.count(ServerToClientModel.OPEN));

        popup.hide();
        assertFalse(popup.isVisible());
        assertEquals(1, writer.count(ServerToClientModel.CLOSE));

        popup.hide();
        assertEquals(1, writer.count(ServerToClientModel.CLOSE));
    }

    @Test
    public void testSetVisibleDelegatesToShowAndHide() {
        final PPopupPanel popup = attachedPopup(false);
        writer.clear();
        popup.setVisible(true);
        assertTrue(popup.isVisible());
        assertTrue((writer.count(ServerToClientModel.OPEN) > 0));
        popup.setVisible(false);
        assertFalse(popup.isVisible());
        assertTrue((writer.count(ServerToClientModel.CLOSE) > 0));
    }

    @Test
    public void testCenter() {
        final PPopupPanel popup = attachedPopup(false);
        writer.clear();
        popup.center();
        assertTrue(popup.isVisible());
        assertTrue((writer.count(ServerToClientModel.CENTER) > 0));
    }

    @Test
    public void testClose() {
        final PPopupPanel popup = attachedPopup(false);
        popup.show();
        writer.clear();
        popup.close();
        assertFalse(popup.isVisible());
        assertNull(popup.getParent());
        assertTrue((writer.count(ServerToClientModel.CLOSE) > 0));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(popup.getID()));
    }

    @Test
    public void testProperties() {
        final PPopupPanel popup = attachedPopup(false);
        writer.clear();

        popup.setModal(true);
        popup.setDraggable(true);
        popup.setGlassEnabled(true);
        popup.setPopupPosition(10, 20);

        assertTrue(writer.valuesOf(ServerToClientModel.MODAL).contains(true));
        assertTrue(writer.valuesOf(ServerToClientModel.DRAGGABLE).contains(true));
        assertTrue(writer.valuesOf(ServerToClientModel.POPUP_GLASS_ENABLED).contains(true));
        assertTrue(writer.valuesOf(ServerToClientModel.POSITION_LEFT).contains(10));
        assertTrue(writer.valuesOf(ServerToClientModel.POSITION_TOP).contains(20));
    }

    @Test
    public void testGlassStyleName() {
        final PPopupPanel popup = attachedPopup(false);
        writer.clear();

        popup.setGlassStyleName("glass");

        assertTrue(writer.valuesOf(ServerToClientModel.POPUP_GLASS_STYLE_NAME).contains("glass"));
    }

    @Test
    public void testAnimationEnabled() {
        final PPopupPanel popup = attachedPopup(false);
        writer.clear();
        popup.setAnimationEnabled(true);
        assertTrue(popup.isAnimationEnabled());
        popup.setAnimationEnabled(true);
        assertEquals(1, writer.count(ServerToClientModel.ANIMATION));
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATION).contains(true));
    }

    @Test
    public void testUpdatesStackedUntilAttached() {
        writer.clear();
        final PPopupPanel popup = Element.newPPopupPanel();
        popup.setModal(true);
        assertFalse((writer.count(ServerToClientModel.MODAL) > 0));
        PWindow.getMain().add(popup);
        assertTrue(writer.valuesOf(ServerToClientModel.MODAL).contains(true));
    }

    @Test
    public void testPopupPositionAndShow() {
        final PPopupPanel popup = attachedPopup(false);
        writer.clear();
        final int[] received = new int[4];
        popup.setPopupPositionAndShow((offsetWidth, offsetHeight, windowWidth, windowHeight) -> {
            received[0] = offsetWidth;
            received[1] = offsetHeight;
            received[2] = windowWidth;
            received[3] = windowHeight;
        });
        assertTrue(popup.isVisible());
        assertTrue(writer.valuesOf(ServerToClientModel.HANDLER_TYPE).contains(HandlerModel.HANDLER_POPUP_POSITION.getValue()));

        writer.clear();
        popup.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.POPUP_POSITION.toStringValue(), Json.createArrayBuilder().add(100).add(50).add(800).add(600))
            .build());
        assertArrayEquals(new int[] { 100, 50, 800, 600 }, received);
        assertTrue(popup.isVisible());
        assertTrue((writer.count(ServerToClientModel.OPEN) > 0));
        assertTrue((writer.count(ServerToClientModel.POPUP_POSITION_AND_SHOW) > 0));
    }

    @Test
    public void testCloseFromClient() {
        final PPopupPanel popup = attachedPopup(false);
        final List<PCloseEvent> events = new ArrayList<>();
        popup.addCloseHandler(events::add);
        popup.show();
        popup.onClientData(Json.createObjectBuilder().addNull(ClientToServerModel.HANDLER_CLOSE.toStringValue()).build());
        assertFalse(popup.isVisible());
        assertEquals(1, events.size());
        assertSame(popup, events.get(0).getSource());
    }

    @Test
    public void testClientDataIgnoredWhenHidden() {
        final PPopupPanel popup = attachedPopup(false);
        final List<PCloseEvent> events = new ArrayList<>();
        popup.addCloseHandler(events::add);
        popup.onClientData(Json.createObjectBuilder().addNull(ClientToServerModel.HANDLER_CLOSE.toStringValue()).build());
        assertTrue(events.isEmpty());
    }

    @Test
    public void testOtherClientDataDelegated() {
        final PPopupPanel popup = attachedPopup(false);
        popup.show();
        final int[] clicks = new int[1];
        popup.addDomHandler((com.ponysdk.core.ui.basic.event.PClickHandler) event -> clicks[0]++,
            com.ponysdk.core.ui.basic.event.PClickEvent.TYPE);
        com.ponysdk.test.PEmulator.click(popup);
        assertEquals(1, clicks[0]);
    }

}
