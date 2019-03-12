package com.ponysdk.core.server.monitoring;

public class Monitor {
    private long lastSentPing;

    private final Latency roundtripLatency = new Latency(10);
    private final Latency networkLatency = new Latency(10);
    private final Latency terminalLatency = new Latency(10);

    /**
     * Adds a roundtrip latency value
     *
     * @param value the value
     */
    public void addRoundtripLatencyValue(final long value) {
        roundtripLatency.add(value);
    }

    /**
     * Gets an average roundtrip latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getRoundtripLatency() {
        return roundtripLatency.getValue();
    }

    /**
     * Adds a network latency value
     *
     * @param value the value
     */
    public void addNetworkLatencyValue(final long value) {
        networkLatency.add(value);
    }

    /**
     * Gets an average network latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getNetworkLatency() {
        return networkLatency.getValue();
    }

    /**
     * Adds a terminal latency value
     *
     * @param value the ping value
     */
    public void addTerminalLatencyValue(final long value) {
        terminalLatency.add(value);
    }

    /**
     * Gets an average terminal latency from the last 10 measurements
     *
     * @return the lantency
     */
    public double getTerminalLatency() {
        return terminalLatency.getValue();
    }
}
