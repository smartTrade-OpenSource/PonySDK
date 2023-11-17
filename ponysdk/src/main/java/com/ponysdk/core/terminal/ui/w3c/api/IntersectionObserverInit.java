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
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * An optional object which customizes the observer.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/IntersectionObserver">MDN</a>
 * @see <a href=
 * "https://github.com/gwt-jelement/gwt-jelement/blob/master/src/main/java/gwt/jelement/dom/IntersectionObserverInit.java">Source</a>
 */
@JsType(name = "Object", namespace = JsPackage.GLOBAL, isNative = true)
public class IntersectionObserverInit {

    /**
     * An Element object which is an ancestor of the intended target, whose bounding rectangle will be considered the
     * viewport.
     */
    @JsProperty
    private Element root;

    /**
     * A string which specifies a set of offsets to add to the root's bounding_box when calculating intersections,
     * effectively shrinking or growing the root for calculation purposes.
     */
    @JsProperty
    private String rootMargin;

    /**
     * Either a single number or an array of numbers between 0.0 and 1.0, specifying a ratio of intersection area to
     * total bounding box area for the observed target.
     */
    @JsProperty
    private double[] threshold;

    public IntersectionObserverInit() {
    }

    @JsOverlay
    public final Element getRoot() {
        return this.root;
    }

    @JsOverlay
    public final void setRoot(final Element root) {
        this.root = root;
    }

    @JsOverlay
    public final String getRootMargin() {
        return this.rootMargin;
    }

    @JsOverlay
    public final void setRootMargin(final String rootMargin) {
        this.rootMargin = rootMargin;
    }

    @JsOverlay
    public final double[] getThreshold() {
        return this.threshold;
    }

    @JsOverlay
    public final void setThreshold(final double[] threshold) {
        this.threshold = threshold;
    }

}
