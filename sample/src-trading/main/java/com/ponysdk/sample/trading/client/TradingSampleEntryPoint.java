
package com.ponysdk.sample.trading.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.activity.ActivityManager;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.place.DefaultPlaceHistoryMapper;
import com.ponysdk.core.place.PlaceController;
import com.ponysdk.core.place.PlaceHistoryHandler;
import com.ponysdk.core.place.PlaceHistoryMapper;
import com.ponysdk.sample.trading.client.activity.SampleActivityMapper;
import com.ponysdk.sample.trading.client.place.LoginPlace;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.server.basic.PRootLayoutPanel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;

public class TradingSampleEntryPoint implements EntryPoint {

    private static Logger log = LoggerFactory.getLogger(TradingSampleEntryPoint.class);

    public static final String USER = "user";

    @Override
    public void start(final UIContext uiContext) {
        if (uiContext.getApplicationAttribute(USER) == null) uiContext.getHistory().newItem("", false);
        start0(uiContext);
    }

    @Override
    public void restart(final UIContext uiContext) {
        if (uiContext.getApplicationAttribute(USER) == null) uiContext.getHistory().newItem("", false);
        start0(uiContext);
    }

    private void start0(final UIContext uiContext) {

        PPusher.initialize();

        final PSimpleLayoutPanel panel = new PSimpleLayoutPanel();
        PRootLayoutPanel.get().add(panel);

        final EventBus eventBus = UIContext.getRootEventBus();

        final SampleActivityMapper mapper = new SampleActivityMapper();
        final PlaceHistoryMapper historyMapper = new DefaultPlaceHistoryMapper(eventBus);
        final PlaceController placeController = new PlaceController(uiContext.getHistory(), eventBus);

        final ActivityManager activityManager = new ActivityManager(mapper);
        activityManager.setDisplay(panel);

        final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(uiContext.getHistory(), historyMapper, placeController, eventBus);
        historyHandler.setDefaultPlace(new LoginPlace());
        historyHandler.handleCurrentHistory();
    }
}
