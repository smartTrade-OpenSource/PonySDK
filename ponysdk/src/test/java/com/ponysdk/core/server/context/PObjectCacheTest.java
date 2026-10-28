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

package com.ponysdk.core.server.context;

import com.ponysdk.test.ModelWriterForTest;
import com.ponysdk.test.PSuite;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.LogLevel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.util.IntObjectHashMap;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.writer.ModelWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PObjectCacheTest extends PSuite {

    private PObjectCache cache;

    private final List<String> written = new ArrayList<>();
    private ModelWriter previousWriter;

    @BeforeEach
    public void setUp() {
        cache = new PObjectCache();
        previousWriter = UIContext.get().getWriter();
        final ModelWriter recorder = new ModelWriterForTest() {

            @Override
            public void beginObject(final PWindow window) {
                written.add("begin " + window.getID());
            }

            @Override
            public void write(final ServerToClientModel model, final Object value) {
                if (model == ServerToClientModel.TYPE_GC || model == ServerToClientModel.FRAME_ID) written.add(model + " " + value);
            }
        };
        Mockito.when(UIContext.get().getWriter()).thenReturn(recorder);
    }

    @AfterEach
    public void tearDown() {
        Mockito.when(UIContext.get().getWriter()).thenReturn(previousWriter);
    }

    private Reference<?> reference(final int objectID) throws ReflectiveOperationException {
        final Field field = PObjectCache.class.getDeclaredField("referenceByObjectID");
        field.setAccessible(true);
        return (Reference<?>) ((IntObjectHashMap<?>) field.get(cache)).get(objectID);
    }

    /** Stands for the collector: enqueues the weak reference of the object without waiting for a GC. */
    private void collect(final int objectID) throws ReflectiveOperationException {
        reference(objectID).enqueue();
    }

    /**
     * Test method for {@link com.ponysdk.core.server.context.PObjectCache#add(com.ponysdk.core.ui.basic.PObject)}.
     */
    @Test
    public void testAdd() {
        final PObject uiObject1 = Element.newButton();
        final int uiObjectId1 = uiObject1.getID();

        cache.add(uiObject1);
        assertEquals(uiObject1, cache.get(uiObjectId1));
    }

    @Test
    public void testGetOfAnUnknownIdIsNull() {
        assertNull(cache.get(-42));
    }

    @Test
    public void testACollectedObjectOfTheMainWindowIsForgottenAndTheClientTold() throws ReflectiveOperationException {
        final PButton button = Element.newPButton();
        PWindow.getMain().add(button);
        cache.add(button);
        collect(button.getID());
        written.clear();

        assertNull(cache.get(button.getID()));
        assertEquals(List.of("begin " + PWindow.getMain().getID(), ServerToClientModel.TYPE_GC + " " + button.getID()), written);
    }

    @Test
    public void testTheDebugLogChangesNothingToWhatTheClientIsTold() throws ReflectiveOperationException {
        final PButton button = Element.newPButton();
        PWindow.getMain().add(button);
        try (LogLevel debug = LogLevel.set(PObjectCache.class, "DEBUG")) {
            cache.add(button);
            collect(button.getID());
            written.clear();

            assertNull(cache.get(button.getID()));
        }

        assertEquals(List.of("begin " + PWindow.getMain().getID(), ServerToClientModel.TYPE_GC + " " + button.getID()), written);
    }

    @Test
    public void testACollectedObjectWithoutWindowIsForgottenSilently() throws ReflectiveOperationException {
        final PObject detached = Element.newButton();
        cache.add(detached);
        collect(detached.getID());

        assertNull(cache.get(detached.getID()));
        assertEquals(List.of(), written);
    }

    @Test
    public void testAStaleReferenceDoesNotEvictItsReplacement() throws ReflectiveOperationException {
        final PObject object = Element.newButton();
        cache.add(object);
        final Reference<?> first = reference(object.getID());
        cache.add(object); // a second registration of the id replaces the first reference

        first.enqueue();

        assertSame(object, cache.get(object.getID()), "only the reference still in the slot is removed");
    }

}
