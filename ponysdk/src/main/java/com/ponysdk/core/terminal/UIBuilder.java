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
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.terminal.event.CommunicationErrorEvent;
import com.ponysdk.core.terminal.event.HttpRequestSendEvent;
import com.ponysdk.core.terminal.event.HttpResponseReceivedEvent;
import com.ponysdk.core.terminal.exception.ServerException;
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

public class UIBuilder implements HttpResponseReceivedEvent.Handler, HttpRequestSendEvent.Handler {

    private static final Logger log = Logger.getLogger(UIBuilder.class.getName());

    private static final EventBus rootEventBus = new SimpleEventBus();

    private static final WidgetType[] WIDGET_TYPES = WidgetType.values();

    private final UIFactory uiFactory = new UIFactory();
    private final Map<Integer, PTObject> objectByID = new HashMap<>();
    private final Map<UIObject, Integer> objectIDByWidget = new HashMap<>();
    private final Map<Integer, UIObject> widgetIDByObjectID = new HashMap<>();
    private final Map<String, JavascriptAddOnFactory> javascriptAddOnFactories = new HashMap<>();

    private SimplePanel loadingMessageBox;
    private PopupPanel communicationErrorMessagePanel;
    private Timer timer;
    private int numberOfrequestInProgress = 0;
    private boolean pendingClose;
    private RequestBuilder requestBuilder;
    private CommunicationErrorHandler communicationErrorHandler;

    public void init(final RequestBuilder requestBuilder) {
        if (log.isLoggable(Level.INFO)) log.info("Init request builder");

        this.requestBuilder = requestBuilder;

        PTHistory.addValueChangeHandler(this);

        rootEventBus.addHandler(HttpResponseReceivedEvent.TYPE, this);
        rootEventBus.addHandler(HttpRequestSendEvent.TYPE, this);

        loadingMessageBox = new SimplePanel();

        communicationErrorMessagePanel = new PopupPanel(false, true);
        communicationErrorMessagePanel.setGlassEnabled(true);
        communicationErrorMessagePanel.setStyleName("pony-notification");
        communicationErrorMessagePanel.addStyleName("error");

        RootPanel.get().add(loadingMessageBox);

        loadingMessageBox.setStyleName("pony-LoadingMessageBox");
        final com.google.gwt.user.client.Element element = loadingMessageBox.getElement();
        element.getStyle().setVisibility(Visibility.HIDDEN);
        element.setInnerText("Loading ...");

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

    public void updateMainTerminal(final ReaderBuffer buffer) {
        while (buffer.hasRemaining()) {
            final int nextBlockPosition = buffer.shiftNextBlock(true);
            if (nextBlockPosition == -1) return;

            // Detect if the message is not for the main terminal but for a specific window
            final BinaryModel binaryModel = buffer.readBinaryModel();
            if (ServerToClientModel.WINDOW_ID.equals(binaryModel.getModel())) {
                // Event on a specific window
                final int requestedId = binaryModel.getIntValue();
                // Main terminal, we need to dispatch the eventbus
                final PTWindow window = PTWindowManager.getWindow(requestedId);
                if (window != null && window.isReady()) {
                    if (log.isLoggable(Level.FINE)) log.fine("The main terminal send the buffer to window " + requestedId);
                    window.postMessage(buffer.slice(buffer.getPosition(), nextBlockPosition));
                } else {
                    log.warning("The requested window " + requestedId + " doesn't exist anymore"); // TODO PERF LOG
                    buffer.shiftNextBlock(false);
                }
            } else if (ServerToClientModel.FRAME_ID.equals(binaryModel.getModel())) {
                final int requestedId = binaryModel.getIntValue();
                final PTFrame frame = (PTFrame) getPTObject(requestedId);
                if (log.isLoggable(Level.FINE)) log.fine("The main terminal send the buffer to frame " + requestedId);
                frame.postMessage(buffer.slice(buffer.getPosition(), nextBlockPosition));
            } else if (ServerToClientModel.PING_SERVER.equals(binaryModel.getModel())) {
                if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Ping received");
                final PTInstruction requestData = new PTInstruction();
                requestData.put(ClientToServerModel.PING_SERVER, binaryModel.getLongValue());
                requestBuilder.send(requestData);
                buffer.readBinaryModel(); // Read ServerToClientModel.END element
            } else if (ServerToClientModel.HEARTBEAT.equals(binaryModel.getModel())) {
                if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Heart beat received");
                buffer.readBinaryModel(); // Read ServerToClientModel.END element
            } else if (ServerToClientModel.CREATE_CONTEXT.equals(binaryModel.getModel())) {
                PonySDK.get().setContextId(binaryModel.getIntValue());
                buffer.readBinaryModel(); // Read ServerToClientModel.END element
            } else {
                update(binaryModel, buffer);
            }
        }
    }

    public void updateWindowTerminal(final ReaderBuffer buffer) {
        // Detect if the message is not for the window but for a specific frame
        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.FRAME_ID.equals(binaryModel.getModel())) {
            final int requestedId = binaryModel.getIntValue();
            final PTFrame frame = (PTFrame) getPTObject(requestedId);
            if (log.isLoggable(Level.FINE)) log.fine("The main terminal send the buffer to frame " + requestedId);
            frame.postMessage(buffer.slice(buffer.getPosition(), buffer.shiftNextBlock(true)));
        } else {
            update(binaryModel, buffer);
        }
    }

    public void updateFrameTerminal(final ReaderBuffer buffer) {
        update(buffer.readBinaryModel(), buffer);
    }

    private void update(final BinaryModel binaryModel, final ReaderBuffer buffer) {
        final ServerToClientModel model = binaryModel.getModel();

        final int modelOrdinal = model.ordinal();
        if (ServerToClientModel.TYPE_CREATE.ordinal() == modelOrdinal) {
            final PTObject ptObject = processCreate(buffer, binaryModel.getIntValue());
            processUpdate(buffer, ptObject);
        } else if (ServerToClientModel.TYPE_UPDATE.ordinal() == modelOrdinal) {
            processUpdate(buffer, getPTObject(binaryModel.getIntValue()));
        } else if (ServerToClientModel.TYPE_ADD.ordinal() == modelOrdinal) {
            processAdd(buffer, getPTObject(binaryModel.getIntValue()));
        } else if (ServerToClientModel.TYPE_GC.ordinal() == modelOrdinal) {
            processGC(binaryModel.getIntValue());
        } else if (ServerToClientModel.TYPE_REMOVE.ordinal() == modelOrdinal) {
            processRemove(buffer, binaryModel.getIntValue());
        } else if (ServerToClientModel.TYPE_ADD_HANDLER.ordinal() == modelOrdinal) {
            processAddHandler(buffer, getPTObject(binaryModel.getIntValue()));
        } else if (ServerToClientModel.TYPE_REMOVE_HANDLER.ordinal() == modelOrdinal) {
            processRemoveHandler(buffer, getPTObject(binaryModel.getIntValue()));
        } else if (ServerToClientModel.TYPE_HISTORY.ordinal() == modelOrdinal) {
            processHistory(buffer, binaryModel.getStringValue());
        } else if (ServerToClientModel.TYPE_CLOSE.ordinal() == modelOrdinal) {
            processClose();
        } else {
            log.log(Level.WARNING, "Unknown instruction type : " + binaryModel + " ; " + buffer.toString());
            buffer.shiftNextBlock(false);
        }

        buffer.readBinaryModel(); // Read ServerToClientModel.END element
    }

    private PTObject processCreate(final ReaderBuffer buffer, final int objectIdValue) {
        // ServerToClientModel.WIDGET_TYPE
        final WidgetType widgetType = WIDGET_TYPES[buffer.readBinaryModel().getByteValue()];

        final PTObject ptObject = uiFactory.newUIObject(widgetType);
        if (ptObject != null) {
            ptObject.create(buffer, objectIdValue, this);
            objectByID.put(objectIdValue, ptObject);
        } else {
            log.warning("Cannot create object " + objectIdValue + " with widget type : " + widgetType);
        }

        return ptObject;
    }

    private void processAdd(final ReaderBuffer buffer, final PTObject ptObject) {
        // ServerToClientModel.PARENT_OBJECT_ID
        final int parentId = buffer.readBinaryModel().getIntValue();
        final PTObject parentObject = getPTObject(parentId);
        if (parentObject != null) {
            parentObject.add(buffer, ptObject);
        } else {
            log.warning("Cannot add " + ptObject + " to an garbaged parent object #" + parentId
                    + ", so we will consume all the buffer of this object");
            buffer.shiftNextBlock(false);
        }
    }

    private void processUpdate(final ReaderBuffer buffer, final PTObject ptObject) {
        if (ptObject != null) {
            BinaryModel binaryModel;
            boolean result = false;
            do {
                binaryModel = buffer.readBinaryModel();
                if (binaryModel.getModel() != null) {
                    result = !ServerToClientModel.END.equals(binaryModel.getModel()) ? ptObject.update(buffer, binaryModel) : false;
                }
            } while (result && buffer.hasRemaining());

            if (!result) buffer.rewind(binaryModel);
        } else {
            log.warning("Update on a null PTObject, so we will consume all the buffer of this object");
            buffer.shiftNextBlock(false);
        }
    }

    private void processRemove(final ReaderBuffer buffer, final int objectId) {
        final PTObject ptObject = getPTObject(objectId);
        if (ptObject != null) {
            final int parentId = buffer.readBinaryModel().getIntValue();
            final PTObject parentObject = parentId != -1 ? getPTObject(parentId) : ptObject;

            if (parentObject != null) parentObject.remove(buffer, ptObject);
            else log.warning("Cannot remove PTObject " + ptObject + " on a garbaged object #" + parentId);
        } else {
            log.warning("Remove a null PTObject #" + objectId + ", so we will consume all the buffer of this object");
            final int shiftNextBlock = buffer.shiftNextBlock(false);
            if (shiftNextBlock == -1) {
                log.severe("Shift");
            }
        }
    }

    private void processAddHandler(final ReaderBuffer buffer, final PTObject ptObject) {
        // ServerToClientModel.HANDLER_TYPE
        final HandlerModel handlerModel = HandlerModel.values()[buffer.readBinaryModel().getByteValue()];

        if (HandlerModel.HANDLER_STREAM_REQUEST.equals(handlerModel)) new PTStreamResource().addHandler(buffer, handlerModel);
        else if (ptObject != null) ptObject.addHandler(buffer, handlerModel);
    }

    private void processRemoveHandler(final ReaderBuffer buffer, final PTObject ptObject) {
        if (ptObject != null) {
            // ServerToClientModel.HANDLER_TYPE
            final HandlerModel handlerModel = HandlerModel.values()[buffer.readBinaryModel().getByteValue()];
            ptObject.removeHandler(buffer, handlerModel);
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

    private void processClose() {
        pendingClose = true;
        PTWindowManager.closeAll();
        Window.Location.reload();
    }

    private void processGC(final int objectId) {
        final PTObject ptObject = unregisterObject(objectId);
        if (ptObject != null) {
            ptObject.destroy();
        } else {
            log.warning("Cannot GC a garbaged object #" + objectId);
        }
    }

    private PTObject unregisterObject(final Integer objectId) {
        final PTObject ptObject = objectByID.remove(objectId);
        final UIObject uiObject = widgetIDByObjectID.remove(objectId);
        if (uiObject != null) objectIDByWidget.remove(uiObject);
        return ptObject;
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

    public void sendInfoMessageToServer(final String message) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(ClientToServerModel.INFO_MSG, message);
        requestBuilder.send(requestData);
    }

    public PTObject getPTObject(final Integer id) {
        final PTObject ptObject = objectByID.get(id);
        if (ptObject == null) log.warning("PTObject #" + id + " not found");
        return ptObject;
    }

    public PTObject getPTObject(final UIObject uiObject) {
        final Integer objectID = objectIDByWidget.get(uiObject);
        if (objectID != null) return getPTObject(objectID);
        return null;
    }

    public void registerUIObject(final Integer ID, final UIObject uiObject) {
        objectIDByWidget.put(uiObject, ID);
        widgetIDByObjectID.put(ID, uiObject);
    }

    void registerJavascriptAddOnFactory(final String signature, final JavascriptAddOnFactory javascriptAddOnFactory) {
        this.javascriptAddOnFactories.put(signature, javascriptAddOnFactory);
    }

    public Map<String, JavascriptAddOnFactory> getJavascriptAddOnFactory() {
        return javascriptAddOnFactories;
    }

    @Override
    public void onHttpRequestSend(final HttpRequestSendEvent event) {
        numberOfrequestInProgress++;
        if (timer == null) timer = scheduleLoadingMessageBox();
    }

    private Timer scheduleLoadingMessageBox() {
        if (loadingMessageBox != null) {
            final Timer timer = new Timer() {

                @Override
                public void run() {
                    loadingMessageBox.getElement().getStyle().setVisibility(Visibility.VISIBLE);
                }
            };
            timer.schedule(500);
            return timer;
        } else {
            return null;
        }
    }

    @Override
    public void onHttpResponseReceivedEvent(final HttpResponseReceivedEvent event) {
        if (numberOfrequestInProgress > 0) numberOfrequestInProgress--;
        if (loadingMessageBox != null && numberOfrequestInProgress < 1 && timer != null) {
            timer.cancel();
            timer = null;
            loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        }
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

    public void onCommunicationError(final Throwable exception) {
        rootEventBus.fireEvent(new CommunicationErrorEvent(exception));

        if (pendingClose) return;

        if (loadingMessageBox == null) {
            // First load failed
            if (exception instanceof StatusCodeException) {
                final StatusCodeException codeException = (StatusCodeException) exception;
                if (codeException.getStatusCode() == 0) return;
            }
            log.log(Level.SEVERE, "Cannot initialize the application : " + exception.getMessage() + "\n" + exception
                    + "\nPlease reload your application",
                exception);
        }

        if (exception instanceof StatusCodeException) {
            final StatusCodeException statusCodeException = (StatusCodeException) exception;
            final int statusCode = statusCodeException.getStatusCode();
            if (communicationErrorHandler != null) {
                communicationErrorHandler.onCommunicationError(statusCode, statusCodeException.getMessage());
            } else {
                final VerticalPanel content = new VerticalPanel();
                final HorizontalPanel actionPanel = new HorizontalPanel();
                actionPanel.setSize("100%", "100%");

                if (statusCode == ServerException.INVALID_SESSION) {
                    content.add(new HTML("Server connection failed <br/>Code : " + statusCode + "<br/>" + "Cause : "
                            + statusCodeException.getMessage()));

                    final Anchor reloadAnchor = new Anchor("reload");
                    reloadAnchor.addClickHandler(event -> {
                        History.newItem("");
                        Window.Location.reload();
                    });

                    actionPanel.add(reloadAnchor);
                    actionPanel.setCellHorizontalAlignment(reloadAnchor, HasHorizontalAlignment.ALIGN_CENTER);
                    actionPanel.setCellVerticalAlignment(reloadAnchor, HasVerticalAlignment.ALIGN_MIDDLE);
                } else {
                    content.add(new HTML("An unexpected error occured <br/>Code : " + statusCode + "<br/>" + "Cause : "
                            + statusCodeException.getMessage()));
                }

                final Anchor closeAnchor = new Anchor("close");
                closeAnchor.addClickHandler(event -> communicationErrorMessagePanel.hide());
                actionPanel.add(closeAnchor);
                actionPanel.setCellHorizontalAlignment(closeAnchor, HasHorizontalAlignment.ALIGN_CENTER);
                actionPanel.setCellVerticalAlignment(closeAnchor, HasVerticalAlignment.ALIGN_MIDDLE);

                content.add(actionPanel);

                communicationErrorMessagePanel.setWidget(content);
                communicationErrorMessagePanel.setPopupPositionAndShow((offsetWidth, offsetHeight) -> communicationErrorMessagePanel
                    .setPopupPosition((Window.getClientWidth() - offsetWidth) / 2, (Window.getClientHeight() - offsetHeight) / 2));
            }
        } else {
            if (communicationErrorHandler != null) {
                communicationErrorHandler.onCommunicationError(500, exception.getMessage());
            } else {
                log.log(Level.SEVERE, "An unexcepted error occured: " + exception.getMessage() + ". Please check the server logs.",
                    exception);
            }
        }
    }

    void registerCommunicationError(final CommunicationErrorHandler communicationErrorClosure) {
        this.communicationErrorHandler = communicationErrorClosure;
    }

}
