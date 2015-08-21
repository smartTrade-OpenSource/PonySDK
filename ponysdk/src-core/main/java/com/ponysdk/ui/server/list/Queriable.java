
package com.ponysdk.ui.server.list;

public interface Queriable {

    public Sortable asSortable();

    public HasCriteria asHasCriteria();

    public Resetable asResetable();

    public Validable asValidable();
}
