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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.ui.main.EntryPoint;

public class ApplicationConfigurationTest {

    private ApplicationConfiguration config;

    @BeforeEach
    public void setUp() {
        config = new ApplicationConfiguration();
        assertNotNull(config.toString());
    }

    /**
     * Pins the WebSocket hardening defaults — notably the secure-by-default Origin check — and
     * verifies the setters round-trip.
     */
    @Test
    public void testWebSocketConfigDefaultsAndSetters() {
        // Secure / safe defaults
        assertTrue(config.isWsOriginCheckEnabled(), "Origin check (anti-CSWSH) must be on by default");
        assertTrue(config.isWsPermessageDeflateEnabled(), "permessage-deflate on by default");
        assertEquals(1 << 20, config.getWsMaxInboundMessageSize());
        assertEquals(1_000_000L, config.getWsIdleTimeoutMs());
        assertEquals(60_000L, config.getWsSendTimeoutMs());

        // Setters round-trip
        config.setWsOriginCheckEnabled(false);
        assertFalse(config.isWsOriginCheckEnabled());
        config.setWsPermessageDeflateEnabled(false);
        assertFalse(config.isWsPermessageDeflateEnabled());
        config.setWsAllowedOrigins(Set.of("https://app.example.com"));
        assertTrue(config.getWsAllowedOrigins().contains("https://app.example.com"));
        config.setWsMaxInboundMessageSize(4096);
        assertEquals(4096, config.getWsMaxInboundMessageSize());
        config.setWsSendTimeoutMs(5_000);
        assertEquals(5_000L, config.getWsSendTimeoutMs());
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

    @Test
    public void testBuilderAppliesEverySetting() {
        final ApplicationConfiguration.ReconnectionListener listener = uiContext -> {};
        final ApplicationConfiguration built = ApplicationConfiguration.builder()
            .applicationID("id")
            .applicationName("name")
            .applicationDescription("description")
            .applicationContextName("context")
            .entryPointClass(EntryPoint.class)
            .sessionTimeout(42)
            .heartBeatPeriod(7, TimeUnit.SECONDS)
            .enableClientToServerHeartBeat(false)
            .reconnectionTimeoutMs(15_000)
            .maxRecordingEntries(99)
            .reconnectionListener(listener)
            .javascript(Set.of("a.js"))
            .style(Map.of("main", "main.css"))
            .meta(Set.of("viewport"))
            .wcSharedSheets(Set.of("shared.css"))
            .servedResourceExtensions(".JSON")
            .contentSecurityPolicyEnabled(false)
            .scriptSources("https://cdn.example.com")
            .debugMode(true)
            .tabindexOnlyFormField(true)
            .legacyClassNames(true)
            .stringDictionaryEnabled(false)
            .stringDictionaryMaxSize(128)
            .stringDictionaryMinLength(2)
            .stringDictionaryMaxEvictionsPerFlush(5)
            .stringDictionaryAdmitOnSecondUse(false)
            .stringDictionaryPersistenceEnabled(false)
            .stringDictionaryPersistPath("dict")
            .stringDictionaryPreSeedSize(16)
            .wsOriginCheckEnabled(false)
            .wsAllowedOrigins(Set.of("https://host.example.com"))
            .wsMaxInboundMessageSize(2048)
            .wsInputBufferSize(512)
            .wsDeflateLevel(-1)
            .maxProtocolVersion(ProtocolVersion.V1)
            .updateCoalescingEnabled(false)
            .wsIdleTimeoutMs(30_000)
            .wsSendTimeoutMs(1_000)
            .wsPermessageDeflateEnabled(false)
            .build();

        assertEquals("id", built.getApplicationID());
        assertEquals("name", built.getApplicationName());
        assertEquals("description", built.getApplicationDescription());
        assertEquals("context", built.getApplicationContextName());
        assertEquals(EntryPoint.class, built.getEntryPointClass());
        assertEquals(42, built.getSessionTimeout());
        assertEquals(7, built.getHeartBeatPeriod());
        assertEquals(TimeUnit.SECONDS, built.getHeartBeatPeriodTimeUnit());
        assertFalse(built.isEnableClientToServerHeartBeat());
        assertEquals(15_000, built.getReconnectionTimeoutMs());
        assertEquals(99, built.getMaxRecordingEntries());
        assertSame(listener, built.getReconnectionListener());
        assertEquals(Set.of("a.js"), built.getJavascript());
        assertEquals(Map.of("main", "main.css"), built.getStyle());
        assertEquals(Set.of("viewport"), built.getMeta());
        assertEquals(Set.of("shared.css"), built.getWcSharedSheets());
        assertTrue(built.getServedResourceExtensions().contains("json"));
        assertFalse(built.isContentSecurityPolicyEnabled());
        assertEquals(Set.of("https://cdn.example.com"), built.getScriptSources());
        assertTrue(built.isDebugMode());
        assertTrue(built.isTabindexOnlyFormField());
        assertTrue(built.isLegacyClassNames());
        assertFalse(built.isStringDictionaryEnabled());
        assertEquals(128, built.getStringDictionaryMaxSize());
        assertEquals(2, built.getStringDictionaryMinLength());
        assertEquals(5, built.getStringDictionaryMaxEvictionsPerFlush());
        assertFalse(built.isStringDictionaryAdmitOnSecondUse());
        assertFalse(built.isStringDictionaryPersistenceEnabled());
        assertEquals("dict", built.getStringDictionaryPersistPath());
        assertEquals(16, built.getStringDictionaryPreSeedSize());
        assertFalse(built.isWsOriginCheckEnabled());
        assertEquals(Set.of("https://host.example.com"), built.getWsAllowedOrigins());
        assertEquals(2048, built.getWsMaxInboundMessageSize());
        assertEquals(512, built.getWsInputBufferSize());
        assertEquals(-1, built.getWsDeflateLevel());
        assertEquals(ProtocolVersion.V1, built.getMaxProtocolVersion());
        assertFalse(built.isUpdateCoalescingEnabled());
        assertEquals(30_000, built.getWsIdleTimeoutMs());
        assertEquals(1_000, built.getWsSendTimeoutMs());
        assertFalse(built.isWsPermessageDeflateEnabled());
    }

    @Test
    public void testServedResourceExtensionsAreWebFilesOnlyByDefaultAndReadOnly() {
        final Set<String> extensions = config.getServedResourceExtensions();
        assertTrue(extensions.contains("js"));
        assertTrue(extensions.contains("woff2"));
        assertFalse(extensions.contains("class"));
        assertFalse(extensions.contains("properties"));
        assertThrows(UnsupportedOperationException.class, () -> extensions.add("class"));

        config.addServedResourceExtensions("Json", ".TXT");
        assertTrue(config.getServedResourceExtensions().contains("json"));
        assertTrue(config.getServedResourceExtensions().contains("txt"));
    }

    @Test
    public void testScriptSourcesKeepInsertionOrderAndAreReadOnly() {
        config.addScriptSources("https://b.example.com", "https://a.example.com");
        assertEquals(java.util.List.of("https://b.example.com", "https://a.example.com"), java.util.List.copyOf(config.getScriptSources()));
        assertThrows(UnsupportedOperationException.class, () -> config.getScriptSources().clear());
    }

    @Test
    public void testMaxProtocolVersionIsBounded() {
        assertEquals(ProtocolVersion.CURRENT, config.getMaxProtocolVersion());
        assertThrows(IllegalArgumentException.class, () -> config.setMaxProtocolVersion(ProtocolVersion.V1 - 1));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxProtocolVersion(ProtocolVersion.CURRENT + 1));
        config.setMaxProtocolVersion(ProtocolVersion.CURRENT);
        assertEquals(ProtocolVersion.CURRENT, config.getMaxProtocolVersion());
    }

    @Test
    public void testDeflateLevelIsBounded() {
        assertEquals(3, config.getWsDeflateLevel());
        assertThrows(IllegalArgumentException.class, () -> config.setWsDeflateLevel(-2));
        assertThrows(IllegalArgumentException.class, () -> config.setWsDeflateLevel(10));
        config.setWsDeflateLevel(9);
        assertEquals(9, config.getWsDeflateLevel());
    }

    @Test
    public void testSessionAccessPolicyDefaultsToSameHttpSessionAndRejectsNull() {
        assertSame(SessionAccessPolicy.SAME_HTTP_SESSION, config.getSessionAccessPolicy());
        assertThrows(NullPointerException.class, () -> config.setSessionAccessPolicy(null));
        assertSame(SessionAccessPolicy.SAME_HTTP_SESSION, config.getSessionAccessPolicy());
    }
}
