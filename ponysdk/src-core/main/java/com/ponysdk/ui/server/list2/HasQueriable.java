
package com.ponysdk.ui.server.list2;

import java.util.Collection;

public interface HasQueriable {

    public void addQueriable(Queriable queriable);

    public Collection<Queriable> getQueriable();
}
