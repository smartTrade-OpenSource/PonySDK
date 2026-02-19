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

package com.ponysdk.sample.client.page.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.component.PReactComponent;

/**
 * Sample PComponent demonstrating the new typed component system.
 * This is a simple counter that can be incremented/decremented from the server.
 */
public class CounterComponent extends PReactComponent<CounterProps> {

    private static final Logger log = LoggerFactory.getLogger(CounterComponent.class);

    public CounterComponent() {
        this("Counter", 0, "#3498db");
    }

    public CounterComponent(final String label, final int initialCount, final String color) {
        super(new CounterProps(label, initialCount, color));
        
        // Register click event handler from client
        onEvent("increment", payload -> {
            log.info("Increment event received from client");
            increment();
        });
        
        onEvent("decrement", payload -> {
            log.info("Decrement event received from client");
            decrement();
        });
    }

    @Override
    protected Class<CounterProps> getPropsClass() {
        return CounterProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "sample-counter";
    }

    /**
     * Increments the counter by 1.
     */
    public void increment() {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), current.count() + 1, current.color()));
        log.info("Counter incremented to {}", current.count() + 1);
    }

    /**
     * Decrements the counter by 1.
     */
    public void decrement() {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), current.count() - 1, current.color()));
        log.info("Counter decremented to {}", current.count() - 1);
    }

    /**
     * Sets the counter value directly.
     */
    public void setCount(final int count) {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), count, current.color()));
    }

    /**
     * Sets the label.
     */
    public void setLabel(final String label) {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(label, current.count(), current.color()));
    }

    /**
     * Sets the color.
     */
    public void setColor(final String color) {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), current.count(), color));
    }

    /**
     * Gets the current count value.
     */
    public int getCount() {
        return getCurrentProps().count();
    }
}
