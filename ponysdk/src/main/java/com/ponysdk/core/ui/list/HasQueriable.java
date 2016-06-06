
package com.ponysdk.core.ui.list;

import java.util.Collection;

public interface HasQueriable {

    void addQueriable(Queriable queriable);

    Collection<Queriable> getQueriable();
}
