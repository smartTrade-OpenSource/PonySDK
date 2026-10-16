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

        // Linked collections: load order matters (jQuery before the scripts using it) and Set.of /
        // Map.of have no defined iteration order. Kept in sync with etc/server_application.xml.
        configuration.setJavascript(new java.util.LinkedHashSet<>(java.util.List.of(
            "script/jquery-3.7.1.min.js",
            "script/webcomponents.js",
            "script/sample.js",
            "script/flexlayout.js",
            "script/flexlayout-addon.js"
        )));

        final java.util.Map<String, String> styles = new java.util.LinkedHashMap<>();
        styles.put("ponysdk", "style/ponysdk.css");
        styles.put("sample", "css/sample.css");
        styles.put("samplePony", "css/ponysdk.css");
        styles.put("flexlayout", "style/flexlayout.css");
        styles.put("showcase", "css/showcase.css"); // last: the showcase theme wins over the GWT defaults
        configuration.setStyle(styles);

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
        server.setSessionCookieSecure(false); // plain-http dev server (see PonySDKServer)
        server.start();
    }
}
