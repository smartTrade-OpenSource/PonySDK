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

package com.ponysdk.impl.webapplication.login;

import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;

public abstract class AbstractLoginPageActivity extends AbstractActivity implements PClickHandler {

    protected LoginPageView loginPageView;

    protected PAcceptsOneWidget world;

    protected boolean remember;

    protected abstract void sendLogon(String login, String password);

    @Override
    public void onClick(final PClickEvent clickEvent) {
        sendLogon(loginPageView.getLogin(), loginPageView.getPassword());
    }

    @Override
    public void start(final PAcceptsOneWidget world) {
        this.world = world;
        world.setWidget(loginPageView);
    }

    public LoginPageView getLoginPageView() {
        return loginPageView;
    }

    public PAcceptsOneWidget getWorld() {
        return world;
    }

    public void setLoginPageView(final LoginPageView loginPageView) {
        this.loginPageView = loginPageView;
        final PKeyUpFilterHandler keyPressHandler = new PKeyUpFilterHandler(PKeyCodes.ENTER) {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                loginPageView.clearMessages();
                sendLogon(loginPageView.getLogin(), loginPageView.getPassword());
            }
        };
        loginPageView.addLoginShortcutListener(keyPressHandler);
        loginPageView.addPasswordShortcutListener(keyPressHandler);

        this.loginPageView.addLoginClickHandler(this);
    }

}
