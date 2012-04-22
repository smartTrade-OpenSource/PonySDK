
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.PEventHandler;

public abstract class PMouseEvent<H extends PEventHandler> extends PHumanInputEvent<H> {

    private int clientX;

    private int clientY;

    private int sourceAbsoluteLeft;

    private int sourceAbsoluteTop;

    private int sourceOffsetHeight;

    private int sourceOffsetWidth;

    public PMouseEvent(Object sourceComponent) {
        super(sourceComponent);
    }

    public int getClientX() {
        return clientX;
    }

    public int getClientY() {
        return clientY;
    }

    public void setClientY(int clientY) {
        this.clientY = clientY;
    }

    public void setClientX(int clientX) {
        this.clientX = clientX;
    }

    public void setSourceAbsoluteLeft(int sourceAbsoluteLeft) {
        this.sourceAbsoluteLeft = sourceAbsoluteLeft;
    }

    public int getSourceAbsoluteLeft() {
        return sourceAbsoluteLeft;
    }

    public void setSourceAbsoluteTop(int sourceAbsoluteTop) {
        this.sourceAbsoluteTop = sourceAbsoluteTop;
    }

    public int getSourceAbsoluteTop() {
        return sourceAbsoluteTop;
    }

    public void setSourceOffsetHeight(int sourceOffsetHeight) {
        this.sourceOffsetHeight = sourceOffsetHeight;
    }

    public int getSourceOffsetHeight() {
        return sourceOffsetHeight;
    }

    public void setSourceOffsetWidth(int sourceOffsetWidth) {
        this.sourceOffsetWidth = sourceOffsetWidth;
    }

    public int getSourceOffsetWidth() {
        return sourceOffsetWidth;
    }

}