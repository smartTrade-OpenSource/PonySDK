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
package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PPasteEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import static org.junit.jupiter.api.Assertions.*;

/** {@link PTextBox} with its bases {@link PTextBoxBase} and {@link PValueBoxBase}, and {@link PPasswordTextBox}. */
public class PTextBoxTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    private static PTextBox attached(final String text) {
        final PTextBox textBox = Element.newPTextBox(text);
        PWindow.getMain().add(textBox);
        return textBox;
    }

    private static JsonObject valueChange(final String value) {
        return Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), value).build();
    }

    private static JsonObject paste(final String data) {
        return Json.createObjectBuilder().add(ClientToServerModel.HANDLER_PASTE.toStringValue(), data).build();
    }

    @Test
    public void textAndPlaceholderTravelInTheCreateOnlyWhenNotEmpty() {
        final PTextBox empty = Element.newPTextBox();
        assertEquals("", empty.getText());
        assertTrue(empty.isEmpty());
        assertEquals("", empty.getPlaceholder());
        PWindow.getMain().add(empty);
        assertEquals(0, writer.count(ServerToClientModel.TEXT));
        assertEquals(0, writer.count(ServerToClientModel.PLACEHOLDER));
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_TYPE).contains(WidgetType.TEXTBOX.getValue()));

        writer.clear();
        final PTextBox filled = Element.newPTextBox("hello");
        filled.setPlaceholder("type here");
        assertTrue(writer.isEmpty(), "nothing is sent before the create");
        PWindow.getMain().add(filled);
        assertEquals(List.of("hello"), writer.valuesOf(ServerToClientModel.TEXT));
        assertEquals(List.of("type here"), writer.valuesOf(ServerToClientModel.PLACEHOLDER));
    }

    @Test
    public void textAndPlaceholderChangesAreSentOnceAndNullIsEmpty() {
        final PTextBox textBox = attached("a");
        writer.clear();

        textBox.setValue("b");
        textBox.setText("b");
        assertEquals("b", textBox.getValue());
        textBox.setText(null);
        assertEquals("", textBox.getText());
        textBox.setPlaceholder("p");
        textBox.setPlaceholder("p");
        textBox.setPlaceholder(null);
        assertEquals("", textBox.getPlaceholder());

        assertEquals(List.of("b", ""), writer.valuesOf(ServerToClientModel.TEXT));
        assertEquals(List.of("p", ""), writer.valuesOf(ServerToClientModel.PLACEHOLDER));
        assertTrue(textBox.toString().endsWith("text=, placeholder="));
    }

    @Test
    public void lengthsMaskAndFilterAreSent() {
        final PTextBox textBox = attached("");
        writer.clear();

        textBox.setMaxLength(10);
        textBox.setMaxLength(10);
        textBox.setVisibleLength(5);
        textBox.setVisibleLength(5);
        assertEquals(10, textBox.getMaxLength());
        assertEquals(5, textBox.getVisibleLength());
        assertEquals(List.of(10), writer.valuesOf(ServerToClientModel.MAX_LENGTH));
        assertEquals(List.of(5), writer.valuesOf(ServerToClientModel.VISIBLE_LENGTH));

        textBox.applyMask("99/99");
        textBox.applyMask("AA", false, "_");
        assertEquals(List.of("99/99", "AA"), writer.valuesOf(ServerToClientModel.MASK));
        assertEquals(List.of(true, false), writer.valuesOf(ServerToClientModel.VISIBILITY));
        assertEquals(List.of(" ", "_"), writer.valuesOf(ServerToClientModel.REPLACEMENT_STRING));

        textBox.setFilter("[0-9]");
        assertEquals(List.of("[0-9]"), writer.valuesOf(ServerToClientModel.REGEX_FILTER));
    }

    @Test
    public void selectionIsClampedToTheText() {
        final PTextBox textBox = attached("hello");
        writer.clear();

        textBox.selectAll();
        assertEquals(1, writer.count(ServerToClientModel.SELECT_ALL));

        textBox.select(1, 2);
        textBox.select(3, 10);
        textBox.select(5, 1);
        assertEquals(List.of(1, 3), writer.valuesOf(ServerToClientModel.SELECTION_RANGE_START));
        assertEquals(List.of(2, 2), writer.valuesOf(ServerToClientModel.SELECTION_RANGE_LENGTH));

        textBox.setCursorPosition(2);
        textBox.setCursorPosition(99);
        assertEquals(List.of(2, 5), writer.valuesOf(ServerToClientModel.CURSOR_POSITION));
    }

    @Test
    public void aValueChangeFromTheTerminalUpdatesTheTextWithoutEchoAndNotifies() {
        final PTextBox textBox = attached("");
        final List<String> values = new ArrayList<>();
        final PValueChangeHandler<String> handler = e -> values.add(e.getData());
        textBox.addValueChangeHandler(handler);
        assertEquals(1, textBox.getValueChangeHandlers().size());
        writer.clear();

        textBox.onClientData(valueChange("typed"));
        assertEquals("typed", textBox.getText());
        assertEquals(List.of("typed"), values);
        assertTrue(writer.isEmpty(), "the terminal already shows the text");

        assertTrue(textBox.removeValueChangeHandler(handler));
        assertFalse(textBox.removeValueChangeHandler(handler));
        textBox.onClientData(valueChange("again"));
        assertEquals("again", textBox.getText());
        assertEquals(1, values.size());
    }

    @Test
    public void aDisabledOrHiddenBoxIgnoresTheTerminal() {
        final PTextBox textBox = attached("");
        final List<String> values = new ArrayList<>();
        textBox.addValueChangeHandler(e -> values.add(e.getData()));
        textBox.addPasteHandler(e -> values.add("paste"));

        textBox.setEnabled(false);
        textBox.onClientData(valueChange("x"));
        textBox.onClientData(paste("y"));
        textBox.setEnabled(true);
        textBox.setVisible(false);
        textBox.onClientData(valueChange("x"));
        assertEquals("", textBox.getText());
        assertTrue(values.isEmpty());
    }

    @Test
    public void withoutHandlerAValueChangeStillUpdatesTheText() {
        final PTextBox textBox = attached("");
        assertTrue(textBox.getValueChangeHandlers().isEmpty());
        assertFalse(textBox.removeValueChangeHandler(e -> {
        }));
        textBox.onClientData(valueChange("v"));
        assertEquals("v", textBox.getText());
    }

    @Test
    public void pasteHandlersAreRegisteredOnceAndRemovedWithTheLast() {
        final PTextBox textBox = attached("");
        writer.clear();
        final List<String> pasted = new ArrayList<>();
        final PPasteEvent.PPasteHandler first = e -> pasted.add("1:" + e.getData());
        final PPasteEvent.PPasteHandler second = e -> pasted.add("2:" + e.getData());

        textBox.addPasteHandler(first);
        textBox.addPasteHandler(second);
        assertEquals(1, writer.count(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(List.of(HandlerModel.HANDLER_PASTE.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));

        textBox.onClientData(paste("clip"));
        assertEquals(List.of("1:clip", "2:clip"), pasted);

        writer.clear();
        textBox.removePasteHandler(first);
        assertEquals(0, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
        textBox.removePasteHandler(second);
        assertEquals(1, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
        assertEquals(List.of(HandlerModel.HANDLER_PASTE.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));
    }

    @Test
    public void pasteWithoutHandlerAndRemoveWithoutAddAreNoOps() {
        final PTextBox textBox = attached("t");
        writer.clear();
        textBox.removePasteHandler(e -> {
        });
        textBox.onClientData(paste("clip"));
        assertTrue(writer.isEmpty());
        assertEquals("t", textBox.getText());
    }

    @Test
    public void otherTerminalDataReachesTheDomHandlers() {
        final PTextBox textBox = attached("");
        final List<String> clicks = new ArrayList<>();
        textBox.addClickHandler(e -> clicks.add("click"));
        PEmulator.click(textBox);
        assertEquals(List.of("click"), clicks);
    }

    @Test
    public void dumpDomShowsStylesAndText() {
        final PTextBox textBox = Element.newPTextBox("v");
        textBox.addStyleName("s");
        assertEquals("<input pid=\"" + textBox.getID() + "\" class=\"s\">v</input>", textBox.dumpDOM());
    }

    @Test
    public void passwordTextBoxIsATextBoxOfItsOwnType() {
        final PPasswordTextBox password = Element.newPPasswordTextBox("secret");
        assertEquals(WidgetType.PASSWORD_TEXTBOX, password.getWidgetType());
        assertEquals("secret", password.getText());
        assertEquals("", Element.newPPasswordTextBox().getText());
        PWindow.getMain().add(password);
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_TYPE).contains(WidgetType.PASSWORD_TEXTBOX.getValue()));
    }

    @Test
    public void everyValueChangeHandlerIsKept() {
        final PTextBox box = Element.newPTextBox();
        box.addValueChangeHandler(event -> {
        });
        box.addValueChangeHandler(event -> {
        });
        assertEquals(2, box.getValueChangeHandlers().size());
    }

    @Test
    public void aDisabledBoxIgnoresAPaste() {
        final PTextBox box = Element.newPTextBox();
        PWindow.getMain().add(box);
        final List<PPasteEvent> pasted = new ArrayList<>();
        box.addPasteHandler(pasted::add);
        box.setEnabled(false);
        box.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_PASTE.toStringValue(), "x").build());
        assertTrue(pasted.isEmpty());
    }

    @Test
    public void aDisabledValueBoxIgnoresAPaste() {
        final PValueBoxBase box = new PValueBoxBase() {

            @Override
            protected com.ponysdk.core.model.WidgetType getWidgetType() {
                return com.ponysdk.core.model.WidgetType.TEXTBOX;
            }
        };
        PWindow.getMain().add(box);
        final List<PPasteEvent> pasted = new ArrayList<>();
        box.addPasteHandler(pasted::add);
        box.setEnabled(false);
        box.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_PASTE.toStringValue(), "x").build());
        assertTrue(pasted.isEmpty());
        box.setEnabled(true);
        box.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_PASTE.toStringValue(), "x").build());
        assertEquals(1, pasted.size());
    }
}
