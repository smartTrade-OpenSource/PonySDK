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

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PCommand;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;
import com.ponysdk.ui.server.basic.PVerticalPanel;

public class MenuPageActivity extends SamplePageActivity {

    public MenuPageActivity() {
        super("MenuBar", "UI Components");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();
        final PVerticalPanel panel = new PVerticalPanel();

        final PMenuBar menuBar1 = createMenuBar(false);
        final PMenuBar menuBar2 = createMenuBar(true);
        final PMenuBar menuBar3 = createMenuBar(true);
        final PMenuBar menuBar4 = createStyledMenuBar();

        panel.add(new PLabel("Horizontal Menu Bar [Default Style]"));
        panel.add(menuBar1);
        panel.add(new PLabel("Vertical Menu Bar [Default Style]"));
        panel.add(menuBar2);
        panel.add(new PLabel("Vertical Menu Bar [Light Style]"));
        menuBar3.addStyleName(PonySDKTheme.MENUBAR_LIGHT);
        panel.add(menuBar3);
        panel.add(new PLabel("Vertical Menu Bar [Toolbar Style]"));
        panel.add(menuBar4);

        examplePanel.setWidget(panel);
    }

    private PMenuBar createStyledMenuBar() {
        final PMenuBar menuBar = new PMenuBar();
        menuBar.setStyleName("pony-ActionToolbar");

        menuBar.addItem("Refresh");

        final PMenuBar exportMenuBar = new PMenuBar(true);
        exportMenuBar.addItem(new PMenuItem("CSV"));
        exportMenuBar.addItem(new PMenuItem("PDF"));
        exportMenuBar.addItem(new PMenuItem("XML"));
        menuBar.addItem("Export", exportMenuBar);

        menuBar.addItem("Reload");

        return menuBar;
    }

    private PMenuBar createMenuBar(final boolean vertical) {
        final PMenuBar menuBar = new PMenuBar();
        final PMenuBar fileBar = new PMenuBar(vertical);

        menuBar.addItem("File", fileBar);

        final PMenuItem newItem = new PMenuItem("New");
        newItem.setCommand(new PCommand() {

            @Override
            public void execute() {
                PNotificationManager.notify("Menu Selection", newItem.getText());
            }
        });
        final PMenuItem openItem = new PMenuItem("Open");
        openItem.setCommand(new PCommand() {

            @Override
            public void execute() {
                PNotificationManager.notify("Menu Selection", openItem.getText());
            }
        });

        final PMenuItem closeItem = new PMenuItem("Close");
        openItem.setCommand(new PCommand() {

            @Override
            public void execute() {
                PNotificationManager.notify("Menu Selection", closeItem.getText());
            }
        });

        fileBar.addItem(newItem);
        fileBar.addItem(openItem);
        fileBar.addItem(closeItem);
        fileBar.addSeparator();

        final PMenuBar recentItem = new PMenuBar();

        fileBar.addItem("Recent", recentItem);

        return menuBar;
    }
}
