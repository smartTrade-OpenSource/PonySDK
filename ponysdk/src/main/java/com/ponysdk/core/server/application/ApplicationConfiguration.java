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

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    /** {@link Terminal} name, case-insensitive: {@code -Dponysdk.terminal=typescript}. */
    public static final String TERMINAL = "ponysdk.terminal";

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
    /** See {@link #setUpdateCoalescingEnabled(boolean)}. */
    private boolean updateCoalescingEnabled = true;
    private int stringDictionaryMaxSize = 4096;
    /**
     * Strings shorter than this are always sent inline. 8 (was 4): with permessage-deflate on, short
     * strings compress well inline, while interning them costs a DICTIONARY_ADD per distinct value and
     * eviction churn for short unique values (prices, quantities, times). Measured on 4 workloads under
     * deflate: -7% bytes overall (-11% on unique numeric values, +1% on repeated 5-7 char strings).
     */
    private int stringDictionaryMinLength = 8;
    private int stringDictionaryMaxEvictionsPerFlush = 32;
    private boolean stringDictionaryAdmitOnSecondUse = true;
    private boolean stringDictionaryPersistenceEnabled = true;
    private String stringDictionaryPersistPath = "data";
    private int stringDictionaryPreSeedSize = 512;

    // ── WebSocket transport hardening ─────────────────────────────────────
    /** When true, the WebSocket upgrade validates the Origin header (anti-CSWSH). */
    private boolean wsOriginCheckEnabled = true;
    /** Explicit allow-list of accepted Origins. When empty, only same-origin is accepted. */
    private Set<String> wsAllowedOrigins;
    /** Upper bound on inbound (client -> server) text messages, in bytes. Guards against memory abuse. */
    private int wsMaxInboundMessageSize = 1 << 20; // 1 MB
    /**
     * Jetty input buffer size for client -> server WebSocket messages. Jetty sizes the text builder of
     * every incoming message with it (default 4 KB, re-allocated per message), while PonySDK's
     * upstream traffic is small JSON (heartbeats, latency replies, events): 1 KB avoids ~5 KB of
     * garbage per incoming message. Larger messages still work (the builder grows).
     */
    private int wsInputBufferSize = 1024;
    /**
     * zlib level of permessage-deflate (embedded PonySDKServer). Jetty's default is 6; on PonySDK's
     * stream of small binary frames level 3 was measured both cheaper (-25% CPU per pushed update)
     * and as compact (1.448 vs 1.474 MB/s deflated). 0-9, or -1 for Jetty's default.
     */
    private int wsDeflateLevel = 3;
    /**
     * Highest binary protocol version the server uses (see ProtocolVersion). Each connection uses
     * min(this, version announced by the terminal); 1 forces the original format for everyone.
     */
    private int maxProtocolVersion = com.ponysdk.core.model.ProtocolVersion.CURRENT;
    /** WebSocket idle timeout in milliseconds (no traffic either way before close). */
    private long wsIdleTimeoutMs = 1_000_000;
    /** Max time to await a previous (slow-consumer) send before disconnecting, in milliseconds. */
    private long wsSendTimeoutMs = 60_000;
    /** When false, permessage-deflate is disabled on the WebSocket upgrade (no frame compression). */
    private boolean wsPermessageDeflateEnabled = true;

    public ApplicationConfiguration() {
        applicationID = System.getProperty(APPLICATION_ID);
        applicationName = System.getProperty(APPLICATION_NAME);
        applicationDescription = System.getProperty(APPLICATION_DESCRIPTION);
        final String terminalName = System.getProperty(TERMINAL);
        if (terminalName != null) terminal = Terminal.valueOf(terminalName.trim().toUpperCase(Locale.ROOT));
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
     *   <li>a page element with id {@code reconnection} — shown while the connection is lost (its child with id
     *   {@code reconnecting} receives the message), hidden once transparent reconnection succeeds</li>
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

    /**
     * Who may act on which session through {@link SessionAdmin} (ADR-0001). Default
     * {@link SessionAccessPolicy#SAME_HTTP_SESSION}: a caller acts only on its own tabs and frames. An operator console
     * acting on every user needs an explicit policy, typically a role read from the caller's session.
     */
    private SessionAccessPolicy sessionAccessPolicy = SessionAccessPolicy.SAME_HTTP_SESSION;

    /** The terminal the bootstrap page loads (ADR-0002). {@link Terminal#TYPESCRIPT} unless {@value #TERMINAL} says otherwise. */
    private Terminal terminal = Terminal.TYPESCRIPT;

    public Terminal getTerminal() {
        return terminal;
    }

    public void setTerminal(final Terminal terminal) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
    }

    public SessionAccessPolicy getSessionAccessPolicy() {
        return sessionAccessPolicy;
    }

    public void setSessionAccessPolicy(final SessionAccessPolicy sessionAccessPolicy) {
        this.sessionAccessPolicy = Objects.requireNonNull(sessionAccessPolicy, "sessionAccessPolicy");
    }

    @Override
    public String toString() {
        return "ApplicationManagerOption [heartBeatPeriod=" + heartBeatPeriod + " " + heartBeatPeriodTimeUnit + "]";
    }

    // ── Fluent Builder ────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final ApplicationConfiguration config = new ApplicationConfiguration();

        public Builder applicationID(final String v)               { config.setApplicationID(v); return this; }
        public Builder applicationName(final String v)             { config.setApplicationName(v); return this; }
        public Builder applicationDescription(final String v)      { config.setApplicationDescription(v); return this; }
        public Builder applicationContextName(final String v)      { config.setApplicationContextName(v); return this; }
        public Builder entryPointClass(final Class<? extends EntryPoint> v) { config.setEntryPointClass(v); return this; }
        public Builder sessionTimeout(final int v)                 { config.setSessionTimeout(v); return this; }
        public Builder heartBeatPeriod(final long v, final TimeUnit u) { config.setHeartBeatPeriod(v, u); return this; }
        public Builder enableClientToServerHeartBeat(final boolean v) { config.setEnableClientToServerHeartBeat(v); return this; }
        public Builder reconnectionTimeoutMs(final long v)         { config.setReconnectionTimeoutMs(v); return this; }
        public Builder maxRecordingEntries(final int v)            { config.setMaxRecordingEntries(v); return this; }
        public Builder reconnectionListener(final ReconnectionListener v) { config.setReconnectionListener(v); return this; }
        public Builder javascript(final Set<String> v)             { config.setJavascript(v); return this; }
        public Builder style(final Map<String, String> v)          { config.setStyle(v); return this; }
        public Builder meta(final Set<String> v)                   { config.setMeta(v); return this; }
        public Builder wcSharedSheets(final Set<String> v)         { config.setWcSharedSheets(v); return this; }
        public Builder debugMode(final boolean v)                  { config.setDebugMode(v); return this; }
        public Builder tabindexOnlyFormField(final boolean v)      { config.setTabindexOnlyFormField(v); return this; }
        public Builder stringDictionaryEnabled(final boolean v)    { config.setStringDictionaryEnabled(v); return this; }
        public Builder stringDictionaryMaxSize(final int v)        { config.setStringDictionaryMaxSize(v); return this; }
        public Builder stringDictionaryMinLength(final int v)      { config.setStringDictionaryMinLength(v); return this; }
        public Builder stringDictionaryMaxEvictionsPerFlush(final int v) { config.setStringDictionaryMaxEvictionsPerFlush(v); return this; }
        public Builder stringDictionaryAdmitOnSecondUse(final boolean v) { config.setStringDictionaryAdmitOnSecondUse(v); return this; }
        public Builder stringDictionaryPersistenceEnabled(final boolean v) { config.setStringDictionaryPersistenceEnabled(v); return this; }
        public Builder stringDictionaryPersistPath(final String v) { config.setStringDictionaryPersistPath(v); return this; }
        public Builder stringDictionaryPreSeedSize(final int v)    { config.setStringDictionaryPreSeedSize(v); return this; }

        public Builder wsOriginCheckEnabled(final boolean v)       { config.setWsOriginCheckEnabled(v); return this; }
        public Builder wsAllowedOrigins(final Set<String> v)       { config.setWsAllowedOrigins(v); return this; }
        public Builder wsMaxInboundMessageSize(final int v)        { config.setWsMaxInboundMessageSize(v); return this; }
        public Builder wsInputBufferSize(final int v)              { config.setWsInputBufferSize(v); return this; }
        public Builder wsDeflateLevel(final int v)                 { config.setWsDeflateLevel(v); return this; }
        public Builder maxProtocolVersion(final int v)             { config.setMaxProtocolVersion(v); return this; }
        public Builder updateCoalescingEnabled(final boolean v)    { config.setUpdateCoalescingEnabled(v); return this; }
        public Builder wsIdleTimeoutMs(final long v)               { config.setWsIdleTimeoutMs(v); return this; }
        public Builder wsSendTimeoutMs(final long v)               { config.setWsSendTimeoutMs(v); return this; }
        public Builder wsPermessageDeflateEnabled(final boolean v) { config.setWsPermessageDeflateEnabled(v); return this; }
        public Builder terminal(final Terminal v)                  { config.setTerminal(v); return this; }

        public ApplicationConfiguration build() {
            return config;
        }
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
    public boolean isUpdateCoalescingEnabled() {
        return updateCoalescingEnabled;
    }

    /**
     * Coalesces "last value wins" property updates (text, html, title, size, style / attribute /
     * property values) made within one transaction: only the final value of each property is sent,
     * and consecutive updates of one widget share one update block. Intermediate values were never
     * painted by the browser, so this only matters to client code reacting to each intermediate DOM
     * change (e.g. a MutationObserver or a custom element's attributeChangedCallback). Default: on.
     */
    public void setUpdateCoalescingEnabled(final boolean updateCoalescingEnabled) {
        this.updateCoalescingEnabled = updateCoalescingEnabled;
    }

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

    public int getStringDictionaryMaxEvictionsPerFlush() {
        return stringDictionaryMaxEvictionsPerFlush;
    }

    public void setStringDictionaryMaxEvictionsPerFlush(final int stringDictionaryMaxEvictionsPerFlush) {
        this.stringDictionaryMaxEvictionsPerFlush = stringDictionaryMaxEvictionsPerFlush;
    }

    public boolean isStringDictionaryAdmitOnSecondUse() {
        return stringDictionaryAdmitOnSecondUse;
    }

    /**
     * When true (default), a string enters the per-session dictionary on its second use only: values
     * written once are sent inline (no DICTIONARY_ADD, no slot, no retained String), and admitted
     * strings share one instance across sessions. False = admit on first use (previous behaviour).
     */
    public void setStringDictionaryAdmitOnSecondUse(final boolean admitOnSecondUse) {
        this.stringDictionaryAdmitOnSecondUse = admitOnSecondUse;
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

    // ── WebSocket transport hardening accessors ───────────────────────────

    public boolean isWsOriginCheckEnabled() {
        return wsOriginCheckEnabled;
    }

    public void setWsOriginCheckEnabled(final boolean wsOriginCheckEnabled) {
        this.wsOriginCheckEnabled = wsOriginCheckEnabled;
    }

    public Set<String> getWsAllowedOrigins() {
        return wsAllowedOrigins;
    }

    /**
     * Sets the allow-list of accepted WebSocket Origins (e.g. {@code "https://app.example.com"}).
     * When null/empty, the upgrade only accepts same-origin requests (Origin authority == Host).
     * Requests without an Origin header (non-browser clients) are always allowed.
     */
    public void setWsAllowedOrigins(final Set<String> wsAllowedOrigins) {
        this.wsAllowedOrigins = wsAllowedOrigins;
    }

    public int getMaxProtocolVersion() {
        return maxProtocolVersion;
    }

    /** @param maxProtocolVersion 1..ProtocolVersion.CURRENT (1 = original format, e.g. as a kill switch) */
    public void setMaxProtocolVersion(final int maxProtocolVersion) {
        if (maxProtocolVersion < com.ponysdk.core.model.ProtocolVersion.V1 || maxProtocolVersion > com.ponysdk.core.model.ProtocolVersion.CURRENT)
            throw new IllegalArgumentException("protocol version must be 1.." + com.ponysdk.core.model.ProtocolVersion.CURRENT + ": " + maxProtocolVersion);
        this.maxProtocolVersion = maxProtocolVersion;
    }

    public int getWsDeflateLevel() {
        return wsDeflateLevel;
    }

    /** @param wsDeflateLevel 0 (store) .. 9 (best compression), -1 keeps Jetty's default (6) */
    public void setWsDeflateLevel(final int wsDeflateLevel) {
        if (wsDeflateLevel < -1 || wsDeflateLevel > 9) throw new IllegalArgumentException("deflate level must be -1..9: " + wsDeflateLevel);
        this.wsDeflateLevel = wsDeflateLevel;
    }

    public int getWsInputBufferSize() {
        return wsInputBufferSize;
    }

    /** @param wsInputBufferSize bytes, &lt;= 0 keeps Jetty's default (4 KB) */
    public void setWsInputBufferSize(final int wsInputBufferSize) {
        this.wsInputBufferSize = wsInputBufferSize;
    }

    public int getWsMaxInboundMessageSize() {
        return wsMaxInboundMessageSize;
    }

    public void setWsMaxInboundMessageSize(final int wsMaxInboundMessageSize) {
        this.wsMaxInboundMessageSize = wsMaxInboundMessageSize;
    }

    public long getWsIdleTimeoutMs() {
        return wsIdleTimeoutMs;
    }

    public void setWsIdleTimeoutMs(final long wsIdleTimeoutMs) {
        this.wsIdleTimeoutMs = wsIdleTimeoutMs;
    }

    public long getWsSendTimeoutMs() {
        return wsSendTimeoutMs;
    }

    public void setWsSendTimeoutMs(final long wsSendTimeoutMs) {
        this.wsSendTimeoutMs = wsSendTimeoutMs;
    }

    public boolean isWsPermessageDeflateEnabled() {
        return wsPermessageDeflateEnabled;
    }

    /**
     * Enables/disables permessage-deflate frame compression on the WebSocket upgrade.
     * Disabling trades bandwidth for lower CPU/latency (useful for high-throughput/real-time apps)
     * and is also required by non-browser clients that cannot decode compressed frames.
     */
    public void setWsPermessageDeflateEnabled(final boolean wsPermessageDeflateEnabled) {
        this.wsPermessageDeflateEnabled = wsPermessageDeflateEnabled;
    }

}
