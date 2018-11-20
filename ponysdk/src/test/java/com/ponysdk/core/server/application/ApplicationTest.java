/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.server.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationTest {

    private Application application;

    @Before
    public void setUp() {
        final String id = "id";
        final ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        application = new Application(id, Mockito.mock(HttpSession.class), configuration);
        assertEquals(id, application.getId());
        assertEquals(configuration, application.getOptions());
        assertNotNull(application.toString());
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.application.Application#registerUIContext(com.ponysdk.core.server.application.UIContext)}.
     */
    @Test
    public void testRegisterUIContext() {
        final int uiContextID1 = 1;
        final UIContext uiContext1 = Mockito.mock(UIContext.class);
        Mockito.when(uiContext1.getID()).thenReturn(uiContextID1);
        application.registerUIContext(uiContext1);

        final int uiContextID2 = 2;
        final UIContext uiContext2 = Mockito.mock(UIContext.class);
        Mockito.when(uiContext2.getID()).thenReturn(uiContextID2);
        application.registerUIContext(uiContext2);

        assertEquals(2, application.countUIContexts());
        assertEquals(2, application.getUIContexts().size());
        assertEquals(uiContext1, application.getUIContext(uiContextID1));
        assertEquals(uiContext2, application.getUIContext(uiContextID2));
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.Application#deregisterUIContext(int)}.
     */
    @Test
    public void testDeregisterUIContext() {
        final int uiContextID1 = 1;
        final UIContext uiContext1 = Mockito.mock(UIContext.class);
        Mockito.when(uiContext1.getID()).thenReturn(uiContextID1);
        application.registerUIContext(uiContext1);

        final int uiContextID2 = 2;
        final UIContext uiContext2 = Mockito.mock(UIContext.class);
        Mockito.when(uiContext2.getID()).thenReturn(uiContextID2);
        application.registerUIContext(uiContext2);
        application.deregisterUIContext(uiContext2.getID());

        assertEquals(1, application.countUIContexts());
        assertEquals(1, application.getUIContexts().size());
        assertEquals(uiContext1, application.getUIContext(uiContextID1));
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.Application#destroy()}.
     */
    @Test
    public void testDestroy() {
        final int uiContextID1 = 1;
        final UIContext uiContext1 = Mockito.mock(UIContext.class);
        Mockito.when(uiContext1.getID()).thenReturn(uiContextID1);
        application.registerUIContext(uiContext1);

        application.destroy();

        assertEquals(0, application.countUIContexts());
        assertEquals(0, application.getUIContexts().size());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.Application#pushToClients(java.lang.Object)}.
     */
    @Test
    public void testPushToClients() {
        final int uiContextID1 = 1;
        final UIContext uiContext1 = Mockito.mock(UIContext.class);
        Mockito.when(uiContext1.getID()).thenReturn(uiContextID1);
        application.registerUIContext(uiContext1);

        final Object message = new Object();
        application.pushToClients(message);
        Mockito.verify(uiContext1).pushToClient(message);
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.application.Application#setAttribute(java.lang.String, java.lang.Object)}.
     */
    @Test
    public void testSetAttribute() {
        final String name = "A";
        final String value = "B";
        application.setAttribute(name, value);
        assertEquals(value, application.getAttribute(name));
    }

}
