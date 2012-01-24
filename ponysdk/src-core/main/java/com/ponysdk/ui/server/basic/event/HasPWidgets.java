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

package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PWidget;

public interface HasPWidgets extends Iterable<PWidget> {

    /**
     * Adds a child widget.
     * 
     * @param w
     *            the widget to be added
     * @throws UnsupportedOperationException
     *             if this method is not supported (most often this means that a specific overload must be called)
     */
    void add(PWidget w);

    /**
     * Adds a child widget
     * 
     * @param w the widget to be added
     */
    void add(IsPWidget w);

    /**
     * Removes all child widgets.
     */
    void clear();

    /**
     * Removes a child widget.
     * 
     * @param w
     *            the widget to be removed
     * @return <code>true</code> if the widget was present
     */
    boolean remove(PWidget w);

}