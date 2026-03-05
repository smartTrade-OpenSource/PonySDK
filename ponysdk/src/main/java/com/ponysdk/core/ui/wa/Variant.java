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
 * Visual variants shared across multiple Web Awesome components.
 * <p>
 * Each variant serializes as its lowercase string value for
 * compatibility with Web Awesome component attributes
 * (e.g., {@code "primary"}, {@code "danger"}).
 * </p>
 *
 * @see com.ponysdk.core.ui.component.PWebComponent
 */
public enum Variant {

    PRIMARY("primary"),
    SUCCESS("success"),
    NEUTRAL("neutral"),
    WARNING("warning"),
    DANGER("danger");

    private final String value;

    Variant(final String value) {
        this.value = value;
    }

    /**
     * Returns the lowercase string value for Web Awesome serialization.
     *
     * @return the variant string (e.g. "primary", "success")
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the lowercase string value, ensuring correct JSON serialization
     * via PropsDiffer (which uses toString() for enum values).
     *
     * @return the variant string (e.g. "primary", "success")
     */
    @Override
    public String toString() {
        return value;
    }

}
