
package com.ponysdk.core.terminal.basic;

import com.google.gwt.layout.client.Layout.Alignment;

/**
 * Used to specify the alignment of child elements within a layer.
 */
public enum PTAlignment {

    /**
     * Positions an element at the beginning of a given axis.
     */
    BEGIN,

    /**
     * Positions an element at the beginning of a given axis.
     */
    END,

    /**
     * Stretches an element to fill the layer on a given axis.
     */
    STRETCH;

    public byte getValue() {
        return (byte) ordinal();
    }

    public static Alignment getAlignement(final PTAlignment alignment) {
        switch (alignment) {
            case BEGIN:
                return Alignment.BEGIN;
            case END:
                return Alignment.END;
            case STRETCH:
                return Alignment.STRETCH;
        }
        return null;
    }
}