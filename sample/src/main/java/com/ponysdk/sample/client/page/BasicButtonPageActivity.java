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
import com.ponysdk.sample.client.event.DemoBusinessEvent;
import com.ponysdk.sample.client.page.addon.BasicAddOn;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class BasicButtonPageActivity extends SamplePageActivity {

    protected PButton normalButton;

    protected PButton showLoadingOnRequestButton;

    protected PButton disabledOnRequestButton;

    protected PButton comboOnRequestButton;

    protected PButton disabledButton;

    private BasicAddOn basicAddOn;

    public BasicButtonPageActivity() {
        super("Basic Button", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();

        panel.add(buildButtonPanel());
        panel.add(buildThemeSelectorPanel());

        basicAddOn = new BasicAddOn();
        basicAddOn.send("initialization done");

        examplePanel.setWidget(panel);
    }

    private PWidget buildThemeSelectorPanel() {
        final PHorizontalPanel panel = new PHorizontalPanel();
        panel.setSpacing(10);

        final PListBox styleListBox = new PListBox(false);

        styleListBox.addItem(PonySDKTheme.BUTTON_WHITE);
        styleListBox.addItem(PonySDKTheme.BUTTON_BLACK);
        styleListBox.addItem(PonySDKTheme.BUTTON_BLUE);
        styleListBox.addItem(PonySDKTheme.BUTTON_GRAY);
        styleListBox.addItem(PonySDKTheme.BUTTON_GREEN);
        styleListBox.addItem(PonySDKTheme.BUTTON_ORANGE);
        styleListBox.addItem(PonySDKTheme.BUTTON_PINK);
        styleListBox.addItem(PonySDKTheme.BUTTON_ROSY);
        styleListBox.addItem("pony-PButton accent");

        styleListBox.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final PChangeEvent event) {
                final String styleName = styleListBox.getSelectedItem();

                normalButton.setStyleName(styleName);
                disabledOnRequestButton.setStyleName(styleName);
                showLoadingOnRequestButton.setStyleName(styleName);
                comboOnRequestButton.setStyleName(styleName);
                disabledButton.setStyleName(styleName);
            }
        });

        panel.add(new PLabel("Select the button style : "));
        panel.add(styleListBox);

        return panel;
    }

    private PHorizontalPanel buildButtonPanel() {
        final PHorizontalPanel panel = new PHorizontalPanel();
        panel.setSpacing(10);

        normalButton = new PButton("Normal Button");
        panel.add(normalButton);

        disabledButton = new PButton("Disabled Button");
        disabledButton.setEnabled(false);
        panel.add(disabledButton);

        showLoadingOnRequestButton = new PButton("Show loading on request");
        showLoadingOnRequestButton.showLoadingOnRequest(true);
        showLoadingOnRequestButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                fireEvent(new DemoBusinessEvent("Button clicked"));
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e) {}
            }
        });

        panel.add(showLoadingOnRequestButton);

        disabledOnRequestButton = new PButton("Disabled on request");
        disabledOnRequestButton.setEnabledOnRequest(false);
        disabledOnRequestButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                fireEvent(new DemoBusinessEvent("Button clicked"));
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e) {}
            }
        });

        panel.add(disabledOnRequestButton);

        comboOnRequestButton = new PButton("Show loading and disable on request");
        comboOnRequestButton.setEnabledOnRequest(false);
        comboOnRequestButton.showLoadingOnRequest(true);
        comboOnRequestButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                fireEvent(new DemoBusinessEvent("Button clicked"));
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e) {}
            }
        });

        panel.add(comboOnRequestButton);

        return panel;
    }
}
