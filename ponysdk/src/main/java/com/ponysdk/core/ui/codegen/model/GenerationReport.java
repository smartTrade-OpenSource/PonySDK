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

package com.ponysdk.core.ui.codegen.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Report of code generation results across all libraries.
 *
 * @param timestamp when the generation started
 * @param duration  how long the generation took
 * @param libraries per-library generation reports
 * @param warnings  list of warning messages
 * @param errors    list of error messages
 */
public record GenerationReport(
    Instant timestamp,
    Duration duration,
    List<LibraryReport> libraries,
    List<String> warnings,
    List<String> errors
) {
    /**
     * Returns the total number of components generated across all libraries.
     *
     * @return total component count
     */
    public int getTotalComponents() {
        return libraries.stream()
            .mapToInt(LibraryReport::componentCount)
            .sum();
    }

    /**
     * Returns the total number of properties processed across all libraries.
     *
     * @return total property count
     */
    public int getTotalProperties() {
        return libraries.stream()
            .mapToInt(LibraryReport::propertyCount)
            .sum();
    }

    /**
     * Returns the total number of events processed across all libraries.
     *
     * @return total event count
     */
    public int getTotalEvents() {
        return libraries.stream()
            .mapToInt(LibraryReport::eventCount)
            .sum();
    }
}
