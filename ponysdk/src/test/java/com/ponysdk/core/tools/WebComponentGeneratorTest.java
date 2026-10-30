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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
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
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
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

    @Test
    public void aUsageErrorIsPrintedWithStatusTwo() throws IOException {
        final java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();

        assertEquals(2, WebComponentGenerator.run(new String[] { "only-one" }, new java.io.PrintStream(err, true, StandardCharsets.UTF_8)));
        assertTrue(err.toString(StandardCharsets.UTF_8).startsWith("Usage: WebComponentGenerator"));
    }

    @Test
    public void mainFailsOnAUsageError() {
        final java.io.PrintStream stderr = System.err;
        System.setErr(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        try {
            assertThrows(IllegalArgumentException.class, () -> WebComponentGenerator.main(new String[] { "only-one" }));
        } finally {
            System.setErr(stderr);
        }
    }

    @Test
    public void mainWritesTheClassesOfTheManifest() throws Exception {
        final Path manifest = Path.of(WebComponentGeneratorTest.class.getResource("/webcomponents/custom-elements.json").toURI());
        final Path target = output.resolve("main");
        WebComponentGenerator.main(new String[] { manifest.toString(), PACKAGE, target.toString() });
        assertEquals(source, Files.readString(target.resolve("com/example/wc/PXGauge.java")));
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
        assertMethod("onValueChanged", HandlerRegistration.class, Consumer.class);
        assertMethod("onReady", HandlerRegistration.class, Consumer.class);
        assertMethod("configure", void.class, JsonValue.class);
        assertMethod("setSizeAttribute", void.class, enumType("Size"));
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
        assertTrue(source.contains("Named {@code setTitleProperty}: {@code setTitle} is PWebComponent's."), source);
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
        invoke(generated, "setSizeAttribute", constantOf("Size", "M"));
        invoke(generated, "configure", Json.createObjectBuilder().add("speed", 2).build());
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
        written.setAttribute("aria-label", "Value");
        written.setAttribute("compact", "");
        written.removeAttribute("compact");
        written.setStyleProperty("--needle-color", "red");
        written.setAttribute("size", "m");
        written.call("configure", Json.createObjectBuilder().add("speed", 2).build());
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
        final HandlerRegistration registration = (HandlerRegistration) invoke(generated, "onValueChanged", listener);
        assertEquals(List.of("value-changed"), writer.valuesOf(com.ponysdk.core.model.ServerToClientModel.WC_LISTEN_EVENT));

        generated.onClientData(event("value-changed", "{\"value\":7}"));
        generated.onClientData(event("value-changed", null));
        generated.onClientData(event("value-changed", "not json"));
        assertEquals(7, details.get(0).asJsonObject().getInt("value"));
        assertEquals(JsonValue.NULL, details.get(1));
        assertEquals(Json.createValue("not json"), details.get(2));

        registration.removeHandler();
        generated.onClientData(event("value-changed", "{}"));
        assertEquals(3, details.size(), "removed by its registration");
        assertEquals(List.of("value-changed"), writer.valuesOf(com.ponysdk.core.model.ServerToClientModel.WC_UNLISTEN_EVENT));
    }

    // ---- Failures ----

    @Test
    public void aRenamedMemberKeepsItsDescriptionBeforeTheRenaming() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"field","name":"title","type":{"text":"string"},"description":"The heading."}]}]}]}""";
        final String source = String.join("\n", sources(manifest).values());
        assertTrue(source.contains("The heading.\n     * Named {@code setTitleProperty}: {@code setTitle} is PWebComponent's."), source);
    }

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
    public void anEscapedQuoteInALiteralIsUnescaped() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"field","name":"mode","type":{"text":"'it\\\\'s' | 'b'"}}]}]}]}""";
        final String source = String.join("\n", sources(manifest).values());
        assertTrue(source.contains("IT_S(\"it's\")"), source);
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

    @Test
    public void moreNamesAndConstants() {
        assertEquals("_", WebComponentGenerator.camel("--"));
        assertEquals("A", WebComponentGenerator.constant("--a--"));
        assertEquals("ABC", WebComponentGenerator.constant("ABC"));
        assertEquals("A_B", WebComponentGenerator.constant("aB"));
    }

    @Test
    public void typesOfTheManifest() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"Plain"},
              {"kind":"class","name":"NoTag","customElement":true},
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a","deprecated":"use x-b",
               "members":[
                 {"kind":"field","name":"nothing","type":{"text":"null | undefined"}},
                 {"kind":"field","name":"untyped"},
                 {"kind":"field","name":"flag","type":{"text":"boolean | undefined"},"deprecated":true},
                 {"kind":"field","name":"ratio","type":{"text":"number | null"}},
                 {"kind":"field","name":"mode","type":{"text":"| 'a' | \\"b|c\\" | null"}},
                 {"kind":"field","name":"items","type":{"text":"Array<'x' | 'y'> | {k: (1 | 2)[]}"}},
                 {"kind":"field","name":"quoted","type":{"text":"'it\\\\'s' | 'q'"}},
                 {"kind":"method","name":"compute","return":{"type":{"text":"number"}},
                  "parameters":[{"name":"values","rest":true}]},
                 {"kind":"method","name":"reset","return":{"type":{"text":"Promise<void>"}}},
                 {"kind":"method","name":"clear","return":{"type":{"text":"void"}}},
                 {"kind":"method","name":"pick","parameters":[{"name":"mode","type":{"text":"'a' | \\"b|c\\""}}]}
               ],
               "attributes":[{"name":"mode","type":{"text":"'a' | \\"b|c\\""}}],
               "events":[{"description":"no name"}]}]}]}""";

        final String source = WebComponentGenerator.sources(Json.createReader(new StringReader(manifest)).readObject(), "", "test")
            .get("PA");

        assertFalse(source.contains("package "), "the default package");
        assertFalse(source.contains("Consumer"), "no named event");
        assertTrue(source.contains("@Deprecated\npublic class PA"), source);
        assertTrue(source.contains("public void setNothingJson(final String value)"), source);
        assertTrue(source.contains("As JSON text, of type {@code null | undefined}."), source);
        assertTrue(source.contains("public void setUntypedJson(final String value)"), source);
        assertTrue(source.contains("@Deprecated\n    public void setFlag(final Boolean value)"), source);
        assertTrue(source.contains("property(\"flag\").set(value.booleanValue());"), source);
        assertTrue(source.contains("property(\"ratio\").set(jsonOf(value.doubleValue()));"), source);
        assertTrue(source.contains("public void setMode(final Mode value)"), source);
        assertTrue(source.contains("B_C(\"b|c\")"), source);
        assertTrue(source.contains("public void setItemsJson(final String value)"), source);
        assertTrue(source.contains("public void setQuoted(final Quoted value)"), source);
        assertTrue(source.contains("public void compute(final JsonValue values)"), source);
        assertTrue(source.contains("public void compute()"), "the rest parameter can be left out");
        assertTrue(source.contains("Its result, of type {@code number}, does not come back."), source);
        assertFalse(source.contains("Promise"), "a promise of nothing returns nothing");
        assertTrue(source.contains("public void setModeAttribute(final Mode value)"), "the enum is shared");
        assertTrue(source.contains("call(\"pick\", mode == null ? null : mode.literal);"), source);
        assertEquals(1, source.split("public enum Mode ").length - 1);
    }

    @Test
    public void oddMembersAndTypesOfTheManifest() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[
                 1,
                 {"kind":"accessor","name":"skipped"},
                 {"kind":"field","name":"blank","type":{"text":"  "}},
                 {"kind":"field","name":"numbered","type":{"text":5}},
                 {"kind":"field","name":"generic","description":"First.\\n\\nThird.","type":{"text":"T"}},
                 {"kind":"field","name":"odd","type":{"text":"'a'b\\""}},
                 {"kind":"field","name":"template","type":{"text":"`t-${string}` | 'b'"}},
                 {"kind":"method","name":"fetch","description":"Fetches.","return":{"type":{"text":"string"}}},
                 {"kind":"method","name":"untypedResult","return":{}}
               ]}]}]}""";

        final String source = sources(manifest).get("PA");

        assertFalse(source.contains("skipped"), source);
        assertTrue(source.contains("As JSON text, of type {@code unknown}.\n     */\n    public void setBlankJson("), source);
        assertTrue(source.contains("public void setNumberedJson(final String value)"), source);
        assertTrue(source.contains("     * First.\n     *\n     * Third.\n     * As JSON text, of type {@code T}."), source);
        assertTrue(source.contains("public void setOddJson(final String value)"), source);
        assertTrue(source.contains("public void setTemplateJson(final String value)"), source);
        assertTrue(source.contains("     * Fetches.\n     * Its result, of type {@code string}, does not come back."), source);
        assertTrue(source.contains("    public void untypedResult() {"), source);
        assertFalse(source.contains("Its result, of type {@code null}"), source);
    }

    @Test
    public void aMemberNamedWithALeadingUnderscoreIsInternal() {
        // Web Awesome's wa-option: a backing field _label beside the property label, both "public" in the manifest
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"field","name":"_label","type":{"text":"string"}},
                          {"kind":"field","name":"label","type":{"text":"string"}},
                          {"kind":"method","name":"_render"}]}]}]}""";

        final String source = sources(manifest).get("PA");

        assertEquals(1, source.split("public void setLabel\\(").length - 1, source);
        assertFalse(source.contains("_render") || source.contains("render"), source);
    }

    @Test
    public void aTypeNoJsonTextCanStandForGivesNoAccessorAndNoMethod() {
        // Web Awesome's manifest lists what its Lit decorators query and its callbacks as public members: a dialog's
        // inner <dialog>, its ElementInternals, a function that formats, another custom element
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"WaPopup","customElement":true,"tagName":"x-popup"},
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"field","name":"dialog","type":{"text":"HTMLDialogElement"}},
                          {"kind":"field","name":"inputs","type":{"text":"(HTMLElement & { value: unknown }) | HTMLInputElement | undefined"}},
                          {"kind":"field","name":"internals","type":{"text":"ElementInternals"}},
                          {"kind":"field","name":"states","type":{"text":"CustomStateSet"}},
                          {"kind":"field","name":"reflected","type":{"text":"Map<string, unknown>"}},
                          {"kind":"field","name":"formatter","type":{"text":"(value: number) => string"}},
                          {"kind":"field","name":"popup","type":{"text":"WaPopup"}},
                          {"kind":"field","name":"options","type":{"text":"WaPopup[]"}},
                          {"kind":"field","name":"keyframes","type":{"text":"Keyframe[] | undefined"}},
                          {"kind":"field","name":"date","type":{"text":"Date | string"}},
                          {"kind":"field","name":"label","type":{"text":"string"}},
                          {"kind":"method","name":"drag","parameters":[{"name":"event","type":{"text":"PointerEvent"}}]},
                          {"kind":"method","name":"anchorTo","parameters":[{"name":"element","type":{"text":"Element | string"}}]},
                          {"kind":"method","name":"scrollTo","parameters":[{"name":"options","type":{"text":"ScrollToOptions"}}]}]}]}]}""";

        final String source = sources(manifest).get("PA");

        for (final String name : List.of("Dialog", "Inputs", "Internals", "States", "Reflected", "Formatter", "Popup", "Options")) {
            assertFalse(source.contains("set" + name), name + ": " + source);
            assertFalse(source.contains("get" + name), name + ": " + source);
        }
        assertFalse(source.contains(" drag("), "an event cannot be sent: " + source);
        assertFalse(source.contains(" anchorTo("), source);
        assertTrue(source.contains("public void scrollTo(final JsonValue options)"), "a dictionary can: " + source);
        assertTrue(source.contains("public void setKeyframesJson(final String value)"), source);
        assertTrue(source.contains("public void setDateJson(final String value)"), source);
        assertTrue(source.contains("public void setLabel(final String value)"), source);
    }

    @Test
    public void aMethodNamedLikeAnAccessorDoesNotOverloadIt() {
        // Web Awesome's form controls: a property value, and the internal method setValue(...args) of their base class,
        // listed in the manifest before and after it; setValue(null) would not compile with both
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"method","name":"setValue","description":"Internal.",
                           "parameters":[{"name":"args","type":{"text":"Parameters<typeof this.internals.setFormValue>"}}]},
                          {"kind":"field","name":"value","type":{"text":"string"}},
                          {"kind":"method","name":"reset","parameters":[{"name":"value","optional":true,"type":{"text":"string"}}]}],
               "events":[{"name":"reset"}]}]}]}""";

        final String source = sources(manifest).get("PA");

        assertTrue(source.contains("public void setValue(final String value)"), source);
        assertTrue(source.contains("public void setValueMethod(final JsonValue args)"), source);
        assertTrue(source.contains("Named {@code setValueMethod}: {@code setValue} is also the name of a property's accessor."), source);
        assertTrue(source.contains("public void reset(final String value)"), "no accessor named reset, no listener: " + source);
        assertFalse(source.contains("setValue(final JsonValue"), source);
    }

    @Test
    public void twoEnumsOfOneNameTakeTheNameOfTheirMember() {
        // Web Awesome's color picker: a field format and a method getFormattedValue(format), of different literals
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"field","name":"format","type":{"text":"'hex' | 'rgb'"}},
                          {"kind":"method","name":"getFormatted","parameters":[{"name":"format","type":{"text":"'hex' | 'hexa' | 'rgb'"}}]},
                          {"kind":"method","name":"other","parameters":[{"name":"format","type":{"text":"'hex' | 'rgb'"}}]}],
               "attributes":[{"name":"format","fieldName":"none","type":{"text":"'a' | 'b'"}}]}]}]}""";

        final String source = sources(manifest).get("PA");

        assertTrue(source.contains("public void setFormat(final Format value)"), source);
        assertTrue(source.contains("public void getFormatted(final GetFormattedFormat format)"), "the second takes its method's name: " + source);
        assertTrue(source.contains("public void other(final Format format)"), "the same literals share the first: " + source);
        assertTrue(source.contains("public void setFormatAttribute(final AttributeFormat value)"), source);
        assertEquals(1, source.split("public enum Format ").length - 1);
        assertEquals(1, source.split("public enum GetFormattedFormat ").length - 1);
    }

    @Test
    public void oneEnumNameForTwoTypesStillFailsWhenTheMemberNameDoesNot() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[
              {"kind":"class","name":"A","customElement":true,"tagName":"x-a",
               "members":[{"kind":"field","name":"mode","type":{"text":"'a' | 'b'"}}],
               "attributes":[{"name":"mode","fieldName":"m1","type":{"text":"'a' | 'c'"}},
                             {"name":"mode","fieldName":"m2","type":{"text":"'a' | 'd'"}}]}]}]}""";
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sources(manifest));
        assertEquals("<x-a>: two types named AttributeMode: [a, c], [a, d]", e.getMessage());
    }

    @Test
    public void oneClassNameForTwoTagsFails() {
        final String manifest = """
            {"modules":[{"path":"a.js","declarations":[{"kind":"class","name":"A","customElement":true,"tagName":"x-a"}]},
                        {"path":"b.js","declarations":[{"kind":"class","name":"A","customElement":true,"tagName":"x-b"}]}]}""";
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sources(manifest));
        assertEquals("Class A is declared twice: A in a.js, A in b.js", e.getMessage());
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
