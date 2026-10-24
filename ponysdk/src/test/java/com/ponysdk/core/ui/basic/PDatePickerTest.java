/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import jakarta.json.Json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
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
}
