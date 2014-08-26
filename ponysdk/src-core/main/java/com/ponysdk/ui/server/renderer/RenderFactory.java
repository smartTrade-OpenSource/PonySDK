
package com.ponysdk.ui.server.renderer;

public interface RenderFactory<T> {

    public Renderer<T> newRender();
}
