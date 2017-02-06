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

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.list.refreshable.Cell;
import com.ponysdk.core.ui.place.Place;
import com.ponysdk.core.ui.place.PlaceChangeRequest;

public class PlaceChangeCellRenderer implements CellRenderer<String, PWidget> {

    private static String DASH = "-";

    protected String nullDisplay = DASH;

    private final Place place;

    public PlaceChangeCellRenderer(final Place place) {
        this.place = place;
    }

    public PlaceChangeCellRenderer(final Place place, final String nullDisplay) {
        this(place);
        this.nullDisplay = nullDisplay;
    }

    @Override
    public PWidget render(final int row, final String rawValue) {
        final String value = getValue(rawValue);
        if (value != null) {
            final PAnchor anchor = Element.newPAnchor(rawValue);
            anchor.addClickHandler((event) -> PlaceChangeRequest.fire(this, place));
            return anchor;
        } else {
            return Element.newPLabel(nullDisplay);
        }
    }

    public String getValue(final String value) {
        return value;
    }

    @Override
    public final void update(final String value, final Cell<String, PWidget> previous) {
        // Need to change the type of the element
    }

    public void setNullDisplay(final String nullDisPlay) {
        this.nullDisplay = nullDisPlay;
    }

}
