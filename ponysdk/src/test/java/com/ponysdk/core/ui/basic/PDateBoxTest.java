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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import jakarta.json.Json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PDateBoxTest extends PSuite {

    private static final Date MARCH_15_2020_UTC = new Date(1584230400000L);

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private Object last(final ServerToClientModel model) {
        final List<Object> values = writer.valuesOf(model);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    private static SimpleDateFormat utcFormat(final String pattern) {
        final SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    @Test
    public void testInit() {
        final PDateBox dateBox = Element.newPDateBox();
        assertEquals(WidgetType.DATEBOX, dateBox.getWidgetType());
        assertEquals("MM/dd/yyyy", dateBox.getDateFormat().toPattern());
        assertNotNull(dateBox.getDatePicker());
        assertNull(dateBox.getValue());
        assertNull(dateBox.getRawValue());
        assertEquals("", dateBox.getDisplayedValue());
        assertTrue(dateBox.isEnabled());
        assertTrue(dateBox.getValueChangeHandlers().isEmpty());
        assertFalse(dateBox.removeValueChangeHandler(e -> {
        }));
    }

    @Test
    public void testCreation() {
        final PDateBox dateBox = Element.newPDateBox(utcFormat("yyyy-MM-dd"), true);
        PWindow.getMain().add(dateBox);

        final PDatePicker picker = dateBox.getDatePicker();
        assertTrue(picker.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(picker.getID()));
        assertEquals(List.of(picker.getID()), writer.valuesOf(ServerToClientModel.PICKER));
        assertEquals(List.of("yyyy-MM-dd"), writer.valuesOf(ServerToClientModel.DATE_FORMAT_PATTERN));
        assertEquals(1, writer.count(ServerToClientModel.KEEP_DAY_TIME_NEEDED));
        // The picker is added to the date box
        assertEquals(List.of(dateBox.getID()), writer.valuesAfter(ServerToClientModel.TYPE_ADD, picker.getID()));
    }

    @Test
    public void testCreationWithPicker() {
        final PDatePicker picker = Element.newPDatePicker();
        final PDateBox dateBox = Element.newPDateBox(picker, utcFormat("dd/MM/yyyy"));
        assertSame(picker, dateBox.getDatePicker());
        PWindow.getMain().add(dateBox);
        assertEquals(0, writer.count(ServerToClientModel.KEEP_DAY_TIME_NEEDED));

        final PDateBox other = Element.newPDateBox(Element.newPDatePicker(), utcFormat("dd/MM/yyyy"), false);
        assertEquals("dd/MM/yyyy", other.getDateFormat().toPattern());
        assertEquals("yyyy", Element.newPDateBox(utcFormat("yyyy")).getDateFormat().toPattern());
    }

    @Test
    public void testSetValue() {
        final PDateBox dateBox = Element.newPDateBox(utcFormat("yyyy-MM-dd"));
        PWindow.getMain().add(dateBox);

        dateBox.setValue(MARCH_15_2020_UTC);
        assertEquals(MARCH_15_2020_UTC, dateBox.getValue());
        assertEquals("2020-03-15", dateBox.getRawValue());
        assertEquals("2020-03-15", dateBox.getDisplayedValue());
        assertEquals(MARCH_15_2020_UTC, dateBox.getDatePicker().getValue());
        assertEquals("2020-03-15", last(ServerToClientModel.VALUE));
        assertEquals("<input pid=\"" + dateBox.getID() + "\" format=\"yyyy-MM-dd\">" + MARCH_15_2020_UTC + "</input>", dateBox.dumpDOM());

        dateBox.setValue(null);
        assertNull(dateBox.getValue());
        assertEquals("", dateBox.getRawValue());
        assertEquals("", last(ServerToClientModel.VALUE));
        assertEquals("<input pid=\"" + dateBox.getID() + "\" format=\"yyyy-MM-dd\"></input>", dateBox.dumpDOM());
    }

    @Test
    public void testSetDateFormat() {
        final PDateBox dateBox = Element.newPDateBox(utcFormat("yyyy-MM-dd"));
        final SimpleDateFormat paris = new SimpleDateFormat("dd/MM");
        paris.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        dateBox.setDateFormat(paris);
        assertSame(paris, dateBox.getDateFormat());
        assertEquals(TimeZone.getTimeZone("Europe/Paris"), dateBox.getDatePicker().getTimeZone());
        // Not initialized: no update, the pattern is sent at creation
        assertEquals(0, writer.count(ServerToClientModel.DATE_FORMAT_PATTERN));

        PWindow.getMain().add(dateBox);
        writer.clear();
        dateBox.setDateFormat(dateBox.getDateFormat());
        assertEquals(0, writer.count(ServerToClientModel.DATE_FORMAT_PATTERN));
        dateBox.setDateFormat(utcFormat("yyyy"));
        assertEquals(List.of("yyyy"), writer.valuesOf(ServerToClientModel.DATE_FORMAT_PATTERN));
    }

    @Test
    public void testValueChangeFromClient() {
        final PDateBox dateBox = Element.newPDateBox(utcFormat("yyyy-MM-dd"));
        PWindow.getMain().add(dateBox);
        final List<PValueChangeEvent<Date>> events = new ArrayList<>();
        final PValueChangeHandler<Date> handler = events::add;
        dateBox.addValueChangeHandler(handler);
        assertEquals(1, dateBox.getValueChangeHandlers().size());

        final String key = ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue();
        dateBox.onClientData(Json.createObjectBuilder().add(key, "2020-03-15").build());
        assertEquals(MARCH_15_2020_UTC, dateBox.getValue());
        assertEquals("2020-03-15", dateBox.getRawValue());
        assertEquals(dateBox.getValue(), events.get(0).getData());

        dateBox.onClientData(Json.createObjectBuilder().add(key, "not a date").build());
        assertNull(dateBox.getValue());
        assertEquals("not a date", dateBox.getRawValue());

        dateBox.onClientData(Json.createObjectBuilder().add(key, "").build());
        assertNull(dateBox.getValue());
        assertEquals(3, events.size());

        assertTrue(dateBox.removeValueChangeHandler(handler));
        dateBox.onClientData(Json.createObjectBuilder().add(key, "2020-03-15").build());
        assertEquals(3, events.size());

        dateBox.setVisible(false);
        dateBox.onClientData(Json.createObjectBuilder().add(key, "2021-01-01").build());
        assertEquals(MARCH_15_2020_UTC, dateBox.getValue());
    }

    @Test
    public void testOtherClientDataDelegatesToWidget() {
        final PDateBox dateBox = Element.newPDateBox();
        PWindow.getMain().add(dateBox);
        dateBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(dateBox.isShown());
    }

    @Test
    public void testEnabled() {
        final PDateBox dateBox = Element.newPDateBox();
        PWindow.getMain().add(dateBox);
        dateBox.setEnabled(false);
        dateBox.setEnabled(false);
        assertFalse(dateBox.isEnabled());
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.ENABLED));
    }

    @Test
    public void testFocus() {
        final PDateBox dateBox = Element.newPDateBox();
        PWindow.getMain().add(dateBox);
        writer.clear();
        dateBox.focus();
        dateBox.blur();
        assertEquals(List.of(true, false), writer.valuesOf(ServerToClientModel.FOCUS));
    }

    @Test
    public void testDomHandlers() {
        final PDateBox dateBox = Element.newPDateBox();
        PWindow.getMain().add(dateBox);
        final List<Object> events = new ArrayList<>();
        assertNotNull(dateBox.addMouseOverHandler(events::add));
        assertNotNull(dateBox.addFocusHandler(events::add));
        assertNotNull(dateBox.addBlurHandler(events::add));
        assertNotNull(dateBox.addClickHandler(events::add));
        assertNotNull(dateBox.addDoubleClickHandler(events::add));

        dateBox.fireEvent(new PMouseOverEvent(dateBox));
        dateBox.fireEvent(new PFocusEvent(dateBox));
        dateBox.fireEvent(new PBlurEvent(dateBox));
        dateBox.fireEvent(new PClickEvent(dateBox));
        dateBox.fireEvent(new PDoubleClickEvent(dateBox));
        assertEquals(5, events.size());
    }

    @Test
    public void testTabindexOnlyOnFormFieldsMakesTheBoxFocusable() {
        org.mockito.Mockito.when(com.ponysdk.core.server.application.UIContext.get().getConfiguration().isTabindexOnlyFormField())
            .thenReturn(true);
        try {
            assertEquals(PWidget.TabindexMode.FOCUSABLE.getTabIndex(), Element.newPDateBox().getTabindex());
        } finally {
            org.mockito.Mockito.when(com.ponysdk.core.server.application.UIContext.get().getConfiguration().isTabindexOnlyFormField())
                .thenReturn(false);
        }
    }

    @Test
    public void testSeveralHandlersAndTheRemovalOfAnUnknownOne() {
        final PDateBox dateBox = Element.newPDateBox(utcFormat("yyyy-MM-dd"));
        PWindow.getMain().add(dateBox);
        final List<String> calls = new ArrayList<>();
        dateBox.addValueChangeHandler(e -> calls.add("a"));
        dateBox.addValueChangeHandler(e -> calls.add("b"));
        assertFalse(dateBox.removeValueChangeHandler(e -> {
        }));
        dateBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), "2020-03-15").build());
        assertEquals(List.of("a", "b"), calls);
        assertFalse(Element.newPDateBox().removeValueChangeHandler(e -> {
        }));
    }
}
