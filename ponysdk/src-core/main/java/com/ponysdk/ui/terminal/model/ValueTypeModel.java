/*============================================================================
 *
 * Copyright (c) 2000-2015 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/
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