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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.exception.ServerException;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.ui.PTCookies;
import com.ponysdk.ui.terminal.ui.PTObject;
import com.ponysdk.ui.terminal.ui.PTStreamResource;

public class UIBuilder implements ValueChangeHandler<String>, UIService {

    private final static Logger log = Logger.getLogger(UIBuilder.class.getName());

    private final UIFactory uiFactory = new UIFactory();
    private final Map<String, AddonFactory> addonByKey = new HashMap<String, AddonFactory>();
    private final Map<Long, PTObject> objectByID = new HashMap<Long, PTObject>();
    private final Map<UIObject, Long> objectIDByWidget = new HashMap<UIObject, Long>();
    private final Map<Long, UIObject> widgetIDByObjectID = new HashMap<Long, UIObject>();
    private final List<PTInstruction> stackedInstructions = new ArrayList<PTInstruction>();
    private final List<JSONObject> stackedErrors = new ArrayList<JSONObject>();

    private final Map<Long, JSONObject> incomingMessageQueue = new HashMap<Long, JSONObject>();

    private SimplePanel loadingMessageBox;
    private PopupPanel communicationErrorMessagePanel;
    private Timer timer;
    private int numberOfrequestInProgress;

    private boolean updateMode;
    private boolean pendingClose;

    private final RequestBuilder requestBuilder;

    private long lastReceived = -1;
    private long nextSent = 1;

    public static long sessionID;

    public UIBuilder(final long ID, final RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
        UIBuilder.sessionID = ID;
        History.addValueChangeHandler(this);

        final AddonList addonList = GWT.create(PonyAddonList.class);

        final List<AddonFactory> addonFactoryList = addonList.getAddonFactoryList();

        for (final AddonFactory addonFactory : addonFactoryList) {
            addonByKey.put(addonFactory.getSignature(), addonFactory);
        }
    }

    public void init() {

        loadingMessageBox = new SimplePanel();

        communicationErrorMessagePanel = new PopupPanel(false, true);
        communicationErrorMessagePanel.setGlassEnabled(true);
        communicationErrorMessagePanel.setStyleName("pony-notification");
        communicationErrorMessagePanel.addStyleName("error");

        // RootPanel.get().add(loadingMessageBox);

        loadingMessageBox.setStyleName("pony-LoadingMessageBox");
        loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        loadingMessageBox.getElement().setInnerText("Loading ...");

        final PTCookies cookies = new PTCookies();
        cookies.create(null, null);
        objectByID.put(0l, cookies);

        // hide loading component
        final Widget w = RootPanel.get("loading");
        if (w == null) {
            Window.alert("Include splash screen html element into your index.html with id=\"loading\"");
        } else {
            w.setSize("0px", "0px");
            w.setVisible(false);
        }
    }

    public void onCommunicationError(final Throwable exception) {

        if (loadingMessageBox == null) {
            // log.log(Level.SEVERE, "Error ", exception);
            if (exception instanceof StatusCodeException) {
                final StatusCodeException codeException = (StatusCodeException) exception;
                if (codeException.getStatusCode() == 0) return;
            }
            Window.alert("Cannot inititialize the application : " + exception.getMessage() + "\n" + exception + "\nPlease reload your application");
            return;
        }

        if (pendingClose) return;

        if (exception instanceof StatusCodeException) {
            numberOfrequestInProgress--;
            final StatusCodeException statusCodeException = (StatusCodeException) exception;
            showCommunicationErrorMessage(statusCodeException);
            hideLoadingMessageBox();
        } else {
            Window.alert("An unexcepted error occured: " + exception.getMessage() + ". Please check the server logs.");
        }
    }

    @Override
    public void update(final JSONObject data) {

        long receivedSeqNum = (long) data.get(APPLICATION.SEQ_NUM).isNumber().doubleValue();
        if ((lastReceived + 1) != receivedSeqNum) {
            incomingMessageQueue.put(receivedSeqNum, data);
            return;
        }

        hideLoadingMessageBox();

        final List<PTInstruction> instructions = new ArrayList<PTInstruction>();
        final JSONArray jsonArray = data.get(APPLICATION.INSTRUCTIONS).isArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            instructions.add(new PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject()));
        }

        if (!incomingMessageQueue.isEmpty()) {
            long expected = receivedSeqNum + 1;
            while (incomingMessageQueue.containsKey(expected)) {
                final JSONObject jsonObject = incomingMessageQueue.remove(expected);
                final JSONArray jsonArray2 = jsonObject.get(APPLICATION.INSTRUCTIONS).isArray();
                for (int i = 0; i < jsonArray2.size(); i++) {
                    instructions.add(new PTInstruction(jsonArray2.get(i).isObject().getJavaScriptObject()));
                }
                expected++;
                receivedSeqNum = expected;
            }
        }

        updateMode = true;
        PTInstruction currentInstruction = null;
        try {

            for (final PTInstruction instruction : instructions) {
                currentInstruction = instruction;
                try {
                    processInstruction(instruction);
                } catch (final Throwable e) {
                    log.log(Level.SEVERE, "PonySDK has encountered an internal error on instruction : " + currentInstruction + " => Error Message " + e.getMessage() + ". ReceivedSeqNum: " + receivedSeqNum + " LastProcessSeqNum: "
                            + lastReceived, e);
                    stackError(currentInstruction, e);
                }
            }

            updateIncomingSeqNum(receivedSeqNum);

        } catch (final Throwable e) {
            Window.alert("PonySDK has encountered an internal error on instruction : " + currentInstruction + " => Error Message " + e.getMessage() + ". ReceivedSeqNum: " + receivedSeqNum + " LastProcessSeqNum: " + lastReceived);
            log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
        } finally {
            flushEvents();
            updateMode = false;
        }
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
    public void processInstruction(final PTInstruction instruction) throws Exception {

        final String type = instruction.getString(TYPE.KEY);

        if (TYPE.KEY_.CLOSE.equals(type)) {
            pendingClose = true;
            sendDataToServer(instruction);

            final ScheduledCommand command = new ScheduledCommand() {

                @Override
                public void execute() {
                    CommunicationEntryPoint.reload();
                }
            };

            Scheduler.get().scheduleDeferred(command);
        } else if (TYPE.KEY_.CREATE.equals(type)) {
            PTObject ptObject;
            final boolean isAddon = instruction.containsKey("addOnSignature");
            if (isAddon) {
                final String addOnSignature = instruction.getString("addOnSignature");
                final AddonFactory addonFactory = addonByKey.get(addOnSignature);
                if (addonFactory == null) { throw new Exception("UIBuilder: AddOn factory not found for signature: " + addOnSignature + ", available: " + addonByKey.keySet()); }

                ptObject = addonFactory.newAddon();
                if (ptObject == null) { throw new Exception("UIBuilder: Failed to instanciate an Addon of type: " + addOnSignature); }
                ptObject.create(instruction, this);
            } else {
                ptObject = uiFactory.newUIObject(this, instruction);
                ptObject.create(instruction, this);
            }

            objectByID.put(instruction.getObjectID(), ptObject);

        } else if (TYPE.KEY_.ADD.equals(type)) {
            final PTObject uiObject = objectByID.get(instruction.getParentID());
            if (uiObject == null) {
                log.info("Cannot add object to an garbaged parent object #" + instruction.getObjectID());
                return;
            }
            uiObject.add(instruction, this);

        } else if (TYPE.KEY_.ADD_HANDLER.equals(type)) {
            final String handler = instruction.getString(HANDLER.KEY);
            if (HANDLER.KEY_.STREAM_REQUEST_HANDLER.equals(handler)) {
                new PTStreamResource().addHandler(instruction, this);
            } else {
                final PTObject uiObject = objectByID.get(instruction.getObjectID());
                uiObject.addHandler(instruction, this);
            }
        } else if (TYPE.KEY_.REMOVE_HANDLER.equals(type)) {
            final PTObject uiObject = objectByID.get(instruction.getObjectID());
            uiObject.removeHandler(instruction, this);

        } else if (TYPE.KEY_.REMOVE.equals(type)) {
            PTObject ptObject;
            if (instruction.getParentID() == -1) ptObject = objectByID.get(instruction.getObjectID());
            else {
                ptObject = objectByID.get(instruction.getParentID());
            }
            if (ptObject == null) {
                log.info("Cannot remove an garbaged object #" + instruction.getObjectID());
                return;
            }
            ptObject.remove(instruction, this);
        } else if (TYPE.KEY_.GC.equals(type)) {
            final PTObject unRegisterObject = unRegisterObject(instruction.getObjectID());
            if (unRegisterObject == null) {
                log.info("Cannot GC an garbaged object #" + instruction.getObjectID());
                return;
            }
            unRegisterObject.gc(this);
        } else if (TYPE.KEY_.UPDATE.equals(type)) {
            final PTObject ptObject = objectByID.get(instruction.getObjectID());
            if (ptObject == null) {
                log.info("Cannot update an garbaged object #" + instruction.getObjectID());
                return;
            }
            ptObject.update(instruction, this);
        } else if (TYPE.KEY_.HISTORY.equals(type)) {
            final String oldToken = History.getToken();

            String token = null;

            if (instruction.containsKey(HISTORY.TOKEN)) {
                token = instruction.getString(HISTORY.TOKEN);
            }

            if (oldToken != null && oldToken.equals(token)) {
                if (instruction.getBoolean(HISTORY.FIRE_EVENTS)) History.fireCurrentHistoryState();
            } else {
                History.newItem(token, instruction.getBoolean(HISTORY.FIRE_EVENTS));
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

    private void sendDataToServer(final List<PTInstruction> instructions) {
        numberOfrequestInProgress++;

        if (timer == null) timer = scheduleLoadingMessageBox();

        final PTInstruction requestData = new PTInstruction();
        requestData.put(APPLICATION.VIEW_ID, sessionID);

        final JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < instructions.size(); i++) {
            jsonArray.set(i, instructions.get(i));
        }

        final JSONArray errors = new JSONArray();
        if (!stackedErrors.isEmpty()) {
            int i = 0;
            for (final JSONObject jsoObject : stackedErrors) {
                errors.set(i++, jsoObject);
            }
            stackedErrors.clear();
        }

        requestData.put(APPLICATION.INSTRUCTIONS, jsonArray);
        requestData.put(APPLICATION.ERRORS, errors);
        requestData.put(APPLICATION.SEQ_NUM, nextSent++);

        requestBuilder.send(requestData.toString());
    }

    @Override
    public void sendDataToServer(final PTInstruction instruction) {
        final List<PTInstruction> instructions = new ArrayList<PTInstruction>();
        instructions.add(instruction);
        sendDataToServer(instructions);
    }

    private Timer scheduleLoadingMessageBox() {
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
                    CommunicationEntryPoint.reload();
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

    public void hideLoadingMessageBox() {
        if (numberOfrequestInProgress < 1 && timer != null) {
            timer.cancel();
            timer = null;
            loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        }
    }

    @Override
    public void onValueChange(final ValueChangeEvent<String> event) {
        if (event.getValue() != null && !event.getValue().isEmpty()) {
            final PTInstruction eventInstruction = new PTInstruction();
            eventInstruction.put(TYPE.KEY, TYPE.KEY_.HISTORY);
            eventInstruction.put(HISTORY.TOKEN, event.getValue());
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
}
