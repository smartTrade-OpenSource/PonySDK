/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import com.ponysdk.core.ui.eventbus.EventBus;
import com.ponysdk.core.ui.place.DefaultPlaceHistoryMapper;
import com.ponysdk.core.ui.place.Place;
import com.ponysdk.impl.webapplication.page.place.PagePlace;

public class ApplicationPlaceHistoryMapper extends DefaultPlaceHistoryMapper {

    private PageProvider pageProvider;

    public ApplicationPlaceHistoryMapper(final EventBus eventBus) {
        super(eventBus);
    }

    @Override
    public Place getPlace(final String token) {
        final Place place = placeContextByToken.get(token);
        if (place == null) {
            // History on a new PonySesion instance
            final PageActivity pageActivity = pageProvider.getPageActivity(token);
            if (pageActivity != null) return new PagePlace(pageActivity.getPageName());
        }

        return place;
    }

    public void setPageProvider(final PageProvider pageProvider) {
        this.pageProvider = pageProvider;
    }

    @Override
    public String getToken(final Place place) {
        if (place instanceof PagePlace && ((PagePlace) place).getToken() != null) {
            return ((PagePlace) place).getToken();
        }
        return place.getClass().getSimpleName();
    }

}
