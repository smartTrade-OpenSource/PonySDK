
package com.ponysdk.core.place;

/**
 * Implemented by objects responsible for text serialization and deserialization of Place objects.
 * 
 * @param <P>
 *            a subtype of {@link Place}
 */
public interface PlaceTokenizer<P extends Place> {

    /**
     * Returns the {@link Place} associated with the given token.
     * 
     * @param token
     *            a String token
     * @return a {@link Place} of type P
     */
    P getPlace(String token);

    /**
     * Returns the token associated with the given {@link Place}.
     * 
     * @param place
     *            a {@link Place} of type P
     * @return a String token
     */
    String getToken(P place);
}
