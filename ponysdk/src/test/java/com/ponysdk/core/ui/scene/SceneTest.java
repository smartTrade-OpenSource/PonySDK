/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.ui.scene;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.test.PSuite;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SceneTest extends PSuite {

    @Test
    public void testScene() {
        final Scene scene = new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("scene");
            }
        };
        assertEquals("id", scene.getId());
        assertEquals("name", scene.getName());
        assertEquals("token", scene.getToken());
        assertFalse(scene.isStarted());
    }

    @Test
    public void testSceneLifeCycle() {
        final Scene scene = new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("scene");
            }
        };
        PWindow.getMain().add(scene);

        SceneListener4Test listener4Test = new SceneListener4Test();
        scene.addLifeCycleListener(listener4Test);

        assertFalse(scene.isStarted());
        scene.start();
        assertEquals(1, listener4Test.startingCount);
        assertEquals(1, listener4Test.startedCount);
        assertEquals(0, listener4Test.stoppingCount);
        assertEquals(0, listener4Test.stoppedCount);
        assertTrue(scene.isStarted());
        scene.stop();
        assertEquals(1, listener4Test.startingCount);
        assertEquals(1, listener4Test.startedCount);
        assertEquals(1, listener4Test.stoppingCount);
        assertEquals(1, listener4Test.stoppedCount);
        assertFalse(scene.isStarted());
        scene.start();
        assertEquals(2, listener4Test.startingCount);
        assertEquals(2, listener4Test.startedCount);
        assertEquals(1, listener4Test.stoppingCount);
        assertEquals(1, listener4Test.stoppedCount);
        assertTrue(scene.isStarted());
        scene.stop();
        assertEquals(2, listener4Test.startingCount);
        assertEquals(2, listener4Test.startedCount);
        assertEquals(2, listener4Test.stoppingCount);
        assertEquals(2, listener4Test.stoppedCount);
        assertFalse(scene.isStarted());
    }

    @Test
    public void testStartAndStopWithoutListener() {
        final Scene scene = labelScene();
        PWindow.getMain().add(scene);
        scene.start();
        assertTrue(scene.isStarted());
        assertEquals("true", scene.asWidget().getAttribute("started"));
        scene.stop();
        assertFalse(scene.isStarted());
        assertEquals("false", scene.asWidget().getAttribute("started"));
    }

    @Test
    public void testStartBeforeAttachBuildsTheSceneWidget() {
        final int[] firstStarts = new int[1];
        final Scene scene = new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("scene");
            }

            @Override
            protected void onFirstStart() {
                firstStarts[0]++;
            }
        };
        final SceneListener4Test listener = new SceneListener4Test();
        scene.addLifeCycleListener(listener);

        scene.start();
        // not attached yet: started once the widget is initialized
        assertFalse(scene.isStarted());
        assertEquals(1, listener.startingCount);
        assertEquals(0, listener.startedCount);
        assertTrue(scene.asWidget().hasStyleName("scene"));

        PWindow.getMain().add(scene);
        assertTrue(scene.isStarted());
        assertEquals(1, listener.startedCount);
        assertEquals(1, firstStarts[0]);

        scene.start();
        assertEquals(1, listener.startingCount);
    }

    @Test
    public void testStopWhenNotStartedAndRemoveListener() {
        final Scene scene = labelScene();
        scene.removeLifeCycleListener(new SceneListener4Test());
        final SceneListener4Test listener = new SceneListener4Test();
        scene.addLifeCycleListener(listener);
        scene.stop();
        assertEquals(0, listener.stoppingCount);

        scene.removeLifeCycleListener(listener);
        PWindow.getMain().add(scene);
        scene.start();
        assertTrue(scene.isStarted());
        assertEquals(0, listener.startingCount);

        final SceneListener4Test other = new SceneListener4Test();
        scene.addLifeCycleListener(listener);
        scene.addLifeCycleListener(other);
        scene.stop();
        assertEquals(1, listener.stoppedCount);
        assertEquals(1, other.stoppedCount);
    }

    private static Scene labelScene() {
        return new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("scene");
            }
        };
    }

    private class SceneListener4Test implements Scene.Listener {
        int startingCount = 0;
        int startedCount = 0;
        int stoppingCount = 0;
        int stoppedCount = 0;

        @Override
        public void starting(Scene source) {
            startingCount++;
        }

        @Override
        public void started(Scene source) {
            startedCount++;
        }

        @Override
        public void stopping(Scene source) {
            stoppingCount++;
        }

        @Override
        public void stopped(Scene source) {
            stoppedCount++;
        }


        public void reset() {
            startingCount = 0;
            startedCount = 0;
            stoppingCount = 0;
            stoppedCount = 0;
        }
    }

}
