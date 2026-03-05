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
 * Unit tests for {@link CodeRegionDetector}.
 * Tests detection of generated code regions in source files.
 */
class CodeRegionDetectorTest {

    private CodeRegionDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CodeRegionDetector();
    }

    @Test
    void hasGeneratedRegions_returnsFalseForFileWithoutRegions() {
        final String source = """
            package com.example;
            
            public class MyClass {
                public void myMethod() {
                    // Some code
                }
            }
            """;

        assertFalse(detector.hasGeneratedRegions(source));
    }

    @Test
    void hasGeneratedRegions_returnsTrueForFileWithRegions() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        assertTrue(detector.hasGeneratedRegions(source));
    }

    @Test
    void detectRegions_extractsBeforeGeneratedSection() {
        final String source = """
            package com.example;
            
            import java.util.List;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        assertTrue(structure.beforeGenerated().contains("package com.example;"));
        assertTrue(structure.beforeGenerated().contains("import java.util.List;"));
        assertTrue(structure.beforeGenerated().contains("public class MyClass {"));
    }

    @Test
    void detectRegions_extractsAfterGeneratedSection() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // Manual extensions below
                public void customMethod() {
                    // Custom logic
                }
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        assertTrue(structure.afterGenerated().contains("// Manual extensions below"));
        assertTrue(structure.afterGenerated().contains("public void customMethod()"));
    }

    @Test
    void detectRegions_extractsSingleRegion() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {
                    onEvent("click", handler);
                }
                // ========== END GENERATED CODE ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        assertEquals(1, structure.regions().size());
        
        final CodeRegionDetector.CodeRegion region = structure.regions().get(0);
        assertEquals("EVENT HANDLERS", region.type());
        assertTrue(region.content().contains("public void addClickListener()"));
        assertTrue(region.content().contains("onEvent(\"click\", handler);"));
    }

    @Test
    void detectRegions_extractsMultipleRegions() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                
                // ========== GENERATED SLOT METHODS ==========
                public void addPrefix() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        assertEquals(2, structure.regions().size());
        
        assertEquals("EVENT HANDLERS", structure.regions().get(0).type());
        assertTrue(structure.regions().get(0).content().contains("addClickListener"));
        
        assertEquals("SLOT METHODS", structure.regions().get(1).type());
        assertTrue(structure.regions().get(1).content().contains("addPrefix"));
    }

    @Test
    void detectRegions_handlesRegionWithVariousWhitespace() {
        final String source = """
            package com.example;
            
            public class MyClass {
                //  ==========  GENERATED  EVENT HANDLERS  ==========
                public void addClickListener() {}
                //  ==========  END GENERATED CODE  ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        assertEquals(1, structure.regions().size());
        assertEquals("EVENT HANDLERS", structure.regions().get(0).type());
    }

    @Test
    void detectRegions_handlesEmptyRegion() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                // ========== END GENERATED CODE ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        assertEquals(1, structure.regions().size());
        assertEquals("EVENT HANDLERS", structure.regions().get(0).type());
        assertTrue(structure.regions().get(0).content().isEmpty() || 
                   structure.regions().get(0).content().isBlank());
    }

    @Test
    void detectRegions_throwsExceptionForMalformedRegion() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // Missing end marker
            }
            """;

        final IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> detector.detectRegions(source)
        );

        assertTrue(exception.getMessage().contains("EVENT HANDLERS"));
        assertTrue(exception.getMessage().contains("no end marker"));
    }

    @Test
    void detectRegions_preservesLineNumbers() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        final CodeRegionDetector.CodeRegion region = structure.regions().get(0);
        assertEquals(3, region.startLine()); // Line 3 (0-indexed)
        assertEquals(5, region.endLine());   // Line 5 (0-indexed)
    }

    @Test
    void detectRegions_handlesMultilineRegionContent() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {
                    if (handler != null) {
                        onEvent("click", handler);
                    }
                }
                
                public void addFocusListener() {
                    onEvent("focus", handler);
                }
                // ========== END GENERATED CODE ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        final CodeRegionDetector.CodeRegion region = structure.regions().get(0);
        assertTrue(region.content().contains("addClickListener"));
        assertTrue(region.content().contains("addFocusListener"));
        assertTrue(region.content().contains("if (handler != null)"));
    }

    @Test
    void detectRegions_handlesFileWithNoGeneratedRegions() {
        final String source = """
            package com.example;
            
            public class MyClass {
                public void customMethod() {
                    // All manual code
                }
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        assertEquals(0, structure.regions().size());
        assertTrue(structure.beforeGenerated().contains("package com.example;"));
        assertTrue(structure.beforeGenerated().contains("public void customMethod()"));
        assertTrue(structure.afterGenerated().isEmpty());
    }

    @Test
    void detectRegions_handlesConsecutiveRegions() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                public void addClickListener() {}
                // ========== END GENERATED CODE ==========
                // ========== GENERATED SLOT METHODS ==========
                public void addPrefix() {}
                // ========== END GENERATED CODE ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        assertEquals(2, structure.regions().size());
        assertEquals("EVENT HANDLERS", structure.regions().get(0).type());
        assertEquals("SLOT METHODS", structure.regions().get(1).type());
    }

    @Test
    void detectRegions_preservesIndentation() {
        final String source = """
            package com.example;
            
            public class MyClass {
                // ========== GENERATED EVENT HANDLERS ==========
                    public void addClickListener() {
                        onEvent("click", handler);
                    }
                // ========== END GENERATED CODE ==========
            }
            """;

        final CodeRegionDetector.FileStructure structure = detector.detectRegions(source);

        final CodeRegionDetector.CodeRegion region = structure.regions().get(0);
        assertTrue(region.content().contains("    public void addClickListener()"));
        assertTrue(region.content().contains("        onEvent(\"click\", handler);"));
    }
}
