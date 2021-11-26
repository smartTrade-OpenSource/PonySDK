/*============================================================================
 *
 * Copyright (c) 2000-2021 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.core.ui.dropdown;

import java.util.HashSet;
import java.util.Set;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.model.PKeyCodes;

public abstract class DropDownContainer<V, C extends DropDownContainerConfiguration> implements IsPWidget {

    protected static final String STRING_SPACE = " ";

    protected static final String STYLE_CONTAINER_BUTTON_PLACEHOLDER = "dd-container-button-placeholder";
    protected static final String STYLE_CONTAINER_SELECTED = "dd-container-selected";

    private static final String STYLE_CONTAINER_WIDGET = "dd-container-widget";
    private static final String STYLE_CONTAINER_DISABLED = "dd-container-disabled";
    private static final String STYLE_CONTAINER_BUTTON = "dd-container-button";
    private static final String STYLE_CONTAINER_STATE = "dd-container-state";
    private static final String STYLE_CONTAINER_CLEAR = "dd-container-clear";
    private static final String STYLE_CONTAINER_CLEAR_DISABLED = "dd-container-clear-disabled";
    private static final String STYLE_CONTAINER_OPENED = "dd-container-opened";
    private static final String STYLE_CONTAINER_WIDGET_OPENED = "dd-container-widget-opened";
    private static final String STYLE_CONTAINER_ADDON = "dd-container-addon";
    private static final String STYLE_CONTAINER_CUSTOM = "dd-container-custom";
    private static final String STYLE_CONTAINER_DEFAULT = "dd-container-default";

    private static final String ATTRIBUTE_ID = "id";

    protected PButton clearTitleButton;
    protected PButton mainButton;
    protected PButton stateButton;

    private IsPWidget customContainer;

    private boolean initialized;

    protected final C configuration;

    private final PPanel widget;
    private final DropDownContainerAddon container;
    private final Set<DropDownContainerListener> listeners;

    public DropDownContainer(final C configuration) {
        this.configuration = configuration;
        this.listeners = new HashSet<>();
        this.widget = Element.newPFlowPanel();
        this.widget.addStyleName(STYLE_CONTAINER_WIDGET);
        final String parentId = widget.getID() + "";
        this.widget.setAttribute(ATTRIBUTE_ID, parentId);
        this.container = new DropDownContainerAddon(parentId);
        this.container.addStyleName(STYLE_CONTAINER_ADDON);
    }

    public abstract V getValue();

    public abstract void setValue(V value);

    @Override
    public PWidget asWidget() {
        if (!initialized) {
            widget.setTabindex(TabindexMode.TABULABLE);
            widget.addKeyDownHandler(e -> {
                if (e.getKeyCode() == PKeyCodes.ENTER.getCode()) {
                    setContainerVisible(!container.isVisible());
                } else if (e.getKeyCode() == PKeyCodes.TAB.getCode() && isOpen()) {
                    onFocusWhenOpened();
                }
            });
            mainButton = Element.newPButton(configuration.getTitle());
            mainButton.addStyleName(STYLE_CONTAINER_BUTTON);
            mainButton.setTabindex(TabindexMode.FOCUSABLE);
            if (configuration.isTitleDisplayed() && configuration.isTitlePlaceHolder()) {
                mainButton.addStyleName(STYLE_CONTAINER_BUTTON_PLACEHOLDER);
            }
            widget.add(mainButton);

            stateButton = Element.newPButton();
            stateButton.addStyleName(STYLE_CONTAINER_STATE);
            stateButton.setTabindex(TabindexMode.FOCUSABLE);
            widget.add(stateButton);

            if (configuration.isClearTitleButtonEnabled()) {
                clearTitleButton = Element.newPButton();
                clearTitleButton.addStyleName(STYLE_CONTAINER_CLEAR);
                clearTitleButton.setTitle(configuration.getClearLabel());
                clearTitleButton.addClickHandler(e -> {
                    if (isEnabled()) {
                        setValue(null);
                        listeners.forEach(l -> l.onClearTitleClicked());
                        onValueChange();
                    }
                });
                widget.add(clearTitleButton);
                setClearTitleButtonVisible(false);
            } else {
                widget.addStyleName(STYLE_CONTAINER_CLEAR_DISABLED);
            }
            widget.addInitializeListener(e -> {
                widget.getWindow().getPRootPanel().add(container);
                updateTitle(getValue());
            });

            if (customContainer != null) {
                container.add(customContainer);
                customContainer.asWidget().addStyleName(STYLE_CONTAINER_CUSTOM);
            }

            final PWidget defaultContainer = createDefaultContainer();
            defaultContainer.addStyleName(STYLE_CONTAINER_DEFAULT);
            container.add(defaultContainer);

            final PClickHandler clickHandler = e -> {
                setContainerVisible(!container.isVisible());
            };
            mainButton.addClickHandler(clickHandler);
            stateButton.addClickHandler(clickHandler);
            container.addListener(() -> setContainerVisible(false));
            initialized = true;
        }
        return widget;
    }

    public void focus() {
        widget.focus();
    }

    public C getConfiguration() {
        return configuration;
    }

    public void open() {
        setContainerVisible(true);
    }

    public void close() {
        setContainerVisible(false);
    }

    public void updateContainerPosition() {
        container.updatePosition();
    }

    public void forceUpdateTitle() {
        updateTitle(getValue());
    }

    public boolean isEnabled() {
        return !widget.hasStyleName(STYLE_CONTAINER_DISABLED);
    }

    public void setEnabled(final boolean enabled) {
        if (enabled) {
            widget.removeStyleName(STYLE_CONTAINER_DISABLED);
            widget.setTabindex(TabindexMode.TABULABLE);
        } else {
            widget.addStyleName(STYLE_CONTAINER_DISABLED);
            widget.setTabindex(TabindexMode.FOCUSABLE);
        }
    }

    public void setClearTitleButtonVisible(final boolean visible) {
        if (clearTitleButton != null) clearTitleButton.setVisible(visible);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isOpen() {
        return widget.hasStyleName(STYLE_CONTAINER_OPENED);
    }

    public void addStyleName(final String styleName) {
        widget.addStyleName(styleName);
    }

    public void removeStyleName(final String styleName) {
        widget.removeStyleName(styleName);
    }

    public void addContainerStyleName(final String styleName) {
        container.addStyleName(styleName);
    }

    public void setCustomContainer(final IsPWidget customContainer) {
        if (initialized) throw new IllegalArgumentException("Must be set before asWidget call");
        this.customContainer = customContainer;
    }

    public void onFocusWhenOpened() {
        // Nothing to do by default
    }

    public void setDefaultContainerEnabled(final boolean enabled) {
        // Nothing to do by default
    }

    protected void beforeContainerVisible() {
        // Nothing to do by default
    }

    protected void afterContainerVisible() {
        // Nothing to do by default
    }

    protected void afterContainerClose() {
        // Nothing to do by default
    }

    public void addListener(final DropDownContainerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final DropDownContainerListener listener) {
        listeners.remove(listener);
    }

    protected abstract PWidget createDefaultContainer();

    protected abstract boolean isValueEmpty(V value);

    protected void updateTitle(final V value) {
        if (!isInitialized()) return;
        final StringBuilder text = new StringBuilder();
        if (configuration.isTitleDisplayed()) {
            if (configuration.isTitlePlaceHolder()) {
                if (value == null || isValueEmpty(value)) {
                    mainButton.addStyleName(STYLE_CONTAINER_BUTTON_PLACEHOLDER);
                    text.append(configuration.getTitle());
                } else {
                    mainButton.removeStyleName(STYLE_CONTAINER_BUTTON_PLACEHOLDER);
                }
            } else {
                text.append(configuration.getTitle());
                text.append(STRING_SPACE);
                text.append(configuration.getTitleSeparator());
                text.append(STRING_SPACE);
            }
        }
        if (value == null || isValueEmpty(value)) {
            if (!configuration.isTitlePlaceHolder() && configuration.isSelectionDisplayed()) text.append(configuration.getAllLabel());
            final String display = text.toString();
            mainButton.setText(display);
            mainButton.setTitle(display);
            stateButton.setTitle(display);
            widget.removeStyleName(STYLE_CONTAINER_SELECTED);
        } else {
            if (configuration.isSelectionDisplayed()) appendDisplayValue(text, value);
            final String display = text.toString();
            mainButton.setText(display);
            mainButton.setTitle(display);
            stateButton.setTitle(display);
            widget.addStyleName(STYLE_CONTAINER_SELECTED);
        }
        setClearTitleButtonVisible(value != null && !isValueEmpty(value));
    }

    protected void appendDisplayValue(final StringBuilder text, final V value) {
        text.append(value.toString());
    }

    protected void onValueChange() {
        listeners.forEach(l -> l.onValueChange());
    }

    private void setContainerVisible(final boolean visible) {
        if (!isEnabled()) return;
        if (visible && !isOpen()) {
            widget.addStyleName(STYLE_CONTAINER_OPENED);
            container.addStyleName(STYLE_CONTAINER_WIDGET_OPENED);
            beforeContainerVisible();
            container.show();
            afterContainerVisible();
        } else if (!visible && isOpen()) {
            widget.removeStyleName(STYLE_CONTAINER_OPENED);
            container.removeStyleName(STYLE_CONTAINER_WIDGET_OPENED);
            container.hide();
            updateTitle(getValue());
            afterContainerClose();
            listeners.forEach(l -> l.onClose());
        }
    }

    //

    public interface DropDownContainerListener {

        void onValueChange();

        void onClearTitleClicked();

        void onClose();

    }
}
