/*============================================================================
 *
 * Copyright (c) 2000-2015 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/
package com.ponysdk.ui.server.model;

import com.ponysdk.ui.model.ServerToClientModel;

/**
 * @author nvelin
 */
public class ServerBinaryModel {

    private final ServerToClientModel key;
    private final Object value;

    public ServerBinaryModel(final ServerToClientModel key, final Object value) {
        this.key = key;
        this.value = value;
    }

    public ServerToClientModel getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

}
