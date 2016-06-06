
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
        if (place instanceof PagePlace && ((PagePlace) place).getToken() != null) { return ((PagePlace) place).getToken(); }
        return place.getClass().getSimpleName();
    }

}
