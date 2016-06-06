
package com.ponysdk.core.ui.renderer;

@FunctionalInterface
public interface RenderFactory<T> {

    Renderer<T> newRender();
}
