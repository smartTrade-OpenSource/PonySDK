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

package com.ponysdk.core.terminal.tools;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.ponysdk.core.model.MappingPath;

public class URL {

    public static String getHostURL() {
        return GWT.getHostPageBaseURL();
    }

    public static String getWebsocketURL() {
        return getWebsocketURL(null);
    }

    public static String getWebsocketURL(final Map<String, String> parameters) {
        String url = getHostURL().replaceFirst("http", "ws") + MappingPath.WEBSOCKET;

        if (parameters != null) {
            url += "?";

            final Iterator<Entry<String, String>> iterator = parameters.entrySet().iterator();

            while (iterator.hasNext()) {
                final Entry<String, String> entry = iterator.next();
                url += entry.getKey() + "=" + entry.getValue();
                if (iterator.hasNext()) url += "&";
            }
        }

        return url;
    }

}
