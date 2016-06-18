/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.terminal.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;
import elemental.html.Window;

public class PTWindow extends AbstractPTObject implements EventListener {

    private static final Logger log = Logger.getLogger(PTWindow.class.getName());

    private static final String WINDOW_EVENT_TYPE_BEFORE_UNLOAD = "beforeunload";

    private static final String EMPTY = "";

    private Window window;
    private String url;
    private String name;
    private String features;

    private UIBuilder uiService;

    private boolean ponySDKStarted = false;

    private EventRemover eventRemover;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder builder) {
        super.create(buffer, objectId, builder);

        if (log.isLoggable(Level.INFO))
            log.log(Level.INFO, "PTWindowID created : " + objectID);

        uiService = builder;

        url = buffer.readBinaryModel().getStringValue();
        if (url == null)
            url = GWT.getHostPageBaseURL() + "?wid=" + objectId;

        name = buffer.readBinaryModel().getStringValue();
        if (name == null)
            name = EMPTY;

        features = buffer.readBinaryModel().getStringValue();
        if (features == null)
            features = EMPTY;

        PTWindowManager.get().register(this);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.OPEN.equals(binaryModel.getModel())) {
            window = Browser.getWindow().open(url, name, features);
            // WORKAROUND : Add a specific method to handle IE
            //eventRemover = window.addEventListener(WINDOW_EVENT_TYPE_BEFORE_UNLOAD, this, true);
            eventRemover = addEventListener(WINDOW_EVENT_TYPE_BEFORE_UNLOAD, this, true);
            return true;
        }
        if (ServerToClientModel.TEXT.equals(binaryModel.getModel())) {
            window.postMessage(binaryModel.getStringValue(), "*");
            return true;
        }
        if (ServerToClientModel.CLOSE.equals(binaryModel.getModel())) {
            close(false);
            return true;
        }
        return false;
    }

    public void close(final boolean forced) {
        if (forced) {
            eventRemover.remove();
        }
        window.close();
    }

    @Override
    public void handleEvent(final Event event) {
        if (event.getCurrentTarget() == window && WINDOW_EVENT_TYPE_BEFORE_UNLOAD.equals(event.getType())) {
            final PTInstruction instruction = new PTInstruction(objectID);
            instruction.put(ClientToServerModel.HANDLER_CLOSE);
            uiService.sendDataToServer(instruction);
            PTWindowManager.get().unregister(this);
        }
    }

    public void postMessage(final ReaderBuffer buffer) {
        if (ponySDKStarted) postMessage(buffer, window);
        else log.log(Level.WARNING, "No window set");
    }

    public native void postMessage(final ReaderBuffer buffer, Window window) /*-{window.onDataReceived(buffer);}-*/;

    public native final EventRemover addEventListener(String type, EventListener listener, boolean useCapture) /*-{
                                                                                                               var handler = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
                                                                                                               if (window.addEventListener) window.addEventListener(type, handler, useCapture); // W3C DOM
                                                                                                               else if (window.attachEvent) window.attachEvent("on" + type, handler); // IE DOM
                                                                                                               return @elemental.js.dom.JsElementalMixinBase.Remover::create(Lelemental/events/EventTarget;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Z)(window, type, handler, useCapture);
                                                                                                               }-*/;

    public void setReady() {
        ponySDKStarted = true;

        final PTInstruction instruction = new PTInstruction(objectID);
        instruction.put(ClientToServerModel.HANDLER_OPEN);
        uiService.sendDataToServer(instruction);
    }

}
