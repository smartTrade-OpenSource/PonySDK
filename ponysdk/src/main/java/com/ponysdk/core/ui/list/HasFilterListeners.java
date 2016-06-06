
package com.ponysdk.core.ui.list;

import java.util.Collection;

public interface HasFilterListeners {

    void addFilterListener(FilterListener listener);

    Collection<FilterListener> getFilterListener();
}
