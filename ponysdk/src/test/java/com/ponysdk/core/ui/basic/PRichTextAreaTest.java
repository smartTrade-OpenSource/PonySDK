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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PFontSize;
import com.ponysdk.core.model.PJustification;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PRichTextAreaTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PRichTextArea attached() {
        final PRichTextArea area = Element.newPRichTextArea();
        PWindow.getMain().add(area);
        return area;
    }

    private static JsonObject valueChange(final String value) {
        return Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), value).build();
    }

    @Test
    public void testInit() {
        final PRichTextArea area = Element.newPRichTextArea();
        assertEquals(WidgetType.RICH_TEXT_AREA, area.getWidgetType());
        assertNull(area.getText());
        assertNull(area.getHTML());
        assertNotNull(area.getFormatter());
        assertTrue(area.getValueChangeHandlers().isEmpty());
    }

    @Test
    public void testTextAndHtmlReplaceEachOther() {
        final PRichTextArea area = attached();
        writer.clear();
        area.setText("plain");
        area.setText("plain");
        assertEquals("plain", area.getText());
        area.setHTML("<b>rich</b>");
        area.setHTML("<b>rich</b>");
        assertEquals("<b>rich</b>", area.getHTML());
        assertNull(area.getText());
        area.setText("again");
        assertNull(area.getHTML());
        assertEquals(List.of("plain", "again"), writer.valuesOf(ServerToClientModel.TEXT));
        assertEquals(List.of("<b>rich</b>"), writer.valuesOf(ServerToClientModel.HTML));
    }

    @Test
    public void testValueChangeFromClient() {
        final PRichTextArea area = attached();
        final List<PValueChangeEvent<String>> events = new ArrayList<>();
        final PValueChangeHandler<String> handler = events::add;
        area.addValueChangeHandler(handler);
        assertEquals(1, area.getValueChangeHandlers().size());

        area.onClientData(valueChange("<i>typed</i>"));
        assertEquals("<i>typed</i>", area.getHTML());
        assertEquals(1, events.size());
        assertEquals("<i>typed</i>", events.get(0).getData());

        assertTrue(area.removeValueChangeHandler(handler));
        assertFalse(area.removeValueChangeHandler(handler));
        area.onClientData(valueChange("other"));
        assertEquals("other", area.getHTML());
        assertEquals(1, events.size());
    }

    @Test
    public void testRemoveHandlerWithoutAnyHandler() {
        assertFalse(Element.newPRichTextArea().removeValueChangeHandler(event -> {
        }));
    }

    @Test
    public void testValueChangeIgnoredWhenDisabledOrHidden() {
        final PRichTextArea area = attached();
        final List<PValueChangeEvent<String>> events = new ArrayList<>();
        area.addValueChangeHandler(events::add);
        area.setEnabled(false);
        area.onClientData(valueChange("a"));
        area.setEnabled(true);
        area.setVisible(false);
        area.onClientData(valueChange("b"));
        assertTrue(events.isEmpty());
        assertNull(area.getHTML());
    }

    @Test
    public void testOtherClientDataDelegated() {
        final PRichTextArea area = attached();
        final List<PValueChangeEvent<String>> events = new ArrayList<>();
        area.addValueChangeHandler(events::add);
        area.onClientData(Json.createObjectBuilder().add("unknown", 1).build());
        assertTrue(events.isEmpty());
    }

    @Test
    public void testDumpDOM() {
        final PRichTextArea area = Element.newPRichTextArea();
        area.setText("t");
        assertEquals("<input pid=\"" + area.getID() + "\">t</input>", area.dumpDOM());
        area.setHTML("<b>h</b>");
        assertEquals("<input pid=\"" + area.getID() + "\"><b>h</b></input>", area.dumpDOM());
    }

    @Test
    public void testFormatterCommandsWithValue() {
        final PRichTextArea area = attached();
        final PRichTextArea.Formatter formatter = area.getFormatter();
        writer.clear();

        formatter.createLink("http://link");
        formatter.insertHTML("<hr/>");
        formatter.insertImage("http://img");
        formatter.setBackColor("red");
        formatter.setFontName("Arial");
        formatter.setFontSize(PFontSize.LARGE);
        formatter.setForeColor("blue");
        formatter.setJustification(PJustification.CENTER);

        assertEquals(List.of("http://link"), writer.valuesOf(ServerToClientModel.URL));
        assertEquals(List.of("<hr/>"), writer.valuesOf(ServerToClientModel.INSERT_HTML));
        assertEquals(List.of("http://img"), writer.valuesOf(ServerToClientModel.IMAGE_URL));
        assertEquals(List.of("red"), writer.valuesOf(ServerToClientModel.BACK_COLOR));
        assertEquals(List.of("Arial"), writer.valuesOf(ServerToClientModel.FONT_NAME));
        assertEquals(List.of(PFontSize.LARGE.getValue()), writer.valuesOf(ServerToClientModel.FONT_SIZE));
        assertEquals(List.of("blue"), writer.valuesOf(ServerToClientModel.FONT_COLOR));
        assertEquals(List.of(PJustification.CENTER.getValue()), writer.valuesOf(ServerToClientModel.JUSTIFICATION));
        assertEquals(8, writer.count(ServerToClientModel.TYPE_UPDATE));
    }

    @Test
    public void testFormatterToggleCommands() {
        final PRichTextArea area = attached();
        final PRichTextArea.Formatter formatter = area.getFormatter();
        writer.clear();

        formatter.insertHorizontalRule();
        formatter.insertOrderedList();
        formatter.insertUnorderedList();
        formatter.toggleBold();
        formatter.toggleItalic();
        formatter.toggleSubscript();
        formatter.toggleUnderline();
        formatter.leftIndent();
        formatter.redo();
        formatter.removeFormat();
        formatter.removeLink();
        formatter.rightIndent();
        formatter.selectAll();

        final List<ServerToClientModel> expected = List.of(ServerToClientModel.INSERT_HORIZONTAL_RULE, ServerToClientModel.ORDERED,
            ServerToClientModel.UNORDERED, ServerToClientModel.TOGGLE_BOLD, ServerToClientModel.TOGGLE_ITALIC,
            ServerToClientModel.TOGGLE_SUBSCRIPT, ServerToClientModel.TOGGLE_UNDERLINE, ServerToClientModel.LEFT_INDENT,
            ServerToClientModel.REDO, ServerToClientModel.REMOVE_FORMAT, ServerToClientModel.REMOVE_LINK,
            ServerToClientModel.TOGGLE_RIGHT_INDENT, ServerToClientModel.SELECT_ALL);
        final List<ServerToClientModel> sent = writer.models().stream().filter(expected::contains).toList();
        assertEquals(expected, sent);
        assertEquals(expected.size(), writer.count(ServerToClientModel.TYPE_UPDATE));
    }

}
