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

package com.ponysdk.impl.java.server;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.test.PSuite;
import com.ponysdk.core.ui.main.EntryPoint;

public class JavaApplicationManagerTest extends PSuite {

    private JavaApplicationManager applicationManager;
    private ApplicationConfiguration configuration;

    public static final class SampleEntryPoint implements EntryPoint {

        @Override
        public void start(final UIContext uiContext) {
        }
    }

    @Before
    public void setUp() {
        applicationManager = new JavaApplicationManager();
        configuration = new ApplicationConfiguration();
        applicationManager.setConfiguration(configuration);
        assertEquals(configuration, applicationManager.getConfiguration());
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.application.ApplicationManager#startApplication(com.ponysdk.core.server.application.UIContext)}.
     */
    @Test
    public void testStartApplication() throws Exception {
        configuration.setEntryPointClass(SampleEntryPoint.class);
        applicationManager.startApplication(UIContext.get());

        assertEquals(SampleEntryPoint.class, applicationManager.initializeEntryPoint().getClass());
    }

    /**
     * Test method for {@link com.ponysdk.impl.java.server.JavaApplicationManager#start()}.
     */
    @Test
    public void testStart() {
        applicationManager.start();
    }

}
