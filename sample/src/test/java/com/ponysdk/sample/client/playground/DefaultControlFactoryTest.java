/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.sample.client.playground;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.test.PSuite;

/**
 * Unit tests for {@link DefaultControlFactory}.
 */
public class DefaultControlFactoryTest extends PSuite {

    private DefaultControlFactory factory;

    @Before
    public void setUp() {
        factory = new DefaultControlFactory();
    }

    // Test enum for testing
    enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }

    @Test
    public void testCreateControl_forString_shouldReturnPTextBox() {
        final ParameterInfo param = new ParameterInfo("test", String.class, false, null);
        
        final PWidget control = factory.createControl(param);
        
        assertNotNull(control);
        assertTrue(control instanceof PTextBox);
    }

    @Test
    public void testCreateControl_forBoolean_shouldReturnPCheckBox() {
        final ParameterInfo param = new ParameterInfo("test", boolean.class, false, null);
        
        final PWidget control = factory.createControl(param);
        
        assertNotNull(control);
        assertTrue(control instanceof PCheckBox);
    }

    @Test
    public void testCreateControl_forInt_shouldReturnPTextBox() {
        final ParameterInfo param = new ParameterInfo("test", int.class, false, null);
        
        final PWidget control = factory.createControl(param);
        
        assertNotNull(control);
        assertTrue(control instanceof PTextBox);
    }

    @Test
    public void testCreateControl_forLong_shouldReturnPTextBox() {
        final ParameterInfo param = new ParameterInfo("test", long.class, false, null);
        
        final PWidget control = factory.createControl(param);
        
        assertNotNull(control);
        assertTrue(control instanceof PTextBox);
    }

    @Test
    public void testCreateControl_forEnum_shouldReturnPListBox() {
        final ParameterInfo param = new ParameterInfo("test", TestEnum.class, false, null);
        
        final PWidget control = factory.createControl(param);
        
        assertNotNull(control);
        assertTrue(control instanceof PListBox);
    }

    @Test
    public void testCreateControl_forEnum_shouldPopulateAllEnumValues() {
        final ParameterInfo param = new ParameterInfo("test", TestEnum.class, false, null);
        
        final PWidget control = factory.createControl(param);
        final PListBox listBox = (PListBox) control;
        
        assertEquals(3, listBox.getItemCount());
        assertEquals("VALUE1", listBox.getItem(0));
        assertEquals("VALUE2", listBox.getItem(1));
        assertEquals("VALUE3", listBox.getItem(2));
    }

    @Test
    public void testCreateControl_forOptional_shouldReturnPCheckBox() {
        final ParameterInfo param = new ParameterInfo("test", Optional.class, true, String.class);
        
        final PWidget control = factory.createControl(param);
        
        assertNotNull(control);
        assertTrue(control instanceof PCheckBox);
    }

    @Test
    public void testCreateControl_forUnsupportedType_shouldReturnPLabel() {
        final ParameterInfo param = new ParameterInfo("test", Object.class, false, null);
        
        final PWidget control = factory.createControl(param);
        
        assertNotNull(control);
        assertTrue(control instanceof PLabel);
    }

    @Test
    public void testCreateControl_forUnsupportedType_shouldShowTypeName() {
        final ParameterInfo param = new ParameterInfo("test", Object.class, false, null);
        
        final PWidget control = factory.createControl(param);
        final PLabel label = (PLabel) control;
        
        assertEquals("Type not supported: Object", label.getText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateControl_withNullParameter_shouldThrowException() {
        factory.createControl(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_withNullAnalyzer_shouldThrowException() {
        new DefaultControlFactory(null);
    }
}
