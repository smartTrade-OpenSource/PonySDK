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

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PRadioButtonSelection;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;

/**
 * Default implementation of ControlFactory that creates appropriate PonySDK controls
 * based on parameter types.
 * <p>
 * This factory creates:
 * <ul>
 *   <li>PTextBox for String parameters</li>
 *   <li>PCheckBox for boolean parameters</li>
 *   <li>PTextBox for int/long parameters (numeric)</li>
 *   <li>PListBox for enum parameters (populated with enum values)</li>
 *   <li>PCheckBox + control for Optional parameters</li>
 *   <li>PLabel for unsupported types</li>
 * </ul>
 * </p>
 */
public class DefaultControlFactory implements ControlFactory {

    private final ParameterAnalyzer parameterAnalyzer;

    /**
     * Creates a new DefaultControlFactory with a default ParameterAnalyzer.
     */
    public DefaultControlFactory() {
        this(new ParameterAnalyzer());
    }

    /**
     * Creates a new DefaultControlFactory with the specified ParameterAnalyzer.
     *
     * @param parameterAnalyzer the parameter analyzer to use, must not be null
     * @throws IllegalArgumentException if parameterAnalyzer is null
     */
    public DefaultControlFactory(final ParameterAnalyzer parameterAnalyzer) {
        if (parameterAnalyzer == null) {
            throw new IllegalArgumentException("parameterAnalyzer must not be null");
        }
        this.parameterAnalyzer = parameterAnalyzer;
    }

    @Override
    public PWidget createControl(final ParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            throw new IllegalArgumentException("parameterInfo must not be null");
        }

        final ParameterAnalyzer.ControlType controlType = parameterAnalyzer.determineControlType(parameterInfo);

        switch (controlType) {
            case TEXT_BOX:
                return createTextBox();
            
            case CHECK_BOX:
                return createCheckBox();
            
            case NUMERIC_TEXT_BOX:
                return createNumericTextBox();
            
            case LIST_BOX:
                return createEnumListBox(parameterInfo);
            
            case OPTIONAL_CONTROL:
                return createOptionalControl(parameterInfo);
            
            case UNSUPPORTED:
            default:
                return createUnsupportedLabel(parameterInfo);
        }
    }

    /**
     * Creates a PTextBox for String parameters.
     *
     * @return a new PTextBox instance
     */
    private PTextBox createTextBox() {
        return Element.newPTextBox();
    }

    /**
     * Creates a PCheckBox for boolean parameters.
     *
     * @return a new PCheckBox instance
     */
    private PCheckBox createCheckBox() {
        return Element.newPCheckBox();
    }

    /**
     * Creates a PTextBox for numeric (int/long) parameters.
     *
     * @return a new PTextBox instance configured for numeric input
     */
    private PTextBox createNumericTextBox() {
        final PTextBox textBox = Element.newPTextBox();
        // Apply numeric filter to allow digits, minus sign, and decimal point
        textBox.setFilter("[-0-9.]");
        return textBox;
    }

    /**
     * Creates a radio button group for enum parameters using PRadioButtonSelection.
     *
     * @param parameterInfo the parameter information containing the enum type
     * @return a PFlowPanel containing radio buttons for each enum value, with PRadioButtonSelection attached
     */
    private PWidget createEnumListBox(final ParameterInfo parameterInfo) {
        final Class<?> enumType = parameterInfo.type();
        
        if (!enumType.isEnum()) {
            return createUnsupportedLabel(parameterInfo);
        }
        
        final PFlowPanel radioGroupPanel = Element.newPFlowPanel();
        radioGroupPanel.addStyleName("enum-radio-group");
        
        final Object[] enumConstants = enumType.getEnumConstants();
        final List<PRadioButton> radioButtons = new ArrayList<>();
        
        for (final Object enumConstant : enumConstants) {
            final PRadioButton radioButton = Element.newPRadioButton();
            // Use getValue() if available (for generated enums), otherwise toString()
            final String displayValue = getEnumDisplayValue(enumConstant);
            radioButton.setText(displayValue);
            radioButton.setData(enumConstant);
            radioButton.addStyleName("enum-radio-option");
            radioButtons.add(radioButton);
            radioGroupPanel.add(radioButton);
        }
        
        // Create PRadioButtonSelection to manage the group - it handles exclusive selection
        final PRadioButtonSelection selection = Element.newPRadioButtonSelection(radioButtons);
        // Store the selection object on the panel for later access in PropertyBinder
        radioGroupPanel.setData(selection);
        
        return radioGroupPanel;
    }

    /**
     * Creates a control for Optional parameters.
     * Uses the wrapped type to determine the appropriate control:
     * String → PTextBox, boolean → PCheckBox, numeric → numeric PTextBox, enum → radio group.
     * An empty value means Optional.empty().
     *
     * @param parameterInfo the parameter information containing the Optional type
     * @return a control appropriate for the wrapped type
     */
    private PWidget createOptionalControl(final ParameterInfo parameterInfo) {
        final Class<?> wrappedType = parameterInfo.optionalWrappedType();
        if (wrappedType == null || Object.class.equals(wrappedType)) {
            return createTextBox(); // fallback to text box
        }

        // Create a control based on the wrapped type
        final ParameterInfo unwrapped = new ParameterInfo(
            parameterInfo.name(), wrappedType, false, null
        );
        final ParameterAnalyzer.ControlType controlType = parameterAnalyzer.determineControlType(unwrapped);

        switch (controlType) {
            case TEXT_BOX:
                return createTextBox();
            case CHECK_BOX:
                return createCheckBox();
            case NUMERIC_TEXT_BOX:
                return createNumericTextBox();
            case LIST_BOX:
                return createEnumListBox(unwrapped);
            default:
                return createTextBox();
        }
    }

    /**
     * Creates a PLabel indicating that the parameter type is not supported.
     *
     * @param parameterInfo the parameter information containing the unsupported type
     * @return a new PLabel instance with an unsupported type message
     */
    private PLabel createUnsupportedLabel(final ParameterInfo parameterInfo) {
        final String typeName = parameterInfo.type().getSimpleName();
        final PLabel label = Element.newPLabel("Type not supported: " + typeName);
        return label;
    }

    /**
     * Gets the display value for an enum constant.
     * If the enum has a getValue() method (generated enums), uses that.
     * Otherwise falls back to toString().
     *
     * @param enumConstant the enum constant
     * @return the display value (lowercase for generated enums)
     */
    private String getEnumDisplayValue(final Object enumConstant) {
        try {
            final java.lang.reflect.Method getValueMethod = enumConstant.getClass().getMethod("getValue");
            return (String) getValueMethod.invoke(enumConstant);
        } catch (final Exception e) {
            // Fallback to toString() for standard enums
            return enumConstant.toString();
        }
    }
}
