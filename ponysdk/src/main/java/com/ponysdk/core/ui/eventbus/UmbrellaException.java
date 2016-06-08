/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.ui.eventbus;

import java.util.Iterator;
import java.util.Set;

public class UmbrellaException extends RuntimeException {

    private static final long serialVersionUID = -647912793032725942L;

    private static final String MULTIPLE = " exceptions caught: ";
    private static final String ONE = "Exception caught: ";

    private final Set<Throwable> causes;

    protected static Throwable makeCause(final Set<Throwable> causes) {
        final Iterator<Throwable> iterator = causes.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        return iterator.next();
    }

    protected static String makeMessage(final Set<Throwable> causes) {
        final int count = causes.size();
        if (count == 0) {
            return null;
        }

        final StringBuilder b = new StringBuilder(count == 1 ? ONE : count + MULTIPLE);
        boolean first = true;
        for (final Throwable t : causes) {
            if (first) {
                first = false;
            } else {
                b.append("; ");
            }
            b.append(t.getMessage());
        }

        return b.toString();
    }

    public UmbrellaException(final Set<Throwable> causes) {
        super(makeMessage(causes), makeCause(causes));
        this.causes = causes;
    }

    public Set<Throwable> getCauses() {
        return causes;
    }
}
