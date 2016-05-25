/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.impl.webapplication.notification;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class LogConsolePanel extends PScrollPanel {

    protected static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    protected PVerticalPanel content = new PVerticalPanel();

    protected PVerticalPanel logsPanel = new PVerticalPanel();

    protected PHorizontalPanel actionPanel = new PHorizontalPanel();

    public LogConsolePanel(final String caption) {
        addStyleName(PonySDKTheme.LOG_CONSOLE);
        setSizeFull();

        content.setSizeFull();
        setWidget(content);

        initActionPanel();
        logsPanel.setSizeFull();
        logsPanel.addStyleName(PonySDKTheme.LOG_CONSOLE_LOGS);
        content.add(actionPanel);
        content.add(logsPanel);
    }

    private void initActionPanel() {
        actionPanel = new PHorizontalPanel();
        actionPanel.addStyleName(PonySDKTheme.LOG_CONSOLE_ACTIONS);

        final PAnchor clearLogs = new PAnchor("Clear logs");

        clearLogs.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                logsPanel.clear();
            }
        });
        actionPanel.add(clearLogs);
    }

}