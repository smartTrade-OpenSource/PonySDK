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

public enum ArrayValueModel {
    NULL(ArrayValueModel.NULL_SIZE),
    BOOLEAN_FALSE(ArrayValueModel.BOOLEAN_SIZE),
    BOOLEAN_TRUE(ArrayValueModel.BOOLEAN_SIZE),
    BYTE(ArrayValueModel.BYTE_SIZE),
    SHORT(ArrayValueModel.SHORT_SIZE),
    INTEGER(ArrayValueModel.INTEGER_SIZE),
    LONG(ArrayValueModel.LONG_SIZE),
    DOUBLE(ArrayValueModel.DOUBLE_SIZE),
    FLOAT(ArrayValueModel.FLOAT_SIZE),
    STRING_ASCII(ArrayValueModel.STRING_MIN_SIZE),
    STRING_UTF8(ArrayValueModel.STRING_MIN_SIZE);

    private static final int NULL_SIZE = 0;
    private static final int BOOLEAN_SIZE = 0;
    private static final int BYTE_SIZE = 1;
    private static final int SHORT_SIZE = 2;
    private static final int INTEGER_SIZE = 4;
    private static final int LONG_SIZE = 8;
    private static final int DOUBLE_SIZE = 8;
    private static final int FLOAT_SIZE = 4;
    private static final int STRING_MIN_SIZE = 2;

    private final int minSize;

    private static final ArrayValueModel[] VALUES = ArrayValueModel.values();

    private ArrayValueModel(final int minSize) {
        this.minSize = minSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public byte getValue() {
        return (byte) ordinal();
    }

    public static ArrayValueModel fromRawValue(final byte rawValue) {
        return VALUES[rawValue];
    }

}
