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

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.terminal.PUnit;

public class DefaultPageView extends PSimpleLayoutPanel implements PageView {

    private final PHorizontalPanel header = new PHorizontalPanel();

    private final PSimplePanel body;

    private final PLabel title = new PLabel();

    public DefaultPageView(final String pageTitle) {
        setSizeFull();
        addStyleName(PonySDKTheme.PAGE);
        header.addStyleName(PonySDKTheme.PAGE_HEADER);

        body = buildBody();

        header.add(title);

        title.setText(pageTitle);
        title.addStyleName(PonySDKTheme.PAGE_HEADER_CAPTION);

        final PDockLayoutPanel dockLayoutPanel = new PDockLayoutPanel(PUnit.PX);
        dockLayoutPanel.setSizeFull();
        dockLayoutPanel.addNorth(header, 40);
        dockLayoutPanel.add(body);

        setWidget(dockLayoutPanel);
    }

    public DefaultPageView() {
        this("");
    }

    @Override
    public PSimplePanel getBody() {
        return body;
    }

    @Override
    public void setPageTitle(final String caption) {
        title.setText(caption);
    }

    protected PSimplePanel buildBody() {
        final PSimplePanel panel = new PSimpleLayoutPanel();
        panel.addStyleName(PonySDKTheme.PAGE_BODY);
        return panel;
    }

}
