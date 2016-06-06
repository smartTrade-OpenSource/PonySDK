
package com.ponysdk.core.ui.renderer;

import com.ponysdk.core.ui.basic.IsPWidget;

public interface Renderer<V> {

    IsPWidget render(V value);

    void update(final V value);
}
