/*
 * Copyright (c) 2020 PonySDK
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

package com.ponysdk.core.ui.datagrid2.data;

import java.util.List;

/**
 *
 */
public class DataSrcResult<T> {

    public int firstRowIndex;
    public int absoluteRowCount;
    public int size;
    public int from;
    public int to;
    public int start;
    public List<SimpleRow<T>> liveData;

    public DataSrcResult(final int fri, final int arc, final int size, final int from, final int to, final int start,
            final List<SimpleRow<T>> liveData) {
        this.firstRowIndex = fri;
        this.absoluteRowCount = arc;
        this.size = size;
        this.from = from;
        this.to = to;
        this.liveData = liveData;
    }
}
