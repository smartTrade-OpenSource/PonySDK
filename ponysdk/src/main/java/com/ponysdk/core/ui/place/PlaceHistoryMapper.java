
package com.ponysdk.core.ui.place;

public interface PlaceHistoryMapper {

    /**
     * Returns the {@link Place} associated with the given token.
     * 
     * @param token
     *            a String token
     * @return a {@link Place} instance
     */
    Place getPlace(String token);

    /**
     * Returns the String token associated with the given {@link Place}.
     * 
     * @param place
     *            a {@link Place} instance
     * @return a String token
     */
    String getToken(Place place);

}
