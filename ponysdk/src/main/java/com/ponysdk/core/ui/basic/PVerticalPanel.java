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

package com.ponysdk.core.ui.basic;

import java.util.Objects;

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;

/**
 * A panel that lays all of its widgets out in a single vertical column.
 */
public class PVerticalPanel extends PCellPanel implements HasPAlignment {

    private PHorizontalAlignment horizontalAlignment = PHorizontalAlignment.ALIGN_LEFT;
    private PVerticalAlignment verticalAlignment = PVerticalAlignment.ALIGN_TOP;

    protected PVerticalPanel() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.VERTICAL_PANEL;
    }

    public PHorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    @Override
    public void setHorizontalAlignment(final PHorizontalAlignment align) {
        if (Objects.equals(this.horizontalAlignment, align)) return;
        this.horizontalAlignment = align;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.HORIZONTAL_ALIGNMENT, align.getValue()));
    }

    public PVerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    @Override
    public void setVerticalAlignment(final PVerticalAlignment align) {
        if (Objects.equals(this.verticalAlignment, align)) return;
        this.verticalAlignment = align;
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.VERTICAL_ALIGNMENT, align.getValue()));
    }

}
