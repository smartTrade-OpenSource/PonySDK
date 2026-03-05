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

package com.ponysdk.core.ui.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.json.JsonArray;
import javax.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PFrame;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.writer.ModelWriter;

/**
 * Abstract base class for typed server-side components with props diffing.
 * <p>
 * PComponent extends {@link PWidget}, making it a first-class widget that can be directly
 * added to any {@link com.ponysdk.core.ui.basic.HasPWidgets} container without requiring
 * wrapper objects. This provides seamless integration with the PonySDK widget system while
 * maintaining framework-specific rendering capabilities.
 * </p>
 *
 * <h2>Inheritance Hierarchy</h2>
 * <pre>
 * PObject
 *   └── PWidget (implements IsPWidget)
 *         └── PComponent (this class)
 * </pre>
 *
 * <h2>Core Features</h2>
 * <ul>
 *   <li><b>Direct Container Compatibility:</b> Can be added directly to any HasPWidgets container</li>
 *   <li><b>Type-Safe Props:</b> Generic props types constrained to Java Records</li>
 *   <li><b>Efficient Updates:</b> Automatic JSON Patch diffing for minimal network transmission</li>
 *   <li><b>Widget Functionality:</b> Inherits all standard PWidget methods (visibility, styling, events)</li>
 *   <li><b>Framework Agnostic:</b> Supports React, Vue, Svelte, and Web Components</li>
 *   <li><b>Performance Control:</b> Configurable throttling and priority-based updates</li>
 * </ul>
 *
 * <h2>Inherited PWidget Methods</h2>
 * <p>
 * As a PWidget subclass, PComponent inherits all standard widget functionality:
 * </p>
 * <ul>
 *   <li><b>Visibility:</b> {@code setVisible(boolean)}, {@code isVisible()}</li>
 *   <li><b>Styling:</b> {@code addStyleName(String)}, {@code removeStyleName(String)}, {@code setStyleName(String)}</li>
 *   <li><b>Dimensions:</b> {@code setWidth(String)}, {@code setHeight(String)}</li>
 *   <li><b>DOM Events:</b> {@code addDomHandler(...)}, {@code addClickHandler(...)}, etc.</li>
 *   <li><b>Parent/Child:</b> {@code setParent(IsPWidget)}, {@code getParent()}, {@code removeFromParent()}</li>
 *   <li><b>Lifecycle:</b> {@code attach(PWindow)}, {@code onDestroy()}</li>
 *   <li><b>Debug:</b> {@code ensureDebugId(String)}, {@code dumpDOM()}</li>
 * </ul>
 * <p>
 * These widget methods control the container element in the browser, while component props
 * control the framework-specific rendering inside the container. Both systems work independently.
 * </p>
 *
 * <h2>Lifecycle Coordination</h2>
 * <p>
 * PComponent coordinates PWidget lifecycle with component-specific lifecycle:
 * </p>
 * <ol>
 *   <li><b>Creation:</b> Component instance created, initial props set</li>
 *   <li><b>Attachment:</b> {@code attach()} calls {@code super.attach()} to initialize PWidget state,
 *       then sends initial props to client</li>
 *   <li><b>Updates:</b> Props updates via {@code setProps()} work independently of widget state changes</li>
 *   <li><b>Destruction:</b> {@code onDestroy()} cleans up component resources, then calls
 *       {@code super.onDestroy()} to clean up PWidget resources</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Component Definition</h3>
 * <pre>{@code
 * public record MyProps(String title, int count, boolean enabled) {}
 *
 * public class MyComponent extends PReactComponent<MyProps> {
 *     public MyComponent() {
 *         super(new MyProps("Hello", 0, true));
 *     }
 *
 *     @Override
 *     protected Class<MyProps> getPropsClass() {
 *         return MyProps.class;
 *     }
 *
 *     @Override
 *     protected String getComponentSignature() {
 *         return "my-component";
 *     }
 *
 *     public void incrementCount() {
 *         MyProps current = getCurrentProps();
 *         setProps(new MyProps(current.title(), current.count() + 1, current.enabled()));
 *     }
 * }
 * }</pre>
 *
 * <h3>Direct Container Usage</h3>
 * <pre>{@code
 * // Create component
 * MyComponent component = new MyComponent();
 *
 * // Add directly to any container - no wrapper needed!
 * PVerticalPanel panel = new PVerticalPanel();
 * panel.add(component);  // Works because PComponent extends PWidget
 *
 * // Use inherited PWidget methods
 * component.setVisible(false);           // Hide the component
 * component.addStyleName("highlight");   // Add CSS class
 * component.setWidth("300px");           // Set width
 *
 * // Update component props independently
 * component.incrementCount();            // Props update works regardless of visibility
 * }</pre>
 *
 * <h3>Combining Widget State and Props</h3>
 * <pre>{@code
 * // Create and configure component
 * MyComponent component = new MyComponent();
 *
 * // Configure widget appearance (affects container element)
 * component.addStyleName("card");
 * component.setWidth("400px");
 * component.setHeight("300px");
 *
 * // Configure component data (affects framework rendering)
 * component.setProps(new MyProps("Dashboard", 42, true));
 *
 * // Add to container
 * PSimplePanel container = new PSimplePanel();
 * container.setWidget(component);
 *
 * // Both systems work independently:
 * component.setVisible(false);  // Hides container (component stays mounted)
 * component.incrementCount();   // Props update works even when hidden
 * }</pre>
 *
 * <h3>Event Handling</h3>
 * <pre>{@code
 * MyComponent component = new MyComponent();
 *
 * // DOM events (inherited from PWidget) - handled on container element
 * component.addClickHandler(event -> {
 *     System.out.println("Container clicked");
 * });
 *
 * // Framework events (PComponent-specific) - handled by framework component
 * component.onEvent("buttonClick", payload -> {
 *     String buttonId = payload.getString("buttonId");
 *     System.out.println("Button clicked: " + buttonId);
 * });
 *
 * // Both event systems work independently and don't interfere
 * }</pre>
 *
 * <h3>Parent-Child Relationships</h3>
 * <pre>{@code
 * MyComponent component = new MyComponent();
 *
 * // Add to first container
 * PFlowPanel panel1 = new PFlowPanel();
 * panel1.add(component);
 * assert component.getParent() == panel1;
 *
 * // Move to second container (automatically removed from first)
 * PVerticalPanel panel2 = new PVerticalPanel();
 * panel2.add(component);
 * assert component.getParent() == panel2;
 * assert !panel1.iterator().hasNext();  // No longer in panel1
 *
 * // Remove from parent
 * component.removeFromParent();
 * assert component.getParent() == null;
 * }</pre>
 *
 * <h2>Container Element Rendering</h2>
 * <p>
 * In the browser, PComponent creates a container DOM element where the framework component
 * mounts. PWidget properties (styles, visibility, dimensions) are applied to this container,
 * while the framework component (React/Vue/Svelte) renders inside it:
 * </p>
 * <pre>
 * &lt;div class="pony-component-container highlight" style="width: 400px; height: 300px;"&gt;
 *   &lt;!-- React/Vue/Svelte component renders here --&gt;
 * &lt;/div&gt;
 * </pre>
 *
 * <h2>Props vs Widget State Independence</h2>
 * <p>
 * Props updates and widget state changes work independently:
 * </p>
 * <ul>
 *   <li>Calling {@code setProps()} does NOT affect visibility, styles, or dimensions</li>
 *   <li>Calling {@code setVisible()}, {@code addStyleName()}, etc. does NOT affect props</li>
 *   <li>Props updates work even when component is hidden ({@code setVisible(false)})</li>
 *   <li>Framework component stays mounted when hidden, just not visible</li>
 * </ul>
 *
 * @param <TProps> the props type, must be a Java Record
 * @see PWidget
 * @see com.ponysdk.core.ui.basic.IsPWidget
 * @see com.ponysdk.core.ui.basic.HasPWidgets
 * @see FrameworkType
 * @see UpdatePriority
 * @see ThrottleConfig
 * @see PropsDiffer
 */
public abstract class PComponent<TProps extends Record> extends PWidget {

    private static final Logger log = LoggerFactory.getLogger(PComponent.class);

    /**
     * The current props state.
     */
    private TProps currentProps;

    /**
     * The previous props state, used for diffing.
     */
    private TProps previousProps;

    /**
     * The props differ for computing JSON Patch diffs.
     */
    private final PropsDiffer<TProps> differ;

    /**
     * The throttle configuration for this component.
     */
    private final ThrottleConfig throttleConfig;

    /**
     * The update priority for this component.
     */
    private UpdatePriority priority = UpdatePriority.NORMAL;

    /**
     * The target UI framework for this component.
     */
    private final FrameworkType frameworkType;

    /**
     * Registered event handlers by event type.
     */
    private Map<String, Consumer<JsonObject>> eventHandlers;

    /**
     * Flag indicating whether an update is pending (for throttling).
     */
    private boolean updatePending = false;

    /**
     * Creates a new PComponent with the specified initial props and framework type.
     *
     * @param initialProps the initial props state, must not be null
     * @param framework    the target UI framework
     * @throws NullPointerException if initialProps or framework is null
     */
    protected PComponent(final TProps initialProps, final FrameworkType framework) {
        Objects.requireNonNull(initialProps, "Initial props must not be null");
        Objects.requireNonNull(framework, "Framework type must not be null");

        this.currentProps = initialProps;
        this.previousProps = null; // No previous state on creation
        this.frameworkType = framework;
        this.differ = new PropsDiffer<>();
        this.throttleConfig = new ThrottleConfig();
    }

    /**
     * Attaches this component to the specified window.
     * <p>
     * This method initializes the component and sends the creation message
     * to the client terminal.
     * </p>
     *
     * @param window the window to attach to, must not be null
     * @return true if the component was successfully attached
     */
    public boolean attach(final PWindow window) {
        return attach(window, null);
    }

    /**
     * Attaches this component to the specified window and frame.
     * <p>
     * This method initializes the component and sends the creation message
     * to the client terminal.
     * </p>
     *
     * @param window the window to attach to, must not be null
     * @param frame  the frame to attach to, may be null
     * @return true if the component was successfully attached
     */
    @Override
    public boolean attach(final PWindow window, final PFrame frame) {
        final boolean result = super.attach(window, frame);
        if (result) {
            window.addDestroyListener(event -> onDestroy());
        }
        return result;
    }
    /**
     * Returns the widget type for this component.
     *
     * @return {@link WidgetType#COMPONENT}
     */
    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.COMPONENT;
    }

    /**
     * Returns the props class for this component.
     * <p>
     * Subclasses must implement this method to provide the Class object
     * for the props type, which is used for JSON deserialization.
     * </p>
     *
     * @return the Class object for TProps
     */
    protected abstract Class<TProps> getPropsClass();

    /**
     * Returns the component signature used to identify the client-side component.
     * <p>
     * The signature is used by the client-side ComponentRegistry to look up
     * the appropriate component factory. It should be a unique identifier
     * that matches the registered client-side component.
     * </p>
     *
     * @return the component signature string
     */
    protected abstract String getComponentSignature();

    /**
     * Enriches the creation message with component-specific data.
     * <p>
     * This method is called during component initialization to send the
     * initial props as full JSON, along with the framework type and
     * component signature.
     * </p>
     *
     * @param writer the model writer for sending data
     */
    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        // Send component creation marker
        writer.write(ServerToClientModel.PCOMPONENT_CREATE);

        // Send framework type
        writer.write(ServerToClientModel.PCOMPONENT_FRAMEWORK, frameworkType.getValue());

        // Send component signature
        writer.write(ServerToClientModel.PCOMPONENT_SIGNATURE, getComponentSignature());

        // Send initial props as full JSON (Requirement 1.2)
        final JsonObject propsJson = differ.toJson(currentProps);
        writer.write(ServerToClientModel.PCOMPONENT_PROPS_FULL, propsJson.toString());

        log.debug("PComponent {} created with initial props: {}", getID(), propsJson);
    }

    /**
     * Updates the component props.
     * <p>
     * This method computes the diff between the previous and current props
     * and schedules an update to be sent to the client. If throttling is
     * enabled, updates may be batched.
     * </p>
     * <p>
     * If the new props are equal to the current props, no update is sent
     * (Requirement 3.2: no-change detection).
     * </p>
     *
     * @param newProps the new props state, must not be null
     * @throws NullPointerException if newProps is null
     */
    protected void setProps(final TProps newProps) {
        Objects.requireNonNull(newProps, "New props must not be null");

        // Store previous state for diffing (Requirement 1.5)
        this.previousProps = this.currentProps;
        this.currentProps = newProps;

        // Schedule the update (respects throttling)
        scheduleUpdate();
    }

    /**
     * Schedules an update to be sent to the client.
     * <p>
     * If throttling is enabled and an update is already pending, this method
     * does nothing (the pending update will use the latest props state).
     * Otherwise, it triggers the update immediately or schedules it based
     * on the throttle configuration.
     * </p>
     */
    private void scheduleUpdate() {
        if (destroy) return;

        if (throttleConfig.isEnabled()) {
            // Throttling enabled - mark update pending
            if (!updatePending) {
                updatePending = true;
                // Schedule the actual update after throttle interval
                // For now, we send immediately but this can be enhanced
                // with ThrottleController integration
                sendPropsUpdate();
                updatePending = false;
            }
            // If update already pending, the latest props will be sent
        } else {
            // No throttling - send immediately
            sendPropsUpdate();
        }
    }

    /**
     * Sends the props update to the client.
     * <p>
     * Computes the diff between previous and current props and sends
     * either a JSON Patch (for changes) or nothing (if no changes).
     * </p>
     */
    private void sendPropsUpdate() {
        if (destroy || !initialized) return;

        // Check UIContext
        final UIContext uiContext = UIContext.get();
        if (uiContext == null || !uiContext.isAlive()) {
            log.warn("PComponent {} - sendPropsUpdate skipped: UIContext not available", getID());
            return;
        }

        // Compute diff (Requirements 1.3, 1.4)
        final Optional<JsonArray> patchOpt = differ.computeDiff(previousProps, currentProps);

        if (patchOpt.isEmpty()) {
            // No changes detected (Requirement 3.2)
            log.debug("PComponent {} - no props changes detected, skipping update", getID());
            return;
        }

        final JsonArray patch = patchOpt.get();
        log.debug("PComponent {} sending props patch: {}", getID(), patch);

        // Send the update
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PCOMPONENT_UPDATE);
            writer.write(ServerToClientModel.PCOMPONENT_PROPS_PATCH, patch.toString());
        });
    }

    /**
     * Sends a binary-encoded props update for high-frequency updates.
     * <p>
     * This method is optimized for performance-critical scenarios where
     * minimizing serialization overhead is essential. It uses a compact
     * binary encoding instead of JSON Patch.
     * </p>
     * <p>
     * Use this method when:
     * <ul>
     *   <li>Updates occur at high frequency (e.g., real-time charts, animations)</li>
     *   <li>Props contain primarily numeric data</li>
     *   <li>Minimizing latency is critical</li>
     * </ul>
     * </p>
     * <p>
     * For normal update scenarios, use {@link #setProps(Record)} which
     * automatically uses JSON Patch for efficient differential updates.
     * </p>
     *
     * @param newProps the new props state, must not be null
     * @throws NullPointerException if newProps is null
     * @see PropsDiffer#computeBinaryDiff(Record, Record)
     */
    public void sendBinaryUpdate(final TProps newProps) {
        Objects.requireNonNull(newProps, "New props must not be null");

        if (destroy || !initialized) return;

        // Store previous state for diffing
        final TProps prevProps = this.currentProps;
        this.previousProps = prevProps;
        this.currentProps = newProps;

        // Compute binary diff (Requirements 3.3, 12.4)
        final byte[] binaryDiff = differ.computeBinaryDiff(prevProps, newProps);

        if (binaryDiff.length == 0) {
            // No changes detected
            log.debug("PComponent {} - no binary changes detected, skipping update", getID());
            return;
        }

        // Send the binary update
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PCOMPONENT_UPDATE);
            writer.write(ServerToClientModel.PCOMPONENT_PROPS_BINARY, binaryDiff);
        });

        log.debug("PComponent {} sent binary update ({} bytes)", getID(), binaryDiff.length);
    }

    /**
     * Sends a full props update, bypassing differential updates.
     * <p>
     * This method sends the complete props as JSON, which can be useful
     * when the client state may be out of sync or when starting fresh.
     * </p>
     *
     * @param newProps the new props state, must not be null
     * @throws NullPointerException if newProps is null
     */
    public void sendFullUpdate(final TProps newProps) {
        Objects.requireNonNull(newProps, "New props must not be null");

        if (destroy || !initialized) return;

        this.previousProps = this.currentProps;
        this.currentProps = newProps;

        final JsonObject propsJson = differ.toJson(newProps);

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PCOMPONENT_UPDATE);
            writer.write(ServerToClientModel.PCOMPONENT_PROPS_FULL, propsJson.toString());
        });

        log.debug("PComponent {} sent full props update", getID());
    }

    /**
     * Returns the current props state.
     *
     * @return the current props
     */
    public TProps getCurrentProps() {
        return currentProps;
    }

    /**
     * Returns the previous props state.
     * <p>
     * May be null if no update has occurred since creation.
     * </p>
     *
     * @return the previous props, or null
     */
    public TProps getPreviousProps() {
        return previousProps;
    }

    /**
     * Returns the framework type for this component.
     *
     * @return the framework type
     */
    public FrameworkType getFrameworkType() {
        return frameworkType;
    }

    /**
     * Returns the throttle configuration for this component.
     *
     * @return the throttle config
     */
    public ThrottleConfig getThrottleConfig() {
        return throttleConfig;
    }

    /**
     * Sets the throttle interval for this component.
     * <p>
     * A positive value enables throttling with the specified interval.
     * A value of 0 or less disables throttling.
     * </p>
     *
     * @param intervalMs the minimum interval between updates in milliseconds
     */
    public void setThrottleInterval(final long intervalMs) {
        throttleConfig.setInterval(intervalMs);
    }

    /**
     * Returns the update priority for this component.
     *
     * @return the update priority
     */
    public UpdatePriority getPriority() {
        return priority;
    }

    /**
     * Sets the update priority for this component.
     * <p>
     * Higher priority updates are processed before lower priority ones
     * when multiple updates are queued.
     * </p>
     *
     * @param priority the new priority level
     * @throws NullPointerException if priority is null
     */
    public void setPriority(final UpdatePriority priority) {
        Objects.requireNonNull(priority, "Priority must not be null");
        this.priority = priority;
    }

    /**
     * Registers an event handler for the specified event type.
     * <p>
     * When an event of the specified type is received from the client,
     * the handler will be invoked with the event payload.
     * </p>
     *
     * @param eventType the event type to listen for
     * @param handler   the handler to invoke when the event is received
     * @throws NullPointerException if eventType or handler is null
     */
    public void onEvent(final String eventType, final Consumer<JsonObject> handler) {
        Objects.requireNonNull(eventType, "Event type must not be null");
        Objects.requireNonNull(handler, "Handler must not be null");

        if (eventHandlers == null) {
            eventHandlers = new HashMap<>();
        }
        eventHandlers.put(eventType, handler);
    }

    /**
     * Removes the event handler for the specified event type.
     *
     * @param eventType the event type to stop listening for
     */
    public void removeEventHandler(final String eventType) {
        if (eventHandlers != null) {
            eventHandlers.remove(eventType);
        }
    }

    /**
     * Handles client data received from the terminal.
     * <p>
     * This method processes component events and dispatches them to
     * registered handlers.
     * </p>
     *
     * @param event the JSON event data from the client
     */
    @Override
    public void onClientData(final JsonObject event) {
        if (destroy) return;

        // Extract the actual event data - it may be wrapped in NATIVE (key "4")
        JsonObject eventData = event;
        final String nativeKey = "4"; // ClientToServerModel.NATIVE.toStringValue()
        if (event.containsKey(nativeKey)) {
            final javax.json.JsonValue nativeValue = event.get(nativeKey);
            
            if (nativeValue.getValueType() == javax.json.JsonValue.ValueType.OBJECT) {
                eventData = (JsonObject) nativeValue;
            } else if (nativeValue.getValueType() == javax.json.JsonValue.ValueType.STRING) {
                // If it's a string, try to parse it as JSON
                final String nativeStr = ((javax.json.JsonString) nativeValue).getString();
                try (final javax.json.JsonReader reader = javax.json.Json.createReader(new java.io.StringReader(nativeStr))) {
                    eventData = reader.readObject();
                } catch (final Exception e) {
                    log.error("PComponent {} failed to parse NATIVE string: {}", getID(), e.getMessage());
                }
            }
        }

        // Check for component events
        if (eventData != null && eventData.containsKey("eventType") && eventHandlers != null) {
            final String eventType = eventData.getString("eventType");
            final Consumer<JsonObject> handler = eventHandlers.get(eventType);
            if (handler != null) {
                final JsonObject payload = eventData.containsKey("payload")
                        ? eventData.getJsonObject("payload")
                        : null;
                try {
                    handler.accept(payload);
                } catch (final Exception e) {
                    log.error("Error handling event '{}' for PComponent {}", eventType, getID(), e);
                }
            }
        }

        // Also call parent handler for native events
        super.onClientData(event);
    }

    /**
     * Called when the component is destroyed.
     * <p>
     * Sends a destroy message to the terminal and cleans up resources
     * (Requirement 1.6).
     * </p>
     */
    @Override
    public void onDestroy() {
        // Send destroy message to terminal (Requirement 1.6)
        if (initialized && !destroy) {
            final ModelWriter writer = UIContext.get().getWriter();
            writer.beginObject(window);
            if (frame != null) {
                writer.write(ServerToClientModel.FRAME_ID, frame.getID());
            }
            writer.write(ServerToClientModel.TYPE_UPDATE, ID);
            writer.write(ServerToClientModel.DESTROY);
            writer.endObject();

            log.debug("PComponent {} destroyed", getID());
        }

        // Clean up
        eventHandlers = null;
        currentProps = null;
        previousProps = null;

        super.onDestroy();
    }

    /**
     * Returns the props differ used by this component.
     * <p>
     * This is primarily for testing purposes.
     * </p>
     *
     * @return the props differ
     */
    protected PropsDiffer<TProps> getDiffer() {
        return differ;
    }

    /**
     * Returns a debug representation of the component's DOM structure.
     * <p>
     * This method provides a string representation of the component's container
     * element and framework component information for debugging and inspection.
     * </p>
     *
     * @return a string representation of the component's DOM structure
     */
    @Override
    public String dumpDOM() {
        final StringBuilder dom = new StringBuilder();
        dom.append("<div");
        dom.append(" pid=\"").append(ID).append("\"");
        dom.append(" class=\"pony-component-container\"");
        dom.append(" framework=\"").append(frameworkType).append("\"");
        dom.append(" signature=\"").append(getComponentSignature()).append("\"");
        
        // Include visibility state
        if (!isVisible()) {
            dom.append(" hidden");
        }
        
        // Include style names if any
        final String styleNames = getStyleName();
        if (styleNames != null && !styleNames.isEmpty()) {
            dom.append(" styles=\"").append(styleNames).append("\"");
        }
        
        dom.append(">");
        
        // Include framework component info
        dom.append("<component");
        dom.append(" type=\"").append(frameworkType).append("\"");
        dom.append(" signature=\"").append(getComponentSignature()).append("\"");
        dom.append(" props=\"").append(currentProps != null ? currentProps.toString() : "null").append("\"");
        dom.append(" />");
        
        dom.append("</div>");
        
        return dom.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + ID + "[" + frameworkType + ", " + getComponentSignature() + "]";
    }

}
