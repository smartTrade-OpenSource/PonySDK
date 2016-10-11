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

package com.ponysdk.core.server.application;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

public abstract class AbstractApplicationManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationManager.class);

    private final ApplicationManagerOption options;

    protected AbstractApplicationManager(final ApplicationManagerOption options) {
        this.options = options;
        log.info(options.toString());
    }

    private static void process(final UIContext uiContext, final JsonArray applicationInstructions) {
        for (int i = 0; i < applicationInstructions.size(); i++) {
            final JsonObject item = applicationInstructions.getJsonObject(i);
            uiContext.fireClientData(item);
        }
    }

    public void startApplication(final TxnContext txnContext) throws Exception {
        final UIContext uiContext = txnContext.getUIContext();
        uiContext.begin();
        try {
            final Txn txn = Txn.get();
            txn.begin(txnContext);
            try {

                final int receivedSeqNum = txnContext.getSeqNum();
                uiContext.updateIncomingSeqNum(receivedSeqNum);// ??

                PWindow.initialize();

                final EntryPoint entryPoint = initializeUIContext(uiContext);

                final String historyToken = txnContext.getHistoryToken();

                if (historyToken != null && !historyToken.isEmpty()) {
                    uiContext.getHistory().newItem(historyToken, false);
                }

                entryPoint.start(uiContext);

                txn.commit();
            } catch (final Throwable e) {
                log.error("Cannot send instructions to the browser " + txnContext, e);
                txn.rollback();
            }
        } finally {
            uiContext.end();
        }
    }

    public void fireInstructions(final JsonObject jsonObject, final TxnContext context) throws Exception {
        final int key = 1;

        final Application applicationSession = context.getApplication();
        if (applicationSession == null) throw new Exception("Invalid session, please reload your application (viewID #" + key + ").");

        final UIContext uiContext = context.getUIContext();
        if (uiContext == null)
            throw new Exception("Invalid session (no UIContext found), please reload your application (viewID #" + key + ").");

        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
        uiContext.execute(() -> process(uiContext, jsonObject.getJsonArray(applicationInstructions)));
    }

    protected abstract EntryPoint initializeUIContext(final UIContext ponySession) throws ServletException;

    public ApplicationManagerOption getOptions() {
        return options;
    }

}
