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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PBeforeSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;
import com.ponysdk.core.server.LogCapture;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PStackLayoutPanelTest extends PSuite {

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
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.EM);
        assertEquals(WidgetType.STACKLAYOUT_PANEL, panel.getWidgetType());
        assertNull(panel.getAnimationDuration());
        PWindow.getMain().add(panel);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(panel.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.UNIT).contains(PUnit.EM.getByteValue()));
    }

    @Test
    public void testChildrenAddedBeforeAttachAreCreatedWithThePanel() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        final PLabel child = Element.newPLabel("c");
        panel.add(child, "<b>header</b>", true, 25);
        assertFalse(child.isInitialized());
        assertSame(panel, child.getParent());

        PWindow.getMain().add(panel);
        assertTrue(child.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(child.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.HTML).contains("<b>header</b>"));
        assertTrue(writer.valuesOf(ServerToClientModel.SIZE).contains(25.0));
    }

    @Test
    public void testAddAfterAttachAndRemove() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        PWindow.getMain().add(panel);
        final PLabel a = Element.newPLabel("a");
        final PLabel b = Element.newPLabel("b");
        panel.add(a, "a", false, 10);
        panel.add(b, "b", false, 10);
        assertTrue(a.isInitialized());

        final List<PWidget> children = new ArrayList<>();
        panel.forEach(children::add);
        assertEquals(List.of(a, b), children);

        writer.clear();
        assertTrue(panel.remove(a));
        assertNull(a.getParent());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(a.getID()));
        assertFalse(panel.remove(a));
    }

    @Test
    public void testAddMovesWidgetFromPreviousParent() {
        final PFlowPanel previous = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel("l");
        previous.add(label);
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        panel.add(label, "h", false, 1);
        assertEquals(0, previous.getWidgetCount());
        assertSame(panel, label.getParent());
    }

    @Test
    public void testClear() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        final PLabel a = Element.newPLabel("a");
        final PLabel b = Element.newPLabel("b");
        panel.add(a, "a", false, 10);
        panel.add(b, "b", false, 10);
        panel.clear();
        assertFalse(panel.iterator().hasNext());
        assertNull(a.getParent());
        assertNull(b.getParent());
    }

    @Test
    public void testSingleArgumentAddIsRejected() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        assertThrows(UnsupportedOperationException.class, () -> panel.add(Element.newPLabel("l")));
        assertThrows(UnsupportedOperationException.class, () -> panel.add((IsPWidget) Element.newPLabel("l")));
    }

    @Test
    public void testSelectionHandlers() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        PWindow.getMain().add(panel);

        writer.clear();
        final PSelectionHandler<Integer> selection = event -> {};
        panel.addSelectionHandler(selection);
        assertTrue(writer.valuesOf(ServerToClientModel.HANDLER_TYPE).contains(HandlerModel.HANDLER_SELECTION.getValue()));
        final PBeforeSelectionEvent.Handler<Integer> before = event -> {};
        panel.addBeforeSelectionHandler(before);
        assertTrue(writer.valuesOf(ServerToClientModel.HANDLER_TYPE).contains(HandlerModel.HANDLER_BEFORE_SELECTION.getValue()));
        assertEquals(2, writer.count(ServerToClientModel.TYPE_ADD_HANDLER));

        writer.clear();
        panel.removeSelectionHandler(selection);
        panel.removeBeforeSelectionHandler(before);
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testShowWidgetAndAnimation() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        PWindow.getMain().add(panel);
        final PLabel child = Element.newPLabel("c");
        panel.add(child, "c", false, 10);

        writer.clear();
        panel.showWidget(child);
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_ID).contains(child.getID()));

        panel.animate(Duration.ofMillis(150));
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATE).contains(150));

        panel.setAnimationDuration(Duration.ofMillis(200));
        assertEquals(Duration.ofMillis(200), panel.getAnimationDuration());
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATION_DURATION).contains(200));

        writer.clear();
        panel.setAnimationDuration(Duration.ofMillis(200));
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testDestroyPropagatesToChildren() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        PWindow.getMain().add(panel);
        final PLabel child = Element.newPLabel("c");
        panel.add(child, "c", false, 10);
        panel.onDestroy();
        assertNull(panel.getWindow());
        assertNull(child.getWindow());
    }

    @Test
    public void aChildFailingToBeDestroyedIsLoggedAndTheNextIsDestroyed() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        PWindow.getMain().add(panel);
        panel.add(new PLabel() {

            @Override
            public void onDestroy() {
                throw new IllegalStateException("failing child");
            }
        }, "failing", false, 10);
        final PLabel next = Element.newPLabel("next");
        panel.add(next, "next", false, 10);

        try (LogCapture log = LogCapture.of(PStackLayoutPanel.class)) {
            panel.onDestroy();
            assertEquals(List.of("ERROR An error occurred while trying to process the destroy event"), log.events());
        }
        assertNull(next.getWindow());
    }

    @Test
    public void testDumpDOM() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        final PLabel child = Element.newPLabel("c");
        panel.add(child, "c", false, 10);
        assertEquals("<div pid=\"" + panel.getID() + "\">" + child.dumpDOM() + "</div>", panel.dumpDOM());
    }

    @Test
    public void aWidgetNeedsAHeader() {
        final PStackLayoutPanel panel = Element.newPStackLayoutPanel(PUnit.PX);
        assertThrows(UnsupportedOperationException.class, () -> panel.add((IsPWidget) Element.newPLabel("l")));
    }
}
