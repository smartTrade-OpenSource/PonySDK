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

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.ui.server.model.ServerBinaryModel;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * This class allows to execute native Java-script code.
 */
public abstract class PScript extends PObject {

    private static final String SCRIPT_KEY = PScript.class.getCanonicalName();

    private static final Logger log = LoggerFactory.getLogger(PScript.class);

    private long executionID = 0;
    private final Map<Long, ExecutionCallback> callbacksByID = new HashMap<>();

    private PScript() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCRIPT;
    }

    public static PScript get() {
        final UIContext session = UIContext.get();
        PScript script = session.getAttribute(SCRIPT_KEY);
        if (script == null) {
            script = new PScript() {
            };
            session.setAttribute(SCRIPT_KEY, script);
        }
        return script;
    }

    public static void execute(final String js) {
        get().executeScript(js);
    }

    public static void execute(final String js, final ExecutionCallback callback) {
        get().executeScript(js, callback);
    }

    public static void executeDeffered(final String js, final int delay, final TimeUnit unit) {
        get().executeScriptDeffered(js, null, delay, unit);
    }

    public static void executeDeffered(final String js, final ExecutionCallback callback, final int delay,
            final TimeUnit unit) {
        get().executeScriptDeffered(js, callback, delay, unit);
    }

    private void executeScript(final String js) {
        saveUpdate(new ServerBinaryModel(ServerToClientModel.EVAL, js));
    }

    private void executeScript(final String js, final ExecutionCallback callback) {
        final long id = executionID++;
        callbacksByID.put(id, callback);

        saveUpdate(new ServerBinaryModel(ServerToClientModel.EVAL, js),
                new ServerBinaryModel(ServerToClientModel.COMMAND_ID, executionID++));
    }

    private void executeScriptDeffered(final String js, final ExecutionCallback callback,
            final int delay, final TimeUnit unit) {
        final PTerminalScheduledCommand command = new PTerminalScheduledCommand() {

            @Override
            protected void run() {
                try {
                    if (callback == null) executeScript(js);
                    else executeScript(js, callback);
                } catch (final Exception e) {
                    log.error("Exception while execution the script " + js, e);
                }
            }
        };
        command.schedule(unit.toMillis(delay));
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
            final ExecutionCallback callback = callbacksByID
                    .remove(instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue());
            if (callback != null) callback.onFailure(instruction.getString(ClientToServerModel.ERROR_MSG.toStringValue()));
        } else if (instruction.containsKey(ClientToServerModel.RESULT.toStringValue())) {
            final ExecutionCallback callback = callbacksByID
                    .remove(instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue());
            if (callback != null) callback.onSuccess(instruction.getString(ClientToServerModel.RESULT.toStringValue()));
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
