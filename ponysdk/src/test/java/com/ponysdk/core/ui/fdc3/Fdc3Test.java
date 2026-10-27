/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.fdc3;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/** ADR-0014: {@link Fdc3}, the page's desktop agent driven from Java. */
public class Fdc3Test extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void reset() {
        writer.uninstall();
        UIContext.get().removeAttribute(Fdc3.class.getName());
    }

    /** The addon's method calls, as {@code name[arguments]}. */
    private List<String> calls() {
        final List<String> calls = new ArrayList<>();
        final List<String> frames = writer.frames();
        for (int i = 0; i < frames.size(); i++) {
            if (!frames.get(i).startsWith(ServerToClientModel.PADDON_METHOD + "=")) continue;
            final String method = frames.get(i).substring(frames.get(i).indexOf('=') + 1);
            final boolean withArguments = i + 1 < frames.size() && frames.get(i + 1).startsWith(ServerToClientModel.PADDON_ARGUMENTS + "=");
            calls.add(withArguments ? method + frames.get(i + 1).substring(frames.get(i + 1).indexOf('=') + 1) : method);
        }
        return calls;
    }

    private static void fromAgent(final Fdc3 fdc3, final JsonObject message) {
        fdc3.onClientData(Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), message).build());
    }

    private static String ticker(final JsonObject context) {
        return context.getJsonObject("id").getString("ticker");
    }

    @Test
    public void oneAddonPerContextOnItsMainWindow() {
        final Fdc3 fdc3 = Fdc3.get();
        assertSame(fdc3, Fdc3.get());
        assertEquals(List.of(Fdc3.class.getCanonicalName()), writer.valuesOf(ServerToClientModel.FACTORY));
        assertEquals(Fdc3.State.WAITING, fdc3.getState());
    }

    @Test
    public void aBroadcastSendsItsContextAndAContextHasAType() {
        final Fdc3 fdc3 = Fdc3.get();
        fdc3.broadcast(Fdc3.instrument("AAPL", "Apple"));
        assertEquals(List.of("broadcast[{\"type\":\"fdc3.instrument\",\"name\":\"Apple\",\"id\":{\"ticker\":\"AAPL\"}}]"), calls());
        assertThrows(IllegalArgumentException.class, () -> fdc3.broadcast(Json.createObjectBuilder().add("id", "x").build()));
    }

    @Test
    public void theAgentIsListenedToOncePerTypeUntilTheLastListenerGoes() {
        final Fdc3 fdc3 = Fdc3.get();
        final HandlerRegistration first = fdc3.addContextListener("fdc3.instrument", c -> {});
        final HandlerRegistration second = fdc3.addContextListener("fdc3.instrument", c -> {});
        final HandlerRegistration any = fdc3.addContextListener(null, c -> {});
        first.removeHandler();
        assertEquals(List.of("listenContext[fdc3.instrument]", "listenContext[*]"), calls(), "one per type, still listened");
        second.removeHandler();
        any.removeHandler();
        assertEquals(List.of("listenContext[fdc3.instrument]", "listenContext[*]", "unlistenContext[fdc3.instrument]",
            "unlistenContext[*]"), calls());
    }

    @Test
    public void aContextReachesTheListenersOfItsType() {
        final Fdc3 fdc3 = Fdc3.get();
        final List<String> instruments = new ArrayList<>();
        final List<String> any = new ArrayList<>();
        fdc3.addContextListener("fdc3.instrument", c -> instruments.add(ticker(c)));
        fdc3.addContextListener(null, c -> any.add(c.getString("type")));

        fromAgent(fdc3, Json.createObjectBuilder().add("listener", "fdc3.instrument").add("context", Fdc3.instrument("MSFT", null)).build());
        fromAgent(fdc3, Json.createObjectBuilder().add("listener", "*").add("context", Fdc3.instrument("MSFT", null)).build());

        assertEquals(List.of("MSFT"), instruments);
        assertEquals(List.of("fdc3.instrument"), any);
    }

    /** An intent's message carries its context too: it goes to the intent's handler, not to the context listeners. */
    @Test
    public void anIntentIsHandledAndItsResultSentBack() {
        final Fdc3 fdc3 = Fdc3.get();
        final List<String> contexts = new ArrayList<>();
        fdc3.addContextListener("fdc3.instrument", c -> contexts.add(ticker(c)));
        fdc3.addIntentListener("ViewInstrument", c -> Json.createObjectBuilder().add("type", "ponysdk.shown").add("ticker", ticker(c)).build());
        writer.clear();

        fromAgent(fdc3, Json.createObjectBuilder().add("intent", "ViewInstrument").add("request", 7)
            .add("context", Fdc3.instrument("TSLA", null)).build());

        assertEquals(List.of("intentResult[7, {\"type\":\"ponysdk.shown\",\"ticker\":\"TSLA\"}]"), calls());
        assertEquals(List.of(), contexts);
    }

    @Test
    public void anIntentWithoutResultOrWhoseHandlerFailsIsAnsweredToo() {
        final Fdc3 fdc3 = Fdc3.get();
        fdc3.addIntentListener("ViewChart", c -> JsonValue.NULL);
        fdc3.addIntentListener("StartChat", c -> {
            throw new IllegalStateException("no chat");
        });
        writer.clear();
        fromAgent(fdc3, Json.createObjectBuilder().add("intent", "ViewChart").add("request", 1).add("context", Fdc3.instrument("A", null)).build());
        fromAgent(fdc3, Json.createObjectBuilder().add("intent", "StartChat").add("request", 2).add("context", Fdc3.instrument("A", null)).build());
        fromAgent(fdc3, Json.createObjectBuilder().add("intent", "Unknown").add("request", 3).add("context", Fdc3.instrument("A", null)).build());
        assertEquals(List.of("intentResult[1, null]", "intentFailed[2, no chat]", "intentFailed[3, No handler]"), calls());
    }

    @Test
    public void oneHandlerPerIntentAndItsRemovalStopsTheAgent() {
        final Fdc3 fdc3 = Fdc3.get();
        final HandlerRegistration registration = fdc3.addIntentListener("ViewChart", c -> JsonValue.NULL);
        assertThrows(IllegalStateException.class, () -> fdc3.addIntentListener("ViewChart", c -> JsonValue.NULL));
        registration.removeHandler();
        registration.removeHandler();
        assertEquals(List.of("listenIntent[ViewChart]", "unlistenIntent[ViewChart]"), calls());
    }

    @Test
    public void aRaisedIntentCallsBackWithItsResultOrItsError() {
        final Fdc3 fdc3 = Fdc3.get();
        final List<Fdc3.IntentResult> results = new ArrayList<>();
        fdc3.raiseIntent("ViewInstrument", Fdc3.instrument("MSFT", null), results::add);
        fdc3.raiseIntent("ViewChart", Fdc3.instrument("MSFT", null), results::add);
        assertEquals(List.of("raiseIntent[1, ViewInstrument, {\"type\":\"fdc3.instrument\",\"id\":{\"ticker\":\"MSFT\"}}]",
            "raiseIntent[2, ViewChart, {\"type\":\"fdc3.instrument\",\"id\":{\"ticker\":\"MSFT\"}}]"), calls());

        fromAgent(fdc3, Json.createObjectBuilder().add("raised", 2).add("error", "NoAppsFound").build());
        fromAgent(fdc3, Json.createObjectBuilder().add("raised", 1).add("app", "fdc3-monitor").add("result",
            Json.createObjectBuilder().add("type", "ponysdk.shown")).build());
        fromAgent(fdc3, Json.createObjectBuilder().add("raised", 1).add("app", "late").build()); // answered once

        assertEquals(2, results.size());
        assertTrue(results.get(0).failed());
        assertEquals("NoAppsFound", results.get(0).error());
        assertEquals("fdc3-monitor", results.get(1).app());
        assertEquals("ponysdk.shown", results.get(1).result().asJsonObject().getString("type"));
        assertFalse(results.get(1).failed());
    }

    @Test
    public void theAgentReadyGivesItsInfoAndChannels() {
        final Fdc3 fdc3 = Fdc3.get();
        final List<Fdc3.State> states = new ArrayList<>();
        final List<String> channels = new ArrayList<>();
        fdc3.addStateListener(states::add);
        fdc3.addChannelListener(channels::add);

        fromAgent(fdc3, Json.createObjectBuilder().add("state", "ready")
            .add("info", Json.createObjectBuilder().add("provider", "Demo").add("fdc3Version", "2.1"))
            .add("userChannels", Json.createArrayBuilder().add(Json.createObjectBuilder().add("id", "fdc3.channel.1")))
            .addNull("channel").build());
        fromAgent(fdc3, Json.createObjectBuilder().add("channel", "fdc3.channel.1").build());
        fromAgent(fdc3, Json.createObjectBuilder().addNull("channel").build());

        assertEquals(List.of(Fdc3.State.WAITING, Fdc3.State.READY), states, "the current state at once, then each");
        assertEquals("Demo", fdc3.getInfo().getString("provider"));
        assertEquals("fdc3.channel.1", fdc3.getUserChannels().get(0).getString("id"));
        assertEquals(java.util.Arrays.asList("fdc3.channel.1", null), channels);
        assertNull(fdc3.getCurrentChannel());
    }

    @Test
    public void noAgentInThePageIsUnavailable() {
        final Fdc3 fdc3 = Fdc3.get();
        fromAgent(fdc3, Json.createObjectBuilder().add("state", "unavailable").build());
        assertEquals(Fdc3.State.UNAVAILABLE, fdc3.getState());
        assertNull(fdc3.getInfo());
    }

    @Test
    public void joiningAndLeavingAChannelCallTheAgent() {
        final Fdc3 fdc3 = Fdc3.get();
        fdc3.joinUserChannel("fdc3.channel.2");
        fdc3.leaveCurrentChannel();
        assertThrows(NullPointerException.class, () -> fdc3.joinUserChannel(null));
        assertEquals(List.of("joinUserChannel[fdc3.channel.2]", "leaveCurrentChannel"), calls());
    }

    @Test
    public void removedStateAndChannelListenersAreNotCalled() {
        final Fdc3 fdc3 = Fdc3.get();
        final List<Fdc3.State> states = new ArrayList<>();
        final List<String> channels = new ArrayList<>();
        fdc3.addStateListener(states::add).removeHandler();
        fdc3.addChannelListener(channels::add).removeHandler();
        fromAgent(fdc3, Json.createObjectBuilder().add("state", "unavailable").build());
        fromAgent(fdc3, Json.createObjectBuilder().add("channel", "fdc3.channel.1").build());
        assertEquals(List.of(Fdc3.State.WAITING), states);
        assertEquals(List.of(), channels);
        assertEquals("fdc3.channel.1", fdc3.getCurrentChannel());
    }

    @Test
    public void aContextListenerRemovedTwiceIsRemovedOnce() {
        final Fdc3 fdc3 = Fdc3.get();
        final List<String> received = new ArrayList<>();
        final HandlerRegistration first = fdc3.addContextListener("fdc3.instrument", c -> received.add("first"));
        final HandlerRegistration second = fdc3.addContextListener("fdc3.instrument", c -> received.add("second"));
        first.removeHandler();
        first.removeHandler();
        fromAgent(fdc3, Json.createObjectBuilder().add("listener", "fdc3.instrument").add("context", Fdc3.instrument("A", null)).build());
        assertEquals(List.of("second"), received);

        second.removeHandler();
        second.removeHandler();
        assertEquals(List.of("listenContext[fdc3.instrument]", "unlistenContext[fdc3.instrument]"), calls());

        // a context of a type nobody listens to is dropped
        fromAgent(fdc3, Json.createObjectBuilder().add("listener", "fdc3.instrument").add("context", Fdc3.instrument("B", null)).build());
        assertEquals(List.of("second"), received);
    }

    @Test
    public void aRaisedIntentWithoutCallbackIgnoresItsAnswer() {
        final Fdc3 fdc3 = Fdc3.get();
        fdc3.raiseIntent("ViewChart", Fdc3.instrument("A", null));
        fromAgent(fdc3, Json.createObjectBuilder().add("raised", 1).add("app", "chart").build());
        assertEquals(List.of("raiseIntent[1, ViewChart, {\"type\":\"fdc3.instrument\",\"id\":{\"ticker\":\"A\"}}]"), calls());
        assertThrows(NullPointerException.class, () -> fdc3.raiseIntent(null, Fdc3.instrument("A", null)));
    }

    @Test
    public void anIntentHandlerReturningJavaNullAnswersNull() {
        final Fdc3 fdc3 = Fdc3.get();
        fdc3.addIntentListener("ViewChart", c -> null);
        writer.clear();
        fromAgent(fdc3, Json.createObjectBuilder().add("intent", "ViewChart").add("request", 4).add("context", Fdc3.instrument("A", null)).build());
        assertEquals(List.of("intentResult[4, null]"), calls());
    }

    @Test
    public void otherMessagesGoToTheTerminalHandler() {
        final Fdc3 fdc3 = Fdc3.get();
        final List<JsonObject> events = new ArrayList<>();
        fdc3.setTerminalHandler(e -> events.add(e.getData()));
        final JsonObject other = Json.createObjectBuilder().add("other", 1).build();
        fromAgent(fdc3, other);
        fdc3.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), "x").build());
        assertEquals(List.of(other), events);
    }

    @Test
    public void aDestroyedAgentIgnoresItsMessages() {
        final Fdc3 fdc3 = Fdc3.get();
        fdc3.onDestroy();
        fromAgent(fdc3, Json.createObjectBuilder().add("state", "unavailable").build());
        assertEquals(Fdc3.State.WAITING, fdc3.getState());
    }

    @Test
    public void getOutsideTheUIThreadFails() throws InterruptedException {
        final Throwable[] thrown = new Throwable[1];
        final Thread thread = new Thread(() -> {
            try {
                Fdc3.get();
            } catch (final Throwable t) {
                thrown[0] = t;
            }
        });
        thread.start();
        thread.join();
        assertInstanceOf(IllegalStateException.class, thrown[0]);
    }
}
