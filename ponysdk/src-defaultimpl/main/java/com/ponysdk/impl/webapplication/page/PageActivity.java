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

package com.ponysdk.impl.webapplication.page;

import java.util.Collection;
import java.util.Collections;

import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.core.place.Place;
import com.ponysdk.core.security.Permission;
import com.ponysdk.impl.webapplication.application.ApplicationActivity;
import com.ponysdk.impl.webapplication.page.event.PageDisplayedEvent;
import com.ponysdk.impl.webapplication.page.place.EventPagePlace;
import com.ponysdk.impl.webapplication.page.place.PagePlace;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;

public abstract class PageActivity extends AbstractActivity implements InitializingActivity {

    private ApplicationActivity applicationActivity;

    private static PageActivity currentPage;

    protected final String pageName;

    protected final Collection<String> pageCategories;

    protected PageView pageView;

    protected PagePlace homePlace;

    protected abstract void onInitialization();

    protected abstract void onFirstShowPage();

    protected abstract void onShowPage(Place place);

    protected abstract void onLeavingPage();

    public PageActivity(final String pageName, final String pageCategory) {
        this(pageName, Collections.singleton(pageCategory), Permission.ALLOWED);
    }

    public PageActivity(final String pageName, final Collection<String> pageCategories) {
        this(pageName, pageCategories, Permission.ALLOWED);
    }

    public PageActivity(final String pageName, final String pageCategory, final Permission permission) {
        this(pageName, Collections.singleton(pageCategory), permission);
    }

    public PageActivity(final String pageName, final Collection<String> pageCategories, final Permission permission) {
        super(permission);

        this.pageName = pageName;
        this.pageCategories = pageCategories;

        homePlace = new PagePlace(this) {

            @Override
            public String getToken() {
                return pageName != null ? pageName : pageCategories.toString();
            }
        };
    }

    @Override
    public void start(final PAcceptsOneWidget world) {
        onFirstShowPage();
    }

    @Override
    public void goTo(final Place place, final PAcceptsOneWidget world) {
        if (currentPage != null) currentPage.onLeavingPage();

        currentPage = this;

        world.setWidget(pageView);

        super.goTo(place, world);

        onShowPage(place);

        final PageDisplayedEvent pageDisplayedEvent = new PageDisplayedEvent(this, this);
        fireEvent(pageDisplayedEvent);

        if (place instanceof EventPagePlace) {
            final EventPagePlace eventPagePlace = (EventPagePlace) place;
            fireEvent(eventPagePlace.getEvent());
        }
    }

    public void goToPage(final PagePlace place) {
        applicationActivity.goTo(place);
    }

    protected boolean isShown() {
        return equals(currentPage);
    }

    public String getPageName() {
        return pageName;
    }

    public Collection<String> getPageCategories() {
        return pageCategories;
    }

    public PageView getPageView() {
        return pageView;
    }

    public void setPageView(final PageView pageView) {
        this.pageView = pageView;
    }

    public PagePlace getDefautHomePagePlace() {
        return homePlace;
    }

    public void setApplicationActivity(final ApplicationActivity applicationActivity) {
        this.applicationActivity = applicationActivity;
    }

    @Override
    public final void afterContextInitialized() {
        if (this.pageView == null) {
            this.pageView = new DefaultScrollablePageView();
            this.pageView.setPageTitle(pageName);
        }
        onInitialization();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pageCategories == null) ? 0 : pageCategories.hashCode());
        result = prime * result + ((pageName == null) ? 0 : pageName.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PageActivity other = (PageActivity) obj;
        if (pageCategories == null) {
            if (other.pageCategories != null) return false;
        } else if (!pageCategories.equals(other.pageCategories)) return false;
        if (pageName == null) {
            if (other.pageName != null) return false;
        } else if (!pageName.equals(other.pageName)) return false;
        return true;
    }

}
