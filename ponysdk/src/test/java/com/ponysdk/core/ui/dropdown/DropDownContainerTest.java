/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.core.ui.dropdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.basic.event.POpenHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.dropdown.DropDownContainer.DropDownContainerListener;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

public class DropDownContainerTest extends PSuite {

    private static final String TITLE = "Title";

    private RecordingModelWriter writer;

    @BeforeEach
    public void setUp() {
        writer = RecordingModelWriter.install();
    }

    @AfterEach
    public void tearDown() {
        PWindow.getMain().getPRootPanel().clear();
        RecordingModelWriter.uninstall();
    }

    @Test
    public void asWidgetBuildsButtonsOnce() {
        final TestDropDown dropDown = new TestDropDown(titled());
        assertFalse(dropDown.isInitialized());

        final PFlowPanel widget = dropDown.asWidget();

        assertTrue(dropDown.isInitialized());
        assertSame(widget, dropDown.asWidget());
        assertTrue(widget.hasStyleName("dd-container-widget"));
        assertEquals(String.valueOf(widget.getID()), widget.getAttribute("id"));
        assertEquals(TabindexMode.TABULABLE.getTabIndex(), widget.getTabindex());
        assertEquals(3, widget.getWidgetCount());
        assertSame(dropDown.mainButton, widget.getWidget(0));
        assertSame(dropDown.stateButton, widget.getWidget(1));
        assertSame(dropDown.clearTitleButton, widget.getWidget(2));
        assertEquals(TITLE, dropDown.mainButton.getText());
        assertTrue(dropDown.mainButton.hasStyleName("dd-container-button"));
        assertTrue(dropDown.mainButton.hasStyleName(DropDownContainer.STYLE_CONTAINER_BUTTON_PLACEHOLDER));
        assertTrue(dropDown.stateButton.hasStyleName("dd-container-state"));
        assertFalse(dropDown.clearTitleButton.isVisible());
        assertFalse(widget.hasStyleName("dd-container-clear-disabled"));
        assertTrue(dropDown.defaultContainer.hasStyleName("dd-container-default"));
        assertSame(dropDown.getContainer().asWidget(), dropDown.defaultContainer.getParent());
    }

    @Test
    public void clearButtonDisabledByConfiguration() {
        final TestDropDown dropDown = new TestDropDown(withoutClearButton());
        final PFlowPanel widget = dropDown.asWidget();

        assertEquals(2, widget.getWidgetCount());
        assertNull(dropDown.clearTitleButton);
        assertTrue(widget.hasStyleName("dd-container-clear-disabled"));
        dropDown.setClearTitleButtonVisible(true);
    }

    @Test
    public void stopClickEventIsSentForButtons() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.enableStopClickEvent();
        final TestDropDown dropDown = attach(new TestDropDown(configuration));

        assertEquals(List.of(PEventType.ONCLICK.getCode()), writer.values(dropDown.mainButton, ServerToClientModel.STOP_EVENT));
        assertEquals(List.of(PEventType.ONCLICK.getCode()), writer.values(dropDown.stateButton, ServerToClientModel.STOP_EVENT));
    }

    @Test
    public void openAndCloseNotifyHandlersAndToggleStyles() {
        final TestDropDown dropDown = attach(new TestDropDown(titled()));
        final List<String> events = new ArrayList<>();
        final POpenHandler openHandler = e -> events.add("open");
        final PCloseHandler closeHandler = e -> events.add("close");
        dropDown.addOpenHandler(openHandler);
        dropDown.addCloseHandler(closeHandler);

        dropDown.open();
        assertTrue(dropDown.isOpen());
        assertTrue(dropDown.getContainer().isVisible());
        assertTrue(dropDown.asWidget().hasStyleName("dd-container-opened"));
        assertTrue(dropDown.getContainer().asWidget().hasStyleName("dd-container-widget-opened"));
        assertEquals(List.of("before", "after"), dropDown.hooks);

        dropDown.open();
        assertEquals(List.of("open"), events);

        dropDown.setValue("Selection");
        dropDown.close();
        assertFalse(dropDown.isOpen());
        assertFalse(dropDown.getContainer().isVisible());
        assertFalse(dropDown.getContainer().asWidget().hasStyleName("dd-container-widget-opened"));
        assertEquals(List.of("open", "close"), events);
        assertEquals(List.of("before", "after", "afterClose"), dropDown.hooks);
        // Closing refreshes the title from the current value
        assertEquals("Selection", dropDown.mainButton.getText());

        dropDown.removeOpenHandler(openHandler);
        dropDown.removeCloseHandler(closeHandler);
        dropDown.open();
        dropDown.close();
        assertEquals(List.of("open", "close"), events);
    }

    @Test
    public void disabledDropDownDoesNotOpen() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));

        dropDown.setEnabled(false);
        assertFalse(dropDown.isEnabled());
        assertTrue(dropDown.asWidget().hasStyleName("dd-container-disabled"));
        assertEquals(TabindexMode.FOCUSABLE.getTabIndex(), dropDown.asWidget().getTabindex());
        dropDown.open();
        assertFalse(dropDown.isOpen());

        dropDown.setEnabled(true);
        assertTrue(dropDown.isEnabled());
        assertEquals(TabindexMode.TABULABLE.getTabIndex(), dropDown.asWidget().getTabindex());
        dropDown.open();
        assertTrue(dropDown.isOpen());
    }

    @Test
    public void disabledOpenDropDownCanStillBeClosed() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        final List<String> events = new ArrayList<>();
        dropDown.addCloseHandler(e -> events.add("close"));
        dropDown.open();
        dropDown.setEnabled(false);

        DropDownTestEvents.keyDown(dropDown.asWidget(), PKeyCodes.ESCAPE);

        assertFalse(dropDown.isOpen());
        assertEquals(List.of("close"), events);
        dropDown.open();
        assertFalse(dropDown.isOpen());
    }

    @Test
    public void closingAClosedDropDownDoesNothing() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        final List<String> events = new ArrayList<>();
        dropDown.addCloseHandler(e -> events.add("close"));

        dropDown.close();

        assertTrue(events.isEmpty());
        assertTrue(dropDown.hooks.isEmpty());
    }

    @Test
    public void hiddenTitleHasNoPlaceholderStyle() {
        final DefaultDropDownContainerConfiguration configuration = titled();
        configuration.setTitleDisplayed(false);
        final TestDropDown dropDown = attach(new TestDropDown(configuration));

        assertFalse(dropDown.mainButton.hasStyleName(DropDownContainer.STYLE_CONTAINER_BUTTON_PLACEHOLDER));
        assertEquals("", dropDown.mainButton.getText());
    }

    @Test
    public void emptyValueWithoutSelectionDisplayedShowsTitleOnly() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.setTitle(TITLE).setAllLabel("All").setTitleSeparator("-").disableTitlePlaceHolder();
        configuration.setSelectionDisplayed(false);
        final TestDropDown dropDown = attach(new TestDropDown(configuration));

        dropDown.setValue("");
        dropDown.forceUpdateTitle();

        assertEquals("Title - ", dropDown.mainButton.getText());
        assertFalse(dropDown.asWidget().hasStyleName(DropDownContainer.STYLE_CONTAINER_SELECTED));
    }

    @Test
    public void clickOnButtonsTogglesContainer() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));

        PEmulator.click(dropDown.mainButton);
        assertTrue(dropDown.isOpen());
        PEmulator.click(dropDown.stateButton);
        assertFalse(dropDown.isOpen());
    }

    @Test
    public void clickOnOpenedMultilevelDropDownKeepsItOpen() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.enableMultilevel();
        final TestDropDown dropDown = attach(new TestDropDown(configuration));

        PEmulator.click(dropDown.mainButton);
        assertTrue(dropDown.isOpen());
        PEmulator.click(dropDown.mainButton);
        assertTrue(dropDown.isOpen());
    }

    @Test
    public void clickOnAMultilevelDropDownWhoseContainerTheApplicationShowedLeavesItAsItIs() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.enableMultilevel();
        final TestDropDown dropDown = attach(new TestDropDown(configuration));
        dropDown.getContainer().show();

        PEmulator.click(dropDown.mainButton);

        assertFalse(dropDown.isOpen());
        assertTrue(dropDown.getContainer().isVisible());
    }

    @Test
    public void placeholderTitle() {
        final TestDropDown dropDown = attach(new TestDropDown(titled()));

        assertEquals(TITLE, dropDown.mainButton.getText());
        assertEquals(TITLE, dropDown.mainButton.getTitle());
        assertEquals(TITLE, dropDown.stateButton.getTitle());
        assertFalse(dropDown.asWidget().hasStyleName(DropDownContainer.STYLE_CONTAINER_SELECTED));

        dropDown.setValue("Value");
        dropDown.forceUpdateTitle();
        assertEquals("Value", dropDown.mainButton.getText());
        assertEquals("Value", dropDown.stateButton.getTitle());
        assertFalse(dropDown.mainButton.hasStyleName(DropDownContainer.STYLE_CONTAINER_BUTTON_PLACEHOLDER));
        assertTrue(dropDown.asWidget().hasStyleName(DropDownContainer.STYLE_CONTAINER_SELECTED));
        assertTrue(dropDown.clearTitleButton.isVisible());

        dropDown.setValue("");
        dropDown.forceUpdateTitle();
        assertEquals(TITLE, dropDown.mainButton.getText());
        assertTrue(dropDown.mainButton.hasStyleName(DropDownContainer.STYLE_CONTAINER_BUTTON_PLACEHOLDER));
        assertFalse(dropDown.asWidget().hasStyleName(DropDownContainer.STYLE_CONTAINER_SELECTED));
        assertFalse(dropDown.clearTitleButton.isVisible());
    }

    @Test
    public void titleWithSeparatorWhenPlaceholderDisabled() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.setTitle(TITLE).setAllLabel("All").setTitleSeparator("-").disableTitlePlaceHolder();
        final TestDropDown dropDown = attach(new TestDropDown(configuration));

        assertEquals("Title - All", dropDown.mainButton.getText());
        assertFalse(dropDown.mainButton.hasStyleName(DropDownContainer.STYLE_CONTAINER_BUTTON_PLACEHOLDER));

        dropDown.setValue("Value");
        dropDown.forceUpdateTitle();
        assertEquals("Title - Value", dropDown.mainButton.getText());

        configuration.setSelectionDisplayed(false);
        dropDown.forceUpdateTitle();
        assertEquals("Title - ", dropDown.mainButton.getText());

        configuration.setTitleDisplayed(false);
        dropDown.forceUpdateTitle();
        assertEquals("", dropDown.mainButton.getText());
    }

    @Test
    public void eventOnlyModeNeverUpdatesTitle() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.setTitle(TITLE).enabledEventOnly();
        final TestDropDown dropDown = attach(new TestDropDown(configuration));
        dropDown.setValue("Value");

        dropDown.forceUpdateTitle();
        dropDown.open();
        dropDown.close();

        assertEquals(TITLE, dropDown.mainButton.getText());
    }

    @Test
    public void updateTitleBeforeInitializationIsIgnored() {
        final TestDropDown dropDown = new TestDropDown(new DefaultDropDownContainerConfiguration());
        dropDown.setValue("Value");
        dropDown.forceUpdateTitle();
        assertNull(dropDown.mainButton);
    }

    @Test
    public void clearButtonResetsValueAndNotifies() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        dropDown.setValue("Value");
        dropDown.forceUpdateTitle();
        assertTrue(dropDown.clearTitleButton.isVisible());
        final List<String> values = new ArrayList<>();
        final AtomicInteger clearCount = new AtomicInteger();
        final PValueChangeHandler<String> handler = e -> values.add(e.getData());
        final DropDownContainerListener listener = clearCount::incrementAndGet;
        dropDown.addValueChangeHandler(handler);
        dropDown.addClearSelectionListener(listener);
        assertTrue(dropDown.getValueChangeHandlers().contains(handler));

        PEmulator.click(dropDown.clearTitleButton);
        assertNull(dropDown.getValue());
        assertEquals(1, clearCount.get());
        assertEquals(1, values.size());
        assertNull(values.get(0));

        dropDown.setValue("Value");
        dropDown.setEnabled(false);
        PEmulator.click(dropDown.clearTitleButton);
        assertEquals("Value", dropDown.getValue());

        dropDown.setEnabled(true);
        dropDown.removeValueChangeHandler(handler);
        dropDown.removeClearSelectionListener(listener);
        PEmulator.click(dropDown.clearTitleButton);
        assertNull(dropDown.getValue());
        assertEquals(1, clearCount.get());
        assertEquals(1, values.size());
    }

    @Test
    public void enterKeyTogglesContainerWhenFocused() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        final PFlowPanel widget = dropDown.asWidget();

        // Without focus, ENTER is forwarded as a regular key
        DropDownTestEvents.keyDown(widget, PKeyCodes.ENTER);
        assertFalse(dropDown.isOpen());
        assertEquals(List.of(PKeyCodes.ENTER.getCode()), dropDown.keys);

        DropDownTestEvents.focus(widget);
        assertTrue(dropDown.isFocused());
        assertEquals(1, dropDown.focusCount);

        DropDownTestEvents.keyDown(widget, PKeyCodes.ENTER);
        assertTrue(dropDown.isOpen());
        DropDownTestEvents.keyDown(widget, PKeyCodes.ENTER);
        assertFalse(dropDown.isOpen());
        assertEquals(List.of(Boolean.TRUE), writer.values(widget, ServerToClientModel.FOCUS));

        dropDown.setStopKeys(true);
        DropDownTestEvents.keyDown(widget, PKeyCodes.ENTER);
        assertFalse(dropDown.isOpen());
        assertEquals(List.of(PKeyCodes.ENTER.getCode(), PKeyCodes.ENTER.getCode()), dropDown.keys);
    }

    @Test
    public void escapeKeyClosesContainer() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        dropDown.open();

        DropDownTestEvents.keyDown(dropDown.asWidget(), PKeyCodes.ESCAPE);

        assertFalse(dropDown.isOpen());
        assertEquals(List.of(Boolean.TRUE), writer.values(dropDown.asWidget(), ServerToClientModel.FOCUS));
        assertTrue(dropDown.keys.isEmpty());
    }

    @Test
    public void otherKeysAreForwardedToContainer() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));

        DropDownTestEvents.keyDown(dropDown.asWidget(), PKeyCodes.DOWN);

        assertEquals(List.of(PKeyCodes.DOWN.getCode()), dropDown.keys);
    }

    @Test
    public void hiddenWidgetClosesContainer() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        dropDown.open();

        DropDownTestEvents.visibility(dropDown.asWidget(), true);
        assertTrue(dropDown.isOpen());
        DropDownTestEvents.visibility(dropDown.asWidget(), false);
        assertFalse(dropDown.isOpen());
    }

    @Test
    public void blurOnMultilevelDropDownKeepsItOpen() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.enableMultilevel();
        final TestDropDown dropDown = attach(new TestDropDown(configuration));
        DropDownTestEvents.focus(dropDown.asWidget());
        dropDown.open();

        DropDownTestEvents.blur(dropDown.asWidget());

        assertFalse(dropDown.isFocused());
        assertTrue(dropDown.isOpen());
        assertEquals(0, dropDown.focusContainerCount);
    }

    @Test
    public void blurClosesSingleLevelDropDown() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        dropDown.onBlur();
        assertFalse(dropDown.isOpen());

        dropDown.open();
        dropDown.onBlur();
        assertFalse(dropDown.isOpen());
    }

    @Test
    public void blurFocusesFocusableContainer() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        dropDown.containerFocusable = true;
        dropDown.open();

        dropDown.onBlur();

        assertTrue(dropDown.isOpen());
        assertEquals(1, dropDown.focusContainerCount);
    }

    @Test
    public void containerHideEventClosesDropDown() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        dropDown.open();

        DropDownTestEvents.windowEvent(dropDown.getContainer(), "scroll");

        assertFalse(dropDown.isOpen());
        assertFalse(dropDown.getContainer().isVisible());
    }

    @Test
    public void destroyClearsHandlersAndRemovesContainer() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        final PValueChangeHandler<String> handler = e -> {
        };
        dropDown.addValueChangeHandler(handler);
        dropDown.open();
        assertTrue(dropDown.getContainer().asWidget().getParent() != null);

        dropDown.asWidget().onDestroy();

        assertTrue(dropDown.getValueChangeHandlers().isEmpty());
        assertNull(dropDown.getContainer().asWidget().getParent());
    }

    @Test
    public void customContainerIsAddedBeforeDefaultContainer() {
        final TestDropDown dropDown = new TestDropDown(new DefaultDropDownContainerConfiguration());
        final PLabel custom = Element.newPLabel();
        dropDown.setCustomContainer(custom);
        dropDown.setDefaultContainerEnabled(false);

        dropDown.asWidget();

        assertTrue(custom.hasStyleName("dd-container-custom"));
        assertSame(dropDown.getContainer().asWidget(), custom.getParent());
        assertThrows(IllegalArgumentException.class, () -> dropDown.setCustomContainer(Element.newPLabel()));
    }

    @Test
    public void styleNamesAndVisibilityAreDelegated() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));

        dropDown.addStyleName("custom");
        assertTrue(dropDown.asWidget().hasStyleName("custom"));
        dropDown.removeStyleName("custom");
        assertFalse(dropDown.asWidget().hasStyleName("custom"));

        dropDown.addContainerStyleName("custom-container");
        assertTrue(dropDown.getContainer().asWidget().hasStyleName("custom-container"));
        dropDown.removeContainerStyleName("custom-container");
        assertFalse(dropDown.getContainer().asWidget().hasStyleName("custom-container"));

        assertTrue(dropDown.isVisible());
        dropDown.setVisible(false);
        assertFalse(dropDown.isVisible());
        assertEquals(List.of(Boolean.FALSE), writer.values(dropDown.asWidget(), ServerToClientModel.WIDGET_VISIBLE));

        dropDown.blur();
        assertEquals(List.of(Boolean.FALSE), writer.values(dropDown.asWidget(), ServerToClientModel.FOCUS));
    }

    @Test
    public void positionCallsAreSentToContainer() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.setPositionDropRight(true);
        final TestDropDown dropDown = attach(new TestDropDown(configuration));
        assertSame(configuration, dropDown.getConfiguration());
        dropDown.open();
        writer.clear();

        dropDown.updateContainerPosition();
        dropDown.adjustContainerPosition();
        dropDown.disableSpaceWhenOpened();

        assertEquals(List.of("updatePosition", "adjustPosition", "disableSpaceWhenOpened"), writer.addonMethods(dropDown.getContainer()));
    }

    @Test
    public void dropRightIsRequestedAtCreation() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.setPositionDropRight(true);
        final TestDropDown dropDown = attach(new TestDropDown(configuration));

        dropDown.open();

        assertTrue(writer.addonMethods(dropDown.getContainer()).contains("setDropRight"));
    }

    @Test
    public void multiLevelParentIsDefinedOnChildContainer() {
        final TestDropDown parent = new TestDropDown(new DefaultDropDownContainerConfiguration());
        final TestDropDown child = new TestDropDown(new DefaultDropDownContainerConfiguration());

        parent.defineAsMultiLevelParent(child);

        assertEquals(String.valueOf(parent.getContainer().asWidget().getID()), child.getContainer().asWidget().getAttribute("multilvl-parent"));
    }

    @Test
    public void keyDownHandlerOnContainerCanForceFocus() {
        final TestDropDown dropDown = attach(new TestDropDown(new DefaultDropDownContainerConfiguration()));
        dropDown.open();
        final List<Integer> keys = new ArrayList<>();
        final PWidget container = dropDown.getContainer().asWidget();

        dropDown.addKeyDownHandler(true, e -> keys.add(e.getKeyCode()));
        assertEquals(TabindexMode.TABULABLE.getTabIndex(), container.getTabindex());
        assertEquals(List.of(Boolean.TRUE), writer.values(container, ServerToClientModel.FOCUS));
        DropDownTestEvents.keyDown(container, PKeyCodes.UP);
        assertEquals(List.of(PKeyCodes.UP.getCode()), keys);

        dropDown.removeTabIndex();
        assertEquals(TabindexMode.FOCUSABLE.getTabIndex(), container.getTabindex());
    }

    @Test
    public void defaultHooksAndDisplayValue() {
        final BareDropDown dropDown = attach(new BareDropDown(titled()));

        dropDown.value = 42;
        dropDown.forceUpdateTitle();
        assertEquals("42", dropDown.mainButton.getText());

        DropDownTestEvents.focus(dropDown.asWidget());
        DropDownTestEvents.keyDown(dropDown.asWidget(), PKeyCodes.DOWN);
        dropDown.open();
        dropDown.onBlur();
        assertFalse(dropDown.isOpen());
    }

    @Test
    public void defaultFocusContainerWritesNothing() {
        final BareDropDown dropDown = attach(new BareDropDown(new DefaultDropDownContainerConfiguration()));
        dropDown.open();
        writer.clear();

        dropDown.focusContainer();

        assertFalse(dropDown.isContainerFocusable());
        assertTrue(writer.values(dropDown.getContainer().asWidget(), ServerToClientModel.FOCUS).isEmpty());
        assertTrue(writer.values(dropDown.asWidget(), ServerToClientModel.FOCUS).isEmpty());
    }

    private static DefaultDropDownContainerConfiguration titled() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.setTitle(TITLE);
        return configuration;
    }

    private static DefaultDropDownContainerConfiguration withoutClearButton() {
        final DefaultDropDownContainerConfiguration configuration = new DefaultDropDownContainerConfiguration();
        configuration.disableClearTitleButton();
        return configuration;
    }

    private static <T extends DropDownContainer<?, ?>> T attach(final T dropDown) {
        PWindow.getMain().add(dropDown.asWidget());
        return dropDown;
    }

    /**
     * Drop down over a plain String value, recording the hooks it receives.
     */
    private static class TestDropDown extends DropDownContainer<String, DefaultDropDownContainerConfiguration> {

        private final List<String> hooks = new ArrayList<>();
        private final List<Integer> keys = new ArrayList<>();
        private PFlowPanel defaultContainer;
        private String value;
        private int focusCount;
        private int focusContainerCount;
        private boolean containerFocusable;

        TestDropDown(final DefaultDropDownContainerConfiguration configuration) {
            super(configuration);
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(final String value) {
            this.value = value;
        }

        @Override
        protected PWidget createDefaultContainer() {
            defaultContainer = Element.newPFlowPanel();
            return defaultContainer;
        }

        @Override
        protected boolean isValueEmpty(final String value) {
            return value.isEmpty();
        }

        @Override
        protected void beforeContainerVisible() {
            hooks.add("before");
        }

        @Override
        protected void afterContainerVisible() {
            hooks.add("after");
        }

        @Override
        protected void afterContainerClose() {
            hooks.add("afterClose");
        }

        @Override
        protected void onFocus() {
            focusCount++;
        }

        @Override
        protected void onContainerKeyDown(final int keyCode) {
            keys.add(keyCode);
        }

        @Override
        protected boolean isContainerFocusable() {
            return containerFocusable;
        }

        @Override
        protected void focusContainer() {
            focusContainerCount++;
        }
    }

    /**
     * Drop down relying on every default hook of {@link DropDownContainer}.
     */
    private static class BareDropDown extends DropDownContainer<Integer, DefaultDropDownContainerConfiguration> {

        private Integer value;

        BareDropDown(final DefaultDropDownContainerConfiguration configuration) {
            super(configuration);
        }

        @Override
        public Integer getValue() {
            return value;
        }

        @Override
        public void setValue(final Integer value) {
            this.value = value;
        }

        @Override
        protected PWidget createDefaultContainer() {
            return Element.newPFlowPanel();
        }

        @Override
        protected boolean isValueEmpty(final Integer value) {
            return false;
        }
    }
}
