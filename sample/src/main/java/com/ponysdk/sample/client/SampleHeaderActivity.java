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

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPopupPanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.rich.POptionPane;
import com.ponysdk.core.ui.rich.POptionPane.PActionHandler;
import com.ponysdk.core.ui.rich.POptionPane.POption;
import com.ponysdk.core.ui.rich.POptionPane.POptionType;
import com.ponysdk.impl.webapplication.header.HeaderActivity;
import com.ponysdk.sample.client.datamodel.User;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;

public class SampleHeaderActivity extends HeaderActivity implements PClickHandler {

    private PPopupPanel popup;

    @Override
    public void buildView() {
        // view.addActionWidget(createUserAccountMenu(user));
    }

    private PWidget createUserAccountMenu(final User userLogged) {
        final PAnchor optionsAnchor = Element.newPAnchor(userLogged.getLogin());
        optionsAnchor.ensureDebugId("options_anchor");
        // optionsAnchor.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU);

        popup = Element.newPPopupPanel();
        getView().asWidget().getWindow().add(popup);
        // popup.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_POPUP);

        final PVerticalPanel panel = Element.newPVerticalPanel();
        final PLabel userName = Element.newPLabel(userLogged.getName());
        // userName.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_POPUP_USER_NAME);
        panel.add(userName);

        final PLabel userLogin = Element.newPLabel(userLogged.getLogin());
        // userLogin.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_POPUP_USER_LOGIN);
        panel.add(userLogin);

        final PAnchor signOutAnchor = Element.newPAnchor("Sign out");

        panel.add(signOutAnchor);
        popup.setWidget(panel);

        signOutAnchor.ensureDebugId("sign_out_anchor");
        signOutAnchor.addClickHandler(clickEvent -> {
            // optionsAnchor.removeStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_SELECTED);
            popup.hide();

            final POptionPane optionPane = POptionPane.showConfirmDialog(getView().asWidget().getWindow(), new PActionHandler() {

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
            }, "Really logout user " + userLogged.getName() + " ?", "Sign out", POptionType.YES_NO_OPTION);

            optionPane.asWidget().ensureDebugId("sign_out_dialog");
        });

        optionsAnchor.addClickHandler(clickEvent -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                // optionsAnchor.addStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_SELECTED);
                popup.setPopupPositionAndShow((offsetWidth, offsetHeight, windowWidth, windowHeight) -> {
                    final int left = windowWidth - 250;
                    popup.setPopupPosition(left, 26);

                    // PonySession.getCurrent().getRootLayoutPanel().addDomHandler(SampleHeaderActivity.this,
                    // PClickEvent.TYPE);
                });
            }
        });

        popup.addDomHandler(SampleHeaderActivity.this, PClickEvent.TYPE);

        popup.addCloseHandler(closeEvent -> {
            // optionsAnchor.removeStyleName(PonySDKTheme.HEADER_ACCOUNT_MENU_SELECTED);
            // PonySession.getCurrent().getRootLayoutPanel().removeHandler(SampleHeaderActivity.this,
            // PClickEvent.TYPE);
        });

        return optionsAnchor;
    }

    @Override
    public void onClick(final PClickEvent event) {
        if (popup.isShowing()) popup.hide();
    }
}
