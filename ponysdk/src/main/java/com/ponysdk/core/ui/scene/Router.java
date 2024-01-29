package com.ponysdk.core.ui.scene;

import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Router : synchronise a vue (scene) to the browser history
 * To navigate between scene, Router use History callback mechanism
 * We ca also navigate programmatically to a specific scene
 */
public class Router implements PValueChangeHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final String name;
    private Map<String, Scene> scenes = new HashMap<>();
    private Scene activeScene;
    private PSimplePanel layout;
    private SceneListener listener = new SceneListener();

    public Router(String name) {
        this.name = name;
        UIContextImpl.get().getHistory().addValueChangeHandler(this);
    }

    public void push(Scene scene) {
        scenes.put(scene.getToken(), scene);
        scene.addLifeCycleListener(listener);
    }

    public void go(String sceneID) {
        if (!scenes.containsKey(sceneID)) {
            log.warn("scene {} not found in the router {}", sceneID, name);
        } else {
            UIContextImpl.get().getHistory().newItem(sceneID, true);
        }
    }

    @Override
    public void onValueChange(PValueChangeEvent<String> event) {
        final Scene scene = scenes.get(event.getData());

        if (scene == null) {
            return;
        }

        if (layout == null) {
            log.warn("Layout not set in the router {}", name);
            return;
        }

        if (activeScene == null || !Objects.equals(activeScene.getId(), scene.getId())) {
            selectScene(scene);
        }
    }

    private void selectScene(Scene scene) {
        if (activeScene != null) {
            activeScene.stop();
        }

        activeScene = scene;
        layout.setWidget(activeScene);
        activeScene.start();
    }

    public void setLayout(PSimplePanel layout) {
        this.layout = layout;
    }

    private class SceneListener implements Scene.Listener {

        @Override
        public void starting(Scene source) {
        }

        @Override
        public void started(Scene source) {
        }

        @Override
        public void stopping(Scene source) {
        }

        @Override
        public void stopped(Scene source) {
        }
    }
}
