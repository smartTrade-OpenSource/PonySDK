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

package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * This class allows to execute native Javascript code.
 */
public abstract class PScript extends PObject {

    private static final String SCRIPT_KEY = PScript.class.getCanonicalName();

    private long executionID = 0;
    private final Map<Long, ExecutionCallback> callbacksByID = new HashMap<>();

    private PScript() {}

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCRIPT;
    }

    public static PScript get() {
        final UIContext session = UIContext.get();
        PScript script = session.getAttribute(SCRIPT_KEY);
        if (script == null) {
            script = new PScript() {};
            session.setAttribute(SCRIPT_KEY, script);
        }
        return script;
    }

    public static void execute(final String js) {
        get().executeScript(js);
    }

    public static void execute(final long windowID, final String js) {
        get().executeScript(windowID, js);
    }

    public static void execute(final String js, final ExecutionCallback callback) {
        get().executeScript(js, callback);
    }

    public static void execute(final long windowID, final String js, final ExecutionCallback callback) {
        get().executeScript(windowID, js, callback);
    }

    public static void executeDeffered(final String js, final ExecutionCallback callback, final int delay, final TimeUnit unit) {
        get().executeScriptDeffered(js, callback, delay, unit);
    }

    public static void executeDeffered(final long windowID, final String js, final ExecutionCallback callback, final int delay, final TimeUnit unit) {
        get().executeScriptDeffered(windowID, js, callback, delay, unit);
    }

    // //

    public void executeScript(final Long windowID, final String js) {
        final Update update = new Update(ID, windowID);
        update.put(PROPERTY.EVAL, js);
        update.put(PROPERTY.ID, (executionID++));
        Txn.get().getTxnContext().save(update);
    }

    public void executeScript(final String js) {
        executeScript(null, js);
    }

    public void executeScript(final Long windowID, final String js, final ExecutionCallback callback) {
        final long id = executionID++;
        callbacksByID.put(id, callback);

        final Update update = new Update(ID, windowID);
        update.put(PROPERTY.EVAL, js);
        update.put(PROPERTY.ID, id);
        update.put(PROPERTY.CALLBACK, true);
        Txn.get().getTxnContext().save(update);
    }

    public void executeScript(final String js, final ExecutionCallback callback) {
        executeScript(null, js, callback);
    }

    public void executeScriptDeffered(final Long windowID, final String js, final ExecutionCallback callback, final int delay, final TimeUnit unit) {
        final PTerminalScheduledCommand command = new PTerminalScheduledCommand() {

            @Override
            protected void run() {
                executeScript(windowID, js, callback);
            }
        };
        command.schedule(unit.toMillis(delay));
    }

    public void executeScriptDeffered(final String js, final ExecutionCallback callback, final int delay, final TimeUnit unit) {
        executeScriptDeffered(null, js, callback, delay, unit);
    }

    public void executeScriptDeffered(final Long windowID, final String js, final int delay, final TimeUnit unit) {
        final PTerminalScheduledCommand command = new PTerminalScheduledCommand() {

            @Override
            protected void run() {
                executeScript(windowID, js);
            }
        };
        command.schedule(unit.toMillis(delay));
    }

    public void executeScriptDeffered(final String js, final int delay, final TimeUnit unit) {
        executeScriptDeffered(null, js, delay, unit);
    }

    @Override
    public void onClientData(final JSONObject instruction) throws JSONException {
        if (instruction.has(PROPERTY.ERROR_MSG)) {
            final ExecutionCallback callback = callbacksByID.remove(instruction.getLong(PROPERTY.ID));
            if (callback != null) callback.onFailure(instruction.getString(PROPERTY.ERROR_MSG));
        } else if (instruction.has(PROPERTY.RESULT)) {
            final ExecutionCallback callback = callbacksByID.remove(instruction.getLong(PROPERTY.ID));
            if (callback != null) callback.onSuccess(instruction.getString(PROPERTY.RESULT));
        } else {
            super.onClientData(instruction);
        }
    }

    public interface ExecutionCallback {

        public void onFailure(String msg);

        public void onSuccess(String msg);
    }

    public static class PScriptExecutionLogger implements ExecutionCallback {

        private static Logger log = LoggerFactory.getLogger(PScriptExecutionLogger.class);

        @Override
        public void onFailure(final String msg) {
            log.error(msg);
        }

        @Override
        public void onSuccess(final String msg) {
            log.info(msg);
        }

    }

}
