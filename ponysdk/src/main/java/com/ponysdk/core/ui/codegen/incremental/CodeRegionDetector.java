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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects generated code regions in existing source files.
 * <p>
 * Generated code regions are marked with special comments:
 * <pre>
 * // ========== GENERATED XXX ==========
 * ... generated code ...
 * // ========== END GENERATED CODE ==========
 * </pre>
 * </p>
 */
public class CodeRegionDetector {

    private static final Pattern REGION_START = Pattern.compile("//\\s*=+\\s*GENERATED\\s+(.+?)\\s*=+");
    private static final Pattern REGION_END = Pattern.compile("//\\s*=+\\s*END GENERATED CODE\\s*=+");

    /**
     * Represents a code region in a source file.
     */
    public record CodeRegion(
        String type,        // e.g., "EVENT HANDLERS", "SLOT METHODS"
        int startLine,      // Line number where region starts (inclusive)
        int endLine,        // Line number where region ends (inclusive)
        String content      // The content between markers
    ) {}

    /**
     * Represents the structure of a source file with generated regions.
     */
    public record FileStructure(
        String beforeGenerated,     // Code before first generated region
        List<CodeRegion> regions,   // All generated regions
        String afterGenerated       // Code after last generated region
    ) {}

    /**
     * Parses a source file and extracts generated code regions.
     *
     * @param sourceCode the source code to parse
     * @return the file structure with detected regions
     */
    public FileStructure detectRegions(final String sourceCode) {
        final String[] lines = sourceCode.split("\n", -1);
        final List<CodeRegion> regions = new ArrayList<>();
        
        int currentLine = 0;
        final StringBuilder beforeGenerated = new StringBuilder();
        String afterGenerated = "";
        
        while (currentLine < lines.length) {
            final String line = lines[currentLine];
            final Matcher startMatcher = REGION_START.matcher(line);
            
            if (startMatcher.find()) {
                // Found start of generated region
                final String regionType = startMatcher.group(1);
                final int regionStartLine = currentLine;
                
                // Find end of region
                int regionEndLine = -1;
                final StringBuilder regionContent = new StringBuilder();
                currentLine++; // Move past start marker
                
                while (currentLine < lines.length) {
                    final String regionLine = lines[currentLine];
                    final Matcher endMatcher = REGION_END.matcher(regionLine);
                    
                    if (endMatcher.find()) {
                        regionEndLine = currentLine;
                        break;
                    }
                    
                    regionContent.append(regionLine).append("\n");
                    currentLine++;
                }
                
                if (regionEndLine != -1) {
                    regions.add(new CodeRegion(
                        regionType,
                        regionStartLine,
                        regionEndLine,
                        regionContent.toString()
                    ));
                    currentLine++; // Move past end marker
                } else {
                    // Malformed region - no end marker found
                    throw new IllegalStateException(
                        "Generated region '" + regionType + "' at line " + regionStartLine + 
                        " has no end marker"
                    );
                }
            } else {
                // Not in a generated region
                if (regions.isEmpty()) {
                    // Still in "before generated" section
                    beforeGenerated.append(line).append("\n");
                } else {
                    // In "after generated" section
                    final StringBuilder after = new StringBuilder();
                    while (currentLine < lines.length) {
                        after.append(lines[currentLine]).append("\n");
                        currentLine++;
                    }
                    afterGenerated = after.toString();
                    break;
                }
                currentLine++;
            }
        }
        
        return new FileStructure(
            beforeGenerated.toString(),
            regions,
            afterGenerated
        );
    }

    /**
     * Checks if a source file contains any generated code regions.
     *
     * @param sourceCode the source code to check
     * @return true if file contains generated regions
     */
    public boolean hasGeneratedRegions(final String sourceCode) {
        return REGION_START.matcher(sourceCode).find();
    }
}
