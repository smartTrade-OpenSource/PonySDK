package com.ponysdk.core.ui.flexlayout;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.ModelWriterForTest;
import com.ponysdk.test.PSuite;

/** Tool zones: what {@link FlexLayoutAddon#setToolZone} and its panel delegates send to the terminal. */
public class FlexToolZoneTest extends PSuite {

    private static final String UNPARKED = "fl-pony-unparked";

    /** Records the add-on terminal calls (PADDON_METHOD + PADDON_ARGUMENTS), keeping the emulation of the test writer. */
    private static final class Recorder extends ModelWriterForTest {

        private static final class Call {

            final int objectId;
            final String method;
            Object[] args = new Object[0];

            Call(final int objectId, final String method) {
                this.objectId = objectId;
                this.method = method;
            }

            @Override
            public String toString() {
                return method + Arrays.toString(args);
            }
        }

        private final List<Call> calls = new ArrayList<>();
        private int currentId;

        @Override
        public void write(final ServerToClientModel model, final Object value) {
            if (model == ServerToClientModel.TYPE_CREATE || model == ServerToClientModel.TYPE_UPDATE) {
                currentId = (int) value;
            } else if (model == ServerToClientModel.PADDON_METHOD) {
                calls.add(new Call(currentId, (String) value));
            } else if (model == ServerToClientModel.PADDON_ARGUMENTS) {
                calls.get(calls.size() - 1).args = (Object[]) value;
            }
            super.write(model, value);
        }

        List<Call> calls(final PObject object) {
            return calls.stream().filter(c -> c.objectId == object.getID()).collect(Collectors.toList());
        }

        void assertLast(final PObject object, final String method, final Object... args) {
            final List<Call> own = calls(object);
            assertFalse("no terminal call recorded", own.isEmpty());
            final Call call = own.get(own.size() - 1);
            assertEquals(method, call.method);
            assertArrayEquals(call.toString(), args, call.args);
        }

        void clear() {
            calls.clear();
        }
    }

    private Recorder recorder;
    private FlexLayoutPanel panel;
    private FlexLayoutAddon addon;

    @Before
    public void setUp() {
        recorder = new Recorder();
        Mockito.doReturn(recorder).when(UIContext.get()).getWriter();
        panel = new FlexLayoutPanel();
        addon = panel.getAddon();
        PWindow.getMain().add(panel.asWidget());
    }

    @After
    public void tearDown() {
        Mockito.doReturn(new ModelWriterForTest()).when(UIContext.get()).getWriter();
    }

    private static String id(final PWidget w) {
        return String.valueOf(w.getID());
    }

    @Test
    public void aToolZoneWidgetIsParkedByTheTerminalInItsZone() {
        final PLabel title = Element.newPLabel("App");
        addon.setToolZone(FlexToolZone.TOP_LEFT, title);
        recorder.assertLast(addon, "setToolZone", "top-left", id(title));
        // A content widget like a tab's: in the panel, its id forced, hidden until the terminal moves it to its zone
        assertSame(addon.getPanel(), title.getParent());
        assertTrue(title.hasStyleName(UNPARKED));
        assertSame(title, addon.getToolZone(FlexToolZone.TOP_LEFT));
        assertNull(addon.getToolZone(FlexToolZone.BOTTOM_LEFT));
    }

    @Test
    public void everyToolZoneHasItsBarAndItsPlaceOnTheWire() {
        final String[] expected = { "top-left", "top-center", "top-right", "bottom-left", "bottom-center", "bottom-right" };
        final FlexToolZone[] zones = FlexToolZone.values();
        assertEquals(expected.length, zones.length);
        for (int i = 0; i < zones.length; i++) {
            final PLabel w = Element.newPLabel();
            addon.setToolZone(zones[i], w);
            recorder.assertLast(addon, "setToolZone", expected[i], id(w));
        }
    }

    @Test
    public void aToolZoneHoldsOneWidgetTheNextReplacesIt() {
        final PLabel first = Element.newPLabel();
        final PLabel second = Element.newPLabel();
        addon.setToolZone(FlexToolZone.BOTTOM_RIGHT, first);
        addon.setToolZone(FlexToolZone.BOTTOM_RIGHT, second);
        recorder.assertLast(addon, "setToolZone", "bottom-right", id(second));
        assertNull(first.getParent());
        assertSame(second, addon.getToolZone(FlexToolZone.BOTTOM_RIGHT));

        // The same widget again: nothing to do
        recorder.clear();
        addon.setToolZone(FlexToolZone.BOTTOM_RIGHT, second);
        assertTrue(recorder.calls(addon).isEmpty());
    }

    @Test
    public void aWidgetSetInAnotherZoneMovesThere() {
        final PLabel clock = Element.newPLabel();
        addon.setToolZone(FlexToolZone.TOP_RIGHT, clock);
        addon.setToolZone(FlexToolZone.BOTTOM_CENTER, clock);
        recorder.assertLast(addon, "setToolZone", "bottom-center", id(clock));
        assertSame(addon.getPanel(), clock.getParent());
        assertNull(addon.getToolZone(FlexToolZone.TOP_RIGHT));
        assertSame(clock, addon.getToolZone(FlexToolZone.BOTTOM_CENTER));
    }

    @Test
    public void clearingAToolZoneReleasesItsWidget() {
        final PLabel status = Element.newPLabel();
        addon.setToolZone(FlexToolZone.BOTTOM_LEFT, status);
        addon.clearToolZone(FlexToolZone.BOTTOM_LEFT);
        recorder.assertLast(addon, "clearToolZone", "bottom-left");
        assertNull(status.getParent());
        assertNull(addon.getToolZone(FlexToolZone.BOTTOM_LEFT));

        // An empty zone: nothing travels; null clears like clearToolZone
        recorder.clear();
        addon.clearToolZone(FlexToolZone.BOTTOM_LEFT);
        assertTrue(recorder.calls(addon).isEmpty());
        final PLabel other = Element.newPLabel();
        addon.setToolZone(FlexToolZone.TOP_CENTER, other);
        addon.setToolZone(FlexToolZone.TOP_CENTER, null);
        recorder.assertLast(addon, "clearToolZone", "top-center");
        assertNull(other.getParent());
    }

    @Test
    public void toolZonesAreNotPartOfTheLayoutModel() {
        final PLabel menu = Element.newPLabel();
        addon.setToolZone(FlexToolZone.TOP_RIGHT, menu);
        addon.addTab("t1", "Tab", Element.newPLabel());
        addon.loadModel("{\"type\":\"row\"}");
        // Loading a model releases the tabs' widgets, not the application chrome
        assertTrue(addon.getTabWidgets().isEmpty());
        assertSame(addon.getPanel(), menu.getParent());
        assertSame(menu, addon.getToolZone(FlexToolZone.TOP_RIGHT));
    }

    @Test
    public void thePanelDelegatesToolZonesToItsAddon() {
        final PLabel menu = Element.newPLabel();
        panel.setToolZone(FlexToolZone.TOP_RIGHT, menu);
        recorder.assertLast(addon, "setToolZone", "top-right", id(menu));
        assertSame(menu, panel.getToolZone(FlexToolZone.TOP_RIGHT));

        panel.clearToolZone(FlexToolZone.TOP_RIGHT);
        recorder.assertLast(addon, "clearToolZone", "top-right");
        assertNull(panel.getToolZone(FlexToolZone.TOP_RIGHT));
    }
}
