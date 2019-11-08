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

package com.ponysdk.core.model;

import java.util.Collection;
import java.util.Date;

public final class DateConverter {

    private static final int MAX_ARRAY_SIZE = Byte.MAX_VALUE * 2 + 1;
    private static final int EMPTY_TIMESTAMP = -1;

    private DateConverter() {
    }

    public static Long[][] encode(final Collection<Date> dates) {
        if (dates == null || dates.isEmpty()) return null;
        final int remainder = dates.size() % MAX_ARRAY_SIZE;
        final int nbArrays = dates.size() / MAX_ARRAY_SIZE + (remainder == 0 ? 0 : 1);
        final Long[][] arrays = new Long[nbArrays][];
        for (int i = 0; i < arrays.length - 1; i++) {
            arrays[i] = new Long[MAX_ARRAY_SIZE];
        }
        arrays[arrays.length - 1] = new Long[remainder == 0 ? MAX_ARRAY_SIZE : remainder];
        int i = 0;
        for (final Date date : dates) {
            arrays[i / MAX_ARRAY_SIZE][i % MAX_ARRAY_SIZE] = encode(date);
            i++;
        }
        return arrays;
    }

    public static Long encode(final Date date) {
        return date != null ? date.getTime() : null;
    }

    public static Date decode(final long timestamp) {
        return new Date(timestamp);
    }

    public static long toTimestamp(final Date date) {
        return date != null ? date.getTime() : EMPTY_TIMESTAMP;
    }

    public static Date fromTimestamp(final long timestamp) {
        return timestamp != EMPTY_TIMESTAMP ? new Date(timestamp) : null;
    }

}
