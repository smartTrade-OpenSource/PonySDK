/*
 * Copyright (c) 2026 PonySDK
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class LiveDataViewTest {

    @Test
    public void liveDataViewCopiesRows() {
        final List<DefaultRow<String>> rows = new ArrayList<>(List.of(new DefaultRow<>(0, "a")));
        final LiveDataView<String> view = new LiveDataView<>(10, rows);
        rows.clear();

        Assertions.assertEquals(10, view.getAbsoluteRowCount());
        Assertions.assertEquals(1, view.getLiveData().size());
        Assertions.assertEquals("LiveDataView [absoluteRowCount=10, liveData.size()=1]", view.toString());
    }

    @Test
    public void intervalKeepsBounds() {
        final Interval interval = new Interval(2, 5);

        Assertions.assertEquals(2, interval.from);
        Assertions.assertEquals(5, interval.to);
    }
}
