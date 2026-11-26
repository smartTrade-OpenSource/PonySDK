/*
 * Copyright (c) 2017 PonySDK
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

package com.ponysdk.core.terminal;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;

import com.ponysdk.core.terminal.ui.PTWindowManager;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.Window;
import elemental.xml.XMLHttpRequest;

public class ReconnectionChecker {

    private static final int HTTP_STATUS_CODE_OK = 200;

    private static final int RETRY_TIMEOUT = 30000; // 30 seconds
    private static final int RETRY_PERIOD = 2000; // 2 seconds
    // A ping not answered after this is sent again: it may be held by a connection that died with the WebSocket,
    // which the browser keeps reusing
    private static final int PING_RESEND_DELAY = 4000; // 4 seconds
    // A resume not answered after this is given up and tried again
    private static final int RESUME_TIMEOUT = 15000; // 15 seconds

    private static final Logger log = Logger.getLogger(ReconnectionChecker.class.getName());

    private final Window window;

    private final XMLHttpRequest reconnectionRequest;

    private boolean errorDetected;
    /** A new WebSocket is being opened to resume the context. */
    private boolean resuming;
    /** The server is being pinged, every {@link #RETRY_PERIOD}, until it answers. */
    private boolean pinging;
    private boolean pingInFlight;
    /** Each ping loop stops once a newer one is started. */
    private int pingGeneration;
    private int resumeAttempt;
    private long pingSentAt;

    public ReconnectionChecker() {
        window = Browser.getWindow();

        reconnectionRequest = window.newXMLHttpRequest();
        reconnectionRequest.setOnreadystatechange(evt -> {
            if (reconnectionRequest.getReadyState() != XMLHttpRequest.DONE) return;
            pingInFlight = false;
            if (pinging && reconnectionRequest.getStatus() == HTTP_STATUS_CODE_OK) {
                pinging = false;
                onServerReachable();
            }
            // Otherwise the next ping goes on the next tick
        });
    }

    /**
     * The server answers again: resume the context on a new WebSocket if the server keeps lost contexts, otherwise
     * reload the page. A resume is tried even past the timeout: the server refuses it if the context is gone, and
     * releases it first if it still holds it, so that the reloaded page is not refused as a second tab.
     */
    private void onServerReachable() {
        final PonySDK ponySDK = PonySDK.get();
        if (ponySDK.isResumable()) {
            log.info("Server reachable, resuming the context");
            resuming = true;
            final int attempt = ++resumeAttempt;
            ponySDK.reconnect();
            // A resume the server never answers is given up: its socket is closed, which tries again
            Scheduler.get().scheduleFixedDelay(() -> {
                if (resuming && attempt == resumeAttempt) ponySDK.abortReconnect();
                return false;
            }, RESUME_TIMEOUT);
        } else {
            errorDetected = false;
            reloadWindow();
        }
    }

    /** The server cannot resume the context: the page reloads. */
    public void onResumeRefused() {
        log.info("The server cannot resume the context, reloading");
        resuming = false;
        errorDetected = false;
        try {
            notifyConnectionLostListeners(); // the connection is now lost for good
        } finally {
            reloadWindow(); // even if a listener fails: the page must not stay frozen
        }
    }

    /** The context was resumed: the page carries on as it is. */
    public void onReconnected() {
        resuming = false;
        errorDetected = false;
        hideReconnectionInformation();
        PTWindowManager.setConnectionLost(false);
    }

    /**
     * Pings the server every {@link #RETRY_PERIOD} until it answers. A ping still unanswered after
     * {@link #PING_RESEND_DELAY} is sent again (the browser aborts the previous one): one held by a dead connection must
     * not hold the reconnection.
     */
    private void startPinging() {
        if (pinging) return;
        pinging = true;
        final int generation = ++pingGeneration;
        Scheduler.get().scheduleFixedDelay(() -> {
            if (!pinging || generation != pingGeneration) return false; // stopped, or replaced by a newer loop
            if (!pingInFlight || System.currentTimeMillis() - pingSentAt >= PING_RESEND_DELAY) retryConnection();
            return true;
        }, RETRY_PERIOD);
    }

    private void hideReconnectionInformation() {
        if (isSpecificReconnectionInformation()) {
            hideSpecificReconnectionInformation();
        } else {
            final Element reconnectionElement = Browser.getDocument().getElementById("reconnection");
            if (reconnectionElement != null) reconnectionElement.getStyle().setDisplay("none");
        }
    }

    private final native void hideSpecificReconnectionInformation() /*-{
                                                                    if ($wnd.hideReconnectionInformation && typeof $wnd.hideReconnectionInformation == 'function') {
                                                                        $wnd.hideReconnectionInformation();
                                                                    }
                                                                    }-*/;

    public void detectConnectionFailure() {
        if (errorDetected) {
            // The WebSocket opened to resume the context was lost too: try again
            if (resuming) {
                resuming = false;
                startPinging();
            }
            return;
        }
        errorDetected = true;

        log.severe("Failure detected");
        // The listeners take the connection as lost for good (they close child windows, clear what the server sent):
        // for a context that may be resumed, they are told only if the resume is refused
        if (!PonySDK.get().isResumable()) notifyConnectionLostListeners();

        if (isSpecificReconnectionInformation()) {
            showSpecificReconnectionInformation();
        } else {
            final Document document = Browser.getDocument();
            // A page without these elements still retries
            final Element reconnectionElement = document.getElementById("reconnection");
            if (reconnectionElement != null) reconnectionElement.getStyle().setDisplay("block");

            final Element reconnectingElement = document.getElementById("reconnecting");
            if (reconnectingElement != null) reconnectingElement.setInnerHTML("Connection to server lost<br>Reconnecting ...");
        }

        // The child windows have no connection of their own: they show the main window's panel too, while the context
        // may be resumed. After the main window's panel is shown, so that they copy it as it is.
        if (PonySDK.get().isResumable()) PTWindowManager.setConnectionLost(true);

        startPinging();
    }

    public static final native void reloadWindow() /*-{
                                                   $wnd.document.doReload();
                                                   }-*/;

    private final native void notifyConnectionLostListeners() /*-{
                                                              for(var i = 0 ; i < $wnd.document.onConnectionLostListeners.length ; i++) {
                                                                  var connectionLostListener = $wnd.document.onConnectionLostListeners[i];
                                                                  try {
                                                                      connectionLostListener();
                                                                  } catch (error) {
                                                                      throw "cannot call onConnectionLostListeners callback: " + connectionLostListener + ", error " + error;
                                                                  }
                                                              }
                                                              }-*/;

    private final native boolean isSpecificReconnectionInformation() /*-{
                                                                     return $wnd.showReconnectionInformation && typeof $wnd.showReconnectionInformation == 'function';
                                                                     }-*/;

    private final native void showSpecificReconnectionInformation() /*-{
                                                                    $wnd.showReconnectionInformation();
                                                                    }-*/;

    private void retryConnection() {
        pingInFlight = true;
        pingSentAt = System.currentTimeMillis();
        reconnectionRequest.open("GET", getPingUrl() + "&retry");
        setHTTPRequestTimeout(reconnectionRequest, RETRY_TIMEOUT);
        reconnectionRequest.send();
    }

    private static final native void setHTTPRequestTimeout(XMLHttpRequest xmlHTTPRequest, int timeout) /*-{
                                                                                                       xmlHTTPRequest.timeout = timeout;
                                                                                                       }-*/;

    private static final String getPingUrl() {
        return GWT.getHostPageBaseURL() + "?ping=" + System.currentTimeMillis();
    }

}
