/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ponysdk.core.ui.main.EntryPoint;

public class ApplicationConfiguration {

    public static final String APPLICATION_ID = "ponysdk.application.id";
    public static final String APPLICATION_NAME = "ponysdk.application.name";
    public static final String APPLICATION_DESCRIPTION = "ponysdk.application.description";
    public static final String APPLICATION_CONTEXT_NAME = "ponysdk.application.context.name";
    public static final String STYLESHEETS = "ponysdk.application.stylesheets";
    public static final String JAVASCRIPTS = "ponysdk.application.javascripts";
    public static final String POINTCLASS = "ponysdk.entry.point.class";

    private String applicationID;
    private String applicationName;
    private String applicationDescription;
    private String applicationContextName = "sample";
    private long heartBeatPeriod = 5000;// ms
    private TimeUnit heartBeatPeriodTimeUnit = TimeUnit.MILLISECONDS;
    private boolean enableClientToServerHeartBeat = true;

    private int sessionTimeout = 15; // minutes

    private Set<String> meta;
    private Map<String, String> style;
    private Set<String> javascript;

    private Class<? extends EntryPoint> entryPointClass;

    private String clientConfigFile;

    private boolean debugMode;

    private boolean tabindexOnlyFormField;

    // String Dictionary configuration for protocol optimization
    private boolean stringDictionaryEnabled = true;
    private int stringDictionaryMaxSize = 4096;
    private int stringDictionaryMinLength = 4;
    private boolean stringDictionaryPersistenceEnabled = true;
    private String stringDictionaryPersistPath = "data";
    private int stringDictionaryPreSeedSize = 512;

    public ApplicationConfiguration() {
        applicationID = System.getProperty(APPLICATION_ID);
        applicationName = System.getProperty(APPLICATION_NAME);
        applicationDescription = System.getProperty(APPLICATION_DESCRIPTION);
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(final String applicationID) {
        this.applicationID = applicationID;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationDescription() {
        return applicationDescription;
    }

    public void setApplicationDescription(final String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    public String getApplicationContextName() {
        return applicationContextName;
    }

    public void setApplicationContextName(final String applicationContextName) {
        this.applicationContextName = applicationContextName;
    }

    public void setHeartBeatPeriod(final long heartBeatPeriod, final TimeUnit heartBeatPeriodTimeUnit) {
        this.heartBeatPeriod = heartBeatPeriod;
        this.heartBeatPeriodTimeUnit = heartBeatPeriodTimeUnit;
    }

    public long getHeartBeatPeriod() {
        return heartBeatPeriod;
    }

    public void setHeartBeatPeriod(final long heartBeatPeriod) {
        setHeartBeatPeriod(heartBeatPeriod, TimeUnit.MILLISECONDS);
    }

    public TimeUnit getHeartBeatPeriodTimeUnit() {
        return heartBeatPeriodTimeUnit;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(final int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Set<String> getJavascript() {
        return javascript;
    }

    public void setJavascript(final Set<String> javascript) {
        this.javascript = javascript;
    }

    public Map<String, String> getStyle() {
        return style;
    }

    public void setStyle(final Map<String, String> style) {
        this.style = style;
    }

    public Set<String> getMeta() {
        return meta;
    }

    public void setMeta(final Set<String> meta) {
        this.meta = meta;
    }

    public Class<? extends EntryPoint> getEntryPointClass() {
        return entryPointClass;
    }

    public void setEntryPointClass(final Class<? extends EntryPoint> entryPointClass) {
        this.entryPointClass = entryPointClass;
    }

    public String getClientConfigFile() {
        return clientConfigFile;
    }

    public void setClientConfigFile(final String clientConfigFile) {
        this.clientConfigFile = clientConfigFile;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(final boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isTabindexOnlyFormField() {
        return tabindexOnlyFormField;
    }

    public void setTabindexOnlyFormField(final boolean tabindexOnlyFormField) {
        this.tabindexOnlyFormField = tabindexOnlyFormField;
    }

    // Web Component shared stylesheets — injected into every PonyElement shadow root
    private Set<String> wcSharedSheets;

    /**
     * Server-side hook called when a UIContext WebSocket disconnects.
     * Default: null (no action — the client handles reconnection via JS hooks).
     *
     * <p>Use this to log the event, trigger alerts, or clean up server-side resources.
     * When transparent reconnection is enabled, the UIContext is suspended (not destroyed)
     * at the time this listener fires — do NOT call {@code execute()} on it.</p>
     *
     * <p>For client-side reconnection behaviour, use the JavaScript hooks:
     * <ul>
     *   <li>{@code document.onConnectionLost(callback)} — fired immediately on disconnect</li>
     *   <li>{@code window.showReconnectionInformation()} — override to show custom UI</li>
     *   <li>{@code window.onPonyReconnected()} — override to replace the default page reload</li>
     * </ul>
     * </p>
     */
    private ReconnectionListener reconnectionListener;

    public interface ReconnectionListener {
        /**
         * Called server-side when a UIContext WebSocket is lost.
         * If transparent reconnection is enabled, the UIContext is suspended (still alive)
         * at this point. Otherwise it is about to be destroyed.
         *
         * @param uiContext the affected UIContext — do not call execute() on it
         */
        void onConnectionLost(UIContext uiContext);
    }

    public Set<String> getWcSharedSheets() {
        return wcSharedSheets;
    }

    /**
     * Registers one or more CSS stylesheet URLs to be shared across all Web Components
     * that extend {@code PonyElement}. These sheets are injected into each shadow root
     * automatically, enabling a global Design System without breaking shadow DOM isolation.
     * <p>
     * If never called, no shared styles are injected — fully opt-in.
     * </p>
     */
    public void setWcSharedSheets(final Set<String> wcSharedSheets) {
        this.wcSharedSheets = wcSharedSheets;
    }

    public ReconnectionListener getReconnectionListener() {
        return reconnectionListener;
    }

    public void setReconnectionListener(final ReconnectionListener reconnectionListener) {
        this.reconnectionListener = reconnectionListener;
    }

    /**
     * Enables transparent WebSocket reconnection.
     * When > 0, a disconnected UIContext is kept alive for this duration (ms)
     * instead of being destroyed. If the client reconnects within this window,
     * the existing UIContext and all widget state are reused — no page reload.
     *
     * <p>Default: 0 (disabled — standard destroy-on-disconnect behaviour).</p>
     *
     * <p>Recommended values: 5000–30000ms depending on your network tolerance.</p>
     *
     * <p><strong>Not recommended for trading applications</strong> where stale market
     * data must never be silently resumed. Use the JS hooks instead to show an explicit
     * "reconnected" acknowledgement.</p>
     */
    private long reconnectionTimeoutMs = 0;

    public long getReconnectionTimeoutMs() {
        return reconnectionTimeoutMs;
    }

    public void setReconnectionTimeoutMs(final long reconnectionTimeoutMs) {
        this.reconnectionTimeoutMs = reconnectionTimeoutMs;
    }

    /**
     * Maximum number of protocol entries the {@code RecordingEncoder} may buffer
     * during a suspended UIContext. If exceeded, the UIContext is destroyed immediately
     * to prevent unbounded memory growth.
     *
     * <p>Each entry is ~32 bytes on-heap (enum ref + value ref). Default 10_000 entries
     * ≈ 320 KB per suspended session — safe for most deployments.</p>
     *
     * <p>Set to 0 to disable the limit (not recommended in production).</p>
     */
    private int maxRecordingEntries = 10_000;

    public int getMaxRecordingEntries() {
        return maxRecordingEntries;
    }

    public void setMaxRecordingEntries(final int maxRecordingEntries) {
        this.maxRecordingEntries = maxRecordingEntries;
    }

    @Override
    public String toString() {
        return "ApplicationManagerOption [heartBeatPeriod=" + heartBeatPeriod + " " + heartBeatPeriodTimeUnit + "]";
    }

    public boolean isEnableClientToServerHeartBeat() {
        return enableClientToServerHeartBeat;
    }

    public void setEnableClientToServerHeartBeat(final boolean enableClientToServerHeartBeat) {
        this.enableClientToServerHeartBeat = enableClientToServerHeartBeat;
    }

    /**
     * Returns whether the string dictionary optimization is enabled.
     *
     * @return true if string dictionary is enabled (default: true)
     */
    public boolean isStringDictionaryEnabled() {
        return stringDictionaryEnabled;
    }

    /**
     * Enables or disables the string dictionary optimization.
     *
     * @param stringDictionaryEnabled true to enable, false to disable
     */
    public void setStringDictionaryEnabled(final boolean stringDictionaryEnabled) {
        this.stringDictionaryEnabled = stringDictionaryEnabled;
    }

    /**
     * Returns the maximum number of entries in the string dictionary.
     *
     * @return the maximum size (default: 65535)
     */
    public int getStringDictionaryMaxSize() {
        return stringDictionaryMaxSize;
    }

    /**
     * Sets the maximum number of entries in the string dictionary.
     *
     * @param stringDictionaryMaxSize the maximum size (must be positive)
     */
    public void setStringDictionaryMaxSize(final int stringDictionaryMaxSize) {
        this.stringDictionaryMaxSize = stringDictionaryMaxSize;
    }

    /**
     * Returns the minimum string length required for dictionary interning.
     *
     * @return the minimum length (default: 4)
     */
    public int getStringDictionaryMinLength() {
        return stringDictionaryMinLength;
    }

    /**
     * Sets the minimum string length required for dictionary interning.
     * Strings shorter than this will be sent as raw strings.
     *
     * @param stringDictionaryMinLength the minimum length (must be non-negative)
     */
    public void setStringDictionaryMinLength(final int stringDictionaryMinLength) {
        this.stringDictionaryMinLength = stringDictionaryMinLength;
    }

    /**
     * Returns whether dictionary persistence is enabled.
     * When enabled, string frequencies are persisted to disk so that
     * new sessions benefit from previously learned string patterns.
     *
     * @return true if persistence is enabled (default: true)
     */
    public boolean isStringDictionaryPersistenceEnabled() {
        return stringDictionaryPersistenceEnabled;
    }

    /**
     * Enables or disables dictionary persistence.
     *
     * @param stringDictionaryPersistenceEnabled true to enable
     */
    public void setStringDictionaryPersistenceEnabled(final boolean stringDictionaryPersistenceEnabled) {
        this.stringDictionaryPersistenceEnabled = stringDictionaryPersistenceEnabled;
    }

    /**
     * Returns the directory path where the dictionary file is persisted.
     *
     * @return the persist path (default: "data")
     */
    public String getStringDictionaryPersistPath() {
        return stringDictionaryPersistPath;
    }

    /**
     * Sets the directory path for dictionary persistence.
     *
     * @param stringDictionaryPersistPath the directory path
     */
    public void setStringDictionaryPersistPath(final String stringDictionaryPersistPath) {
        this.stringDictionaryPersistPath = stringDictionaryPersistPath;
    }

    /**
     * Returns the maximum number of strings to pre-seed from the persisted dictionary.
     *
     * @return the pre-seed size (default: 512)
     */
    public int getStringDictionaryPreSeedSize() {
        return stringDictionaryPreSeedSize;
    }

    /**
     * Sets the maximum number of strings to pre-seed from the persisted dictionary.
     *
     * @param stringDictionaryPreSeedSize the pre-seed size
     */
    public void setStringDictionaryPreSeedSize(final int stringDictionaryPreSeedSize) {
        this.stringDictionaryPreSeedSize = stringDictionaryPreSeedSize;
    }

}
