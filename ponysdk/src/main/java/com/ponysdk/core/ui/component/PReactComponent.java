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
 * Abstract base class for React-based PComponents.
 * <p>
 * PReactComponent extends {@link PComponent} and specifies React as the target
 * UI framework. Components extending this class will be rendered using the
 * React 18 client-side adapter, which uses {@code createRoot} for mounting
 * and props-based updates for re-rendering.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public record CounterProps(String label, int count) {}
 *
 * public class CounterComponent extends PReactComponent<CounterProps> {
 *
 *     public CounterComponent() {
 *         super(new CounterProps("Counter", 0));
 *     }
 *
 *     @Override
 *     protected Class<CounterProps> getPropsClass() {
 *         return CounterProps.class;
 *     }
 *
 *     @Override
 *     protected String getComponentSignature() {
 *         return "counter-component";
 *     }
 *
 *     public void increment() {
 *         CounterProps current = getCurrentProps();
 *         setProps(new CounterProps(current.label(), current.count() + 1));
 *     }
 * }
 * }</pre>
 *
 * @param <TProps> the props type, must be a Java Record
 * @see PComponent
 * @see FrameworkType#REACT
 */
public abstract class PReactComponent<TProps extends Record> extends PComponent<TProps> {

    /**
     * Creates a new PReactComponent with the specified initial props.
     * <p>
     * The component is automatically configured to use the React framework type,
     * which will be included in the creation message sent to the client terminal.
     * </p>
     *
     * @param initialProps the initial props state, must not be null
     * @throws NullPointerException if initialProps is null
     */
    protected PReactComponent(final TProps initialProps) {
        super(initialProps, FrameworkType.REACT);
    }

}
