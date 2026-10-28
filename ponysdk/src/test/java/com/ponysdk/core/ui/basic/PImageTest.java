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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;
import static org.junit.jupiter.api.Assertions.*;

public class PImageTest extends PSuite {

    private static final String RESOURCE = "images/disclosure_closed.png";

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    @Test
    public void urlAndPositionTravelInTheCreate() {
        final PImage image = Element.newPImage("img.png", 1, 2, 3, 4);
        assertEquals(WidgetType.IMAGE, image.getWidgetType());
        PWindow.getMain().add(image);

        assertEquals(List.of("img.png"), writer.valuesOf(ServerToClientModel.URL));
        assertEquals(List.of(1), writer.valuesOf(ServerToClientModel.POSITION_LEFT));
        assertEquals(List.of(2), writer.valuesOf(ServerToClientModel.POSITION_TOP));
        assertEquals(List.of(3), writer.valuesOf(ServerToClientModel.IMAGE_WIDTH));
        assertEquals(List.of(4), writer.valuesOf(ServerToClientModel.IMAGE_HEIGHT));
    }

    /** The terminal reads the clip by position: left, top, width, height (image.ts). */
    @Test
    public void theClipIsWrittenInTheOrderTheTerminalReadsIt() {
        PWindow.getMain().add(Element.newPImage("img.png", 1, 2, 3, 4));
        final List<ServerToClientModel> models = writer.models();
        final int left = models.indexOf(ServerToClientModel.POSITION_LEFT);
        assertEquals(List.of(ServerToClientModel.POSITION_LEFT, ServerToClientModel.POSITION_TOP, ServerToClientModel.IMAGE_WIDTH,
            ServerToClientModel.IMAGE_HEIGHT), models.subList(left, left + 4));
    }

    @Test
    public void aUrlWithoutPositionSendsNoPosition() {
        PWindow.getMain().add(Element.newPImage("img.png"));
        assertEquals(List.of("img.png"), writer.valuesOf(ServerToClientModel.URL));
        assertEquals(0, writer.count(ServerToClientModel.POSITION_LEFT));
        assertEquals(0, writer.count(ServerToClientModel.IMAGE_WIDTH));
    }

    @Test
    public void noUrlSendsNoUrl() {
        final PImage image = Element.newPImage();
        assertNull(image.getUrl());
        PWindow.getMain().add(image);
        assertEquals(0, writer.count(ServerToClientModel.URL));
    }

    @Test
    public void setUrlIsSentOnlyOnceInitializedAndWhenChanged() {
        final PImage image = Element.newPImage();
        image.setUrl("before.png");
        assertTrue(writer.isEmpty());
        PWindow.getMain().add(image);
        assertEquals(List.of("before.png"), writer.valuesOf(ServerToClientModel.URL));
        writer.clear();

        image.setUrl("before.png");
        image.setUrl("after.png");
        assertEquals("after.png", image.getUrl());
        assertEquals(List.of("after.png"), writer.valuesOf(ServerToClientModel.URL));
        assertEquals("<img pid=\"" + image.getID() + "\" url=\"after.png\"></img>", image.dumpDOM());
    }

    @Test
    public void clickHandlerIsRegisteredAndFired() {
        final PImage image = Element.newPImage("i.png");
        PWindow.getMain().add(image);
        writer.clear();

        final List<String> clicks = new ArrayList<>();
        image.addClickHandler(e -> clicks.add("click"));
        assertEquals(List.of(image.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));

        PEmulator.click(image);
        assertEquals(List.of("click"), clicks);
    }

    @Test
    public void setStreamAsksTheTerminalForAnEmbeddedStream() {
        final PImage image = Element.newPImage();
        PWindow.getMain().add(image);
        writer.clear();

        image.setStream((request, response, uiContext) -> {
        });

        assertEquals(List.of(image.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(List.of(HandlerModel.HANDLER_EMBEDDED_STREAM_REQUEST.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));
        assertEquals(1, writer.count(ServerToClientModel.STREAM_REQUEST_ID));
    }

    @Test
    public void setStreamBeforeTheImageIsAddedAsksOnceItIsCreated() {
        final PImage image = Element.newPImage();
        image.setStream((request, response, uiContext) -> {
        });
        assertEquals(0, writer.count(ServerToClientModel.TYPE_ADD_HANDLER));

        PWindow.getMain().add(image);

        assertEquals(List.of(image.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(List.of(HandlerModel.HANDLER_EMBEDDED_STREAM_REQUEST.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));
        assertEquals(1, writer.count(ServerToClientModel.STREAM_REQUEST_ID));
    }

    @Test
    public void classPathUrlIsADataUrlOfTheResourceExtension() {
        final PImage.ClassPathURL url = new PImage.ClassPathURL(RESOURCE);
        assertTrue(url.toString().startsWith("ClassPathURL [url="));
        assertTrue(url.toString().contains("disclosure_closed.png"));

        final PImage image = Element.newPImage(url);
        assertTrue(image.getUrl().startsWith("data:image/png;base64,"));
    }

    @Test
    public void classPathUrlIsTheBase64OfTheResource() throws IOException {
        final byte[] bytes;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            bytes = in.readAllBytes();
        }
        assertEquals("data:image/png;base64," + Base64.getEncoder().encodeToString(bytes),
            new PImage.ClassPathURL(RESOURCE).toUrl());
    }

    @Test
    public void aPartialClipIsNotSent() {
        for (final PImage image : List.of(Element.newPImage("u", 1, -1, 3, 4), Element.newPImage("u", -1, 2, 3, 4),
            Element.newPImage("u", 1, 2, 3, -1), Element.newPImage("u", 1, 2, -1, 4))) {
            writer.clear();
            PWindow.getMain().add(image);
            assertEquals(List.of("u"), writer.valuesOf(ServerToClientModel.URL));
            assertEquals(0, writer.count(ServerToClientModel.POSITION_LEFT));
            assertEquals(0, writer.count(ServerToClientModel.IMAGE_WIDTH));
        }
    }
}
