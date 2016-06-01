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

package com.ponysdk.core.main;

import com.ponysdk.core.UIContext;

public interface EntryPoint {

    /**
     * Newly created session.
     *
     * @param uiContext
     */
    public void start(UIContext uiContext);

    /**
     * The HTTP session already exists, and a new UIContext has been created.
     * <h4>Cases :</h4>
     * <ul>
     * <li>Reload</li>
     * <li>New browser tabulation</li>
     * </ul>
     *
     * @param uiContext
     */
    public void restart(UIContext uiContext);

}