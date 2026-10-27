/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PSuggestBoxTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PSuggestBox attachedBox() {
        final PSuggestBox box = Element.newPSuggestBox();
        PWindow.getMain().add(box);
        return box;
    }

    private static jakarta.json.JsonObject selection(final String display, final String replacement) {
        return Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_SELECTION.toStringValue(), display)
            .add(ClientToServerModel.REPLACEMENT_STRING.toStringValue(), replacement).build();
    }

    /** Its creation names its oracle and its text box (ORACLE, TEXTBOX_ID): both exist on the terminal first. */
    @Test
    public void theOracleAndTheTextBoxAreCreatedWithTheSuggestBox() {
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        oracle.addAll(List.of("Apple", "Apricot"));
        final PSuggestBox box = Element.newPSuggestBox(oracle);
        assertNotNull(box.getTextBox());
        PWindow.getMain().add(box);
        assertTrue(oracle.isInitialized());
        assertTrue(box.getTextBox().isInitialized());
        assertTrue(box.isInitialized());
        box.setText("Apple");
        assertTrue("Apple".equals(box.getText()), "the text is the text box's");
    }

    @Test
    public void theCreationNamesTheOracleTheTextBoxAndTheHandlers() {
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        final PSuggestBox box = Element.newPSuggestBox(oracle);
        assertEquals(WidgetType.SUGGESTBOX, box.getWidgetType());
        assertSame(oracle, box.getSuggestOracle());
        PWindow.getMain().add(box);
        assertEquals(List.of(oracle.getID()), writer.valuesOf(ServerToClientModel.ORACLE));
        assertEquals(List.of(box.getTextBox().getID()), writer.valuesOf(ServerToClientModel.TEXTBOX_ID));
        final List<Object> handlerTypes = writer.valuesOf(ServerToClientModel.HANDLER_TYPE);
        assertTrue(handlerTypes.contains(HandlerModel.HANDLER_STRING_VALUE_CHANGE.getValue()));
        assertTrue(handlerTypes.contains(HandlerModel.HANDLER_STRING_SELECTION.getValue()));
    }

    @Test
    public void theDefaultOracleIsMultiWord() {
        assertTrue(Element.newPSuggestBox().getSuggestOracle() instanceof PMultiWordSuggestOracle);
    }

    @Test
    public void theLimitIsSentOncePerChange() {
        final PSuggestBox box = attachedBox();
        assertEquals(0, box.getLimit());
        box.setLimit(10);
        box.setLimit(10);
        assertEquals(10, box.getLimit());
        assertEquals(List.of(10), writer.valuesOf(ServerToClientModel.LIMIT));
    }

    @Test
    public void aTypedValueReachesTheValueChangeHandlers() {
        final PSuggestBox box = attachedBox();
        final List<PValueChangeEvent<String>> events = new ArrayList<>();
        box.addValueChangeHandler(events::add);
        assertEquals(1, box.getValueChangeHandlers().size());
        box.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), "Ap").build());
        assertEquals(1, events.size());
        assertEquals("Ap", events.get(0).getData());
        assertSame(box, events.get(0).getSource());
        assertEquals("Ap", box.getText());
    }

    @Test
    public void aSelectedSuggestionSetsTheTextAndReachesTheSelectionHandlers() {
        final PSuggestBox box = attachedBox();
        box.onClientData(selection("d0", "r0")); // no handler yet
        assertEquals("r0", box.getText());

        final List<PSelectionEvent<PSuggestOracle.PSuggestion>> events = new ArrayList<>();
        final PSelectionHandler<PSuggestOracle.PSuggestion> handler = events::add;
        box.addSelectionHandler(handler);
        box.onClientData(selection("Apple (fruit)", "Apple"));
        assertEquals("Apple", box.getText());
        assertEquals(1, events.size());
        assertEquals("Apple", events.get(0).getSelectedItem().getReplacementString());
        assertEquals("Apple (fruit)", events.get(0).getSelectedItem().getDisplayString());

        box.removeSelectionHandler(handler);
        box.onClientData(selection("d", "r"));
        assertEquals(1, events.size());
    }

    @Test
    public void removingASelectionHandlerWithoutAnyIsANoOp() {
        final PSuggestBox box = Element.newPSuggestBox();
        box.removeSelectionHandler(e -> {
        });
        box.onSelection(new PSelectionEvent<>(box, new PSuggestBox.MultiWordSuggestion("r", "d")));
        assertEquals("", box.getText());
    }

    @Test
    public void aHiddenSuggestBoxIgnoresTheTerminal() {
        final PSuggestBox box = attachedBox();
        box.setVisible(false);
        box.onClientData(selection("d", "r"));
        assertEquals("", box.getText());
    }

    @Test
    public void otherTerminalDataGoesToTheWidget() {
        final PSuggestBox box = attachedBox();
        box.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(box.isShown());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void focusGoesToTheTextBox() {
        final PSuggestBox box = attachedBox();
        writer.clear();
        box.focus();
        box.blur();
        box.setFocus(true);
        box.setFocus(false);
        assertEquals(List.of(true, false, true, false), writer.valuesOf(ServerToClientModel.FOCUS));
        assertEquals(List.of(box.getTextBox().getID()), writer.valuesOf(ServerToClientModel.TYPE_UPDATE).stream().distinct().toList());
    }

    @Test
    public void theDomIsTheTextBoxs() {
        final PSuggestBox box = Element.newPSuggestBox();
        assertEquals(box.getTextBox().dumpDOM(), box.dumpDOM());
    }

    @Test
    public void aValueChangeHandlerCanBeRemoved() {
        final PSuggestBox box = Element.newPSuggestBox();
        final PValueChangeHandler<String> handler = e -> {
        };
        box.addValueChangeHandler(handler);
        assertTrue(box.removeValueChangeHandler(handler));
        assertTrue(box.getValueChangeHandlers().isEmpty());
    }
}
