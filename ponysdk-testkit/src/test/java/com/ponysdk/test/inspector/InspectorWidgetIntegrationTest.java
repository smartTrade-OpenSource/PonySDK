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

import static com.ponysdk.test.inspector.predicate.Predicates.style;
import static com.ponysdk.test.inspector.predicate.Predicates.text;
import static com.ponysdk.test.inspector.predicate.Predicates.type;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.listbox.ListBox;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.ui.listbox.ListBoxConfiguration;
import com.ponysdk.test.PSuite;
import com.ponysdk.test.inspector.predicate.Predicates;

import org.junit.After;
import org.junit.Test;

/**
 * Integration tests exercising the full testkit workflow against real PonySDK widgets attached
 * to the mocked {@link PWindow#getMain() main window} provided by {@link PSuite}.
 *
 * <p>Each test builds a widget tree, wraps the root in an {@link InspectorWidget} and drives it
 * through the public API (traversal, state queries, actions, specialized inspectors). The goal
 * is to verify that the testkit behaves end-to-end in the same environment that a consumer's
 * tests will run in, covering requirements 7.1, 7.3, 8.1, 8.2, 8.3 and 8.4.</p>
 */
public class InspectorWidgetIntegrationTest extends PSuite {

    @After
    public void tearDown() {
        PWindow.getMain().getPRootPanel().clear();
    }

    // =====================================================================================
    // Predicate traversal
    // =====================================================================================

    @Test
    public void shouldFindWidgetByStyle() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel plain = Element.newPLabel("plain");
        final PLabel styled = Element.newPLabel("styled");
        styled.addStyleName("my-label");
        panel.add(plain);
        panel.add(styled);
        PWindow.getMain().add(panel);

        final InspectorWidget found = InspectorWidget.of(panel).find(style("my-label"));

        assertSame(styled, found.getWidget());
    }

    @Test
    public void shouldFindWidgetByType() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel("hello");
        final PButton button = Element.newPButton("click");
        final PTextBox textBox = Element.newPTextBox();
        panel.add(label);
        panel.add(button);
        panel.add(textBox);
        PWindow.getMain().add(panel);

        final InspectorWidget found = InspectorWidget.of(panel).find(type(PButton.class));

        assertSame(button, found.getWidget());
    }

    @Test
    public void shouldFindWidgetByText() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel hello = Element.newPLabel("Hello");
        final PLabel world = Element.newPLabel("World");
        panel.add(hello);
        panel.add(world);
        PWindow.getMain().add(panel);

        final InspectorWidget found = InspectorWidget.of(panel).find(text("Hello"));

        assertSame(hello, found.getWidget());
    }

    @Test
    public void shouldFailFindWithDescriptiveError() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel("Hello");
        label.addStyleName("existing-style");
        panel.add(label);
        PWindow.getMain().add(panel);

        try {
            InspectorWidget.of(panel).find(style("missing-style"));
            fail("Expected AssertionError when no widget matches");
        } catch (final AssertionError e) {
            final String message = e.getMessage();
            assertNotNull("AssertionError must carry a message", message);
            // Predicate description is included.
            assertTrue("Message should mention the predicate, got: " + message,
                    message.contains("style(missing-style)"));
            // Hierarchy dump is included (shows the PLabel we added).
            assertTrue("Message should include the hierarchy dump, got: " + message,
                    message.contains("Visible hierarchy:"));
            assertTrue("Hierarchy dump should mention PLabel, got: " + message,
                    message.contains("PLabel"));
            assertTrue("Hierarchy dump should include the label's style, got: " + message,
                    message.contains("existing-style"));
        }
    }

    @Test
    public void shouldSkipInvisibleSubtrees() {
        final PFlowPanel panel = Element.newPFlowPanel();

        final PButton visibleButton = Element.newPButton("visible");
        panel.add(visibleButton);

        final PFlowPanel hiddenBranch = Element.newPFlowPanel();
        final PButton hiddenButton = Element.newPButton("hidden");
        hiddenBranch.add(hiddenButton);
        hiddenBranch.setVisible(false);
        panel.add(hiddenBranch);

        PWindow.getMain().add(panel);

        final List<InspectorWidget> buttons = InspectorWidget.of(panel).findAll(type(PButton.class));

        assertEquals(1, buttons.size());
        assertSame(visibleButton, buttons.get(0).getWidget());
    }

    // =====================================================================================
    // User action simulation
    // =====================================================================================

    @Test
    public void shouldClickButton_TriggersHandler() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PButton button = Element.newPButton("click me");
        final boolean[] clicked = { false };
        button.addClickHandler(event -> clicked[0] = true);
        panel.add(button);
        PWindow.getMain().add(panel);

        InspectorWidget.of(button).click();

        assertTrue("Click handler should have been invoked", clicked[0]);
    }

    @Test
    public void shouldTypeIntoTextBox_UpdatesValue() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PTextBox textBox = Element.newPTextBox();
        panel.add(textBox);
        PWindow.getMain().add(panel);

        InspectorWidget.of(textBox).type("hello");

        assertEquals("hello", textBox.getText());
    }

    @Test
    public void shouldThrowOnDisabledWidget() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PButton button = Element.newPButton("disabled");
        button.setEnabled(false);
        panel.add(button);
        PWindow.getMain().add(panel);

        try {
            InspectorWidget.of(button).click();
            fail("Expected AssertionError when clicking a disabled widget");
        } catch (final AssertionError e) {
            assertTrue("Error message should explain the disabled state, got: " + e.getMessage(),
                    e.getMessage().contains("Cannot interact with disabled widget"));
        }
    }

    // =====================================================================================
    // PSuite / PWindow integration
    // =====================================================================================

    @Test
    public void shouldWorkWithWidgetAttachedToPWindow() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel("hello");
        label.addStyleName("greeting");
        panel.add(label);
        PWindow.getMain().add(panel);

        // Traversal must be reachable from the root panel, not just from our local panel.
        final InspectorWidget root = InspectorWidget.of(PWindow.getMain().getPRootPanel());
        final InspectorWidget found = root.find(style("greeting"));

        assertSame(label, found.getWidget());
        assertEquals("hello", found.getText());
        assertTrue(found.isVisible());
    }

    // =====================================================================================
    // InspectorListBox
    // =====================================================================================

    @Test
    public void shouldFindListBoxViaFactory() {
        final ListBox<String> listBox = newListBox("A", "B", "C");
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(listBox.asWidget());
        PWindow.getMain().add(panel);

        final InspectorListBox inspector = InspectorWidget.of(panel).find(InspectorListBox.FACTORY);

        assertNotNull(inspector);
        assertSame(listBox.asWidget(), inspector.getWidget());
    }

    @Test
    public void shouldSelectListBoxItem() {
        final ListBox<String> listBox = newListBox("A", "B", "C");
        PWindow.getMain().add(listBox.asWidget());

        final InspectorListBox inspector = InspectorWidget.of(PWindow.getMain().getPRootPanel())
                .find(InspectorListBox.FACTORY);
        inspector.select("B");

        final List<String> selectedLabels = listBox.getSelectedItems().stream()
                .map(ListBoxItem::getLabel)
                .collect(Collectors.toList());
        assertEquals(List.of("B"), selectedLabels);
    }

    @Test
    public void shouldGetAvailableLabels_FromListBox() {
        final ListBox<String> listBox = newListBox("A", "B", "C");
        PWindow.getMain().add(listBox.asWidget());

        final InspectorListBox inspector = InspectorWidget.of(PWindow.getMain().getPRootPanel())
                .find(InspectorListBox.FACTORY);
        final List<String> labels = inspector.getAvailableLabels();

        assertEquals(List.of("A", "B", "C"), labels);
    }

    // =====================================================================================
    // Custom inspector extension pattern
    // =====================================================================================

    @Test
    public void shouldCreateCustomInspector_Extension() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PFlowPanel customWidget = Element.newPFlowPanel();
        customWidget.addStyleName("my-test-widget");
        final PLabel innerLabel = Element.newPLabel("inner");
        customWidget.add(innerLabel);
        panel.add(customWidget);
        PWindow.getMain().add(panel);

        final InspectorTestWidget custom = InspectorWidget.of(panel).find(InspectorTestWidget.FACTORY);

        assertNotNull(custom);
        assertSame(customWidget, custom.getWidget());
        assertEquals("inner", custom.getInnerLabel());
    }

    /**
     * Sample custom inspector used to verify the extension pattern documented on
     * {@link InspectorFactory}. Exposes its own base predicate through a {@code FACTORY}
     * constant and a domain-specific helper method.
     */
    public static class InspectorTestWidget extends InspectorWidget {

        public static final InspectorFactory<InspectorTestWidget> FACTORY = new InspectorFactory<InspectorTestWidget>() {

            @Override
            public Predicate<PWidget> basePredicate() {
                return style("my-test-widget");
            }

            @Override
            public InspectorTestWidget create(final PWidget widget) {
                return new InspectorTestWidget(widget);
            }
        };

        public InspectorTestWidget(final PWidget widget) {
            super(widget);
        }

        public String getInnerLabel() {
            return find(Predicates.type(PLabel.class)).getText();
        }
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static ListBox<String> newListBox(final String... labels) {
        final ListBoxConfiguration configuration = new ListBoxConfiguration();
        configuration.disableSorting();
        final List<ListBoxItem<String>> items = new ArrayList<>();
        for (final String label : labels) {
            items.add(ListBoxItem.of(label, label));
        }
        return new ListBox<>(configuration, items);
    }
}
