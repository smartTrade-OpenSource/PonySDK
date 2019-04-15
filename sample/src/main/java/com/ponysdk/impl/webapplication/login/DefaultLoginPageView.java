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

import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.i18n.PString;

public class DefaultLoginPageView extends PSimplePanel implements LoginPageView {

    private final PButton loginButton;
    private final PCheckBox rememberMe;

    private final PTextBox loginTextBox = Element.newPTextBox();

    private final PPasswordTextBox passwordTextBox = Element.newPPasswordTextBox();

    private final PFlowPanel messagePanel = Element.newPFlowPanel();

    private final PLabel versionInformation = Element.newPLabel();

    private int messageIndex = 1;

    public DefaultLoginPageView(final String title) {

        loginButton = Element.newPButton(PString.get("activity.login.signin"));
        rememberMe = Element.newPCheckBox(PString.get("activity.login.rememberme"));

        loginTextBox.setStyleName("pony-LoginPage-LoginTextBox");
        passwordTextBox.setStyleName("pony-LoginPage-PasswordTextBox");
        loginButton.addStyleName("pony-LoginPage-SubmitButton");
        versionInformation.addStyleName("pony-LoginPage-VersionInformation");

        final PFlowPanel panel = Element.newPFlowPanel();
        panel.setStyleName("pony-LoginPage");

        // logo
        final PLabel logo = Element.newPLabel(title);
        logo.addStyleName("pony-LoginPage-Logo");
        panel.add(logo);

        // input
        final PFlowPanel inputPanel = Element.newPFlowPanel();
        inputPanel.add(buildLoginInput());
        inputPanel.add(buildPasswordInput());
        panel.add(inputPanel);

        final PFlowPanel buttonAndCheckbox = Element.newPFlowPanel();
        panel.add(buttonAndCheckbox);
        buttonAndCheckbox.add(loginButton);
        buttonAndCheckbox.add(rememberMe);

        // messages
        panel.add(messagePanel);
        panel.add(versionInformation);

        setWidget(panel);
    }

    private PWidget buildLoginInput() {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(Element.newPLabel(PString.get("activity.login.login")));
        panel.add(loginTextBox);
        return panel;
    }

    private PWidget buildPasswordInput() {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(Element.newPLabel(PString.get("activity.login.password")));
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
        final PLabel messageLabel = Element.newPLabel(message);
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
        if (focused) loginTextBox.focus();
        else loginTextBox.blur();
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
