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
package com.ponysdk.core.ui.datagrid2.column;

import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ColumnDefinitionStateTest {

    @Test
    public void unpinnedShownTransitions() {
        final State s = State.UNPINNED_SHOWN;
        Assertions.assertEquals(State.UNPINNED_HIDDEN, s.onHide());
        Assertions.assertEquals(State.UNPINNED_SHOWN, s.onShow());
        Assertions.assertEquals(State.PINNED_SHOWN, s.onPin());
        Assertions.assertEquals(State.UNPINNED_SHOWN, s.onUnpin());
        Assertions.assertFalse(s.isPinned());
        Assertions.assertTrue(s.isShown());
    }

    @Test
    public void pinnedShownCannotBeHidden() {
        final State s = State.PINNED_SHOWN;
        Assertions.assertEquals(State.PINNED_SHOWN, s.onHide());
        Assertions.assertEquals(State.PINNED_SHOWN, s.onShow());
        Assertions.assertEquals(State.PINNED_SHOWN, s.onPin());
        Assertions.assertEquals(State.UNPINNED_SHOWN, s.onUnpin());
        Assertions.assertTrue(s.isPinned());
        Assertions.assertTrue(s.isShown());
    }

    @Test
    public void unpinnedHiddenCannotBePinned() {
        final State s = State.UNPINNED_HIDDEN;
        Assertions.assertEquals(State.UNPINNED_HIDDEN, s.onHide());
        Assertions.assertEquals(State.UNPINNED_SHOWN, s.onShow());
        Assertions.assertEquals(State.UNPINNED_HIDDEN, s.onPin());
        Assertions.assertEquals(State.UNPINNED_HIDDEN, s.onUnpin());
        Assertions.assertFalse(s.isPinned());
        Assertions.assertFalse(s.isShown());
    }

    @Test
    public void columnKeepsIdAndDefinition() {
        final ColumnDefinition<String> def = new DecoratorColumnDefinition<>(null) {
        };
        final Column<String> column = new Column<>(7, def);

        Assertions.assertEquals(7, column.getID());
        Assertions.assertSame(def, column.getColDef());
    }
}
