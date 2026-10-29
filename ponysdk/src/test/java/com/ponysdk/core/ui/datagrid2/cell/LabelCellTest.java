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

import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.datagrid2.DomEvents;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class LabelCellTest extends PSuite {

    private LabelCell<String> cell;
    private PrimaryCellController<String> controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        cell = new LabelCell<>((v, s) -> {
        }, 80);
        controller = Mockito.mock(PrimaryCellController.class);
    }

    @Test
    public void renderWritesRenderingHelperInLabel() {
        cell.render("data", 12);

        Assertions.assertEquals("12", cell.asWidget().getText());
    }

    @Test
    public void pendingWidgetShowsEllipsis() {
        Assertions.assertEquals("...", ((PLabel) cell.asPendingWidget()).getText());
    }

    @Test
    public void doubleClickSwitchesToExtendedMode() {
        cell.setController(controller);

        DomEvents.domEvent(cell.asWidget(), DomHandlerType.DOUBLE_CLICK);

        Mockito.verify(controller).setExtendedMode();
    }

    @Test
    public void doubleClickWithoutControllerIsIgnored() {
        DomEvents.domEvent(cell.asWidget(), DomHandlerType.DOUBLE_CLICK);

        Mockito.verifyNoInteractions(controller);
    }

    @Test
    public void genExtendedCreatesTextBoxWithCurrentText() {
        cell.render("data", "hello");

        final Optional<ExtendedCell<String>> extended = cell.genExtended();

        Assertions.assertTrue(extended.isPresent());
        final PTextBox textBox = (PTextBox) extended.get().asWidget();
        Assertions.assertEquals("hello", textBox.getText());
        Assertions.assertEquals("80px", textBox.getWidth());
        Assertions.assertNotSame(extended.get(), cell.genExtended().get());
    }

    @Test
    public void selectAndUnselectKeepRenderedText() {
        cell.render("data", "x");
        cell.select();
        cell.unselect();

        Assertions.assertEquals("x", cell.asWidget().getText());
    }
}
