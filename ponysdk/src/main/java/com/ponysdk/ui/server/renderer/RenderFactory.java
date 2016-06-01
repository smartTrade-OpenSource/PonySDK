
package com.ponysdk.ui.server.renderer;

@FunctionalInterface
public interface RenderFactory<T> {

    public Renderer<T> newRender();
}
