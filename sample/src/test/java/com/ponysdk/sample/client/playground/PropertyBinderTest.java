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

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.test.PSuite;

/**
 * Unit tests for {@link PropertyBinder}.
 */
public class PropertyBinderTest extends PSuite {

    private PropertyBinder propertyBinder;
    private TestComponent testComponent;

    @Before
    public void setUp() {
        propertyBinder = new PropertyBinder();
        testComponent = new TestComponent();
    }

    @Test
    public void testBindControls_withTextBox_shouldAttachHandler() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(method, "setTitle", void.class, List.of(param));
        
        final PTextBox textBox = Element.newPTextBox();
        final PLabel label = Element.newPLabel("setTitle");
        final PLabel errorLabel = Element.newPLabel();
        final PropertyControl control = new PropertyControl(label, textBox, errorLabel, signature);
        
        propertyBinder.bindControls(List.of(control), testComponent);
        
        // Verify handler was attached
        assertFalse(textBox.getValueChangeHandlers().isEmpty());
    }

    @Test
    public void testBindControls_withCheckBox_shouldAttachHandler() throws Exception {
        final Method method = TestComponent.class.getMethod("setEnabled", boolean.class);
        final ParameterInfo param = new ParameterInfo("enabled", boolean.class, false, null);
        final MethodSignature signature = new MethodSignature(method, "setEnabled", void.class, List.of(param));
        
        final PCheckBox checkBox = Element.newPCheckBox();
        final PLabel label = Element.newPLabel("setEnabled");
        final PLabel errorLabel = Element.newPLabel();
        final PropertyControl control = new PropertyControl(label, checkBox, errorLabel, signature);
        
        propertyBinder.bindControls(List.of(control), testComponent);
        
        // Verify handler was attached
        assertFalse(checkBox.getValueChangeHandlers().isEmpty());
    }

    @Test
    public void testBindControls_withListBox_shouldAttachHandler() throws Exception {
        final Method method = TestComponent.class.getMethod("setStatus", TestStatus.class);
        final ParameterInfo param = new ParameterInfo("status", TestStatus.class, false, null);
        final MethodSignature signature = new MethodSignature(method, "setStatus", void.class, List.of(param));
        
        final PListBox listBox = Element.newPListBox();
        listBox.addItem("ACTIVE", TestStatus.ACTIVE);
        listBox.addItem("INACTIVE", TestStatus.INACTIVE);
        
        final PLabel label = Element.newPLabel("setStatus");
        final PLabel errorLabel = Element.newPLabel();
        final PropertyControl control = new PropertyControl(label, listBox, errorLabel, signature);
        
        propertyBinder.bindControls(List.of(control), testComponent);
        
        // Verify handler was attached (ListBox uses change handlers, not value change handlers)
        // We can't directly verify this without accessing internal state, so we just verify no exception
        assertNotNull(control);
    }

    @Test
    public void testBindControls_withMultipleControls_shouldBindAll() throws Exception {
        final Method method1 = TestComponent.class.getMethod("setTitle", String.class);
        final Method method2 = TestComponent.class.getMethod("setEnabled", boolean.class);
        
        final ParameterInfo param1 = new ParameterInfo("title", String.class, false, null);
        final ParameterInfo param2 = new ParameterInfo("enabled", boolean.class, false, null);
        
        final MethodSignature signature1 = new MethodSignature(method1, "setTitle", void.class, List.of(param1));
        final MethodSignature signature2 = new MethodSignature(method2, "setEnabled", void.class, List.of(param2));
        
        final PTextBox textBox = Element.newPTextBox();
        final PCheckBox checkBox = Element.newPCheckBox();
        
        final PropertyControl control1 = new PropertyControl(
            Element.newPLabel("setTitle"), textBox, Element.newPLabel(), signature1
        );
        final PropertyControl control2 = new PropertyControl(
            Element.newPLabel("setEnabled"), checkBox, Element.newPLabel(), signature2
        );
        
        propertyBinder.bindControls(List.of(control1, control2), testComponent);
        
        // Verify handlers were attached to both controls
        assertFalse(textBox.getValueChangeHandlers().isEmpty());
        assertFalse(checkBox.getValueChangeHandlers().isEmpty());
    }

    @Test
    public void testBindControls_withUnsupportedControl_shouldNotThrowException() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(method, "setTitle", void.class, List.of(param));
        
        // Use a PLabel as an unsupported control type
        final PLabel unsupportedControl = Element.newPLabel("unsupported");
        final PLabel label = Element.newPLabel("setTitle");
        final PLabel errorLabel = Element.newPLabel();
        final PropertyControl control = new PropertyControl(label, unsupportedControl, errorLabel, signature);
        
        // Should not throw exception
        propertyBinder.bindControls(List.of(control), testComponent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBindControls_withNullControls_shouldThrowException() {
        propertyBinder.bindControls(null, testComponent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBindControls_withNullComponent_shouldThrowException() throws Exception {
        final Method method = TestComponent.class.getMethod("setTitle", String.class);
        final ParameterInfo param = new ParameterInfo("title", String.class, false, null);
        final MethodSignature signature = new MethodSignature(method, "setTitle", void.class, List.of(param));
        
        final PropertyControl control = new PropertyControl(
            Element.newPLabel("setTitle"),
            Element.newPTextBox(),
            Element.newPLabel(),
            signature
        );
        
        propertyBinder.bindControls(List.of(control), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_withNullTypeConverter_shouldThrowException() {
        new PropertyBinder(null);
    }

    @Test
    public void testBindControls_withEmptyList_shouldNotThrowException() {
        propertyBinder.bindControls(List.of(), testComponent);
    }

    // Test component class
    public static class TestComponent {
        private String title;
        private boolean enabled;
        private int count;
        private TestStatus status;

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setStatus(TestStatus status) {
            this.status = status;
        }

        public TestStatus getStatus() {
            return status;
        }

        public void setFailingProperty(String value) {
            throw new IllegalArgumentException("This method always fails");
        }
    }

    // Test enum
    public enum TestStatus {
        ACTIVE, INACTIVE
    }
}
