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
package com.ponysdk.impl.webapplication.menu;

import org.springframework.beans.factory.annotation.Autowired;

import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.impl.webapplication.application.ApplicationActivity;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.impl.webapplication.page.PageProvider;
import com.ponysdk.impl.webapplication.page.event.PageDisplayedEvent;
import com.ponysdk.impl.webapplication.page.event.PageDisplayedEvent.PageDisplayHandler;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class MenuActivity extends AbstractActivity implements PageDisplayHandler, InitializingActivity, PSelectionHandler<String> {

    @Autowired
    private MenuView menuView;

    @Autowired
    private ApplicationActivity applicationActivity;

    @Autowired
    private PageProvider pageProvider;

    @Override
    public void onPageDisplayed(PageDisplayedEvent event) {
        if (event.getSource().equals(this))
            return;

        final PageActivity pageActivity = event.getPageActivity();
        if (pageActivity.getPageName() != null) {
            menuView.selectItem(pageActivity.getPageCategory(), pageActivity.getPageName());
        }
    }

    @Override
    public void start(PAcceptsOneWidget world) {
        world.setWidget(menuView);
        for (final PageActivity pageActivity : pageProvider.getPageActivities()) {
            menuView.addItem(pageActivity.getPageCategory(), pageActivity.getPageName());
        }

    }

    @Override
    public void onSelection(PSelectionEvent<String> event) {
        selectItem(event.getSelectedItem());
    }

    public void selectItem(final String pageName) {
        final PageActivity pageActivity = pageProvider.getPageActivity(pageName);
        if (pageActivity != null) {
            applicationActivity.goTo(pageActivity.getDefautHomePagePlace());
        }
    }

    @Override
    public void afterContextInitialized() {
        this.menuView.addSelectionHandler(this);
        addHandler(PageDisplayedEvent.TYPE, this);
    }

    public void setMenuView(MenuView menuView) {
        this.menuView = menuView;
    }

}
