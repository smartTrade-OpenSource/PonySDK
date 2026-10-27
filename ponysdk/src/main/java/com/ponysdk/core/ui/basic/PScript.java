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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.JsonObject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Calls a JavaScript function of the terminal's window by name: a function the application's own scripts define
 * ({@link #call(PWindow, String, Object...)}). No JavaScript source travels: the page needs no {@code 'unsafe-eval'}
 * (ADR-0015).
 */
public class PScript extends PObject {

    private static final Logger log = LoggerFactory.getLogger(PScript.class);
    private static final String SCRIPT_KEY = PScript.class.getCanonicalName();

    private Map<Long, ExecutionCallback> callbacksByID;
    private long executionID = 0;
    private String key;

    private PScript() {
    }

    private static PScript get(final PWindow window) {
        if (window != null) {
            final UIContext uiContext = UIContext.get();
            final String key = SCRIPT_KEY + window.getID();
            final PScript script = uiContext.getAttribute(SCRIPT_KEY + window.getID());
            if (script == null) {
                final PScript newScript = new PScript();
                newScript.key = key;
                uiContext.setAttribute(key, newScript);
                newScript.attach(window, null);
                return newScript;
            }
            return script;
        } else {
            throw new IllegalArgumentException("PScript need to be executed on a window");
        }
    }

    @Override
    protected boolean attach(final PWindow window, final PFrame frame) {
        final boolean result = super.attach(window, frame);
        if (result) window.addDestroyListener(event -> onDestroy());
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            UIContext.get().removeAttribute(key);
        } catch (Exception e) {
            log.error("An error occurred while trying to process the destroy event", e);
        }

    }

    /**
     * Calls {@code window[functionName](args...)} in the terminal.
     *
     * @param args strings, numbers, booleans or null
     */
    public static void call(final PWindow window, final String functionName, final Object... args) {
        call(window, functionName, args, null, null);
    }

    /**
     * Calls {@code window[functionName](args...)} in the terminal.
     *
     * @param args     strings, numbers, booleans or null; may be null
     * @param callback receives the returned string or number, "" for anything else, or the error; may be null
     * @param period   the call waits this long in the terminal; may be null
     */
    public static void call(final PWindow window, final String functionName, final Object[] args, final ExecutionCallback callback,
                            final Duration period) {
        get(window).send(functionName, args, callback, period);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCRIPT;
    }

    private void send(final String functionName, final Object[] args, final ExecutionCallback callback, final Duration period) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.FUNCTION_NAME, functionName);
            if (args != null && args.length > 0) writer.write(ServerToClientModel.FUNCTION_ARGS, args);
            if (callback != null) {
                if (callbacksByID == null) callbacksByID = new HashMap<>(4);
                callbacksByID.put(++executionID, callback);
                writer.write(ServerToClientModel.COMMAND_ID, executionID);
            }
            if (period != null) writer.write(ServerToClientModel.FIXDELAY, period.toMillis());
        });
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
            if (callbacksByID != null) {
                final ExecutionCallback callback = callbacksByID
                        .remove(instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue());
                if (callback != null)
                    callback.onFailure(instruction.getString(ClientToServerModel.ERROR_MSG.toStringValue()));
            }
        } else if (instruction.containsKey(ClientToServerModel.RESULT.toStringValue())) {
            if (callbacksByID != null) {
                final ExecutionCallback callback = callbacksByID
                        .remove(instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue());
                if (callback != null)
                    callback.onSuccess(instruction.getString(ClientToServerModel.RESULT.toStringValue()));
            }
        } else {
            super.onClientData(instruction);
        }
    }

    public interface ExecutionCallback {

        void onFailure(String msg);

        void onSuccess(String msg);
    }

}
