
package com.ponysdk.core.ui.place;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.ui.eventbus.EventBus;

public class DefaultPlaceHistoryMapper implements PlaceHistoryMapper {

    protected final Map<String, Place> placeContextByToken = new HashMap<>();

    protected PlaceTokenizer<Place> placeTokenizer;

    public DefaultPlaceHistoryMapper(final EventBus eventBus) {
        eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeHandler() {

            @Override
            public void onPlaceChange(final PlaceChangeEvent event) {
                final Place place = event.getNewPlace();
                placeContextByToken.put(getToken(place), place);
            }
        });
    }

    @Override
    public Place getPlace(final String token) {
        return placeContextByToken.get(token);
    }

    @Override
    public String getToken(final Place place) {
        return place.getClass().getSimpleName();
    }

    public void setPlaceTokenizer(final PlaceTokenizer<Place> placeTokenizer) {
        this.placeTokenizer = placeTokenizer;
    }
}
