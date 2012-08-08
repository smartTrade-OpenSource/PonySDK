
package com.ponysdk.sample.trading.client.activity;

import com.ponysdk.core.activity.Activity;
import com.ponysdk.core.activity.ActivityMapper;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.SimplePageView;
import com.ponysdk.sample.trading.client.place.LoginPlace;

public class SampleActivityMapper implements ActivityMapper {

    private LoginActivity loginActivity;
    private MarketPageActivity marketPageActivity;

    @Override
    public Activity getActivity(final Place place) {
        if (place instanceof LoginPlace) {
            if (loginActivity == null) loginActivity = new LoginActivity();
            return loginActivity;
        } else {
            if (marketPageActivity == null) {
                marketPageActivity = new MarketPageActivity();
                marketPageActivity.setPageView(new SimplePageView());
            }
            return marketPageActivity;
        }
    }

}
