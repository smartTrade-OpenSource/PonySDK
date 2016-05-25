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

package com.ponysdk.impl.webapplication.application;

import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PSplitLayoutPanel;
import com.ponysdk.ui.terminal.PUnit;

public class DefaultApplicationView2 extends PDockLayoutPanel implements ApplicationView {

    private final PSimpleLayoutPanel header = new PSimpleLayoutPanel();

    private final PSimpleLayoutPanel menu = new PSimpleLayoutPanel();

    private final PSimpleLayoutPanel body = new PSimpleLayoutPanel();

    private final PSimpleLayoutPanel footer = new PSimpleLayoutPanel();

    private final PSimpleLayoutPanel logs = new PSimpleLayoutPanel();

    public DefaultApplicationView2() {
        super(PUnit.PX);
        setSizeFull();

        addNorth(header, 4);
        addSouth(footer, 3);
        addWest(menu, 10);

        final PSplitLayoutPanel splitLayoutPanel = new PSplitLayoutPanel();
        splitLayoutPanel.setSizeFull();
        splitLayoutPanel.addSouth(logs, 70);
        splitLayoutPanel.add(body);
        body.setSizeFull();

        add(splitLayoutPanel);
    }

    @Override
    public PSimplePanel getHeader() {
        return header;
    }

    @Override
    public PSimplePanel getMenu() {
        return menu;
    }

    @Override
    public PSimplePanel getBody() {
        return body;
    }

    @Override
    public PSimplePanel getFooter() {
        return footer;
    }

    @Override
    public PSimplePanel getLogs() {
        return logs;
    }

}
