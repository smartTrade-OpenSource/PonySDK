
package com.ponysdk.ui.server.list;

import java.util.Collection;

public interface HasFilterListeners {

    public void addFilterListener(FilterListener listener);

    public Collection<FilterListener> getFilterListener();
}
