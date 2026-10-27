/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.writer;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelWriterTest extends PSuite {

    private ModelWriter modelWriter;
    private WebsocketEncoder websocketEncoder;

    @BeforeEach
    public void setUp() {
        websocketEncoder = Mockito.mock(WebsocketEncoder.class);
        modelWriter = new ModelWriter(websocketEncoder);
        assertNull(modelWriter.getCurrentWindow());
    }

    /**
     * Test method for {@link com.ponysdk.core.writer.ModelWriter#beginObject(com.ponysdk.core.ui.basic.PWindow)}.
     */
    @Test
    public void testBeginObject() {
        final PWindow window1 = Element.newPWindow(null, null);
        modelWriter.beginObject(window1);

        modelWriter.beginObject(window1);

        final PWindow window2 = Element.newPWindow(null, null);
        modelWriter.beginObject(window2);

        Mockito.verify(websocketEncoder, Mockito.times(3)).beginObject();
        Mockito.verify(websocketEncoder, Mockito.times(2)).encodeInt(ArgumentMatchers.eq(ServerToClientModel.WINDOW_ID), ArgumentMatchers.anyInt());
        assertEquals(window2, modelWriter.getCurrentWindow());
    }

    /**
     * Test method for {@link com.ponysdk.core.writer.ModelWriter#write(com.ponysdk.core.model.ServerToClientModel)}.
     */
    @Test
    public void testWriteNull() {
        modelWriter.write(null);
        Mockito.verify(websocketEncoder).encode(null, null);
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.writer.ModelWriter#write(com.ponysdk.core.model.ServerToClientModel, java.lang.Object)}.
     */
    @Test
    public void testWrite() {
        modelWriter.write(null, null);
        Mockito.verify(websocketEncoder).encode(null, null);
    }

    /**
     * Test method for {@link com.ponysdk.core.writer.ModelWriter#endObject()}.
     */
    @Test
    public void testEndObject() {
        modelWriter.endObject();
        Mockito.verify(websocketEncoder).endObject();
    }

    private static UpdateTarget target(final int id) {
        final UpdateTarget target = Mockito.mock(UpdateTarget.class);
        Mockito.when(target.getID()).thenReturn(id);
        Mockito.when(target.isUpdatable()).thenReturn(true);
        Mockito.when(target.beginCoalescedUpdate(ArgumentMatchers.any())).thenReturn(true);
        return target;
    }

    @Test
    public void onlyCoalescibleKeyedUpdatesAreDeferred() {
        final UpdateTarget target = target(5);
        assertEquals(0, modelWriter.pendingUpdateCount());
        assertFalse(modelWriter.deferUpdate(target, ServerToClientModel.TEXT, "off"), "coalescing is off");

        modelWriter.setCoalescing(true);
        assertTrue(modelWriter.isCoalescing());
        assertFalse(modelWriter.deferUpdate(target, ServerToClientModel.TYPE_UPDATE, 5), "not a property");
        assertFalse(modelWriter.deferUpdate(target, ServerToClientModel.PUT_STYLE_KEY, null, ServerToClientModel.STYLE_VALUE, "red"));
        assertFalse(modelWriter.deferUpdate(target, ServerToClientModel.ADD_STYLE_NAME, null));
        assertTrue(modelWriter.deferUpdate(target, ServerToClientModel.TEXT, "a"));
        assertTrue(modelWriter.deferUpdate(target, ServerToClientModel.PUT_STYLE_KEY, "color", ServerToClientModel.STYLE_VALUE, "red"));
        assertTrue(modelWriter.deferUpdate(target, ServerToClientModel.ADD_STYLE_NAME, "k"));
        assertEquals(3, modelWriter.pendingUpdateCount());

        modelWriter.setCoalescing(false); // drains
        assertEquals(0, modelWriter.pendingUpdateCount());
        Mockito.verify(websocketEncoder).encode(ServerToClientModel.TEXT, "a");
        Mockito.verify(websocketEncoder).encode(ServerToClientModel.ADD_STYLE_NAME, "k");
    }

    @Test
    public void updatesDrainedOutsideTheContextAreDropped() throws Exception {
        modelWriter.setCoalescing(true);
        assertTrue(modelWriter.deferUpdate(target(5), ServerToClientModel.TEXT, "a"));

        final Thread thread = Thread.ofVirtual().start(modelWriter::drainPendingUpdates);
        thread.join();

        assertEquals(0, modelWriter.pendingUpdateCount());
        Mockito.verify(websocketEncoder, Mockito.never()).encode(ServerToClientModel.TEXT, "a");
    }

    @Test
    public void aValueOfTheWrongTypeIsNamed() {
        assertTrue(ModelWriter.accepts(ServerToClientModel.TEXT, "a"));
        assertTrue(ModelWriter.accepts(null, 1));
        assertEquals("TEXT takes STRING, given Integer 5",
            assertThrows(AssertionError.class, () -> ModelWriter.accepts(ServerToClientModel.TEXT, 5)).getMessage());
        assertEquals("TYPE_UPDATE takes UINT31, given null",
            assertThrows(AssertionError.class, () -> ModelWriter.accepts(ServerToClientModel.TYPE_UPDATE, null)).getMessage());
    }

}
