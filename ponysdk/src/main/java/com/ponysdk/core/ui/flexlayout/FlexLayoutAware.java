package com.ponysdk.core.ui.flexlayout;

/**
 * Interface for widgets that provide their own metadata for FlexLayout integration.
 * <p>
 * Widgets implementing this interface can be added to a FlexLayout without
 * explicitly specifying title or icon - the layout will query the widget directly.
 *
 * <pre>{@code
 * public class SearchPanel extends PFlowPanel implements FlexLayoutAware {
 *     @Override
 *     public String getFlexTitle() { return "Search"; }
 *
 *     @Override
 *     public String getFlexIcon() { return "search"; }
 * }
 *
 * // Then simply:
 * flexLayout.addTab("search", new SearchPanel());
 * }</pre>
 */
public interface FlexLayoutAware {

    /**
     * Returns the display title for this widget in FlexLayout tabs/sidebars.
     *
     * @return the title, must not be null
     */
    String getFlexTitle();

    /**
     * Returns the icon key for this widget in FlexLayout tabs/sidebars.
     * <p>
     * The icon key is interpreted according to the icon mode configured on the layout
     * (text or attribute-based for Design System integration).
     *
     * @return the icon key, or null if no icon should be displayed
     */
    default String getFlexIcon() {
        return null;
    }
}
