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

/**
 * @author mzoughagh
 * @param <D> type of data
 * @param <W> type of widget
 */

public interface InfiniteScrollProvider<D, W> {

    /**
     * Recuperes data from provider list
     *
     * @param beginIndex the beginning index to recuperate data from provider list
     * @param maxSize the last index to recuperate data from provider list
     * @return data by taking into consideration beginIndex and maxSize
     */
    List<D> getData(int beginIndex, int maxSize);

    /**
     * @return current length of provider list
     */
    int getFullSize();

    /**
     * Uptades widgets and assigning data to them
     *
     * @param index the index of the widget
     * @param data the assigned data to the widget
     * @param w the widget
     * @return the widget after updating
     */
    W handleUI(int index, D data, W w);

    /**
     * Adding handler to widgets
     *
     * @param handler to add
     */
    void addHandler(Consumer<D> handler);

}
