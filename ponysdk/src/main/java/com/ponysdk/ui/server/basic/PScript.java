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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.ui.model.ClientToServerModel;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * This class allows to execute native Java-script code.
 */
public class PScript extends PObject {

    private static final String SCRIPT_KEY = PScript.class.getCanonicalName();

    private long executionID = 0;

    private final Map<Long, ExecutionCallback> callbacksByID = new HashMap<>();

    private PScript(final int windowID) {
        attach(windowID);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCRIPT;
    }

    private static PScript get() {
        return get(PWindow.MAIN_WINDOW_ID);
    }

    private static PScript get(final int windowID) {
        final UIContext session = UIContext.get();
        PScript script = session.getAttribute(SCRIPT_KEY + windowID);
        if (script == null) {
            script = new PScript(windowID);
            session.setAttribute(SCRIPT_KEY, script);
        }
        return script;
    }

    public static void execute(final int windowID, final String js) {
        execute(windowID, js, null, null);
    }

    public static void execute(final String js) {
        execute(js, null, null);
    }

    public static void execute(final int windowID, final String js, final ExecutionCallback callback) {
        execute(windowID, js, callback, null);
    }

    public static void execute(final String js, final ExecutionCallback callback) {
        execute(js, callback, null);
    }

    public static void execute(final String js, final Duration period) {
        execute(js, null, period);
    }

    public static void execute(final int windowID, final String js, final Duration period) {
        execute(windowID, js, null, period);
    }

    public static void execute(final String js, final ExecutionCallback callback, final Duration period) {
        get().executeScript(js, callback, period);
    }

    public static void execute(final int windowID, final String js, final ExecutionCallback callback, final Duration period) {
        get(windowID).executeScript(js, callback, period);
    }

    private void executeScript(final String js, final ExecutionCallback callback, final Duration period) {
        writeUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.EVAL, js);
            if (callback != null) {
                callbacksByID.put(++executionID, callback);
                writer.writeModel(ServerToClientModel.COMMAND_ID, executionID);
            }
            if (period != null) {
                writer.writeModel(ServerToClientModel.FIXDELAY, period.toMillis());
            }
        });
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
            final ExecutionCallback callback = callbacksByID
                    .remove(instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue());
            if (callback != null)
                callback.onFailure(instruction.getString(ClientToServerModel.ERROR_MSG.toStringValue()));
        } else if (instruction.containsKey(ClientToServerModel.RESULT.toStringValue())) {
            final ExecutionCallback callback = callbacksByID
                    .remove(instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue());
            if (callback != null)
                callback.onSuccess(instruction.getString(ClientToServerModel.RESULT.toStringValue()));
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
