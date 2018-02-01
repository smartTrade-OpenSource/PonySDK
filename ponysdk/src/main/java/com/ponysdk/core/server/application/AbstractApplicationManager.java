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

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.context.CommunicationSanityChecker;
import com.ponysdk.core.server.context.UIContext;
import com.ponysdk.core.ui.main.EntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public abstract class AbstractApplicationManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationManager.class);

    protected ApplicationConfiguration configuration;

    private CommunicationSanityChecker communicationSanityChecker;

    public void start() {
        long period = configuration.getHeartBeatPeriodTimeUnit().toMillis(configuration.getHeartBeatPeriod());
        communicationSanityChecker = new CommunicationSanityChecker(Duration.ofMillis(period));
        communicationSanityChecker.start();
    }

    public void startContext(UIContext uiContext) {
        communicationSanityChecker.registerUIContext(uiContext);
        uiContext.execute(() -> {

            final EntryPoint entryPoint;
            try {
                uiContext.getWriter().write(ServerToClientModel.CREATE_CONTEXT, uiContext.getID());
                uiContext.getWriter().write(ServerToClientModel.END, null);

                entryPoint = initializeUIContext(uiContext);
                final String historyToken = uiContext.getHistoryToken();

                if (historyToken != null && !historyToken.isEmpty())
                    uiContext.getHistory().newItem(historyToken, false);

                entryPoint.start(uiContext);
            } catch (Exception e) {
                log.error("Cannot start UIContext", e);
                //TODO nciaravola destroy if exception ?
            }
        });
    }

    protected abstract EntryPoint initializeUIContext(final UIContext uiContext) throws Exception;

    public void setConfiguration(ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    public ApplicationConfiguration getConfiguration() {
        return configuration;
    }

}
