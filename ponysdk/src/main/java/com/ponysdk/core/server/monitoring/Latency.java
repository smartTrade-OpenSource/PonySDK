package com.ponysdk.core.server.monitoring;

import java.util.Arrays;

public class Latency {

    private int index = 0;
    private final long[] values;

    public Latency(final int size) {
        values = new long[size];
        Arrays.fill(values, 0);
    }

    public void add(final long value) {
        values[index++] = value;
        if (index >= values.length) index = 0;
    }

    public double getValue() {
        return Arrays.stream(values).average().orElse(0);
    }
}