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
 */package com.ponysdk.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.BroadcastEventHandler;
import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.Event.Type;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.core.security.Permission;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PRootLayoutPanel;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.exception.PonySessionException;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Close;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Instruction;

public class PonySession {

    private static final Logger log = LoggerFactory.getLogger(PonySession.class);

    private static ThreadLocal<PonySession> currentSession = new ThreadLocal<PonySession>();

    private long objectCounter = 1;
    private long streamRequestCounter = 0;

    private final PWeakHashMap objectByID = new PWeakHashMap();

    // to do a weak reference ?
    private final Map<Long, StreamHandler> streamListenerByID = new HashMap<Long, StreamHandler>();
    private final List<Instruction> pendingInstructions = new ArrayList<Instruction>();
    private final PRootLayoutPanel rootPanel = new PRootLayoutPanel();

    private Set<Permission> permissions = new HashSet<Permission>();

    private EntryPoint entryPoint;
    private PHistory history;
    private PlaceController placeController;
    private EventBus rootEventBus;
    private PCookies cookies;

    private final PonyApplicationSession applicationSession;

    public PonySession(PonyApplicationSession applicationSession) {
        this.applicationSession = applicationSession;
        objectByID.put(0l, rootPanel);
    }

    public void stackInstruction(Instruction instruction) {
        if (instruction instanceof Add) {
            final Add add = (Add) instruction;
            objectByID.assignParentID(add.getObjectID(), add.getParentID());
        }
        pendingInstructions.add(instruction);
    }

    public void fireInstructions(List<Instruction> instructions) throws PonySessionException {
        for (final Instruction instruction : instructions) {
            fireInstruction(instruction);
        }
    }

    private void fireInstruction(Instruction instruction) throws PonySessionException {
        if (instruction instanceof Close) {
            PonySession.getCurrent().invalidate();
            return;
        }

        if (instruction instanceof EventInstruction) {
            final HandlerType handlerType = ((EventInstruction) instruction).getHandlerType();

            if (HandlerType.HISTORY.equals(handlerType)) {
                if (history != null) {
                    history.fireHistoryChanged(instruction.getMainProperty().getValue());
                }
                return;
            }

        }

        final PObject object = objectByID.get(instruction.getObjectID());

        if (object == null) {
            log.warn("unknown reference from the browser #" + instruction.getObjectID());
            return;
        }

        if (instruction instanceof EventInstruction) {
            object.onEventInstruction((EventInstruction) instruction);
        }
    }

    public List<Instruction> flushInstructions() {
        final List<Instruction> instructions = new ArrayList<Instruction>(pendingInstructions);
        pendingInstructions.clear();
        return instructions;
    }

    public long nextID() {
        return objectCounter++;
    }

    public long nextStreamRequestID() {
        return streamRequestCounter++;
    }

    public void registerObject(long objectID, PObject object) {
        objectByID.put(objectID, object);
    }

    public HttpSession getHttpSession() {
        return applicationSession.getHttpSession();
    }

    public PRootLayoutPanel getRootLayoutPanel() {
        return rootPanel;
    }

    public StreamHandler removeStreamListener(Long streamID) {
        return streamListenerByID.remove(streamID);
    }

    public void stackStreamRequest(StreamHandler streamListener) {
        final AddHandler addHandler = new AddHandler(0, HandlerType.STREAM_REQUEST_HANDLER);
        final long streamRequestID = PonySession.getCurrent().nextStreamRequestID();
        addHandler.setMainPropertyValue(PropertyKey.STREAM_REQUEST_ID, streamRequestID);
        stackInstruction(addHandler);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    public void stackEmbededStreamRequest(StreamHandler streamListener, long objectID) {
        final AddHandler addHandler = new AddHandler(objectID, HandlerType.EMBEDED_STREAM_REQUEST_HANDLER);
        final long streamRequestID = PonySession.getCurrent().nextStreamRequestID();
        addHandler.setMainPropertyValue(PropertyKey.STREAM_REQUEST_ID, streamRequestID);
        stackInstruction(addHandler);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    public PHistory getHistory() {
        return history;
    }

    public PlaceController getPlaceController() {
        return placeController;
    }

    private EventBus getEventBus() {
        return rootEventBus;
    }

    public PCookies getCookies() {
        return cookies;
    }

    void setHistory(PHistory history) {
        this.history = history;
    }

    void setPlaceController(PlaceController placeController) {
        this.placeController = placeController;
    }

    void setRootEventBus(EventBus eventBus) {
        this.rootEventBus = eventBus;
    }

    void setCookies(PCookies cookies) {
        this.cookies = cookies;
    }

    public static PonySession getCurrent() {
        return currentSession.get();
    }

    public static void setCurrent(PonySession ponySession) {
        currentSession.set(ponySession);
    }

    public static <H extends EventHandler> HandlerRegistration addHandler(Type<H> type, H handler) {
        return getCurrent().getEventBus().addHandler(type, handler);
    }

    public static <H extends EventHandler> HandlerRegistration addHandlerToSource(Type<H> type, Object source, H handler) {
        return getCurrent().getEventBus().addHandlerToSource(type, source, handler);
    }

    public static void fireEvent(Event<?> event) {
        getCurrent().getEventBus().fireEvent(event);
    }

    public static void fireEventFromSource(Event<?> event, Object source) {
        getCurrent().getEventBus().fireEventFromSource(event, source);
    }

    public static void addHandler(BroadcastEventHandler handler) {
        getCurrent().getEventBus().addHandler(handler);
    }

    public static EventBus getRootEventBus() {
        return getCurrent().getEventBus();
    }

    void invalidate() {
        getHttpSession().invalidate();
    }

    public void close() {
        final Close close = new Close();
        stackInstruction(close);
    }

    public EntryPoint getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public void setAttribute(String name, Object value) {
        this.applicationSession.setAttribute(name, value);
    }

    public void addSessionListener(HttpSessionListener sessionListener) {
        this.applicationSession.addSessionListener(sessionListener);
    }

    public boolean removeSessionListener(HttpSessionListener sessionListener) {
        return this.applicationSession.removeSessionListener(sessionListener);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, Class<T> clazz) {
        return (T) this.applicationSession.getAttribute(name);
    }

}