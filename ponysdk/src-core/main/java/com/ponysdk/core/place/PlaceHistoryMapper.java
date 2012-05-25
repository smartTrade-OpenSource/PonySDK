
package com.ponysdk.core.place;

public interface PlaceHistoryMapper {

    /**
     * Returns the {@link Place} associated with the given token.
     * 
     * @param token
     *            a String token
     * @return a {@link Place} instance
     */
    Place getPlace(String token);

}
