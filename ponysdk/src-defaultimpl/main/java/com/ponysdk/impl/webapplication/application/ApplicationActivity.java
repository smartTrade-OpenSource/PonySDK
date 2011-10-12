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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.core.place.PlaceContext;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.impl.webapplication.footer.FooterActivity;
import com.ponysdk.impl.webapplication.header.HeaderActivity;
import com.ponysdk.impl.webapplication.menu.MenuActivity;
import com.ponysdk.impl.webapplication.notification.NotificationActivity;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.impl.webapplication.page.PageProvider;
import com.ponysdk.impl.webapplication.page.place.PagePlace;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

public class ApplicationActivity extends AbstractActivity implements ApplicationContextAware, PValueChangeHandler<String> {

    private final Map<String, PageActivity> pageActivitiesByName = new LinkedHashMap<String, PageActivity>();

    @Autowired
    private ApplicationView applicationView;

    @Autowired
    private MenuActivity menuActivity;

    @Autowired
    private HeaderActivity headerActivity;

    @Autowired
    private FooterActivity footerActivity;

    @Autowired
    private NotificationActivity notificationActivity;

    @Autowired
    private PageProvider pageProvider;

    @Override
    public void start(PAcceptsOneWidget world) {
        PonySession.getCurrent().getHistory().addValueChangeHandler(this);

        world.setWidget(applicationView.asWidget());

        menuActivity.start(applicationView.getMenu());
        headerActivity.start(applicationView.getHeader());
        footerActivity.start(applicationView.getFooter());
        notificationActivity.start(applicationView.getLogs());
    }

    public void goTo(PagePlace place) {
        PonySession.getCurrent().getPlaceController().goTo(place.getPageActivity(), place, applicationView.getBody());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        final Map<String, PageActivity> pageActivities = applicationContext.getBeansOfType(PageActivity.class);
        if (pageActivities.isEmpty()) {
            throw new FatalBeanException("The application must contain at least 1 PageActivity");
        }
        for (final PageActivity pageActivity : pageActivities.values()) {

            if (pageActivity.getPageName() != null) {
                pageActivitiesByName.put(pageActivity.getPageName(), pageActivity);
            } else if (pageActivity.getPageCategory() != null) {
                pageActivitiesByName.put(pageActivity.getPageCategory(), pageActivity);
            }
        }
    }

    public ApplicationView getApplicationView() {
        return applicationView;
    }

    @Override
    public void onValueChange(final String token) {
        final PlaceController placeController = PonySession.getCurrent().getPlaceController();
        if (placeController.getPlaceContext(token) == null) { // History on a new PonySesion instance
            final PageActivity pageActivity = pageProvider.getPageActivity(token);
            if (pageActivity != null) {
                final PlaceContext context = new PlaceContext();
                context.setPlace(new PagePlace(pageActivity) {

                    @Override
                    public String getToken() {
                        return token;
                    }
                });
                context.setActivity(pageActivity);
                context.setWorld(applicationView.getBody());

                placeController.registerPlaceContext(context);
            }
        }
    }
}
