package com.ponysdk.core.ui.datagrid2.cell;

import java.util.Optional;

import com.ponysdk.core.ui.basic.PWidget;

/**
 * {@link Cell} made visible by default and when {@link ExtendedCellController#setPrimaryMode()} is called.<br><br>
 * All {@link PrimaryCell} are expected to have the same immutable height for a given {@code DataGridView}.
 * 
 * @author pvbossche
 */
public interface PrimaryCell<V> extends Cell<V, PrimaryCellController<V>> {
	/**
     * Must always return the same instance and cannot be {@code null} or the
     * same as the main widget.
     *
     * @return a widget that will replace the main widget when the value for
     *         this cell is not available
     */
    PWidget asPendingWidget();
    
    /**
     * @return a fresh new instance of an {@link ExtendedCell} corresponding to this cell
     *  (or none if not possible; in that case, no {@link ExtendedCell} will be added to {@code DataGridView}).
     */
    default Optional<ExtendedCell<V>> genExtended() {
        return Optional.empty();
    }
}
