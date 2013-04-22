
package com.ponysdk.core.activity;

import com.ponysdk.core.place.Place;

/**
 * Finds the activity to run for a given {@link Place}, used to configure an {@link ActivityManager}.
 */
public interface ActivityMapper {

    /**
     * Returns the activity to run for the given {@link Place}, or null.
     * 
     * @param place
     *            a Place
     */
    Activity getActivity(Place place);
}
