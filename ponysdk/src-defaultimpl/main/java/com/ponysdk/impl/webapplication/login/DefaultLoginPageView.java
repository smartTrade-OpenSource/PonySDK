package com.ponysdk.impl.webapplication.login;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPasswordTextBox;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class DefaultLoginPageView extends PSimplePanel implements LoginPageView {

    private final PButton loginButton = new PButton("Sign in");
    private final PTextBox loginTextBox = new PTextBox();
    private final PPasswordTextBox passwordTextBox = new PPasswordTextBox();
    private final PCheckBox rememberMe = new PCheckBox("Stay signed in");
    private final PVerticalPanel messagePanel = new PVerticalPanel();
    private final PLabel versionInformation = new PLabel("");
    private int messageIndex = 1;

    public DefaultLoginPageView(String title) {
        setSizeFull();

        loginTextBox.setStyleName("pony-LoginPage-LoginTextBox");
        passwordTextBox.setStyleName("pony-LoginPage-PasswordTextBox");
        loginButton.addStyleName("pony-LoginPage-SubmitButton");
        versionInformation.addStyleName("pony-LoginPage-VersionInformation");

        final PVerticalPanel panel = new PVerticalPanel();
        panel.setStyleName("pony-LoginPage");

        // logo
        final PLabel logo = new PLabel(title);
        logo.addStyleName("pony-LoginPage-Logo");
        panel.add(logo);

        // input
        final PVerticalPanel inputPanel = new PVerticalPanel();
        inputPanel.setSizeFull();
        inputPanel.add(buildLoginInput());
        inputPanel.add(buildPasswordInput());
        panel.add(inputPanel);

        final PHorizontalPanel buttonAndCheckbox = new PHorizontalPanel();
        panel.add(buttonAndCheckbox);
        buttonAndCheckbox.add(loginButton);
        final PSimplePanel separator = new PSimplePanel();
        separator.setWidth("20px");
        buttonAndCheckbox.add(separator);
        buttonAndCheckbox.add(rememberMe);
        buttonAndCheckbox.setCellVerticalAlignment(rememberMe, PVerticalAlignment.ALIGN_MIDDLE);

        loginButton.setStyleName(PonySDKTheme.BUTTON);
        loginButton.addStyleName("blue");

        // messages
        panel.add(messagePanel);
        panel.add(versionInformation);
        panel.setCellHorizontalAlignment(versionInformation, PHorizontalAlignment.ALIGN_RIGHT);

        setWidget(panel);

        loginTextBox.setFocus(true);
    }

    private PWidget buildLoginInput() {
        final PVerticalPanel panel = new PVerticalPanel();
        panel.setSizeFull();
        panel.add(new PLabel("Login:"));
        panel.add(loginTextBox);
        return panel;
    }

    private PWidget buildPasswordInput() {
        final PVerticalPanel panel = new PVerticalPanel();
        panel.setSizeFull();
        panel.add(new PLabel("Password:"));
        panel.add(passwordTextBox);
        return panel;
    }

    @Override
    public void addLoginShortcutListener(PKeyUpHandler handler) {
        loginTextBox.addKeyUpHandler(handler);
    }

    @Override
    public void addLoginClickHandler(PClickHandler handler) {
        loginButton.addClickHandler(handler);
    }

    @Override
    public void addPasswordShortcutListener(PKeyUpHandler handler) {
        passwordTextBox.addKeyUpHandler(handler);
    }

    @Override
    public void addVersionInformation(String version) {
        versionInformation.setText(version);
    }

    @Override
    public void addMessage(String message) {
        final PLabel messageLabel = new PLabel(message);
        messageLabel.ensureDebugId("login_page_message_" + messageIndex);
        messageIndex++;
        this.messagePanel.add(messageLabel);
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
    public void setFocusOnLogin(boolean focused) {
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
