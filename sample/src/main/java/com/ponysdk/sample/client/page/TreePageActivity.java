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

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.service.query.Query;
import com.ponysdk.sample.client.event.DemoBusinessEvent;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PImage;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTree;
import com.ponysdk.core.ui.basic.PTreeItem;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;

public class TreePageActivity extends SamplePageActivity {

    public TreePageActivity() {
        super("Tree", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();

        panel.add(new PLabel("Static Tree:"));

        final PTree tree = new PTree();
        tree.setAnimationEnabled(false);
        tree.setWidth("300px");

        tree.addSelectionHandler(new PSelectionHandler<PTreeItem>() {

            @Override
            public void onSelection(final PSelectionEvent<PTreeItem> event) {
                final String msg = "Selected item : name = " + event.getSelectedItem();
                UIContext.getRootEventBus().fireEvent(new DemoBusinessEvent(msg));
            }
        });

        final PTreeItem firstItem = new PTreeItem("First item");

        final PAnchor anchor = new PAnchor("Second item");
        final PTreeItem secondItem = new PTreeItem(anchor);
        anchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                secondItem.setState(!secondItem.getState());
            }
        });

        final PTreeItem thirdItem = new PTreeItem(new PImage("images/pony.png"));

        tree.addItem(firstItem);
        tree.addItem(secondItem);
        tree.addItem(thirdItem);

        final Query query = new Query();
        // final FindPonysCommand command = new FindPonysCommand(query);
        // final Result<List<Pony>> ponys = command.execute();
        //
        // for (final Pony pony : ponys.getData()) {
        // firstItem.addItem(pony.getName());
        // secondItem.addItem(pony.getName());
        // thirdItem.addItem(pony.getName());
        // }

        panel.add(tree);

        examplePanel.setWidget(panel);
    }
}
