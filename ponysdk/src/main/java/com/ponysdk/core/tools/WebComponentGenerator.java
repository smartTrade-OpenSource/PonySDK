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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.ponysdk.core.ui.basic.PWebComponent;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Writes a {@link PWebComponent} subclass per custom element of a Custom Elements Manifest
 * ({@code custom-elements.json}): typed setters for its fields, attributes and CSS custom properties, a listener per
 * event, its methods and its named slots (ADR-0010). Run by a build:
 *
 * <pre>
 * java -cp ponysdk.jar com.ponysdk.core.tools.WebComponentGenerator custom-elements.json com.example.wc build/generated/wc
 * </pre>
 */
public final class WebComponentGenerator {

    private static final Set<String> KEYWORDS = Set.of("abstract", "assert", "boolean", "break", "byte", "case",
        "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
        "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "true",
        "false", "null", "var", "record", "yield", "sealed", "permits");

    /** The signatures a generated method must not take: those of PWebComponent and its superclasses. */
    private static final Set<String> INHERITED = inheritedSignatures();

    private WebComponentGenerator() {
    }

    public static void main(final String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: WebComponentGenerator <custom-elements.json> <java package> <output directory>");
            System.exit(2);
        }
        generate(Path.of(args[0]), args[1], Path.of(args[2]));
    }

    /**
     * Writes the classes of the manifest's custom elements under {@code outputDirectory}, in {@code javaPackage}.
     *
     * @return the files written
     */
    public static List<Path> generate(final Path manifest, final String javaPackage, final Path outputDirectory)
            throws IOException {
        final JsonObject root;
        try (Reader in = Files.newBufferedReader(manifest); JsonReader reader = Json.createReader(in)) {
            root = reader.readObject();
        }
        final Map<String, String> sources = sources(root, javaPackage, manifest.getFileName().toString());
        final Path directory = outputDirectory.resolve(javaPackage.replace('.', '/'));
        Files.createDirectories(directory);
        final List<Path> files = new ArrayList<>();
        for (final Map.Entry<String, String> source : sources.entrySet()) {
            final Path file = directory.resolve(source.getKey() + ".java");
            Files.writeString(file, source.getValue());
            files.add(file);
        }
        return files;
    }

    /** The source of each custom element's class, by class name. */
    static Map<String, String> sources(final JsonObject manifest, final String javaPackage, final String origin) {
        final Map<String, String> sources = new LinkedHashMap<>();
        final Map<String, String> tags = new HashMap<>();
        final Map<String, String> classes = new HashMap<>();
        for (final JsonObject module : objects(manifest, "modules")) {
            final String path = module.getString("path", "?");
            for (final JsonObject declaration : objects(module, "declarations")) {
                if (!declaration.getBoolean("customElement", false) || !declaration.containsKey("tagName")) continue;
                final String tag = declaration.getString("tagName");
                final String name = declaration.getString("name");
                final String where = name + " in " + path;
                final String sameTag = tags.putIfAbsent(tag, where);
                if (sameTag != null) throw new IllegalArgumentException("<" + tag + "> is declared twice: " + sameTag + ", " + where);
                final String sameName = classes.putIfAbsent(name, where);
                if (sameName != null) throw new IllegalArgumentException("Class " + name + " is declared twice: " + sameName + ", " + where);
                final ElementClass element = new ElementClass(javaPackage, origin, declaration);
                sources.put(element.className, element.source());
            }
        }
        return sources;
    }

    // ---- One element ----

    private static final class ElementClass {

        private final String javaPackage;
        private final String origin;
        private final JsonObject declaration;
        private final String className;
        private final String tag;
        private final StringBuilder body = new StringBuilder();
        private final StringBuilder enums = new StringBuilder();
        private final Map<String, List<String>> enumLiterals = new HashMap<>();
        private final Set<String> signatures = new HashSet<>();
        private boolean usesEvents;
        private boolean usesJson;

        ElementClass(final String javaPackage, final String origin, final JsonObject declaration) {
            this.javaPackage = javaPackage;
            this.origin = origin;
            this.declaration = declaration;
            this.className = "P" + declaration.getString("name");
            this.tag = declaration.getString("tagName");
        }

        String source() {
            final Set<String> fields = new HashSet<>();
            for (final JsonObject member : objects(declaration, "members")) {
                if (!isPublic(member)) continue;
                if ("field".equals(member.getString("kind", ""))) {
                    fields.add(member.getString("name"));
                    if (!member.getBoolean("readonly", false)) field(member);
                } else if ("method".equals(member.getString("kind", ""))) {
                    methodOf(member);
                }
            }
            for (final JsonObject attribute : objects(declaration, "attributes")) {
                if (!fields.contains(attribute.getString("fieldName", ""))) attribute(attribute);
            }
            for (final JsonObject event : objects(declaration, "events")) {
                if (event.containsKey("name")) event(event);
            }
            for (final JsonObject slot : objects(declaration, "slots")) {
                if (!slot.getString("name", "").isEmpty()) slot(slot);
            }
            for (final JsonObject property : objects(declaration, "cssProperties")) {
                cssProperty(property);
            }

            final StringBuilder out = new StringBuilder();
            out.append("/*\n * Generated by ").append(WebComponentGenerator.class.getName()).append(" from ")
                .append(origin).append(": do not edit.\n */\n\n");
            if (!javaPackage.isEmpty()) out.append("package ").append(javaPackage).append(";\n\n");
            if (usesEvents) out.append("import java.util.function.Consumer;\n\n");
            out.append("import com.ponysdk.core.ui.basic.PWebComponent;\n");
            if (usesEvents) out.append("import com.ponysdk.core.ui.eventbus.HandlerRegistration;\n");
            if (usesEvents || usesJson) out.append("\nimport jakarta.json.JsonValue;\n");
            out.append('\n');
            javadoc(out, "", "{@code <" + tag + ">}" + text(declaration).map(d -> ": " + d).orElse(""));
            deprecated(out, "", declaration);
            out.append("public class ").append(className).append(" extends PWebComponent {\n\n");
            out.append("    public static final String TAG = \"").append(tag).append("\";\n\n");
            out.append("    public ").append(className).append("() {\n        super(TAG);\n    }\n");
            out.append(body);
            out.append(enums);
            out.append("}\n");
            return out.toString();
        }

        private void field(final JsonObject member) {
            final String name = member.getString("name");
            final Type type = Type.parse(typeText(member));
            final String javaName = capitalize(camel(name));
            final String javaType = type.java(this, javaName);
            final String q = quote(name);
            final String setter = type.kind == Kind.JSON ? "set" + javaName + "Json" : "set" + javaName;
            final String getter = type.kind == Kind.JSON ? "get" + javaName + "Json" : "get" + javaName;
            final String write = switch (type.kind) {
                case STRING -> "property(" + q + ").set(jsonOf(value));";
                case NUMBER -> "property(" + q + ").set(jsonOf(" + (type.nullable ? "value.doubleValue()" : "value") + "));";
                case BOOLEAN -> "property(" + q + ").set(" + (type.nullable ? "value.booleanValue()" : "value") + ");";
                case ENUM -> "property(" + q + ").set(jsonOf(value.literal));";
                case JSON -> "property(" + q + ").set(value);";
            };
            final boolean reference = type.kind == Kind.STRING || type.kind == Kind.ENUM || type.kind == Kind.JSON || type.nullable;
            final String setterBody = reference
                    ? "        if (value == null) property(" + q + ").remove();\n        else " + write + "\n"
                    : "        " + write + "\n";
            final String setterName = method(member, setter, List.of(javaType), "void", List.of("value"), setterBody, type);

            final String read = switch (type.kind) {
                case STRING -> "stringOf(property(" + q + ").get())";
                case NUMBER -> "numberOf(property(" + q + ").get())";
                case BOOLEAN -> "booleanOf(property(" + q + ").get())";
                case ENUM -> javaType + ".of(stringOf(property(" + q + ").get()))";
                case JSON -> "property(" + q + ").get()";
            };
            final String boxed = switch (type.kind) {
                case NUMBER -> "Double";
                case BOOLEAN -> "Boolean";
                default -> javaType;
            };
            method(getter, List.of(), boxed, List.of(), "        return " + read + ";\n",
                "The value last set by {@link #" + setterName + "}, null when none.", member);
        }

        private void attribute(final JsonObject attribute) {
            final String name = attribute.getString("name");
            final Type type = Type.parse(typeText(attribute));
            final String q = quote(name);
            if (type.kind == Kind.BOOLEAN) {
                method(attribute, "set" + capitalize(camel(name)), List.of("boolean"), "void", List.of("value"),
                    "        if (value) setAttribute(" + q + ", \"\");\n        else removeAttribute(" + q + ");\n", null);
            } else if (type.kind == Kind.ENUM) {
                final String javaType = type.java(this, capitalize(camel(name)));
                method(attribute, "set" + capitalize(camel(name)) + "Attribute", List.of(javaType), "void", List.of("value"),
                    "        if (value == null) removeAttribute(" + q + ");\n        else setAttribute(" + q + ", value.literal);\n", null);
            } else {
                method(attribute, "set" + capitalize(camel(name)) + "Attribute", List.of("String"), "void", List.of("value"),
                    "        if (value == null) removeAttribute(" + q + ");\n        else setAttribute(" + q + ", value);\n", null);
            }
        }

        private void event(final JsonObject event) {
            usesEvents = true;
            final String name = event.getString("name");
            method(event, "on" + capitalize(camel(name)), List.of("Consumer<JsonValue>"), "HandlerRegistration",
                List.of("listener"), "        return addEventListener(" + quote(name) + ", listener);\n", null);
        }

        private void methodOf(final JsonObject member) {
            final String name = member.getString("name");
            final List<JsonObject> parameters = objects(member, "parameters");
            int required = 0;
            for (final JsonObject parameter : parameters) {
                if (!parameter.getBoolean("optional", false) && !parameter.getBoolean("rest", false)) required++;
                else break;
            }
            // One overload per number of trailing optional parameters left out
            for (int count = required; count <= parameters.size(); count++) {
                final List<String> types = new ArrayList<>();
                final List<String> names = new ArrayList<>();
                final StringJoiner args = new StringJoiner(", ");
                for (final JsonObject parameter : parameters.subList(0, count)) {
                    final String parameterName = identifier(camel(parameter.getString("name")));
                    final Type type = Type.parse(parameter.getBoolean("rest", false) ? null : typeText(parameter));
                    // An object or an array as a JsonValue: the terminal parses an array element that is JSON
                    if (type.kind == Kind.JSON) usesJson = true;
                    types.add(type.kind == Kind.JSON ? "JsonValue" : type.java(this, capitalize(camel(parameter.getString("name")))));
                    names.add(parameterName);
                    args.add(type.kind == Kind.ENUM ? parameterName + " == null ? null : " + parameterName + ".literal" : parameterName);
                }
                final String call = count == 0 ? "call(" + quote(name) + ")" : "call(" + quote(name) + ", " + args + ")";
                method(member, identifier(camel(name)), types, "void", names, "        " + call + ";\n", null);
            }
        }

        private void slot(final JsonObject slot) {
            final String name = slot.getString("name");
            method(slot, "slot" + capitalize(camel(name)), List.of(), "SlotPanel", List.of(),
                "        return slot(" + quote(name) + ");\n", null);
        }

        private void cssProperty(final JsonObject property) {
            final String name = property.getString("name");
            final String q = quote(name);
            method(property, "setCss" + capitalize(camel(name)), List.of("String"), "void", List.of("value"),
                "        if (value == null) removeStyleProperty(" + q + ");\n        else setStyleProperty(" + q + ", value);\n",
                null);
        }

        private String method(final JsonObject documented, final String name, final List<String> types,
                              final String returns, final List<String> names, final String code, final Type type) {
            String doc = text(documented).orElse(null);
            if (type != null && type.kind == Kind.JSON) {
                doc = (doc == null ? "" : doc + "\n") + "As JSON text, of type {@code " + escape(type.text) + "}.";
            }
            if ("method".equals(documented.getString("kind", "")) && documented.containsKey("return")) {
                final String returned = typeText(documented.getJsonObject("return"));
                if (returned != null && !"void".equals(returned.trim()) && !returned.trim().startsWith("Promise<void")) {
                    doc = (doc == null ? "" : doc + "\n") + "Its result, of type {@code " + escape(returned) + "}, does not come back.";
                }
            }
            return method(name, types, returns, names, code, doc, documented);
        }

        private String method(final String name, final List<String> types, final String returns,
                              final List<String> names, final String code, final String doc, final JsonObject deprecation) {
            String javaName = name;
            String docText = doc;
            if (INHERITED.contains(signature(javaName, types))) {
                javaName = name + "Property";
                System.err.println("WebComponentGenerator: <" + tag + "> " + className + "." + signature(name, types)
                    + " is PWebComponent's: generated as " + javaName);
                docText = (doc == null ? "" : doc + "\n") + "Named {@code " + javaName + "}: {@code " + name
                    + "} is PWebComponent's.";
            }
            final String signature = signature(javaName, types);
            if (INHERITED.contains(signature) || !signatures.add(signature)) {
                throw new IllegalArgumentException("<" + tag + ">: two members give " + className + "." + signature);
            }
            body.append('\n');
            if (docText != null) javadoc(body, "    ", docText);
            deprecated(body, "    ", deprecation);
            body.append("    public ").append(returns).append(' ').append(javaName).append('(');
            for (int i = 0; i < types.size(); i++) {
                if (i > 0) body.append(", ");
                body.append("final ").append(types.get(i)).append(' ').append(names.get(i));
            }
            body.append(") {\n").append(code).append("    }\n");
            return javaName;
        }

        /** A nested enum of string literals, once per name. */
        String enumOf(final String name, final List<String> literals) {
            final List<String> known = enumLiterals.putIfAbsent(name, literals);
            if (known != null) {
                if (!known.equals(literals)) {
                    throw new IllegalArgumentException("<" + tag + ">: two types named " + name + ": " + known + ", " + literals);
                }
                return name;
            }
            final Set<String> constants = new HashSet<>();
            enums.append("\n    public enum ").append(name).append(" {\n\n");
            for (int i = 0; i < literals.size(); i++) {
                final String constant = constant(literals.get(i));
                if (!constants.add(constant)) {
                    throw new IllegalArgumentException("<" + tag + ">: two literals of " + name + " give " + constant);
                }
                enums.append("        ").append(constant).append('(').append(quote(literals.get(i))).append(')')
                    .append(i < literals.size() - 1 ? ",\n" : ";\n");
            }
            enums.append("\n        public final String literal;\n\n");
            enums.append("        ").append(name).append("(final String literal) {\n            this.literal = literal;\n        }\n\n");
            enums.append("        /** The constant of a literal, null for none. */\n");
            enums.append("        public static ").append(name).append(" of(final String literal) {\n");
            enums.append("            for (final ").append(name).append(" value : values()) {\n");
            enums.append("                if (value.literal.equals(literal)) return value;\n            }\n");
            enums.append("            return null;\n        }\n    }\n");
            return name;
        }
    }

    // ---- Types ----

    private enum Kind {
        STRING, NUMBER, BOOLEAN, ENUM, JSON
    }

    private record Type(Kind kind, boolean nullable, List<String> literals, String text) {

        static Type parse(final String text) {
            if (text == null || text.isBlank()) return new Type(Kind.JSON, true, List.of(), "unknown");
            final List<String> parts = new ArrayList<>();
            boolean nullable = false;
            for (final String part : splitUnion(text)) {
                if ("undefined".equals(part) || "null".equals(part)) nullable = true;
                else parts.add(part);
            }
            if (parts.isEmpty()) return new Type(Kind.JSON, true, List.of(), text);
            final List<String> literals = new ArrayList<>();
            for (final String part : parts) {
                if (part.length() >= 2 && (part.charAt(0) == '\'' || part.charAt(0) == '"')
                        && part.charAt(part.length() - 1) == part.charAt(0)) {
                    literals.add(part.substring(1, part.length() - 1).replaceAll("\\\\(.)", "$1"));
                }
            }
            if (literals.size() == parts.size()) return new Type(Kind.ENUM, nullable, literals, text);
            if (parts.size() == 1) {
                switch (parts.get(0)) {
                    case "string":
                        return new Type(Kind.STRING, nullable, List.of(), text);
                    case "number":
                        return new Type(Kind.NUMBER, nullable, List.of(), text);
                    case "boolean":
                        return new Type(Kind.BOOLEAN, nullable, List.of(), text);
                    default:
                }
            }
            return new Type(Kind.JSON, nullable, List.of(), text);
        }

        /** The Java type; an enum is declared in the element's class under {@code name}. */
        String java(final ElementClass element, final String name) {
            return switch (kind) {
                case STRING, JSON -> "String";
                case NUMBER -> nullable ? "Double" : "double";
                case BOOLEAN -> nullable ? "Boolean" : "boolean";
                case ENUM -> element.enumOf(name, literals);
            };
        }

        /** The members of a union at its top level: not those inside brackets, braces, parentheses or quotes. */
        private static List<String> splitUnion(final String text) {
            final List<String> parts = new ArrayList<>();
            int depth = 0;
            char quote = 0;
            int start = 0;
            for (int i = 0; i < text.length(); i++) {
                final char c = text.charAt(i);
                if (quote != 0) {
                    if (c == '\\') i++;
                    else if (c == quote) quote = 0;
                } else if (c == '\'' || c == '"' || c == '`') {
                    quote = c;
                } else if (c == '<' || c == '{' || c == '(' || c == '[') {
                    depth++;
                } else if (c == '>' || c == '}' || c == ')' || c == ']') {
                    depth--;
                } else if (c == '|' && depth == 0) {
                    parts.add(text.substring(start, i).trim());
                    start = i + 1;
                }
            }
            parts.add(text.substring(start).trim());
            parts.removeIf(String::isEmpty); // a leading '|'
            return parts;
        }
    }

    // ---- Names and text ----

    /** {@code count-changed}, {@code --accent-color}, {@code my_name} give {@code countChanged}, {@code accentColor}, {@code myName}. */
    static String camel(final String name) {
        final StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(upper && !out.isEmpty() ? Character.toUpperCase(c) : c);
                upper = false;
            } else {
                upper = true;
            }
        }
        if (out.isEmpty()) return "_";
        if (Character.isDigit(out.charAt(0))) out.insert(0, '_');
        out.setCharAt(0, Character.toLowerCase(out.charAt(0)));
        return out.toString();
    }

    private static String capitalize(final String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String identifier(final String name) {
        return KEYWORDS.contains(name) ? name + "_" : name;
    }

    /** {@code light}, {@code top-start}, {@code 2x} give {@code LIGHT}, {@code TOP_START}, {@code _2X}. */
    static String constant(final String literal) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            final char c = literal.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(literal.charAt(i - 1))) out.append('_');
                out.append(Character.toUpperCase(c));
            } else if (!out.isEmpty() && out.charAt(out.length() - 1) != '_') {
                out.append('_');
            }
        }
        if (!out.isEmpty() && out.charAt(out.length() - 1) == '_') out.setLength(out.length() - 1);
        if (out.isEmpty()) return "EMPTY";
        if (Character.isDigit(out.charAt(0))) out.insert(0, '_');
        return out.toString();
    }

    private static String quote(final String value) {
        return Json.createValue(value).toString();
    }

    private static String signature(final String name, final List<String> types) {
        final StringJoiner joiner = new StringJoiner(",", name + "(", ")");
        for (final String type : types) joiner.add(type.replaceAll("<.*>", ""));
        return joiner.toString();
    }

    private static Set<String> inheritedSignatures() {
        final Set<String> signatures = new HashSet<>();
        for (Class<?> type = PWebComponent.class; type != null; type = type.getSuperclass()) {
            for (final Method method : type.getDeclaredMethods()) {
                if (Modifier.isPrivate(method.getModifiers())) continue;
                final List<String> types = new ArrayList<>();
                for (final Class<?> parameter : method.getParameterTypes()) types.add(parameter.getSimpleName());
                signatures.add(signature(method.getName(), types));
            }
        }
        return signatures;
    }

    private static boolean isPublic(final JsonObject member) {
        return "public".equals(member.getString("privacy", "public")) && !member.getBoolean("static", false)
                && !member.getString("name", "#").startsWith("#");
    }

    private static String typeText(final JsonObject typed) {
        final JsonValue type = typed.get("type");
        return type instanceof JsonObject object && object.get("text") instanceof JsonString text ? text.getString() : null;
    }

    private static java.util.Optional<String> text(final JsonObject documented) {
        final String description = documented.getString("description", documented.getString("summary", ""));
        return description.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(escape(description.trim()));
    }

    private static void javadoc(final StringBuilder out, final String indent, final String text) {
        out.append(indent).append("/**\n");
        for (final String line : text.split("\n")) {
            out.append(indent).append(" *").append(line.isBlank() ? "" : " " + line.stripTrailing()).append('\n');
        }
        out.append(indent).append(" */\n");
    }

    private static void deprecated(final StringBuilder out, final String indent, final JsonObject documented) {
        final JsonValue deprecated = documented.get("deprecated");
        if (deprecated == JsonValue.TRUE || deprecated instanceof JsonString) out.append(indent).append("@Deprecated\n");
    }

    /** Text safe in a Javadoc comment. */
    private static String escape(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("*/", "*&#47;")
            .replace("@", "&#64;").replace("\\u", "\\\\u");
    }

    private static List<JsonObject> objects(final JsonObject parent, final String name) {
        final List<JsonObject> out = new ArrayList<>();
        if (parent.get(name) instanceof JsonArray array) {
            for (final JsonValue value : array) if (value instanceof JsonObject object) out.add(object);
        }
        return out;
    }
}
