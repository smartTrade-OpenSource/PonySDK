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
package com.ponysdk.core.ui.flexlayout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.test.ModelWriterForTest;
import com.ponysdk.test.PEmulator;

/**
 * Model writer that records the terminal method calls (PADDON_METHOD + PADDON_ARGUMENTS) and the
 * creation arguments written for each add-on, keeping the emulation of {@link ModelWriterForTest}.
 */
final class TerminalCallRecorder extends ModelWriterForTest {

    static final class Call {

        final int objectId;
        final String method;
        Object[] args = new Object[0];

        Call(final int objectId, final String method) {
            this.objectId = objectId;
            this.method = method;
        }

        @Override
        public String toString() {
            return method + java.util.Arrays.toString(args);
        }
    }

    private final List<Call> calls = new ArrayList<>();
    private final Map<Integer, Object[]> creationArgs = new HashMap<>();
    private final List<Integer> pendingCloses = new ArrayList<>();
    private boolean deferWindowClose;
    private int currentId;

    static TerminalCallRecorder install() {
        final TerminalCallRecorder recorder = new TerminalCallRecorder();
        Mockito.doReturn(recorder).when(UIContext.get()).getWriter();
        return recorder;
    }

    static void uninstall() {
        Mockito.doReturn(new ModelWriterForTest()).when(UIContext.get()).getWriter();
    }

    @Override
    public void write(final ServerToClientModel model, final Object value) {
        if (model == ServerToClientModel.TYPE_CREATE || model == ServerToClientModel.TYPE_UPDATE) {
            currentId = (int) value;
        } else if (model == ServerToClientModel.PADDON_METHOD) {
            calls.add(new Call(currentId, (String) value));
        } else if (model == ServerToClientModel.PADDON_ARGUMENTS) {
            calls.get(calls.size() - 1).args = (Object[]) value;
        } else if (model == ServerToClientModel.PADDON_CREATION_ARGS) {
            creationArgs.put(currentId, (Object[]) value);
        } else if (model == ServerToClientModel.CLOSE && deferWindowClose) {
            pendingCloses.add(currentId);
            return;
        }
        super.write(model, value);
    }

    List<Call> calls(final PObject object) {
        return calls.stream().filter(c -> c.objectId == object.getID()).collect(Collectors.toList());
    }

    List<String> methods(final PObject object) {
        return calls(object).stream().map(c -> c.method).collect(Collectors.toList());
    }

    Call last(final PObject object) {
        final List<Call> own = calls(object);
        assertTrue(!own.isEmpty(), "no terminal call recorded");
        return own.get(own.size() - 1);
    }

    void assertLast(final PObject object, final String method, final Object... args) {
        final Call call = last(object);
        assertEquals(method, call.method);
        assertArrayEquals(args, call.args, call.toString());
    }

    Object[] creationArgs(final PObject object) {
        return creationArgs.get(object.getID());
    }

    /**
     * Holds the CLOSE of a PWindow until {@link #deliverWindowCloses()}, as a real terminal answers it
     * in a later round trip instead of inside the {@code close()} call.
     */
    void deferWindowClose() {
        deferWindowClose = true;
    }

    void deliverWindowCloses() {
        final List<Integer> ids = new ArrayList<>(pendingCloses);
        pendingCloses.clear();
        ids.forEach(id -> PEmulator.windowClosed(UIContext.get().getObject(id)));
    }

    void clear() {
        calls.clear();
    }

    /** Emulates {@code pony.sendDataToServer} from the add-on's terminal side. */
    static void fire(final PObject object, final JsonObjectBuilder data) {
        final JsonObject event = Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), data).build();
        object.onClientData(event);
    }
}
