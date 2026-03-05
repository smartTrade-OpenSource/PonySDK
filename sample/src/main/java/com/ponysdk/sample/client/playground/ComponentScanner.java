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

package com.ponysdk.sample.client.playground;

import java.util.List;

import com.ponysdk.core.ui.component.PWebComponent;

/**
 * Discovers Web Awesome components in the classpath.
 * <p>
 * Scans the {@code com.ponysdk.core.ui.wa} package for all classes extending
 * {@link PWebComponent} and returns them for use in the playground.
 * </p>
 */
public interface ComponentScanner {

    /**
     * Scans the classpath for Web Awesome component classes.
     *
     * @return a list of component classes extending PWebComponent, never null
     */
    List<Class<? extends PWebComponent<?>>> scanComponents();
}
