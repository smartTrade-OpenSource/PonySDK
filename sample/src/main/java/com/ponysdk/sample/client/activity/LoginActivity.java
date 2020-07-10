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

package com.ponysdk.sample.client.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.activity.AbstractActivity;
import com.ponysdk.core.ui.basic.event.PKeyPressEvent;
import com.ponysdk.core.ui.basic.event.PKeyPressHandler;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.impl.webapplication.login.DefaultLoginPageView;
import com.ponysdk.impl.webapplication.page.place.PagePlace;
import com.ponysdk.sample.client.datamodel.User;
import com.ponysdk.sample.client.event.UserLoggedInEvent;

public class LoginActivity extends AbstractActivity<DefaultLoginPageView> {

    private static Logger log = LoggerFactory.getLogger(LoginActivity.class);

    @Override
    public DefaultLoginPageView getView() {
        if (view == null) view = new DefaultLoginPageView("PonySDK trading showcase");
        return super.getView();
    }

    @Override
    protected void buildView() {
        log.info("Showing login page");

        view.getLoginTextBox().setText("Trader");
        view.getPasswordTextBox().setText("Trader");

        view.getLoginTextBox().ensureDebugId("login");
        view.getPasswordTextBox().ensureDebugId("password");
        view.getLoginButton().ensureDebugId("signin");

        view.addLoginClickHandler(clickEvent -> doLogin());

        view.asWidget().addKeyPressHandler(new PKeyPressHandler() {

            @Override
            public void onKeyPress(final PKeyPressEvent keyPressEvent) {
                doLogin();
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[] { PKeyCodes.ENTER };
            }
        });
    }

    private void doLogin() {
        final User user = new User();
        user.setID(0);
        user.setLogin(view.getLogin());
        user.setName(view.getLogin());
        user.setPassword(view.getPassword());

        // UIContext.get().setApplicationAttribute(UISampleEntryPoint.USER, user);

        final UserLoggedInEvent loggedInEvent = new UserLoggedInEvent(LoginActivity.this, user);
        loggedInEvent.setBusinessMessage(view.getLogin() + " is now connected");
        fireEvent(loggedInEvent);

        log.info("Logged with #" + user.getLogin() + "/" + user.getPassword());

        goTo(new PagePlace("CheckBox"));
    }
}
