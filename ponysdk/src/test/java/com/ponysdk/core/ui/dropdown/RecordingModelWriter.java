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
package com.ponysdk.core.ui.dropdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.test.ModelWriterForTest;

/**
 * Test writer that keeps every instruction sent to the client, keyed by the object it targets.
 */
public class RecordingModelWriter extends ModelWriterForTest {

    public record Entry(int objectId, ServerToClientModel model, Object value) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private int currentObjectId;

    public static RecordingModelWriter install() {
        final RecordingModelWriter writer = new RecordingModelWriter();
        Mockito.doReturn(writer).when(UIContext.get()).getWriter();
        return writer;
    }

    public static void uninstall() {
        Mockito.doReturn(new ModelWriterForTest()).when(UIContext.get()).getWriter();
    }

    @Override
    public void write(final ServerToClientModel model, final Object value) {
        if (model == ServerToClientModel.TYPE_CREATE || model == ServerToClientModel.TYPE_UPDATE) currentObjectId = (int) value;
        entries.add(new Entry(currentObjectId, model, value));
        super.write(model, value);
    }

    public void clear() {
        entries.clear();
    }

    public List<Object> values(final PObject object, final ServerToClientModel model) {
        final List<Object> values = new ArrayList<>();
        for (final Entry entry : entries) {
            if (entry.objectId() == object.getID() && entry.model() == model) values.add(entry.value());
        }
        return values;
    }

    public List<Object> addonMethods(final PObject object) {
        return values(object, ServerToClientModel.PADDON_METHOD);
    }

    /**
     * Pairs each key instruction with the value instruction that follows it, e.g. a style property and its value.
     */
    public Map<Object, Object> pairs(final PObject object, final ServerToClientModel keyModel, final ServerToClientModel valueModel) {
        final Map<Object, Object> pairs = new LinkedHashMap<>();
        for (int i = 0; i < entries.size() - 1; i++) {
            final Entry entry = entries.get(i);
            final Entry next = entries.get(i + 1);
            if (entry.objectId() == object.getID() && entry.model() == keyModel && next.model() == valueModel) {
                pairs.put(entry.value(), next.value());
            }
        }
        return pairs;
    }
}
