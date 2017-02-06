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

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterHandler;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragLeaveHandler;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDragStartHandler;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PDropHandler;

public class DragAndDropPageActivity extends SamplePageActivity {

    private final PFlowPanel boxContainer = Element.newPFlowPanel();

    public DragAndDropPageActivity() {
        super("Drag and Drop", "Extra");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel verticalPanel = Element.newPVerticalPanel();

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
        final PLabel lbl = Element.newPLabel(label);
        lbl.addStyleName("label");

        final PFlowPanel box = Element.newPFlowPanel();
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
