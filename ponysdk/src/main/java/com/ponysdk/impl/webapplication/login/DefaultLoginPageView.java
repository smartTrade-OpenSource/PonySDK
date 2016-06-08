/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.impl.webapplication.login;

import com.ponysdk.core.internalization.PString;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPasswordTextBox;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;

public class DefaultLoginPageView extends PSimplePanel implements LoginPageView {

    private final PButton loginButton;
    private final PCheckBox rememberMe;

    private final PTextBox loginTextBox = new PTextBox();

    private final PPasswordTextBox passwordTextBox = new PPasswordTextBox();

    private final PFlowPanel messagePanel = new PFlowPanel();

    private final PLabel versionInformation = new PLabel();

    private int messageIndex = 1;

    public DefaultLoginPageView(final String title) {

        loginButton = new PButton(PString.get("activity.login.signin"));
        rememberMe = new PCheckBox(PString.get("activity.login.rememberme"));

        loginTextBox.setStyleName("pony-LoginPage-LoginTextBox");
        passwordTextBox.setStyleName("pony-LoginPage-PasswordTextBox");
        loginButton.addStyleName("pony-LoginPage-SubmitButton");
        versionInformation.addStyleName("pony-LoginPage-VersionInformation");

        final PFlowPanel panel = new PFlowPanel();
        panel.setStyleName("pony-LoginPage");

        // logo
        final PLabel logo = new PLabel(title);
        logo.addStyleName("pony-LoginPage-Logo");
        panel.add(logo);

        // input
        final PFlowPanel inputPanel = new PFlowPanel();
        inputPanel.add(buildLoginInput());
        inputPanel.add(buildPasswordInput());
        panel.add(inputPanel);

        final PFlowPanel buttonAndCheckbox = new PFlowPanel();
        panel.add(buttonAndCheckbox);
        buttonAndCheckbox.add(loginButton);
        buttonAndCheckbox.add(rememberMe);

        // messages
        panel.add(messagePanel);
        panel.add(versionInformation);

        setWidget(panel);
    }

    private PWidget buildLoginInput() {
        final PFlowPanel panel = new PFlowPanel();
        panel.add(new PLabel(PString.get("activity.login.login")));
        panel.add(loginTextBox);
        return panel;
    }

    private PWidget buildPasswordInput() {
        final PFlowPanel panel = new PFlowPanel();
        panel.add(new PLabel(PString.get("activity.login.password")));
        panel.add(passwordTextBox);
        return panel;
    }

    @Override
    public void addLoginShortcutListener(final PKeyUpHandler handler) {
        loginTextBox.addKeyUpHandler(handler);
    }

    @Override
    public void addLoginClickHandler(final PClickHandler handler) {
        loginButton.addClickHandler(handler);
    }

    @Override
    public void addPasswordShortcutListener(final PKeyUpHandler handler) {
        passwordTextBox.addKeyUpHandler(handler);
    }

    @Override
    public void addVersionInformation(final String version) {
        versionInformation.setText(version);
    }

    @Override
    public void addMessage(final String message) {
        final PLabel messageLabel = new PLabel(message);
        messageLabel.ensureDebugId("login_page_message_" + messageIndex);
        messageIndex++;
        messagePanel.add(messageLabel);
    }

    @Override
    public String getLogin() {
        return loginTextBox.getText();
    }

    @Override
    public String getPassword() {
        return passwordTextBox.getText();
    }

    @Override
    public Boolean isRememberMe() {
        return rememberMe.getValue();
    }

    @Override
    public void clearMessages() {
        messagePanel.clear();
        messageIndex = 1;
    }

    @Override
    public void setFocusOnLogin(final boolean focused) {
        loginTextBox.setFocus(focused);
    }

    public PButton getLoginButton() {
        return loginButton;
    }

    public PTextBox getLoginTextBox() {
        return loginTextBox;
    }

    public PPasswordTextBox getPasswordTextBox() {
        return passwordTextBox;
    }

    public PCheckBox getRememberMe() {
        return rememberMe;
    }

}
