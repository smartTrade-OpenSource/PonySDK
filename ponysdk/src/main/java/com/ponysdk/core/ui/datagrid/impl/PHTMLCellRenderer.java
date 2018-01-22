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

import java.util.function.Function;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PHTML;

public class PHTMLCellRenderer<DataType> extends TypedCellRenderer<DataType, PHTML> {

    private final Function<DataType, String> transform;

    public PHTMLCellRenderer() {
        this(String::valueOf);
    }

    public PHTMLCellRenderer(final Function<DataType, String> transform) {
        this.transform = transform;
    }

    @Override
    public PHTML render(final DataType value) {
        return Element.newPHTML(transform.apply(value));
    }

    @Override
    protected PHTML update0(final DataType value, final PHTML widget) {
        widget.setHTML(transform.apply(value));
        return widget;
    }

    @Override
    protected void reset0(final PHTML widget) {
        if (widget != null) widget.setHTML("");
    }

}
