
package com.ponysdk.core.ui.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.ponysdk.core.model.WidgetType;

public class PFlowPanelTest extends PSuite {

    @Test
    public void testInit() {
        final PFlowPanel widget = new PFlowPanel();
        assertEquals(WidgetType.FLOW_PANEL, widget.getWidgetType());
        assertNotNull(widget.toString());
    }

    @Test
    public void testAdd() {
        final PFlowPanel flowPanel = new PFlowPanel();
        assertEquals(0, flowPanel.getWidgetCount());

        final PWidget widget = new PFlowPanel();
        flowPanel.add(widget);
        assertEquals(widget, flowPanel.getChild(widget.getID()));
        assertEquals(1, flowPanel.getWidgetCount());
    }

    @Test
    public void testAdds() {
        final PFlowPanel flowPanel = new PFlowPanel();

        final PWidget widget1 = new PFlowPanel();
        final PWidget widget2 = new PFlowPanel();
        flowPanel.add(widget1, widget2);
        assertEquals(widget1, flowPanel.getChild(widget1.getID()));
        assertEquals(widget2, flowPanel.getChild(widget2.getID()));
        assertEquals(2, flowPanel.getWidgetCount());
    }

    @Test
    public void testRemove() {
        final PFlowPanel flowPanel = new PFlowPanel();
        assertEquals(0, flowPanel.getWidgetCount());

        final PWidget widget = new PFlowPanel();
        flowPanel.add(widget);
        assertEquals(1, flowPanel.getWidgetCount());

        flowPanel.remove(widget);
        assertNull(flowPanel.getChild(widget.getID()));
        assertEquals(0, flowPanel.getWidgetCount());
    }

    @Test
    public void testInsertPWidget() {
        final PFlowPanel flowPanel = new PFlowPanel();
        assertEquals(0, flowPanel.getWidgetCount());

        final PWidget widget1 = new PFlowPanel();
        flowPanel.insert(widget1, 0);
        final PWidget widget2 = new PFlowPanel();
        flowPanel.insert(widget2, 0);
        assertEquals(2, flowPanel.getWidgetCount());
        assertEquals(widget2, flowPanel.getWidget(0));
        assertEquals(0, flowPanel.getWidgetIndex(widget2));
        assertEquals(widget1, flowPanel.getWidget(1));
        assertEquals(1, flowPanel.getWidgetIndex(widget1));
    }

}
