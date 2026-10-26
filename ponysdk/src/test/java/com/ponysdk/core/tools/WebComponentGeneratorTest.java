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

package com.ponysdk.core.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.ui.basic.PWebComponent;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * {@link WebComponentGenerator} (ADR-0010): the classes of a fixture manifest compile, have the API of the
 * manifest, and write the frames of the hand-written {@link PWebComponent} calls.
 */
public class WebComponentGeneratorTest extends PSuite {

    private static final String PACKAGE = "com.example.wc";

    @TempDir
    static Path output;
    private static Class<?> gauge;
    private static String source;

    private CapturingModelWriter writer;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        final Path manifest = Path.of(WebComponentGeneratorTest.class.getResource("/webcomponents/custom-elements.json").toURI());
        final List<Path> files = WebComponentGenerator.generate(manifest, PACKAGE, output.resolve("src"));
        assertEquals(List.of("PXGauge.java"), files.stream().map(f -> f.getFileName().toString()).toList(),
            "one class per custom element, none for XBase");
        source = Files.readString(files.get(0));

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final Path classes = Files.createDirectories(output.resolve("classes"));
        final List<String> args = new ArrayList<>(List.of("-classpath", System.getProperty("java.class.path"), "-d",
            classes.toString(), "-Xlint:all", "-Werror", "-proc:none"));
        for (final Path file : files) args.add(file.toString());
        assertEquals(0, compiler.run(null, null, null, args.toArray(String[]::new)), source);

        final URLClassLoader loader = new URLClassLoader(new URL[] { classes.toUri().toURL() },
            WebComponentGeneratorTest.class.getClassLoader());
        gauge = loader.loadClass(PACKAGE + ".PXGauge");
    }

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    // ---- API ----

    @Test
    public void theClassIsAPWebComponentOfItsTag() throws Exception {
        assertEquals(PWebComponent.class, gauge.getSuperclass());
        assertEquals("x-gauge", gauge.getField("TAG").get(null));
        assertEquals("x-gauge", ((PWebComponent) gauge.getConstructor().newInstance()).getTagName());
    }

    @Test
    public void fieldsGiveATypedSetterAndGetter() throws Exception {
        final Class<?> theme = enumType("Theme");
        assertMethod("setLabel", void.class, String.class);
        assertMethod("getLabel", String.class);
        assertMethod("setValue", void.class, double.class);
        assertMethod("getValue", Double.class);
        assertMethod("setMax", void.class, Double.class);
        assertMethod("setAnimated", void.class, boolean.class);
        assertMethod("getAnimated", Boolean.class);
        assertMethod("setTheme", void.class, theme);
        assertMethod("getTheme", theme);
        assertMethod("setRangesJson", void.class, String.class);
        assertMethod("getRangesJson", String.class);
        assertMethod("setLocale", void.class, String.class); // inherited in the manifest
        assertEquals(List.of("LIGHT", "DARK", "HIGH_CONTRAST"),
            Arrays.stream(theme.getEnumConstants()).map(c -> ((Enum<?>) c).name()).toList());
    }

    @Test
    public void readonlyPrivateAndStaticFieldsGiveNothing() {
        for (final Method method : gauge.getDeclaredMethods()) {
            assertFalse(method.getName().matches("(set|get)(Version|Cache|_cache|Secret|Styles)"), method.getName());
        }
    }

    @Test
    public void aNameThePWidgetHasTakesTheSuffixProperty() throws Exception {
        assertMethod("setTitleProperty", void.class, String.class);
        assertMethod("getTitleProperty", String.class);
        assertNotEquals(gauge, gauge.getMethod("setTitle", String.class).getDeclaringClass(), "PWidget's setTitle stays");
    }

    @Test
    public void attributesEventsMethodsSlotsAndCssProperties() throws Exception {
        assertMethod("setAriaLabelAttribute", void.class, String.class);
        assertMethod("setCompact", void.class, boolean.class);
        assertThrows(NoSuchMethodException.class, () -> gauge.getDeclaredMethod("setLabelAttribute", String.class),
            "label is the field's attribute");
        assertMethod("onValueChanged", void.class, Consumer.class);
        assertMethod("onReady", void.class, Consumer.class);
        assertMethod("reset", void.class);
        assertMethod("animateTo", void.class, double.class);
        assertMethod("animateTo", void.class, double.class, enumType("Easing"));
        assertMethod("default_", void.class);
        assertMethod("measure", void.class);
        assertMethod("slotFooter", PWebComponent.SlotPanel.class);
        assertMethod("setCssNeedleColor", void.class, String.class);
        assertTrue(gauge.getMethod("setOldScale", double.class).isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void descriptionsAreJavadocThatCannotCloseTheComment() {
        assertTrue(source.contains("A gauge. Its &lt;b&gt;needle&lt;/b&gt; ends the comment *&#47; not."), source);
        assertTrue(source.contains("Its result, of type {@code number}, does not come back."), source);
        assertTrue(source.contains("As JSON text, of type {@code Array&lt;{ from: number; to: number }&gt;}."), source);
    }

    // ---- The frames: those of the hand-written calls ----

    @Test
    public void settersWriteTheFramesOfTheHandWrittenCalls() throws Exception {
        final PWebComponent generated = attached();
        final Object dark = constantOf("Theme", "DARK");
        invoke(generated, "setLabel", "Gross \"P&L\"");
        invoke(generated, "setValue", 42.0);
        invoke(generated, "setValue", 0.5);
        invoke(generated, "setMax", (Object) null);
        invoke(generated, "setAnimated", true);
        invoke(generated, "setTheme", dark);
        invoke(generated, "setRangesJson", "[{\"from\":0,\"to\":1}]");
        invoke(generated, "setTitleProperty", "Gauge");
        invoke(generated, "setAriaLabelAttribute", "Value");
        invoke(generated, "setCompact", true);
        invoke(generated, "setCompact", false);
        invoke(generated, "setCssNeedleColor", "red");
        invoke(generated, "reset");
        invoke(generated, "animateTo", 3.0, constantOf("Easing", "EASE_IN"));
        invoke(generated, "slotFooter");
        final List<String> generatedFrames = frames();

        final PWebComponent written = attached();
        written.property("label").set("\"Gross \\\"P&L\\\"\"");
        written.property("value").set(42);
        written.property("value").set(0.5);
        written.property("max").remove();
        written.property("animated").set(true);
        written.property("theme").set("\"dark\"");
        written.property("ranges").set("[{\"from\":0,\"to\":1}]");
        written.property("title").set("\"Gauge\"");
        written.attr("aria-label", "Value");
        written.attr("compact", "");
        written.removeAttr("compact");
        written.setStyleProperty("--needle-color", "red");
        written.call("reset");
        written.call("animateTo", 3.0, "ease-in");
        written.slot("footer");
        assertEquals(frames(), generatedFrames);
    }

    @Test
    public void anIntegralNumberIsWrittenWithoutAFraction() throws Exception {
        final PWebComponent generated = attached();
        invoke(generated, "setValue", 42.0);
        invoke(generated, "setValue", -3.0);
        invoke(generated, "setValue", 1e20);
        assertEquals(List.of("42", "-3", "1.0E20"), writer.valuesOf(com.ponysdk.core.model.ServerToClientModel.WC_PROPERTY_VALUE));
    }

    @Test
    public void gettersReadTheValueLastSet() throws Exception {
        final PWebComponent generated = attached();
        assertNull(invoke(generated, "getValue"));
        invoke(generated, "setLabel", "Gross \"P&L\"");
        invoke(generated, "setValue", 42.0);
        invoke(generated, "setAnimated", false);
        invoke(generated, "setTheme", constantOf("Theme", "HIGH_CONTRAST"));
        assertEquals("Gross \"P&L\"", invoke(generated, "getLabel"));
        assertEquals(42.0, invoke(generated, "getValue"));
        assertEquals(Boolean.FALSE, invoke(generated, "getAnimated"));
        assertEquals("HIGH_CONTRAST", ((Enum<?>) invoke(generated, "getTheme")).name());
        invoke(generated, "setLabel", (Object) null);
        assertNull(invoke(generated, "getLabel"));
    }

    @Test
    public void anEventListenerReceivesTheDetail() throws Exception {
        final PWebComponent generated = attached();
        final List<JsonValue> details = new ArrayList<>();
        final Consumer<JsonValue> listener = details::add;
        invoke(generated, "onValueChanged", listener);
        assertEquals(List.of("value-changed"), writer.valuesOf(com.ponysdk.core.model.ServerToClientModel.WC_LISTEN_EVENT));

        generated.onClientData(event("value-changed", "{\"value\":7}"));
        generated.onClientData(event("value-changed", null));
        generated.onClientData(event("value-changed", "not json"));
        assertEquals(7, details.get(0).asJsonObject().getInt("value"));
        assertEquals(JsonValue.NULL, details.get(1));
        assertEquals(Json.createValue("not json"), details.get(2));
    }

    // ---- Failures ----

    @Test
    public void twoDeclarationsOfOneTagFail() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a"},
              {"kind":"class","name":"B","customElement":true,"tagName":"x-a"}]}]}""";
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sources(manifest));
        assertEquals("<x-a> is declared twice: A in a.js, B in a.js", e.getMessage());
    }

    @Test
    public void twoMembersGivingOneMethodFail() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"field","name":"labelAttribute","type":{"text":"string"}}],
               "attributes":[{"name":"label","type":{"text":"string"}}]}]}]}""";
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sources(manifest));
        assertEquals("<x-a>: two members give PA.setLabelAttribute(String)", e.getMessage());
    }

    @Test
    public void twoLiteralsGivingOneConstantFail() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"field","name":"mode","type":{"text":"'a-b' | 'a_b'"}}]}]}]}""";
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sources(manifest));
        assertEquals("<x-a>: two literals of Mode give A_B", e.getMessage());
    }

    @Test
    public void namesAndConstants() {
        assertEquals("countChanged", WebComponentGenerator.camel("count-changed"));
        assertEquals("needleColor", WebComponentGenerator.camel("--needle-color"));
        assertEquals("ariaLabel", WebComponentGenerator.camel("aria-label"));
        assertEquals("_2x", WebComponentGenerator.camel("2x"));
        assertEquals("TOP_START", WebComponentGenerator.constant("top-start"));
        assertEquals("TOP_START", WebComponentGenerator.constant("topStart"));
        assertEquals("_2X", WebComponentGenerator.constant("2x"));
        assertEquals("EMPTY", WebComponentGenerator.constant(""));
    }

    // ---- Helpers ----

    private static java.util.Map<String, String> sources(final String manifest) {
        return WebComponentGenerator.sources(Json.createReader(new StringReader(manifest)).readObject(), "p", "test");
    }

    private PWebComponent attached() throws Exception {
        final PWebComponent component = (PWebComponent) gauge.getConstructor().newInstance();
        PWindow.getMain().add(component);
        writer.clear();
        return component;
    }

    /** The frames since the last call, the object's id left out: two objects are compared. */
    private List<String> frames() {
        final List<String> frames = writer.frames().stream().filter(f -> !f.startsWith("TYPE_UPDATE=")).toList();
        writer.clear();
        return frames;
    }

    private static Class<?> enumType(final String name) {
        return Arrays.stream(gauge.getDeclaredClasses()).filter(c -> c.getSimpleName().equals(name)).findFirst()
            .orElseThrow(() -> new AssertionError("no enum " + name));
    }

    private static Object constantOf(final String type, final String name) {
        return Arrays.stream(enumType(type).getEnumConstants()).filter(c -> ((Enum<?>) c).name().equals(name)).findFirst()
            .orElseThrow();
    }

    private static void assertMethod(final String name, final Class<?> returns, final Class<?>... parameters)
            throws NoSuchMethodException {
        final Method method = gauge.getMethod(name, parameters);
        assertEquals(returns, method.getReturnType(), name);
        assertTrue(Modifier.isPublic(method.getModifiers()), name);
    }

    private static Object invoke(final Object target, final String name, final Object... args) throws Exception {
        for (final Method method : gauge.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(name + " with " + args.length + " arguments");
    }

    private static JsonObject event(final String name, final String detail) {
        final var builder = Json.createObjectBuilder().add(ClientToServerModel.WC_EVENT_NAME.toStringValue(), name);
        if (detail != null) builder.add(ClientToServerModel.WC_EVENT_DETAIL.toStringValue(), detail);
        return builder.build();
    }

}
