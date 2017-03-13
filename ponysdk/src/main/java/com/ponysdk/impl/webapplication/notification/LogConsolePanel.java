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

import com.ponysdk.core.ui.basic.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class LogConsolePanel extends PScrollPanel {

    protected static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    protected PVerticalPanel content = Element.newPVerticalPanel();

    protected PVerticalPanel logsPanel = Element.newPVerticalPanel();

    protected PHorizontalPanel actionPanel = Element.newPHorizontalPanel();

    public LogConsolePanel(final String caption) {
        setSizeFull();

        content.setSizeFull();
        setWidget(content);

        initActionPanel();
        logsPanel.setSizeFull();
        content.add(actionPanel);
        content.add(logsPanel);
    }

    private void initActionPanel() {
        actionPanel = Element.newPHorizontalPanel();
        final PAnchor clearLogs = Element.newPAnchor("Clear logs");
        clearLogs.addClickHandler(event -> logsPanel.clear());
        actionPanel.add(clearLogs);
    }

}