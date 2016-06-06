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

package com.ponysdk.core.ui.list.renderer.cell;


import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PLabel;

public abstract class AbstractCellRenderer<V, W extends IsPWidget> implements CellRenderer<V, W> {

    private static String DASH = "-";

    protected String nullDisplay = DASH;

    @SuppressWarnings("unchecked")
    @Override
    public final W render(final int row, final V value) {
        if (value == null) return (W) new PLabel(nullDisplay);
        return render0(row, value);
    }

    public abstract W render0(int row, V value);

    public void setNullDisplay(final String nullDisPlay) {
        this.nullDisplay = nullDisPlay;
    }

}
