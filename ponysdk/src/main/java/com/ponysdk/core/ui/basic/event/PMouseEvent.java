
package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.ui.eventbus.EventHandler;

public abstract class PMouseEvent<H extends EventHandler> extends PHumanInputEvent<H> {

    private int nativeButton;
    private int x;
    private int y;
    private int clientX;
    private int clientY;
    private int sourceAbsoluteLeft;
    private int sourceAbsoluteTop;
    private int sourceOffsetHeight;
    private int sourceOffsetWidth;

    public PMouseEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    public int getClientX() {
        return clientX;
    }

    public int getClientY() {
        return clientY;
    }

    public void setClientY(final int clientY) {
        this.clientY = clientY;
    }

    public void setClientX(final int clientX) {
        this.clientX = clientX;
    }

    public void setSourceAbsoluteLeft(final int sourceAbsoluteLeft) {
        this.sourceAbsoluteLeft = sourceAbsoluteLeft;
    }

    public int getSourceAbsoluteLeft() {
        return sourceAbsoluteLeft;
    }

    public void setSourceAbsoluteTop(final int sourceAbsoluteTop) {
        this.sourceAbsoluteTop = sourceAbsoluteTop;
    }

    public int getSourceAbsoluteTop() {
        return sourceAbsoluteTop;
    }

    public void setSourceOffsetHeight(final int sourceOffsetHeight) {
        this.sourceOffsetHeight = sourceOffsetHeight;
    }

    public int getSourceOffsetHeight() {
        return sourceOffsetHeight;
    }

    public void setSourceOffsetWidth(final int sourceOffsetWidth) {
        this.sourceOffsetWidth = sourceOffsetWidth;
    }

    public int getSourceOffsetWidth() {
        return sourceOffsetWidth;
    }

    public int getNativeButton() {
        return nativeButton;
    }

    public void setNativeButton(final int nativeButton) {
        this.nativeButton = nativeButton;
    }

    public int getX() {
        return x;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(final int y) {
        this.y = y;
    }

}