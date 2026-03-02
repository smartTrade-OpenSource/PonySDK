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

import elemental2.dom.DomGlobal;
import elemental2.dom.Document;
import elemental2.dom.Element;
import elemental2.dom.HTMLElement;
import elemental2.dom.XMLHttpRequest;

import jsinterop.base.Js;

public class ReconnectionChecker {

    private static final int HTTP_STATUS_CODE_OK = 200;

    private static final int RETRY_TIMEOUT = 30000; // 30 seconds
    private static final int RETRY_PERIOD = 2000; // 2 seconds

    private static final Logger log = Logger.getLogger(ReconnectionChecker.class.getName());

    private final XMLHttpRequest reconnectionRequest;

    private boolean errorDetected;

    public ReconnectionChecker() {
        reconnectionRequest = new XMLHttpRequest();
        setOnReadyStateChange(reconnectionRequest);
    }

    private void setOnReadyStateChange(final XMLHttpRequest request) {
        request.addEventListener("readystatechange", evt -> {
            if (request.readyState == XMLHttpRequest.DONE
                    && request.status == HTTP_STATUS_CODE_OK) {
                errorDetected = false;
                reloadWindow();
            } else {
                Scheduler.get().scheduleFixedDelay(() -> {
                    retryConnection();
                    return false;
                }, RETRY_PERIOD);
            }
        });
    }

    public void detectConnectionFailure() {
        if (errorDetected) return;
        errorDetected = true;

        log.severe("Failure detected");
        notifyConnectionLostListeners();

        if (isSpecificReconnectionInformation()) {
            showSpecificReconnectionInformation();
        } else {
            final Document document = DomGlobal.document;
            final Element reconnectionElement = document.getElementById("reconnection");
            setStyleDisplay(reconnectionElement, "block");

            final Element reconnectingElement = document.getElementById("reconnecting");
            setInnerHTML(reconnectingElement, "Connection to server lost<br>Reconnecting ...");
        }

        Scheduler.get().scheduleFixedDelay(() -> {
            retryConnection();
            return false;
        }, RETRY_PERIOD);
    }

    /**
     * Called when the server confirms the application is reachable again.
     * If transparent reconnection mode is active (window.ponyReconnectMode === true),
     * reconnects the WebSocket carrying the current uiContextId instead of reloading.
     * Otherwise falls back to page reload, unless {@code window.onPonyReconnected} is defined.
     */
    public void reloadWindow() {
        if (isTransparentReconnectMode()) {
            reconnectWebSocket();
        } else if (hasOnPonyReconnected()) {
            callOnPonyReconnected();
        } else {
            doReload();
        }
    }

    private static native boolean isTransparentReconnectMode() /*-{
        return $wnd.ponyReconnectMode === true;
    }-*/;

    private static native void reconnectWebSocket() /*-{
        var url = @com.ponysdk.core.terminal.PonySDK::get()().@com.ponysdk.core.terminal.PonySDK::buildReconnectUrl()();
        @com.ponysdk.core.terminal.PonySDK::get()().@com.ponysdk.core.terminal.PonySDK::reconnectSocket(Ljava/lang/String;)(url);
    }-*/;

    private static native boolean hasOnPonyReconnected() /*-{
        return $wnd.onPonyReconnected && typeof $wnd.onPonyReconnected === 'function';
    }-*/;

    private static native void callOnPonyReconnected() /*-{
        $wnd.onPonyReconnected();
    }-*/;

    private static native void doReload() /*-{
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
        reconnectionRequest.open("GET", getPingUrl() + "&retry");
        setHTTPRequestTimeout(reconnectionRequest, RETRY_TIMEOUT);
        reconnectionRequest.send();
    }

    private static final void setHTTPRequestTimeout(final XMLHttpRequest xmlHTTPRequest, final int timeout) {
        Js.asPropertyMap(xmlHTTPRequest).set("timeout", timeout);
    }

    private static void setStyleDisplay(final Element element, final String display) {
        if (element != null) ((HTMLElement) element).style.display = display;
    }

    private static void setInnerHTML(final Element element, final String html) {
        if (element != null) element.innerHTML = html;
    }

    private static final String getPingUrl() {
        return GWT.getHostPageBaseURL() + "?ping=" + System.currentTimeMillis();
    }

}
