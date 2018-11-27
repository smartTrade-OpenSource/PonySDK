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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.websocket.WebsocketEncoder;

public class ModelWriterTest {

    private ModelWriter modelWriter;
    private WebsocketEncoder websocketEncoder;

    @Before
    public void setUp() {
        websocketEncoder = Mockito.mock(WebsocketEncoder.class);
        modelWriter = new ModelWriter(websocketEncoder);
    }

    /**
     * Test method for {@link com.ponysdk.core.writer.ModelWriter#beginObject()}.
     */
    @Test
    public void testBeginObject() {
        modelWriter.beginObject(-1);
        Mockito.verify(websocketEncoder).beginObject();
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

}
