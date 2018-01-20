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

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.ui.model.PKeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class BasicUITest extends PSuite {

    @Test
    public void testLabel() {
        final PLabel label = Element.newPLabel("test");
        Assert.assertEquals("test", label.getText());
    }

    @Test
    public void testListbox() {
        final PListBox listBox = Element.newPListBox();

        final int itemCount = 10;

        for (int i = 0; i < itemCount; i++) {
            listBox.addItem("Item" + i);
        }

        Assert.assertEquals(itemCount, listBox.getItemCount());

        for (int i = 0; i < itemCount; i++) {
            Assert.assertEquals("Item" + i, listBox.getItem(i));
        }

        listBox.setSelectedIndex(itemCount - 1);
        Assert.assertEquals("Item" + (itemCount - 1), listBox.getItem(itemCount - 1));

        //Assert.assertNull(listBox.getSelectedValue()); //TODO nciaravola behaviour ?
    }

    @Test
    public void testButton() {
        final PButton button = Element.newPButton("test");
        Assert.assertEquals("test", button.getText());
        button.setText("test2");
        Assert.assertEquals("test2", button.getText());

        final PClickEvent event = new PClickEvent(button);
        final PClickHandler handler = Mockito.mock(PClickHandler.class);
        button.addClickHandler(handler);
        button.fireEvent(event);
        Mockito.verify(handler, Mockito.times(1)).onClick(event);
    }

    @Test
    public void testCheckBox() {
        final PCheckBox checkBox = Element.newPCheckBox();
        Assert.assertEquals(PCheckBoxState.UNCHECKED, checkBox.getState());
        checkBox.setState(PCheckBoxState.CHECKED);
        Assert.assertEquals(PCheckBoxState.CHECKED, checkBox.getState());
    }

    @Test
    public void testTextBox() {
        final PTextBox textBox = Element.newPTextBox("test");
        Assert.assertEquals("test", textBox.getText());
        final PValueChangeHandler<String> handler = Mockito.mock(PValueChangeHandler.class);
        textBox.addValueChangeHandler(handler);
        final PValueChangeEvent<String> event = new PValueChangeEvent<>(this, "test2");
        textBox.fireOnValueChange(event);
        Mockito.verify(handler, Mockito.times(1)).onValueChange(event);
        Assert.assertEquals("test2", textBox.getText());

        Mockito.reset(handler);
        textBox.fireEvent(new PKeyUpEvent(this, PKeyCodes.DOWN.getCode()));
        Mockito.verify(handler, Mockito.times(0)).onValueChange(event);
    }

}
