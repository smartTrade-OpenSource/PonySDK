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

package com.ponysdk.core.ui.listbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.dropdown.DropDownContainer;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollAddon;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollProvider;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem.ListBoxItemType;
import com.ponysdk.core.ui.model.PKeyCodes;

public class ListBox<D> extends DropDownContainer<List<ListBoxItem<D>>, ListBoxConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(ListBox.class);

    private static final String STYLE_LISTBOX = "dd-listbox";
    private static final String STYLE_LISTBOX_MULTI = "dd-listbox-multi";
    private static final String STYLE_LISTBOX_GROUP = "dd-listbox-group";
    private static final String STYLE_LISTBOX_CONTAINER = "dd-listbox-container";
    private static final String STYLE_LISTBOX_FILTER = "dd-listbox-filter";
    private static final String STYLE_LISTBOX_CLEAR_MULTI = "dd-listbox-clear-multi";
    protected static final String STYLE_LISTBOX_ITEM_SELECTED = "dd-listbox-item-selected";
    protected static final String STYLE_LISTBOX_ITEM_LAST_SELECTED = "dd-listbox-item-last-selected";
    protected static final String STYLE_LISTBOX_ITEM_GROUP = "dd-listbox-item-group";

    protected static final String DISABLED = "disabled";

    private ListBoxTextBox textBox;
    private PButton clearMultiButton;
    private InfiniteScrollAddon<ListBoxItem<D>, ListBoxItemWidget> itemContainer;

    private Comparator<ListBoxItem<D>> groupComparator;
    private Comparator<D> dataComparator;
    private final Comparator<ListBoxItem<D>> comparator = (i1, i2) -> {
        if (i1.groupName != null) {
            if (i2.groupName == null) return -1;
            final int compareToGroup;
            if (groupComparator != null) {
                compareToGroup = groupComparator.compare(i1, i2);
            } else {
                compareToGroup = i1.groupName.compareToIgnoreCase(i2.groupName);
            }
            if (compareToGroup != 0) return compareToGroup;
        }
        if (i1.data == null) return -1;
        if (i2.data == null) return 1;
        try {
            if (dataComparator != null) return dataComparator.compare(i1.data, i2.data);
        } catch (final Exception e) {
            log.debug("Error when sorting data values", e);
            return 0;
        }
        if (i1.label == null) {
            if (i2.label == null) return 0;
            else return -1;
        } else if (i2.label == null) {
            return 1;
        }
        return i1.label.compareToIgnoreCase(i2.label);
    };
    private final Comparator<ListBoxItem<D>> selectionComparator = (i1, i2) -> (i2.isSelected() ? 1 : 0)
            - (i1.isSelected() ? 1 : 0);

    protected ListBoxItem<D> lastSelectedItem;
    private boolean shiftPressed;
    private HandlerRegistration upDownKeyHandler;
    private int index;
    private boolean forceIndex;

    protected final List<ListBoxItem<D>> items;
    protected final ListBoxDataProvider<D> dataProvider;
    protected final List<ListBoxItem<D>> visibleItems;
    protected final Map<String, GroupListBoxItem<D>> groupItems;
    protected final List<ListBoxItem<D>> selectedDataItems;

    
    public ListBox(final ListBoxConfiguration configuration) {
        this(configuration, List.of());

    }

    public ListBox(final ListBoxConfiguration configuration, final Collection<? extends ListBoxItem<D>> items) {
        super(configuration);
        if (configuration.isMultiSelectionEnabled() && configuration.isGroupEnabled()) {
            throw new IllegalStateException("Group mode cannot be enabled in multi selection mode.");
        }
        if (configuration.isMultiSelectionEnabled() && configuration.isEventOnlyEnabled()) {
            throw new IllegalStateException("Multi selection cannot be activated in event only mode");
        }

        this.groupItems = new HashMap<>();
        if (configuration.isGroupEnabled()) this.items = new ArrayList<>(initializeGroupItems(items));
        else this.items = new ArrayList<>(items);
        this.visibleItems = new ArrayList<>(items);
        this.dataProvider = null;
        this.selectedDataItems = new ArrayList<>();
    }

    public ListBox(final ListBoxConfiguration configuration, final ListBoxDataProvider<D> dataProvider) {
        super(configuration);
        this.groupItems = new HashMap<>();
        this.items = new ArrayList<>();
        this.visibleItems = new ArrayList<>();
        this.dataProvider = dataProvider;
        this.selectedDataItems = new ArrayList<>();
    }
    


    @Override
    public List<ListBoxItem<D>> getValue() {
        if (configuration.isEventOnlyEnabled())
            throw new UnsupportedOperationException("Not available in event only mode");
        return getSelectedItems();
    }

    @Override
    public void setValue(final List<ListBoxItem<D>> value) {
        if (configuration.isEventOnlyEnabled())
            throw new UnsupportedOperationException("Not available in event only mode");
        if (value == null) {
            clearSelection();
            updateTitle(List.of());
            updateVisibleItems();
        } else {
            setSelectedItems(value);
        }
    }

    public Comparator<ListBoxItem<D>> getComparator() {
        return comparator;
    }

    public void addItem(final ListBoxItem<D> item) {
        if (dataProvider != null) throw new IllegalCallerException("Not supported in DataProvider mode");
        if (configuration.isGroupEnabled() && ListBoxItemType.GROUP.equals(item.getType())) {
            addGroup(items, (GroupListBoxItem<D>) item);
        } else {
            this.items.add(item);
        }
        if (isInitialized()) updateVisibleItems();
    }

    public void addItems(final Collection<ListBoxItem<D>> newItems) {
        if (dataProvider != null) throw new IllegalCallerException("Not supported in DataProvider mode");
        if (configuration.isGroupEnabled()) {
            for (final ListBoxItem<D> item : newItems) {
                if (ListBoxItemType.GROUP.equals(item.getType())) {
                    addGroup(items, (GroupListBoxItem<D>) item);
                }
            }
        } else {
            this.items.addAll(newItems);
        }
        if (isInitialized()) updateVisibleItems();
    }

    public void addItemInGroup(final String groupName, final ListBoxItem<D> item) {
        if (dataProvider != null) throw new IllegalCallerException("Not supported in DataProvider mode");
        if (!configuration.isGroupEnabled()) throw new IllegalCallerException("Group mode must be enabled");
        final GroupListBoxItem<D> groupItem = groupItems.get(groupName);
        if (groupItem != null) {
            groupItem.addItem(item);
            items.add(item);
            if (isInitialized()) updateVisibleItems();
        }
    }

    public void removeItem(final String itemLabel) {
        if (dataProvider != null) throw new IllegalCallerException("Not supported in DataProvider mode");
        this.items.stream().filter(i -> i.label.equals(itemLabel)).findFirst().ifPresentOrElse(removedItem -> {
            this.items.remove(removedItem);
            this.visibleItems.remove(removedItem);
            if (configuration.isMultiSelectionEnabled()) {
                setSelectedItems(getSelectedItems());
            } else if (removedItem.isSelected()) {
                setSelected(null);
            }
            if (itemContainer != null && isOpen()) itemContainer.refresh();
        }, () -> log.warn("Item to remove not found {}", itemLabel));
    }

    public void removeItemFromGroup(final String groupName, final String itemLabel) {
        if (dataProvider != null) throw new IllegalCallerException("Not supported in DataProvider mode");
        if (!configuration.isGroupEnabled()) throw new IllegalCallerException("Group mode must be enabled");
        final GroupListBoxItem<D> groupItem = groupItems.get(groupName);
        if (groupItem != null) {
            final ListBoxItem<D> removedItem = groupItem.getGroupItems().stream()
                    .filter(i -> i.getLabel().equals(itemLabel)).findFirst().orElse(null);
            if (removedItem != null) {
                groupItem.getGroupItems().remove(removedItem);
                this.items.remove(removedItem);
                this.visibleItems.remove(removedItem);
                if (removedItem.isSelected()) {
                    setSelected(null);
                }
                if (itemContainer != null && isOpen()) itemContainer.refresh();
            }
        }
    }

    public void removeGroup(final String groupName) {
        if (dataProvider != null) throw new IllegalCallerException("Not supported in DataProvider mode");
        if (!configuration.isGroupEnabled()) throw new IllegalCallerException("Group mode must be enabled");
        final GroupListBoxItem<D> groupItem = groupItems.remove(groupName);
        if (groupItem != null) {
            boolean removeSelection = false;
            this.items.remove(groupItem);
            this.visibleItems.remove(groupItem);
            for (final ListBoxItem<D> item : groupItem.getGroupItems()) {
                removeSelection |= item.isSelected();
                this.items.remove(item);
                this.visibleItems.remove(item);
            }
            if (removeSelection) {
                setSelected(null);
            }
            if (itemContainer != null && isOpen()) itemContainer.refresh();
        }
    }

    public void renameItem(final String oldLabel, final String newLabel) {
        if (dataProvider != null) throw new IllegalCallerException("Not supported in DataProvider mode");
        if (configuration.isGroupEnabled()) throw new IllegalCallerException("Group mode must not be enabled");
        final ListBoxItem<D> item = this.items.stream().filter(i -> i.getLabel().equals(oldLabel)).findFirst()
                .orElse(null);
        if (item != null) {
            item.setLabel(newLabel);
            if (isInitialized()) {
                updateVisibleItems();
                updateTitle(getSelectedItems());
            }
        }
    }

    public void renameItemInGroup(final String groupName, final String oldLabel, final String newLabel) {
        if (dataProvider != null) throw new IllegalCallerException("Not supported in DataProvider mode");
        if (!configuration.isGroupEnabled()) throw new IllegalCallerException("Group mode must be enabled");
        final GroupListBoxItem<D> groupItem = groupItems.get(groupName);
        if (groupItem != null) {
            final ListBoxItem<D> item = groupItem.getGroupItems().stream().filter(i -> i.getLabel().equals(oldLabel))
                    .findFirst().orElse(null);
            if (item != null) {
                item.setLabel(newLabel);
                if (isInitialized()) {
                    updateVisibleItems();
                    updateTitle(getSelectedItems());
                }
            }
        }
    }

    public void clear() {
        this.items.clear();
        this.visibleItems.clear();
        this.groupItems.clear();
        this.selectedDataItems.clear();
        updateTitle(List.of());
        if (itemContainer != null && isOpen()) itemContainer.refresh();
    }

    /**
     * Returns the selected data in single selection mode and the first selected
     * data in multi selection mode.
     *
     * @return the selected data or null if no selection
     */
    public D getSelectedData() {
        if (dataProvider != null) return selectedDataItems.isEmpty() ? null : selectedDataItems.get(0).getData();
        return items.stream().filter(ListBoxItem::isSelected).map(ListBoxItem::getData).findFirst().orElse(null);
    }

    public List<D> getSelectedDataList() {
        if (dataProvider != null) return selectedDataItems.isEmpty() ? List.of()
                : selectedDataItems.stream().map(ListBoxItem::getData).collect(Collectors.toList());
        return items.stream().filter(ListBoxItem::isSelected).map(ListBoxItem::getData).collect(Collectors.toList());
    }

    /**
     * Returns the selected item in single selection mode and the first selected
     * item in multi selection mode.
     *
     * @return the selected item or null if no selection
     */
    public ListBoxItem<D> getSelectedItem() {
        if (dataProvider != null) return selectedDataItems.isEmpty() ? null : selectedDataItems.get(0);
        return items.stream().filter(ListBoxItem::isSelected).findFirst().orElse(null);
    }

    public List<ListBoxItem<D>> getSelectedItems() {
        if (dataProvider != null) return new ArrayList<>(selectedDataItems);
        return items.stream().filter(ListBoxItem::isSelected).collect(Collectors.toList());
    }

    public void setSelected(final D selectedData) {
        if (configuration.isMultiSelectionEnabled())
            throw new IllegalArgumentException("Use setSelected(Collection) method instead");
        if (configuration.isEventOnlyEnabled())
            throw new UnsupportedOperationException("Not available in event only mode");

        if (dataProvider == null) {
            if (selectedData == null) {
                clearSelection();
                updateTitle(List.of());
                updateVisibleItems();
                return;
            }
            for (final ListBoxItem<D> item : this.items) {
                if (item.getData() != null && item.getData().equals(selectedData)) {
                    setSelectedItem(item);
                }
            }
        } else {
            clearSelection();
            final ListBoxItem<D> item = dataProvider.getItemFromData(selectedData);
            if (item != null) selectedDataItems.add(item);
            updateTitle(selectedDataItems);
            updateVisibleItems();
        }
    }

    public void setMultiSelected(final Collection<D> selectedData) {
        if (!configuration.isMultiSelectionEnabled())
            throw new IllegalArgumentException("Use setSelected(D) method instead");
        if (configuration.isEventOnlyEnabled())
            throw new UnsupportedOperationException("Not available in event only mode");
        if (dataProvider == null) {
            if (selectedData == null || selectedData.isEmpty()) {
                clearSelection();
                updateTitle(List.of());
                updateVisibleItems();
                return;
            }
            final List<ListBoxItem<D>> selectedItems = new ArrayList<>();
            if (isSelectionAllowed(selectedData)) {
                for (final D data : selectedData) {
                    for (final ListBoxItem<D> item : this.items) {
                        if (item.getData() != null && item.getData().equals(data)) {
                            selectedItems.add(item);
                            break;
                        }
                    }
                }
            }
            setSelectedItems(selectedItems);
        } else {
            clearSelection();
            if (selectedData != null && !selectedData.isEmpty()) {
                if (isSelectionAllowed(selectedData)) {
                    for (final D data : selectedData) {
                        final ListBoxItem<D> item = dataProvider.getItemFromData(data);
                        if (item != null) selectedDataItems.add(item);
                    }
                }
            }
            updateTitle(selectedDataItems);
            updateVisibleItems();
        }
    }

    public void addToSelection(final D selectedData) {
        if (selectedData == null) return;
        if (dataProvider != null) throw new IllegalArgumentException("Only available without a ListBoxDataProvider");
        if (configuration.isEventOnlyEnabled())
            throw new UnsupportedOperationException("Not available in event only mode");

        if (!configuration.isMultiSelectionEnabled()) setSelected(selectedData);
        final List<ListBoxItem<D>> selectedItems = getSelectedItems();
        for (final ListBoxItem<D> item : this.items) {
            if (item.getData() != null && item.getData().equals(selectedData)) {
                selectedItems.add(item);
                break;
            }
        }
        if (isSelectionAllowed(selectedItems)) {
            setSelectedItems(selectedItems);
        }
    }

    public void removeFromSelection(final D unselectedData) {
        if (unselectedData == null) return;
        if (dataProvider != null) throw new IllegalArgumentException("Only available without a ListBoxDataProvider");
        if (!configuration.isMultiSelectionEnabled())
            throw new IllegalArgumentException("Only available in multi selection mode");
        if (configuration.isEventOnlyEnabled())
            throw new UnsupportedOperationException("Not available in event only mode");

        final List<ListBoxItem<D>> selectedItems = getSelectedItems();
        final Iterator<ListBoxItem<D>> iterator = selectedItems.iterator();
        while (iterator.hasNext()) {
            final ListBoxItem<D> listBoxItem = iterator.next();
            if (listBoxItem.getData() != null && listBoxItem.getData().equals(unselectedData)) {
                iterator.remove();
                break;
            }
        }
        setSelectedItems(selectedItems);
    }

    public void replaceItems(final Collection<? extends ListBoxItem<D>> items) {
        if (dataProvider != null) throw new IllegalArgumentException("Only available without a ListBoxDataProvider");
        clearSelection();
        this.items.clear();
        this.items.addAll(items);
        updateTitle(getSelectedItems());
        updateVisibleItems();
    }

    public void setSelectedId(final Object id) {
        if (dataProvider == null) throw new IllegalArgumentException("Only available with a ListBoxDataProvider");
        if (configuration.isMultiSelectionEnabled())
            throw new IllegalArgumentException("Use setSelectedIds(...) method instead");
        if (configuration.isEventOnlyEnabled())
            throw new UnsupportedOperationException("Not available in event only mode");

        clearSelection();
        if (id != null) selectedDataItems.addAll(dataProvider.getDataByIds(List.of(id)));
        updateTitle(selectedDataItems);
        updateVisibleItems();
    }

    public void setSelectedIds(final Collection<Object> ids) {
        if (dataProvider == null) throw new IllegalArgumentException("Only available with a ListBoxDataProvider");
        if (!configuration.isMultiSelectionEnabled())
            throw new IllegalArgumentException("Use setSelectedId(...) method instead");
        if (configuration.isEventOnlyEnabled())
            throw new UnsupportedOperationException("Not available in event only mode");

        clearSelection();
        if (ids != null && !ids.isEmpty()) {
            if (isSelectionAllowed(ids)) {
                selectedDataItems.addAll(dataProvider.getDataByIds(ids));
            }
        }
        updateTitle(selectedDataItems);
        updateVisibleItems();
    }

    public void setSelectedIndex(final int index) {
        clearSelection();
        if (dataProvider == null) {
            try {
                sort();
                final ListBoxItem<D> listBoxItem = items.get(index);
                if (configuration.isMultiSelectionEnabled()) {
                    setSelectedItems(List.of(listBoxItem));
                } else {
                    setSelectedItem(listBoxItem);
                }
            } catch (final Exception e) {
                log.debug("Cannot select index {} ", index, e);
            }
        } else {
            final List<ListBoxItem<D>> data = dataProvider.getData(index, 0, null);
            if (data != null && !data.isEmpty()) {
                if (configuration.isMultiSelectionEnabled()) {
                    setMultiSelected(List.of(data.get(0).data));
                } else {
                    setSelected(data.get(0).data);
                }
            }
        }
    }

    public void setDataEnabled(final D value, final boolean enabled) {
        Objects.requireNonNull(value);
        if (dataProvider != null) throw new UnsupportedOperationException("Only available with a ListBoxDataProvider");
        this.items.stream().filter(item -> item.getData().equals(value)).findFirst().ifPresent(item -> {
            item.setEnabled(enabled);
            refresh();
        });
    }

    public void setComparator(final Comparator<D> dataComparator) {
        this.dataComparator = dataComparator;
    }

    public void setGroupComparator(final Comparator<ListBoxItem<D>> groupComparator) {
        this.groupComparator = groupComparator;
    }

    public int getSize() {
        if (dataProvider != null) {
            return dataProvider.getFullDataSize(null);
        }
        return items.size();
    }

    @Override
    protected PWidget createDefaultContainer() {
        final PPanel defaultContainer = Element.newPFlowPanel();

        if (configuration.isSearchEnabled()) {
            final PPanel panel = Element.newPSimplePanel();
            panel.addStyleName(STYLE_LISTBOX_FILTER);
            textBox = buildTextBox();
            if (configuration.getPlaceholder() != null) {
                textBox.setPlaceholder(configuration.getPlaceholder());
            }
            textBox.setTabindex(TabindexMode.TABULABLE);
            textBox.addKeyDownHandler(e -> {
                if (e.getKeyCode() == PKeyCodes.SHIFT.getCode()) {
                    shiftPressed = true;
                }
                if (shiftPressed && e.getKeyCode() == PKeyCodes.TAB.getCode()) {
                    focus();
                }
            });
            textBox.addKeyUpHandler(e -> {
                if (e.getKeyCode() == PKeyCodes.SHIFT.getCode()) {
                    shiftPressed = false;
                } else if (e.getKeyCode() != PKeyCodes.TAB.getCode() && //
                e.getKeyCode() != PKeyCodes.TAB.getCode() && //
                e.getKeyCode() != PKeyCodes.CTRL.getCode() && //
                e.getKeyCode() != PKeyCodes.ALT.getCode() && //
                e.getKeyCode() != PKeyCodes.PAGE_UP.getCode() && //
                e.getKeyCode() != PKeyCodes.PAGE_DOWN.getCode() && //
                e.getKeyCode() != PKeyCodes.END.getCode() && //
                e.getKeyCode() != PKeyCodes.ENTER.getCode() && //
                e.getKeyCode() != PKeyCodes.HOME.getCode() && //
                e.getKeyCode() != PKeyCodes.LEFT.getCode() && //
                e.getKeyCode() != PKeyCodes.UP.getCode() && //
                e.getKeyCode() != PKeyCodes.RIGHT.getCode() && //
                e.getKeyCode() != PKeyCodes.DOWN.getCode()) {
                    updateVisibleItems();
                }
            });
            panel.add(textBox);
            defaultContainer.add(panel);
        } else {
            disableSpaceWhenOpened();
        }
        if (configuration.isMultiSelectionEnabled() && configuration.getClearLabel() != null) {
            clearMultiButton = Element.newPButton(configuration.getClearLabel());
            clearMultiButton.addStyleName(STYLE_LISTBOX_CLEAR_MULTI);
            clearMultiButton.addClickHandler(e -> {
                clearSelection();
                onSelectionChange();
                itemContainer.refresh();
            });
            defaultContainer.add(clearMultiButton);
        }
        itemContainer = buildInfiniteScrollAddon();
        defaultContainer.add(itemContainer);
        itemContainer.addListener(beginIndex -> {
            if (!forceIndex) {
                index = beginIndex;
                if (index == 0) {
                    initIndex();
                }
            }
            forceIndex = false;
        });
        if (configuration.isGroupEnabled()) {
            initIndex();
            itemContainer.setStartIndex(1);
        }
        updateTitle(getSelectedItems());

        addStyleName(STYLE_LISTBOX);
        addContainerStyleName(STYLE_LISTBOX_CONTAINER);
        if (configuration.isMultiSelectionEnabled()) {
            addStyleName(STYLE_LISTBOX_MULTI);
            addContainerStyleName(STYLE_LISTBOX_MULTI);
        }
        if (configuration.isGroupEnabled()) {
            addStyleName(STYLE_LISTBOX_GROUP);
            addContainerStyleName(STYLE_LISTBOX_GROUP);
        }
        return defaultContainer;
    }


    protected InfiniteScrollAddon<ListBoxItem<D>, ListBox<D>.ListBoxItemWidget> buildInfiniteScrollAddon() {
        return new InfiniteScrollAddon<>(new ListBoxInfiniteScrollProvider());
    }

    @Override
    protected ListBoxTextBox buildTextBox() {
		return new DefaultTextBox();
	}
    
    protected Supplier<ListBoxLabel> getItemWidgetLabel() {
    	return () -> new DefaultListBoxLabel();
    }


	@Override
    protected void updateTitle(final List<ListBoxItem<D>> selectedItems) {
        if (!isInitialized()) return;
        final StringBuilder text = new StringBuilder();
        String title = "";
        if (configuration.isTitleDisplayed()) {
            if (configuration.isTitlePlaceHolder()) {
                if (selectedItems.isEmpty() && configuration.getTitle() != null) {
                    mainButton.addStyleName(STYLE_CONTAINER_BUTTON_PLACEHOLDER);
                    text.append(configuration.getTitle());
                } else {
                    mainButton.removeStyleName(STYLE_CONTAINER_BUTTON_PLACEHOLDER);
                }
            } else if (configuration.getTitle() != null) {
                text.append(configuration.getTitle());
            }
        }
        if (selectedItems.isEmpty()) {
            if ((configuration.isMultiSelectionEnabled() || configuration.isSelectionDisplayed())
                    && !configuration.isTitlePlaceHolder()) {
                if (text.length() > 0) {
                    text.append(STRING_SPACE);
                    text.append(configuration.getTitleSeparator());
                    text.append(STRING_SPACE);
                }
                if (configuration.getAllLabel() != null) {
                    text.append(configuration.getAllLabel());
                }
            }
            mainButton.setText(text.toString());
            mainButton.setTitle(title);
            stateButton.setTitle(title);
            removeStyleName(STYLE_CONTAINER_SELECTED);
        } else {
            if (configuration.isSelectionDisplayed()) {
                if (text.length() > 0) {
                    text.append(STRING_SPACE);
                    text.append(configuration.getTitleSeparator());
                    text.append(STRING_SPACE);
                }
                title = selectedItems.stream().map(ListBoxItem::getLabel).collect(Collectors.joining(", "));
                final Integer displaySelectionLimit = configuration.getDisplaySelectionLimit();
                if (configuration.isMultiSelectionEnabled() && displaySelectionLimit != null
                        && selectedItems.size() > displaySelectionLimit) {
                    text.append(selectedItems.size() + STRING_SPACE + configuration.getDisplaySelectionLabel());
                } else {
                    text.append(title);
                }
            }
            mainButton.setText(text.toString());
            mainButton.setTitle(title);
            stateButton.setTitle(title);
            addStyleName(STYLE_CONTAINER_SELECTED);
        }
        setClearTitleButtonVisible(!selectedItems.isEmpty());
    }

    @Override
    protected void beforeContainerVisible() {
        initIndex();
        if (textBox != null) {
            textBox.setText(null);
        }
        sort();
        updateVisibleItems();
    }

    @Override
    protected void afterContainerVisible() {
        itemContainer.setScrollTop();
        if (textBox != null) textBox.focus();
        if (clearMultiButton != null) clearMultiButton.setEnabled(!getSelectedItems().isEmpty());
        upDownKeyHandler = asWidget().getWindow().getPRootPanel().addKeyDownHandler(e -> {
            if (PKeyCodes.UP.getCode() == e.getKeyCode()) {
                if (index > 0) {
                    index--;
                    if (index > 0 && configuration.isGroupEnabled()
                            && visibleItems.get(index) instanceof GroupListBoxItem) {
                        index--;
                    }
                }
                if (index <= 0 && configuration.isGroupEnabled()) {
                    final int previous = index;
                    initIndex();
                    if (previous != index) {
                        itemContainer.showIndex(previous);
                    }
                }
            } else if (PKeyCodes.DOWN.getCode() == e.getKeyCode()) {
                if (index < itemContainer.getFullSize() - 1) {
                    index++;
                    if (configuration.isGroupEnabled() && visibleItems.get(index) instanceof GroupListBoxItem) {
                        index++;
                    }
                }
            } else if (PKeyCodes.ENTER.getCode() == e.getKeyCode()) {
                final ListBoxItemWidget listBoxItemWidget = itemContainer.getCurrentItemIndex();
                if (listBoxItemWidget != null) {
                    listBoxItemWidget.select();
                }
            } else if (PKeyCodes.ESCAPE.getCode() == e.getKeyCode()) {
                close();
            }
            forceIndex = true;
            itemContainer.showIndex(index);
        });
    }

    @Override
    protected void afterContainerClose() {
        super.afterContainerClose();
        if (upDownKeyHandler != null) {
            upDownKeyHandler.removeHandler();
        }
    }

    @Override
    protected boolean isValueEmpty(final List<ListBoxItem<D>> value) {
        return value.isEmpty();
    }

    @Override
    protected boolean isContainerFocusable() {
        return textBox != null;
    }

    @Override
    protected void focusContainer() {
        textBox.focus();
    }

    private Collection<ListBoxItem<D>> initializeGroupItems(final Collection<? extends ListBoxItem<D>> groupItems) {
        final List<ListBoxItem<D>> newItems = new ArrayList<>();
        for (final ListBoxItem<D> currentItem : groupItems) {
            if (ListBoxItemType.GROUP.equals(currentItem.getType())) {
                addGroup(newItems, (GroupListBoxItem<D>) currentItem);
            }
        }
        return newItems;
    }

    private void addGroup(final List<ListBoxItem<D>> newItems, final GroupListBoxItem<D> groupItem) {
        newItems.add(groupItem);
        newItems.addAll(groupItem.getGroupItems());
        this.groupItems.put(groupItem.getGroupName(), groupItem);
    }

    private void clearSelection() {
        if (dataProvider != null) this.selectedDataItems.clear();
        this.items.forEach(i -> i.setSelected(false));
        setClearTitleButtonVisible(false);
        if (clearMultiButton != null) clearMultiButton.setEnabled(false);
    }

    private void onSelectionChange() {
        onSelectionChange(null, false);
    }

    protected void onSelectionChange(final ListBoxItem<D> selectedItem, final boolean add) {
        final List<ListBoxItem<D>> selectedItems;
        if (selectedItem == null) {
            selectedItems = List.of();
            setClearTitleButtonVisible(false);
            if (clearMultiButton != null) clearMultiButton.setEnabled(false);
        } else {
            if (configuration.isMultiSelectionEnabled()) {
                selectedItem.setSelected(add);
                if (dataProvider != null) {
                    if (add) selectedDataItems.add(selectedItem);
                    else selectedDataItems.remove(selectedItem);
                }
            } else {
                clearSelection();
                selectedItem.setSelected(true);
                if (dataProvider != null) selectedDataItems.add(selectedItem);
            }
            selectedItems = new ArrayList<>(getSelectedItems());
            setClearTitleButtonVisible(!selectedItems.isEmpty());
            if (clearMultiButton != null) clearMultiButton.setEnabled(!selectedItems.isEmpty());
        }
        updateTitle(selectedItems);
        onValueChange();
    }

    private void sort() {
        if (configuration.isSortingEnabled() && dataProvider == null) {
            items.sort(comparator);
            if (configuration.isMultiSelectionEnabled() && configuration.isSelectionSortingEnabled()) {
                final List<ListBoxItem<D>> selectedItems = getSelectedItems();
                if (!selectedItems.isEmpty()) {
                    items.sort(selectionComparator);
                    lastSelectedItem = selectedItems.get(selectedItems.size() - 1);
                } else lastSelectedItem = null;
            }
        }
    }

    protected void updateVisibleItems() {
        if (dataProvider == null) {
            this.visibleItems.clear();
            final String filter = getFilter() != null ? getFilter().toLowerCase() : null;
            if (filter != null && !filter.isEmpty()) {
                if (configuration.isGroupEnabled()) {
                    for (final ListBoxItem<D> item : items) {
                        if (ListBoxItemType.ITEM.equals(item.getType())
                                && item.getLabel().toLowerCase().contains(filter)) {
                            if (configuration.isGroupEnabled()) {
                                final GroupListBoxItem<D> groupItem = groupItems.get(item.getGroupName());
                                if (!this.visibleItems.contains(groupItem)) {
                                    this.visibleItems.add(groupItem);
                                }
                            }
                            this.visibleItems.add(item);
                        }
                    }
                } else {
                    this.visibleItems.addAll(this.items.stream()
                            .filter(i -> i.getLabel().toLowerCase().contains(filter)).collect(Collectors.toList()));
                }
            } else {
                this.visibleItems.addAll(items);
            }
        }
        refresh();
    }

    public void refresh() {
        if (isOpen()) itemContainer.refresh();
    }
    
    public void refresh() {
    	if (isOpen()) itemContainer.refresh();
    }

    private void setSelectedItem(final ListBoxItem<D> selectedItem) {
        clearSelection();
        final ListBoxItem<D> item = this.items.stream().filter(selectedItem::equals).findFirst().orElse(null);
        if (item != null) {
            item.setSelected(true);
            setClearTitleButtonVisible(true);
        } else {
            setClearTitleButtonVisible(false);
        }
        updateTitle(List.of(selectedItem));
        updateVisibleItems();
    }

    private void setSelectedItems(final List<ListBoxItem<D>> selectedItems) {
        this.items.forEach(i -> i.setSelected(selectedItems.contains(i)));
        updateTitle(selectedItems);
        updateVisibleItems();
    }

    protected boolean isSelectionAllowed(Collection<?> selectedItems) {
        if (selectedItems == null) selectedItems = getSelectedItems();
        final Integer selectionLimit = configuration.getSelectionLimit();
        if (configuration.isMultiSelectionEnabled() && selectionLimit != null
                && selectedItems.size() >= selectionLimit) {
            log.debug("Selection limit reached ({})", selectionLimit);
            return false;
        }
        return true;
    }
    

    protected String getFilter() {
        return textBox != null ? textBox.getText() : null;
    }

    protected void applyCloseOnClickMode() {
        switch (configuration.getCloseOnClickMode()) {
        case DEFAULT:
            if (!configuration.isMultiSelectionEnabled()) close();
            break;
        case TRUE:
            close();
            break;
        case FALSE:
            if (!configuration.isMultiSelectionEnabled()) itemContainer.refresh();
            break;
        }
    }

    private void initIndex() {
        index = configuration.isGroupEnabled() ? 1 : 0;
    }
    protected ListBoxDataProvider<D> getDataProvider(){
    	return dataProvider;
    }

    //

    public interface ListBoxDataProvider<D> {

        int getFullDataSize(String containsLabelFilter);

        List<ListBoxItem<D>> getDataByIds(Collection<Object> ids);

        List<ListBoxItem<D>> getData(int beginIndex, int maxSize, String containsLabelFilter);

        ListBoxItem<D> getItemFromData(D selectedData);
    }

    public static class ListBoxItem<D> {

        private String label;
        private final D data;
        private boolean selected;
        private boolean enabled = true;
        private String groupName;
        
        private ListBoxItem(final String label, final D data, final boolean selected) {
            this.label = label;
            this.data = data;
            this.selected = selected;
        }

        public String getLabel() {
        	return label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }

        public D getData() {
            return data;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(final boolean selected) {
            this.selected = selected;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        public String getGroupName() {
            return groupName;
        }

        void setGroupName(final String groupName) {
            this.groupName = groupName;
        }

        public ListBoxItemType getType() {
            return ListBoxItemType.ITEM;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (groupName == null ? 0 : groupName.hashCode());
            result = prime * result + (label == null ? 0 : label.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final ListBoxItem<?> other = (ListBoxItem<?>) obj;
            if (groupName == null) {
                if (other.groupName != null) return false;
            } else if (!groupName.equals(other.groupName)) return false;
            if (label == null) {
                if (other.label != null) return false;
            } else if (!label.equals(other.label)) return false;
            return true;
        }

        @Override
        public String toString() {
            return "ListBoxItem [label=" + label + ", data=" + data + ", selected=" + selected + "]";
        }

        public static ListBoxItem<String> of(final String data) {
            return of(data, data, false);
        }

        public static ListBoxItem<String> of(final String data, final boolean selected) {
            return of(data, data, selected);
        }

        public static <D> ListBoxItem<D> of(final String label, final D data) {
            return of(label, data, false);
        }

        public static <D> ListBoxItem<D> of(final String label, final D data, final boolean selected) {
            return new ListBoxItem<>(label, data, selected);
        }

        public static <D> List<ListBoxItem<D>> of(final List<D> data, final Function<D, String> labelMapper) {
            return data.stream().map(t -> ListBox.ListBoxItem.of(labelMapper.apply(t), t)).collect(Collectors.toList());
        }

        public enum ListBoxItemType {
            ITEM, GROUP;
        }
    }

    public static class GroupListBoxItem<D> extends ListBoxItem<D> {

        private final List<ListBoxItem<D>> groupItems;

        private GroupListBoxItem(final String groupName, final Collection<ListBoxItem<D>> groupItems) {
            super(groupName, null, false);
            this.groupItems = new ArrayList<>(groupItems);
            setGroupName(groupName);
            this.groupItems.parallelStream().forEach(i -> i.setGroupName(groupName));
        }

        public List<ListBoxItem<D>> getGroupItems() {
            return groupItems;
        }

        public void addItem(final ListBoxItem<D> item) {
            item.setGroupName(getGroupName());
            groupItems.add(item);
        }

        @Override
        public ListBoxItemType getType() {
            return ListBoxItemType.GROUP;
        }

        @Override
        public String toString() {
            return "GroupListBoxItem [groupName=" + getGroupName() + "]";
        }

        public static <D> GroupListBoxItem<D> of(final String groupName, final Collection<ListBoxItem<D>> groupItems) {
            return new GroupListBoxItem<>(groupName, groupItems);
        }
    }

    public class ListBoxItemWidget implements IsPWidget {

        private final PPanel panel;
        private PCheckBox checkBox;
        private final ListBoxLabel label;

        private boolean built;
        private ListBoxItem<D> item;
        
        ListBoxItemWidget(Supplier<ListBoxLabel> labelFactory){
        	panel = Element.newPFlowPanel();
            if (configuration.isMultiSelectionEnabled()) checkBox = Element.newPCheckBox();
            label = labelFactory.get();
        }
        
        @Override
        public PWidget asWidget() {
            if (!built) {
                if (checkBox != null) panel.add(checkBox);
                panel.add(label);
                
                label.addClickHandler(e -> {
                    if (ListBoxItemType.GROUP.equals(item.getType())) return;
                    if (!item.isEnabled()) return;
                    final boolean multiSelect = checkBox != null && Boolean.TRUE.equals(!checkBox.getValue());
                    if (multiSelect && !isSelectionAllowed(null)) {
                        return;
                    }
                    select();
                });
                built = true;
            }
            return panel;
        }

        void select() {
            if (checkBox != null) checkBox.setValue(!checkBox.getValue());
            if (configuration.isEventOnlyEnabled()) {
                onValueChange(List.of(item));
            } else {
                onSelectionChange(item, checkBox != null ? checkBox.getValue() : true);
            }
            blur();
            applyCloseOnClickMode();
        }

        void setItem(final ListBoxItem<D> item) {
            this.item = item;
            if (checkBox != null) {
                if (dataProvider != null) checkBox.setValue(selectedDataItems.contains(item));
                else checkBox.setValue(item.isSelected());
                if (item.equals(lastSelectedItem)) panel.addStyleName(STYLE_LISTBOX_ITEM_LAST_SELECTED);
                else panel.removeStyleName(STYLE_LISTBOX_ITEM_LAST_SELECTED);
            } else {
                boolean selected;
                if (dataProvider != null) selected = selectedDataItems.contains(item);
                else selected = item.isSelected();
                if (selected) panel.addStyleName(STYLE_LISTBOX_ITEM_SELECTED);
                else panel.removeStyleName(STYLE_LISTBOX_ITEM_SELECTED);
                if (ListBoxItemType.GROUP.equals(item.getType())) panel.addStyleName(STYLE_LISTBOX_ITEM_GROUP);
                else panel.removeStyleName(STYLE_LISTBOX_ITEM_GROUP);
            }
            label.setText(item.getLabel());
            label.asWidget().setTitle(item.getLabel());
            if (item.isEnabled()) label.asWidget().removeAttribute(DISABLED);
            else label.asWidget().setAttribute(DISABLED);
        }
    }

    public class ListBoxInfiniteScrollProvider implements InfiniteScrollProvider<ListBoxItem<D>, ListBoxItemWidget> {

        @Override
        public void getData(final int beginIndex, final int maxSize, final Consumer<List<ListBoxItem<D>>> callback) {
            if (dataProvider != null) {
                items.clear();
                items.addAll(dataProvider.getData(beginIndex, maxSize, getFilter()));
                visibleItems.addAll(items);
                callback.accept(items);
            } else {
                callback.accept(visibleItems.subList(beginIndex, beginIndex + maxSize));
            }
        }

        @Override
        public void getFullSize(final Consumer<Integer> callback) {
            if (dataProvider != null) callback.accept(dataProvider.getFullDataSize(getFilter()));
            else callback.accept(visibleItems.size());
        }

        @Override
        public ListBoxItemWidget handleUI(final int index, final ListBoxItem<D> item, ListBoxItemWidget widget) {
            if (widget == null) widget = new ListBoxItemWidget(getItemWidgetLabel());
            widget.setItem(item);
            return widget;
        }

        @Override
        public void addHandler(final Consumer<ListBoxItem<D>> handler) {
            // Nothing to do
        }
    }
}
