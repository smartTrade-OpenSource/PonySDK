/*
 * Copyright (c) 2017 PonySDK
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

package com.ponysdk.core.server.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.ponysdk.core.model.ServerToClientModel;

public class RecordingEncoderTest {

    /** Replays a recorder as "MODEL=value" strings, "END" for the end of a block. */
    private static List<String> replay(final RecordingEncoder recorder) {
        final List<String> out = new ArrayList<>();
        recorder.replayTo(new WebsocketEncoder() {

            @Override
            public void beginObject() {
            }

            @Override
            public void encode(final ServerToClientModel model, final Object value) {
                out.add(model + "=" + value);
            }

            @Override
            public void endObject() {
                out.add("END");
            }
        });
        return out;
    }

    private static void block(final RecordingEncoder recorder, final ServerToClientModel type, final int id, final Object... pairs) {
        recorder.encode(type, id);
        for (int i = 0; i < pairs.length; i += 2) {
            recorder.encode((ServerToClientModel) pairs[i], pairs[i + 1]);
        }
        recorder.endObject();
    }

    @Test
    public void theLastValueOfASetterWins() {
        final RecordingEncoder recorder = new RecordingEncoder();
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.TEXT, "a");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.TEXT, "b");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.TEXT, "c");
        recorder.compact();

        assertEquals(List.of("TYPE_UPDATE=1", "TEXT=c", "END"), replay(recorder));
    }

    @Test
    public void differentStyleKeysDoNotOverwriteEachOther() {
        final RecordingEncoder recorder = new RecordingEncoder();
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.PUT_STYLE_KEY, "color", ServerToClientModel.STYLE_VALUE,
            "red");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.PUT_STYLE_KEY, "width", ServerToClientModel.STYLE_VALUE,
            "10px");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.PUT_STYLE_KEY, "color", ServerToClientModel.STYLE_VALUE,
            "blue");
        recorder.compact();

        assertEquals(List.of("TYPE_UPDATE=1", "PUT_STYLE_KEY=width", "STYLE_VALUE=10px", "PUT_STYLE_KEY=color", "STYLE_VALUE=blue", "END"),
            replay(recorder));
    }

    @Test
    public void anObjectCreatedThenCollectedWhileSuspendedIsDropped() {
        final RecordingEncoder recorder = new RecordingEncoder();
        block(recorder, ServerToClientModel.TYPE_CREATE, 5, ServerToClientModel.WIDGET_TYPE, 1);
        block(recorder, ServerToClientModel.TYPE_UPDATE, 5, ServerToClientModel.TEXT, "x");
        block(recorder, ServerToClientModel.TYPE_GC, 5);
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.TEXT, "kept");
        recorder.compact();

        assertEquals(List.of("TYPE_UPDATE=1", "TEXT=kept", "END"), replay(recorder));
    }

    @Test
    public void aWidgetMovedToAnotherParentWhileSuspendedIsKept() {
        final RecordingEncoder recorder = new RecordingEncoder();
        // TYPE_REMOVE takes a widget out of its parent: it lives on and is added again elsewhere
        block(recorder, ServerToClientModel.TYPE_CREATE, 5, ServerToClientModel.WIDGET_TYPE, 1);
        block(recorder, ServerToClientModel.TYPE_ADD, 5, ServerToClientModel.PARENT_OBJECT_ID, 10);
        block(recorder, ServerToClientModel.TYPE_REMOVE, 5, ServerToClientModel.PARENT_OBJECT_ID, 10);
        block(recorder, ServerToClientModel.TYPE_ADD, 5, ServerToClientModel.PARENT_OBJECT_ID, 11);
        recorder.compact();

        assertEquals(List.of("TYPE_CREATE=5", "WIDGET_TYPE=1", "END", "TYPE_ADD=5", "PARENT_OBJECT_ID=10", "END", "TYPE_REMOVE=5",
            "PARENT_OBJECT_ID=10", "END", "TYPE_ADD=5", "PARENT_OBJECT_ID=11", "END"), replay(recorder));
    }

    @Test
    public void aCollectedObjectThatIsAnotherOnesParentIsKept() {
        final RecordingEncoder recorder = new RecordingEncoder();
        block(recorder, ServerToClientModel.TYPE_CREATE, 5, ServerToClientModel.WIDGET_TYPE, 1);
        block(recorder, ServerToClientModel.TYPE_CREATE, 6, ServerToClientModel.WIDGET_TYPE, 1);
        block(recorder, ServerToClientModel.TYPE_ADD, 6, ServerToClientModel.PARENT_OBJECT_ID, 5);
        block(recorder, ServerToClientModel.TYPE_GC, 5);
        recorder.compact();

        assertEquals("the ADD of 6 must find its parent 5", 11, replay(recorder).size());
    }

    @Test
    public void anObjectRemovedBeforeItIsCreatedAgainIsKept() {
        final RecordingEncoder recorder = new RecordingEncoder();
        block(recorder, ServerToClientModel.TYPE_REMOVE, 5);
        block(recorder, ServerToClientModel.TYPE_CREATE, 5, ServerToClientModel.WIDGET_TYPE, 1);
        recorder.compact();

        assertEquals(List.of("TYPE_REMOVE=5", "END", "TYPE_CREATE=5", "WIDGET_TYPE=1", "END"), replay(recorder));
    }

    @Test
    public void aCommandKeepsItsPositionAmongUpdates() {
        final RecordingEncoder recorder = new RecordingEncoder();
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.TEXT, "a");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.ADD_STYLE_NAME, "selected");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.TEXT, "b");
        recorder.compact();

        assertEquals(List.of("TYPE_UPDATE=1", "TEXT=a", "END", "TYPE_UPDATE=1", "ADD_STYLE_NAME=selected", "END", "TYPE_UPDATE=1",
            "TEXT=b", "END"), replay(recorder));
    }

    @Test
    public void aCommandOnOneObjectDoesNotStopTheOthersFromMerging() {
        final RecordingEncoder recorder = new RecordingEncoder();
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.TEXT, "a");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 2, ServerToClientModel.TEXT, "x");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.PADDON_METHOD, "blink");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 2, ServerToClientModel.TEXT, "y");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 1, ServerToClientModel.TEXT, "b");
        recorder.compact();

        assertEquals(List.of("TYPE_UPDATE=1", "TEXT=a", "END", "TYPE_UPDATE=1", "PADDON_METHOD=blink", "END", "TYPE_UPDATE=2",
            "TEXT=y", "END", "TYPE_UPDATE=1", "TEXT=b", "END"), replay(recorder));
    }

    @Test
    public void aBlinkingPriceGridKeepsWithinItsLimit() {
        final AtomicInteger overflows = new AtomicInteger();
        final RecordingEncoder recorder = new RecordingEncoder(1000, (count, max) -> overflows.incrementAndGet());
        for (int tick = 0; tick < 1000; tick++) {
            final int cell = tick % 20;
            block(recorder, ServerToClientModel.TYPE_UPDATE, cell, ServerToClientModel.TEXT, "p" + tick);
            // A blink every tenth tick: a global barrier would keep nearly every tick (~2 000 entries)
            if (tick % 10 == 0) block(recorder, ServerToClientModel.TYPE_UPDATE, cell, ServerToClientModel.PADDON_METHOD, "blink");
        }

        assertFalse("overflowed " + overflows.get() + " times", recorder.isOverflowed());
    }

    @Test
    public void aMovedBlockKeepsTheWindowItWasWrittenTo() {
        final RecordingEncoder recorder = new RecordingEncoder();
        // As ModelWriter writes them: WINDOW_ID only when the window changes
        recorder.encode(ServerToClientModel.WINDOW_ID, 1);
        block(recorder, ServerToClientModel.TYPE_UPDATE, 10, ServerToClientModel.TEXT, "a");
        recorder.encode(ServerToClientModel.WINDOW_ID, 2);
        block(recorder, ServerToClientModel.TYPE_UPDATE, 20, ServerToClientModel.TEXT, "x");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 21, ServerToClientModel.TEXT, "y");
        recorder.encode(ServerToClientModel.WINDOW_ID, 1);
        block(recorder, ServerToClientModel.TYPE_UPDATE, 10, ServerToClientModel.TEXT, "b");
        block(recorder, ServerToClientModel.TYPE_UPDATE, 10, ServerToClientModel.PADDON_METHOD, "blink");
        recorder.encode(ServerToClientModel.WINDOW_ID, 2);
        block(recorder, ServerToClientModel.TYPE_UPDATE, 20, ServerToClientModel.TEXT, "z");
        recorder.compact();

        assertEquals(List.of("WINDOW_ID=1", "TYPE_UPDATE=10", "TEXT=b", "END", "TYPE_UPDATE=10", "PADDON_METHOD=blink", "END",
            "WINDOW_ID=2", "TYPE_UPDATE=20", "TEXT=z", "END", "TYPE_UPDATE=21", "TEXT=y", "END"), replay(recorder));
    }

    @Test
    public void aTickerKeepsWithinItsLimitByCompactingOnTheWay() {
        final AtomicInteger overflows = new AtomicInteger();
        final RecordingEncoder recorder = new RecordingEncoder(100, (count, max) -> overflows.incrementAndGet());
        for (int tick = 0; tick < 1000; tick++) {
            for (int cell = 0; cell < 5; cell++) {
                block(recorder, ServerToClientModel.TYPE_UPDATE, cell, ServerToClientModel.TEXT, "p" + tick);
            }
        }

        assertEquals(0, overflows.get());
        assertFalse(recorder.isOverflowed());
        recorder.compact();
        assertEquals(15, replay(recorder).size()); // 5 cells x (TYPE_UPDATE, TEXT, END)
        assertTrue(replay(recorder).contains("TEXT=p999"));
    }

    @Test
    public void distinctStateThatDoesNotFitOverflows() {
        final AtomicInteger overflows = new AtomicInteger();
        final RecordingEncoder recorder = new RecordingEncoder(100, (count, max) -> overflows.incrementAndGet());
        for (int id = 0; id < 100; id++) {
            block(recorder, ServerToClientModel.TYPE_CREATE, id, ServerToClientModel.WIDGET_TYPE, 1);
        }

        assertTrue(recorder.isOverflowed());
        assertEquals(1, overflows.get());
    }
}
