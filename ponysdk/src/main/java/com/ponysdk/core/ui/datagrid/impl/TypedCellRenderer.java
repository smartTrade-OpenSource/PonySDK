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

package com.ponysdk.core.ui.datagrid.impl;

import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid.CellRenderer;

public abstract class TypedCellRenderer<DataType, WidgetType extends PWidget> implements CellRenderer<DataType> {

    @Override
    public PWidget update(final DataType value, final PWidget current) {
        return update0(value, cast(current));
    }

    private WidgetType cast(final PWidget w) {
        return (WidgetType) w;
    }

    @Override
    public void reset(final PWidget widget) {
        reset0(cast(widget));
    }

    @Override
    public abstract WidgetType render(final DataType value);

    protected abstract WidgetType update0(final DataType value, final WidgetType widget);

    protected abstract void reset0(final WidgetType widget);

}
