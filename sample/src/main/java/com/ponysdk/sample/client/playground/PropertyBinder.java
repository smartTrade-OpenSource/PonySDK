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

import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Binds form controls to component methods for property manipulation.
 * <p>
 * The PropertyBinder attaches event handlers to form controls, captures value changes,
 * coordinates type conversion, and invokes setter methods on the component instance.
 * It handles errors gracefully and displays error messages in the appropriate locations.
 * </p>
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Attaching value change handlers to form controls</li>
 *   <li>Extracting values from different control types</li>
 *   <li>Converting string values to appropriate parameter types</li>
 *   <li>Invoking setter methods via reflection</li>
 *   <li>Displaying error messages when conversion or invocation fails</li>
 * </ul>
 * </p>
 */
public class PropertyBinder {

    private static final Logger LOGGER = Logger.getLogger(PropertyBinder.class.getName());

    private final TypeConverter typeConverter;

    /**
     * Creates a new PropertyBinder with a default TypeConverter.
     */
    public PropertyBinder() {
        this(new DefaultTypeConverter());
    }

    /**
     * Creates a new PropertyBinder with the specified TypeConverter.
     *
     * @param typeConverter the type converter to use, must not be null
     * @throws IllegalArgumentException if typeConverter is null
     */
    public PropertyBinder(final TypeConverter typeConverter) {
        if (typeConverter == null) {
            throw new IllegalArgumentException("typeConverter must not be null");
        }
        this.typeConverter = typeConverter;
    }

    /**
     * Binds a list of property controls to a component instance.
     * <p>
     * For each property control, this method attaches an event handler that:
     * <ol>
     *   <li>Captures the value change</li>
     *   <li>Converts the value to the appropriate type</li>
     *   <li>Invokes the setter method on the component</li>
     *   <li>Displays any errors that occur</li>
     * </ol>
     * </p>
     *
     * @param propertyControls the list of property controls to bind, must not be null
     * @param componentInstance the component instance to bind to, must not be null
     * @throws IllegalArgumentException if propertyControls or componentInstance is null
     */
    public void bindControls(final List<PropertyControl> propertyControls, final Object componentInstance) {
        if (propertyControls == null) {
            throw new IllegalArgumentException("propertyControls must not be null");
        }
        if (componentInstance == null) {
            throw new IllegalArgumentException("componentInstance must not be null");
        }

        for (final PropertyControl propertyControl : propertyControls) {
            bindControl(propertyControl, componentInstance);
        }
    }

    /**
     * Binds a single property control to a component instance.
     *
     * @param propertyControl the property control to bind
     * @param componentInstance the component instance to bind to
     */
    private void bindControl(final PropertyControl propertyControl, final Object componentInstance) {
        final PWidget control = propertyControl.control();
        final MethodSignature methodSignature = propertyControl.method();

        // Attach appropriate event handler based on control type
        if (control instanceof PTextBox) {
            bindTextBox((PTextBox) control, propertyControl, componentInstance);
        } else if (control instanceof PCheckBox) {
            bindCheckBox((PCheckBox) control, propertyControl, componentInstance);
        } else if (control instanceof PListBox) {
            bindListBox((PListBox) control, propertyControl, componentInstance);
        } else if (control instanceof PFlowPanel) {
            // Radio button group for enums
            bindRadioGroup((PFlowPanel) control, propertyControl, componentInstance);
        } else {
            // Unsupported control type - log warning
            LOGGER.log(Level.WARNING, "Unsupported control type for method: " + methodSignature.methodName());
        }
    }

    /**
     * Binds a PTextBox control to a component method.
     *
     * @param textBox the text box control
     * @param propertyControl the property control containing metadata
     * @param componentInstance the component instance
     */
    private void bindTextBox(final PTextBox textBox, final PropertyControl propertyControl, final Object componentInstance) {
        textBox.addValueChangeHandler(event -> {
            final String value = event.getData();
            handleValueChange(value, propertyControl, componentInstance);
        });
    }

    /**
     * Binds a PCheckBox control to a component method.
     *
     * @param checkBox the checkbox control
     * @param propertyControl the property control containing metadata
     * @param componentInstance the component instance
     */
    private void bindCheckBox(final PCheckBox checkBox, final PropertyControl propertyControl, final Object componentInstance) {
        checkBox.addValueChangeHandler(event -> {
            final Boolean value = event.getData();
            handleValueChange(value != null ? value.toString() : "false", propertyControl, componentInstance);
        });
    }

    /**
     * Binds a PListBox control to a component method.
     *
     * @param listBox the list box control
     * @param propertyControl the property control containing metadata
     * @param componentInstance the component instance
     */
    private void bindListBox(final PListBox listBox, final PropertyControl propertyControl, final Object componentInstance) {
        listBox.addChangeHandler(event -> {
            final int selectedIndex = listBox.getSelectedIndex();
            if (selectedIndex >= 0) {
                final String value = listBox.getItem(selectedIndex);
                handleValueChange(value, propertyControl, componentInstance);
            }
        });
    }

    /**
     * Binds a radio button group (PFlowPanel containing PRadioButtons) to a component method.
     *
     * @param radioGroup the flow panel containing radio buttons
     * @param propertyControl the property control containing metadata
     * @param componentInstance the component instance
     */
    private void bindRadioGroup(final PFlowPanel radioGroup, final PropertyControl propertyControl, final Object componentInstance) {
        // Iterate through all radio buttons in the group
        for (int i = 0; i < radioGroup.getWidgetCount(); i++) {
            final PWidget widget = radioGroup.getWidget(i);
            if (widget instanceof PRadioButton) {
                final PRadioButton radioButton = (PRadioButton) widget;
                radioButton.addValueChangeHandler(event -> {
                    if (Boolean.TRUE.equals(event.getData())) {
                        final String value = radioButton.getText();
                        handleValueChange(value, propertyControl, componentInstance);
                    }
                });
            }
        }
    }

    /**
     * Handles a value change from a control.
     * <p>
     * This method:
     * <ol>
     *   <li>Clears any previous error message</li>
     *   <li>Converts the value to the appropriate type</li>
     *   <li>Invokes the setter method on the component</li>
     *   <li>Displays any errors that occur</li>
     * </ol>
     * </p>
     *
     * @param value the new value as a string
     * @param propertyControl the property control containing metadata
     * @param componentInstance the component instance
     */
    private void handleValueChange(final String value, final PropertyControl propertyControl, final Object componentInstance) {
        // Clear previous error
        clearError(propertyControl);

        try {
            // Convert value to appropriate type and invoke method
            invokeMethod(value, propertyControl.method(), componentInstance);
        } catch (final ConversionException e) {
            // Display conversion error
            displayError(propertyControl, e.getMessage());
            LOGGER.log(Level.WARNING, "Type conversion failed for method: " + propertyControl.method().methodName(), e);
        } catch (final Exception e) {
            // Display invocation error
            displayError(propertyControl, "Error setting " + propertyControl.method().methodName() + ": " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Method invocation failed for method: " + propertyControl.method().methodName(), e);
        }
    }

    /**
     * Invokes a setter method on the component instance with the converted value.
     *
     * @param value the value as a string
     * @param methodSignature the method signature
     * @param componentInstance the component instance
     * @throws ConversionException if type conversion fails
     * @throws Exception if method invocation fails
     */
    private void invokeMethod(final String value, final MethodSignature methodSignature, final Object componentInstance) throws Exception {
        final List<ParameterInfo> parameters = methodSignature.parameters();
        
        if (parameters.isEmpty()) {
            // No parameters - invoke method directly
            methodSignature.method().invoke(componentInstance);
            return;
        }

        // Get the first parameter (we only support single-parameter setters)
        final ParameterInfo parameterInfo = parameters.get(0);
        final Class<?> parameterType = parameterInfo.type();

        // Convert the value to the appropriate type
        final Object convertedValue = typeConverter.convert(value, parameterType);

        // Invoke the method
        try {
            methodSignature.method().invoke(componentInstance, convertedValue);
        } catch (final InvocationTargetException e) {
            // Unwrap the target exception for clearer error messages
            final Throwable cause = e.getCause();
            if (cause != null) {
                throw new Exception(cause.getMessage(), cause);
            }
            throw e;
        }
    }

    /**
     * Displays an error message in the property control's error label.
     *
     * @param propertyControl the property control
     * @param errorMessage the error message to display
     */
    private void displayError(final PropertyControl propertyControl, final String errorMessage) {
        propertyControl.errorLabel().setText("⚠ " + errorMessage);
        propertyControl.errorLabel().setVisible(true);
    }

    /**
     * Clears the error message from the property control's error label.
     *
     * @param propertyControl the property control
     */
    private void clearError(final PropertyControl propertyControl) {
        propertyControl.errorLabel().setText("");
        propertyControl.errorLabel().setVisible(false);
    }
}
