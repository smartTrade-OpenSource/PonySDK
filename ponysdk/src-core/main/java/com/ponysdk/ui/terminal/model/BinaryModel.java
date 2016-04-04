/*============================================================================
 *
 * Copyright (c) 2000-2015 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/
package com.ponysdk.ui.terminal.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.ponysdk.ui.terminal.PonySDK;

import elemental.html.ArrayBuffer;

/**
 * @author nvelin
 */
public class BinaryModel {

    private final static Logger log = Logger.getLogger(BinaryModel.class.getName());

    public static final byte TRUE = 1;
    public static final byte FALSE = 0;

    private final Model model;
    private final JSONValue value;
    private final int position;

    public BinaryModel(Model model, JSONValue value, int position) {
        this.model = model;
        this.value = value;
        this.position = position;

        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Message : " + toString());
    }

    public Model getModel() {
        return model;
    }

    public JSONValue getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return model + " => " + value;
    }

    /**
     * @param message
     * @param position
     * @return
     */
    public static BinaryModel getObject(ArrayBuffer message, int position) {
        try {
            final Model model = Model.getModel(PonySDK.getShort(message, position));
            position += TypeModel.SHORT_SIZE.getSize();

            JSONValue value;

            switch (model.getTypeModel()) {
                case NULL_SIZE:
                    value = JSONNull.getInstance();
                    position += model.getTypeModel().getSize();
                    break;
                case BOOLEAN_SIZE:
                    value = JSONBoolean.getInstance(PonySDK.getBoolean(message, position));
                    position += model.getTypeModel().getSize();
                    break;
                case BYTE_SIZE:
                    value = new JSONNumber(PonySDK.getByte(message, position));
                    position += model.getTypeModel().getSize();
                    break;
                case SHORT_SIZE:
                    value = new JSONNumber(PonySDK.getShort(message, position));
                    position += model.getTypeModel().getSize();
                    break;
                case INTEGER_SIZE:
                    value = new JSONNumber(PonySDK.getInteger(message, position));
                    position += model.getTypeModel().getSize();
                    break;
                case LONG_SIZE:
                    value = new JSONNumber(PonySDK.getInteger(message, position));
                    position += model.getTypeModel().getSize();
                    break;
                case VARIABLE_SIZE:
                    final int size = PonySDK.getShort(message, position);
                    position += TypeModel.SHORT_SIZE.getSize();
                    value = new JSONString(PonySDK.getString(message, position, position + size));
                    position += size;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown type model : " + model.getTypeModel());
            }

            return new BinaryModel(model, value, position);
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Cannot parse " + PonySDK.getString(message, position), e);
            throw e;
        }
    }

}
