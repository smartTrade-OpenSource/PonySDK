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

package com.ponysdk.core.ui.basic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.logging.Level;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/**
 * Protocol tests for {@link PAddOn} / {@link PAddOnComposite}: binary vs JSON creation arguments,
 * terminal method calls and lifecycle commands.
 */
public class PAddOnTest extends PSuite {

    private static final class BinaryAddOn extends PAddOn {
        BinaryAddOn(final Object... args) { super(args); }
        void call(final String method, final Object... args) { callTerminalMethod(method, args); }
    }

    private static final class JsonAddOn extends PAddOn {
        JsonAddOn(final JsonObject args) { super(args); }
    }

    private static final class NoArgAddOn extends PAddOn {
    }

    private static final class LabelAddOn extends PAddOnComposite<PLabel> {
        LabelAddOn(final PLabel label, final Object... args) { super(label, args); }
        LabelAddOn(final PLabel label, final JsonObject args) { super(label, args); }
    }

    private CapturingModelWriter writer;

    @Before
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @After
    public void restoreWriter() {
        writer.uninstall();
    }

    // Pre-existing tests (kept)

    @Test
    public void testInit() {
        final PAddOn widget = new PAddOn() {
        };
        assertEquals(WidgetType.ADDON, widget.getWidgetType());
        assertNotNull(widget.toString());
    }

    @Test
    public void testSignature() {
        final PAddOn widget = new JsonAddOn(Json.createObjectBuilder().build());
        assertEquals(JsonAddOn.class.getCanonicalName(), widget.getSignature());
    }

    // ---- creation args ----

    @Test
    public void binaryCreationArgsAreSentAsATypedArray() {
        final BinaryAddOn addOn = new BinaryAddOn(1, 2L, 3.5, true, "s");
        assertTrue(addOn.attach(PWindow.getMain()));

        assertEquals(List.of(WidgetType.ADDON.getValue()), writer.valuesOf(ServerToClientModel.WIDGET_TYPE));
        assertEquals(List.of(addOn.getSignature()), writer.valuesOf(ServerToClientModel.FACTORY));
        assertArrayEquals(new Object[] { 1, 2L, 3.5, true, "s" },
                (Object[]) writer.valuesOf(ServerToClientModel.PADDON_CREATION_ARGS).get(0));
        assertEquals("no JSON form", 0, writer.count(ServerToClientModel.PADDON_CREATION));
    }

    @Test
    public void jsonCreationArgsAreSentAsAString() {
        final JsonObject args = Json.createObjectBuilder().add("k", 1).build();
        new JsonAddOn(args).attach(PWindow.getMain());

        assertEquals(List.of(args.toString()), writer.valuesOf(ServerToClientModel.PADDON_CREATION));
        assertEquals(0, writer.count(ServerToClientModel.PADDON_CREATION_ARGS));
    }

    @Test
    public void addOnWithoutArgsSendsOnlyTheFactory() {
        final NoArgAddOn addOn = new NoArgAddOn();
        addOn.attach(PWindow.getMain());

        assertEquals(1, writer.count(ServerToClientModel.FACTORY));
        assertEquals(0, writer.count(ServerToClientModel.PADDON_CREATION));
        assertEquals(0, writer.count(ServerToClientModel.PADDON_CREATION_ARGS));
        assertEquals(NoArgAddOn.class.getCanonicalName(), addOn.getSignature());
    }

    @Test
    public void terminalMethodCallsCarryArgumentsOnlyWhenPresent() {
        final BinaryAddOn addOn = new BinaryAddOn();
        addOn.attach(PWindow.getMain());
        writer.clear();

        addOn.call("refresh");
        addOn.call("zoom", 2, "x");

        assertEquals(List.of("refresh", "zoom"), writer.valuesOf(ServerToClientModel.PADDON_METHOD));
        assertEquals("no args block for a no-arg call", 1, writer.count(ServerToClientModel.PADDON_ARGUMENTS));
        assertArrayEquals(new Object[] { 2, "x" }, (Object[]) writer.valuesOf(ServerToClientModel.PADDON_ARGUMENTS).get(0));
    }

    @Test
    public void callsBeforeAttachAreDeliveredAfterCreation() {
        final BinaryAddOn addOn = new BinaryAddOn();
        addOn.call("init", 1);
        assertTrue("stacked, not sent", writer.valuesOf(ServerToClientModel.PADDON_METHOD).isEmpty());

        addOn.attach(PWindow.getMain());

        final List<ServerToClientModel> models = writer.models();
        assertTrue("method sent after the CREATE",
                models.indexOf(ServerToClientModel.TYPE_CREATE) < models.indexOf(ServerToClientModel.PADDON_METHOD));
    }

    @Test
    public void setLogLevelAndDestroyAreSent() {
        final BinaryAddOn addOn = new BinaryAddOn();
        addOn.attach(PWindow.getMain());
        writer.clear();

        addOn.setLogLevel(Level.WARNING);
        addOn.destroy();

        assertEquals(List.of("setLogLevel"), writer.valuesOf(ServerToClientModel.PADDON_METHOD));
        assertArrayEquals(new Object[] { (byte) 2 }, (Object[]) writer.valuesOf(ServerToClientModel.PADDON_ARGUMENTS).get(0));
        assertEquals(1, writer.count(ServerToClientModel.DESTROY));
    }

    @Test
    public void attachTwiceToTheSameWindowIsANoOp() {
        final BinaryAddOn addOn = new BinaryAddOn();
        assertTrue(addOn.attach(PWindow.getMain()));
        writer.clear();
        assertEquals(false, addOn.attach(PWindow.getMain()));
        assertTrue(writer.isEmpty());
    }

    // ---- PAddOnComposite ----

    @Test
    public void compositeOnAttachedWidgetIsCreatedImmediatelyWithWidgetId() {
        final PLabel label = Element.newPLabel("x");
        PWindow.getMain().add(label);
        writer.clear();

        final LabelAddOn addOn = new LabelAddOn(label, 42);

        assertEquals(List.of(WidgetType.ADDON_COMPOSITE.getValue()), writer.valuesOf(ServerToClientModel.WIDGET_TYPE));
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.WIDGET_ID));
        assertArrayEquals(new Object[] { 42 }, (Object[]) writer.valuesOf(ServerToClientModel.PADDON_CREATION_ARGS).get(0));
        assertEquals(label, addOn.asWidget());
    }

    @Test
    public void compositeOnDetachedWidgetWaitsForTheWidget() {
        final PLabel label = Element.newPLabel("later");
        final JsonObject args = Json.createObjectBuilder().add("k", "v").build();
        new LabelAddOn(label, args);
        assertEquals("not created before its widget", 0, writer.count(ServerToClientModel.FACTORY));

        PWindow.getMain().add(label);

        assertEquals(1, writer.count(ServerToClientModel.FACTORY));
        assertEquals(List.of(args.toString()), writer.valuesOf(ServerToClientModel.PADDON_CREATION));
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.WIDGET_ID));
    }
}
