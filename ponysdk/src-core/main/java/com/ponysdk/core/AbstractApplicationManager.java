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

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.stm.TxnContext;
import com.ponysdk.ui.terminal.exception.ServerException;
import com.ponysdk.ui.terminal.model.Model;

public abstract class AbstractApplicationManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationManager.class);

    private final ApplicationManagerOption options;

    public AbstractApplicationManager() {
        this(new ApplicationManagerOption());
    }

    public AbstractApplicationManager(final ApplicationManagerOption options) {
        this.options = options;
        log.info(options.toString());
    }

    public void process(final TxnContext context) throws Exception {
        if (context.getApplication() != null) {
            fireInstructions(context.getJsonObject(), context);
        } else {
            final Integer reloadedViewID = null;
            boolean isNewHttpSession = false;
            Application application = context.getApplication();
            if (application == null) {
                application = new Application(context, options);
                context.setApplication(application);

                isNewHttpSession = true;
                log.info("Creating a new application, {}", application.toString());
            } else {
                // if (data.containsKey(Model.APPLICATION_VIEW_ID.toStringValue())) {
                // final JsonNumber jsonNumber =
                // data.getJsonNumber(Model.APPLICATION_VIEW_ID.toStringValue());
                // reloadedViewID = jsonNumber.longValue();
                // }
                // log.info("Reloading application {} {}", reloadedViewID, context);
            }

            startApplication(context, application, isNewHttpSession, reloadedViewID);
        }
    }

    public void startApplication(final TxnContext context, final Application application,
            final boolean isNewHttpSession, final Integer reloadedViewID) throws Exception {
        synchronized (context) {
            final UIContext uiContext = new UIContext(application, context);
            UIContext.setCurrent(uiContext);

            if (reloadedViewID != null) {
                final UIContext previousUIContext = application.getUIContext(reloadedViewID);
                if (previousUIContext != null)
                    previousUIContext.destroy();
            }

            try {
                final Txn txn = Txn.get();
                txn.begin(context);
                try {

                    final int receivedSeqNum = context.getSeqNum();
                    uiContext.updateIncomingSeqNum(receivedSeqNum);// ??

                    final EntryPoint entryPoint = initializeUIContext(uiContext);

                    final String historyToken = context.getHistoryToken();

                    if (historyToken != null && !historyToken.isEmpty()) {
                        uiContext.getHistory().newItem(historyToken, false);
                    }

                    if (isNewHttpSession) {
                        entryPoint.start(uiContext);
                    } else {
                        entryPoint.restart(uiContext);
                    }

                    txn.commit();
                } catch (final Throwable e) {
                    log.error("Cannot send instructions to the browser " + context, e);
                    txn.rollback();
                }
            } finally {
                UIContext.remove();
            }
        }
    }

    protected void fireInstructions(final JsonObject jsonObject, final TxnContext context) throws Exception {
        final String applicationInstructions = Model.APPLICATION_INSTRUCTIONS.toStringValue();
        if (jsonObject.containsKey(applicationInstructions)) {
            // final int key = jsonObject.getJsonNumber(Model.APPLICATION_VIEW_ID.toStringValue()).intValue();
            final int key = 1;

            final Application applicationSession = context.getApplication();

            if (applicationSession == null) {
                throw new ServerException(ServerException.INVALID_SESSION,
                        "Invalid session, please reload your application (viewID #" + key + ").");
            }

            final UIContext uiContext = context.getUIContext();

            if (uiContext == null) {
                throw new ServerException(ServerException.INVALID_SESSION,
                        "Invalid session (no UIContext found), please reload your application (viewID #" + key + ").");
            }

            uiContext.execute(() -> process(uiContext, jsonObject.getJsonArray(applicationInstructions)));
        }
    }

    private void printClientErrorMessage(final JsonObject data) {
        try {
            final JsonArray errors = data.getJsonArray(Model.APPLICATION_ERRORS.toStringValue());
            for (int i = 0; i < errors.size(); i++) {
                final JsonObject jsoObject = errors.getJsonObject(i);
                final String message = jsoObject.getString("message");
                final String details = jsoObject.getString("details");
                log.error(
                        "There was an unexpected error on the terminal. Message: " + message + ". Details: " + details);
            }
        } catch (final Throwable e) {
            log.error("Failed to display errors", e);
        }
    }

    private void process(final UIContext uiContext, final JsonArray applicationInstructions) {
        for (int i = 0; i < applicationInstructions.size(); i++) {
            final JsonObject item = applicationInstructions.getJsonObject(i);
            uiContext.fireClientData(item);
        }
    }

    protected abstract EntryPoint initializeUIContext(final UIContext ponySession) throws ServletException;

    public ApplicationManagerOption getOptions() {
        return options;
    }

}
