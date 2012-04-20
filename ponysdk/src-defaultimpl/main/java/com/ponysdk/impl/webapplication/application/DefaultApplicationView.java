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

import com.google.gwt.dom.client.Style.Unit;
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PSplitLayoutPanel;

public class DefaultApplicationView extends PDockLayoutPanel implements ApplicationView {

    private final PSimplePanel header = new PSimpleLayoutPanel();

    private final PSimpleLayoutPanel menu = new PSimpleLayoutPanel();

    private final PSimplePanel body = new PSimpleLayoutPanel();

    private final PSimplePanel footer = new PSimpleLayoutPanel();

    private final PSimplePanel logs = new PSimpleLayoutPanel();

    private final PSplitLayoutPanel center;

    private int headerHeight = 30;

    private int footerHeight = 20;

    private int logsHeight = 130;

    private final int menuWidth = 190;

    public DefaultApplicationView() {
        super(Unit.PX);
        setSizeFull();

        addNorth(header, headerHeight);
        addSouth(footer, footerHeight);

        center = new PSplitLayoutPanel(Unit.PX);
        center.setSizeFull();
        center.addSouth(logs, logsHeight);
        center.addWest(menu, menuWidth);
        center.add(body);

        add(center);
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

    public void setHeaderHeight(final int height) {
        this.headerHeight = height;
    }

    public void setFooterHeight(final int height) {
        this.footerHeight = height;
    }

    public void setLogsHeight(final int height) {
        this.logsHeight = height;
    }

}
