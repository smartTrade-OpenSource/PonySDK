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

package com.ponysdk.core.ui.rich;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPopupPanel;
import com.ponysdk.core.ui.basic.PRootPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.rich.PNotificationManager.Notification;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;

public class PNotificationManagerTest extends PSuite {

    private CapturingModelWriter writer;

    @AfterEach
    public void tearDown() {
        if (writer != null) writer.uninstall();
        final PRootPanel root = PWindow.getMain().getPRootPanel();
        while (root.getWidgetCount() > 0) {
            root.getWidget(0).removeFromParent();
        }
    }

    private static PPopupPanel lastPopup() {
        final PRootPanel root = PWindow.getMain().getPRootPanel();
        return (PPopupPanel) root.getWidget(root.getWidgetCount() - 1);
    }

    @Test
    public void testHumanizedWarningAndErrorAreCentered() {
        PNotificationManager.notify("hello", Notification.HUMANIZED);
        PPopupPanel popup = lastPopup();
        assertTrue(popup.hasStyleName("pony-notification"));
        assertTrue(popup.hasStyleName("humanized"));
        assertTrue(popup.isVisible());
        assertEquals("hello", ((PLabel) popup.getWidget()).getText());

        PNotificationManager.notify(PWindow.getMain(), "careful", Notification.WARNING_MESSAGE);
        popup = lastPopup();
        assertTrue(popup.hasStyleName("warning"));
        assertEquals("careful", ((PLabel) popup.getWidget()).getText());

        PNotificationManager.notify(PWindow.getMain(), "boom", Notification.ERROR_MESSAGE);
        popup = lastPopup();
        assertTrue(popup.hasStyleName("error"));

        // A click closes the notification
        final int count = PWindow.getMain().getPRootPanel().getWidgetCount();
        PEmulator.click(popup);
        assertFalse(popup.isVisible());
        assertEquals(count - 1, PWindow.getMain().getPRootPanel().getWidgetCount());
    }

    @Test
    public void testWidgetNotifications() {
        final PLabel widget = Element.newPLabel("w");
        PNotificationManager.notify(PWindow.getMain(), widget, Notification.HUMANIZED);
        assertSame(widget, lastPopup().getWidget());
        PNotificationManager.notify(PWindow.getMain(), Element.newPLabel(), Notification.WARNING_MESSAGE);
        assertTrue(lastPopup().hasStyleName("warning"));
        PNotificationManager.notify(PWindow.getMain(), Element.newPLabel(), Notification.ERROR_MESSAGE);
        assertTrue(lastPopup().hasStyleName("error"));

        final PLabel trayContent = Element.newPLabel("tray");
        PNotificationManager.notify(PWindow.getMain(), trayContent, Notification.TRAY);
        assertSame(trayContent, ((PSimplePanel) lastPopup().getWidget()).getWidget());

        PNotificationManager.showHumanizedNotification(PWindow.getMain(), "h");
        assertTrue(lastPopup().hasStyleName("humanized"));
        PNotificationManager.showWarningNotification(PWindow.getMain(), "w");
        assertTrue(lastPopup().hasStyleName("warning"));
        PNotificationManager.showErrorNotification(PWindow.getMain(), "e");
        assertTrue(lastPopup().hasStyleName("error"));
        PNotificationManager.showTrayNotification(PWindow.getMain(), "t");
        assertTrue(lastPopup().hasStyleName("tray"));
    }

    @Test
    public void testTrayIsDisplayedAtBottomRight() {
        PNotificationManager.notify(PWindow.getMain(), "tray", Notification.TRAY);
        final PPopupPanel popup = lastPopup();
        assertTrue(popup.hasStyleName("tray"));
        assertEquals("tray", ((PLabel) ((PSimplePanel) popup.getWidget()).getWidget()).getText());
        assertFalse(popup.hasStyleName("closing"));

        writer = CapturingModelWriter.install();
        popup.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.POPUP_POSITION.toStringValue(), Json.createArrayBuilder().add(100).add(40).add(1000).add(800))
            .build());
        assertEquals(List.of(895), writer.valuesOf(ServerToClientModel.POSITION_LEFT));
        assertEquals(List.of(755), writer.valuesOf(ServerToClientModel.POSITION_TOP));
        assertTrue(popup.hasStyleName("closing"));
    }
}
