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
 * Abstract base class for Web Component-based PComponents.
 * <p>
 * PWebComponent extends {@link PComponent} and specifies Web Components as the target
 * UI framework. Components extending this class will be rendered using the
 * Web Components client-side adapter, which uses custom elements and shadow DOM
 * for encapsulated rendering.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public record CardProps(String title, String content) {}
 *
 * public class CardComponent extends PWebComponent<CardProps> {
 *
 *     public CardComponent() {
 *         super(new CardProps("Default Title", "Default content"));
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
 *
 *     public void updateTitle(String newTitle) {
 *         CardProps current = getCurrentProps();
 *         setProps(new CardProps(newTitle, current.content()));
 *     }
 * }
 * }</pre>
 *
 * @param <TProps> the props type, must be a Java Record
 * @see PComponent
 * @see FrameworkType#WEB_COMPONENT
 */
public abstract class PWebComponent<TProps extends Record> extends PComponent<TProps> {

    /**
     * Creates a new PWebComponent with the specified initial props.
     * <p>
     * The component is automatically configured to use the Web Component framework type,
     * which will be included in the creation message sent to the client terminal.
     * </p>
     *
     * @param initialProps the initial props state, must not be null
     * @throws NullPointerException if initialProps is null
     */
    protected PWebComponent(final TProps initialProps) {
        super(initialProps, FrameworkType.WEB_COMPONENT);
    }

}
