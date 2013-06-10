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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.tools.Objects;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

/**
 * A panel that lays all of its widgets out in a single vertical column.
 */
public class PVerticalPanel extends PCellPanel implements HasPAlignment {

    private PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.ALIGN_LEFT;
    private PVerticalAlignment verticalAlignment = PVerticalAlignment.ALIGN_TOP;

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.VERTICAL_PANEL;
    }

    @Override
    public void setHorizontalAlignment(final PHorizontalAlignment horizontalAlignment) {
        if (Objects.equals(this.horizontalAlignment, horizontalAlignment)) return;
        this.horizontalAlignment = horizontalAlignment;
        saveUpdate(PROPERTY.HORIZONTAL_ALIGNMENT, this.horizontalAlignment.ordinal());
    }

    @Override
    public void setVerticalAlignment(final PVerticalAlignment verticalAlignment) {
        if (Objects.equals(this.verticalAlignment, verticalAlignment)) return;
        this.verticalAlignment = verticalAlignment;
        saveUpdate(PROPERTY.VERTICAL_ALIGNMENT, this.verticalAlignment.ordinal());
    }

    public PHorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public PVerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

}
