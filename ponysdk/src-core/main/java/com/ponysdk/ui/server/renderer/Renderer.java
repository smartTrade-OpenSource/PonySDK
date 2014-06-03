
package com.ponysdk.ui.server.renderer;

import com.ponysdk.ui.server.basic.IsPWidget;

public interface Renderer<V> {

    public IsPWidget render(V value);

    public void update(final V value);
}
