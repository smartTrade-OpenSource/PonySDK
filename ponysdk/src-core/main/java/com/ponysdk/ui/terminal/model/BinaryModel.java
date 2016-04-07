/*============================================================================
 *
 * Copyright (c) 2000-2015 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/
package com.ponysdk.ui.terminal.model;

import com.google.gwt.json.client.JSONObject;

/**
 * @author nvelin
 */
public class BinaryModel {

    public static final BinaryModel NULL = new BinaryModel(null, 0);

    private final Model model;
    private final int size;

    private boolean booleanValue;
    private byte byteValue;
    private short shortValue;
    private int intValue;
    private long longValue;
    private double doubleValue;
    private String stringValue;
    private JSONObject jsonObject;

    public BinaryModel(final Model key, final boolean booleanValue, final int size) {
        this(key, size);
        this.booleanValue = booleanValue;
    }

    public BinaryModel(final Model key, final byte byteValue, final int size) {
        this(key, size);
        this.byteValue = byteValue;
    }

    public BinaryModel(final Model key, final short shortValue, final int size) {
        this(key, size);
        this.shortValue = shortValue;
    }

    public BinaryModel(final Model key, final int intValue, final int size) {
        this(key, size);
        this.intValue = intValue;
    }

    public BinaryModel(final Model key, final long longValue, final int size) {
        this(key, size);
        this.longValue = longValue;
    }

    public BinaryModel(final Model key, final double doubleValue, final int size) {
        this(key, size);
        this.doubleValue = doubleValue;
    }

    public BinaryModel(final Model key, final String stringValue, final int size) {
        this(key, size);
        this.stringValue = stringValue;
    }

    public BinaryModel(final Model key, final JSONObject jsonObject, final int size) {
        this(key, size);
        this.jsonObject = jsonObject;
    }

    public BinaryModel(final Model key, final int size) {
        this.model = key;
        this.size = size;
    }

    public Model getModel() {
        return model;
    }

    public short getShortValue() {
        return shortValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }

    public byte getByteValue() {
        return byteValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        if (model == null) return null;
        switch (model.getTypeModel()) {
            case NULL:
                return String.valueOf(model);
            case BOOLEAN:
                return model + " => " + booleanValue;
            case BYTE:
                return model + " => " + byteValue;
            case SHORT:
                return model + " => " + shortValue;
            case INTEGER:
                return model + " => " + intValue;
            case LONG:
                return model + " => " + longValue;
            case DOUBLE:
                return model + " => " + doubleValue;
            case STRING:
                return model + " => " + stringValue;
            case JSON_OBJECT:
                return model + " => " + jsonObject;
            default:
                throw new IllegalArgumentException("No model type configured");
        }
    }

}
