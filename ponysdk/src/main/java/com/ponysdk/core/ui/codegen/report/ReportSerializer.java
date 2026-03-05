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

package com.ponysdk.core.ui.codegen.report;

import com.ponysdk.core.ui.codegen.model.GenerationReport;
import com.ponysdk.core.ui.codegen.model.LibraryReport;
import com.ponysdk.core.ui.codegen.model.TypeMappingWarning;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializes generation reports to JSON format.
 */
public class ReportSerializer {

    /**
     * Serializes a generation report to JSON and writes it to a file.
     *
     * @param report the generation report
     * @param outputPath the path to write the JSON file
     * @throws IOException if writing fails
     */
    public void serializeToFile(final GenerationReport report, final Path outputPath) throws IOException {
        final String json = serializeToJson(report);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, json);
    }

    /**
     * Serializes a generation report to a JSON string.
     *
     * @param report the generation report
     * @return JSON string representation
     */
    public String serializeToJson(final GenerationReport report) {
        final JsonObject jsonObject = buildJsonObject(report);
        
        // Pretty print the JSON
        final Map<String, Object> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        final JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        
        final StringWriter stringWriter = new StringWriter();
        try (final JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        
        return stringWriter.toString();
    }

    /**
     * Outputs a generation report to the console.
     *
     * @param report the generation report
     */
    public void printToConsole(final GenerationReport report) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Web Component Generation Report");
        System.out.println("=".repeat(80));
        System.out.println();
        
        System.out.println("Timestamp: " + report.timestamp());
        System.out.println("Duration: " + report.duration().toMillis() + " ms");
        System.out.println();
        
        System.out.println("Summary:");
        System.out.println("  Total Components: " + report.getTotalComponents());
        System.out.println("  Total Properties: " + report.getTotalProperties());
        System.out.println("  Total Events: " + report.getTotalEvents());
        System.out.println();
        
        if (!report.libraries().isEmpty()) {
            System.out.println("Libraries:");
            for (final LibraryReport lib : report.libraries()) {
                System.out.println("  " + lib.libraryName() + ":");
                System.out.println("    Components: " + lib.componentCount());
                System.out.println("    Properties: " + lib.propertyCount());
                System.out.println("    Events: " + lib.eventCount());
                System.out.println("    Methods: " + lib.methodCount());
                System.out.println("    Generated Files: " + lib.generatedFiles().size());
                
                if (!lib.skippedComponents().isEmpty()) {
                    System.out.println("    Skipped Components: " + lib.skippedComponents().size());
                }
                
                if (!lib.typeMappingWarnings().isEmpty()) {
                    System.out.println("    Type Mapping Warnings: " + lib.typeMappingWarnings().size());
                }
            }
            System.out.println();
        }
        
        if (!report.warnings().isEmpty()) {
            System.out.println("Warnings (" + report.warnings().size() + "):");
            for (final String warning : report.warnings()) {
                System.out.println("  - " + warning);
            }
            System.out.println();
        }
        
        if (!report.errors().isEmpty()) {
            System.out.println("Errors (" + report.errors().size() + "):");
            for (final String error : report.errors()) {
                System.out.println("  - " + error);
            }
            System.out.println();
        }
        
        if (report.errors().isEmpty()) {
            System.out.println("✓ Generation completed successfully!");
        } else {
            System.out.println("✗ Generation completed with errors.");
        }
        
        System.out.println("=".repeat(80));
    }

    /**
     * Builds a JSON object from a generation report.
     *
     * @param report the generation report
     * @return JSON object
     */
    private JsonObject buildJsonObject(final GenerationReport report) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        
        builder.add("timestamp", report.timestamp().toString());
        builder.add("durationMs", report.duration().toMillis());
        
        // Summary
        final JsonObjectBuilder summary = Json.createObjectBuilder()
            .add("totalComponents", report.getTotalComponents())
            .add("totalProperties", report.getTotalProperties())
            .add("totalEvents", report.getTotalEvents());
        builder.add("summary", summary);
        
        // Libraries
        final JsonArrayBuilder libraries = Json.createArrayBuilder();
        for (final LibraryReport lib : report.libraries()) {
            libraries.add(buildLibraryJson(lib));
        }
        builder.add("libraries", libraries);
        
        // Warnings
        final JsonArrayBuilder warnings = Json.createArrayBuilder();
        for (final String warning : report.warnings()) {
            warnings.add(warning);
        }
        builder.add("warnings", warnings);
        
        // Errors
        final JsonArrayBuilder errors = Json.createArrayBuilder();
        for (final String error : report.errors()) {
            errors.add(error);
        }
        builder.add("errors", errors);
        
        return builder.build();
    }

    /**
     * Builds a JSON object from a library report.
     *
     * @param lib the library report
     * @return JSON object
     */
    private JsonObject buildLibraryJson(final LibraryReport lib) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        
        builder.add("name", lib.libraryName());
        builder.add("componentCount", lib.componentCount());
        builder.add("propertyCount", lib.propertyCount());
        builder.add("eventCount", lib.eventCount());
        builder.add("methodCount", lib.methodCount());
        
        // Generated files
        final JsonArrayBuilder files = Json.createArrayBuilder();
        for (final String file : lib.generatedFiles()) {
            files.add(file);
        }
        builder.add("generatedFiles", files);
        
        // Skipped components
        final JsonArrayBuilder skipped = Json.createArrayBuilder();
        for (final String component : lib.skippedComponents()) {
            skipped.add(component);
        }
        builder.add("skippedComponents", skipped);
        
        // Type mapping warnings
        final JsonArrayBuilder warnings = Json.createArrayBuilder();
        for (final TypeMappingWarning warning : lib.typeMappingWarnings()) {
            final JsonObjectBuilder warningObj = Json.createObjectBuilder()
                .add("componentName", warning.componentName())
                .add("propertyName", warning.propertyName())
                .add("cemType", warning.cemType())
                .add("fallbackType", warning.fallbackType())
                .add("reason", warning.reason());
            warnings.add(warningObj);
        }
        builder.add("typeMappingWarnings", warnings);
        
        return builder.build();
    }
}
