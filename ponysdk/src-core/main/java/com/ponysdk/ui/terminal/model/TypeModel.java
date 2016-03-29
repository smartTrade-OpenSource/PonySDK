/*============================================================================
 *
 * Copyright (c) 2000-2015 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/
package com.ponysdk.ui.terminal.model;

public enum TypeModel {

    NULL_SIZE(0),
    BOOLEAN_SIZE(1),
    BYTE_SIZE(1),
    SHORT_SIZE(2),
    INTEGER_SIZE(4),
    LONG_SIZE(8),
    VARIABLE_SIZE(-1);

    private int size;

    TypeModel(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

}