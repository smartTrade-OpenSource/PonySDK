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

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;

import javax.inject.Inject;

/**
 * Extension for configuring the web component generator plugin.
 * <p>
 * This extension allows users to configure the code generation task through
 * the build.gradle file.
 * </p>
 */
public abstract class WebComponentGeneratorExtension {

    private final Project project;

    @Inject
    public WebComponentGeneratorExtension(final Project project) {
        this.project = project;
        
        // Set default values
        getJavaOutputDir().convention(
            project.getLayout().getBuildDirectory().dir("generated/sources/webcomponents/java")
        );
        getTypescriptOutputDir().convention(
            project.getLayout().getBuildDirectory().dir("generated/sources/webcomponents/ts")
        );
    }

    /**
     * The configuration file containing library definitions and generation settings.
     */
    public abstract RegularFileProperty getConfigFile();

    /**
     * The output directory for generated Java source files.
     */
    public abstract DirectoryProperty getJavaOutputDir();

    /**
     * The output directory for generated TypeScript files.
     */
    public abstract DirectoryProperty getTypescriptOutputDir();
}
