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

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PSimpleLayoutPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.PWidget;

public class DefaultApplicationView implements ApplicationView {

    protected PDockLayoutPanel panel;

    protected PSimplePanel header;
    protected PSimpleLayoutPanel menu;
    protected PSimplePanel body;
    protected PSimplePanel footer;
    protected PSimplePanel logs;
    protected PSplitLayoutPanel center;

    private PUnit unit = PUnit.PX;
    private int headerHeight = 30;
    private int footerHeight = 20;
    private int logsHeight = 130;
    private int menuWidth = 190;

    @Override
    public PWidget asWidget() {
        if (panel == null) {
            buildUI();
        }
        return panel;
    }

    private void buildUI() {
        panel = Element.newPDockLayoutPanel(unit);

        buildHeader();
        buildFooter();
        buildCenter();

        panel.add(center);
    }

    protected void buildFooter() {
        footer = Element.newPSimpleLayoutPanel();
        panel.addSouth(footer, footerHeight);
    }

    protected void buildHeader() {
        header = Element.newPSimpleLayoutPanel();
        panel.addNorth(header, headerHeight);
    }

    protected void buildCenter() {
        center = Element.newPSplitLayoutPanel();
        buildLogs();
        buildMenu();
        buildBody();
    }

    protected void buildBody() {
        body = Element.newPSimpleLayoutPanel();
        center.add(body);
    }

    protected void buildMenu() {
        menu = Element.newPSimpleLayoutPanel();
        center.addWest(menu, menuWidth);
    }

    protected void buildLogs() {
        logs = Element.newPSimpleLayoutPanel();
        center.addSouth(logs, logsHeight);
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

    public void setUnit(final PUnit unit) {
        this.unit = unit;
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

    public void setMenuWidth(final int menuWidth) {
        this.menuWidth = menuWidth;
    }

}
