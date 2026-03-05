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

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PRadioButton;
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
        // Apply numeric filter to only allow digits and optional minus sign
        textBox.setFilter("[-0-9]");
        return textBox;
    }

    /**
     * Creates a radio button group for enum parameters.
     *
     * @param parameterInfo the parameter information containing the enum type
     * @return a PFlowPanel containing radio buttons for each enum value
     */
    private PWidget createEnumListBox(final ParameterInfo parameterInfo) {
        final Class<?> enumType = parameterInfo.type();
        
        if (!enumType.isEnum()) {
            return createUnsupportedLabel(parameterInfo);
        }
        
        final PFlowPanel radioGroup = Element.newPFlowPanel();
        radioGroup.addStyleName("enum-radio-group");
        
        final Object[] enumConstants = enumType.getEnumConstants();
        final String groupName = "enum_" + System.identityHashCode(parameterInfo);
        
        for (int i = 0; i < enumConstants.length; i++) {
            final Object enumConstant = enumConstants[i];
            final PRadioButton radioButton = Element.newPRadioButton(groupName);
            radioButton.setText(enumConstant.toString());
            radioButton.setData(enumConstant);
            radioButton.addStyleName("enum-radio-option");
            
            // Select first option by default
            if (i == 0) {
                radioButton.setValue(true);
            }
            
            radioGroup.add(radioButton);
        }
        
        return radioGroup;
    }

    /**
     * Creates a control for Optional parameters.
     * This creates a PCheckBox to enable/disable the optional value,
     * combined with an appropriate control for the wrapped type.
     * <p>
     * Note: For now, this returns just a PCheckBox. The full implementation
     * with combined controls will be added in the FormGenerator.
     * </p>
     *
     * @param parameterInfo the parameter information containing the Optional type
     * @return a PCheckBox for enabling/disabling the optional value
     */
    private PWidget createOptionalControl(final ParameterInfo parameterInfo) {
        // For Optional parameters, we return a checkbox
        // The FormGenerator will handle creating the combined control structure
        return Element.newPCheckBox();
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
}
