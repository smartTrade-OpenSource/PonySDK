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

package com.ponysdk.ui.terminal;

import java.util.ArrayList;
import java.util.Date;
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
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Frame;
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
import com.ponysdk.ui.terminal.exception.PonySessionException;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Close;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.GC;
import com.ponysdk.ui.terminal.instruction.Instruction;
import com.ponysdk.ui.terminal.instruction.Remove;
import com.ponysdk.ui.terminal.instruction.RemoveHandler;
import com.ponysdk.ui.terminal.instruction.Update;
import com.ponysdk.ui.terminal.ui.PTObject;

public class UIBuilder implements ValueChangeHandler<String>, UIService {

    private final static Logger log = Logger.getLogger(UIBuilder.class.getName());

    private final PonyEngineServiceAsync ponyService = GWT.create(PonyEngineService.class);

    private final UIFactory uiFactory = new UIFactory();
    private final Map<String, AddonFactory> addonByKey = new HashMap<String, AddonFactory>();
    private final Map<Long, PTObject> objectByID = new HashMap<Long, PTObject>();
    private final Map<UIObject, Long> objectIDByWidget = new HashMap<UIObject, Long>();
    private final Map<Long, UIObject> widgetIDByObjectID = new HashMap<Long, UIObject>();
    private final List<Instruction> stackedInstructions = new ArrayList<Instruction>();

    private SimplePanel loadingMessageBox;
    private PopupPanel communicationErrorMessagePanel;
    private Timer timer;
    private int numberOfrequestInProgress;
    private Frame frame;
    private boolean updateMode;
    private boolean pendingClose;

    public static long sessionID;

    public UIBuilder(final long ID) {
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

        RootPanel.get().add(loadingMessageBox);

        loadingMessageBox.setStyleName("pony-LoadingMessageBox");
        loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        loadingMessageBox.getElement().setInnerText("Loading ...");

        /* Frame for stream resource handling */
        frame = new Frame();

        frame.setWidth("0px");
        frame.setHeight("0px");
        frame.getElement().getStyle().setProperty("visibility", "hidden");
        frame.getElement().getStyle().setProperty("position", "fixed");

        // hide loading component
        final Widget w = RootPanel.get("loading");
        if (w == null) {
            Window.alert("Include splash screen html element into your index.html with id=\"loading\"");
        } else {
            w.setSize("0px", "0px");
            w.setVisible(false);
        }

        RootPanel.get().add(frame);
    }

    public void update(final List<Instruction> instructions) {
        updateMode = true;
        try {

            log.info("UPDATING UI with " + instructions.size() + " instructions");

            for (final Instruction instruction : instructions) {
                if (instruction instanceof Close) {
                    pendingClose = true;
                    triggerEvent(instruction);

                    final ScheduledCommand command = new ScheduledCommand() {

                        @Override
                        public void execute() {
                            reload();
                        }
                    };

                    Scheduler.get().scheduleDeferred(command);
                    return;
                }
                if (instruction instanceof Create) {
                    final Create create = (Create) instruction;

                    if (WidgetType.COOKIE.equals(create.getWidgetType())) {
                        final String name = create.getMainProperty().getStringPropertyValue(PropertyKey.NAME);
                        final String value = create.getMainProperty().getStringPropertyValue(PropertyKey.VALUE);
                        final Property expires = create.getMainProperty().getChildProperty(PropertyKey.COOKIE_EXPIRE);
                        if (expires != null) {
                            final Long time = expires.getLongValue();
                            final Date date = new Date(time);
                            Cookies.setCookie(name, value, date);
                        } else {
                            Cookies.setCookie(name, value);
                        }
                    } else {

                        log.info("Create: " + create.getObjectID() + ", " + create.getWidgetType().name());
                        PTObject ptObject;
                        if (create.getAddOnSignature() != null) {
                            final AddonFactory addonFactory = addonByKey.get(create.getAddOnSignature());
                            ptObject = addonFactory.newAddon();
                            if (ptObject == null) {
                                Window.alert("UIBuilder: AddOn factory not found, type : " + create.getWidgetType());
                            }
                            ptObject.create(create, this);
                        } else {
                            ptObject = uiFactory.newUIObject(this, create);
                            ptObject.create(create, this);
                        }

                        objectByID.put(create.getObjectID(), ptObject);
                    }

                } else if (instruction instanceof Add) {

                    final Add add = (Add) instruction;
                    log.info("Add: " + add.getObjectID() + ", " + add.getParentID() + ", " + add.getMainProperty());

                    final PTObject uiObject = objectByID.get(add.getParentID());
                    uiObject.add(add, this);

                } else if (instruction instanceof AddHandler) {

                    final AddHandler addHandler = (AddHandler) instruction;
                    log.info("AddHandler: " + addHandler.getType() + ", " + addHandler.getObjectID() + ", " + addHandler.getMainProperty());

                    if (HandlerType.STREAM_REQUEST_HANDLER.equals(addHandler.getType())) {
                        frame.setUrl(GWT.getModuleBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&" + PropertyKey.STREAM_REQUEST_ID.name() + "=" + addHandler.getMainProperty().getValue());
                    } else {
                        final PTObject uiObject = objectByID.get(addHandler.getObjectID());
                        uiObject.addHandler(addHandler, this);
                    }

                } else if (instruction instanceof RemoveHandler) {

                    final RemoveHandler addHandler = (RemoveHandler) instruction;
                    log.info("AddHandler: " + addHandler.getType() + ", " + addHandler.getObjectID() + ", " + addHandler.getMainProperty());

                    final PTObject uiObject = objectByID.get(addHandler.getObjectID());
                    uiObject.removeHandler(addHandler, this);

                } else if (instruction instanceof Remove) {
                    final Remove remove = (Remove) instruction;
                    PTObject ptObject;

                    if (PropertyKey.COOKIE.equals(instruction.getMainProperty().getKey())) { // TODO
                                                                                             // nciaravola
                                                                                             // merge with
                                                                                             // PTCookie ?
                        Cookies.removeCookie(instruction.getMainProperty().getValue());
                    } else {
                        if (remove.getParentID() == -1) ptObject = objectByID.get(remove.getObjectID());
                        else {
                            ptObject = objectByID.get(remove.getParentID());
                        }
                        ptObject.remove(remove, this);
                    }
                } else if (instruction instanceof GC) {
                    final GC remove = (GC) instruction;
                    log.info("GC: " + remove.getObjectID());

                    final PTObject ptObject = objectByID.remove(remove.getObjectID());
                    final UIObject uiObject = widgetIDByObjectID.remove(remove.getObjectID());
                    if (uiObject != null) {
                        objectIDByWidget.remove(uiObject);
                    }

                    ptObject.gc(remove, this);
                } else if (instruction instanceof Update) {

                    final Update update = (Update) instruction;
                    // log.info("Update " + update.getMainProperty().getKey() + " / " +
                    // update.getMainProperty().getValue());

                    final PTObject ptObject = objectByID.get(update.getObjectID());
                    ptObject.update(update, this);

                } else if (instruction instanceof com.ponysdk.ui.terminal.instruction.History) {
                    final com.ponysdk.ui.terminal.instruction.History history = (com.ponysdk.ui.terminal.instruction.History) instruction;
                    final String oldToken = History.getToken();
                    if (oldToken != null && oldToken.equals(history.getToken())) {
                        History.fireCurrentHistoryState();
                    } else {
                        History.newItem(history.getToken(), true);
                    }
                }
            }
        } catch (final Throwable e) {
            Window.alert("PonySDK has encountered an internal error : " + e.getMessage());
            log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
        } finally {
            flushEvent();
            updateMode = false;
        }
    }

    private void updateTimer(final Timer object, final Property mainProperty) {
        object.scheduleRepeating(mainProperty.getIntValue());
    }

    public void stackEvent(final Instruction instruction) {
        if (!updateMode) triggerEvent(instruction);
        else stackedInstructions.add(instruction);
    }

    public void flushEvent() {
        if (stackedInstructions.isEmpty()) return;
        fireEvents(stackedInstructions);
    }

    private void fireEvents(final List<Instruction> instructions) {
        numberOfrequestInProgress++;
        if (timer == null) {
            timer = scheduleLoadingMessageBox();
        }
        ponyService.fireInstructions(sessionID, instructions, new AsyncCallback<List<Instruction>>() {

            @Override
            public void onFailure(final Throwable caught) {
                if (pendingClose) return;
                log.log(Level.SEVERE, "fireInstruction failed", caught);

                numberOfrequestInProgress--;
                hideLoadingMessageBox();
                instructions.clear();

                if (caught instanceof PonySessionException) {
                    reload();
                    return;
                }
                showCommunicationErrorMessage(caught);
            }

            @Override
            public void onSuccess(final List<Instruction> result) {
                numberOfrequestInProgress--;
                hideLoadingMessageBox();
                instructions.clear();
                update(result);
            }
        });
    }

    @Override
    public void triggerEvent(final Instruction instruction) {
        final List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(instruction);
        fireEvents(instructions);
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

    private void showCommunicationErrorMessage(final Throwable caught) {
        final VerticalPanel content = new VerticalPanel();
        if (caught instanceof StatusCodeException) {
            final StatusCodeException exception = (StatusCodeException) caught;
            content.add(new HTML("Server connection failed <br/>Code : " + exception.getStatusCode() + "<br/>" + "cause : " + exception.getMessage()));
        } else if (caught instanceof InvocationException) {
            content.add(new HTML("Exception durring server invocation : " + caught.getMessage()));
        } else {
            content.add(new HTML("Failure : " + caught == null ? "" : caught.getMessage()));
        }

        final HorizontalPanel actionPanel = new HorizontalPanel();
        actionPanel.setSize("100%", "100%");

        final Anchor reloadAnchor = new Anchor("reload");
        reloadAnchor.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                History.newItem("");
                reload();
            }
        });

        final Anchor closeAnchor = new Anchor("close");
        closeAnchor.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                communicationErrorMessagePanel.hide();
            }
        });

        actionPanel.add(reloadAnchor);
        actionPanel.add(closeAnchor);

        actionPanel.setCellHorizontalAlignment(reloadAnchor, HasHorizontalAlignment.ALIGN_CENTER);
        actionPanel.setCellHorizontalAlignment(closeAnchor, HasHorizontalAlignment.ALIGN_CENTER);
        actionPanel.setCellVerticalAlignment(reloadAnchor, HasVerticalAlignment.ALIGN_MIDDLE);
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

    protected void hideLoadingMessageBox() {
        if (numberOfrequestInProgress < 1) {
            timer.cancel();
            timer = null;
            loadingMessageBox.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        }
    }

    @Override
    public void onValueChange(final ValueChangeEvent<String> event) {
        if (event.getValue() != null && !event.getValue().isEmpty()) {
            final EventInstruction eventInstruction = new EventInstruction(-1, HandlerType.HISTORY);
            eventInstruction.setMainPropertyValue(PropertyKey.VALUE, event.getValue());
            stackEvent(eventInstruction);
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

    private native void reload() /*-{$wnd.location.reload();}-*/;
}
