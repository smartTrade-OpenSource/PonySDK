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

package com.ponysdk.impl.webapplication.application;

import com.ponysdk.core.activity.Activity;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.footer.FooterActivity;
import com.ponysdk.impl.webapplication.header.HeaderActivity;
import com.ponysdk.impl.webapplication.menu.MenuActivity;
import com.ponysdk.impl.webapplication.notification.NotificationActivity;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.impl.webapplication.page.PageProvider;
import com.ponysdk.impl.webapplication.page.place.HasActivity;
import com.ponysdk.impl.webapplication.page.place.HasPageName;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;

public class ApplicationActivity implements Activity {

    private ApplicationView applicationView;

    private MenuActivity menuActivity;
    private HeaderActivity headerActivity;
    private FooterActivity footerActivity;
    private NotificationActivity notificationActivity;

    private PageProvider pageProvider;

    private PageActivity currentPageActivity;

    @Override
    public void start(final PAcceptsOneWidget world, final Place place) {
        if (applicationView.getMenu() != null) {
            menuActivity.start(applicationView.getMenu(), place);
        }

        if (applicationView.getHeader() != null) {
            headerActivity.start(applicationView.getHeader(), place);
        }

        if (applicationView.getFooter() != null) {
            footerActivity.start(applicationView.getFooter(), place);
        }

        if (applicationView.getLogs() != null) {
            notificationActivity.start(applicationView.getLogs(), place);
        }

        if (place instanceof HasPageName) {
            final HasPageName pagePlace = (HasPageName) place;
            final PageActivity pageActivity = pageProvider.getPageActivity(pagePlace.getPageName());

            if (!com.ponysdk.core.security.SecurityManager.checkPermission(pageActivity.getPermission())) throw new RuntimeException("Missing permission #" + pageActivity.getPermission());

            if (currentPageActivity != null) {
                currentPageActivity.leave();
            }

            currentPageActivity = pageActivity;

            pageActivity.start(applicationView.getBody(), place);
        }

        if (place instanceof HasActivity) {
            final HasActivity activityPlace = (HasActivity) place;
            activityPlace.getActivity().start(applicationView.getBody(), place);
        }

        world.setWidget(applicationView.asWidget());
    }

    public ApplicationView getApplicationView() {
        return applicationView;
    }

    public void setApplicationView(final ApplicationView applicationView) {
        this.applicationView = applicationView;
    }

    public void setMenuActivity(final MenuActivity menuActivity) {
        this.menuActivity = menuActivity;
    }

    public void setHeaderActivity(final HeaderActivity headerActivity) {
        this.headerActivity = headerActivity;
    }

    public void setFooterActivity(final FooterActivity footerActivity) {
        this.footerActivity = footerActivity;
    }

    public void setNotificationActivity(final NotificationActivity notificationActivity) {
        this.notificationActivity = notificationActivity;
    }

    public void setPageProvider(final PageProvider pageProvider) {
        this.pageProvider = pageProvider;
    }

}
