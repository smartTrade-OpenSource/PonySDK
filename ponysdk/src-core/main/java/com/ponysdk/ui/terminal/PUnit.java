
package com.ponysdk.ui.terminal;

public enum PUnit {
    PX("px"), PCT("%"), EM("em"), EX("ex"), PT("pt"), PC("pc"), IN("in"), CM("cm"), MM("mm");

    private final String value;

    PUnit(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}