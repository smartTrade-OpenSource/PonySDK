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
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the creation of form controls for component properties.
 * <p>
 * The FormGenerator takes a list of method signatures and generates complete
 * PropertyControl instances for each method, including labels, input controls,
 * and error display labels. It applies consistent styling and spacing to create
 * a uniform form layout.
 * </p>
 * <p>
 * Each generated PropertyControl includes:
 * <ul>
 *   <li>A label displaying the method name</li>
 *   <li>An appropriate input control based on parameter type</li>
 *   <li>An error label (initially hidden) for displaying validation/invocation errors</li>
 * </ul>
 * </p>
 */
public class FormGenerator {

    private final ControlFactory controlFactory;

    /**
     * Creates a new FormGenerator with a default ControlFactory.
     */
    public FormGenerator() {
        this(new DefaultControlFactory());
    }

    /**
     * Creates a new FormGenerator with the specified ControlFactory.
     *
     * @param controlFactory the control factory to use for creating input controls, must not be null
     * @throws IllegalArgumentException if controlFactory is null
     */
    public FormGenerator(final ControlFactory controlFactory) {
        if (controlFactory == null) {
            throw new IllegalArgumentException("controlFactory must not be null");
        }
        this.controlFactory = controlFactory;
    }

    /**
     * Generates PropertyControl instances for a list of method signatures.
     * <p>
     * For each method signature, this creates:
     * <ul>
     *   <li>A label showing the method name</li>
     *   <li>An appropriate control based on the first parameter type</li>
     *   <li>An error label (initially hidden) for displaying errors</li>
     * </ul>
     * </p>
     * <p>
     * The method applies consistent 10px vertical spacing between controls.
     * Unsupported types are filtered out to keep the UI clean.
     * </p>
     *
     * @param methodSignatures the list of method signatures to generate controls for, must not be null
     * @return a list of PropertyControl instances, one for each method signature
     * @throws IllegalArgumentException if methodSignatures is null
     */
    public List<PropertyControl> generateControls(final List<MethodSignature> methodSignatures) {
        if (methodSignatures == null) {
            throw new IllegalArgumentException("methodSignatures must not be null");
        }

        final List<PropertyControl> controls = new ArrayList<>();

        for (final MethodSignature methodSignature : methodSignatures) {
            // Skip methods with unsupported parameter types
            if (isUnsupportedType(methodSignature)) {
                continue;
            }
            
            final PropertyControl control = createPropertyControl(methodSignature);
            controls.add(control);
        }

        return controls;
    }
    
    /**
     * Checks if a method signature has unsupported parameter types.
     * 
     * @param methodSignature the method signature to check
     * @return true if the parameter type is unsupported, false otherwise
     */
    private boolean isUnsupportedType(final MethodSignature methodSignature) {
        final List<ParameterInfo> parameters = methodSignature.parameters();
        
        if (parameters.isEmpty()) {
            return false;
        }
        
        final ParameterInfo firstParameter = parameters.get(0);
        final Class<?> type = firstParameter.type();
        
        // Check if it's a supported type
        if (type == String.class || 
            type == boolean.class || 
            type == Boolean.class ||
            type == int.class || 
            type == Integer.class ||
            type == long.class || 
            type == Long.class ||
            type.isEnum()) {
            return false;
        }
        
        // Everything else is unsupported
        return true;
    }

    /**
     * Creates a single PropertyControl for a method signature.
     *
     * @param methodSignature the method signature to create a control for
     * @return a PropertyControl instance
     */
    private PropertyControl createPropertyControl(final MethodSignature methodSignature) {
        // Create label showing method name
        final PLabel label = createMethodLabel(methodSignature.methodName());

        // Create the input control based on the first parameter type
        final PWidget control = createInputControl(methodSignature);

        // Create error label (initially hidden)
        final PLabel errorLabel = createErrorLabel();

        return new PropertyControl(label, control, errorLabel, methodSignature);
    }

    /**
     * Creates a label displaying the method name.
     *
     * @param methodName the method name to display
     * @return a PLabel instance with the method name
     */
    private PLabel createMethodLabel(final String methodName) {
        final PLabel label = Element.newPLabel(methodName);
        // Apply consistent styling
        label.addStyleName("property-label");
        return label;
    }

    /**
     * Creates an input control for the method's first parameter.
     * <p>
     * If the method has no parameters, returns a label indicating no parameters.
     * </p>
     *
     * @param methodSignature the method signature
     * @return a PWidget control appropriate for the parameter type
     */
    private PWidget createInputControl(final MethodSignature methodSignature) {
        final List<ParameterInfo> parameters = methodSignature.parameters();

        if (parameters.isEmpty()) {
            // No parameters - create a label indicating this
            final PLabel label = Element.newPLabel("(no parameters)");
            label.addStyleName("no-params-label");
            return label;
        }

        // Get the first parameter (we only support single-parameter setters for now)
        final ParameterInfo firstParameter = parameters.get(0);

        // Use the control factory to create the appropriate control
        final PWidget control = controlFactory.createControl(firstParameter);
        control.addStyleName("property-control");

        return control;
    }

    /**
     * Creates an error label for displaying validation and invocation errors.
     * <p>
     * The error label is initially hidden and styled with red text and a warning icon.
     * </p>
     *
     * @return a PLabel instance configured for error display
     */
    private PLabel createErrorLabel() {
        final PLabel errorLabel = Element.newPLabel();
        
        // Apply error styling
        errorLabel.addStyleName("error-label");
        
        // Initially hidden
        errorLabel.setVisible(false);
        
        return errorLabel;
    }
}
