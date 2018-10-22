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

    NULL(ValueTypeModel.NULL_SIZE, false),
    BOOLEAN_FALSE(ArrayValueModel.BOOLEAN_SIZE, false),
    BOOLEAN_TRUE(ArrayValueModel.BOOLEAN_SIZE, false),
    BYTE(ValueTypeModel.BYTE_SIZE, false),
    SHORT(ValueTypeModel.SHORT_SIZE, false),
    INTEGER(ValueTypeModel.INTEGER_SIZE, false),
    LONG(ValueTypeModel.LONG_SIZE, false),
    DOUBLE(ValueTypeModel.DOUBLE_SIZE, false),
    FLOAT(ValueTypeModel.FLOAT_SIZE, false),

    STRING_ASCII_UINT8_LENGTH(ArrayValueModel.STRING_UINT8_LENGTH_MIN_SIZE, true),
    STRING_ASCII_UINT16_LENGTH(ArrayValueModel.STRING_UINT16_LENGTH_SIZE, true),

    STRING_UTF8_UINT8_LENGTH(ArrayValueModel.STRING_UINT8_LENGTH_MIN_SIZE, true),
    STRING_UTF8_UINT16_LENGTH(ArrayValueModel.STRING_UINT16_LENGTH_SIZE, true),
    STRING_UTF8_INT32_LENGTH(ArrayValueModel.STRING_INT32_LENGTH_MIN_SIZE, true);

    private static final int BOOLEAN_SIZE = 0;
    private static final int STRING_UINT8_LENGTH_MIN_SIZE = 1;
    private static final int STRING_UINT16_LENGTH_SIZE = 2;
    private static final int STRING_INT32_LENGTH_MIN_SIZE = 4;

    private final int minSize;
    private final boolean dynamicSize;

    private static final ArrayValueModel[] VALUES = ArrayValueModel.values();

    private ArrayValueModel(final int minSize, final boolean dynamicSize) {
        this.minSize = minSize;
        this.dynamicSize = dynamicSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public byte getValue() {
        return (byte) ordinal();
    }

    public boolean isDynamicSize() {
        return dynamicSize;
    }

    public static ArrayValueModel fromRawValue(final int rawValue) {
        return VALUES[rawValue];
    }

}
