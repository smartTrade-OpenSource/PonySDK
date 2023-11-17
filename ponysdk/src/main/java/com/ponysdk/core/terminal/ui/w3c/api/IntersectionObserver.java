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
import jsinterop.annotations.*;

/**
 * The IntersectionObserver interface of the Intersection Observer API provides a way to asynchronously observe changes
 * in the intersection of a target element with an ancestor element or with a top-level document's viewport.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver">MDN</a>
 * @see <a href=
 * "https://github.com/gwt-jelement/gwt-jelement/blob/master/src/main/java/gwt/jelement/dom/IntersectionObserver.java">Source</a>
 */
@JsType(namespace = JsPackage.GLOBAL, name = "IntersectionObserver", isNative = true)
public class IntersectionObserver {

    /**
     * The IntersectionObserver() constructor creates and returns a new IntersectionObserver object.
     *
     * @param callback A function which is called when the percentage of the target element is visible crosses a
     *                 threshold.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/IntersectionObserver">MDN</a>
     */
    @JsConstructor
    public IntersectionObserver(final IntersectionObserverCallback callback) {
    }

    /**
     * The IntersectionObserver() constructor creates and returns a new IntersectionObserver object.
     *
     * @param callback A function which is called when the percentage of the target element is visible crosses a
     *                 threshold.
     * @param options  An optional object which customizes the observer.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/IntersectionObserver">MDN</a>
     */
    @JsConstructor
    public IntersectionObserver(final IntersectionObserverCallback callback, final IntersectionObserverInit options) {
    }

    /**
     * A specific ancestor of the target element being observed.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/root">MDN</a>
     */
    @JsProperty
    public native com.google.gwt.dom.client.Element getRoot();

    /**
     * An offset rectangle applied to the root's bounding box when calculating intersections, effectively shrinking or
     * growing the root for calculation purposes.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/rootMargin">MDN</a>
     */
    @JsProperty
    public native String getRootMargin();

    /**
     * A list of thresholds, sorted in increasing numeric order, where each threshold is a ratio of intersection area to
     * bounding box area of an observed target.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/thresholds">MDN</a>
     */
    @JsProperty
    public native double[] getThresholds();

    /**
     * Stops the IntersectionObserver object from observing any target.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/thresholds">MDN</a>
     */
    @JsMethod
    public native void disconnect();

    /**
     * Tells the IntersectionObserver a target element to observe.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/observe">MDN</a>
     */
    @JsMethod
    public native void observe(Element targetElement);

    /**
     * Returns an array of {@link IntersectionObserverEntry} objects for all observed targets.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/takeRecords">MDN</a>
     */
    @JsMethod
    public native IntersectionObserverEntry[] takeRecords();

    /**
     * Tells the IntersectionObserver to stop observing a particular target element.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/IntersectionObserver/unobserve">MDN</a>
     */
    @JsMethod
    public native void unobserve(Element target);

}
