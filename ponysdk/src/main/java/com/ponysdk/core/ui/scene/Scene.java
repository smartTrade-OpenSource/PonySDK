package com.ponysdk.core.ui.scene;

import com.ponysdk.core.ui.basic.IsPWidget;

import java.util.EventListener;

public interface Scene extends IsPWidget {
    String getId();

    String getName();

    String getToken();

    void start();

    void stop();

    boolean isStarted();

    void addLifeCycleListener(Scene.Listener listener);

    void removeLifeCycleListener(Scene.Listener listener);

    interface Listener extends EventListener {
        void starting(Scene source);

        void started(Scene source);

        void stopping(Scene source);

        void stopped(Scene source);
    }
}
