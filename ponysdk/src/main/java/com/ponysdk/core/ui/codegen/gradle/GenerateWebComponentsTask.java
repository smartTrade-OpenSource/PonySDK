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

package com.ponysdk.core.ui.codegen.gradle;

import com.ponysdk.core.ui.codegen.config.ConfigurationManager;
import com.ponysdk.core.ui.codegen.config.DefaultConfigurationManager;
import com.ponysdk.core.ui.codegen.model.GenerationReport;
import com.ponysdk.core.ui.codegen.model.GeneratorConfiguration;
import com.ponysdk.core.ui.codegen.model.LibraryConfig;
import com.ponysdk.core.ui.codegen.model.LibraryReport;
import com.ponysdk.core.ui.codegen.pipeline.CodeGenerationPipeline;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Gradle task for generating web component wrappers from Custom Elements Manifest files.
 * <p>
 * This task reads a configuration file, parses CEM files, and generates Java wrapper classes,
 * props records, and TypeScript interfaces for web components.
 * </p>
 * <p>
 * Example usage in build.gradle:
 * <pre>
 * task generateWebComponents(type: GenerateWebComponentsTask) {
 *     configFile = file('webcomponent-config.json')
 *     javaOutputDir = file('src/main/java')
 *     typescriptOutputDir = file('src/main/typescript')
 * }
 * </pre>
 * </p>
 */
public abstract class GenerateWebComponentsTask extends DefaultTask {

    /**
     * The configuration file containing library definitions and generation settings.
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getConfigFile();

    /**
     * The CEM files to track for incremental builds.
     * This is populated automatically from the configuration.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    public FileCollection getCemFiles() {
        try {
            final Path configPath = getConfigFile().get().getAsFile().toPath();
            final ConfigurationManager configManager = new DefaultConfigurationManager();
            final GeneratorConfiguration config = configManager.parse(configPath);
            
            final Set<File> cemFiles = new HashSet<>();
            for (final LibraryConfig library : config.libraries()) {
                cemFiles.add(library.cemPath().toFile());
            }
            
            return getProject().files(cemFiles);
        } catch (final Exception e) {
            // If we can't parse config, return empty collection
            return getProject().files();
        }
    }

    /**
     * The output directory for generated Java source files.
     */
    @OutputDirectory
    public abstract DirectoryProperty getJavaOutputDir();

    /**
     * The output directory for generated TypeScript files.
     */
    @OutputDirectory
    public abstract DirectoryProperty getTypescriptOutputDir();

    /**
     * Executes the code generation task.
     * <p>
     * This method:
     * <ol>
     *   <li>Loads the configuration from the config file</li>
     *   <li>Creates a code generation pipeline</li>
     *   <li>Executes the pipeline to generate code</li>
     *   <li>Logs the generation report</li>
     *   <li>Fails the build if errors occurred</li>
     * </ol>
     * </p>
     *
     * @throws GradleException if generation fails
     */
    @TaskAction
    public void generate() {
        try {
            // Load configuration
            final Path configPath = getConfigFile().get().getAsFile().toPath();
            getLogger().lifecycle("Loading configuration from: {}", configPath);
            
            final ConfigurationManager configManager = new DefaultConfigurationManager();
            final GeneratorConfiguration config = configManager.parse(configPath);

            // Create pipeline
            final CodeGenerationPipeline pipeline = new CodeGenerationPipeline(config);

            // Execute generation
            getLogger().lifecycle("Generating web component wrappers...");
            final GenerationReport report = pipeline.execute();

            // Log results
            logReport(report);

            // Fail build if errors
            if (!report.errors().isEmpty()) {
                throw new GradleException("Web component generation failed with " +
                    report.errors().size() + " error(s). See log for details.");
            }

        } catch (final GradleException e) {
            throw e;
        } catch (final Exception e) {
            throw new GradleException("Failed to generate web components", e);
        }
    }

    /**
     * Logs the generation report to the Gradle logger.
     *
     * @param report the generation report
     */
    private void logReport(final GenerationReport report) {
        getLogger().lifecycle("Generated {} components in {} ms",
            report.getTotalComponents(),
            report.duration().toMillis());

        for (final LibraryReport lib : report.libraries()) {
            getLogger().lifecycle("  {}: {} components, {} properties, {} events, {} methods",
                lib.libraryName(),
                lib.componentCount(),
                lib.propertyCount(),
                lib.eventCount(),
                lib.methodCount());
            
            if (!lib.skippedComponents().isEmpty()) {
                getLogger().warn("    Skipped {} components", lib.skippedComponents().size());
            }
        }

        if (!report.warnings().isEmpty()) {
            getLogger().warn("Warnings ({}):", report.warnings().size());
            for (final String warning : report.warnings()) {
                getLogger().warn("  - {}", warning);
            }
        }

        if (!report.errors().isEmpty()) {
            getLogger().error("Errors ({}):", report.errors().size());
            for (final String error : report.errors()) {
                getLogger().error("  - {}", error);
            }
        }
    }
}
