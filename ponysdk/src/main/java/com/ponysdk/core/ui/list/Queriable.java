
package com.ponysdk.core.ui.list;

public interface Queriable {

    Sortable asSortable();

    HasCriteria asHasCriteria();

    Resetable asResetable();

    Validable asValidable();
}
