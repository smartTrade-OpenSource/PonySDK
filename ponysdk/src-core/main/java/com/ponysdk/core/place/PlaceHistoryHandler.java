
package com.ponysdk.core.place;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.EventBus;
import com.ponysdk.ui.server.basic.PHistory;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

public class PlaceHistoryHandler {

    private static final Logger log = LoggerFactory.getLogger(PlaceHistoryHandler.class);

    private final PHistory history;
    private final PlaceHistoryMapper mapper;
    private final PlaceController placeController;

    private Place defaultPlace;

    public PlaceHistoryHandler(final PHistory history, final PlaceHistoryMapper mapper, final PlaceController placeController, final EventBus eventBus) {
        this.history = history;
        this.mapper = mapper;
        this.placeController = placeController;

        eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeHandler() {

            @Override
            public void onPlaceChange(final PlaceChangeEvent event) {
                final Place newPlace = event.getNewPlace();
                history.newItem(newPlace.getToken(), false);
            }
        });

        history.addValueChangeHandler(new PValueChangeHandler<String>() {

            @Override
            public void onValueChange(final PValueChangeEvent<String> event) {
                final String token = event.getValue();
                handleHistoryToken(token);
            }
        });
    }

    public void setDefaultPlace(final Place defaultPlace) {
        this.defaultPlace = defaultPlace;
    }

    public void handleCurrentHistory() {
        handleHistoryToken(history.getToken());
    }

    private void handleHistoryToken(final String token) {

        Place newPlace = null;

        if (token == null || token.isEmpty()) {
            newPlace = defaultPlace;
        }

        if (newPlace == null) {
            newPlace = mapper.getPlace(token);
        }

        if (newPlace == null) {
            if (defaultPlace != null) {
                newPlace = defaultPlace;
                log.warn("Unrecognized history token: " + token + ". Going to default place: " + defaultPlace);
            } else {
                log.warn("Unrecognized history token: " + token);
                return;
            }
        }

        placeController.goTo(newPlace);
    }

}