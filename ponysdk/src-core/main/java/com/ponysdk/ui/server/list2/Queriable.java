
package com.ponysdk.ui.server.list2;

public interface Queriable {

    public Sortable asSortable();

    public HasCriteria asHasCriteria();

    public Resetable asResetable();
}
