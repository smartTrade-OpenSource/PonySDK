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

package com.ponysdk.impl.webapplication.page;

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimpleLayoutPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;

public class DefaultPageView implements PageView {

    protected PSimpleLayoutPanel panel;
    protected PFlowPanel header;
    protected PSimplePanel body;
    protected PLabel title;
    private int headerHeight = 40;
    private String pageTitle;

    public DefaultPageView() {
        this(null);
    }

    public DefaultPageView(final String pageTitle) {
        this.pageTitle = pageTitle;
    }

    @Override
    public PWidget asWidget() {
        if (panel == null) buildUI();
        return panel;
    }

    private void buildUI() {
        panel = new PSimpleLayoutPanel();
        buildHeader();
        buildBody();
        buildLayout();
    }

    protected void buildLayout() {
        final PDockLayoutPanel dockLayoutPanel = new PDockLayoutPanel(PUnit.PX);
        dockLayoutPanel.addNorth(header, headerHeight);
        dockLayoutPanel.add(body);
        panel.setWidget(dockLayoutPanel);
    }

    protected void buildHeader() {
        header = new PFlowPanel();
        header.addStyleName("pony-Page-Header");
        buildTitle();
    }

    protected void buildTitle() {
        title = new PLabel(pageTitle);
        title.setText(pageTitle);
        title.addStyleName("pony-Page-Header-Caption");
        header.add(title);
    }

    protected void buildBody() {
        body = new PSimpleLayoutPanel();
        body.addStyleName("pony-Page-Body");
    }

    @Override
    public void setWidget(final IsPWidget w) {
        if (panel == null) buildUI();
        body.setWidget(w);
    }

    @Override
    public void setPageTitle(final String caption) {
        pageTitle = caption;
        if (title != null) title.setText(pageTitle);
    }

    public void setHeaderHeight(final int headerHeight) {
        this.headerHeight = headerHeight;
    }

    @Override
    public PSimplePanel getBody() {
        return body;
    }

}
