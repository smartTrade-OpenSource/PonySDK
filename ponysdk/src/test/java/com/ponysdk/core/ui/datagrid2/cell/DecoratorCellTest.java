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
package com.ponysdk.core.ui.datagrid2.cell;

import com.ponysdk.core.ui.basic.PWidget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class DecoratorCellTest {

    @Test
    @SuppressWarnings("unchecked")
    public void delegatesEverythingToWrappedCell() {
        final PrimaryCell<String> wrapped = Mockito.mock(PrimaryCell.class);
        final PWidget widget = Mockito.mock(PWidget.class);
        final PWidget pending = Mockito.mock(PWidget.class);
        final ExtendedCell<String> extended = Mockito.mock(ExtendedCell.class);
        final PrimaryCellController<String> controller = Mockito.mock(PrimaryCellController.class);
        Mockito.when(wrapped.asWidget()).thenReturn(widget);
        Mockito.when(wrapped.asPendingWidget()).thenReturn(pending);
        Mockito.when(wrapped.genExtended()).thenReturn(Optional.of(extended));

        final DecoratorCell<String, PrimaryCell<String>> cell = new DecoratorCell<>(wrapped);

        Assertions.assertSame(wrapped, cell.getDelegate());
        Assertions.assertSame(widget, cell.asWidget());
        Assertions.assertSame(pending, cell.asPendingWidget());
        Assertions.assertSame(extended, cell.genExtended().orElseThrow());

        cell.render("v", "h");
        cell.setController(controller);
        cell.select();
        cell.unselect();

        Mockito.verify(wrapped).render("v", "h");
        Mockito.verify(wrapped).setController(controller);
        Mockito.verify(wrapped).select();
        Mockito.verify(wrapped).unselect();
    }

    @Test
    public void primaryCellHasNoExtendedCellByDefault() {
        final PrimaryCell<String> cell = new PrimaryCell<>() {

            @Override
            public PWidget asPendingWidget() {
                return null;
            }

            @Override
            public void setController(final PrimaryCellController<String> cellController) {
            }

            @Override
            public void render(final String data, final Object renderingHelper) {
            }

            @Override
            public void select() {
            }

            @Override
            public void unselect() {
            }

            @Override
            public PWidget asWidget() {
                return null;
            }
        };

        Assertions.assertTrue(cell.genExtended().isEmpty());
    }
}
