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

import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PDragEnterEvent;
import com.ponysdk.ui.server.basic.event.PDragEnterHandler;
import com.ponysdk.ui.server.basic.event.PDragLeaveEvent;
import com.ponysdk.ui.server.basic.event.PDragLeaveHandler;
import com.ponysdk.ui.server.basic.event.PDragStartEvent;
import com.ponysdk.ui.server.basic.event.PDragStartHandler;
import com.ponysdk.ui.server.basic.event.PDropEvent;
import com.ponysdk.ui.server.basic.event.PDropHandler;

public class DragAndDropPageActivity extends SamplePageActivity {

    private final PFlowPanel boxContainer = new PFlowPanel();

    public DragAndDropPageActivity() {
        super("Drag and Drop", "Extra");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel verticalPanel = new PVerticalPanel();

        final PWidget box1 = buildBox("Box 1");
        final PWidget box2 = buildBox("Box 2");
        final PWidget box3 = buildBox("Box 3");
        final PWidget box4 = buildBox("Box 4");
        boxContainer.add(box1);
        boxContainer.add(box2);
        boxContainer.add(box3);
        boxContainer.add(box4);

        verticalPanel.add(boxContainer);

        examplePanel.setWidget(verticalPanel);
    }

    private PWidget buildBox(final String label) {
        final PLabel lbl = new PLabel(label);
        lbl.addStyleName("label");

        final PFlowPanel box = new PFlowPanel();
        box.addStyleName("ddbox");
        box.add(lbl);

        box.addDomHandler(new PDragStartHandler() {

            @Override
            public void onDragStart(final PDragStartEvent event) {
            }
        }, PDragStartEvent.TYPE);

        box.addDomHandler(new PDropHandler() {

            @Override
            public void onDrop(final PDropEvent event) {
                box.removeStyleName("dragenter");
                final PWidget source = event.getDragSource();
                if (source != null && source != box) {
                    final int dropIndex = boxContainer.getWidgetIndex(box);
                    boxContainer.remove(source);
                    boxContainer.insert(source, dropIndex);
                }
            }
        }, PDropEvent.TYPE);

        box.addDomHandler(new PDragEnterHandler() {

            @Override
            public void onDragEnter(final PDragEnterEvent event) {
                box.addStyleName("dragenter");
            }
        }, PDragEnterEvent.TYPE);

        box.addDomHandler(new PDragLeaveHandler() {

            @Override
            public void onDragLeave(final PDragLeaveEvent event) {
                box.removeStyleName("dragenter");
            }
        }, PDragLeaveEvent.TYPE);

        return box;
    }
}
