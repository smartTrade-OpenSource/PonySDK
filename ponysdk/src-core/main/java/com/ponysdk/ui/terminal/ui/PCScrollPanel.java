
package com.ponysdk.ui.terminal.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.ponysdk.ui.terminal.addon.floatablepanel.PCFloatablePanel;

public class PCScrollPanel extends ScrollPanel implements ScrollHandler, RequiresResize, ResizeHandler {

    private final List<PCFloatablePanel> floatablePanels = new ArrayList<PCFloatablePanel>();

    public PCScrollPanel() {
        Window.addResizeHandler(this);
        addScrollHandler(this);
    }

    @Override
    public void onScroll(ScrollEvent event) {
        if (!isAttached()) return;
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                fireOnResize();
            }
        });
    }

    private void fireOnResize() {
        for (PCFloatablePanel floatablePanel : floatablePanels) {
            floatablePanel.onResize();
        }
    }

    public void addFloatablePanel(PCFloatablePanel floatablePanel) {
        floatablePanels.add(floatablePanel);
    }

    @Override
    public void onResize() {
        super.onResize();
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                fireOnResize();
            }
        });
    }

    @Override
    public void onResize(ResizeEvent event) {
        super.onResize();
        fireOnResize();
    }

}
