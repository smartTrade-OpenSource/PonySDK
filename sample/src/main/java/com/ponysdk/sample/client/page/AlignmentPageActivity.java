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

import org.springframework.beans.factory.annotation.Autowired;

import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.sample.client.page.hello.HelloPageActivity;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class AlignmentPageActivity extends PageActivity {

    @Autowired
    private HelloPageActivity helloPageActivity;

    public AlignmentPageActivity() {
        super("Vertical Panel", "Category");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onShowPage(Place place) {
        final PVerticalPanel verticalPanel = new PVerticalPanel();

        verticalPanel.setHorizontalAlignment(PHorizontalAlignment.ALIGN_CENTER);
        verticalPanel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        verticalPanel.setBorderWidth(10);
        verticalPanel.setWidth("300px");
        verticalPanel.setHeight("100%");
        final PLabel leftLabel = new PLabel("Left");
        verticalPanel.add(leftLabel);

        final PLabel centerLabel = new PLabel("Center");
        verticalPanel.add(centerLabel);

        final PLabel rightLabel = new PLabel("Right");
        verticalPanel.add(rightLabel);

        final PLabel leftTopLabel = new PLabel("LeftTop");
        verticalPanel.add(leftTopLabel);
        verticalPanel.setCellHorizontalAlignment(leftTopLabel, PHorizontalAlignment.ALIGN_LEFT);
        verticalPanel.setCellVerticalAlignment(leftTopLabel, PVerticalAlignment.ALIGN_TOP);

        final PLabel centerTopLabel = new PLabel("CenterTop");
        verticalPanel.add(centerTopLabel);
        verticalPanel.setCellHorizontalAlignment(centerTopLabel, PHorizontalAlignment.ALIGN_CENTER);
        verticalPanel.setCellVerticalAlignment(centerTopLabel, PVerticalAlignment.ALIGN_TOP);

        final PLabel rightTopLabel = new PLabel("RightTop");
        verticalPanel.add(rightTopLabel);
        verticalPanel.setCellHorizontalAlignment(rightTopLabel, PHorizontalAlignment.ALIGN_RIGHT);
        verticalPanel.setCellVerticalAlignment(rightTopLabel, PVerticalAlignment.ALIGN_TOP);

        final PLabel leftMiddleLabel = new PLabel("LeftMiddle");
        verticalPanel.add(leftMiddleLabel);
        verticalPanel.setCellHorizontalAlignment(leftMiddleLabel, PHorizontalAlignment.ALIGN_LEFT);
        verticalPanel.setCellVerticalAlignment(leftMiddleLabel, PVerticalAlignment.ALIGN_MIDDLE);

        final PLabel centerMiddleLabel = new PLabel("CenterMiddle");
        verticalPanel.add(centerMiddleLabel);
        verticalPanel.setCellHorizontalAlignment(centerMiddleLabel, PHorizontalAlignment.ALIGN_CENTER);
        verticalPanel.setCellVerticalAlignment(centerMiddleLabel, PVerticalAlignment.ALIGN_MIDDLE);

        final PLabel rightMiddleLabel = new PLabel("RightMiddle");
        verticalPanel.add(rightMiddleLabel);
        verticalPanel.setCellHorizontalAlignment(rightMiddleLabel, PHorizontalAlignment.ALIGN_RIGHT);
        verticalPanel.setCellVerticalAlignment(rightMiddleLabel, PVerticalAlignment.ALIGN_MIDDLE);

        final PLabel leftBottomLabel = new PLabel("LeftBottom");
        verticalPanel.add(leftBottomLabel);
        verticalPanel.setCellHorizontalAlignment(leftBottomLabel, PHorizontalAlignment.ALIGN_LEFT);
        verticalPanel.setCellVerticalAlignment(leftBottomLabel, PVerticalAlignment.ALIGN_BOTTOM);

        final PLabel centerBottomLabel = new PLabel("CenterBottom");
        verticalPanel.add(centerBottomLabel);
        verticalPanel.setCellHorizontalAlignment(centerBottomLabel, PHorizontalAlignment.ALIGN_CENTER);
        verticalPanel.setCellVerticalAlignment(centerBottomLabel, PVerticalAlignment.ALIGN_BOTTOM);

        final PLabel rightBottomLabel = new PLabel("RightBottom");
        verticalPanel.add(rightBottomLabel);
        verticalPanel.setCellHorizontalAlignment(rightBottomLabel, PHorizontalAlignment.ALIGN_RIGHT);
        verticalPanel.setCellVerticalAlignment(rightBottomLabel, PVerticalAlignment.ALIGN_BOTTOM);
        verticalPanel.setCellHeight(rightBottomLabel, "200px");
        verticalPanel.setCellWidth(rightBottomLabel, "200px");

        final PAnchor anchor = new PAnchor("ref crois� vers Hello");
        anchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                goToPage(helloPageActivity.newPopupPlace("test"));
            }
        });

        verticalPanel.add(anchor);

        pageView.getBody().setWidget(verticalPanel);
    }

    @Override
    protected void onFirstShowPage() {}
}
