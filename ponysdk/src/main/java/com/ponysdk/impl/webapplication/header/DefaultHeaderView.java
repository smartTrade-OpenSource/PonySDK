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

package com.ponysdk.impl.webapplication.header;


import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.alignment.PHorizontalAlignment;
import com.ponysdk.core.ui.basic.alignment.PVerticalAlignment;

public class DefaultHeaderView extends PSimplePanel implements HeaderView {

    private final PHorizontalPanel actionPanel = new PHorizontalPanel();

    public DefaultHeaderView(final String title) {
        setSizeFull();

        final PLabel logo = new PLabel(title);

        PHorizontalPanel gridLayout = new PHorizontalPanel();
        gridLayout.add(logo);
        gridLayout.setCellHorizontalAlignment(logo, PHorizontalAlignment.ALIGN_LEFT);
        gridLayout.setCellVerticalAlignment(logo, PVerticalAlignment.ALIGN_MIDDLE);

        gridLayout.add(actionPanel);
        gridLayout.setCellHorizontalAlignment(actionPanel, PHorizontalAlignment.ALIGN_RIGHT);
        gridLayout.setCellVerticalAlignment(actionPanel, PVerticalAlignment.ALIGN_MIDDLE);

        setWidget(gridLayout);
    }

    @Override
    public void addActionWidget(final PWidget widget) {
        actionPanel.add(widget);
        actionPanel.setCellVerticalAlignment(widget, PVerticalAlignment.ALIGN_MIDDLE);
        actionPanel.setCellHorizontalAlignment(widget, PHorizontalAlignment.ALIGN_CENTER);
    }
}
