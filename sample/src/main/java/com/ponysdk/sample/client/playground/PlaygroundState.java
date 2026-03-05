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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.ui.component.PWebComponent;

/**
 * Current state of the playground.
 * <p>
 * Maintains the ephemeral state including the selected component, its instance,
 * generated controls, and current form values. This state is not persisted.
 * </p>
 */
public class PlaygroundState {

    private String selectedComponentName;
    private PWebComponent<?> componentInstance;
    private List<PropertyControl> controls = new ArrayList<>();
    private Map<String, String> currentValues = new HashMap<>();

    /**
     * Gets the selected component name.
     *
     * @return the component name, or null if none selected
     */
    public String getSelectedComponentName() {
        return selectedComponentName;
    }

    /**
     * Sets the selected component name.
     *
     * @param selectedComponentName the component name, may be null
     */
    public void setSelectedComponentName(String selectedComponentName) {
        this.selectedComponentName = selectedComponentName;
    }

    /**
     * Gets the current component instance.
     *
     * @return the component instance, or null if none created
     */
    public PWebComponent<?> getComponentInstance() {
        return componentInstance;
    }

    /**
     * Sets the current component instance.
     *
     * @param componentInstance the component instance, may be null
     */
    public void setComponentInstance(PWebComponent<?> componentInstance) {
        this.componentInstance = componentInstance;
    }

    /**
     * Gets the list of property controls.
     *
     * @return the list of controls, never null
     */
    public List<PropertyControl> getControls() {
        return controls;
    }

    /**
     * Sets the list of property controls.
     *
     * @param controls the list of controls, must not be null
     */
    public void setControls(List<PropertyControl> controls) {
        if (controls == null) throw new IllegalArgumentException("controls must not be null");
        this.controls = controls;
    }

    /**
     * Gets the current form values.
     *
     * @return the map of method names to values, never null
     */
    public Map<String, String> getCurrentValues() {
        return currentValues;
    }

    /**
     * Sets a current form value.
     *
     * @param methodName the method name, must not be null
     * @param value      the value, may be null
     */
    public void setCurrentValue(String methodName, String value) {
        if (methodName == null) throw new IllegalArgumentException("methodName must not be null");
        currentValues.put(methodName, value);
    }

    /**
     * Clears all state.
     */
    public void clear() {
        selectedComponentName = null;
        componentInstance = null;
        controls.clear();
        currentValues.clear();
    }
}
