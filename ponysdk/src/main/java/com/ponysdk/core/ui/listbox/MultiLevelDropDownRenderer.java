
package com.ponysdk.core.ui.listbox;

import java.util.List;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.dropdown.DropDownContainerConfiguration.DropDownPosition;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem.ListBoxItemType;

public class MultiLevelDropDownRenderer<D> implements ListBoxItemRenderer<MultiLevelDropDownNode<D>> {

    private static final String STYLE = "is-item-wrapper";
    private static final String STYLE_NESTED = "multi-level-dropdown-container";
    private static final String ATTR_DISABLED = "disabled";

    private final PSimplePanel panel = Element.newPSimplePanel();
    private final ListBox<MultiLevelDropDownNode<D>> parentContainer;

    private ListBox<MultiLevelDropDownNode<D>> nestedListBox;
    private PLabel label;
    private MultiLevelDropDownRenderer<D> parent;
    private Runnable r;
    private ListBoxItem<MultiLevelDropDownNode<D>> item;

    public MultiLevelDropDownRenderer(final ListBox<MultiLevelDropDownNode<D>> parentContainer) {
        this(parentContainer, null);
    }

    private MultiLevelDropDownRenderer(final ListBox<MultiLevelDropDownNode<D>> parentContainer,
            final MultiLevelDropDownRenderer<D> parent) {
        this.parentContainer = parentContainer;
        this.parent = parent;
        panel.addStyleName(STYLE);
        panel.addDestroyListener(e -> {
            if (panel.getWidget() == label) {
                if (nestedListBox != null) nestedListBox.asWidget().onDestroy();
            } else if (label != null) {
                label.onDestroy();
            }
        });
    }

    @Override
    public PWidget asWidget() {
        return panel;
    }

    @Override
    public void addSelectionHandler(final Runnable r) {
        this.r = r;
    }

    @Override
    public void setItem(final ListBoxItem<MultiLevelDropDownNode<D>> item) {
        this.item = item;
        if (item.getType() == ListBoxItemType.GROUP || item.getData().isLeaf()) {
            renderAsLabel(item);
        } else {
            if (nestedListBox == null) {
                buildNestedListBox(item);
            }
            renderAsListBox(item);
        }
        if (parent != null) parent.parentContainer.showIndex(parent.item);
    }

    public void openNested() {
        if (nestedListBox != null && panel.getWidget() == nestedListBox.asWidget()) {
            nestedListBox.open();
        }
    }

    /**
     * Close all the hierarchy of the current dropdown, parent by parent.
     * If there is no parent at all, this is the first level and we just close the parent container.
     */
    public void closeAll() {
        if (nestedListBox != null) {
            nestedListBox.close();
        }
        if (parent != null) {
            parent.closeAll();
        } else {
            parentContainer.close();
        }
    }

    private void buildNestedListBox(final ListBoxItem<MultiLevelDropDownNode<D>> item) {
        final ListBoxConfiguration configuration = getConfiguration();
        configuration.setTitle(item.getLabel());

        // If the sub level contains group, we have to configure the listbox for it
        if (item.getData().getItems().stream().anyMatch(i -> i.getType() == ListBoxItemType.GROUP)) {
            configuration.enableGroup();
        }
        nestedListBox = new ListBox<>(configuration, item.getData().getItems());
        nestedListBox.addContainerStyleName(STYLE_NESTED);

        parentContainer.defineAsMultiLevelParent(nestedListBox);
        nestedListBox.addOpenHandler(e -> parentContainer.removeUpDownKeyHandler());
        nestedListBox.addCloseHandler(e -> parentContainer.addUpDownKeyHandler());

        nestedListBox.setItemRendererSupplier(() -> new MultiLevelDropDownRenderer<>(nestedListBox, this));

        for (final PValueChangeHandler<List<ListBoxItem<MultiLevelDropDownNode<D>>>> handler : parentContainer
            .getValueChangeHandlers()) {
            nestedListBox.addValueChangeHandler(handler);
        }
    }

    private void renderAsListBox(final ListBoxItem<MultiLevelDropDownNode<D>> item) {
        nestedListBox.getConfiguration().setTitle(item.getLabel());
        nestedListBox.clear();
        nestedListBox.addItems(item.getData().getItems());
        panel.setWidget(nestedListBox);
    }

    private void renderAsLabel(final ListBoxItem<MultiLevelDropDownNode<D>> item) {
        if (label == null) label = Element.newPLabel();
        label.setText(item.getLabel());
        label.setTitle(item.getLabel());
        label.addDomHandler((PClickHandler) e -> r.run(), PClickEvent.TYPE);
        if (item.isEnabled()) label.removeAttribute(ATTR_DISABLED);
        else label.setAttribute(ATTR_DISABLED);
        panel.setWidget(label);
    }

    public static ListBoxConfiguration getConfiguration() {
        final ListBoxConfiguration configuration = new ListBoxConfiguration();
        configuration.enabledEventOnly();
        configuration.disableClearTitleButton();
        configuration.enableStopClickEvent();
        configuration.setPosition(DropDownPosition.OUTSIDE);
        configuration.enableMultilevel();
        configuration.disableSorting();
        configuration.disableSearch();
        configuration.enabledEventOnly();
        return configuration;
    }

}
