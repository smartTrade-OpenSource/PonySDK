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

package com.ponysdk.core.terminal;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Element;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.request.RequestBuilder;
import com.ponysdk.core.terminal.ui.PTCookies;
import com.ponysdk.core.terminal.ui.PTFrame;
import com.ponysdk.core.terminal.ui.PTHistory;
import com.ponysdk.core.terminal.ui.PTObject;
import com.ponysdk.core.terminal.ui.PTStreamResource;
import com.ponysdk.core.terminal.ui.PTWindow;
import com.ponysdk.core.terminal.ui.PTWindowManager;

import elemental.client.Browser;
import elemental.html.Uint8Array;
import elemental.js.util.JsMapFromIntTo;
import elemental.js.util.JsMapFromStringTo;

public class UIBuilder {

    private static final Logger log = Logger.getLogger(UIBuilder.class.getName());

    private final UIFactory uiFactory = new UIFactory();
    private final JsMapFromIntTo<PTObject> objectByID = JsMapFromIntTo.create();
    private final Map<UIObject, Integer> objectIDByWidget = new HashMap<>();
    private final JsMapFromIntTo<UIObject> widgetIDByObjectID = JsMapFromIntTo.create();
    private final JsMapFromStringTo<JavascriptAddOnFactory> javascriptAddOnFactories = JsMapFromStringTo.create();

    private final ReaderBuffer readerBuffer = new ReaderBuffer();

    private RequestBuilder requestBuilder;

    private static class AvoidBlockException extends Exception {
    }

    public void init(final RequestBuilder requestBuilder) {
        if (log.isLoggable(Level.INFO)) log.info("Init request builder");

        this.requestBuilder = requestBuilder;

        PTHistory.addValueChangeHandler(this);

        final PTCookies cookies = new PTCookies(this);
        objectByID.put(0, cookies);

        // hide loading component
        final Widget w = RootPanel.get("loading");
        if (w != null) {
            w.setSize("0px", "0px");
            w.setVisible(false);
        } else {
            log.log(Level.WARNING, "Include splash screen html element into your index.html with id=\"loading\"");
        }
    }

    public void updateMainTerminal(final Uint8Array buffer) {
        readerBuffer.init(buffer);

        while (readerBuffer.hasEnoughKeyBytes()) {
            final int nextBlockPosition = readerBuffer.shiftNextBlock(true);
            if (nextBlockPosition == ReaderBuffer.NOT_FULL_BUFFER_POSITION) return;

            // Detect if the message is not for the main terminal but for a specific window
            final BinaryModel binaryModel = readerBuffer.readBinaryModel();
            final ServerToClientModel model = binaryModel.getModel();

            if (ServerToClientModel.WINDOW_ID == model) {
                // Event on a specific window
                final int requestedId = binaryModel.getIntValue();
                // Main terminal, we need to dispatch the eventbus
                final PTWindow window = PTWindowManager.getWindow(requestedId);
                if (window != null && window.isReady()) {
                    final int startPosition = readerBuffer.getPosition();
                    int endPosition = nextBlockPosition;

                    // Concat multiple messages for the same window
                    readerBuffer.setPosition(endPosition);
                    while (readerBuffer.hasEnoughKeyBytes()) {
                        final int nextBlockPosition1 = readerBuffer.shiftNextBlock(true);
                        if (nextBlockPosition1 != ReaderBuffer.NOT_FULL_BUFFER_POSITION) {
                            final BinaryModel newBinaryModel = readerBuffer.readBinaryModel();
                            if (ServerToClientModel.WINDOW_ID == newBinaryModel.getModel()
                                    && requestedId == newBinaryModel.getIntValue()) {
                                endPosition = nextBlockPosition1;
                                readerBuffer.setPosition(endPosition);
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }

                    window.postMessage(readerBuffer.slice(startPosition, endPosition));
                } else {
                    log.warning("The requested window " + requestedId + " doesn't exist anymore"); // TODO PERF LOG
                    readerBuffer.shiftNextBlock(false);
                }
            } else if (ServerToClientModel.FRAME_ID == model) {
                final int requestedId = binaryModel.getIntValue();
                final PTFrame frame = (PTFrame) getPTObject(requestedId);
                frame.postMessage(readerBuffer.slice(readerBuffer.getPosition(), nextBlockPosition));
            } else if (ServerToClientModel.PING_SERVER == model) {
                final PTInstruction requestData = new PTInstruction();
                requestData.put(ClientToServerModel.PING_SERVER, binaryModel.getLongValue());
                requestBuilder.send(requestData);
                readerBuffer.readBinaryModel(); // Read ServerToClientModel.END element
            } else if (ServerToClientModel.HEARTBEAT == model) {
                readerBuffer.readBinaryModel(); // Read ServerToClientModel.END element
            } else if (ServerToClientModel.CREATE_CONTEXT == model) {
                PonySDK.get().setContextId(binaryModel.getIntValue());
                readerBuffer.readBinaryModel(); // Read ServerToClientModel.END element
            } else if (ServerToClientModel.DESTROY_CONTEXT == model) {
                destroy();
                readerBuffer.readBinaryModel(); // Read ServerToClientModel.END element
            } else {
                update(binaryModel, readerBuffer);
            }
        }
    }

    public void updateWindowTerminal(final Uint8Array buffer) {
        readerBuffer.init(buffer);

        while (readerBuffer.hasEnoughKeyBytes()) {
            // Detect if the message is not for the window but for a specific frame
            BinaryModel binaryModel = readerBuffer.readBinaryModel();

            if (ServerToClientModel.WINDOW_ID == binaryModel.getModel()) binaryModel = readerBuffer.readBinaryModel();

            if (ServerToClientModel.FRAME_ID == binaryModel.getModel()) {
                final int requestedId = binaryModel.getIntValue();
                final PTFrame frame = (PTFrame) getPTObject(requestedId);
                if (log.isLoggable(Level.FINE)) log.fine("The main terminal send the buffer to frame " + requestedId);
                frame.postMessage(readerBuffer.slice(readerBuffer.getPosition(), readerBuffer.shiftNextBlock(true)));
            } else {
                update(binaryModel, readerBuffer);
            }
        }
    }

    public void updateFrameTerminal(final Uint8Array buffer) {
        readerBuffer.init(buffer);

        update(readerBuffer.readBinaryModel(), readerBuffer);
    }

    private void update(final BinaryModel binaryModel, final ReaderBuffer buffer) {
        final ServerToClientModel model = binaryModel.getModel();

        try {
            if (ServerToClientModel.TYPE_CREATE == model) {
                final int objectID = binaryModel.getIntValue();
                processCreate(buffer, objectID);
                processUpdate(buffer, objectID);
            } else if (ServerToClientModel.TYPE_UPDATE == model) {
                processUpdate(buffer, binaryModel.getIntValue());
            } else if (ServerToClientModel.TYPE_ADD == model) {
                processAdd(buffer, binaryModel.getIntValue());
            } else if (ServerToClientModel.TYPE_GC == model) {
                processGC(binaryModel.getIntValue());
            } else if (ServerToClientModel.TYPE_REMOVE == model) {
                processRemove(buffer, binaryModel.getIntValue());
            } else if (ServerToClientModel.TYPE_ADD_HANDLER == model) {
                processAddHandler(buffer, binaryModel.getIntValue());
            } else if (ServerToClientModel.TYPE_REMOVE_HANDLER == model) {
                processRemoveHandler(buffer, binaryModel.getIntValue());
            } else if (ServerToClientModel.TYPE_HISTORY == model) {
                processHistory(buffer, binaryModel.getStringValue());
            } else {
                log.log(Level.WARNING, "Unknown instruction type : " + binaryModel + " ; " + buffer.toString());
                buffer.shiftNextBlock(false);
            }
            buffer.readBinaryModel(); // Read ServerToClientModel.END element
        } catch (final AvoidBlockException e) {
            buffer.shiftNextBlock(false);
        }
    }

    private void processCreate(final ReaderBuffer buffer, final int objectID) throws AvoidBlockException {
        // ServerToClientModel.WIDGET_TYPE
        final WidgetType widgetType = WidgetType.fromRawValue(buffer.readBinaryModel().getByteValue());

        final PTObject ptObject = uiFactory.newUIObject(widgetType);
        if (ptObject != null) {
            ptObject.create(buffer, objectID, this);
            objectByID.put(objectID, ptObject);
        } else {
            log.warning("Cannot create PObject #" + objectID + " with widget type : " + widgetType);
            throw new AvoidBlockException();
        }
    }

    private void processAdd(final ReaderBuffer buffer, final int objectID) throws AvoidBlockException {
        final PTObject ptObject = getPTObject(objectID);
        if (ptObject != null) {
            // ServerToClientModel.PARENT_OBJECT_ID
            final int parentId = buffer.readBinaryModel().getIntValue();
            final PTObject parentObject = getPTObject(parentId);
            if (parentObject != null) {
                parentObject.add(buffer, ptObject);
            } else {
                log.warning("Cannot add " + ptObject + " to an garbaged parent object #" + parentId
                        + ", so we will consume all the buffer of this object");
                throw new AvoidBlockException();
            }
        } else {
            log.warning("Add a null PTObject #" + objectID + ", so we will consume all the buffer of this object");
            throw new AvoidBlockException();
        }
    }

    private void processUpdate(final ReaderBuffer buffer, final int objectID) throws AvoidBlockException {
        final PTObject ptObject = getPTObject(objectID);
        if (ptObject != null) {
            BinaryModel binaryModel;
            boolean result = false;
            do {
                binaryModel = buffer.readBinaryModel();
                final ServerToClientModel model = binaryModel.getModel();
                if (model != null) result = ServerToClientModel.END != model ? ptObject.update(buffer, binaryModel) : false;
            } while (result && buffer.hasEnoughKeyBytes());

            if (!result) buffer.rewind(binaryModel);
        } else {
            log.warning("Update on a null PTObject #" + objectID + ", so we will consume all the buffer of this object");
            throw new AvoidBlockException();
        }
    }

    private void processRemove(final ReaderBuffer buffer, final int objectID) throws AvoidBlockException {
        final PTObject ptObject = getPTObject(objectID);
        if (ptObject != null) {
            final int parentId = buffer.readBinaryModel().getIntValue();
            final PTObject parentObject = parentId != -1 ? getPTObject(parentId) : ptObject;

            if (parentObject != null) {
                parentObject.remove(buffer, ptObject);
            } else {
                log.warning("Cannot remove " + ptObject + " on a garbaged object #" + parentId);
                throw new AvoidBlockException();
            }
        } else {
            log.warning("Remove a null PTObject #" + objectID + ", so we will consume all the buffer of this object");
            throw new AvoidBlockException();
        }
    }

    private void processAddHandler(final ReaderBuffer buffer, final int objectID) throws AvoidBlockException {
        final PTObject ptObject = getPTObject(objectID);

        // ServerToClientModel.HANDLER_TYPE
        final HandlerModel handlerModel = HandlerModel.fromRawValue(buffer.readBinaryModel().getByteValue());

        if (HandlerModel.HANDLER_STREAM_REQUEST == handlerModel) {
            new PTStreamResource().addHandler(buffer, handlerModel);
        } else if (ptObject != null) {
            ptObject.addHandler(buffer, handlerModel);
        } else {
            log.warning("Add handler on a null PTObject #" + objectID + ", so we will consume all the buffer of this object");
            throw new AvoidBlockException();
        }
    }

    private void processRemoveHandler(final ReaderBuffer buffer, final int objectID) throws AvoidBlockException {
        final PTObject ptObject = getPTObject(objectID);
        if (ptObject != null) {
            // ServerToClientModel.HANDLER_TYPE
            final HandlerModel handlerModel = HandlerModel.fromRawValue(buffer.readBinaryModel().getByteValue());
            ptObject.removeHandler(buffer, handlerModel);
        } else {
            log.warning("Remove handler on a null PTObject #" + objectID + ", so we will consume all the buffer of this object");
            throw new AvoidBlockException();
        }
    }

    private void processHistory(final ReaderBuffer buffer, final String token) {
        final String oldToken = History.getToken();

        // ServerToClientModel.HISTORY_FIRE_EVENTS
        final boolean fireEvents = buffer.readBinaryModel().getBooleanValue();
        if (oldToken != null && oldToken.equals(token)) {
            if (fireEvents) History.fireCurrentHistoryState();
        } else {
            History.newItem(token, fireEvents);
        }
    }

    private void processGC(final int objectID) throws AvoidBlockException {
        final PTObject ptObject = unregisterObject(objectID);
        if (ptObject != null) {
            ptObject.destroy();
        } else {
            log.warning("Cannot GC a garbaged PTObject #" + objectID);
            throw new AvoidBlockException();
        }
    }

    private PTObject unregisterObject(final int objectID) {
        final PTObject ptObject = objectByID.get(objectID);
        objectByID.remove(objectID);
        final UIObject uiObject = widgetIDByObjectID.get(objectID);
        widgetIDByObjectID.remove(objectID);
        if (uiObject != null) objectIDByWidget.remove(uiObject);
        return ptObject;
    }

    private void destroy() {
        PTWindowManager.closeAll();
        Browser.getWindow().getLocation().reload();
    }

    public void sendDataToServer(final Widget widget, final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) {
            if (widget != null) {
                final Element source = widget.getElement();
                if (source != null) log.fine("Action triggered, Instruction [" + instruction + "] , " + source.getInnerHTML());
            }
        }
        sendDataToServer(instruction);
    }

    public void sendDataToServer(final JSONValue instruction) {
        requestBuilder.send(instruction);
    }

    public void sendDataToServer(final JSONObject instruction) {
        final PTInstruction requestData = new PTInstruction();
        final JSONArray jsonArray = new JSONArray();
        jsonArray.set(0, instruction);
        requestData.put(ClientToServerModel.APPLICATION_INSTRUCTIONS, jsonArray);

        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Data to send " + requestData.toString());

        requestBuilder.send(requestData);
    }

    public void sendErrorMessageToServer(final String message) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(ClientToServerModel.ERROR_MSG, message);
        requestBuilder.send(requestData);
    }

    public void sendErrorMessageToServer(final String message, final int objectID) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(ClientToServerModel.ERROR_MSG, message);
        requestData.put(ClientToServerModel.OBJECT_ID, objectID);
        requestBuilder.send(requestData);
    }

    public void sendWarningMessageToServer(final String message) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(ClientToServerModel.WARNING_MSG, message);
        requestBuilder.send(requestData);
    }

    public void sendWarningMessageToServer(final String message, final int objectID) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(ClientToServerModel.WARNING_MSG, message);
        requestData.put(ClientToServerModel.OBJECT_ID, objectID);
        requestBuilder.send(requestData);
    }

    public void sendInfoMessageToServer(final String message) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(ClientToServerModel.INFO_MSG, message);
        requestBuilder.send(requestData);
    }

    public void sendInfoMessageToServer(final String message, final int objectID) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(ClientToServerModel.INFO_MSG, message);
        requestData.put(ClientToServerModel.OBJECT_ID, objectID);
        requestBuilder.send(requestData);
    }

    public PTObject getPTObject(final int id) {
        final PTObject ptObject = objectByID.get(id);
        if (ptObject == null) {
            log.warning("PTObject #" + id + " not found");
            sendWarningMessageToServer("PTObject #" + id + " not found", id);
        }
        return ptObject;
    }

    public PTObject getPTObject(final UIObject uiObject) {
        final Integer objectID = objectIDByWidget.get(uiObject);
        if (objectID != null) return getPTObject(objectID.intValue());
        return null;
    }

    public void registerUIObject(final Integer ID, final UIObject uiObject) {
        objectIDByWidget.put(uiObject, ID);
        widgetIDByObjectID.put(ID, uiObject);
    }

    void registerJavascriptAddOnFactory(final String signature, final JavascriptAddOnFactory javascriptAddOnFactory) {
        this.javascriptAddOnFactories.put(signature, javascriptAddOnFactory);
    }

    public JsMapFromStringTo<JavascriptAddOnFactory> getJavascriptAddOnFactory() {
        return javascriptAddOnFactories;
    }

    void setReadyWindow(final int windowID) {
        final PTWindow window = PTWindowManager.getWindow(windowID);
        if (window != null) window.setReady();
        else log.warning("Window " + windowID + " doesn't exist");
    }

    void setReadyFrame(final int frameID) {
        final PTFrame frame = (PTFrame) getPTObject(frameID);
        if (frame != null) frame.setReady();
        else log.warning("Frame " + frame + " doesn't exist");
    }

}
