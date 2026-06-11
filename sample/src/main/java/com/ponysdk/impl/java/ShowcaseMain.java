package com.ponysdk.impl.java;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.server.websocket.WebSocketServlet;
import com.ponysdk.impl.java.server.JavaApplicationManager;
import com.ponysdk.impl.main.PonySDKServer;
import com.ponysdk.sample.client.UISampleEntryPoint;

/**
 * Standalone showcase launcher — no Spring required.
 */
public class ShowcaseMain {

    /** Application attribute key to share metrics with UIContexts. */
    public static final String METRICS_KEY = "ponysdk.metrics";

    public static void main(final String[] args) throws Exception {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setApplicationID("showcase");
        configuration.setApplicationName("PonySDK Showcase");
        configuration.setApplicationDescription("PonySDK widget showcase");
        configuration.setApplicationContextName("sample");
        configuration.setSessionTimeout(60);
        configuration.setReconnectionTimeoutMs(10_000); // 10s transparent reconnect window
        configuration.setEntryPointClass(UISampleEntryPoint.class);

        configuration.setJavascript(java.util.Set.of(
            "script/jquery-3.0.0.min.js",
            "script/webcomponents.js",
            "script/sample.js"
        ));

        configuration.setStyle(java.util.Map.of(
            "ponysdk", "style/ponysdk.css",
            "sample",  "css/sample.css",
            "samplePony", "css/ponysdk.css"
        ));

        // Create metrics — tracks sessions, bytes, latency
        final PonySDKMetrics metrics = new PonySDKMetrics("showcase");

        final JavaApplicationManager applicationManager = new JavaApplicationManager();
        applicationManager.setConfiguration(configuration);

        final PonySDKServer server = new PonySDKServer() {
            @Override
            protected WebSocketServlet createWebSocketServlet() {
                final WebSocketServlet ws = new WebSocketServlet(applicationManager);
                ws.setWebsocketMonitor(metrics.websocketMonitor());
                ws.setMetrics(metrics);
                return ws;
            }
        };
        server.setApplicationManager(applicationManager);
        server.setPort(8081);
        server.setHost("0.0.0.0");
        server.setUseSSL(false);
        server.start();
    }
}
