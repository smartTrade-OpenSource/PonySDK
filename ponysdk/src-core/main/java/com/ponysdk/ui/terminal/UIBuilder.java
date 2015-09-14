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
import com.google.gwt.core.client.GWT;
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
import com.ponysdk.ui.terminal.extension.AddonList;
import com.ponysdk.ui.terminal.extension.PonyAddonList;
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
    private final Map<Long, PTObject> objectByID = new HashMap<>();
    private final Map<UIObject, Long> objectIDByWidget = new HashMap<>();
    private final Map<Long, UIObject> widgetIDByObjectID = new HashMap<>();
    private final List<PTInstruction> stackedInstructions = new ArrayList<>();
    private final List<JSONObject> stackedErrors = new ArrayList<>();

    private final Map<Long, JSONObject> incomingMessageQueue = new HashMap<>();

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

    private final List<JSONObject> instructions = new ArrayList<>();

    public UIBuilder() {
        History.addValueChangeHandler(this);

        final AddonList addonList = GWT.create(PonyAddonList.class);

        final List<AddonFactory> addonFactoryList = addonList.getAddonFactoryList();

        for (final AddonFactory addonFactory : addonFactoryList) {
            addonByKey.put(addonFactory.getSignature(), addonFactory);
        }

        rootEventBus.addHandler(HttpResponseReceivedEvent.TYPE, this);
        rootEventBus.addHandler(HttpRequestSendEvent.TYPE, this);

    }

    public void init(final int ID, final RequestBuilder requestBuilder) {
        log.info("Init request builder");

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
        objectByID.put(0l, cookies);

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
        instructions.add(data);

        AnimationScheduler.get().requestAnimationFrame(this);

        //
        // final JSONArray jsonArray = data.get(Model.APPLICATION_INSTRUCTIONS.getKey()).isArray();
        // for (int i = 0; i < jsonArray.size(); i++) {
        // final PTInstruction instruction = new
        // PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject());
        // processInstruction(instruction);
        // }

        /**
         * final long receivedSeqNum = (long)
         * data.get(Model.APPLICATION_SEQ_NUM.getKey()).isNumber().doubleValue(); if ((lastReceived + 1) !=
         * receivedSeqNum) { incomingMessageQueue.put(receivedSeqNum, data); log.log(Level.INFO,
         * "Wrong seqnum received. Expecting #" + (lastReceived + 1) + " but received #" + receivedSeqNum);
         * return; } lastReceived = receivedSeqNum; final List<PTInstruction> instructions = new ArrayList
         * <>(); final JSONArray jsonArray = data.get(Model.APPLICATION_INSTRUCTIONS.getKey()).isArray(); for
         * (int i = 0; i < jsonArray.size(); i++) { instructions.add( } if (!incomingMessageQueue.isEmpty()) {
         * long expected = receivedSeqNum + 1; while (incomingMessageQueue.containsKey(expected)) { final
         * JSONObject jsonObject = incomingMessageQueue.remove(expected); final JSONArray jsonArray2 =
         * jsonObject.get(Model.APPLICATION_INSTRUCTIONS.getKey()).isArray(); for (int i = 0; i <
         * jsonArray2.size(); i++) { instructions.add(new
         * PTInstruction(jsonArray2.get(i).isObject().getJavaScriptObject())); } lastReceived = expected;
         * expected++; } log.log(Level.INFO, "Message synchronized from #" + receivedSeqNum + " to #" +
         * lastReceived); } updateMode = true; PTInstruction currentInstruction = null; try { for (final
         * PTInstruction instruction : instructions) { currentInstruction = instruction; try {
         * processInstruction(instruction); } catch (final Throwable e) { log.log(Level.SEVERE,
         * "PonySDK has encountered an internal error on instruction : " + currentInstruction +
         * " => Error Message " + e.getMessage() + ". ReceivedSeqNum: " + receivedSeqNum +
         * " LastProcessSeqNum: " + lastReceived, e); stackError(currentInstruction, e); } } } catch (final
         * Throwable e) { log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e); } finally
         * { flushEvents(); updateMode = false; }
         **/

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
        PTObject ptObject;

        if (instruction.containsKey(Model.TYPE_CLOSE)) {
            pendingClose = true;
            sendDataToServer(instruction);

            final ScheduledCommand command = new ScheduledCommand() {

                @Override
                public void execute() {
                    PonySDK.reload();
                }
            };

            Scheduler.get().scheduleDeferred(command);
        } else if (instruction.containsKey(Model.TYPE_CREATE))

        {
            final boolean isAddon = instruction.containsKey("addOnSignature");
            if (isAddon) {
                final String addOnSignature = instruction.getString("addOnSignature");
                final AddonFactory addonFactory = addonByKey.get(addOnSignature);
                if (addonFactory == null) { throw new RuntimeException("UIBuilder: AddOn factory not found for signature: " + addOnSignature + ", available: " + addonByKey.keySet()); }

                ptObject = addonFactory.newAddon();
                if (ptObject == null) { throw new RuntimeException("UIBuilder: Failed to instanciate an Addon of type: " + addOnSignature); }
                ptObject.create(instruction, this);
            } else {
                ptObject = uiFactory.newUIObject(this, instruction);
                ptObject.create(instruction, this);
            }

            objectByID.put(instruction.getObjectID(), ptObject);

        } else if (instruction.containsKey(Model.TYPE_ADD))

        {
            ptObject = objectByID.get(instruction.getParentID());
            if (ptObject == null) {
                log.info("Cannot add object to an garbaged parent object #" + instruction.getObjectID());
                return;
            }
            ptObject.add(instruction, this);

        } else if (instruction.containsKey(Model.TYPE_ADD_HANDLER))

        {
            if (instruction.containsKey(Model.HANDLER_STREAM_REQUEST_HANDLER)) {
                new PTStreamResource().addHandler(instruction, this);
            } else {
                ptObject = objectByID.get(instruction.getObjectID());
                ptObject.addHandler(instruction, this);
            }
        } else if (instruction.containsKey(Model.TYPE_REMOVE_HANDLER))

        {
            ptObject = objectByID.get(instruction.getObjectID());
            ptObject.removeHandler(instruction, this);
        } else if (instruction.containsKey(Model.TYPE_REMOVE))

        {
            if (instruction.getParentID() == -1) ptObject = objectByID.get(instruction.getObjectID());
            else {
                ptObject = objectByID.get(instruction.getParentID());
            }
            if (ptObject == null) {
                log.info("Cannot remove an garbaged object #" + instruction.getObjectID());
                return;
            }
            ptObject.remove(instruction, this);
        } else if (instruction.containsKey(Model.TYPE_GC))

        {
            ptObject = unRegisterObject(instruction.getObjectID());
            if (ptObject == null) {
                log.info("Cannot GC an garbaged object #" + instruction.getObjectID());
                return;
            }
            ptObject.gc(this);
        } else if (instruction.containsKey(Model.TYPE_UPDATE))

        {
            ptObject = objectByID.get(instruction.getObjectID());
            if (ptObject == null) {
                log.info("Cannot update an garbaged object #" + instruction.getObjectID());
                return;
            }

            ptObject.update(instruction, this);
        } else if (instruction.containsKey(Model.TYPE_HISTORY))

        {
            final String oldToken = History.getToken();

            String token = null;

            if (instruction.containsKey(Model.HISTORY_TOKEN)) {
                token = instruction.getString(Model.HISTORY_TOKEN);
            }

            if (oldToken != null && oldToken.equals(token)) {
                if (instruction.getBoolean(Model.HISTORY_FIRE_EVENTS)) {
                    History.fireCurrentHistoryState();
                }
            } else {
                History.newItem(token, instruction.getBoolean(Model.HISTORY_FIRE_EVENTS));
            }
        }

    }

    protected void updateIncomingSeqNum(final long receivedSeqNum) {
        final long previous = lastReceived;
        if ((previous + 1) != receivedSeqNum) {
            log.log(Level.SEVERE, "Wrong seqnum received. Expecting #" + (previous + 1) + " but received #" + receivedSeqNum);
        }
        lastReceived = receivedSeqNum;
    }

    @Override
    public PTObject unRegisterObject(final Long objectId) {
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
        if (widget == null) {
            sendDataToServer(instruction);
        } else {
            sendDataToServer(widget.getElement(), instruction);
        }
    }

    public void sendDataToServer(final List<PTInstruction> instructions) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(Model.APPLICATION_VIEW_ID, sessionID);

        final JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < instructions.size(); i++) {
            jsonArray.set(0, instructions.get(0));
        }

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

        log.info("Data to send" + requestData.toString());
        log.info("Request Builder" + requestBuilder);

        requestBuilder.send(requestData.toString());
    }

    @Override
    public void sendDataToServer(final PTInstruction instruction) {
        final PTInstruction requestData = new PTInstruction();
        requestData.put(Model.APPLICATION_VIEW_ID, sessionID);

        final JSONArray jsonArray = new JSONArray();
        jsonArray.set(0, instruction);
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

        log.info("Data to send" + requestData.toString());
        log.info("Request Builder" + requestBuilder);

        requestBuilder.send(requestData.toString());
    }

    @Override
    public void sendDataToServer(final Element source, final PTInstruction instruction) {
        if (source != null) {
            log.info("Action triggered, Instruction [" + instruction + "] , " + source.getInnerHTML());
        }

        sendDataToServer(instruction);
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
    public PTObject getPTObject(final Long ID) {
        return objectByID.get(ID);
    }

    @Override
    public PTObject getPTObject(final UIObject uiObject) {
        final Long objectID = objectIDByWidget.get(uiObject);
        if (objectID != null) return objectByID.get(objectID);
        return null;
    }

    @Override
    public void registerUIObject(final Long ID, final UIObject uiObject) {
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

        for (final JSONObject json : instructions) {
            final JSONArray jsonArray = json.get(Model.APPLICATION_INSTRUCTIONS.getKey()).isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                final PTInstruction instruction = new PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject());
                processInstruction(instruction);
            }
        }
        instructions.clear();
    }

}
