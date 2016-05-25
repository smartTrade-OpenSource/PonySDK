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

package com.ponysdk.ui.terminal.model;

public enum ValueTypeModel {

    NULL((byte) 0),
    BOOLEAN((byte) 1),
    BYTE((byte) 1),
    SHORT((byte) 2),
    INTEGER((byte) 4),
    LONG((byte) 8), // FIXME
    DOUBLE((byte) 16), // FIXME
    STRING((byte) -1),
    JSON_OBJECT((byte) -1);

    private int size;

    ValueTypeModel(final byte size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

}