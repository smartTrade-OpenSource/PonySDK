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

package com.ponysdk.core.ui.list.renderer.cell;

import javax.validation.constraints.NotNull;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.list.refreshable.Cell;

public class HtmlCellRenderer<D> implements CellRenderer<D, PHTML> {

    private static String DASH = "-";

    protected String nullDisplay = DASH;

    public HtmlCellRenderer() {
    }

    public HtmlCellRenderer(final String nullDisplay) {
        this.nullDisplay = nullDisplay;
    }

    @Override
    public PHTML render(final int row, final D rawValue) {
        final String value = rawValue != null ? getValue(rawValue) : null;
        return Element.newPHTML(value != null ? value : nullDisplay);
    }

    protected String getValue(@NotNull final D value) {
        return value.toString();
    }

    @Override
    public final void update(final D value, final Cell<D, PHTML> previous) {
        previous.getWidget().setText(getValue(value));
    }

    public void setNullDisplay(final String nullDisPlay) {
        this.nullDisplay = nullDisPlay;
    }

}
