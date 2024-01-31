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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.ui.main.EntryPoint;

public class ApplicationConfigurationTest {

    private ApplicationConfiguration config;

    @Before
    public void setUp() {
        config = new ApplicationConfiguration();
        assertNotNull(config.toString());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getApplicationID()}.
     */
    @Test
    public void testGetApplicationID() {
        final String expected = "ID";
        config.setApplicationID(expected);
        assertEquals(expected, config.getApplicationID());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getApplicationName()}.
     */
    @Test
    public void testGetApplicationName() {
        final String expected = "NAME";
        config.setApplicationName(expected);
        assertEquals(expected, config.getApplicationName());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getApplicationDescription()}.
     */
    @Test
    public void testGetApplicationDescription() {
        final String expected = "NAME";
        config.setApplicationDescription(expected);
        assertEquals(expected, config.getApplicationDescription());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getApplicationContextName()}.
     */
    @Test
    public void testGetApplicationContextName() {
        final String expected = "NAME";
        config.setApplicationContextName(expected);
        assertEquals(expected, config.getApplicationContextName());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getHeartBeatPeriod()}.
     */
    @Test
    public void testGetHeartBeatPeriod() {
        final long expected = 123;
        config.setHeartBeatPeriod(expected);
        assertEquals(expected, config.getHeartBeatPeriod());
        assertEquals(TimeUnit.MILLISECONDS, config.getHeartBeatPeriodTimeUnit());
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.application.ApplicationConfiguration#getHeartBeatPeriodTimeUnit()}.
     */
    @Test
    public void testGetHeartBeatPeriodTimeUnit() {
        final long expected = 123;
        final TimeUnit expectedUnit = TimeUnit.SECONDS;
        config.setHeartBeatPeriod(expected, expectedUnit);
        assertEquals(expected, config.getHeartBeatPeriod());
        assertEquals(expectedUnit, config.getHeartBeatPeriodTimeUnit());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getSessionTimeout()}.
     */
    @Test
    public void testGetSessionTimeout() {
        final int expected = 321;
        config.setSessionTimeout(expected);
        assertEquals(expected, config.getSessionTimeout());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getJavascript()}.
     */
    @Test
    public void testGetJavascript() {
        final Set<String> expected = Set.of("a.js", "b.js");
        config.setJavascript(expected);
        assertEquals(expected, config.getJavascript());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getStyle()}.
     */
    @Test
    public void testGetStyle() {
        final Map<String, String> expected = Map.of("a", "a.css", "b", "b.css");
        config.setStyle(expected);
        assertEquals(expected, config.getStyle());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getMeta()}.
     */
    @Test
    public void testGetMeta() {
        final Set<String> expected = Set.of("a.js", "b.js");
        config.setMeta(expected);
        assertEquals(expected, config.getMeta());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getEntryPointClass()}.
     */
    @Test
    public void testGetEntryPointClass() {
        final Class<EntryPoint> expected = EntryPoint.class;
        config.setEntryPointClass(expected);
        assertEquals(expected, config.getEntryPointClass());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#getClientConfigFile()}.
     */
    @Test
    public void testGetClientConfigFile() {
        final String expected = "NAME";
        config.setClientConfigFile(expected);
        assertEquals(expected, config.getClientConfigFile());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#isDebugMode()}.
     */
    @Test
    public void testIsDebugMode() {
        final boolean expected = true;
        config.setDebugMode(expected);
        assertEquals(expected, config.isDebugMode());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.application.ApplicationConfiguration#isTabindexOnlyFormField()}.
     */
    @Test
    public void testIsTabindexOnlyFormField() {
        final boolean expected = true;
        config.setTabindexOnlyFormField(expected);
        assertEquals(expected, config.isTabindexOnlyFormField());
    }

}
