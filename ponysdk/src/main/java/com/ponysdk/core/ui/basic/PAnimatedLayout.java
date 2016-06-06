
package com.ponysdk.core.ui.basic;

/**
 * Specifies that a panel can animate between layouts.
 * <p>
 * The normal use pattern is to set all childrens' positions, then to call {@link #animate(int)} to move them
 * to their new positions over some period of time.
 * </p>
 */
public interface PAnimatedLayout {

    /**
     * Layout children, animating over the specified period of time.
     * 
     * @param duration
     *            the animation duration, in milliseconds
     */
    void animate(int duration);
}