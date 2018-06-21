/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.util;

import java.io.Reader;
import java.io.StringReader;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.spi.JsonProvider;

public class JsonUtil {

    /**
     * A constant representing the name of the default {@code JsonProvider} implementation class.
     */
    private static final String DEFAULT_PROVIDER = "org.glassfish.json.JsonProviderImpl";

    public static final JsonProvider createJsonProvider() {
        try {
            return (JsonProvider) Class.forName(DEFAULT_PROVIDER).getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            return JsonProvider.provider();
        }
    }

    /**
     * Creates a JSON array builder
     *
     * @return a JSON array builder
     */
    public static JsonArrayBuilder createArrayBuilder() {
        return createJsonProvider().createArrayBuilder();
    }

    /**
     * Creates a JSON object builder.
     *
     * @return a JSON object builder
     */
    public static JsonObjectBuilder createObjectBuilder() {
        return createJsonProvider().createObjectBuilder();
    }

    public static JsonObject readObject(final String text) {
        try (final JsonReader reader = JsonUtil.createReader(new StringReader(text))) {
            return reader.readObject();
        }
    }

    /**
     * Creates a JSON reader from a character stream.
     *
     * @param reader
     *            a reader from which JSON is to be read
     * @return a JSON reader
     */
    private static JsonReader createReader(final Reader reader) {
        return createJsonProvider().createReader(reader);
    }

}
