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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.PBroadcastEventHandler;
import com.ponysdk.core.event.PEvent;
import com.ponysdk.core.event.PEvent.Type;
import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.event.PEventHandler;
import com.ponysdk.core.event.PHandlerRegistration;
import com.ponysdk.core.event.PStreamHandler;
import com.ponysdk.core.instruction.Add;
import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Close;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.security.Permission;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PTimer;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.exception.PonySessionException;

public class PonySession {

    private static final Logger log = LoggerFactory.getLogger(PonySession.class);

    private static ThreadLocal<PonySession> currentSession = new ThreadLocal<PonySession>();

    private long objectCounter = 1;

    private long streamRequestCounter = 0;

    private final PWeakHashMap weakReferences = new PWeakHashMap();

    private final Map<Long, PTimer> timers = new ConcurrentHashMap<Long, PTimer>();

    // to do a weak reference ?
    private final Map<Long, PStreamHandler> streamListenerByID = new HashMap<Long, PStreamHandler>();

    private final List<Instruction> pendingInstructions = new ArrayList<Instruction>();

    private Map<String, Permission> permissions = new HashMap<String, Permission>();

    private EntryPoint entryPoint;

    private PHistory history;

    private PEventBus rootEventBus;

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

    public void fireInstruction(final JSONObject instruction) throws PonySessionException, JSONException {
        if (instruction.has(TYPE.KEY)) {
            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.CLOSE)) {
                PonySession.getCurrent().invalidate();
                return;
            }

            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.HISTORY)) {
                if (history != null) {
                    history.fireHistoryChanged(instruction.getString(HISTORY.TOKEN));
                }
                return;
            }

        }

        final PObject object = weakReferences.get(instruction.getLong(PROPERTY.OBJECT_ID));

        if (object == null) {
            log.warn("unknown reference from the browser. Unable to execute instruction: " + instruction);
            try {
                if (instruction.has(PROPERTY.PARENT_ID)) {
                    final PObject parentOfGarbageObject = weakReferences.get(instruction.getLong(PROPERTY.PARENT_ID));
                    log.warn("parent: " + parentOfGarbageObject);
                }
            } catch (final Exception e) {}

            return;
        }
        if (instruction.has(TYPE.KEY)) {
            if (instruction.get(TYPE.KEY).equals(TYPE.KEY_.EVENT)) {
                object.onEventInstruction(instruction);
            }
        }
    }

    public boolean flushInstructions(final JSONObject data) throws JSONException {
        if (pendingInstructions.isEmpty()) return false;
        data.put(APPLICATION.INSTRUCTIONS, pendingInstructions);
        pendingInstructions.clear();
        return true;
    }

    public long nextID() {
        return objectCounter++;
    }

    public long nextStreamRequestID() {
        return streamRequestCounter++;
    }

    public void registerObject(final PObject object) {
        weakReferences.put(object.getID(), object);
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

    public PStreamHandler removeStreamListener(final Long streamID) {
        return streamListenerByID.remove(streamID);
    }

    public void stackStreamRequest(final PStreamHandler streamListener) {
        final AddHandler addHandler = new AddHandler(0, HANDLER.KEY_.STREAM_REQUEST_HANDLER);
        final long streamRequestID = PonySession.getCurrent().nextStreamRequestID();
        addHandler.put(PROPERTY.STREAM_REQUEST_ID, streamRequestID);
        stackInstruction(addHandler);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    public void stackEmbededStreamRequest(final PStreamHandler streamListener, final long objectID) {
        final AddHandler addHandler = new AddHandler(objectID, HANDLER.KEY_.EMBEDED_STREAM_REQUEST_HANDLER);
        final long streamRequestID = PonySession.getCurrent().nextStreamRequestID();
        addHandler.put(PROPERTY.STREAM_REQUEST_ID, streamRequestID);
        stackInstruction(addHandler);
        streamListenerByID.put(streamRequestID, streamListener);
    }

    public PHistory getHistory() {
        return history;
    }

    private PEventBus getEventBus() {
        return rootEventBus;
    }

    public PCookies getCookies() {
        return cookies;
    }

    public void setHistory(final PHistory history) {
        this.history = history;
    }

    public void setRootEventBus(final PEventBus eventBus) {
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

    public static <H extends PEventHandler> PHandlerRegistration addHandler(final Type<H> type, final H handler) {
        return getCurrent().getEventBus().addHandler(type, handler);
    }

    public static <H extends PEventHandler> PHandlerRegistration addHandlerToSource(final Type<H> type, final Object source, final H handler) {
        return getCurrent().getEventBus().addHandlerToSource(type, source, handler);
    }

    public static void fireEvent(final PEvent<?> event) {
        getCurrent().getEventBus().fireEvent(event);
    }

    public static void fireEventFromSource(final PEvent<?> event, final Object source) {
        getCurrent().getEventBus().fireEventFromSource(event, source);
    }

    public static void addHandler(final PBroadcastEventHandler handler) {
        getCurrent().getEventBus().addHandler(handler);
    }

    public static PEventBus getRootEventBus() {
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
