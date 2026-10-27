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

package com.ponysdk.core.ui.list.renderer.cell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.list.refreshable.Cell;
import com.ponysdk.core.ui.place.Place;
import com.ponysdk.core.ui.place.PlaceChangeEvent;
import com.ponysdk.core.ui.place.PlaceChangeHandler;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

public class CellRendererTest extends PSuite {

    @Test
    public void testLabelCellRenderer() {
        final LabelCellRenderer<Integer> renderer = new LabelCellRenderer<>();
        assertEquals("12", renderer.render(0, 12).getText());
        assertEquals("-", renderer.render(0, null).getText());
        renderer.setNullDisplay("n/a");
        assertEquals("n/a", renderer.render(0, null).getText());
        assertEquals("none", new LabelCellRenderer<Integer>("none").render(0, null).getText());

        final PLabel label = renderer.render(0, 1);
        final Cell<Integer, PLabel> cell = new Cell<>();
        cell.setWidget(label);
        renderer.update(5, cell);
        assertEquals("5", label.getText());
    }

    @Test
    public void testHtmlCellRenderer() {
        final HtmlCellRenderer<String> renderer = new HtmlCellRenderer<>();
        assertEquals("<b>a</b>", renderer.render(0, "<b>a</b>").getHTML());
        assertEquals("-", renderer.render(0, null).getHTML());
        renderer.setNullDisplay("?");
        assertEquals("?", renderer.render(0, null).getHTML());
        assertEquals("empty", new HtmlCellRenderer<String>("empty").render(0, null).getHTML());

        final PHTML html = renderer.render(0, "x");
        final Cell<String, PHTML> cell = new Cell<>();
        cell.setWidget(html);
        renderer.update("y", cell);
        assertEquals("y", html.getText());
    }

    @Test
    public void testDateCellRenderer() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final Date date = new Date(0);
        assertEquals("1970-01-01", new DateCellRenderer(format).render(0, date).getText());
        assertEquals("-", new DateCellRenderer(format).render(0, null).getText());
        assertEquals("never", new DateCellRenderer(format, "never").render(0, null).getText());
        assertEquals("never", new DateCellRenderer("yyyy", "never").getValue(null));
        assertEquals(new SimpleDateFormat("yyyy").format(date), new DateCellRenderer("yyyy").getValue(date));
    }

    @Test
    public void testEmptyCellRenderer() {
        final EmptyCellRenderer<String> renderer = new EmptyCellRenderer<>();
        final PLabel label = renderer.render(0, "value");
        assertEquals("", label.getText());
        final Cell<String, PLabel> cell = new Cell<>();
        cell.setWidget(label);
        renderer.update("other", cell);
        assertEquals("", label.getText());
    }

    @Test
    public void testWidgetCellRenderer() {
        final WidgetCellRenderer<Integer> renderer = new WidgetCellRenderer<>() {

            @Override
            protected PWidget render0(final int row, final Integer rawValue) {
                return Element.newPLabel("#" + row + ":" + rawValue);
            }

            @Override
            public void update(final Integer value, final Cell<Integer, PWidget> current) {
            }
        };
        assertEquals("#2:7", ((PLabel) renderer.render(2, 7)).getText());
        assertEquals("-", ((PLabel) renderer.render(2, null)).getText());
        renderer.setNullDisplay("none");
        assertEquals("none", ((PLabel) renderer.render(2, null)).getText());
        assertEquals("7", renderer.getValue(7));

        final WidgetCellRenderer<Integer> withPlace = new WidgetCellRenderer<>(Place.NOWHERE, "nothing") {

            @Override
            protected PWidget render0(final int row, final Integer rawValue) {
                return Element.newPLabel();
            }

            @Override
            public void update(final Integer value, final Cell<Integer, PWidget> current) {
            }
        };
        assertEquals("nothing", ((PLabel) withPlace.render(0, null)).getText());
    }

    @Test
    public void testPlaceChangeCellRenderer() {
        final Place place = new Place() {
        };
        final List<Place> places = new ArrayList<>();
        final HandlerRegistration registration = UIContext.get().getRootEventBus().addHandler(PlaceChangeEvent.TYPE,
            (PlaceChangeHandler) event -> places.add(event.getNewPlace()));
        try {
            final PlaceChangeCellRenderer renderer = new PlaceChangeCellRenderer(place);
            final PWidget anchor = renderer.render(0, "Go");
            assertTrue(anchor instanceof PAnchor);
            assertEquals("Go", ((PAnchor) anchor).getText());
            PEmulator.click(anchor);
            assertEquals(List.of(place), places);

            assertEquals("-", ((PLabel) renderer.render(0, null)).getText());
            renderer.setNullDisplay("x");
            assertEquals("x", ((PLabel) renderer.render(0, null)).getText());
            assertEquals("y", ((PLabel) new PlaceChangeCellRenderer(place, "y").render(0, null)).getText());

            // Update is a no-op
            final Cell<String, PWidget> cell = new Cell<>();
            cell.setWidget(anchor);
            renderer.update("Other", cell);
            assertSame(anchor, cell.getWidget());
            assertEquals("Go", ((PAnchor) anchor).getText());
        } finally {
            registration.removeHandler();
        }
    }
}
