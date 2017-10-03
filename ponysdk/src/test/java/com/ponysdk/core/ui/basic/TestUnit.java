/*
 * Copyright (c) 2017 PonySDK
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

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;

public class TestUnit extends PSuite {

    @Test
    public void testLabel() {
        final PLabel label = Element.newPLabel("test");
        Assert.assertEquals("test", label.getText());
    }

    @Test
    public void testListbox() {
        final PListBox listBox = Element.newPListBox();
        listBox.addItem("Item1");
        listBox.addItem("Item2");
        listBox.addItem("Item3");
        listBox.addItem("Item4");
        listBox.addItem("Item5");
        Assert.assertEquals(5, listBox.getItemCount());
    }

    @Test
    public void testButton() {
        final PButton button = Element.newPButton("test");
        Assert.assertEquals("test", button.getText());
        button.setText("test2");
        Assert.assertEquals("test2", button.getText());

        final PClickEvent trueEvent = new PClickEvent(button);

        final PClickHandler handler = Mockito.mock(PClickHandler.class);
        button.addClickHandler(handler);

        button.addClickHandler(event -> assertEquals(trueEvent, event));

        button.fireEvent(trueEvent);
        Mockito.verify(handler);
    }

    @Test
    public void testCheckBox() {
        final PCheckBox checkBox = Element.newPCheckBox();
        Assert.assertEquals(PCheckBoxState.UNCHECKED, checkBox.getState());
        checkBox.setState(PCheckBoxState.CHECKED);
        Assert.assertEquals(PCheckBoxState.CHECKED, checkBox.getState());
    }

}
