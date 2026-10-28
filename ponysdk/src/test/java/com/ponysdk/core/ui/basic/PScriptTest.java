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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;
import static org.junit.jupiter.api.Assertions.*;

public class PScriptTest extends PSuite {

    private static final String SCRIPT_KEY = PScript.class.getCanonicalName();

    private CapturingModelWriter writer;

    private static final class RecordingCallback implements PScript.ExecutionCallback {

        private final List<String> successes = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();

        @Override
        public void onFailure(final String msg) {
            failures.add(msg);
        }

        @Override
        public void onSuccess(final String msg) {
            successes.add(msg);
        }
    }

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    private static PScript scriptOf(final PWindow window) {
        return UIContext.get().getAttribute(SCRIPT_KEY + window.getID());
    }

    private static PWindow openWindow() {
        final PWindow window = Element.newPWindow("script", null);
        window.open();
        return window;
    }

    @Test
    public void theFirstCallCreatesOneScriptPerWindow() {
        final PWindow window = openWindow();
        writer.clear();

        PScript.call(window, "first");

        final PScript script = scriptOf(window);
        assertNotNull(script);
        assertEquals(WidgetType.SCRIPT, script.getWidgetType());
        assertEquals(List.of(WidgetType.SCRIPT.getValue()), writer.valuesOf(ServerToClientModel.WIDGET_TYPE));
        assertEquals(List.of("first"), writer.valuesOf(ServerToClientModel.FUNCTION_NAME));
        assertEquals(0, writer.count(ServerToClientModel.FUNCTION_ARGS), "no arguments, none sent");
        assertEquals(0, writer.count(ServerToClientModel.COMMAND_ID));
        assertEquals(0, writer.count(ServerToClientModel.FIXDELAY));

        writer.clear();
        PScript.call(window, "second", 1, "two");
        assertSame(script, scriptOf(window));
        assertEquals(0, writer.count(ServerToClientModel.TYPE_CREATE));
        assertEquals(List.of("second"), writer.valuesOf(ServerToClientModel.FUNCTION_NAME));
        assertEquals("[[1, two]]", writer.frames().stream().filter(f -> f.startsWith("FUNCTION_ARGS="))
            .map(f -> f.substring("FUNCTION_ARGS=".length())).toList().toString());
    }

    @Test
    public void callbacksAreAnsweredOnceBySuccessOrFailure() {
        final PWindow window = openWindow();
        final RecordingCallback callback = new RecordingCallback();

        PScript.call(window, "ok", new Object[0], callback, null);
        final List<Object> ids = writer.valuesOf(ServerToClientModel.COMMAND_ID);
        final long okID = (long) ids.get(ids.size() - 1);
        PScript.call(window, "ko", null, callback, Duration.ofMillis(10));
        final List<Object> ids2 = writer.valuesOf(ServerToClientModel.COMMAND_ID);
        final long koID = (long) ids2.get(ids2.size() - 1);
        assertNotEquals(okID, koID);
        assertEquals(List.of(10L), writer.valuesOf(ServerToClientModel.FIXDELAY));

        final PScript script = scriptOf(window);
        script.onClientData(Json.createObjectBuilder().add(ClientToServerModel.COMMAND_ID.toStringValue(), okID)
            .add(ClientToServerModel.RESULT.toStringValue(), "done").build());
        script.onClientData(Json.createObjectBuilder().add(ClientToServerModel.COMMAND_ID.toStringValue(), koID)
            .add(ClientToServerModel.ERROR_MSG.toStringValue(), "boom").build());
        assertEquals(List.of("done"), callback.successes);
        assertEquals(List.of("boom"), callback.failures);

        script.onClientData(Json.createObjectBuilder().add(ClientToServerModel.COMMAND_ID.toStringValue(), okID)
            .add(ClientToServerModel.RESULT.toStringValue(), "again").build());
        script.onClientData(Json.createObjectBuilder().add(ClientToServerModel.COMMAND_ID.toStringValue(), koID)
            .add(ClientToServerModel.ERROR_MSG.toStringValue(), "again").build());
        assertEquals(1, callback.successes.size());
        assertEquals(1, callback.failures.size());
    }

    @Test
    public void answersWithoutCallbackAreIgnoredAndOtherDataGoesToTheTerminalHandler() {
        final PWindow window = openWindow();
        PScript.call(window, "noop");
        final PScript script = scriptOf(window);
        script.onClientData(Json.createObjectBuilder().add(ClientToServerModel.COMMAND_ID.toStringValue(), 1)
            .add(ClientToServerModel.RESULT.toStringValue(), "r").build());
        script.onClientData(Json.createObjectBuilder().add(ClientToServerModel.COMMAND_ID.toStringValue(), 1)
            .add(ClientToServerModel.ERROR_MSG.toStringValue(), "e").build());

        final List<Object> terminal = new ArrayList<>();
        script.setTerminalHandler(terminal::add);
        script.onClientData(Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(),
            Json.createObjectBuilder().build()).build());
        assertEquals(1, terminal.size());
    }

    @Test
    public void theScriptIsReleasedWhenItsWindowCloses() {
        final PWindow window = openWindow();
        PScript.call(window, "x");
        final PScript script = scriptOf(window);
        assertNotNull(script);

        window.close();

        assertNull(scriptOf(window));
        writer.clear();
        PScript.call(window, "afterClose");
        assertTrue(writer.valuesOf(ServerToClientModel.FUNCTION_NAME).isEmpty(), "a destroyed window sends nothing");
    }

    @Test
    public void aCallNeedsAWindow() {
        assertThrows(IllegalArgumentException.class, () -> PScript.call(null, "x"));
    }

    @Test
    public void attachingTheScriptAgainListensOnce() {
        final PWindow window = openWindow();
        PScript.call(window, "first");
        final PScript script = scriptOf(window);
        assertFalse(script.attach(window, null));
    }
}
