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

package com.ponysdk.ui.terminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.animation.client.AnimationScheduler.AnimationCallback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
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
import com.google.gwt.json.client.JSONString;
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
import com.ponysdk.ui.terminal.event.CommunicationErrorEvent;
import com.ponysdk.ui.terminal.event.HttpRequestSendEvent;
import com.ponysdk.ui.terminal.event.HttpResponseReceivedEvent;
import com.ponysdk.ui.terminal.exception.ServerException;
import com.ponysdk.ui.terminal.extension.AddonFactory;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.request.RequestBuilder;
import com.ponysdk.ui.terminal.ui.PTCookies;
import com.ponysdk.ui.terminal.ui.PTObject;
import com.ponysdk.ui.terminal.ui.PTStreamResource;

public class UIBuilder implements ValueChangeHandler<String>, UIService, HttpResponseReceivedEvent.Handler, HttpRequestSendEvent.Handler, AnimationCallback {

    private final static Logger log = Logger.getLogger(UIBuilder.class.getName());

    private static EventBus rootEventBus = new SimpleEventBus();

    private final UIFactory uiFactory = new UIFactory();
    private final Map<String, AddonFactory> addonByKey = new HashMap<>();
    private final Map<Integer, PTObject> objectByID = new HashMap<>();
    private final Map<UIObject, Integer> objectIDByWidget = new HashMap<>();
    private final Map<Integer, UIObject> widgetIDByObjectID = new HashMap<>();
    private final List<PTInstruction> stackedInstructions = new ArrayList<>();
    private final List<JSONObject> stackedErrors = new ArrayList<>();

    // private final Map<Integer, JSONObject> incomingMessageQueue = new HashMap<>();

    private SimplePanel loadingMessageBox;
    private PopupPanel communicationErrorMessagePanel;
    private Timer timer;
    private int numberOfrequestInProgress = 0;

    private boolean updateMode;
    private boolean pendingClose;

    private RequestBuilder requestBuilder;

    private long lastReceived = -1;
    // private final long nextSent = 1;

    public static int sessionID;

    private CommunicationErrorHandler communicationErrorHandler;
    private final Map<String, JavascriptAddOnFactory> javascriptAddOnFactories = new HashMap<>();

    private final List<PTInstruction> instructions = new ArrayList<>();

    private final Map<Integer, List<PTInstruction>> instructionsByObjectID = new HashMap<>();

    public UIBuilder() {
        History.addValueChangeHandler(this);

        // final AddonList addonList = GWT.create(PonyAddonList.class);

        // final List<AddonFactory> addonFactoryList = addonList.getAddonFactoryList();
        //
        // for (final AddonFactory addonFactory : addonFactoryList) {
        // addonByKey.put(addonFactory.getSignature(), addonFactory);
        // }

        rootEventBus.addHandler(HttpResponseReceivedEvent.TYPE, this);
        rootEventBus.addHandler(HttpRequestSendEvent.TYPE, this);

    }

    public void init(final int ID, final RequestBuilder requestBuilder) {
        if (log.isLoggable(Level.INFO)) log.info("Init request builder");

        this.requestBuilder = requestBuilder;
        UIBuilder.sessionID = ID;

        loadingMessageBox = new SimplePanel();

        communicationErrorMessagePanel = new PopupPanel(false, true);
        communicationErrorMessagePanel.setGlassEnabled(true);
        communicationErrorMessagePanel.setStyleName("pony-notification");
        communicationErrorMessagePanel.addStyleName("error");

        RootPanel.get().add(loadingMessageBox);

        loadingMessageBox.setStyleName("pony-LoadingMessageBox");
        loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        loadingMessageBox.getElement().setInnerText("Loading ...");

        final PTCookies cookies = new PTCookies();
        cookies.create(null, null);
        objectByID.put(0, cookies);

        // hide loading component
        final Widget w = RootPanel.get("loading");
        if (w == null) {
            log.log(Level.WARNING, "Include splash screen html element into your index.html with id=\"loading\"");
        } else {
            w.setSize("0px", "0px");
            w.setVisible(false);
        }
    }

    @Override
    public void onCommunicationError(final Throwable exception) {
        rootEventBus.fireEvent(new CommunicationErrorEvent(exception));

        if (pendingClose) return;

        if (loadingMessageBox == null) {
            // First load failed
            if (exception instanceof StatusCodeException) {
                final StatusCodeException codeException = (StatusCodeException) exception;
                if (codeException.getStatusCode() == 0) return;
            }
            log.log(Level.SEVERE, "Cannot inititialize the application : " + exception.getMessage() + "\n" + exception + "\nPlease reload your application", exception);
            return;
        }

        if (communicationErrorHandler != null) {
            if (exception instanceof StatusCodeException) {
                final StatusCodeException statusCodeException = (StatusCodeException) exception;
                communicationErrorHandler.onCommunicationError("" + statusCodeException.getStatusCode(), statusCodeException.getMessage());
            } else {
                communicationErrorHandler.onCommunicationError("x", exception.getMessage());
            }
        } else {
            if (exception instanceof StatusCodeException) {
                final StatusCodeException statusCodeException = (StatusCodeException) exception;
                showCommunicationErrorMessage(statusCodeException);
            } else {
                log.log(Level.SEVERE, "An unexcepted error occured: " + exception.getMessage() + ". Please check the server logs.", exception);
            }
        }
    }

    @Override
    public void update(final JSONObject data) {
        final JSONArray jsonArray = data.get(Model.APPLICATION_INSTRUCTIONS.getKey()).isArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            final PTInstruction instruction = new PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject());
            instructions.add(instruction);
        }

        if (jsonArray.size() > 0) AnimationScheduler.get().requestAnimationFrame(this);
    }

    @Override
    public void stackError(final PTInstruction currentInstruction, final Throwable e) {
        String msg;
        if (e.getMessage() == null) msg = "NA";
        else msg = e.getMessage();

        final JSONObject jsoObject = new JSONObject();
        jsoObject.put("message", new JSONString("PonySDK has encountered an internal error on instruction : " + currentInstruction));
        jsoObject.put("details", new JSONString(msg));
        stackedErrors.add(jsoObject);
    }

    @Override
    public void processInstruction(final PTInstruction instruction) {
        if (instruction.containsKey(Model.TYPE_CREATE)) processCreate(instruction);
        else if (instruction.containsKey(Model.TYPE_ADD)) processAdd(instruction);
        else if (instruction.containsKey(Model.TYPE_UPDATE)) processUpdate(instruction);
        else if (instruction.containsKey(Model.TYPE_REMOVE)) processRemove(instruction);
        else if (instruction.containsKey(Model.TYPE_ADD_HANDLER)) processAddHandler(instruction);
        else if (instruction.containsKey(Model.TYPE_REMOVE_HANDLER)) processRemoveHandler(instruction);
        else if (instruction.containsKey(Model.TYPE_HISTORY)) processHistory(instruction);
        else if (instruction.containsKey(Model.TYPE_CLOSE)) processClose(instruction);
        else if (instruction.containsKey(Model.TYPE_GC)) processGC(instruction);
        else log.log(Level.WARNING, "Unknown instruction type : " + instruction);
    }

    private void processCreate(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Create instruction : " + instruction);

        final PTObject ptObject;
        // TEMP

        // final int widgetType = instruction.getInt(Model.WIDGET_TYPE);
        // if (widgetType == WidgetType.WINDOW.ordinal()) {
        // GWT.log("Je vais creer la window : " + instruction.getObjectID());
        //
        // ptObject = uiFactory.newUIObject(this, instruction);
        // ptObject.create(instruction, this);
        // objectByID.put(instruction.getObjectID(), ptObject);
        // } else {
        // processSubCreate(instruction);
        // }

        processSubCreate(instruction);
    }

    private void processSubCreate(final PTInstruction instruction) {
        PTObject ptObject;
        final boolean isAddon = instruction.containsKey("addOnSignature");
        if (isAddon) {
            final String addOnSignature = instruction.getString("addOnSignature");
            final AddonFactory addonFactory = addonByKey.get(addOnSignature);
            if (addonFactory != null) {
                ptObject = addonFactory.newAddon();
                if (ptObject != null) {
                    ptObject.create(instruction, this);
                    if (log.isLoggable(Level.FINE))
                        log.log(Level.FINE, "Add object " + instruction.getObjectID() + " with widget type : "
                                + instruction.getInt(Model.WIDGET_TYPE));
                    objectByID.put(instruction.getObjectID(), ptObject);
                } else throw new RuntimeException("UIBuilder: Failed to instanciate an Addon of type: " + addOnSignature);
            } else throw new RuntimeException("UIBuilder: AddOn factory not found for signature: " + addOnSignature + ", available: " + addonByKey.keySet());
        } else {
            // stackInstruction(instruction);

            ptObject = uiFactory.newUIObject(this, instruction);
            if (ptObject != null) {
                ptObject.create(instruction, this);
                if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
                        "Add object " + instruction.getObjectID() + " with widget type : " + instruction.getInt(Model.WIDGET_TYPE));
                objectByID.put(instruction.getObjectID(), ptObject);
            } else log.warning("Cannot create object " + instruction.getObjectID() + " with widget type : " + instruction.getInt(Model.WIDGET_TYPE));
        }
    }

    private void processAdd(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Add instruction : " + instruction);

        PTObject ptObject;

        // if (instruction.containsKey(Model.WINDOW_ID)) {
        // final List<PTInstruction> waitingInstructions =
        // instructionsByObjectID.remove(instruction.getObjectID());
        // if (waitingInstructions != null) {
        // final int windowID = instruction.getInt(Model.WINDOW_ID);
        // for (final PTInstruction waitingInstruction : waitingInstructions) {
        // GWT.log("Transfert data to window : " + waitingInstruction);
        //
        // GWT.log("Window search : " + windowID);
        //
        // GWT.log("Window found : " + PTWindowManager.getWindow(windowID));
        //
        // PTWindowManager.getWindow(windowID).postMessage(waitingInstruction);
        //
        // // ptObject = uiFactory.newUIObject(this, ptInstruction);
        // // ptObject.create(ptInstruction, this);
        // // objectByID.put(ptInstruction.getObjectID(), ptObject);
        // }
        // }
        // }
        //
        // if (instructionsByObjectID.containsKey(instruction.getObjectID())) {
        // stackInstruction(instruction);
        // } else {
        // ptObject = objectByID.get(instruction.getObjectID());
        //
        // if (ptObject == null) {
        // // TODO throw
        // log.info("Cannot update a garbaged object #" + instruction.getObjectID());
        // return;
        // }
        //
        // ptObject.update(instruction, this);
        // // }

        ptObject = objectByID.get(instruction.getParentID());
        if (ptObject != null) ptObject.add(instruction, this);
        else log.warning("Cannot add object " + instruction.getObjectID() + " to an garbaged parent object #" + instruction.getParentID());
    }

    private void processUpdate(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Update instruction : " + instruction);

        PTObject ptObject;
        // if (instruction.containsKey(Model.WINDOW_ID)) {
        //
        // final List<PTInstruction> waitingInstructions =
        // instructionsByObjectID.remove(instruction.getObjectID());
        // if (waitingInstructions != null) {
        // final int windowID = instruction.getInt(Model.WINDOW_ID);
        //
        // for (final PTInstruction waitingInstruction : waitingInstructions) {
        // GWT.log("Transfert data to window : " + waitingInstruction);
        // PTWindowManager.getWindow(windowID).postMessage(waitingInstruction);
        // // ptObject = uiFactory.newUIObject(this, ptInstruction);
        // // ptObject.create(ptInstruction, this);
        // // objectByID.put(ptInstruction.getObjectID(), ptObject);
        // }
        // }
        // }
        //
        // if (instructionsByObjectID.containsKey(instruction.getObjectID())) {
        // stackInstruction(instruction);
        // } else {
        ptObject = objectByID.get(instruction.getObjectID());

        if (ptObject != null) ptObject.update(instruction, this);
        else log.warning("Cannot update a garbaged object #" + instruction.getObjectID());
        // }
    }

    private void processRemove(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Remove instruction : " + instruction);

        PTObject ptObject;

        if (instruction.getParentID() == -1) ptObject = objectByID.get(instruction.getObjectID());
        else ptObject = objectByID.get(instruction.getParentID());

        if (ptObject != null) ptObject.remove(instruction, this);
        else log.warning("Cannot remove a garbaged object #" + instruction.getObjectID());
    }

    private void processAddHandler(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Add handler instruction : " + instruction);

        PTObject ptObject;
        if (instruction.containsKey(Model.HANDLER_STREAM_REQUEST_HANDLER)) {
            new PTStreamResource().addHandler(instruction, this);
        } else {
            ptObject = objectByID.get(instruction.getObjectID());
            if (ptObject != null) ptObject.addHandler(instruction, this);
            else log.warning("Cannot add handler on a garbaged object #" + instruction.getObjectID());
        }
    }

    private void processRemoveHandler(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Remove handler instruction : " + instruction);

        final PTObject ptObject = objectByID.get(instruction.getObjectID());
        if (ptObject != null) ptObject.removeHandler(instruction, this);
        else log.warning("Cannot remove handler on a garbaged object #" + instruction.getObjectID());
    }

    private void processHistory(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "History instruction : " + instruction);

        final String oldToken = History.getToken();

        String token = null;
        if (instruction.containsKey(Model.HISTORY_TOKEN)) token = instruction.getString(Model.HISTORY_TOKEN);

        if (oldToken != null && oldToken.equals(token)) {
            if (instruction.getBoolean(Model.HISTORY_FIRE_EVENTS)) {
                History.fireCurrentHistoryState();
            }
        } else {
            History.newItem(token, instruction.getBoolean(Model.HISTORY_FIRE_EVENTS));
        }
    }

    private void processClose(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Close instruction : " + instruction);

        pendingClose = true;
        sendDataToServer(instruction);

        // TODO nciaravola no need

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                PonySDK.reload();
            }
        });
    }

    private void processGC(final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "GC instruction : " + instruction);

        final PTObject ptObject = unRegisterObject(instruction.getObjectID());
        if (ptObject != null) ptObject.gc(this);
        else log.warning("Cannot GC a garbaged object #" + instruction.getObjectID());
    }

    /**
     * Stack instruction until window information is not done
     *
     * @param instruction
     */
    private void stackInstruction(final PTInstruction instruction) {
        List<PTInstruction> instructions = instructionsByObjectID.get(instruction.getObjectID());
        if (instructions == null) {
            instructions = new ArrayList<>();

            if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Stack Instruction : " + instruction);

            instructionsByObjectID.put(instruction.getObjectID(), instructions);
        }

        instructions.add(instruction); // wait window information
    }

    protected void updateIncomingSeqNum(final long receivedSeqNum) {
        final long previous = lastReceived;
        if ((previous + 1) != receivedSeqNum) {
            log.log(Level.SEVERE, "Wrong seqnum received. Expecting #" + (previous + 1) + " but received #" + receivedSeqNum);
        }
        lastReceived = receivedSeqNum;
    }

    @Override
    public PTObject unRegisterObject(final int objectId) {
        final PTObject ptObject = objectByID.remove(objectId);
        final UIObject uiObject = widgetIDByObjectID.remove(objectId);
        if (uiObject != null) {
            objectIDByWidget.remove(uiObject);
        }
        return ptObject;
    }

    @Override
    public void stackInstrution(final PTInstruction instruction) {
        if (!updateMode) sendDataToServer(instruction);
        else stackedInstructions.add(instruction);
    }

    @Override
    public void flushEvents() {
        if (stackedInstructions.isEmpty()) return;

        sendDataToServer(stackedInstructions);

        stackedInstructions.clear();
    }

    @Override
    public void sendDataToServer(final Widget widget, final PTInstruction instruction) {
        if (log.isLoggable(Level.FINE)) {
            if (widget != null) {
                final Element source = widget.getElement();
                if (source != null) {
                    log.fine("Action triggered, Instruction [" + instruction + "] , " + source.getInnerHTML());
                }
            }
        }
        sendDataToServer(instruction);
    }

    public void sendDataToServer(final List<PTInstruction> instructions) {
        final JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < instructions.size(); i++) {
            jsonArray.set(0, instructions.get(0));
        }

        sendDataToServer(jsonArray);
    }

    @Override
    public void sendDataToServer(final PTInstruction instruction) {
        final JSONArray jsonArray = new JSONArray();
        jsonArray.set(0, instruction);

        sendDataToServer(jsonArray);
    }

    public void sendDataToServer(final JSONArray jsonArray) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(Model.APPLICATION_VIEW_ID, sessionID);
        requestData.put(Model.APPLICATION_INSTRUCTIONS, jsonArray);

        if (!stackedErrors.isEmpty()) {
            final JSONArray errors = new JSONArray();
            int i = 0;
            for (final JSONObject jsoObject : stackedErrors) {
                errors.set(i++, jsoObject);
            }
            stackedErrors.clear();
            requestData.put(Model.APPLICATION_ERRORS, errors);
        }

        // requestData.put(Model.APPLICATION_SEQ_NUM, nextSent++);

        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Data to send " + requestData.toString());

        requestBuilder.send(requestData.toString());
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
            content.add(new HTML("Server connection failed <br/>Code : " + caught.getStatusCode() + "<br/>" + "Cause : " + caught.getMessage()));

            final Anchor reloadAnchor = new Anchor("reload");
            reloadAnchor.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(final ClickEvent event) {
                    History.newItem("");
                    PonySDK.reload();
                }
            });

            actionPanel.add(reloadAnchor);
            actionPanel.setCellHorizontalAlignment(reloadAnchor, HasHorizontalAlignment.ALIGN_CENTER);
            actionPanel.setCellVerticalAlignment(reloadAnchor, HasVerticalAlignment.ALIGN_MIDDLE);
        } else {
            content.add(new HTML("An unexpected error occured <br/>Code : " + caught.getStatusCode() + "<br/>" + "Cause : " + caught.getMessage()));
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
                communicationErrorMessagePanel.setPopupPosition((Window.getClientWidth() - offsetWidth) / 2, (Window.getClientHeight() - offsetHeight) / 2);
            }
        });
    }

    @Override
    public void onValueChange(final ValueChangeEvent<String> event) {
        if (event.getValue() != null && !event.getValue().isEmpty()) {
            final PTInstruction eventInstruction = new PTInstruction();
            eventInstruction.put(Model.TYPE_HISTORY);
            eventInstruction.put(Model.HISTORY_TOKEN, event.getValue());
            stackInstrution(eventInstruction);
        }
    }

    @Override
    public PTObject getPTObject(final int ID) {
        return objectByID.get(ID);
    }

    @Override
    public PTObject getPTObject(final UIObject uiObject) {
        final Integer objectID = objectIDByWidget.get(uiObject);
        if (objectID != null) return objectByID.get(objectID);
        return null;
    }

    @Override
    public void registerUIObject(final int ID, final UIObject uiObject) {
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
        if (numberOfrequestInProgress > 0) {
            numberOfrequestInProgress--;
        }

        hideLoadingMessageBox();
    }

    private void hideLoadingMessageBox() {

        if (loadingMessageBox == null) return;

        if (numberOfrequestInProgress < 1 && timer != null) {
            timer.cancel();
            timer = null;
            loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        }
    }

    public void executeInstruction(final JavaScriptObject jso) {
        update(new JSONObject(jso));
    }

    public static EventBus getRootEventBus() {
        return rootEventBus;
    }

    public void registerCommunicationError(final CommunicationErrorHandler communicationErrorClosure) {
        this.communicationErrorHandler = communicationErrorClosure;
    }

    public void registerJavascriptAddOnFactory(final String signature, final JavascriptAddOnFactory javascriptAddOnFactory) {
        this.javascriptAddOnFactories.put(signature, javascriptAddOnFactory);
    }

    @Override
    public Map<String, JavascriptAddOnFactory> getJavascriptAddOnFactory() {
        return javascriptAddOnFactories;
    }

    @Override
    public void execute(final double timestamp) {
        if (instructions.isEmpty()) return;

        for (final PTInstruction instruction : instructions) {
            try {
                processInstruction(instruction);
            } catch (final Exception e) {
                log.log(Level.SEVERE, "Exception while executing the instruction " + instruction, e);
                throw e;
            }
        }
        instructions.clear();
    }

}
