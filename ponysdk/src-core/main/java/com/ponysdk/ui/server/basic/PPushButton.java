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

package com.ponysdk.ui.server.basic;

import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A normal push button with custom styling.
 * <p>
 * <img class='gallery' src='doc-files/PPushButton.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.gwt-PushButton-up/down/up-hovering/down-hovering/up-disabled/down-disabled {.html-face}</li>
 * </ul>
 */
public class PPushButton extends PButton {

    public PPushButton(final PImage image) {
        super();
        create.put(PROPERTY.IMAGE, image.getID());
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.PUSH_BUTTON;
    }
}
