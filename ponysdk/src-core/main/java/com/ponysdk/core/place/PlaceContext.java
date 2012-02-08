
package com.ponysdk.core.place;

import com.ponysdk.core.activity.Activity;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;

public class PlaceContext {

    private Place place = Place.NOWHERE;

    private Activity activity;

    private PAcceptsOneWidget world;

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        this.place = place;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public PAcceptsOneWidget getWorld() {
        return world;
    }

    public void setWorld(PAcceptsOneWidget world) {
        this.world = world;
    }
}