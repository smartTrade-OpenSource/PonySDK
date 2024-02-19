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

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.framework.PSuite;
import com.ponysdk.core.ui.basic.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class RouterTest extends PSuite {

    @Test
    public void testBadRoute() {
        final Router router = new Router("testBadRoute");
        router.go("fake");
        assertNotEquals("fake", UIContext.get().getHistory().getToken());
    }

    @Test
    public void testGoodRouteWithoutLayout() {
        final Router router = new Router("testGoodRouteWithoutLayout");
        router.push(new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return null;
            }
        });
        router.go("token");
        assertEquals("token", UIContext.get().getHistory().getToken());
    }

    @Test
    public void testGoodRouteWithLayout() {
        final Router router = new Router("testGoodRouteWithLayout");
        router.setLayout(Element.newPSimplePanel());
        router.push(new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("tests");
            }
        });
        router.go("token");
        assertEquals("token", UIContext.get().getHistory().getToken());
    }

    @Test
    public void testHistoryChangedWithoutAnAssociatedScene() {
        final Router router = new Router("test");
        router.setLayout(Element.newPSimplePanel());
        AbstractScene scene = new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("tests");
            }
        };
        router.push(scene);
        UIContext.get().getHistory().newItem("fake", true);

        assertFalse(scene.isStarted());
    }

    @Test
    public void testHistoryChangedWithAnAssociatedScene() {
        final Router router = new Router("testHistoryChangedWithAnAssociatedScene");
        final PSimplePanel layout = Element.newPSimplePanel();
        router.setLayout(layout);

        AbstractScene scene = new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("tests");
            }
        };
        router.push(scene);
        PWindow.getMain().add(layout);

        UIContext.get().getHistory().newItem("token", true);

        assertTrue(scene.isStarted());
    }


    @Test
    public void testHistoryChangedWithoutLayout() {
        final Router router = new Router("testHistoryChangedWithoutLayout");
        AbstractScene scene = new AbstractScene("id", "name", "token") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("tests");
            }
        };
        router.push(scene);
        UIContext.get().getHistory().newItem("token", true);

        assertFalse(scene.isStarted());
    }

    @Test
    public void testSceneNavigation() {
        final Router router = new Router("testSceneNavigation");
        final PSimplePanel layout = Element.newPSimplePanel();
        router.setLayout(layout);

        AbstractScene scene1 = new AbstractScene("id1", "name1", "token1") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("test1");
            }
        };

        AbstractScene scene2 = new AbstractScene("id2", "name2", "token2") {
            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("test2");
            }
        };
        router.push(scene1);
        router.push(scene2);

        PWindow.getMain().add(layout);

        UIContext.get().getHistory().newItem("token1", true);
        assertTrue(scene1.isStarted());
        assertFalse(scene2.isStarted());
        UIContext.get().getHistory().newItem("token2", true);
        assertFalse(scene1.isStarted());
        assertTrue(scene2.isStarted());
    }

}
