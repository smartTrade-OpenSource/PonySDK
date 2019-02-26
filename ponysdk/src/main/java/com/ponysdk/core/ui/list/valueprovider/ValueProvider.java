/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.ui.list.valueprovider;

import java.util.function.Function;

import com.ponysdk.core.ui.list.DataGridActivity;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;

/**
 * <p>
 * Way to provide data to a {@link DataGridActivity} for a given cell
 * </p>
 * <p>
 * Defined in a {@link DataGridColumnDescriptor}
 * </p>
 *
 * @param <D>
 *            type of data managed by a given {@link DataGridActivity}
 * @param <V>
 *            type of the rendered object for a given cell
 * @deprecated Use {@link Function} directly instead
 * @since v2.8.10
 */
@Deprecated(forRemoval = true, since = "v2.8.10")
@FunctionalInterface
public interface ValueProvider<D, V> extends Function<D, V> {

    V getValue(D data);

    @Override
    default V apply(final D data) {
        return getValue(data);
    }

}
