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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin for generating web component wrappers.
 * <p>
 * This plugin registers the {@link GenerateWebComponentsTask} and wires it into
 * the build lifecycle to run before Java compilation.
 * </p>
 * <p>
 * Example usage in build.gradle:
 * <pre>
 * plugins {
 *     id 'java'
 *     id 'com.ponysdk.webcomponent-generator'
 * }
 *
 * webComponentGenerator {
 *     configFile = file('webcomponent-config.json')
 *     javaOutputDir = file('build/generated/sources/webcomponents/java')
 *     typescriptOutputDir = file('build/generated/sources/webcomponents/ts')
 * }
 * </pre>
 * </p>
 */
public class WebComponentGeneratorPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        // Ensure Java plugin is applied
        project.getPlugins().apply(JavaPlugin.class);

        // Create extension
        final WebComponentGeneratorExtension extension = project.getExtensions()
            .create("webComponentGenerator", WebComponentGeneratorExtension.class, project);

        // Register task
        final TaskProvider<GenerateWebComponentsTask> generateTask = project.getTasks()
            .register("generateWebComponents", GenerateWebComponentsTask.class, task -> {
                task.setGroup("code generation");
                task.setDescription("Generates Java and TypeScript wrappers from Custom Elements Manifest files");
                
                // Wire extension properties to task
                task.getConfigFile().set(extension.getConfigFile());
                task.getJavaOutputDir().set(extension.getJavaOutputDir());
                task.getTypescriptOutputDir().set(extension.getTypescriptOutputDir());
            });

        // Wire task into build lifecycle - run before compileJava
        project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME).configure(compileJava -> {
            compileJava.dependsOn(generateTask);
        });

        // Add generated sources to Java source set
        project.afterEvaluate(p -> {
            project.getConvention().getPlugin(org.gradle.api.plugins.JavaPluginConvention.class)
                .getSourceSets()
                .getByName("main")
                .getJava()
                .srcDir(extension.getJavaOutputDir());
        });
    }
}
