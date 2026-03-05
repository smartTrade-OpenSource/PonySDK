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

package com.ponysdk.core.ui.codegen.pipeline;

import com.ponysdk.core.ui.codegen.config.ConfigurationManager;
import com.ponysdk.core.ui.codegen.config.DefaultConfigurationManager;
import com.ponysdk.core.ui.codegen.dependency.CircularDependencyException;
import com.ponysdk.core.ui.codegen.dependency.DependencyResolver;
import com.ponysdk.core.ui.codegen.filter.ComponentFilter;
import com.ponysdk.core.ui.codegen.generator.CodeGenerator;
import com.ponysdk.core.ui.codegen.generator.CodeGeneratorImpl;
import com.ponysdk.core.ui.codegen.generator.RegistrationClassGenerator;
import com.ponysdk.core.ui.codegen.incremental.IncrementalRegenerator;
import com.ponysdk.core.ui.codegen.model.*;
import com.ponysdk.core.ui.codegen.parser.CEMParser;
import com.ponysdk.core.ui.codegen.parser.DefaultCEMParser;
import com.ponysdk.core.ui.codegen.report.ReportSerializer;
import com.ponysdk.core.ui.codegen.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the complete code generation pipeline.
 * <p>
 * This class coordinates parsing, type mapping, code generation, validation,
 * and file writing for multiple component libraries.
 * </p>
 */
public class CodeGenerationPipeline {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationPipeline.class);

    private final GeneratorConfiguration config;
    private final CEMParser parser;
    private final DependencyResolver dependencyResolver;
    private final CodeValidator validator;
    private final IncrementalRegenerator regenerator;
    private final ConfigurationManager configManager;
    private final ReportSerializer reportSerializer;

    /**
     * Creates a new code generation pipeline with the specified configuration.
     *
     * @param config the generator configuration
     */
    public CodeGenerationPipeline(final GeneratorConfiguration config) {
        this.config = config;
        this.parser = new DefaultCEMParser();
        this.dependencyResolver = new DependencyResolver();
        this.validator = new CodeValidator();
        this.regenerator = new IncrementalRegenerator();
        this.configManager = new DefaultConfigurationManager();
        this.reportSerializer = new ReportSerializer();
    }

    /**
     * Executes the complete generation pipeline.
     *
     * @return generation report
     */
    public GenerationReport execute() {
        final Instant startTime = Instant.now();
        final List<LibraryReport> libraryReports = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        log.info("Starting code generation pipeline");

        // Process each library
        for (final LibraryConfig library : config.libraries()) {
            log.info("Processing library: {}", library.name());
            
            try {
                final LibraryReport report = processLibrary(library, warnings, errors);
                libraryReports.add(report);
            } catch (final Exception e) {
                log.error("Failed to process library: {}", library.name(), e);
                errors.add("Library " + library.name() + ": " + e.getMessage());
            }
        }

        final Duration duration = Duration.between(startTime, Instant.now());
        log.info("Code generation completed in {} ms", duration.toMillis());

        final GenerationReport report = new GenerationReport(
            startTime,
            duration,
            libraryReports,
            warnings,
            errors
        );

        // Output report to console
        reportSerializer.printToConsole(report);

        // Write structured report file if configured
        if (config.output().generateReports()) {
            try {
                final Path reportPath = config.output().javaOutputDir()
                    .getParent()
                    .resolve("build")
                    .resolve("reports")
                    .resolve("webcomponent-generation.json");
                reportSerializer.serializeToFile(report, reportPath);
                log.info("Generation report written to: {}", reportPath);
            } catch (final IOException e) {
                log.warn("Failed to write generation report: {}", e.getMessage());
            }
        }

        return report;
    }

    /**
     * Processes a single library.
     *
     * @param library the library configuration
     * @param warnings list to collect warnings
     * @param errors list to collect errors
     * @return library report
     */
    private LibraryReport processLibrary(
        final LibraryConfig library,
        final List<String> warnings,
        final List<String> errors
    ) throws Exception {
        
        // Parse CEM file
        log.debug("Parsing CEM file: {}", library.cemPath());
        final List<ComponentDefinition> components = parser.parse(library.cemPath());
        log.info("Parsed {} components from {}", components.size(), library.name());

        // Apply filters
        final ComponentFilter filter = new ComponentFilter(config.filters());
        final List<ComponentDefinition> filteredComponents = filter.filter(components);
        log.info("Filtered to {} components", filteredComponents.size());

        // Resolve dependencies and sort
        log.debug("Resolving dependencies");
        final List<ComponentDefinition> sortedComponents;
        try {
            sortedComponents = dependencyResolver.topologicalSort(filteredComponents);
        } catch (final CircularDependencyException e) {
            errors.add("Circular dependency in " + library.name() + ": " + e.getMessage());
            throw e;
        }

        // Generate code for each component
        final CodeGenerator generator = new CodeGeneratorImpl(library.javaPackage());
        final List<String> generatedFiles = new ArrayList<>();
        final List<String> skippedComponents = new ArrayList<>();
        final List<TypeMappingWarning> typeMappingWarnings = new ArrayList<>();
        
        int propertyCount = 0;
        int eventCount = 0;
        int methodCount = 0;

        for (final ComponentDefinition component : sortedComponents) {
            try {
                // Generate wrapper class
                final String wrapperCode = generator.generateWrapperClass(component);
                final String wrapperClassName = component.getWrapperClassName();
                
                // Validate
                final CodeValidator.ValidationResult validation = validator.validateJava(
                    library.javaPackage() + "." + wrapperClassName,
                    wrapperCode
                );
                
                if (!validation.valid()) {
                    log.warn("Skipping component {} due to validation errors", component.tagName());
                    skippedComponents.add(component.tagName() + " (validation failed)");
                    for (final CodeValidator.ValidationError error : validation.errors()) {
                        errors.add(component.tagName() + ": " + error.message());
                    }
                    continue;
                }

                // Write wrapper class
                final Path wrapperPath = config.output().javaOutputDir()
                    .resolve(library.javaPackage().replace('.', '/'))
                    .resolve(wrapperClassName + ".java");
                writeFile(wrapperPath, wrapperCode);
                generatedFiles.add(wrapperPath.toString());

                // Generate props record
                final String propsCode = generator.generatePropsRecord(component);
                final String propsClassName = component.getPropsClassName();
                
                // Validate props record
                final CodeValidator.ValidationResult propsValidation = validator.validateJava(
                    library.javaPackage() + "." + propsClassName,
                    propsCode
                );
                
                if (!propsValidation.valid()) {
                    log.warn("Skipping component {} due to props validation errors", component.tagName());
                    skippedComponents.add(component.tagName() + " (props validation failed)");
                    for (final CodeValidator.ValidationError error : propsValidation.errors()) {
                        errors.add(component.tagName() + " props: " + error.message());
                    }
                    continue;
                }
                
                final Path propsPath = config.output().javaOutputDir()
                    .resolve(library.javaPackage().replace('.', '/'))
                    .resolve(propsClassName + ".java");
                writeFile(propsPath, propsCode);
                generatedFiles.add(propsPath.toString());

                // Generate TypeScript interface
                final String tsCode = generator.generateTypeScriptInterface(component);
                
                // Validate TypeScript code
                final CodeValidator.ValidationResult tsValidation = validator.validateTypeScript(tsCode);
                
                if (!tsValidation.valid()) {
                    log.warn("Skipping TypeScript generation for component {} due to validation errors", component.tagName());
                    warnings.add(component.tagName() + " TypeScript validation failed");
                    for (final CodeValidator.ValidationError error : tsValidation.errors()) {
                        warnings.add(component.tagName() + " TypeScript: " + error.message());
                    }
                    // Continue without TypeScript file, but keep Java files
                } else {
                    final Path tsPath = config.output().typescriptOutputDir()
                        .resolve(library.name())
                        .resolve(propsClassName + ".ts");
                    writeFile(tsPath, tsCode);
                    generatedFiles.add(tsPath.toString());
                }

                // Generate event detail classes
                final Map<String, String> eventDetailClasses = generator.generateEventDetailClasses(component);
                for (final Map.Entry<String, String> entry : eventDetailClasses.entrySet()) {
                    final Path eventPath = config.output().javaOutputDir()
                        .resolve(library.javaPackage().replace('.', '/'))
                        .resolve("events")
                        .resolve(entry.getKey() + ".java");
                    writeFile(eventPath, entry.getValue());
                    generatedFiles.add(eventPath.toString());
                }

                // Update statistics
                propertyCount += component.properties().size();
                eventCount += component.events().size();
                methodCount += component.methods().size();

                log.debug("Generated code for component: {}", component.tagName());

            } catch (final Exception e) {
                log.warn("Failed to generate component {}: {}", component.tagName(), e.getMessage());
                skippedComponents.add(component.tagName() + " (" + e.getMessage() + ")");
                errors.add(component.tagName() + ": " + e.getMessage());
            }
        }

        // Generate registration class
        final RegistrationClassGenerator registrationGenerator = new RegistrationClassGenerator(
            library.javaPackage(),
            library.name()
        );
        final String registrationCode = registrationGenerator.generateRegistrationClass(sortedComponents);
        final String registrationClassName = capitalize(library.name()) + "ComponentRegistry";
        final Path registrationPath = config.output().javaOutputDir()
            .resolve(library.javaPackage().replace('.', '/'))
            .resolve(registrationClassName + ".java");
        writeFile(registrationPath, registrationCode);
        generatedFiles.add(registrationPath.toString());

        return new LibraryReport(
            library.name(),
            filteredComponents.size(),
            propertyCount,
            eventCount,
            methodCount,
            generatedFiles,
            skippedComponents,
            typeMappingWarnings
        );
    }

    /**
     * Writes content to a file, creating parent directories if needed.
     *
     * @param path the file path
     * @param content the content to write
     * @throws IOException if writing fails
     */
    private void writeFile(final Path path, final String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private static String capitalize(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
