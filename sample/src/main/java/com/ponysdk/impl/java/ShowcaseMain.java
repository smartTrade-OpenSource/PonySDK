package com.ponysdk.impl.java;

import java.util.Set;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    /** The showcase's port: 8081, or -Dponysdk.port (runShowcase -Pport=8091) for a second server on this machine. */
    public static final int PORT = Integer.getInteger("ponysdk.port", 8081);
    /** The portal of the embedding demo: a page of another origin with <pony-app> elements (ADR-0008). */
    public static final int PORTAL_PORT = PORT + 1;

    private static final Logger log = LoggerFactory.getLogger(ShowcaseMain.class);

    public static void main(final String[] args) throws Exception {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setApplicationID("showcase");
        configuration.setApplicationName("PonySDK Showcase");
        configuration.setApplicationDescription("PonySDK widget showcase");
        // Mobile: render at device width (the showcase stylesheet has a narrow layout); browser chrome colour
        configuration.setMeta(new java.util.LinkedHashSet<>(java.util.List.of(
            "name=\"viewport\" content=\"width=device-width, initial-scale=1\"",
            "name=\"theme-color\" content=\"#ffffff\"",
            "name=\"description\" content=\"PonySDK: real-time web UIs written only in Java\"")));
        configuration.setApplicationContextName("sample");
        configuration.setSessionTimeout(60);
        configuration.setReconnectionTimeoutMs(10_000); // 10s transparent reconnect window
        // The pages allowed to embed the showcase: the portal (docs/decisions/0008-embedded-application.md)
        configuration.setWsAllowedOrigins(Set.of("http://[::1]:" + PORTAL_PORT, "http://localhost:" + PORTAL_PORT,
            "http://127.0.0.1:" + PORTAL_PORT));
        configuration.setEntryPointClass(UISampleEntryPoint.class);
        // -Dponysdk.debug (runShowcase -Pdebug): each widget tells the terminal its class and creation line, and the
        // inspector shows over the page. Off by default: the Costs page measures the bytes of the normal mode
        configuration.setDebugMode(Boolean.getBoolean("ponysdk.debug"));
        // -Dponysdk.hotReload (runShowcase -PhotReload): the showcase's classes reload once compiled again, and its pages
        configuration.setHotReload(Boolean.getBoolean("ponysdk.hotReload"));

        // Linked collections: load order matters (jQuery before the scripts using it) and Set.of /
        // Map.of have no defined iteration order.
        configuration.setJavascript(new java.util.LinkedHashSet<>(java.util.List.of(
            "script/jquery-3.7.1.min.js",
            "script/webcomponents.js",
            "script/sample.js",
            "script/flexlayout.js",
            "script/flexlayout-addon.js",
            "script/fdc3-agent.js", // the sample's demonstration desktop agent: a real desktop brings its own
            "script/fdc3-addon.js"
        )));

        final java.util.Map<String, String> styles = new java.util.LinkedHashMap<>();
        styles.put("ponysdk", "style/ponysdk.css");
        styles.put("flexlayout", "style/flexlayout.css");
        // the frame of the Theme page: its layout (the theme itself is loaded by that page, script/sample.js)
        styles.put("theme-preview", "css/theme-preview.css");
        styles.put("showcase", "css/showcase.css"); // last: the showcase theme wins over the defaults
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
        server.setPort(PORT);
        server.setHost("0.0.0.0");
        server.setUseSSL(false);
        server.setSessionCookieSecure(false); // plain-http dev server (see PonySDKServer)
        // region drain
        // SIGTERM (a deployment, a scale-in): the live pages move to the other servers over 5 s, then the server stops.
        // Behind an AWS load balancer: less than the target group's deregistration delay and the task's stopTimeout
        server.setDrainMillis(5_000);
        // endregion
        server.start();
        startPortal();
    }

    /**
     * The portal: static files of another origin, the gallery of the ways to integrate PonySDK, no PonySDK in them but
     * the <pony-app> elements; and its own copy of PonySDK's terminal under vendor/, as a product of another version
     * would bring it (the gallery's U9).
     */
    private static void startPortal() throws Exception {
        final Server portal = new Server();
        final ServerConnector connector = new ServerConnector(portal);
        connector.setPort(PORTAL_PORT);
        portal.addConnector(connector);
        final ResourceFactory resources = ResourceFactory.of(portal);
        final ResourceHandler files = new ResourceHandler();
        files.setBaseResource(resources.newClassLoaderResource("portal/"));
        files.setWelcomeFiles("index.html");
        files.setDirAllowed(false);
        final ResourceHandler terminal = new ResourceHandler();
        terminal.setBaseResource(resources.newClassLoaderResource("ponyterminal/"));
        terminal.setDirAllowed(false);
        portal.setHandler(new ContextHandlerCollection(new ContextHandler(terminal, "/vendor/ponysdk/ponyterminal"),
            new ContextHandler(files, "/")));
        portal.start();
        log.info("Integrating PonySDK, the gallery: http://[::1]:{}/ shows the showcase from another origin", PORTAL_PORT);
    }
}
