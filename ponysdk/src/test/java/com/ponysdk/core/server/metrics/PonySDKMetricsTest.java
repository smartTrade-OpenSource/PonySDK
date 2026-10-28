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

package com.ponysdk.core.server.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.server.websocket.WebsocketMonitor;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;

/**
 * Verifies {@link PonySDKMetrics#utf8Length(String)} matches the real UTF-8 wire size
 * (the bytes-received metric must count bytes, not characters).
 */
public class PonySDKMetricsTest {

    @Test
    public void utf8LengthMatchesActualEncoding() {
        assertEquals(0, PonySDKMetrics.utf8Length(null));
        assertEquals(0, PonySDKMetrics.utf8Length(""));
        check("ascii");
        check("caf\u00e9");                 // é = 2 bytes
        check("\u20ac");                    // € = 3 bytes
        check("\uD83D\uDE80");              // 🚀 = 4 bytes (surrogate pair)
        check("a\u00e9\u20ac\uD83D\uDE80z"); // mixed
        check("{\"k\":\"\u00e9\uD83D\uDE80\"}");
    }

    @Test
    public void contextLifecycleMovesTheActiveCount() {
        final PonySDKMetrics metrics = new PonySDKMetrics(OpenTelemetry.noop(), "app");
        assertEquals(0, metrics.getActiveContexts());

        metrics.onContextCreated();
        metrics.onContextCreated();
        assertEquals(2, metrics.getActiveContexts());

        metrics.onContextDestroyed("error");
        assertEquals(1, metrics.getActiveContexts());

        metrics.contextDestroyListener().onBeforeDestroy(null);
        assertEquals(0, metrics.getActiveContexts());
    }

    @Test
    public void bytesAreAccumulated() {
        final PonySDKMetrics metrics = new PonySDKMetrics(OpenTelemetry.noop(), "app");
        metrics.recordBytesSent(100);
        metrics.recordBytesSent(23);
        metrics.recordBytesReceived(7);

        assertEquals(123, metrics.getTotalBytesSent());
        assertEquals(7, metrics.getTotalBytesReceived());
    }

    @Test
    public void websocketMonitorCountsReceivedMessagesInUtf8Bytes() {
        final PonySDKMetrics metrics = new PonySDKMetrics(OpenTelemetry.noop(), "app");
        final WebsocketMonitor monitor = metrics.websocketMonitor();

        monitor.onMessageReceived(null, "caf\u00e9");
        monitor.onMessageProcessed(null, "ignored");
        monitor.onMessageUnprocessed(null, "ignored");

        assertEquals(5, metrics.getTotalBytesReceived());
        assertEquals(0, metrics.getTotalBytesSent());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void theActiveGaugeReportsTheOpenContexts() {
        final Meter meter = Mockito.mock(Meter.class, Mockito.RETURNS_DEEP_STUBS);
        final LongGaugeBuilder gauge = Mockito.mock(LongGaugeBuilder.class, Mockito.RETURNS_SELF);
        Mockito.when(meter.gaugeBuilder("ponysdk.uicontext.active").ofLongs()).thenReturn(gauge);
        final OpenTelemetry otel = Mockito.mock(OpenTelemetry.class);
        Mockito.when(otel.getMeter(Mockito.anyString())).thenReturn(meter);
        final PonySDKMetrics metrics = new PonySDKMetrics(otel, "app");
        final ArgumentCaptor<Consumer<ObservableLongMeasurement>> callback = ArgumentCaptor.forClass(Consumer.class);
        Mockito.verify(gauge).buildWithCallback(callback.capture());
        metrics.onContextCreated();
        metrics.onContextCreated();

        final ObservableLongMeasurement measurement = Mockito.mock(ObservableLongMeasurement.class);
        callback.getValue().accept(measurement);

        Mockito.verify(measurement).record(Mockito.eq(2L), Mockito.any(Attributes.class));
    }

    private static void check(final String s) {
        assertEquals(s.getBytes(StandardCharsets.UTF_8).length,
            PonySDKMetrics.utf8Length(s), "UTF-8 length mismatch for: " + s);
    }
}
