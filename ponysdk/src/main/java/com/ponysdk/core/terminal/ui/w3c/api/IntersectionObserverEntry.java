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

import com.google.gwt.dom.client.Element;

import elemental.js.html.JsClientRect;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * The IntersectionObserverEntry interface of the Intersection Observer API describes the intersection between the
 * target element and its root container at a specific moment of transition.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserverEntry">MDN</a>
 * @see <a href=
 *      "https://github.com/gwt-jelement/gwt-jelement/blob/master/src/main/java/gwt/jelement/dom/IntersectionObserverEntry.java">Source</a>
 */
@JsType(namespace = JsPackage.GLOBAL, isNative = true)
public class IntersectionObserverEntry {

    /**
     * A DOMHighResTimeStamp indicating the time at which the intersection was recorded, relative to the
     * IntersectionObserver's time origin.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserverEntry/time">MDN</a>
     */
    @JsProperty
    public native double getTime();

    /**
     * Returns a DOMRectReadOnly for the intersection observer's root.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserverEntry/rootBounds">MDN</a>
     */
    @JsProperty
    public native JsClientRect getRootBounds();

    /**
     * Returns the bounds rectangle of the target element as a DOMRectReadOnly.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserverEntry/boundingClientRect">MDN</a>
     */
    @JsProperty
    public native JsClientRect getBoundingClientRect();

    /**
     * Returns a DOMRectReadOnly representing the target's visible area.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserverEntry/intersectionRect">MDN</a>
     */
    @JsProperty
    public native JsClientRect getIntersectionRect();

    /**
     * A Boolean value which is true if the target element intersects with the intersection observer's root.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserverEntry/isIntersecting">MDN</a>
     */
    @JsProperty
    public native boolean isIsIntersecting();

    /**
     * Returns the ratio of the intersectionRect to the boundingClientRect.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserverEntry/intersectionRatio">MDN</a>
     */
    @JsProperty
    public native double getIntersectionRatio();

    /**
     * The {@link Element} whose intersection with the root changed.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserverEntry/target">MDN</a>
     */
    @JsProperty
    public native Element getTarget();

}
