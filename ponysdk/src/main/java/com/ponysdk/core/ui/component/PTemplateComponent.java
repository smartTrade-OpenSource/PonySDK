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

/**
 * Abstract base class for HTML template-based PComponents.
 * <p>
 * PTemplateComponent extends {@link PWebComponent} and provides a way to create
 * components using HTML templates defined on the client side. The server only sends
 * JSON props updates via WebSocket, keeping the protocol efficient.
 * </p>
 * <p>
 * The template and styles are registered once on the client side (in JavaScript).
 * The server-side component only manages the props state and event handling.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // 1. Server-side (Java)
 * public record CardProps(String title, String content, String color) {}
 *
 * public class CardComponent extends PTemplateComponent<CardProps> {
 *     public CardComponent() {
 *         super(new CardProps("Title", "Content", "#3498db"));
 *     }
 *
 *     @Override
 *     protected Class<CardProps> getPropsClass() {
 *         return CardProps.class;
 *     }
 *
 *     @Override
 *     protected String getComponentSignature() {
 *         return "card-component";
 *     }
 * }
 *
 * // 2. Client-side (JavaScript) - register once
 * registerTemplateComponent("card-component", {
 *     template: `
 *         <div class="card" style="border-color: {{color}}">
 *             <h2>{{title}}</h2>
 *             <p>{{content}}</p>
 *         </div>
 *     `,
 *     styles: `
 *         .card { border: 2px solid; padding: 1rem; }
 *     `
 * });
 * }</pre>
 *
 * @param <TProps> the props type, must be a Java Record
 * @see PWebComponent
 * @see FrameworkType#WEB_COMPONENT
 */
public abstract class PTemplateComponent<TProps extends Record> extends PComponent<TProps> {

    /**
     * Creates a new PTemplateComponent with the specified initial props.
     * <p>
     * The template and styles must be registered on the client side using
     * registerTemplateComponent() before the component is created.
     * </p>
     *
     * @param initialProps the initial props state, must not be null
     * @throws NullPointerException if initialProps is null
     */
    protected PTemplateComponent(final TProps initialProps) {
        super(initialProps, FrameworkType.TEMPLATE);
    }
}
