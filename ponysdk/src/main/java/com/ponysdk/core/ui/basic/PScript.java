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

package com.ponysdk.core.ui.basic;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PWindowManager.RegisterWindowListener;

/**
 * This class allows to execute native Java-script code.
 */
public class PScript extends PObject {

    private static final String SCRIPT_KEY = PScript.class.getCanonicalName();
    private final Map<Long, ExecutionCallback> callbacksByID = new HashMap<>();
    private long executionID = 0;

    private PScript() {
    }

    private static PScript get(final int windowID) {
        if (windowID != PWindow.EMPTY_WINDOW_ID) {
            final UIContext uiContext = UIContext.get();
            final PScript script = uiContext.getAttribute(SCRIPT_KEY + windowID);
            if (script == null) {
                final PScript newScript = new PScript();
                uiContext.setAttribute(SCRIPT_KEY + windowID, newScript);
                if (PWindowManager.getWindow(windowID) != null) {
                    newScript.attach(windowID);
                } else {
                    PWindowManager.addWindowListener(new RegisterWindowListener() {

                        @Override
                        public void registered(final int registeredWindowID) {
                            if (windowID == registeredWindowID) newScript.attach(windowID);
                        }

                        @Override
                        public void unregistered(final int windowID) {
                        }
                    });
                }
                return newScript;
            }
            return script;
        } else {
            throw new IllegalArgumentException("PScript need to be executed on a window");
        }
    }

    public static void execute(final int windowID, final String js) {
        execute(windowID, js, null, null);
    }

    public static void execute(final PWindow window, final String js) {
        execute(window.getID(), js, null, null);
    }

    public static void execute(final int windowID, final String js, final ExecutionCallback callback) {
        execute(windowID, js, callback, null);
    }

    public static void execute(final PWindow window, final String js, final ExecutionCallback callback) {
        execute(window.getID(), js, callback, null);
    }

    public static void execute(final int windowID, final String js, final Duration period) {
        execute(windowID, js, null, period);
    }

    public static void execute(final PWindow window, final String js, final Duration period) {
        execute(window.getID(), js, null, period);
    }

    public static void execute(final int windowID, final String js, final ExecutionCallback callback, final Duration period) {
        get(windowID).executeScript(js, callback, period);
    }

    public static void execute(final PWindow window, final String js, final ExecutionCallback callback, final Duration period) {
        get(window.getID()).executeScript(js, callback, period);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCRIPT;
    }

    private void executeScript(final String js, final ExecutionCallback callback, final Duration period) {
        saveUpdate((writer) -> {
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

        void onFailure(String msg);

        void onSuccess(String msg);
    }

}
