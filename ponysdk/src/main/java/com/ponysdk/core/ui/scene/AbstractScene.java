
package com.ponysdk.core.ui.scene;

import com.ponysdk.core.ui.basic.PWidget;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractScene implements Scene {

    private static final String STYLE = "scene";

    private PWidget widget;

    private boolean started = false;
    private boolean firstStart = true;

    private final String id;
    private final String name;
    private final String token;

    private List<Listener> listeners;

    protected AbstractScene(final String id, final String name, String token) {
        this.id = id;
        this.name = name;
        this.token = token;
    }

    @Override
    public final void start() {
        if (started) return;
        fireStarting();

        if (firstStart) {
            try {
                if (widget == null) {
                    widget = buildGUI();
                }
                if (!widget.isInitialized()) {
                    widget.addInitializeListener(this::onInit);
                } else {
                    onInit(widget);
                }
            } finally {
                firstStart = false;
            }
        } else {
            started();
        }
    }

    private void started() {
        started = true;
        widget.setAttribute("started", "true");
        onStart();
        fireStarted();
    }

    private void onInit(Object w) {
        onFirstStart();
        initHandlers();
        started();
    }

    @Override
    public final void stop() {
        if (!started) return;
        fireStopping();
        onStop();
        widget.setAttribute("started", "false");
        started = false;
        fireStopped();
    }

    @Override
    public PWidget asWidget() {
        if (widget == null) {
            widget = buildGUI();
            widget.setAttribute("started", "false");
            widget.addStyleName(STYLE);
        }
        return widget;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void addLifeCycleListener(Listener listener) {
        if (listeners == null) listeners = new ArrayList<>();
        listeners.add(listener);
    }

    private void fireStarting() {
        listeners.forEach(l -> l.starting(this));
    }

    private void fireStarted() {
        listeners.forEach(l -> l.started(this));
    }

    private void fireStopping() {
        listeners.forEach(l -> l.stopping(this));
    }

    private void fireStopped() {
        listeners.forEach(l -> l.stopped(this));
    }

    @Override
    public void removeLifeCycleListener(Listener listener) {
        if (listeners != null) listeners.remove(listener);
    }

    protected void initHandlers() {
    }

    protected void onFirstStart() {
    }

    protected void onStart() {
    }

    protected void onStop() {
    }

    public abstract PWidget buildGUI();

}
