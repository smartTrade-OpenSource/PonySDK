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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IncrementalRegenerator}.
 * Tests preservation of manual code, conflict detection, and handling of removed components.
 * 
 * Validates Requirements 11.2, 11.3, 11.4, 11.6
 */
class IncrementalRegeneratorTest {

    private IncrementalRegenerator regenerator;

    @BeforeEach
    void setUp() {
        regenerator = new IncrementalRegenerator();
    }

    // ========== Tests for Preservation of Manual Code (Requirement 11.2) ==========

    @Test
    void regenerate_preservesManualCodeBeforeGeneratedRegion() {
        final String existingCode = """
            package com.example;
            
            import java.util.List;
            
            // Custom import added manually
            import com.example.custom.Helper;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            import java.util.List;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Manual import should be preserved
        assertTrue(result.updatedCode().contains("import com.example.custom.Helper;"));
    }

    @Test
    void regenerate_preservesManualCodeAfterGeneratedRegion() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // Manual extension method
                public void customMethod() {
                    // Custom logic here
                }
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Manual method should be preserved
        assertTrue(result.updatedCode().contains("// Manual extension method"));
        assertTrue(result.updatedCode().contains("public void customMethod()"));
        assertTrue(result.updatedCode().contains("// Custom logic here"));
    }

    @Test
    void regenerate_preservesComplexManualCode() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // Manual field
                private final Logger logger = LoggerFactory.getLogger(PButton.class);
                
                // Manual constructor
                public PButton(ButtonProps props, boolean debug) {
                    super(props);
                    if (debug) {
                        logger.info("Debug mode enabled");
                    }
                }
                
                // Manual helper method
                private void logEvent(String eventName) {
                    logger.debug("Event fired: {}", eventName);
                }
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // All manual code should be preserved
        assertTrue(result.updatedCode().contains("private final Logger logger"));
        assertTrue(result.updatedCode().contains("public PButton(ButtonProps props, boolean debug)"));
        assertTrue(result.updatedCode().contains("private void logEvent(String eventName)"));
    }

    @Test
    void regenerate_preservesManualCodeWithMultipleRegions() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // ========== GENERATED SLOT METHODS ==========
                public void addPrefix() {}
                // ========== END GENERATED CODE ==========
                
                // Manual code after all generated regions
                public void customMethod() {
                    // Custom implementation
                }
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
                
                // ========== GENERATED SLOT METHODS ==========
                public void addPrefix() {}
                public void addSuffix() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Manual code should be preserved
        assertTrue(result.updatedCode().contains("public void customMethod()"));
        assertTrue(result.updatedCode().contains("// Custom implementation"));
    }

    // ========== Tests for Updating Generated Regions (Requirement 11.3) ==========

    @Test
    void regenerate_updatesGeneratedRegionContent() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {
                    onEvent("click", handler);
                }
                // ========== END GENERATED CODE ==========
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {
                    onEvent("click", handler);
                }
                
                public void addFocusListener() {
                    onEvent("focus", handler);
                }
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // New generated method should be present
        assertTrue(result.updatedCode().contains("public void addFocusListener()"));
        assertTrue(result.updatedCode().contains("onEvent(\"focus\", handler)"));
    }

    @Test
    void regenerate_replacesOldGeneratedCodeWithNew() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addOldListener() {
                    // Old implementation
                }
                // ========== END GENERATED CODE ==========
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addNewListener() {
                    // New implementation
                }
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Old generated code should be removed
        assertFalse(result.updatedCode().contains("addOldListener"));
        
        // New generated code should be present
        assertTrue(result.updatedCode().contains("addNewListener"));
    }

    @Test
    void regenerate_updatesMultipleGeneratedRegions() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // ========== GENERATED SLOT METHODS ==========
                public void addPrefix() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
                
                // ========== GENERATED SLOT METHODS ==========
                public void addPrefix() {}
                public void addSuffix() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Both regions should be updated
        assertTrue(result.updatedCode().contains("addFocusListener"));
        assertTrue(result.updatedCode().contains("addSuffix"));
    }

    @Test
    void regenerate_maintainsRegionBoundaries() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Region markers should be present
        assertTrue(result.updatedCode().contains("// ========== GENERATED EVENT HANDLERS =========="));
        assertTrue(result.updatedCode().contains("// ========== END GENERATED CODE =========="));
    }

    // ========== Tests for Conflict Detection (Requirement 11.4) ==========

    @Test
    void regenerate_detectsConflictWhenRegionCountChanges() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // Manual code that might be affected
                public void customMethod() {}
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // ========== GENERATED SLOT METHODS ==========
                public void addPrefix() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Should detect conflict due to region count change
        assertTrue(result.hasConflicts());
        assertFalse(result.conflicts().isEmpty());
    }

    @Test
    void regenerate_noConflictWhenOnlyGeneratedCodeChanges() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // No conflicts expected when only generated code changes
        assertFalse(result.hasConflicts());
        assertTrue(result.conflicts().isEmpty());
    }

    @Test
    void regenerate_conflictIncludesDescription() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                public void customMethod() {}
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // ========== GENERATED SLOT METHODS ==========
                public void addPrefix() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        assertTrue(result.hasConflicts());
        
        final IncrementalRegenerator.Conflict conflict = result.conflicts().get(0);
        assertNotNull(conflict.description());
        assertTrue(conflict.description().contains("Number of generated regions changed"));
    }

    // ========== Tests for Handling Removed Components (Requirement 11.6) ==========

    @Test
    void regenerate_returnsNewCodeForFileWithoutGeneratedRegions() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                public void customMethod() {
                    // All manual code, no generated regions
                }
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Should return new generated code since existing has no regions
        assertTrue(result.updatedCode().contains("// ========== GENERATED EVENT HANDLERS =========="));
        assertTrue(result.updatedCode().contains("addClickListener"));
        assertFalse(result.hasConflicts());
    }

    @Test
    void regenerate_handlesEmptyAfterGeneratedSection() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
                
                // Manual extensions can be added below this line
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Should use new "after generated" section when existing is empty
        assertTrue(result.updatedCode().contains("// Manual extensions can be added below this line"));
        assertFalse(result.hasConflicts());
    }

    // ========== Integration Tests ==========

    @Test
    void regenerate_completeWorkflow() {
        final String existingCode = """
            package com.ponysdk.core.ui.webawesome;
            
            import com.ponysdk.core.ui.component.PWebComponent;
            import java.util.function.Consumer;
            import javax.json.JsonObject;
            
            // Custom import
            import org.slf4j.Logger;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener(final Consumer<JsonObject> handler) {
                    onEvent("wa-click", handler);
                }
                // ========== END GENERATED CODE ==========
                
                // Manual field
                private static final Logger LOG = LoggerFactory.getLogger(PButton.class);
                
                // Manual method
                public void logClick() {
                    LOG.info("Button clicked");
                }
            }
            """;

        final String newGeneratedCode = """
            package com.ponysdk.core.ui.webawesome;
            
            import com.ponysdk.core.ui.component.PWebComponent;
            import java.util.function.Consumer;
            import javax.json.JsonObject;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener(final Consumer<JsonObject> handler) {
                    onEvent("wa-click", handler);
                }
                
                public void addFocusListener(final Consumer<JsonObject> handler) {
                    onEvent("wa-focus", handler);
                }
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Verify manual code is preserved
        assertTrue(result.updatedCode().contains("import org.slf4j.Logger"));
        assertTrue(result.updatedCode().contains("private static final Logger LOG"));
        assertTrue(result.updatedCode().contains("public void logClick()"));
        
        // Verify generated code is updated
        assertTrue(result.updatedCode().contains("addFocusListener"));
        
        // No conflicts expected
        assertFalse(result.hasConflicts());
    }

    @Test
    void regenerate_preservesIndentationInManualCode() {
        final String existingCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                public void customMethod() {
                    if (condition) {
                        doSomething();
                    }
                }
            }
            """;

        final String newGeneratedCode = """
            package com.example;
            
            public class PButton extends PWebComponent<ButtonProps> {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                public void addFocusListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final IncrementalRegenerator.RegenerationResult result = regenerator.regenerate(existingCode, newGeneratedCode);

        // Indentation should be preserved
        assertTrue(result.updatedCode().contains("    if (condition) {"));
        assertTrue(result.updatedCode().contains("        doSomething();"));
    }
}
