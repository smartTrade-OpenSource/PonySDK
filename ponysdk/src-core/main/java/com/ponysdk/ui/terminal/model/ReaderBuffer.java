package com.ponysdk.ui.terminal.model;

import elemental.client.Browser;
import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.html.Window;

public class ReaderBuffer {

    public static final byte TRUE = 1;
    public static final byte FALSE = 0;

    private final ArrayBuffer message;
    private int position;

    public ReaderBuffer(final ArrayBuffer message) {
        this.message = message;
    }

    public int getIndex() {
        return position;
    }

    public int getByteLength() {
        return message.getByteLength();
    }

    public BinaryModel getBinaryModel() {
        try {
            final Model key = Model.values()[getByte()];
            int size = TypeModel.BYTE_SIZE.getSize();

            switch (key.getTypeModel()) {
                case NULL_SIZE:
                    size += key.getTypeModel().getSize();
                    return new BinaryModel(key, size);
                case BOOLEAN_SIZE:
                    size += key.getTypeModel().getSize();
                    return new BinaryModel(key, getBoolean(), size);
                case BYTE_SIZE:
                    size += key.getTypeModel().getSize();
                    return new BinaryModel(key, getByte(), size);
                case SHORT_SIZE:
                    size += key.getTypeModel().getSize();
                    return new BinaryModel(key, getShort(), size);
                case INTEGER_SIZE:
                    size += key.getTypeModel().getSize();
                    return new BinaryModel(key, getInt(), size);
                case LONG_SIZE:
                    size += key.getTypeModel().getSize();
                    return new BinaryModel(key, getLong(), size);
                case DOUBLE_SIZE:
                    size += key.getTypeModel().getSize();
                    return new BinaryModel(key, getDouble(), size);
                case VARIABLE_SIZE:
                    size += TypeModel.SHORT_SIZE.getSize();
                    final int messageSize = getShort();
                    size += key.getTypeModel().getSize();
                    return new BinaryModel(key, getString(messageSize), size);
                default:
                    throw new IllegalArgumentException("Unknown type model : " + key.getTypeModel());
            }
        } catch (final Exception e) {
            // log.log(Level.SEVERE, "Cannot parse " + getString(), e);
            throw e;
        }
    }

    private boolean getBoolean() {
        final Window window = Browser.getWindow();
        final int size = TypeModel.BOOLEAN_SIZE.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;
        return arrayType.intAt(0) == TRUE;
    }

    private byte getByte() {
        final Window window = Browser.getWindow();
        final int size = TypeModel.BYTE_SIZE.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;
        return (byte) arrayType.intAt(0);
    }

    private short getShort() {
        final Window window = Browser.getWindow();
        final int size = TypeModel.SHORT_SIZE.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;

        int result = 0;
        for (int i = 0; i < arrayType.length(); i++) {
            result = (result << 8) + arrayType.intAt(i);
        }

        return (short) result;
    }

    private int getInt() {
        final Window window = Browser.getWindow();
        final int size = TypeModel.INTEGER_SIZE.getSize();
        final Uint8Array arrayType = window.newUint8Array(message, position, size);
        position += size;

        int result = 0;
        for (int i = 0; i < arrayType.length(); i++) {
            result = (result << 8) + arrayType.intAt(i);
        }

        return result;
    }

    // FIXME
    private long getLong() {
        return getInt();
    }

    // FIXME
    private double getDouble() {
        return getInt();
    }

    private String getString() {
        final String result = fromCharCode(message.slice(position));
        position = message.getByteLength();
        return result;
    }

    private String getString(final int end) {
        if (end != 0) {
            final String result = fromCharCode(message.slice(position, position + end));
            position += end;
            return result;
        } else {
            return null;
        }
    }

    private static native String fromCharCode(ArrayBuffer buf) /*-{return new TextDecoder().decode(buf);}-*/;

    public void rewind(final BinaryModel binaryModel) {
        position -= binaryModel.getSize();
    }

}
