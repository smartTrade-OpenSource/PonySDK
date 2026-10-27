/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
import com.ponysdk.core.ui.basic.event.PShowRangeEvent;
import com.ponysdk.core.ui.basic.event.PShowRangeHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/**
 * The terminal shows the day a picker's timestamp falls on in UTC: the server sends midnight UTC of the day a date falls
 * on in the picker's time zone, the zone it reads the user's picks in.
 */
public class PDatePickerTest extends PSuite {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static long utcMidnight(final int year, final int month, final int day) {
        return LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private static Date at(final LocalDateTime time, final ZoneId zone) {
        return Date.from(time.atZone(zone).toInstant());
    }

    private PDatePicker attachedPicker() {
        final PDatePicker picker = Element.newPDatePicker();
        PWindow.getMain().add(picker);
        writer.clear();
        return picker;
    }

    @Test
    public void inUtcADateGoesAsMidnightOfItsDay() {
        final PDatePicker picker = attachedPicker();
        picker.setValue(at(LocalDateTime.of(2026, 3, 12, 23, 30), ZoneOffset.UTC));
        picker.setCurrentMonth(at(LocalDateTime.of(2026, 5, 1, 0, 0), ZoneOffset.UTC));
        assertEquals(List.of(utcMidnight(2026, 3, 12)), writer.valuesOf(ServerToClientModel.DATE));
        assertEquals(List.of(utcMidnight(2026, 5, 1)), writer.valuesOf(ServerToClientModel.TIME));
    }

    @Test
    public void inItsTimeZoneADateGoesAsTheDayItFallsOnThere() {
        final PDatePicker picker = attachedPicker();
        picker.setTimeZone(TimeZone.getTimeZone(PARIS));
        // midnight in Paris is still the day before in UTC: the picker must show the 12th
        picker.setValue(at(LocalDateTime.of(2026, 3, 12, 0, 0), PARIS));
        picker.addStyleToDates("holiday", List.of(at(LocalDateTime.of(2026, 3, 17, 0, 0), PARIS)));
        picker.setTransientEnabledOnDates(false, List.of(at(LocalDateTime.of(2026, 3, 20, 0, 0), PARIS)));
        assertEquals(List.of(utcMidnight(2026, 3, 12)), writer.valuesOf(ServerToClientModel.DATE));
        assertEquals(List.of(List.of(utcMidnight(2026, 3, 17))), arrays(writer.valuesOf(ServerToClientModel.ADD_DATE_STYLE)));
        assertEquals(List.of(List.of(utcMidnight(2026, 3, 20))), arrays(writer.valuesOf(ServerToClientModel.DATE_ENABLED)));

        // and a pick is read in the same zone
        picker.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.HANDLER_DATE_VALUE_CHANGE.toStringValue(), "2026-03-15").build());
        assertEquals(at(LocalDateTime.of(2026, 3, 15, 0, 0), PARIS), picker.getValue());
    }

    @Test
    public void theDateBoxsPickerShowsTheBoxsDayInTheZoneOfItsFormat() {
        final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        format.setTimeZone(TimeZone.getTimeZone(TOKYO));
        final PDateBox box = Element.newPDateBox(format);
        PWindow.getMain().add(box);
        writer.clear();

        box.setValue(at(LocalDateTime.of(2026, 3, 12, 0, 0), TOKYO)); // 11 March 15:00 UTC
        assertEquals(List.of("12/03/2026"), writer.valuesOf(ServerToClientModel.VALUE));
        assertEquals(List.of(utcMidnight(2026, 3, 12)), writer.valuesOf(ServerToClientModel.DATE));
        assertEquals(TimeZone.getTimeZone(TOKYO), box.getDatePicker().getTimeZone());
    }

    private static List<List<Object>> arrays(final List<Object> values) {
        return values.stream().map(v -> List.of((Object[]) v)).toList();
    }

    private static final String DATE_KEY = ClientToServerModel.HANDLER_DATE_VALUE_CHANGE.toStringValue();

    private static jakarta.json.JsonObject showRange(final String start, final String end) {
        return Json.createObjectBuilder().add(ClientToServerModel.HANDLER_SHOW_RANGE.toStringValue(), true)
            .add(ClientToServerModel.START_DATE.toStringValue(), start)
            .add(ClientToServerModel.END_DATE.toStringValue(), end).build();
    }

    @Test
    public void aNewPickerHasNoValueAndNoHandler() {
        final PDatePicker picker = Element.newPDatePicker();
        assertEquals(WidgetType.DATEPICKER, picker.getWidgetType());
        assertNull(picker.getValue());
        assertEquals(-1, picker.getYear());
        assertEquals(-1, picker.getMonth());
        assertEquals(-1, picker.getDay());
        assertEquals(TimeZone.getTimeZone("UTC"), picker.getTimeZone());
        assertTrue(picker.getValueChangeHandlers().isEmpty());
        assertTrue(picker.getShowRangeHandlers().isEmpty());
        assertFalse(picker.removeValueChangeHandler(e -> {
        }));
        assertFalse(picker.removeShowRangeHandler(e -> {
        }));
    }

    @Test
    public void theSameValueIsSentOnce() {
        final PDatePicker picker = attachedPicker();
        final Date date = new Date(utcMidnight(2026, 3, 12));
        picker.setValue(date);
        picker.setValue(date);
        assertEquals(date, picker.getValue());
        assertEquals(1, writer.count(ServerToClientModel.DATE));
    }

    @Test
    public void yearArrowsAndStyleRemovalAreSent() {
        final PDatePicker picker = attachedPicker();
        picker.setYearArrowsVisible(false);
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.YEAR_ARROWS_VISIBLE));
        picker.removeStyleFromDates("holiday", List.of(new Date(utcMidnight(2026, 3, 17))));
        assertEquals(List.of(List.of(utcMidnight(2026, 3, 17))), arrays(writer.valuesOf(ServerToClientModel.REMOVE_DATE_STYLE)));
        assertEquals(List.of("holiday"), writer.valuesOf(ServerToClientModel.STYLE_NAME));
    }

    @Test
    public void noDatesSendNothing() {
        final PDatePicker picker = attachedPicker();
        picker.addStyleToDates("holiday", List.of());
        picker.removeStyleFromDates("holiday", null);
        picker.setTransientEnabledOnDates(true, List.of());
        assertTrue(writer.isEmpty());
    }

    @Test
    public void aPickFromTheTerminalSetsTheValueAndNotifiesTheHandlers() {
        final PDatePicker picker = attachedPicker();
        final List<PValueChangeEvent<Date>> events = new ArrayList<>();
        final PValueChangeHandler<Date> handler = events::add;
        picker.addValueChangeHandler(handler);
        assertEquals(1, picker.getValueChangeHandlers().size());

        picker.onClientData(Json.createObjectBuilder().add(DATE_KEY, "2020-03-15").build());
        assertEquals(1, events.size());
        assertEquals(2020, picker.getYear());
        assertEquals(3, picker.getMonth());
        assertEquals(15, picker.getDay());
        assertEquals(new Date(utcMidnight(2020, 3, 15)), picker.getValue());
        assertEquals(picker.getValue(), events.get(0).getData());
        assertEquals("<date pid=\"" + picker.getID() + "\" year=\"2020\" month=\"3\" day=\"15\"></date>", picker.dumpDOM());

        assertTrue(picker.removeValueChangeHandler(handler));
        picker.onClientData(Json.createObjectBuilder().add(DATE_KEY, "2021-01-02").build());
        assertEquals(1, events.size());
        assertEquals(2021, picker.getYear());
    }

    @Test
    public void anUnparsablePickGivesNoValue() {
        final PDatePicker picker = attachedPicker();
        final List<PValueChangeEvent<Date>> events = new ArrayList<>();
        picker.addValueChangeHandler(events::add);
        picker.onClientData(Json.createObjectBuilder().add(DATE_KEY, "garbage").build());
        assertEquals(1, events.size());
        assertNull(events.get(0).getData());
        assertNull(picker.getValue());
        assertEquals(-1, picker.getYear());
    }

    @Test
    public void theShownRangeIsReportedToTheHandlers() {
        final PDatePicker picker = attachedPicker();
        picker.onClientData(showRange("2020-01-01", "2020-01-31")); // no handler: nothing happens

        final List<PShowRangeEvent<Date>> events = new ArrayList<>();
        final PShowRangeHandler<Date> handler = events::add;
        picker.addShowRangeHandler(handler);
        assertEquals(1, picker.getShowRangeHandlers().size());
        picker.onClientData(showRange("2020-01-01", "2020-01-31"));
        assertEquals(1, events.size());
        assertEquals(new Date(utcMidnight(2020, 1, 1)), events.get(0).getStart());
        assertEquals(new Date(utcMidnight(2020, 1, 31)), events.get(0).getEnd());

        picker.onClientData(showRange("x", "y")); // unparsable: dropped
        assertEquals(1, events.size());
        assertTrue(picker.removeShowRangeHandler(handler));
    }

    @Test
    public void aHiddenPickerIgnoresTheTerminal() {
        final PDatePicker picker = attachedPicker();
        picker.setVisible(false);
        picker.onClientData(Json.createObjectBuilder().add(DATE_KEY, "2020-03-15").build());
        assertNull(picker.getValue());
    }

    @Test
    public void otherTerminalDataGoesToTheWidget() {
        final PDatePicker picker = attachedPicker();
        picker.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(picker.isShown());
    }

    @Test
    public void severalHandlersAndTheRemovalOfAnUnknownOne() {
        final PDatePicker picker = attachedPicker();
        final List<String> calls = new ArrayList<>();
        picker.addValueChangeHandler(e -> calls.add("v1"));
        picker.addValueChangeHandler(e -> calls.add("v2"));
        picker.addShowRangeHandler(e -> calls.add("r1"));
        picker.addShowRangeHandler(e -> calls.add("r2"));
        assertFalse(picker.removeValueChangeHandler(e -> {
        }));
        assertFalse(picker.removeShowRangeHandler(e -> {
        }));
        picker.onClientData(Json.createObjectBuilder().add(DATE_KEY, "2020-03-15").build());
        picker.onClientData(showRange("2020-01-01", "2020-01-31"));
        assertEquals(List.of("v1", "v2", "r1", "r2"), calls);
    }
}
