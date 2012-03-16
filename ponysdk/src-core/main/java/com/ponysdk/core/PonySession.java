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

package com.ponysdk.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import com.ponysdk.ui.server.basic.PTimer;
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

    private final PWeakHashMap weakReferences = new PWeakHashMap();

    private final Map<Long, PTimer> timers = new ConcurrentHashMap<Long, PTimer>();

    // to do a weak reference ?
    private final Map<Long, StreamHandler> streamListenerByID = new HashMap<Long, StreamHandler>();

    private final List<Instruction> pendingInstructions = new ArrayList<Instruction>();

    private Map<String, Permission> permissions = new HashMap<String, Permission>();

    private EntryPoint entryPoint;

    private PHistory history;

    private PlaceController placeController;

    private EventBus rootEventBus;

    private PCookies cookies;

    private final PonyApplicationSession applicationSession;

    private final Map<String, Object> ponySessionAttributes = new ConcurrentHashMap<String, Object>();

    public PonySession(final PonyApplicationSession applicationSession) {
        this.applicationSession = applicationSession;
    }

    public void stackInstruction(final Instruction instruction) {
        if (instruction instanceof Add) {
            final Add add = (Add) instruction;
            weakReferences.assignParentID(add.getObjectID(), add.getParentID());
        }
        pendingInstructions.add(instruction);
    }

    public void fireInstructions(final List<Instruction> instructions) throws PonySessionException {
        for (final Instruction instruction : instructions) {
            fireInstruction(instruction);
        }
    }

    private void fireInstruction(final Instruction instruction) throws PonySessionException {
        if (instruction instanceof Close) {
            PonySession.getCurrent().invalidate();
            return;
        }

        if (instruction instanceof EventInstruction) {
            final HandlerType handlerType = ((EventInstruction) instruction).getType();

            if (HandlerType.HISTORY.equals(handlerType)) {
                if (history != null) {
                    history.fireHistoryChanged(instruction.getMainProperty().getValue());
                }
                return;
            }

        }

        final PObject object = weakReferences.get(instruction.getObjectID());

        if (object == null) {
            log.warn("unknown reference from the browser. Unable to execute instruction: " + instruction);
            if (instruction.getParentID() != null) {
                final PObject parentOfGarbageObject = weakReferences.get(instruction.getParentID());
                log.warn("parent: " + parentOfGarbageObject);
            }
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

    public void registerObject(final PObject object) {
        if (object instanceof PTimer) {
            // avoid GC for Timer but memory leak ....
            timers.put(object.getID(), (PTimer) object);
        } else {
            weakReferences.put(object.getID(), object);
        }
    }

    public void unRegisterObject(final PObject object) {
        timers.remove(object.getID());
        weakReferences.remove(object.getID());
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(final long objectID) {
        return (T) weakReferences.get(objectID);
    }

    public HttpSession getHttpSession() {
        return applicationSession.getHttpSession();
    }

    public StreamHandler removeStreamListener(final Long streamID) {
        return streamListenerByID.remove(streamID);
    }

    public void stackStreamRequest(final StreamHandler streamListener) {
        final AddHandler addHandler = new AddHandler(0, HandlerType.STREAM_REQUEST_HANDLER);
        final long streamRequestID = PonySession.getCurrent().nextStreamRequestID();
        addHandler.setMainPropertyValue(PropertyKey.STREAM_REQUEST_ID, streamRequestID);
        stackInstruction(addHandler);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    public void stackEmbededStreamRequest(final StreamHandler streamListener, final long objectID) {
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

    public void setHistory(final PHistory history) {
        this.history = history;
    }

    public void setPlaceController(final PlaceController placeController) {
        this.placeController = placeController;
    }

    public void setRootEventBus(final EventBus eventBus) {
        this.rootEventBus = eventBus;
    }

    public void setCookies(final PCookies cookies) {
        this.cookies = cookies;
    }

    public static PonySession getCurrent() {
        return currentSession.get();
    }

    public static void setCurrent(final PonySession ponySession) {
        currentSession.set(ponySession);
    }

    public static <H extends EventHandler> HandlerRegistration addHandler(final Type<H> type, final H handler) {
        return getCurrent().getEventBus().addHandler(type, handler);
    }

    public static <H extends EventHandler> HandlerRegistration addHandlerToSource(final Type<H> type, final Object source, final H handler) {
        return getCurrent().getEventBus().addHandlerToSource(type, source, handler);
    }

    public static void fireEvent(final Event<?> event) {
        getCurrent().getEventBus().fireEvent(event);
    }

    public static void fireEventFromSource(final Event<?> event, final Object source) {
        getCurrent().getEventBus().fireEventFromSource(event, source);
    }

    public static void addHandler(final BroadcastEventHandler handler) {
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

    public void setEntryPoint(final EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    public boolean hasPermission(final String key) {
        return permissions.containsKey(key);
    }

    public void setPermissions(final Map<String, Permission> permissions) {
        this.permissions = permissions;
    }

    public void addSessionListener(final HttpSessionListener sessionListener) {
        this.applicationSession.addSessionListener(sessionListener);
    }

    public boolean removeSessionListener(final HttpSessionListener sessionListener) {
        return this.applicationSession.removeSessionListener(sessionListener);
    }

    public void setAttribute(final String name, final Object value) {
        ponySessionAttributes.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) ponySessionAttributes.get(name);
    }

    public void setApplicationAttribute(final String name, final Object value) {
        this.applicationSession.setAttribute(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getApplicationAttribute(final String name) {
        return (T) this.applicationSession.getAttribute(name);
    }

}
