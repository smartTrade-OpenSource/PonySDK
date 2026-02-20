package com.ponysdk.sample.client.page.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.component.PVueComponent;

/**
 * Vue-based counter component demonstrating PVueComponent.
 */
public class VueCounterComponent extends PVueComponent<CounterProps> {

    private static final Logger log = LoggerFactory.getLogger(VueCounterComponent.class);

    public VueCounterComponent(final String label, final int initialCount, final String color) {
        super(new CounterProps(label, initialCount, color));
        
        onEvent("increment", payload -> increment());
        onEvent("decrement", payload -> decrement());
    }

    @Override
    protected Class<CounterProps> getPropsClass() {
        return CounterProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "vue-counter";
    }

    public void increment() {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), current.count() + 1, current.color()));
        log.info("Vue counter incremented to {}", current.count() + 1);
    }

    public void decrement() {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), current.count() - 1, current.color()));
    }

    public void setColor(final String color) {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), current.count(), color));
    }

    public void reset() {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), 0, current.color()));
    }

    public int getCount() {
        return getCurrentProps().count();
    }
}
