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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.writer.ModelWriter;

/**
 * A normal push button with custom styling.
 * <h2>CSS Style Rules</h2>
 * <ul class="css">
 * <li>.gwt-PushButton-up/down/up-hovering/down-hovering/up-disabled/down-
 * disabled {.html-face}</li>
 * </ul>
 */
public class PPushButton extends PButton {

    private final PImage image;

    protected PPushButton(final PImage image) {
        super();
        this.image = image;
    }

    @Override
    protected boolean attach(final PWindow window, final PFrame frame) {
        // WORKAROUND : element and sub element need to be created before any add
        final boolean imageResult = image.attach(window, frame);
        final boolean result = super.attach(window, frame);
        if (imageResult) image.saveAdd(image.getID(), ID);
        return result;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.WIDGET_ID, image.getID());
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.PUSH_BUTTON;
    }
}
