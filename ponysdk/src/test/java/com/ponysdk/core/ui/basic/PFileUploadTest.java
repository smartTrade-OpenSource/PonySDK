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
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PChangeEvent;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PFileUploadTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PFileUpload attached() {
        final PFileUpload upload = Element.newPFileUpload();
        PWindow.getMain().add(upload);
        return upload;
    }

    private static JsonObject change(final String fileName, final int size) {
        return Json.createObjectBuilder()
            .add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), fileName)
            .add(ClientToServerModel.SIZE.toStringValue(), size)
            .build();
    }

    @Test
    public void testInit() {
        final PFileUpload upload = Element.newPFileUpload();
        assertEquals(WidgetType.FILE_UPLOAD, upload.getWidgetType());
        assertTrue(upload.isEnabled());
        assertNull(upload.getName());
        assertNull(upload.getFileName());
        assertEquals(0, upload.getFileSize());
        assertEquals("<upload pid=\"" + upload.getID() + "\"></upload>", upload.dumpDOM());
    }

    @Test
    public void testChangeHandlerRegisteredOnInit() {
        writer.clear();
        final PFileUpload upload = attached();
        assertEquals(List.of(upload.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));
        assertTrue(writer.valuesOf(ServerToClientModel.HANDLER_TYPE).contains(HandlerModel.HANDLER_CHANGE.getValue()));
    }

    @Test
    public void testChangeFromClient() {
        final PFileUpload upload = attached();
        final List<PChangeEvent> events = new ArrayList<>();
        upload.addChangeHandler(events::add);
        upload.onClientData(change("report.pdf", 2048));
        assertEquals("report.pdf", upload.getFileName());
        assertEquals(2048, upload.getFileSize());
        assertEquals(1, events.size());
        assertSame(upload, events.get(0).getSource());
    }

    @Test
    public void testSubmitCompleteFromClient() {
        final PFileUpload upload = attached();
        final AtomicInteger completes = new AtomicInteger();
        upload.addSubmitCompleteHandler(completes::incrementAndGet);
        upload.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_SUBMIT_COMPLETE.toStringValue(), 0).build());
        assertEquals(1, completes.get());
    }

    @Test
    public void testClientDataIgnoredWhenHidden() {
        final PFileUpload upload = attached();
        final List<PChangeEvent> events = new ArrayList<>();
        upload.addChangeHandler(events::add);
        upload.setVisible(false);
        upload.onClientData(change("x", 1));
        assertTrue(events.isEmpty());
        assertNull(upload.getFileName());
    }

    @Test
    public void testOtherClientDataDelegated() {
        final PFileUpload upload = attached();
        final List<PChangeEvent> events = new ArrayList<>();
        upload.addChangeHandler(events::add);
        upload.onClientData(Json.createObjectBuilder().add("unknown", 1).build());
        assertTrue(events.isEmpty());
    }

    @Test
    public void testSubmitStacksAnEmbeddedStreamRequest() {
        final PFileUpload upload = attached();
        upload.addStreamHandler((request, response, uiContext) -> {
        });
        writer.clear();
        upload.submit();
        assertEquals(List.of(upload.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(List.of(HandlerModel.HANDLER_EMBEDDED_STREAM_REQUEST.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));
        assertEquals(1, writer.count(ServerToClientModel.STREAM_REQUEST_ID));
    }

    @Test
    public void testPropertiesSentOnlyWhenChanged() {
        final PFileUpload upload = attached();
        writer.clear();
        upload.setName("file");
        upload.setName("file");
        upload.setLabel("Browse");
        upload.setLabel("Browse");
        upload.setEnabled(true);
        upload.setEnabled(false);
        upload.setEnabled(false);
        upload.reset();
        upload.setFileName("manual.txt");

        assertEquals("file", upload.getName());
        assertFalse(upload.isEnabled());
        assertEquals("manual.txt", upload.getFileName());
        assertEquals(List.of("file"), writer.valuesOf(ServerToClientModel.NAME));
        assertEquals(List.of("Browse"), writer.valuesOf(ServerToClientModel.TEXT));
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.ENABLED));
        assertEquals(1, writer.count(ServerToClientModel.CLEAR));
    }

}
