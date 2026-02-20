package com.ponysdk.sample.client.page.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.component.PSvelteComponent;

/**
 * Svelte-based counter component demonstrating PSvelteComponent.
 */
public class SvelteCounterComponent extends PSvelteComponent<CounterProps> {

    private static final Logger log = LoggerFactory.getLogger(SvelteCounterComponent.class);

    public SvelteCounterComponent(final String label, final int initialCount, final String color) {
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
        return "svelte-counter";
    }

    public void increment() {
        final CounterProps current = getCurrentProps();
        setProps(new CounterProps(current.label(), current.count() + 1, current.color()));
        log.info("Svelte counter incremented to {}", current.count() + 1);
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
