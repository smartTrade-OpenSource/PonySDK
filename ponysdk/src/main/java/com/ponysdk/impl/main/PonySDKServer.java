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

package com.ponysdk.impl.main;

import java.net.InetAddress;
import java.net.URL;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.MappingPath;
import com.ponysdk.core.server.application.ApplicationManagerOption;
import com.ponysdk.core.server.servlet.ApplicationLoader;
import com.ponysdk.core.server.servlet.BootstrapServlet;
import com.ponysdk.core.server.servlet.ServletContextFilter;
import com.ponysdk.core.server.servlet.StreamServiceServlet;
import com.ponysdk.core.server.servlet.WebSocketServlet;

public class PonySDKServer {

    public static final String MAPPING_BOOTSTRAP = "/*";
    public static final String MAPPING_WS = "/" + MappingPath.WEBSOCKET + "/*";
    public static final String MAPPING_STREAM = "/" + MappingPath.STREAM;

    private static final Logger log = LoggerFactory.getLogger(PonySDKServer.class);

    protected final Server server;

    protected ApplicationLoader applicationLoader;

    protected String host = "0.0.0.0";
    protected int port = 80;

    private boolean useSSL = true;
    private int sslPort = 443;
    private String sslKeyStoreFile;
    private String sslKeyStorePassphrase;
    private String sslKeyStoreType = "JKS";
    private String sslTrustStoreFile;
    private String sslTrustStorePassphrase;
    private String sslTrustStoreType = "JKS";
    private boolean needClientAuth = false;
    private String[] enabledProtocols = new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" };
    private String enabledCipherSuites;

    public PonySDKServer() {
        server = new Server();
    }

    public void start() throws Exception {
        server.addConnector(createHttpConnector());
        if (useSSL) server.addConnector(createHttpsConnector());

        final ServletContextHandler context = createWebApp();

        final GzipHandler gzip = new GzipHandler();
        gzip.setHandler(context);

        server.setHandler(gzip);

        context.addEventListener(applicationLoader);
        applicationLoader.start();

        server.start();

        server.join();

        log.info("Webserver started on: " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
    }

    protected ServletContextHandler createWebApp() {
        final ApplicationManagerOption applicationManagerOption = applicationLoader.getApplicationManagerOption();
        log.info("Adding application #" + applicationManagerOption.getApplicationContextName());

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/" + applicationManagerOption.getApplicationContextName());

        context.addServlet(new ServletHolder(createBootstrapServlet()), MAPPING_BOOTSTRAP);
        context.addServlet(new ServletHolder(createStreamServiceServlet()), MAPPING_STREAM);
        context.addServlet(new ServletHolder(createWebSocketServlet()), MAPPING_WS);

        final ServletContextFilter servletContextFilter = new ServletContextFilter();
        context.addFilter(new FilterHolder(servletContextFilter), MAPPING_BOOTSTRAP, EnumSet.of(DispatcherType.REQUEST));

        final SessionHandler sessionHandler = context.getSessionHandler();
        sessionHandler.setMaxInactiveInterval(60 * applicationManagerOption.getSessionTimeout());
        sessionHandler.addEventListener(applicationLoader);

        return context;
    }

    protected ServerConnector createHttpConnector() {
        final ServerConnector serverConnector;
        if (useSSL) {
            final HttpConfiguration httpConfiguration = new HttpConfiguration();
            httpConfiguration.setSecurePort(sslPort);
            serverConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
        } else {
            serverConnector = new ServerConnector(server);
        }

        serverConnector.setPort(port);
        serverConnector.setHost(host);
        serverConnector.setReuseAddress(true);
        return serverConnector;
    }

    protected ServerConnector createHttpsConnector() {
        final ServerConnector serverConnector;
        // HTTPS
        URL keyStore = getClass().getResource(sslKeyStoreFile);

        if (keyStore == null) {
            keyStore = getClass().getClassLoader().getResource(sslKeyStoreFile);
        }

        if (keyStore == null) throw new RuntimeException("KeyStore not found #" + sslKeyStoreFile);
        final SslContextFactory sslContextFactory = new SslContextFactory(keyStore.toExternalForm());
        sslContextFactory.setKeyStorePassword(sslKeyStorePassphrase);
        sslContextFactory.setKeyStoreType(sslKeyStoreType);
        sslContextFactory.setIncludeProtocols(enabledProtocols);
        if (enabledCipherSuites != null) sslContextFactory.setIncludeCipherSuites(enabledCipherSuites);

        if (needClientAuth) {
            sslContextFactory.setNeedClientAuth(needClientAuth);
            if (sslTrustStoreFile != null) {
                sslContextFactory.setTrustStorePath(sslTrustStoreFile);
                sslContextFactory.setTrustStorePassword(sslTrustStorePassphrase);
                sslContextFactory.setTrustStoreType(sslTrustStoreType);
            }
        }

        serverConnector = new ServerConnector(server, sslContextFactory);
        serverConnector.setPort(sslPort);
        serverConnector.setHost(host);
        serverConnector.setReuseAddress(true);
        return serverConnector;
    }

    protected BootstrapServlet createBootstrapServlet() {
        final BootstrapServlet bootstrapServlet = new BootstrapServlet();
        bootstrapServlet.setApplication(applicationLoader.getApplicationManagerOption());
        return bootstrapServlet;
    }

    protected StreamServiceServlet createStreamServiceServlet() {
        return new StreamServiceServlet();
    }

    protected WebSocketServlet createWebSocketServlet() {
        return new WebSocketServlet();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public void setApplicationLoader(final ApplicationLoader applicationLoader) {
        this.applicationLoader = applicationLoader;
    }

    public ApplicationManagerOption getApplicationOption() {
        return applicationLoader.getApplicationManagerOption();
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setUseSSL(final boolean useSSL) {
        this.useSSL = useSSL;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setSslKeyStoreFile(final String sslKeyStoreFile) {
        this.sslKeyStoreFile = sslKeyStoreFile;
    }

    public void setSslKeyStorePassphrase(final String sslKeyStorePassphrase) {
        this.sslKeyStorePassphrase = sslKeyStorePassphrase;
    }

    public void setSslPort(final int sslPort) {
        this.sslPort = sslPort;
    }

    public void setSslKeyStoreType(final String sslKeyStoreType) {
        this.sslKeyStoreType = sslKeyStoreType;
    }

    public void setSslTrustStoreFile(final String sslTrustStoreFile) {
        this.sslTrustStoreFile = sslTrustStoreFile;
    }

    public void setSslTrustStorePassphrase(final String sslTrustStorePassphrase) {
        this.sslTrustStorePassphrase = sslTrustStorePassphrase;
    }

    public void setSslTrustStoreType(final String sslTrustStoreType) {
        this.sslTrustStoreType = sslTrustStoreType;
    }

    public void setNeedClientAuth(final boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    public void setEnabledProtocols(final String[] enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }

    public void setEnabledCipherSuites(final String enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }

}
