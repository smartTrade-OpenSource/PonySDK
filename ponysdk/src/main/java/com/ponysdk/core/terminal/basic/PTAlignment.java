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

package com.ponysdk.core.terminal.basic;

import com.google.gwt.layout.client.Layout.Alignment;

/**
 * Used to specify the alignment of child elements within a layer.
 */
public enum PTAlignment {

    /**
     * Positions an element at the beginning of a given axis.
     */
    BEGIN,

    /**
     * Positions an element at the beginning of a given axis.
     */
    END,

    /**
     * Stretches an element to fill the layer on a given axis.
     */
    STRETCH;

    public byte getValue() {
        return (byte) ordinal();
    }

    public static Alignment getAlignement(final PTAlignment alignment) {
        switch (alignment) {
            case BEGIN:
                return Alignment.BEGIN;
            case END:
                return Alignment.END;
            case STRETCH:
                return Alignment.STRETCH;
        }
        return null;
    }
}