
package com.ponysdk.core.ui.scene;

import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractScene implements Scene {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractScene.class);
    private static final String STYLE = "scene";

    private PWidget widget;

    private boolean started = false;
    private boolean firstStart = true;
    private boolean destroyed = false;

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
        if (destroyed) {
            LOG.warn("Cannot start a destroyed atom");
            return;
        }

        if (started) return;

        if (firstStart) {
            try {
                if (widget == null) {
                    widget = buildGUI();
                }
                if (!widget.isInitialized()) {
                    widget.addInitializeListener(this::onInit);
                    return;
                } else {
                    onInit(widget);
                }
            } finally {
                firstStart = false;
            }
        }
        started();
    }

    private void started() {
        started = true;
        widget.setAttribute("started", "true");
        onStart();
    }

    private void onInit(PObject w) {
        onFirstStart();
        initHandlers();
        started();
    }

    @Override
    public final void stop() {
        if (destroyed) {
            LOG.warn("Cannot start a destroyed atom");
            return;
        }
        if (!started) return;
        onStop();
        widget.setAttribute("started", "false");
        started = false;
    }

    @Override
    public void destroy() {
        if (started) stop();
        removeHandlers();
        destroyed = true;
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
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void addLifeCycleListener(Listener listener) {
        if (listeners == null) listeners = new ArrayList<>();
    }

    @Override
    public void removeLifeCycleListener(Listener listener) {
        if (listeners != null) listeners.remove(listener);
    }

    protected void initHandlers() {
    }

    protected void removeHandlers() {
    }

    protected void onFirstStart() {
    }

    protected void onStart() {
    }

    protected void onStop() {
    }

    public abstract PWidget buildGUI();

}
