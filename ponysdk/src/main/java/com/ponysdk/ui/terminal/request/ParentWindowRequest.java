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

package com.ponysdk.ui.terminal.request;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.json.client.JSONValue;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class ParentWindowRequest implements RequestBuilder {

    private static final Logger log = Logger.getLogger(ParentWindowRequest.class.getName());

    protected final RequestCallback callback;

    private final String windowID;

    public ParentWindowRequest(final String windowID, final RequestCallback callback) {
        this.windowID = windowID;
        this.callback = callback;

        exportOnDataReceived();

        setReadyWindow(windowID);
    }

    /**
     * To Main terminal
     */
    public static native void setReadyWindow(final String windowID) /*-{
                                                                    $wnd.opener.pony.setReadyWindow(windowID);
                                                                    }-*/;

    /**
     * To Main terminal
     */
    @Override
    public void send(final JSONValue value) {
        sendToParent(value.toString());
    }

    /**
     * To Main terminal
     */
    public static native void sendToParent(final String data) /*-{
                                                                        $wnd.opener.pony.sendDataToServer(data);
                                                                        }-*/;

    /**
     * From Main terminal to the matching window terminal
     */
    public native void exportOnDataReceived() /*-{
                                              var that = this;
                                              $wnd.onDataReceived = function(buffer) {
                                              $entry(that.@com.ponysdk.ui.terminal.request.ParentWindowRequest::onDataReceived(Lcom/ponysdk/ui/terminal/model/ReaderBuffer;)(buffer));
                                              }
                                              }-*/;

    /**
     * From Main terminal to the matching window terminal
     */
    public void onDataReceived(final ReaderBuffer buffer) {
        if (log.isLoggable(Level.FINE)) log.fine("Data received from main terminal");
        callback.onDataReceived(buffer);
    }

}
