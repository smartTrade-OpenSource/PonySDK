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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PDragEndEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragOverEvent;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PMouseDownEvent;
import com.ponysdk.core.ui.basic.event.PMouseOutEvent;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpEvent;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PFocusPanelTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static JsonObject domEvent(final DomHandlerType type) {
        return Json.createObjectBuilder().add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), type.getValue()).build();
    }

    @Test
    public void everyHandlerIsRegisteredAndReceivesItsEvent() {
        final PFocusPanel panel = Element.newPFocusPanel();
        assertEquals(WidgetType.FOCUS_PANEL, panel.getWidgetType());
        PWindow.getMain().add(panel);
        writer.clear();

        final List<Event<?>> events = new ArrayList<>();
        panel.addDragEndHandler(events::add);
        panel.addDragEnterHandler(events::add);
        panel.addClickHandler(events::add);
        panel.addMouseOverHandler(events::add);
        panel.addFocusHandler(events::add);
        panel.addBlurHandler(events::add);
        panel.addMouseOutHandler(events::add);
        panel.addMouseDownHandler(events::add);
        panel.addMouseUpHandler(events::add);
        panel.addDragStartHandler(events::add);
        panel.addDragLeaveHandler(events::add);
        panel.addDragOverHandler(events::add);
        panel.addDropHandler(events::add);

        assertEquals(List.of(HandlerModel.HANDLER_DOM_DRAG_END.getValue(), HandlerModel.HANDLER_DOM_DRAG_ENTER.getValue(),
            HandlerModel.HANDLER_DOM_CLICK.getValue(), HandlerModel.HANDLER_DOM_MOUSE_OVER.getValue(),
            HandlerModel.HANDLER_DOM_FOCUS.getValue(), HandlerModel.HANDLER_DOM_BLUR.getValue(),
            HandlerModel.HANDLER_DOM_MOUSE_OUT.getValue(), HandlerModel.HANDLER_DOM_MOUSE_DOWN.getValue(),
            HandlerModel.HANDLER_DOM_MOUSE_UP.getValue(), HandlerModel.HANDLER_DOM_DRAG_START.getValue(),
            HandlerModel.HANDLER_DOM_DRAG_LEAVE.getValue(), HandlerModel.HANDLER_DOM_DRAG_OVER.getValue(),
            HandlerModel.HANDLER_DOM_DROP.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));

        for (final DomHandlerType type : new DomHandlerType[] { DomHandlerType.DRAG_END, DomHandlerType.DRAG_ENTER,
                DomHandlerType.CLICK, DomHandlerType.MOUSE_OVER, DomHandlerType.FOCUS, DomHandlerType.BLUR,
                DomHandlerType.MOUSE_OUT, DomHandlerType.MOUSE_DOWN, DomHandlerType.MOUSE_UP, DomHandlerType.DRAG_START,
                DomHandlerType.DRAG_LEAVE, DomHandlerType.DRAG_OVER, DomHandlerType.DROP }) {
            panel.onClientData(domEvent(type));
        }
        assertEquals(List.of(PDragEndEvent.class, PDragEnterEvent.class, PClickEvent.class, PMouseOverEvent.class,
            PFocusEvent.class, PBlurEvent.class, PMouseOutEvent.class, PMouseDownEvent.class, PMouseUpEvent.class,
            PDragStartEvent.class, PDragLeaveEvent.class, PDragOverEvent.class, PDropEvent.class),
            events.stream().map(Object::getClass).collect(Collectors.toList()));
    }
}
