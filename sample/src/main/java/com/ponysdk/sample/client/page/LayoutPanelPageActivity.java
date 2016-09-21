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

package com.ponysdk.sample.client.page;

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PLayoutPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.alignment.PHorizontalAlignment;
import com.ponysdk.core.ui.basic.alignment.PVerticalAlignment;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;

import java.time.Duration;

public class LayoutPanelPageActivity extends SamplePageActivity {

    final PLayoutPanel layoutPanel = new PLayoutPanel();

    public LayoutPanelPageActivity() {
        super("Layout Panel", "Panels");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PWidget fill = buildComponent("Fill panel", "#75ffdc");
        final PWidget leftPane = buildComponent("Left panel top/left 50.50 200x100", "#b879fc");
        final PWidget rightPane = buildComponent("Right panel bottom/right 50.50 200x100", "#e8b6ea");

        layoutPanel.add(fill);
        layoutPanel.add(leftPane);
        layoutPanel.add(rightPane);

        layoutPanel.setWidgetLeftRight(fill, 0, 0, PUnit.PX);
        layoutPanel.setWidgetTopBottom(fill, 0, 0, PUnit.PX);

        layoutPanel.setWidgetLeftWidth(leftPane, 50, 200, PUnit.PX);
        layoutPanel.setWidgetTopHeight(leftPane, 50, 100, PUnit.PX);

        layoutPanel.setWidgetRightWidth(rightPane, 50, 200, PUnit.PX);
        layoutPanel.setWidgetBottomHeight(rightPane, 50, 100, PUnit.PX);

        // test animation
        leftPane.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                // permute left/right
                layoutPanel.setWidgetLeftWidth(rightPane, 50, 200, PUnit.PX);
                layoutPanel.setWidgetTopHeight(rightPane, 50, 100, PUnit.PX);
                layoutPanel.setWidgetRightWidth(leftPane, 50, 200, PUnit.PX);
                layoutPanel.setWidgetBottomHeight(leftPane, 50, 100, PUnit.PX);
                layoutPanel.animate(Duration.ofSeconds(2));
            }
        }, PClickEvent.TYPE);

        rightPane.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                // restore
                layoutPanel.setWidgetLeftWidth(leftPane, 50, 200, PUnit.PX);
                layoutPanel.setWidgetTopHeight(leftPane, 50, 100, PUnit.PX);
                layoutPanel.setWidgetRightWidth(rightPane, 50, 200, PUnit.PX);
                layoutPanel.setWidgetBottomHeight(rightPane, 50, 100, PUnit.PX);
                layoutPanel.animate(Duration.ofSeconds(2));
            }
        }, PClickEvent.TYPE);

        examplePanel.setWidget(layoutPanel);
    }

    private PWidget buildComponent(final String name, final String color) {
        final PHorizontalPanel panel = new PHorizontalPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", color);
        final PLabel label = new PLabel(name);
        panel.add(label);
        panel.setCellHorizontalAlignment(label, PHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(label, PVerticalAlignment.ALIGN_MIDDLE);
        return panel;
    }

}
