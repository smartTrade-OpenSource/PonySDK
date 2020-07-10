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

import com.ponysdk.core.ui.activity.AbstractActivity;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.model.PKeyCodes;

public abstract class AbstractLoginPageActivity extends AbstractActivity<LoginPageView> implements PClickHandler {

    protected boolean remember;

    protected abstract void sendLogon(String login, String password);

    @Override
    public void onClick(final PClickEvent clickEvent) {
        sendLogon(view.getLogin(), view.getPassword());
    }

    public void setLoginPageView(final LoginPageView view) {
        this.view = view;
        final PKeyUpHandler keyPressHandler = new PKeyUpHandler() {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                view.clearMessages();
                sendLogon(view.getLogin(), view.getPassword());
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[] { PKeyCodes.ENTER };
            }
        };
        view.addLoginShortcutListener(keyPressHandler);
        view.addPasswordShortcutListener(keyPressHandler);

        this.view.addLoginClickHandler(this);
    }

}
