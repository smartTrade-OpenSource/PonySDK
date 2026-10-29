package com.ponysdk.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.event.PTerminalEvent;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public class PEmulatorTest extends PSuite {

    @Test
    public void testAddonDataReachesTheTerminalHandlerAsAnObject() {
        final PLabel label = Element.newPLabel();
        final List<PTerminalEvent> events = new ArrayList<>();
        label.setTerminalHandler(events::add);

        final JsonObject data = Json.createObjectBuilder().add("beginIndex", 3).build();
        PEmulator.addonData(label, data);

        assertEquals(1, events.size());
        assertSame(label, events.get(0).getSource());
        assertEquals(data, events.get(0).getData());
    }
}
