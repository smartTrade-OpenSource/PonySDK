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

import javax.json.JsonObject;

/**
 * Functional interface defining the handler contract for component events.
 * <p>
 * This interface is used to handle events dispatched from client-side components
 * back to the server. It supports typed event payloads that match the server-side
 * event handlers, enabling type-safe event handling in PComponent.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Register a handler for a specific event type
 * component.onEvent("click", payload -> {
 *     int x = payload.getInt("x");
 *     int y = payload.getInt("y");
 *     handleClick(x, y);
 * });
 *
 * // Using method reference
 * component.onEvent("submit", this::handleSubmit);
 *
 * // Using ComponentEventHandler explicitly
 * ComponentEventHandler handler = payload -> {
 *     String value = payload.getString("value");
 *     processValue(value);
 * };
 * component.onEvent("change", handler);
 * }</pre>
 *
 * <h2>Event Payload Structure</h2>
 * <p>
 * The event payload is a {@link JsonObject} containing the event data sent from
 * the client. The structure of the payload depends on the event type and the
 * client-side component implementation. Common patterns include:
 * </p>
 * <ul>
 *   <li>Click events: {@code {"x": 100, "y": 200, "button": 0}}</li>
 *   <li>Input events: {@code {"value": "user input", "valid": true}}</li>
 *   <li>Custom events: Any JSON structure defined by the component</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * Implementations should handle potential errors gracefully. If an exception
 * is thrown during event handling, it will be logged by the PComponent but
 * will not affect other event handlers or the component lifecycle.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Event handlers are invoked on the UI thread associated with the component's
 * {@link com.ponysdk.core.server.application.UIContext}. Implementations should
 * avoid blocking operations and delegate long-running tasks to background threads.
 * </p>
 *
 * @see PComponent#onEvent(String, java.util.function.Consumer)
 * @see javax.json.JsonObject
 * @since 2.9
 */
@FunctionalInterface
public interface ComponentEventHandler {

    /**
     * Handles a component event with the given payload.
     * <p>
     * This method is invoked when an event of the registered type is received
     * from the client-side component. The payload contains the event data
     * serialized as JSON.
     * </p>
     *
     * @param payload the event payload as a JSON object, may be {@code null}
     *                if the event was dispatched without data
     */
    void handleEvent(JsonObject payload);

    /**
     * Returns a composed handler that first invokes this handler and then
     * invokes the {@code after} handler.
     * <p>
     * If either handler throws an exception, it is relayed to the caller.
     * If this handler throws an exception, the {@code after} handler will
     * not be invoked.
     * </p>
     *
     * @param after the handler to invoke after this handler
     * @return a composed handler
     * @throws NullPointerException if {@code after} is null
     */
    default ComponentEventHandler andThen(ComponentEventHandler after) {
        java.util.Objects.requireNonNull(after, "after handler must not be null");
        return payload -> {
            handleEvent(payload);
            after.handleEvent(payload);
        };
    }

    /**
     * Creates a handler that does nothing.
     * <p>
     * This can be useful as a default or placeholder handler.
     * </p>
     *
     * @return a no-op handler
     */
    static ComponentEventHandler noOp() {
        return payload -> { /* no-op */ };
    }

    /**
     * Creates a handler that wraps the given consumer.
     * <p>
     * This is a convenience method for converting a {@link java.util.function.Consumer}
     * to a {@code ComponentEventHandler}.
     * </p>
     *
     * @param consumer the consumer to wrap
     * @return a handler that delegates to the consumer
     * @throws NullPointerException if {@code consumer} is null
     */
    static ComponentEventHandler from(java.util.function.Consumer<JsonObject> consumer) {
        java.util.Objects.requireNonNull(consumer, "consumer must not be null");
        return consumer::accept;
    }

}
