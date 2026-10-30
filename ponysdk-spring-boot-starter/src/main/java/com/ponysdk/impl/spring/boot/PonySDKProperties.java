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

package com.ponysdk.impl.spring.boot;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;

import com.ponysdk.core.server.application.ApplicationConfiguration;

/**
 * The {@code ponysdk.*} properties. A property left out keeps the default of {@link ApplicationConfiguration}, which
 * {@link PonySDKAutoConfiguration} builds from these; a setting without a property here is reached by declaring one's own
 * {@code ApplicationConfiguration} bean.
 */
@ConfigurationProperties("ponysdk")
public class PonySDKProperties {

    /** The application's id, name (the page's title) and description. */
    private String applicationId;
    private String applicationName;
    private String applicationDescription;

    /** The path the application is served under: {@code http://host:port/<name>/}. */
    private String applicationContextName;

    /** Transparent reconnection window: a connection cut for less than this resumes its screen; unset or 0 disables it. */
    private Duration reconnectionTimeout;

    /** Each element names its Java class and creation line, and the inspector shows over the page: not for production. */
    private Boolean debugMode;

    /**
     * The key, Base64 of 32 bytes, that turns on the transfer of a moved page's declared state (ADR-0018): the same on
     * every server, from a secret store (an environment variable, Spring Cloud Vault...), never in a repository.
     */
    private String stateTransferKey;

    /** Adds the GWT class names the 2.x stylesheets used. */
    private Boolean legacyClassNames;

    /** The language of the bootstrap page, a BCP 47 tag: a screen reader reads with its voice. English by default. */
    private String language;

    /** {@code <meta>} attributes of the bootstrap page, e.g. {@code name="viewport" content="width=device-width"}. */
    private Set<String> meta;

    /** Scripts of the bootstrap page, in load order, relative to the application. */
    private Set<String> javascript;

    /** Stylesheets of the bootstrap page by id, in order: the id and the stylesheet's path. */
    private Map<String, String> style;

    /** Stylesheets injected into every web component's shadow root. */
    private Set<String> wcSharedSheets;

    /** More file types served from the classpath, without their dot, e.g. {@code json}. */
    private Set<String> servedResourceExtensions;

    /**
     * On a graceful shutdown ({@code server.shutdown=graceful}), the live pages move to the other servers over this time
     * before the server stops (ADR-0017); 0 stops at once. Keep it under {@code spring.lifecycle.timeout-per-shutdown-phase}.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration drainTimeout = Duration.ofSeconds(10);

    private final ContentSecurityPolicy contentSecurityPolicy = new ContentSecurityPolicy();
    private final WebSocket websocket = new WebSocket();
    private final StringDictionary stringDictionary = new StringDictionary();

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final String applicationId) {
        this.applicationId = applicationId;
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

    public Duration getReconnectionTimeout() {
        return reconnectionTimeout;
    }

    public void setReconnectionTimeout(final Duration reconnectionTimeout) {
        this.reconnectionTimeout = reconnectionTimeout;
    }

    public String getStateTransferKey() {
        return stateTransferKey;
    }

    public void setStateTransferKey(final String stateTransferKey) {
        this.stateTransferKey = stateTransferKey;
    }

    public Boolean getDebugMode() {
        return debugMode;
    }

    public void setDebugMode(final Boolean debugMode) {
        this.debugMode = debugMode;
    }

    public Boolean getLegacyClassNames() {
        return legacyClassNames;
    }

    public void setLegacyClassNames(final Boolean legacyClassNames) {
        this.legacyClassNames = legacyClassNames;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public Set<String> getMeta() {
        return meta;
    }

    public void setMeta(final Set<String> meta) {
        this.meta = meta;
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

    public Set<String> getWcSharedSheets() {
        return wcSharedSheets;
    }

    public void setWcSharedSheets(final Set<String> wcSharedSheets) {
        this.wcSharedSheets = wcSharedSheets;
    }

    public Set<String> getServedResourceExtensions() {
        return servedResourceExtensions;
    }

    public void setServedResourceExtensions(final Set<String> servedResourceExtensions) {
        this.servedResourceExtensions = servedResourceExtensions;
    }

    public Duration getDrainTimeout() {
        return drainTimeout;
    }

    public void setDrainTimeout(final Duration drainTimeout) {
        this.drainTimeout = drainTimeout;
    }

    public ContentSecurityPolicy getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    public WebSocket getWebsocket() {
        return websocket;
    }

    public StringDictionary getStringDictionary() {
        return stringDictionary;
    }

    /** The Content-Security-Policy of the bootstrap page (ADR-0015). */
    public static class ContentSecurityPolicy {

        /** The bootstrap page carries a policy; off for an application that sends its own. */
        private Boolean enabled;

        /** More script origins the policy allows, e.g. {@code https://cdn.example.com}. */
        private Set<String> scriptSources;

        /** The page may compile WebAssembly ({@code 'wasm-unsafe-eval'}, never {@code eval}). */
        private Boolean webAssembly;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(final Boolean enabled) {
            this.enabled = enabled;
        }

        public Set<String> getScriptSources() {
            return scriptSources;
        }

        public void setScriptSources(final Set<String> scriptSources) {
            this.scriptSources = scriptSources;
        }

        public Boolean getWebAssembly() {
            return webAssembly;
        }

        public void setWebAssembly(final Boolean webAssembly) {
            this.webAssembly = webAssembly;
        }
    }

    /** The WebSocket of the screens. */
    public static class WebSocket {

        /** The other origins allowed to connect: the pages that embed the application in a {@code <pony-app>}; unset: same origin only. */
        private Set<String> allowedOrigins;

        /** The upgrade checks the {@code Origin} header. */
        private Boolean originCheck;

        /** Largest message a client sends. */
        private DataSize maxMessageSize;

        /** Jetty's buffer per incoming message; the upstream messages are small. */
        private DataSize inputBufferSize;

        /** The connection closes after this long without a frame. */
        private Duration idleTimeout;

        /** Wait on a slow client before disconnecting it. */
        private Duration sendTimeout;

        /** Frame compression (permessage-deflate) is negotiated. */
        private Boolean permessageDeflate;

        /** The zlib level of the frame compression, 0 to 9, or -1 for Jetty's default. */
        private Integer deflateLevel;

        public Set<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(final Set<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public Boolean getOriginCheck() {
            return originCheck;
        }

        public void setOriginCheck(final Boolean originCheck) {
            this.originCheck = originCheck;
        }

        public DataSize getMaxMessageSize() {
            return maxMessageSize;
        }

        public void setMaxMessageSize(final DataSize maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
        }

        public DataSize getInputBufferSize() {
            return inputBufferSize;
        }

        public void setInputBufferSize(final DataSize inputBufferSize) {
            this.inputBufferSize = inputBufferSize;
        }

        public Duration getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(final Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public Duration getSendTimeout() {
            return sendTimeout;
        }

        public void setSendTimeout(final Duration sendTimeout) {
            this.sendTimeout = sendTimeout;
        }

        public Boolean getPermessageDeflate() {
            return permessageDeflate;
        }

        public void setPermessageDeflate(final Boolean permessageDeflate) {
            this.permessageDeflate = permessageDeflate;
        }

        public Integer getDeflateLevel() {
            return deflateLevel;
        }

        public void setDeflateLevel(final Integer deflateLevel) {
            this.deflateLevel = deflateLevel;
        }
    }

    /** Strings sent once then by index, learned across sessions. */
    public static class StringDictionary {

        /** Strings are sent once, then by index. */
        private Boolean enabled;

        /** The dictionary is kept across restarts, in a file under {@link #persistPath}. */
        private Boolean persistence;

        /** The directory the dictionary is kept in, relative to the working directory. */
        private String persistPath;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(final Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean getPersistence() {
            return persistence;
        }

        public void setPersistence(final Boolean persistence) {
            this.persistence = persistence;
        }

        public String getPersistPath() {
            return persistPath;
        }

        public void setPersistPath(final String persistPath) {
            this.persistPath = persistPath;
        }
    }
}
