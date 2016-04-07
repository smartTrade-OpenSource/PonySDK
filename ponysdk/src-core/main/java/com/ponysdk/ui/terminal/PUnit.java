
package com.ponysdk.ui.terminal;

import com.google.gwt.dom.client.Style.Unit;

public enum PUnit {

    PX("px"),
    PCT("%"),
    EM("em"),
    EX("ex"),
    PT("pt"),
    PC("pc"),
    IN("in"),
    CM("cm"),
    MM("mm");

    private final String value;

    PUnit(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public byte getByteValue() {
        return (byte) ordinal();
    }

    public static final Unit getUnit(final PUnit u) {
        switch (u) {
            case PX:
                return Unit.PX;
            case EM:
                return Unit.EM;
            case PCT:
                return Unit.PCT;
            case CM:
                return Unit.CM;
            case EX:
                return Unit.EX;
            case IN:
                return Unit.IN;
            case MM:
                return Unit.MM;
            case PC:
                return Unit.PC;
            case PT:
                return Unit.PT;
        }
        return null;
    }

}