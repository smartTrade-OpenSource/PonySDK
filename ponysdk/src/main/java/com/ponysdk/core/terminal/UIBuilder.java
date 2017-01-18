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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
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
import com.ponysdk.core.terminal.ui.PTObject;
import com.ponysdk.core.terminal.ui.PTStreamResource;
import com.ponysdk.core.terminal.ui.PTWindow;
import com.ponysdk.core.terminal.ui.PTWindowManager;

public class UIBuilder implements ValueChangeHandler<String>, HttpResponseReceivedEvent.Handler, HttpRequestSendEvent.Handler {

    private static final Logger log = Logger.getLogger(UIBuilder.class.getName());

    private static final EventBus rootEventBus = new SimpleEventBus();

    private static final WidgetType[] WIDGET_TYPES = WidgetType.values();

    private final UIFactory uiFactory = new UIFactory();
    private final Map<Integer, PTObject> objectByID = new HashMap<>();
    private final Map<UIObject, Integer> objectIDByWidget = new HashMap<>();
    private final Map<Integer, UIObject> widgetIDByObjectID = new HashMap<>();
    private final List<JSONObject> stackedErrors = new ArrayList<>();
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

        History.addValueChangeHandler(this);

        rootEventBus.addHandler(HttpResponseReceivedEvent.TYPE, this);
        rootEventBus.addHandler(HttpRequestSendEvent.TYPE, this);

        loadingMessageBox = new SimplePanel();

        communicationErrorMessagePanel = new PopupPanel(false, true);
        communicationErrorMessagePanel.setGlassEnabled(true);
        communicationErrorMessagePanel.setStyleName("pony-notification");
        communicationErrorMessagePanel.addStyleName("error");

        RootPanel.get().add(loadingMessageBox);

        loadingMessageBox.setStyleName("pony-LoadingMessageBox");
        loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        loadingMessageBox.getElement().setInnerText("Loading ...");

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
            if (communicationErrorHandler != null) {
                communicationErrorHandler.onCommunicationError(statusCodeException.getStatusCode(), statusCodeException.getMessage());
            } else {
                showCommunicationErrorMessage(statusCodeException);
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

    public void updateMainTerminal(final ReaderBuffer buffer) {
        while (buffer.hasRemaining()) {
            // Detect if the message is not for the main terminal but for a specific window
            final BinaryModel binaryModel = buffer.readBinaryModel();
            if (ServerToClientModel.WINDOW_ID.equals(binaryModel.getModel())) {
                // Event on a specific window
                final int requestedWindowId = binaryModel.getIntValue();
                // Main terminal, we need to dispatch the eventbus
                final PTWindow window = PTWindowManager.getWindow(requestedWindowId);
                if (window != null && window.isReady()) {
                    log.log(Level.FINE, "The main terminal send the buffer to window " + requestedWindowId);

                    final int startPosition = buffer.getIndex();

                    // Type
                    final BinaryModel type = buffer.readBinaryModel();
                    buffer.avoidBlock();
                    final int endPosition = buffer.getIndex();

                    window.postMessage(buffer.slice(startPosition, endPosition));
                } else {
                    log.log(Level.SEVERE, "The requested window " + requestedWindowId + " doesn't exist or not ready");

                    // Type
                    final BinaryModel type = buffer.readBinaryModel();
                    log.log(Level.WARNING, "Consume message block : " + type);

                    while (buffer.hasRemaining()) {
                        final BinaryModel model = buffer.readBinaryModel();
                        log.log(Level.WARNING, "Consume message block : " + model);
                        if (ServerToClientModel.WINDOW_ID.equals(model.getModel())) {
                            if (model.getIntValue() != requestedWindowId) {
                                buffer.rewind(model);
                                break;
                            } else {
                                // Type
                                buffer.readBinaryModel();
                            }
                        } else if (model.isBeginKey()) {
                            buffer.rewind(model);
                            break;
                        }
                    }
                }
            } else {
                update(binaryModel, buffer);
            }
        }
    }

    public void updatWindowTerminal(final ReaderBuffer buffer) {
        update(buffer.readBinaryModel(), buffer);
    }

    private void update(final BinaryModel binaryModel, final ReaderBuffer buffer) {
        if (binaryModel.getModel() == null) return;

        if (ServerToClientModel.TYPE_CREATE.equals(binaryModel.getModel())) {
            final PTObject ptObject = processCreate(buffer, binaryModel.getIntValue());
            processUpdate(buffer, ptObject);
        } else if (ServerToClientModel.TYPE_UPDATE.equals(binaryModel.getModel())) {
            processUpdate(buffer, getPTObject(binaryModel.getIntValue()));
        } else if (ServerToClientModel.TYPE_ADD.equals(binaryModel.getModel())) {
            processAdd(buffer, getPTObject(binaryModel.getIntValue()));
        } else if (ServerToClientModel.TYPE_GC.equals(binaryModel.getModel())) {
            processGC(binaryModel.getIntValue());
        } else if (ServerToClientModel.TYPE_REMOVE.equals(binaryModel.getModel())) {
            processRemove(buffer, binaryModel.getIntValue());
        } else if (ServerToClientModel.TYPE_ADD_HANDLER.equals(binaryModel.getModel())) {
            processAddHandler(buffer, HandlerModel.values()[binaryModel.getByteValue()]);
        } else if (ServerToClientModel.TYPE_REMOVE_HANDLER.equals(binaryModel.getModel())) {
            processRemoveHandler(buffer, getPTObject(binaryModel.getIntValue()));
        } else if (ServerToClientModel.TYPE_HISTORY.equals(binaryModel.getModel())) {
            processHistory(buffer, binaryModel.getStringValue());
        } else if (ServerToClientModel.TYPE_CLOSE.equals(binaryModel.getModel())) {
            processClose();
        } else {
            log.log(Level.WARNING, "Unknown instruction type : " + binaryModel);
            buffer.avoidBlock();
        }
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
            buffer.avoidBlock();
        }
    }

    private void processUpdate(final ReaderBuffer buffer, final PTObject ptObject) {
        if (ptObject != null) {
            BinaryModel binaryModel;
            boolean result = false;
            do {
                binaryModel = buffer.readBinaryModel();
                if (binaryModel.getModel() != null) result = ptObject.update(buffer, binaryModel);
            } while (result && buffer.hasRemaining());

            if (!result) buffer.rewind(binaryModel);
        } else {
            log.warning("Update on a null PTObject, so we will consume all the buffer of this object");
            buffer.avoidBlock();
        }
    }

    private void processRemove(final ReaderBuffer buffer, final int objectId) {
        final PTObject ptObject = getPTObject(objectId);
        if (ptObject != null) {
            final int parentId = buffer.readBinaryModel().getIntValue();
            final PTObject parentObject = parentId != -1 ? getPTObject(parentId) : ptObject;

            if (parentObject != null) {
                parentObject.remove(buffer, ptObject, this);
            } else log.warning("Cannot remove PTObject " + ptObject + " on a garbaged object #" + parentId);
        } else {
            log.warning("Remove a null PTObject #" + objectId + ", so we will consume all the buffer of this object");
            buffer.avoidBlock();
        }
    }

    private void processAddHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_STREAM_REQUEST.equals(handlerModel)) {
            new PTStreamResource().addHandler(buffer, handlerModel, this);
        } else {
            // ServerToClientModel.OBJECT_ID
            final int id = buffer.readBinaryModel().getIntValue();
            final PTObject ptObject = getPTObject(id);
            if (ptObject != null) ptObject.addHandler(buffer, handlerModel, this);
            else log.warning("Cannot add handler on a garbaged object #" + id);
        }
    }

    private void processRemoveHandler(final ReaderBuffer buffer, final PTObject ptObject) {
        if (ptObject != null) {
            ptObject.removeHandler(buffer, this);
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
        if (ptObject != null) ptObject.gc(this);
        else log.warning("Cannot GC a garbaged object #" + objectId);
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
        final JSONArray jsonArray = new JSONArray();
        jsonArray.set(0, instruction);

        sendDataToServer(jsonArray);
    }

    private void sendDataToServer(final JSONArray jsonArray) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(ClientToServerModel.APPLICATION_INSTRUCTIONS, jsonArray);

        if (!stackedErrors.isEmpty()) {
            final JSONArray errors = new JSONArray();
            int i = 0;
            for (final JSONObject jsoObject : stackedErrors)
                errors.set(i++, jsoObject);
            stackedErrors.clear();
            requestData.put(ClientToServerModel.APPLICATION_ERRORS, errors);
        }

        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Data to send " + requestData.toString());

        requestBuilder.send(requestData);
    }

    private Timer scheduleLoadingMessageBox() {
        if (loadingMessageBox == null) return null;

        final Timer timer = new Timer() {

            @Override
            public void run() {
                loadingMessageBox.getElement().getStyle().setVisibility(Visibility.VISIBLE);
            }
        };
        timer.schedule(500);
        return timer;
    }

    private void showCommunicationErrorMessage(final StatusCodeException caught) {
        final VerticalPanel content = new VerticalPanel();
        final HorizontalPanel actionPanel = new HorizontalPanel();
        actionPanel.setSize("100%", "100%");

        if (caught.getStatusCode() == ServerException.INVALID_SESSION) {
            content.add(new HTML(
                "Server connection failed <br/>Code : " + caught.getStatusCode() + "<br/>" + "Cause : " + caught.getMessage()));

            final Anchor reloadAnchor = new Anchor("reload");
            reloadAnchor.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(final ClickEvent event) {
                    History.newItem("");
                    Window.Location.reload();
                }
            });

            actionPanel.add(reloadAnchor);
            actionPanel.setCellHorizontalAlignment(reloadAnchor, HasHorizontalAlignment.ALIGN_CENTER);
            actionPanel.setCellVerticalAlignment(reloadAnchor, HasVerticalAlignment.ALIGN_MIDDLE);
        } else {
            content.add(new HTML(
                "An unexpected error occured <br/>Code : " + caught.getStatusCode() + "<br/>" + "Cause : " + caught.getMessage()));
        }

        final Anchor closeAnchor = new Anchor("close");
        closeAnchor.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                communicationErrorMessagePanel.hide();
            }
        });
        actionPanel.add(closeAnchor);
        actionPanel.setCellHorizontalAlignment(closeAnchor, HasHorizontalAlignment.ALIGN_CENTER);
        actionPanel.setCellVerticalAlignment(closeAnchor, HasVerticalAlignment.ALIGN_MIDDLE);

        content.add(actionPanel);

        communicationErrorMessagePanel.setWidget(content);
        communicationErrorMessagePanel.setPopupPositionAndShow(new PositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight) {
                communicationErrorMessagePanel.setPopupPosition((Window.getClientWidth() - offsetWidth) / 2,
                    (Window.getClientHeight() - offsetHeight) / 2);
            }
        });
    }

    @Override
    public void onValueChange(final ValueChangeEvent<String> event) {
        if (event.getValue() != null && !event.getValue().isEmpty()) {
            final PTInstruction eventInstruction = new PTInstruction();
            eventInstruction.put(ClientToServerModel.TYPE_HISTORY, event.getValue());
            sendDataToServer(eventInstruction);
        }
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

    @Override
    public void onHttpRequestSend(final HttpRequestSendEvent event) {
        numberOfrequestInProgress++;
        if (timer == null) timer = scheduleLoadingMessageBox();
    }

    @Override
    public void onHttpResponseReceivedEvent(final HttpResponseReceivedEvent event) {
        if (numberOfrequestInProgress > 0) numberOfrequestInProgress--;
        hideLoadingMessageBox();
    }

    private void hideLoadingMessageBox() {
        if (loadingMessageBox != null && numberOfrequestInProgress < 1 && timer != null) {
            timer.cancel();
            timer = null;
            loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        }
    }

    void registerCommunicationError(final CommunicationErrorHandler communicationErrorClosure) {
        this.communicationErrorHandler = communicationErrorClosure;
    }

    void registerJavascriptAddOnFactory(final String signature, final JavascriptAddOnFactory javascriptAddOnFactory) {
        this.javascriptAddOnFactories.put(signature, javascriptAddOnFactory);
    }

    public Map<String, JavascriptAddOnFactory> getJavascriptAddOnFactory() {
        return javascriptAddOnFactories;
    }

    void setReadyWindow(final int windowID) {
        final PTWindow window = PTWindowManager.getWindow(windowID);
        if (window != null) window.setReady();
        else log.warning("Window " + windowID + " doesn't exist");
    }

}
