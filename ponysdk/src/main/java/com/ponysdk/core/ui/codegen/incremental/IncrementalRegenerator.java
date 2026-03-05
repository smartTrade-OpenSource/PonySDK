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

package com.ponysdk.core.ui.codegen.incremental;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles incremental regeneration of code while preserving manual customizations.
 * <p>
 * This class replaces only the generated code regions while keeping manual code intact.
 * </p>
 */
public class IncrementalRegenerator {

    private final CodeRegionDetector detector;

    public IncrementalRegenerator() {
        this.detector = new CodeRegionDetector();
    }

    /**
     * Represents a conflict between manual and generated code.
     */
    public record Conflict(
        String description,
        int lineNumber,
        String manualCode,
        String generatedCode
    ) {}

    /**
     * Result of incremental regeneration.
     */
    public record RegenerationResult(
        String updatedCode,
        List<Conflict> conflicts,
        boolean hasConflicts
    ) {
        public RegenerationResult(final String updatedCode, final List<Conflict> conflicts) {
            this(updatedCode, conflicts, !conflicts.isEmpty());
        }
    }

    /**
     * Regenerates code while preserving manual customizations.
     *
     * @param existingCode the existing source code
     * @param newGeneratedCode the newly generated code
     * @return the regeneration result
     */
    public RegenerationResult regenerate(final String existingCode, final String newGeneratedCode) {
        // Check if existing code has generated regions
        if (!detector.hasGeneratedRegions(existingCode)) {
            // No generated regions - this is a new file or fully manual
            // Return the new generated code as-is
            return new RegenerationResult(newGeneratedCode, List.of());
        }

        // Parse existing file structure
        final CodeRegionDetector.FileStructure existingStructure = detector.detectRegions(existingCode);
        
        // Parse new generated code structure
        final CodeRegionDetector.FileStructure newStructure = detector.detectRegions(newGeneratedCode);
        
        // Detect conflicts
        final List<Conflict> conflicts = detectConflicts(existingStructure, newStructure);
        
        // Merge: keep manual code, replace generated regions
        final String mergedCode = mergeCode(existingStructure, newStructure);
        
        return new RegenerationResult(mergedCode, conflicts);
    }

    /**
     * Detects conflicts between existing and new code.
     *
     * @param existing the existing file structure
     * @param newCode the new file structure
     * @return list of conflicts
     */
    private List<Conflict> detectConflicts(
        final CodeRegionDetector.FileStructure existing,
        final CodeRegionDetector.FileStructure newCode
    ) {
        final List<Conflict> conflicts = new ArrayList<>();
        
        // Check if manual code in "after generated" section conflicts with new generated regions
        // This is a simplified conflict detection - in practice, you'd want more sophisticated analysis
        
        // For now, we just warn if there's substantial manual code after generated regions
        if (!existing.afterGenerated().trim().isEmpty() && 
            !existing.afterGenerated().trim().equals("// Manual extensions can be added below this line")) {
            
            // Check if new code has different structure
            if (newCode.regions().size() != existing.regions().size()) {
                conflicts.add(new Conflict(
                    "Number of generated regions changed - manual code may need adjustment",
                    -1,
                    existing.afterGenerated(),
                    newCode.afterGenerated()
                ));
            }
        }
        
        return conflicts;
    }

    /**
     * Merges existing manual code with new generated code.
     *
     * @param existing the existing file structure
     * @param newCode the new file structure
     * @return the merged code
     */
    private String mergeCode(
        final CodeRegionDetector.FileStructure existing,
        final CodeRegionDetector.FileStructure newCode
    ) {
        final StringBuilder merged = new StringBuilder();
        
        // Use the new "before generated" section (package, imports, class declaration)
        // But preserve any manual modifications if they exist
        merged.append(newCode.beforeGenerated());
        
        // Add new generated regions
        for (final CodeRegionDetector.CodeRegion region : newCode.regions()) {
            merged.append("    // ========== GENERATED ").append(region.type()).append(" ==========\n\n");
            merged.append(region.content());
            merged.append("    // ========== END GENERATED CODE ==========\n");
        }
        
        // Preserve manual code from "after generated" section
        if (!existing.afterGenerated().trim().isEmpty()) {
            merged.append(existing.afterGenerated());
        } else {
            // Use new "after generated" section if no manual code exists
            merged.append(newCode.afterGenerated());
        }
        
        return merged.toString();
    }
}
