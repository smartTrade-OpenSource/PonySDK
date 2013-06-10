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

    private static final String SCRIPT_KEY = PScript.class.getCanonicalName();

    private long executionID = 0;
    private final Map<Long, ExecutionCallback> callbacksByID = new HashMap<Long, PScript.ExecutionCallback>();

    private PScript() {}

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCRIPT;
    }

    public static PScript get() {
        PScript script = UIContext.get().getAttribute(SCRIPT_KEY);
        if (script == null) {
            script = new PScript() {};
            UIContext.get().setAttribute(SCRIPT_KEY, script);
        }
        return script;
    }

    public void execute(final String js) {
        final Update update = new Update(ID);
        update.put(PROPERTY.EVAL, js);
        update.put(PROPERTY.ID, (executionID++));
        Txn.get().getTxnContext().save(update);
    }

    public void execute(final String js, final ExecutionCallback callback) {
        final long id = executionID++;
        callbacksByID.put(id, callback);

        final Update update = new Update(ID);
        update.put(PROPERTY.EVAL, js);
        update.put(PROPERTY.ID, id);
        update.put(PROPERTY.CALLBACK, true);
        Txn.get().getTxnContext().save(update);
    }

    public void executeDeffered(final String js, final int delay, final TimeUnit unit) {
        final PTerminalScheduledCommand command = new PTerminalScheduledCommand() {

            @Override
            protected void run() {
                PScript.get().execute(js);
            }
        };
        command.schedule(unit.toMillis(delay));
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

}
