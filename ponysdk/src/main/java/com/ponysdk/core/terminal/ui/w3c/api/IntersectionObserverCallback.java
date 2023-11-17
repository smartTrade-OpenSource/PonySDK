/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.terminal.ui.w3c.api;

import jsinterop.annotations.JsFunction;

/**
 * A function which is called when the percentage of the target element is visible crosses a threshold.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/IntersectionObserver">MDN</a>
 * @see <a href=
 * "https://github.com/gwt-jelement/gwt-jelement/blob/master/src/main/java/gwt/jelement/dom/IntersectionObserverCallback.java">Source</a>
 */
@JsFunction
public interface IntersectionObserverCallback {

    /**
     * @param entries  A list of {@link IntersectionObserverEntry} objects, each representing one threshold which was
     *                 crossed,
     *                 either becoming more or less visible than the percentage specified by that threshold.
     * @param observer The {@link IntersectionObserver} for which the callback is being invoked.
     */
    void callback(IntersectionObserverEntry[] entries, IntersectionObserver observer);

}
