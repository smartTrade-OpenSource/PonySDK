
package com.ponysdk.impl.webapplication.login;

import com.ponysdk.core.internalization.PString;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPasswordTextBox;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;

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

        loginButton.setStyleName(PonySDKTheme.BUTTON_BLUE);

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
