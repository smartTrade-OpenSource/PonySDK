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

public abstract class PageActivity extends AbstractActivity<PageView> implements InitializingActivity {

    protected final String pageName;

    protected final Collection<String> pageCategories;

    protected abstract void onInitialization();

    protected abstract void onFirstShowPage();

    protected abstract void onShowPage(Place place);

    protected abstract void onLeavingPage();

    protected final Permission permission;

    private boolean shown = false;

    public PageActivity(final String pageName, final String pageCategory) {
        this(pageName, Collections.singleton(pageCategory), Permission.ALLOWED);
    }

    public PageActivity(final String pageName, final String pageCategory, final Permission permission) {
        this(pageName, Collections.singleton(pageCategory), permission);
    }

    public PageActivity(final String pageName, final Collection<String> pageCategories) {
        this(pageName, pageCategories, Permission.ALLOWED);
    }

    public PageActivity(final String pageName, final Collection<String> pageCategories, final Permission permission) {
        this.pageName = pageName;
        this.pageCategories = pageCategories;
        this.permission = permission;
    }

    @Override
    protected void buildView() {
        onFirstShowPage();
    }

    @Override
    public void updateView(final Place place) {
        onShowPage(place);
        shown = true;
    }

    public void leave() {
        onLeavingPage();
        shown = false;
    }

    protected boolean isShown() {
        return shown;
    }

    public String getPageName() {
        return pageName;
    }

    public Permission getPermission() {
        return permission;
    }

    public Collection<String> getPageCategories() {
        return pageCategories;
    }

    public void setPageView(final PageView view) {
        this.view = view;
    }

    @Override
    public void afterContextInitialized() {
        if (this.view == null) {
            this.view = new DefaultScrollablePageView();
            this.view.setPageTitle(pageName);
        }
        onInitialization();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (pageCategories == null ? 0 : pageCategories.hashCode());
        result = prime * result + (pageName == null ? 0 : pageName.hashCode());
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
