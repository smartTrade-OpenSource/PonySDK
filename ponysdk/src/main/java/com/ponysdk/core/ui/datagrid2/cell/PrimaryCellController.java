/*============================================================================
 *
 * Copyright (c) 2000-2022 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.core.ui.datagrid2.cell;

public interface PrimaryCellController<V> extends CellController<V> {

    /**
     * Switches to an extended mode that can be used to have a richer cell not
     * constrained by the dimensions of the original cell. The
     * {@link ExtendedCell} can be used, for example, for the edit mode or for
     * having a more detailed view.
     *
     * @see ExtendedCellController#setPrimaryMode()
     */
    void setExtendedMode();
}
