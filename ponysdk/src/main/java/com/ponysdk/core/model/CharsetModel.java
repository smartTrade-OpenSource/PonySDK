/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.model;

public enum CharsetModel {

    ASCII(CharsetModel.ASCII_TYPE),
    UTF8(CharsetModel.UTF8_TYPE);

    public static final int ASCII_TYPE = 0;
    public static final int UTF8_TYPE = 1;

    private static final CharsetModel[] VALUES = CharsetModel.values();

    private final int size;

    private CharsetModel(final int size) {
        this.size = size;
    }

    public final int getSize() {
        return size;
    }

    public final byte getValue() {
        return (byte) ordinal();
    }

    public static CharsetModel fromRawValue(final byte rawValue) {
        return VALUES[rawValue];
    }

}
