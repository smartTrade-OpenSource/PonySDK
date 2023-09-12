/*
 * Copyright (c) 2021 PonySDK
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

package com.ponysdk.core.ui.infinitescroll;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * @author mzoughagh
 * @param <D> Data
 * @param <W> Widget
 */

public interface InfiniteScrollProvider<D, W> {

    /**
     * Get data from provider list
     *
     * @param beginIndex
     *            the beginning index to recuperate data from provider list
     * @param maxSize
     *            the last index to get data from provider list
     * @param callback
     *            return the list in the callback
     */
    void getData(int beginIndex, int maxSize, Consumer<List<D>> callback);

    /**
     * Get full size
     *
     * @param callback
     *            return the size in the callback
     */
    void getFullSize(IntConsumer callback);

    /**
     * Updating widgets and assigning data to them
     *
     * @param index
     *            the index of the widget
     * @param data
     *            the assigned data to the widget
     * @param w
     *            the widget
     * @return the updated widget
     */
    W handleUI(int index, D data, W w);

    /**
     * add callback to call when data change
     *
     * @param handler to add
     */
    void addHandler(Runnable handler);

}
