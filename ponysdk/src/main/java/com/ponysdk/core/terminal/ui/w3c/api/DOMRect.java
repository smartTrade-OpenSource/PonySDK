package com.ponysdk.core.terminal.ui.w3c.api;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * JsInterop wrapper for the native DOMRect / DOMRectReadOnly interface.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "DOMRect")
public class DOMRect {

    @JsProperty
    public native double getX();

    @JsProperty
    public native double getY();

    @JsProperty
    public native double getWidth();

    @JsProperty
    public native double getHeight();

    @JsProperty
    public native double getTop();

    @JsProperty
    public native double getRight();

    @JsProperty
    public native double getBottom();

    @JsProperty
    public native double getLeft();
}
