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

package com.ponysdk.testutil;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

/**
 * A {@link ModelWriterForTest} that records every (model, value) written, for protocol assertions in
 * {@link com.ponysdk.test.PSuite} based tests. {@link #install()} swaps it into the current (spied)
 * UIContext; {@link #uninstall()} restores the previous writer.
 */
public final class CapturingModelWriter extends ModelWriterForTest {

    private final List<Object[]> writes = new ArrayList<>();
    private ModelWriter previous;

    public static CapturingModelWriter install() {
        final CapturingModelWriter w = new CapturingModelWriter();
        final UIContext ctx = UIContext.get();
        w.previous = ctx.getWriter();
        Mockito.doReturn(w).when(ctx).getWriter();
        return w;
    }

    public void uninstall() {
        Mockito.doReturn(previous).when(UIContext.get()).getWriter();
    }

    @Override
    public void write(final ServerToClientModel model, final Object value) {
        writes.add(new Object[] { model, value });
        super.write(model, value);
    }

    @Override
    public void endObject() {
        writes.add(new Object[] { ServerToClientModel.END, null });
    }

    public void clear() {
        writes.clear();
    }

    public boolean isEmpty() {
        return writes.isEmpty();
    }

    public long count(final ServerToClientModel model) {
        return writes.stream().filter(w -> w[0] == model).count();
    }

    /** All values written for {@code model}, in order. */
    public List<Object> valuesOf(final ServerToClientModel model) {
        final List<Object> out = new ArrayList<>();
        for (final Object[] w : writes) if (w[0] == model) out.add(w[1]);
        return out;
    }

    /** Values written right after a {@code key} write whose value is {@code name}. */
    public List<Object> valuesAfter(final ServerToClientModel key, final Object name) {
        final List<Object> out = new ArrayList<>();
        for (int i = 0; i < writes.size() - 1; i++) {
            if (writes.get(i)[0] == key && name.equals(writes.get(i)[1])) out.add(writes.get(i + 1)[1]);
        }
        return out;
    }

    public List<ServerToClientModel> models() {
        final List<ServerToClientModel> out = new ArrayList<>();
        for (final Object[] w : writes) out.add((ServerToClientModel) w[0]);
        return out;
    }
}
