/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mmsili
 *
 */
public class URLUtils {

    public static Map<String, String> getQueryStringParameters(final String queryString) {
        final Map<String, String> queryParameters = new HashMap<>();
        String[] firstSplit;

        if (queryString.contains("&") == true) {
            firstSplit = queryString.split("&");
            for (final String string : firstSplit) {
                final String[] secondSplit = string.split("=");
                if (secondSplit.length == 1) queryParameters.put(secondSplit[0], null);
                else queryParameters.put(secondSplit[0], secondSplit[1]);
            }
        } else {
            firstSplit = queryString.split("=");
            if (firstSplit.length == 1) queryParameters.put(firstSplit[0], null);
            else queryParameters.put(firstSplit[0], firstSplit[1]);
        }
        return queryParameters;
    }
}
