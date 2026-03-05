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

package com.ponysdk.core.ui.wa;

/**
 * Size options shared across multiple Web Awesome components.
 * <p>
 * Each size serializes as its lowercase string value for
 * compatibility with Web Awesome component attributes
 * (e.g., {@code "small"}, {@code "medium"}, {@code "large"}).
 * </p>
 *
 * @see com.ponysdk.core.ui.component.PWebComponent
 */
public enum Size {

    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large");

    private final String value;

    Size(final String value) {
        this.value = value;
    }

    /**
     * Returns the lowercase string value for Web Awesome serialization.
     *
     * @return the size string (e.g. "small", "medium", "large")
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the lowercase string value, ensuring correct JSON serialization
     * via PropsDiffer (which uses toString() for enum values).
     *
     * @return the size string (e.g. "small", "medium", "large")
     */
    @Override
    public String toString() {
        return value;
    }

}
