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

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.Window;
import elemental.xml.XMLHttpRequest;

public class ReconnectionChecker {

    private static final int HTTP_STATUS_CODE_OK = 200;

    private static final int RETRY_TIMEOUT = 30000; // 30 seconds
    private static final int RETRY_PERIOD = 2000; // 2 seconds

    private static final Logger log = Logger.getLogger(ReconnectionChecker.class.getName());

    private final Window window;

    private final XMLHttpRequest reconnectionRequest;

    private boolean errorDetected;

    public ReconnectionChecker() {
        window = Browser.getWindow();

        reconnectionRequest = window.newXMLHttpRequest();
        reconnectionRequest.setOnreadystatechange(evt -> {
            if (reconnectionRequest.getReadyState() == XMLHttpRequest.DONE) {
                if (reconnectionRequest.getStatus() == HTTP_STATUS_CODE_OK) {
                    errorDetected = false;
                    window.getLocation().reload();
                } else {
                    // We reschedule the next check (we wait to avoid spaming)
                    Scheduler.get().scheduleFixedDelay(() -> {
                        retryConnection();
                        return false;
                    }, RETRY_PERIOD);
                }
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
            final Document document = Browser.getDocument();
            final Element reconnectionElement = document.getElementById("reconnection");
            reconnectionElement.getStyle().setDisplay("block");

            final Element reconnectingElement = document.getElementById("reconnecting");
            reconnectingElement.setInnerHTML("Connection to server lost<br>Reconnecting ...");
        }

        Scheduler.get().scheduleFixedDelay(() -> {
            retryConnection();
            return false;
        }, RETRY_PERIOD);
    }

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

    private static final native void setHTTPRequestTimeout(XMLHttpRequest xmlHTTPRequest, int timeout) /*-{
                                                                                                       xmlHTTPRequest.timeout = timeout;
                                                                                                       }-*/;

    private static final String getPingUrl() {
        return GWT.getHostPageBaseURL() + "?ping=" + System.currentTimeMillis();
    }

}
