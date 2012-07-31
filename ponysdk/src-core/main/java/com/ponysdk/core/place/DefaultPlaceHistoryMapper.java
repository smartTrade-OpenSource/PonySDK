
package com.ponysdk.core.place;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.core.event.EventBus;

public class DefaultPlaceHistoryMapper implements PlaceHistoryMapper {

    protected final Map<String, Place> placeContextByToken = new ConcurrentHashMap<String, Place>();

    public DefaultPlaceHistoryMapper(final EventBus eventBus) {
        eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeHandler() {

            @Override
            public void onPlaceChange(final PlaceChangeEvent event) {
                final Place newPlace = event.getNewPlace();
                placeContextByToken.put(newPlace.getToken(), newPlace);
            }
        });
    }

    @Override
    public Place getPlace(final String token) {
        return placeContextByToken.get(token);
    }

}
