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

package com.ponysdk.core.ui.component;

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.PSimplePanel;

/**
 * A PWidget wrapper for PComponent instances.
 * <p>
 * This class allows PComponent instances to be used in contexts that require
 * PWidget or IsPWidget, such as adding them to panels like PSimplePanel.
 * </p>
 * <p>
 * The wrapper is simply a PSimplePanel that contains the component after
 * it has been attached to the window.
 * </p>
 */
public class PComponentWidget extends PSimplePanel {

    private final PComponent<?> component;

    /**
     * Creates a new PComponentWidget wrapping the specified component.
     *
     * @param component the component to wrap, must not be null
     * @throws NullPointerException if component is null
     */
    public PComponentWidget(final PComponent<?> component) {
        if (component == null) {
            throw new NullPointerException("component must not be null");
        }
        this.component = component;
        addStyleName("pcomponent-wrapper");
    }

    /**
     * Gets the wrapped component.
     *
     * @return the wrapped PComponent
     */
    public PComponent<?> getComponent() {
        return component;
    }
}
