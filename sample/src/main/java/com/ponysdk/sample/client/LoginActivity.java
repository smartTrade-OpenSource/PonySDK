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

import org.springframework.beans.factory.annotation.Autowired;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.impl.webapplication.application.ApplicationActivity;
import com.ponysdk.impl.webapplication.login.DefaultLoginPageView;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.impl.webapplication.page.place.PagePlace;
import com.ponysdk.sample.client.datamodel.User;
import com.ponysdk.sample.client.event.UserLoggedInEvent;
import com.ponysdk.sample.client.page.CheckBoxPageActivity;
import com.ponysdk.sample.client.place.ApplicationPlace;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressFilterHandler;

public class LoginActivity extends AbstractActivity {

    @Autowired
    private ApplicationActivity applicationActivity;
    @Autowired
    private CheckBoxPageActivity checkBoxPageActivity;

    private DefaultLoginPageView loginPageView;

    private PAcceptsOneWidget world;

    @Override
    public void start(final PAcceptsOneWidget world) {
        this.world = world;

        loginPageView = new DefaultLoginPageView("PonySDK Showcase");

        loginPageView.getLoginTextBox().setText("Guest");
        loginPageView.getPasswordTextBox().setText("Guest");

        loginPageView.addLoginClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                doLogin();
            }

        });
        world.setWidget(loginPageView);

        loginPageView.asWidget().addDomHandler(new PKeyPressFilterHandler(PKeyCodes.ENTER) {

            @Override
            public void onKeyPress(final int keyCode) {
                doLogin();
            }
        }, PKeyPressEvent.TYPE);

    }

    private void doLogin() {
        final User user = new User();
        user.setID(0);
        user.setLogin(loginPageView.getLogin());
        user.setName(loginPageView.getLogin());
        user.setPassword(loginPageView.getPassword());

        PonySession.getCurrent().setApplicationAttribute(UISampleEntryPoint.USER, user);

        final UserLoggedInEvent loggedInEvent = new UserLoggedInEvent(LoginActivity.this, user);
        loggedInEvent.setBusinessMessage(loginPageView.getLogin() + " is now connected");
        fireEvent(loggedInEvent);

        applicationActivity.goTo(new ApplicationPlace(), world);

        applicationActivity.goTo(buildPagePlace("CheckBox", checkBoxPageActivity));
    }

    private PagePlace buildPagePlace(final String token, final PageActivity pageActivity) {
        return new PagePlace(pageActivity) {

            @Override
            public String getToken() {
                return token;
            }
        };
    }

}
