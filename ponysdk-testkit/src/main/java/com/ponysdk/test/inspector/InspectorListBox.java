/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.test.inspector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.inspector.predicate.Predicates;

/**
 * High-level inspector for ListBox dropdowns.
 *
 * <p>Wraps the outer {@code PFlowPanel} produced by a {@code ListBox} (the widget carrying
 * the {@code dd-listbox} style) and exposes a fluent API to open and close the dropdown,
 * select labels, read back the current selection, filter items, and clear multi-selection.
 * Internally the inspector drives the same DOM paths a real user would: clicks go through
 * the dropdown button, item clicks through the rendered labels, and filter text through
 * the search text box.</p>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * InspectorListBox lb = InspectorWidget.of(panel).find(InspectorListBox.FACTORY);
 * lb.select("Option A");
 * assertEquals(List.of("Option A"), lb.getSelectedLabels());
 * }</pre>
 *
 * <h2>Open / close contract</h2>
 * <p>{@link #open()} clicks the {@code dd-container-button} child, then triggers a full
 * {@code InfiniteScroll} render so that all items are immediately available for selection
 * - no extra plumbing required in test code. {@link #close()} clicks the same button to
 * toggle the dropdown closed. Both methods are idempotent.</p>
 *
 * <h2>Item lookup</h2>
 * <p>Once opened, the dropdown container (a {@code PFlowPanel} carrying the
 * {@code dd-container-addon} style) is attached to {@code widget.getWindow().getPRootPanel()}.
 * The inspector looks up items through this root panel - the first container found is used,
 * which is the common case where a single dropdown is open at a time.</p>
 */
public class InspectorListBox extends InspectorWidget {

    private static final String STYLE_LISTBOX = "dd-listbox";
    private static final String STYLE_CONTAINER_BUTTON = "dd-container-button";
    private static final String STYLE_CONTAINER_ADDON = "dd-container-addon";
    private static final String STYLE_CONTAINER_OPENED = "dd-container-opened";
    private static final String STYLE_LISTBOX_FILTER = "dd-listbox-filter";
    private static final String STYLE_LISTBOX_CLEAR_MULTI = "dd-listbox-clear-multi";
    private static final String STYLE_LISTBOX_ITEM_GROUP = "dd-listbox-item-group";
    private static final String STYLE_IS_ITEM = "is-item";

    /**
     * Factory matching widgets carrying the {@code dd-listbox} style - the root panel of a
     * {@code ListBox} dropdown.
     */
    public static final InspectorFactory<InspectorListBox> FACTORY = new InspectorFactory<InspectorListBox>() {

        @Override
        public Predicate<PWidget> basePredicate() {
            return Predicates.style(STYLE_LISTBOX);
        }

        @Override
        public InspectorListBox create(final PWidget widget) {
            return new InspectorListBox(widget);
        }
    };

    /**
     * Wraps the given widget.
     *
     * @param widget the ListBox root widget; must not be {@code null}
     * @throws NullPointerException if {@code widget} is {@code null}
     */
    public InspectorListBox(final PWidget widget) {
        super(widget);
    }

    /**
     * Opens the dropdown (idempotent).
     *
     * <p>If the dropdown is already open this method returns immediately. Otherwise it
     * clicks the {@code dd-container-button} child and triggers a full
     * {@code InfiniteScroll} render so every item the provider knows about is available
     * for selection.</p>
     *
     * @return this inspector, for fluent chaining
     */
    public InspectorListBox open() {
        if (isOpen()) return this;
        find(Predicates.style(STYLE_CONTAINER_BUTTON)).click();
        getInfiniteScroll().simulateFullRender();
        return this;
    }

    /**
     * Closes the dropdown (idempotent).
     *
     * <p>If the dropdown is not open this method returns immediately. Otherwise it clicks
     * the {@code dd-container-button} child, which toggles the dropdown closed exactly as
     * clicking it a second time would in a real browser.</p>
     *
     * @return this inspector, for fluent chaining
     */
    public InspectorListBox close() {
        if (!isOpen()) return this;
        find(Predicates.style(STYLE_CONTAINER_BUTTON)).click();
        return this;
    }

    /**
     * @return {@code true} if the dropdown is currently open
     */
    public boolean isOpen() {
        return hasStyle(STYLE_CONTAINER_OPENED);
    }

    /**
     * Opens the dropdown, clicks each item whose label matches one of the supplied values,
     * then closes the dropdown.
     *
     * @param labels the item labels to select; all must exist in the dropdown
     * @return this inspector, for fluent chaining
     * @throws AssertionError if any supplied label is not present in the dropdown
     */
    public InspectorListBox select(final String... labels) {
        if (labels == null || labels.length == 0) {
            return this;
        }
        open();
        final PWidget addon = findOpenedContainer();
        if (addon == null) {
            throw new AssertionError("Dropdown container is not attached. " //
                    + "Expected a widget with style " + STYLE_CONTAINER_ADDON
                    + " under the root panel after open().");
        }
        final InspectorWidget addonInspector = new InspectorWidget(addon);
        for (final String label : labels) {
            final InspectorWidget item = findItemByLabel(addonInspector, label);
            if (item == null) {
                throw new AssertionError("Label not found: " + label //
                        + ". Available: " + collectAvailableLabels(addonInspector));
            }
            item.click();
        }
        close();
        return this;
    }

    /**
     * Returns the labels of the currently selected items in selection order, or an empty
     * list when nothing is selected.
     *
     * <p>The inspector reads the selection from the {@code dd-container-button}'s title,
     * which {@code ListBox} keeps in sync with the current selection (comma-separated
     * labels). An empty title maps to an empty list.</p>
     *
     * @return the ordered list of selected labels; never {@code null}
     */
    public List<String> getSelectedLabels() {
        final InspectorWidget button = findButton();
        if (button == null) return new ArrayList<>();
        final String title = button.getWidget().getTitle();
        if (title == null || title.isEmpty()) return new ArrayList<>();
        final String[] parts = title.split(", ");
        final List<String> result = new ArrayList<>(parts.length);
        for (final String part : parts) {
            if (!part.isEmpty()) result.add(part);
        }
        return result;
    }

    /**
     * Returns every selectable label currently rendered in the dropdown, excluding group
     * headers. The dropdown is opened (and fully rendered) if necessary so callers do not
     * have to worry about the open state.
     *
     * @return the ordered list of available labels; never {@code null}
     */
    public List<String> getAvailableLabels() {
        final boolean wasOpen = isOpen();
        if (!wasOpen) open();
        try {
            final PWidget addon = findOpenedContainer();
            if (addon == null) return new ArrayList<>();
            return collectAvailableLabels(new InspectorWidget(addon));
        } finally {
            if (!wasOpen) close();
        }
    }

    /**
     * Types {@code filterText} into the ListBox search input.
     *
     * <p>The dropdown is opened first if not already open. The filter input lives inside a
     * child carrying the {@code dd-listbox-filter} style - this inspector locates the
     * single text-bearing descendant of that panel and dispatches the text through the
     * standard {@code type()} path, simulating a user typing.</p>
     *
     * @param filterText the text to type into the filter input; may be empty
     * @return this inspector, for fluent chaining
     * @throws AssertionError if the ListBox does not have search enabled
     */
    public InspectorListBox filter(final String filterText) {
        final boolean wasOpen = isOpen();
        if (!wasOpen) open();
        final PWidget addon = findOpenedContainer();
        final InspectorWidget scope = addon != null ? new InspectorWidget(addon) : this;
        if (!scope.has(Predicates.style(STYLE_LISTBOX_FILTER))) {
            if (!wasOpen) close();
            throw new AssertionError("Search is not enabled on this ListBox");
        }
        final InspectorWidget filterPanel = scope.find(Predicates.style(STYLE_LISTBOX_FILTER));
        final InspectorWidget input = findFilterInput(filterPanel);
        if (input == null) {
            if (!wasOpen) close();
            throw new AssertionError("Filter input not found inside " + STYLE_LISTBOX_FILTER);
        }
        input.type(filterText == null ? "" : filterText);
        return this;
    }

    /**
     * Clicks the clear button (only available in multi-selection mode with a configured
     * clear label).
     *
     * @return this inspector, for fluent chaining
     * @throws AssertionError if the clear button is not available on this ListBox
     */
    public InspectorListBox clear() {
        final boolean wasOpen = isOpen();
        if (!wasOpen) open();
        final PWidget addon = findOpenedContainer();
        final InspectorWidget scope = addon != null ? new InspectorWidget(addon) : this;
        if (!scope.has(Predicates.style(STYLE_LISTBOX_CLEAR_MULTI))) {
            if (!wasOpen) close();
            throw new AssertionError("Clear button is not available");
        }
        scope.find(Predicates.style(STYLE_LISTBOX_CLEAR_MULTI)).click();
        if (!wasOpen) close();
        return this;
    }

    /**
     * Returns an {@link InspectorInfiniteScroll} bound to the ListBox's item container for
     * advanced scenarios such as testing partial renders or scroll pagination.
     *
     * <p>The dropdown must be open - {@link #open()} attaches the addon container to the
     * root panel, which is where the {@code InfiniteScroll} lives.</p>
     *
     * @return the infinite scroll inspector
     * @throws AssertionError if the dropdown is not open
     */
    public InspectorInfiniteScroll getInfiniteScroll() {
        final PWidget addon = findOpenedContainer();
        if (addon == null) {
            throw new AssertionError("Dropdown is not open");
        }
        return new InspectorWidget(addon).find(InspectorInfiniteScroll.FACTORY);
    }

    // =====================================================================================
    // Internals
    // =====================================================================================

    private InspectorWidget findButton() {
        final List<InspectorWidget> matches = findAll(Predicates.style(STYLE_CONTAINER_BUTTON));
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * Finds the {@code dd-container-addon} widget attached to the window's root panel when
     * the dropdown is open. Returns {@code null} if no such container is currently attached
     * (the dropdown is closed, or the root panel has no such addon).
     *
     * <p>The PRootPanel can carry addons for several ListBoxes at once; this inspector
     * picks the first match, which is correct for the common case of a single open
     * dropdown at a time.</p>
     */
    private PWidget findOpenedContainer() {
        final PWindow window = widget.getWindow();
        if (window == null) return null;
        final PWidget rootPanel = window.getPRootPanel();
        if (rootPanel == null) return null;
        final InspectorWidget rootInspector = new InspectorWidget(rootPanel);
        final List<InspectorWidget> addons = rootInspector.findAll(Predicates.style(STYLE_CONTAINER_ADDON));
        if (addons.isEmpty()) return null;
        return addons.get(0).getWidget();
    }

    /**
     * Finds a selectable item inside the dropdown addon whose resolved text matches
     * {@code label}. Item rows wrap a text-bearing widget (a {@code PLabel} by default);
     * we match on that inner text and reject group rows (rows whose wrapping panel
     * carries {@code dd-listbox-item-group}).
     */
    private static InspectorWidget findItemByLabel(final InspectorWidget addonInspector, final String label) {
        final List<InspectorWidget> textMatches = addonInspector.findAll(Predicates.text(label));
        for (final InspectorWidget candidate : textMatches) {
            if (isGroupHeader(candidate)) continue;
            return candidate;
        }
        return null;
    }

    /**
     * Checks whether the supplied widget sits inside a ListBox row flagged as a group
     * header. Group headers are not user-selectable so we never click them for
     * {@link #select(String...)} and never report them as available labels.
     */
    private static boolean isGroupHeader(final InspectorWidget candidate) {
        IsPWidget cursor = candidate.getWidget();
        while (cursor instanceof PWidget) {
            final PWidget asWidget = (PWidget) cursor;
            if (asWidget.hasStyleName(STYLE_LISTBOX_ITEM_GROUP)) return true;
            cursor = asWidget.getParent();
        }
        return false;
    }

    /**
     * Collects the labels of every selectable item rendered under the dropdown addon,
     * preserving their display order. Group headers (rows with
     * {@code dd-listbox-item-group}) are skipped.
     *
     * <p>The {@code InfiniteScrollAddon} decorates every rendered row with the
     * {@code is-item} style, which is the most reliable anchor for locating rows across
     * renderer implementations. For each row we pick the first text-bearing descendant -
     * that is the label produced by the item renderer (a {@code PLabel} by default).</p>
     */
    private static List<String> collectAvailableLabels(final InspectorWidget addonInspector) {
        final List<InspectorWidget> rows = addonInspector.findAll(Predicates.style(STYLE_IS_ITEM));
        final List<String> labels = new ArrayList<>();
        for (final InspectorWidget row : rows) {
            if (row.hasStyle(STYLE_LISTBOX_ITEM_GROUP)) continue;
            final List<InspectorWidget> texts = row.findAll(new TextualLeaf());
            if (texts.isEmpty()) continue;
            final String text = texts.get(0).getText();
            if (text == null || text.isEmpty()) continue;
            labels.add(text);
        }
        return labels;
    }

    /**
     * Finds the text-bearing widget living inside the filter panel. The default
     * {@code TextListBoxFilterWidget} uses a {@code PTextBox}; this helper walks the panel
     * looking for the first widget whose text is accessible through the inspector's
     * polymorphic {@code getText()} dispatch.
     */
    private static InspectorWidget findFilterInput(final InspectorWidget filterPanel) {
        final List<InspectorWidget> candidates = filterPanel.findAll(new TextualLeaf());
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Predicate matching any widget whose {@link WidgetText#extract(PWidget)} returns a
     * non-{@code null} value - that is, any widget that can carry text in the inspector
     * model. Centralized here so that item lookup, filter-input lookup, and available-label
     * collection all agree on what "a text-bearing widget" means.
     */
    private static final class TextualLeaf implements Predicate<PWidget> {
        @Override
        public boolean test(final PWidget w) {
            return w != null && WidgetText.extract(w) != null;
        }

        @Override
        public String toString() {
            return "textualLeaf()";
        }
    }
}
