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
package com.ponysdk.core.ui.dropdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.Json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.dropdown.DropDownContainerAddon.DropDownContainerAddonListener;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

public class DropDownContainerAddonTest extends PSuite {

    private RecordingModelWriter writer;
    private PFlowPanel parent;

    @BeforeEach
    public void setUp() {
        writer = RecordingModelWriter.install();
        parent = Element.newPFlowPanel();
        PWindow.getMain().add(parent);
    }

    @AfterEach
    public void tearDown() {
        PWindow.getMain().getPRootPanel().clear();
        RecordingModelWriter.uninstall();
    }

    @Test
    public void creationArgsAndPositionOfDefaultDropDown() {
        final DropDownContainerAddon addon = new DropDownContainerAddon(parent);
        assertFalse(addon.isVisible());

        addon.show();

        assertEquals(List.of(creationArgs(true, false)), writer.values(addon, ServerToClientModel.PADDON_CREATION));
        assertEquals(Map.of("position", "absolute", "top", "0", "left", "0"),
            writer.pairs(addon.asWidget(), ServerToClientModel.PUT_STYLE_KEY, ServerToClientModel.STYLE_VALUE));
    }

    @Test
    public void multilevelDropDownIsNotStuckToTheLeft() {
        final DropDownContainerAddon addon = DropDownContainerAddon.newMultilevelDopdown(parent);

        addon.show();

        assertEquals(List.of(creationArgs(true, true)), writer.values(addon, ServerToClientModel.PADDON_CREATION));
        assertEquals(Map.of("position", "absolute", "top", "0"),
            writer.pairs(addon.asWidget(), ServerToClientModel.PUT_STYLE_KEY, ServerToClientModel.STYLE_VALUE));
    }

    @Test
    public void showAttachesToRootPanelAndHideDetaches() {
        final DropDownContainerAddon addon = new DropDownContainerAddon(parent);

        addon.show();
        assertTrue(addon.isVisible());
        assertSame(PWindow.getMain().getPRootPanel(), addon.asWidget().getParent());
        assertEquals(List.of("setVisible", "updatePosition", "setVisible"), writer.addonMethods(addon));
        assertEquals(List.of(false, true), writer.values(addon, ServerToClientModel.PADDON_ARGUMENTS).stream()
            .map(args -> ((Object[]) args)[0]).toList());

        addon.hide();
        assertFalse(addon.isVisible());
        assertNull(addon.asWidget().getParent());
    }

    @Test
    public void terminalMethodsAreSent() {
        final DropDownContainerAddon addon = new DropDownContainerAddon(parent);
        addon.show();
        writer.clear();

        addon.updatePosition();
        addon.adjustPosition();
        addon.disableSpaceWhenOpened();
        addon.setDropRight();

        assertEquals(List.of("updatePosition", "adjustPosition", "disableSpaceWhenOpened", "setDropRight"), writer.addonMethods(addon));
    }

    @Test
    public void windowEventsHideTheContainerAndNotifyListeners() {
        final DropDownContainerAddon addon = new DropDownContainerAddon(parent);
        final AtomicInteger hidden = new AtomicInteger();
        final DropDownContainerAddonListener listener = hidden::incrementAndGet;
        addon.addListener(listener);

        for (final String event : List.of("resize", "scroll", "click")) {
            addon.show();
            DropDownTestEvents.windowEvent(addon, event);
            assertFalse(addon.isVisible(), event);
        }
        assertEquals(3, hidden.get());

        addon.show();
        DropDownTestEvents.windowEvent(addon, "focus");
        addon.onClientData(Json.createObjectBuilder()
            .add(com.ponysdk.core.model.ClientToServerModel.NATIVE.toStringValue(), Json.createObjectBuilder().add("other", 1))
            .build());
        assertTrue(addon.isVisible());
        assertEquals(3, hidden.get());

        addon.removeListener(listener);
        DropDownTestEvents.windowEvent(addon, "resize");
        assertFalse(addon.isVisible());
        assertEquals(3, hidden.get());
    }

    @Test
    public void contentAndStylesAreAppliedToTheInnerPanel() {
        final DropDownContainerAddon addon = new DropDownContainerAddon(parent);
        final PLabel content = Element.newPLabel();

        addon.add(content);
        addon.addStyleName("style");
        addon.setStyleProperty("width", "10px");
        addon.stopEvent(PEventType.ONCLICK);
        addon.show();

        assertSame(addon.asWidget(), content.getParent());
        assertTrue(addon.asWidget().hasStyleName("style"));
        addon.removeStyleName("style");
        assertFalse(addon.asWidget().hasStyleName("style"));
        assertEquals("10px",
            writer.pairs(addon.asWidget(), ServerToClientModel.PUT_STYLE_KEY, ServerToClientModel.STYLE_VALUE).get("width"));
        assertEquals(List.of(PEventType.ONCLICK.getCode()), writer.values(addon.asWidget(), ServerToClientModel.STOP_EVENT));
    }

    @Test
    public void domHandlerIsRegisteredOnTheInnerPanel() {
        final DropDownContainerAddon addon = new DropDownContainerAddon(parent);
        final AtomicInteger clicks = new AtomicInteger();

        addon.addDomHandler((PClickHandler) e -> clicks.incrementAndGet(), PClickEvent.TYPE);
        PEmulator.click(addon.asWidget());

        assertEquals(1, clicks.get());
    }

    @Test
    public void multiLevelParentAttribute() {
        final DropDownContainerAddon addon = new DropDownContainerAddon(parent);
        final DropDownContainerAddon child = DropDownContainerAddon.newMultilevelDopdown(parent);

        addon.defineAsMultiLevelParent(child);

        assertEquals(String.valueOf(addon.asWidget().getID()), child.asWidget().getAttribute("multilvl-parent"));
    }

    private String creationArgs(final boolean stickLeft, final boolean multiLevel) {
        return Json.createObjectBuilder().add("parentId", parent.getID()).add("stickLeft", stickLeft).add("multiLevel", multiLevel).build()
            .toString();
    }
}
