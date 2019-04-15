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

import com.ponysdk.core.ui.basic.*;
import com.ponysdk.sample.client.event.DemoBusinessEvent;

public class BasicButtonPageActivity extends SamplePageActivity {

    protected PButton normalButton;

    protected PButton showLoadingOnRequestButton;

    protected PButton disabledOnRequestButton;

    protected PButton comboOnRequestButton;

    protected PButton disabledButton;

    public BasicButtonPageActivity() {
        super("Basic Button", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = Element.newPVerticalPanel();

        panel.add(buildButtonPanel());
        panel.add(buildThemeSelectorPanel());

        examplePanel.setWidget(panel);
    }

    private PWidget buildThemeSelectorPanel() {
        final PHorizontalPanel panel = Element.newPHorizontalPanel();
        panel.setSpacing(10);

        final PListBox styleListBox = Element.newPListBox(false);

        // styleListBox.addItem(PonySDKTheme.BUTTON_WHITE);
        // styleListBox.addItem(PonySDKTheme.BUTTON_BLACK);
        // styleListBox.addItem(PonySDKTheme.BUTTON_BLUE);
        // styleListBox.addItem(PonySDKTheme.BUTTON_GRAY);
        // styleListBox.addItem(PonySDKTheme.BUTTON_GREEN);
        // styleListBox.addItem(PonySDKTheme.BUTTON_ORANGE);
        // styleListBox.addItem(PonySDKTheme.BUTTON_PINK);
        // styleListBox.addItem(PonySDKTheme.BUTTON_ROSY);
        // styleListBox.addItem("pony-PButton accent");

        styleListBox.addChangeHandler(event -> {
            final String styleName = styleListBox.getSelectedItem();

            normalButton.setStyleName(styleName);
            disabledOnRequestButton.setStyleName(styleName);
            showLoadingOnRequestButton.setStyleName(styleName);
            comboOnRequestButton.setStyleName(styleName);
            disabledButton.setStyleName(styleName);
        });

        panel.add(Element.newPLabel("Select the button style : "));
        panel.add(styleListBox);

        return panel;
    }

    private PHorizontalPanel buildButtonPanel() {
        final PHorizontalPanel panel = Element.newPHorizontalPanel();
        panel.setSpacing(10);

        normalButton = Element.newPButton("Normal Button");
        panel.add(normalButton);

        disabledButton = Element.newPButton("Disabled Button");
        disabledButton.setEnabled(false);
        panel.add(disabledButton);

        showLoadingOnRequestButton = Element.newPButton("Show loading on request");
        showLoadingOnRequestButton.showLoadingOnRequest(true);
        showLoadingOnRequestButton.addClickHandler(event -> {
            fireEvent(new DemoBusinessEvent("Button clicked"));
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        panel.add(showLoadingOnRequestButton);

        disabledOnRequestButton = Element.newPButton("Disabled on request");
        disabledOnRequestButton.setEnabledOnRequest(false);
        disabledOnRequestButton.addClickHandler(event -> {
            fireEvent(new DemoBusinessEvent("Button clicked"));
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        panel.add(disabledOnRequestButton);

        comboOnRequestButton = Element.newPButton("Show loading and disable on request");
        comboOnRequestButton.setEnabledOnRequest(false);
        comboOnRequestButton.showLoadingOnRequest(true);
        comboOnRequestButton.addClickHandler(event -> {
            fireEvent(new DemoBusinessEvent("Button clicked"));
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        panel.add(comboOnRequestButton);

        return panel;
    }
}
