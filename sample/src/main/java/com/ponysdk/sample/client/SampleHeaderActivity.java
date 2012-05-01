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

package com.ponysdk.sample.client;

import com.ponysdk.core.PonySession;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.impl.webapplication.header.HeaderActivity;
import com.ponysdk.sample.client.datamodel.User;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PDialogBox;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PPopupPanel.PPositionCallback;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PCloseHandler;
import com.ponysdk.ui.server.rich.POptionPane;
import com.ponysdk.ui.server.rich.POptionPane.PActionHandler;
import com.ponysdk.ui.server.rich.POptionPane.POption;
import com.ponysdk.ui.server.rich.POptionPane.POptionType;

public class SampleHeaderActivity extends HeaderActivity implements PClickHandler {

    private PPopupPanel popup;

    @Override
    public void start(final PAcceptsOneWidget world) {
        super.start(world);

        final User user = PonySession.getCurrent().getApplicationAttribute(UISampleEntryPoint.USER);
        getHeaderView().addActionWidget(createUserAccountMenu(user));
    }

    private PWidget createUserAccountMenu(final User userLogged) {
        final PAnchor optionsAnchor = new PAnchor(userLogged.getLogin());
        optionsAnchor.ensureDebugId("options_anchor");
        optionsAnchor.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU);

        popup = new PPopupPanel();
        popup.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_POPUP);

        final PVerticalPanel panel = new PVerticalPanel();
        final PLabel userName = new PLabel(userLogged.getName());
        userName.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_POPUP_USER_NAME);
        panel.add(userName);

        final PLabel userLogin = new PLabel(userLogged.getLogin());
        userLogin.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_POPUP_USER_LOGIN);
        panel.add(userLogin);

        final PAnchor signOutAnchor = new PAnchor("Sign out");

        panel.add(signOutAnchor);
        popup.setWidget(panel);

        signOutAnchor.ensureDebugId("sign_out_anchor");
        signOutAnchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                optionsAnchor.removeStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_SELECTED);
                popup.hide();

                final POptionPane optionPane = POptionPane.showConfirmDialog(new PActionHandler() {

                    @Override
                    public void onAction(final PDialogBox dialogBox, final String option) {
                        if (POption.YES_OPTION.equals(option)) {
                            dialogBox.hide();
                            final UserLoggedOutEvent userLoggedOutEvent = new UserLoggedOutEvent(this, userLogged);
                            fireEvent(userLoggedOutEvent);
                        } else {
                            dialogBox.hide();
                        }
                    }
                }, "Really logout user " + userLogged.getName() + " ?", null, "Sign out", POptionType.YES_NO_OPTION);

                optionPane.getDialogBox().ensureDebugId("sign_out_dialog");
            }
        });

        optionsAnchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                if (popup.isShowing()) {
                    popup.hide();
                } else {
                    optionsAnchor.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_SELECTED);
                    popup.setPopupPositionAndShow(new PPositionCallback() {

                        @Override
                        public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
                            final int left = windowWidth - 250;
                            popup.setPopupPosition(left, 26);

                            // PonySession.getCurrent().getRootLayoutPanel().addDomHandler(SampleHeaderActivity.this,
                            // PClickEvent.TYPE);
                        }
                    });
                }
            }
        });

        popup.addDomHandler(SampleHeaderActivity.this, PClickEvent.TYPE);

        popup.addCloseHandler(new PCloseHandler() {

            @Override
            public void onClose() {
                optionsAnchor.removeStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_SELECTED);
                // PonySession.getCurrent().getRootLayoutPanel().removeHandler(SampleHeaderActivity.this,
                // PClickEvent.TYPE);
            }
        });

        return optionsAnchor;
    }

    @Override
    public void onClick(final PClickEvent event) {
        if (popup.isShowing()) popup.hide();
    }
}
