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

package com.ponysdk.core.server.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mockito;

public class PonyServiceRegistryTest {

    private static interface ServiceA extends PonyService {
    }

    private static interface ServiceB extends PonyService {
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.service.PonyServiceRegistry#registerPonyService(com.ponysdk.core.server.service.PonyService)}.
     */
    @Test
    public void testRegisterPonyService() {
        final PonyService service1 = Mockito.mock(ServiceA.class);
        PonyServiceRegistry.registerPonyService(service1);

        final PonyService service2 = Mockito.mock(ServiceB.class);
        PonyServiceRegistry.registerPonyService(service2);

        assertEquals(service1, PonyServiceRegistry.getPonyService(ServiceA.class));
        assertEquals(service2, PonyServiceRegistry.getPonyService(ServiceB.class));
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.service.PonyServiceRegistry#registerPonyService(com.ponysdk.core.server.service.PonyService)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetNotUnregistedPonyService() {
        PonyServiceRegistry.getPonyService(ServiceA.class);
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.service.PonyServiceRegistry#registerPonyService(com.ponysdk.core.server.service.PonyService)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegistedPonyServiceTwice() {
        final PonyService service1 = Mockito.mock(ServiceA.class);
        PonyServiceRegistry.registerPonyService(service1);
        PonyServiceRegistry.registerPonyService(service1);
    }

}
