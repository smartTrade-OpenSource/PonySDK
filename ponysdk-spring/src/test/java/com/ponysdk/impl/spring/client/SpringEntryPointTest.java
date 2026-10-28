/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.impl.spring.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.activity.Activity;
import com.ponysdk.core.ui.basic.PAcceptsOneWidget;
import com.ponysdk.core.ui.basic.PHistory;
import com.ponysdk.core.ui.basic.PRootLayoutPanel;
import com.ponysdk.core.ui.basic.PSimpleLayoutPanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.place.Place;
import com.ponysdk.core.ui.place.PlaceController;
import com.ponysdk.core.ui.place.PlaceHistoryMapper;
import com.ponysdk.test.PSuite;

public class SpringEntryPointTest extends PSuite {

    private record NamedPlace(String token) implements Place {
    }

    private static final NamedPlace HOME = new NamedPlace("home");
    private static final NamedPlace DETAIL = new NamedPlace("detail");

    private final List<Place> started = new ArrayList<>();
    private final List<PAcceptsOneWidget> worlds = new ArrayList<>();

    private TestEntryPoint entryPoint;

    private final class TestEntryPoint extends SpringEntryPoint {

        @Override
        public void start(final UIContext uiContext) {
            start(HOME);
        }
    }

    private final class RecordingActivity implements Activity {

        @Override
        public void start(final PAcceptsOneWidget world, final Place place) {
            worlds.add(world);
            started.add(place);
        }

        @Override
        public void stop() {
        }
    }

    @BeforeEach
    public void setUp() {
        final RecordingActivity activity = new RecordingActivity();
        final Map<String, NamedPlace> places = Map.of(HOME.token(), HOME, DETAIL.token(), DETAIL);

        entryPoint = new TestEntryPoint();
        entryPoint.eventBus = UIContext.get().getRootEventBus();
        entryPoint.history = new PHistory();
        entryPoint.placeController = new PlaceController(entryPoint.history, entryPoint.eventBus);
        entryPoint.mapper = place -> activity;
        entryPoint.historyMapper = new PlaceHistoryMapper() {

            @Override
            public Place getPlace(final String token) {
                return places.get(token);
            }

            @Override
            public String getToken(final Place place) {
                return ((NamedPlace) place).token();
            }
        };
    }

    @Test
    public void withoutAHistoryTokenTheDefaultPlaceIsShownInTheRootPanel() {
        entryPoint.start(UIContext.get());

        assertEquals(List.of(HOME), started);
        final PSimpleLayoutPanel panel = assertInstanceOf(PSimpleLayoutPanel.class, worlds.get(0));
        assertSame(PRootLayoutPanel.get(PWindow.getMain()), panel.getParent());
        assertEquals(HOME.token(), entryPoint.history.getToken());
    }

    @Test
    public void theCurrentHistoryTokenWinsOverTheDefaultPlace() {
        entryPoint.history.newItem(DETAIL.token(), false);

        entryPoint.start(UIContext.get());

        assertEquals(List.of(DETAIL), started);
    }

    @Test
    public void aLaterHistoryChangeShowsItsPlace() {
        entryPoint.start(UIContext.get());

        entryPoint.history.fireHistoryChanged(DETAIL.token());

        assertEquals(List.of(HOME, DETAIL), started);
    }

}
