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

import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.component.PWebComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main playground class for testing Web Awesome components.
 * <p>
 * The ComponentPlayground provides an interactive interface for exploring and testing
 * Web Awesome components. It uses Java reflection to automatically discover components
 * and their setter methods, then dynamically generates form controls for property
 * manipulation with real-time visual feedback.
 * </p>
 * <p>
 * The playground consists of:
 * <ul>
 *   <li>Left panel (25%): Component selection list</li>
 *   <li>Right panel (75%): Component preview and property controls</li>
 * </ul>
 * </p>
 * <p>
 * Requirements: 1.1, 2.1, 2.2
 * </p>
 */
public class ComponentPlayground extends PSimplePanel {

    private static final Logger LOGGER = Logger.getLogger(ComponentPlayground.class.getName());

    // Subsystems
    private final ComponentScanner scanner;
    private final MethodIntrospector introspector;
    private final FormGenerator formGenerator;
    private final PropertyBinder binder;
    private final ComponentRegistry registry;
    private final ComponentMetadataLoader metadataLoader;

    // UI Components
    private final ComponentListPanel componentListPanel;
    private final PropertyPanel propertyPanel;
    private final PlaygroundLayout layout;

    // Current state
    private PWebComponent<?> currentComponent;

    /**
     * Creates a new ComponentPlayground with default subsystems.
     */
    public ComponentPlayground() {
        this(new DefaultComponentScanner(),
             new DefaultMethodIntrospector(),
             new FormGenerator(),
             new PropertyBinder(),
             new ComponentMetadataLoader());
    }

    /**
     * Creates a new ComponentPlayground with specified subsystems.
     * <p>
     * This constructor allows dependency injection for testing purposes.
     * </p>
     *
     * @param scanner        the component scanner, must not be null
     * @param introspector   the method introspector, must not be null
     * @param formGenerator  the form generator, must not be null
     * @param binder         the property binder, must not be null
     * @param metadataLoader the component metadata loader, must not be null
     * @throws IllegalArgumentException if any parameter is null
     */
    public ComponentPlayground(final ComponentScanner scanner,
                               final MethodIntrospector introspector,
                               final FormGenerator formGenerator,
                               final PropertyBinder binder,
                               final ComponentMetadataLoader metadataLoader) {
        if (scanner == null) {
            throw new IllegalArgumentException("scanner must not be null");
        }
        if (introspector == null) {
            throw new IllegalArgumentException("introspector must not be null");
        }
        if (formGenerator == null) {
            throw new IllegalArgumentException("formGenerator must not be null");
        }
        if (binder == null) {
            throw new IllegalArgumentException("binder must not be null");
        }
        if (metadataLoader == null) {
            throw new IllegalArgumentException("metadataLoader must not be null");
        }

        this.scanner = scanner;
        this.introspector = introspector;
        this.formGenerator = formGenerator;
        this.binder = binder;
        this.metadataLoader = metadataLoader;
        this.registry = new ComponentRegistry();

        // Create UI components
        this.componentListPanel = new ComponentListPanel();
        this.propertyPanel = new PropertyPanel();
        this.layout = new PlaygroundLayout(componentListPanel, propertyPanel);

        // Initialize the playground
        initialize();
    }

    /**
     * Initializes the playground by discovering components and wiring event handlers.
     */
    private void initialize() {
        // Set the layout as the main widget
        setWidget(layout);

        // Load component metadata
        metadataLoader.load();

        // Discover and register components
        discoverComponents();

        // Wire component selection event handler
        componentListPanel.setSelectionHandler(this::onComponentSelected);

        LOGGER.log(Level.INFO, "ComponentPlayground initialized successfully");
    }

    /**
     * Discovers Web Awesome components and populates the component list.
     * <p>
     * This method:
     * <ol>
     *   <li>Scans for component classes using the scanner</li>
     *   <li>Extracts component names and registers them</li>
     *   <li>Populates the component list panel</li>
     *   <li>Handles discovery errors gracefully</li>
     * </ol>
     * </p>
     */
    private void discoverComponents() {
        try {
            // Scan for component classes
            final List<Class<? extends PWebComponent<?>>> componentClasses = scanner.scanComponents();

            if (componentClasses.isEmpty()) {
                componentListPanel.showError("No components found in package");
                LOGGER.log(Level.WARNING, "No Web Awesome components discovered");
                return;
            }

            // Register each component
            for (final Class<? extends PWebComponent<?>> componentClass : componentClasses) {
                final String componentName = DefaultComponentScanner.extractComponentName(componentClass);
                registry.register(componentName, componentClass);
            }

            // Populate the component list
            final List<String> componentNames = registry.getComponentNames();
            componentListPanel.setComponentNames(componentNames);

            LOGGER.log(Level.INFO, "Discovered {0} components", componentNames.size());

        } catch (final Exception e) {
            // Display error in component list
            componentListPanel.showError("Failed to discover components: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Component discovery failed", e);
        }
    }

    /**
     * Handles component selection events.
     * <p>
     * This method is called when a user selects a component from the list.
     * It orchestrates the complete flow:
     * <ol>
     *   <li>Clear previous component state</li>
     *   <li>Instantiate the selected component</li>
     *   <li>Discover setter methods</li>
     *   <li>Generate form controls</li>
     *   <li>Bind controls to component instance</li>
     *   <li>Display component preview</li>
     * </ol>
     * </p>
     *
     * @param componentName the name of the selected component
     */
    private void onComponentSelected(final String componentName) {
        LOGGER.log(Level.INFO, "Component selected: {0}", componentName);

        // Clear previous state (Requirement 2.3)
        clearPreviousComponent();

        try {
            // Get component class from registry
            final Class<? extends PWebComponent<?>> componentClass = registry.get(componentName);
            if (componentClass == null) {
                propertyPanel.showError("Component not found: " + componentName);
                return;
            }

            // Instantiate component (Requirement 3.1)
            currentComponent = instantiateComponent(componentClass);

            // Display component preview (Requirement 3.2)
            propertyPanel.setPreviewComponent(currentComponent);

            // Load slot metadata and create slot controls
            final String tagName = extractTagName(componentName);
            final List<SlotControl> slotControls = createSlotControls(tagName);
            propertyPanel.setSlotControls(slotControls);
            
            // Bind slot controls to component
            if (currentComponent instanceof PWebComponent) {
                for (final SlotControl slotControl : slotControls) {
                    slotControl.bindTo((PWebComponent<?>) currentComponent);
                }
            }

            // Discover setter methods (Requirement 4.1)
            final List<MethodSignature> methods = introspector.discoverSetters(componentClass);

            // Generate form controls (Requirement 5.1)
            final List<PropertyControl> controls = formGenerator.generateControls(methods);

            // Display form controls
            propertyPanel.setPropertyControls(controls);

            // Bind controls to component instance (Requirement 6.1)
            binder.bindControls(controls, currentComponent);

            LOGGER.log(Level.INFO, "Successfully loaded component: {0} with {1} properties",
                      new Object[]{componentName, methods.size()});

        } catch (final Exception e) {
            // Display error in property panel (Requirement 3.3)
            propertyPanel.showError("Failed to create " + componentName + ": " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Failed to load component: " + componentName, e);
        }
    }

    /**
     * Clears the previous component state.
     * <p>
     * This ensures only one component instance exists at a time (Requirement 3.4).
     * </p>
     */
    private void clearPreviousComponent() {
        currentComponent = null;
        propertyPanel.clear();
    }

    /**
     * Instantiates a component using its default constructor.
     *
     * @param componentClass the component class to instantiate
     * @return a new component instance
     * @throws Exception if instantiation fails
     */
    private PWebComponent<?> instantiateComponent(final Class<? extends PWebComponent<?>> componentClass) throws Exception {
        try {
            return componentClass.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            // Unwrap reflection exceptions for clearer error messages
            if (e.getCause() != null) {
                throw new Exception(e.getCause().getMessage(), e.getCause());
            }
            throw e;
        }
    }

    /**
     * Gets the current component instance.
     *
     * @return the current component, or null if no component is selected
     */
    public PWebComponent<?> getCurrentComponent() {
        return currentComponent;
    }

    /**
     * Gets the component list panel.
     *
     * @return the component list panel
     */
    public ComponentListPanel getComponentListPanel() {
        return componentListPanel;
    }

    /**
     * Gets the property panel.
     *
     * @return the property panel
     */
    public PropertyPanel getPropertyPanel() {
        return propertyPanel;
    }

    /**
     * Gets the component registry.
     *
     * @return the component registry
     */
    public ComponentRegistry getRegistry() {
        return registry;
    }

    /**
     * Extracts the tag name from a component name.
     * <p>
     * Converts "WAButton" to "wa-button".
     * </p>
     */
    private String extractTagName(final String componentName) {
        if (componentName.startsWith("WA")) {
            final String withoutPrefix = componentName.substring(2);
            return "wa-" + camelToKebab(withoutPrefix);
        }
        return camelToKebab(componentName);
    }

    /**
     * Converts camelCase to kebab-case.
     */
    private String camelToKebab(final String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    /**
     * Creates slot controls for a component based on its metadata.
     */
    private List<SlotControl> createSlotControls(final String tagName) {
        return metadataLoader.getMetadata(tagName)
            .map(metadata -> {
                final List<SlotControl> controls = new ArrayList<>();
                for (final SlotMetadata slot : metadata.slots()) {
                    controls.add(new SlotControl(slot));
                }
                return controls;
            })
            .orElse(Collections.emptyList());
    }
}
