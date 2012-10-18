/*============================================================================
 *
 * Copyright (c) 2000-2008 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.ui.server.list2;

import java.util.Collection;

public interface HasQueriable {

    public void addQueriable(Queriable queriable);

    public Collection<Queriable> getQueriable();
}
