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
     * Called, under the context's lock, when its connection is lost and it is about to be suspended. Not called when
     * reconnection is off: the context is then destroyed, and its destroy listeners are told.
     */
    @FunctionalInterface
    public interface ReconnectionListener {

        void onConnectionLost(UIContext uiContext);
    }

    private ReconnectionListener reconnectionListener;
    private long reconnectionTimeoutMs = 0;
    private int maxRecordingEntries = 10_000;
    private int replayBufferMaxBytes = 4 << 20;

    public ReconnectionListener getReconnectionListener() {
        return reconnectionListener;
    }

    public void setReconnectionListener(final ReconnectionListener reconnectionListener) {
        this.reconnectionListener = reconnectionListener;
    }

    /**
     * How long a UIContext whose WebSocket was lost is kept suspended, waiting for its terminal to reconnect.
     * 0, the default, disables transparent reconnection: the context is destroyed with its WebSocket.
     * <p>
     * The delay counts from when the server sees the loss: a cut with no close is seen by the heartbeat, after up to
     * two heartbeat periods. Acknowledgements travel with the client heartbeat
     * ({@link #isEnableClientToServerHeartBeat()}): without it, each context holds up to
     * {@link #getReplayBufferMaxBytes()}.
     * <p>
     * Not covered: a write that fails while the application writes (the send buffer full for 60 s on a dead
     * connection, or a failed asynchronous send found by the next write) still destroys the context, since the failure
     * may fall inside a block, which a suspension would cut in two; the page reloads as before.
     * <p>
     * A page that shows its own reconnection panel ({@code window.showReconnectionInformation}) must also define
     * {@code window.hideReconnectionInformation}, called once the context is resumed.
     */
    public long getReconnectionTimeoutMs() {
        return reconnectionTimeoutMs;
    }

    public void setReconnectionTimeoutMs(final long reconnectionTimeoutMs) {
        this.reconnectionTimeoutMs = reconnectionTimeoutMs;
    }

    /**
     * The protocol entries a suspended UIContext may hold, after compaction, before it is destroyed instead of
     * resumed.
     */
    public int getMaxRecordingEntries() {
        return maxRecordingEntries;
    }

    public void setMaxRecordingEntries(final int maxRecordingEntries) {
        this.maxRecordingEntries = maxRecordingEntries;
    }

    /**
     * The bytes sent to a terminal and not acknowledged yet that a UIContext holds at most, to send them again if
     * the terminal reconnects. A terminal that lost more than this cannot resume: it reloads its page. At least
     * 16 KB.
     */
    public int getReplayBufferMaxBytes() {
        return replayBufferMaxBytes;
    }

    public void setReplayBufferMaxBytes(final int replayBufferMaxBytes) {
        this.replayBufferMaxBytes = replayBufferMaxBytes;
    }

}
