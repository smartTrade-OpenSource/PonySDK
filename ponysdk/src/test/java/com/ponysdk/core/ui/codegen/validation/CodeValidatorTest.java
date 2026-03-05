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

package com.ponysdk.core.ui.codegen.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CodeValidator.
 * <p>
 * Tests Java syntax validation, TypeScript syntax validation, and error reporting.
 * Requirements: 16.1, 16.2, 16.3, 16.5
 * </p>
 */
public class CodeValidatorTest {

    private CodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CodeValidator();
    }

    // ========== Java Syntax Validation Tests ==========

    /**
     * Test validation of valid Java code.
     * Validates: Requirement 16.1
     */
    @Test
    void testValidateValidJavaCode() {
        String validJava = """
            package com.example;
            
            public class TestClass {
                private String name;
                
                public TestClass(String name) {
                    this.name = name;
                }
                
                public String getName() {
                    return name;
                }
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", validJava);
        
        assertTrue(result.valid(), "Valid Java code should pass validation");
        assertTrue(result.errors().isEmpty(), "Valid Java code should have no errors");
    }

    /**
     * Test validation detects missing semicolon.
     * Validates: Requirement 16.1, 16.5
     */
    @Test
    void testValidateJavaMissingSemicolon() {
        String invalidJava = """
            package com.example;
            
            public class TestClass {
                private String name
                
                public String getName() {
                    return name;
                }
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", invalidJava);
        
        assertFalse(result.valid(), "Java code with missing semicolon should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
        
        // Verify error details
        CodeValidator.ValidationError error = result.errors().get(0);
        assertNotNull(error.message(), "Error should have a message");
        assertTrue(error.lineNumber() > 0, "Error should have a line number");
    }

    /**
     * Test validation detects unmatched braces.
     * Validates: Requirement 16.1, 16.5
     */
    @Test
    void testValidateJavaUnmatchedBraces() {
        String invalidJava = """
            package com.example;
            
            public class TestClass {
                private String name;
                
                public String getName() {
                    return name;
                }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", invalidJava);
        
        assertFalse(result.valid(), "Java code with unmatched braces should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
    }

    /**
     * Test validation detects invalid method syntax.
     * Validates: Requirement 16.1, 16.5
     */
    @Test
    void testValidateJavaInvalidMethodSyntax() {
        String invalidJava = """
            package com.example;
            
            public class TestClass {
                public void invalidMethod( {
                    System.out.println("test");
                }
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", invalidJava);
        
        assertFalse(result.valid(), "Java code with invalid method syntax should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
    }

    /**
     * Test validation detects undefined type.
     * Validates: Requirement 16.1, 16.5
     */
    @Test
    void testValidateJavaUndefinedType() {
        String invalidJava = """
            package com.example;
            
            public class TestClass {
                private UndefinedType field;
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", invalidJava);
        
        assertFalse(result.valid(), "Java code with undefined type should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
        
        // Verify error message mentions the undefined type
        String errorMessage = result.errors().get(0).message();
        assertTrue(errorMessage.contains("UndefinedType") || errorMessage.contains("cannot find symbol"),
                   "Error message should mention the undefined type");
    }

    /**
     * Test validation of Java code with imports.
     * Validates: Requirement 16.1
     */
    @Test
    void testValidateJavaWithImports() {
        String validJava = """
            package com.example;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class TestClass {
                private List<String> items;
                
                public TestClass() {
                    this.items = new ArrayList<>();
                }
                
                public List<String> getItems() {
                    return items;
                }
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", validJava);
        
        assertTrue(result.valid(), "Valid Java code with imports should pass validation");
        assertTrue(result.errors().isEmpty(), "Valid Java code should have no errors");
    }

    /**
     * Test validation of Java record.
     * Validates: Requirement 16.1
     */
    @Test
    void testValidateJavaRecord() {
        String validJava = """
            package com.example;
            
            public record TestRecord(String name, int age) {
                public TestRecord {
                    if (age < 0) {
                        throw new IllegalArgumentException("Age cannot be negative");
                    }
                }
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestRecord", validJava);
        
        assertTrue(result.valid(), "Valid Java record should pass validation");
        assertTrue(result.errors().isEmpty(), "Valid Java record should have no errors");
    }

    // ========== TypeScript Syntax Validation Tests ==========

    /**
     * Test validation of valid TypeScript code.
     * Validates: Requirement 16.2
     */
    @Test
    void testValidateValidTypeScriptCode() {
        String validTypeScript = """
            export interface TestInterface {
                name: string;
                age: number;
                active: boolean;
            }
            
            export function isTestInterface(obj: unknown): obj is TestInterface {
                if (typeof obj !== 'object' || obj === null) return false;
                const test = obj as Record<string, unknown>;
                return typeof test.name === 'string' &&
                       typeof test.age === 'number' &&
                       typeof test.active === 'boolean';
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(validTypeScript);
        
        assertTrue(result.valid(), "Valid TypeScript code should pass validation");
        assertTrue(result.errors().isEmpty(), "Valid TypeScript code should have no errors");
    }

    /**
     * Test validation detects unmatched braces in TypeScript.
     * Validates: Requirement 16.2, 16.3
     */
    @Test
    void testValidateTypeScriptUnmatchedBraces() {
        String invalidTypeScript = """
            export interface TestInterface {
                name: string;
                age: number;
            
            export function test() {
                console.log("test");
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(invalidTypeScript);
        
        assertFalse(result.valid(), "TypeScript code with unmatched braces should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
        
        // Verify error details
        CodeValidator.ValidationError error = result.errors().get(0);
        assertNotNull(error.message(), "Error should have a message");
        assertTrue(error.message().contains("brace"), "Error message should mention braces");
        assertEquals("UNMATCHED_BRACES", error.code(), "Error code should be UNMATCHED_BRACES");
    }

    /**
     * Test validation detects unmatched parentheses in TypeScript.
     * Validates: Requirement 16.2, 16.3
     */
    @Test
    void testValidateTypeScriptUnmatchedParentheses() {
        String invalidTypeScript = """
            export function test() {
                const result = calculate(1, 2;
                return result;
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(invalidTypeScript);
        
        assertFalse(result.valid(), "TypeScript code with unmatched parentheses should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
        
        // Verify error details
        CodeValidator.ValidationError error = result.errors().get(0);
        assertNotNull(error.message(), "Error should have a message");
        assertTrue(error.message().contains("parenthes"), "Error message should mention parentheses");
        assertEquals("UNMATCHED_PARENS", error.code(), "Error code should be UNMATCHED_PARENS");
    }

    /**
     * Test validation of TypeScript with arrow functions.
     * Validates: Requirement 16.2
     */
    @Test
    void testValidateTypeScriptWithArrowFunctions() {
        String validTypeScript = """
            export const add = (a: number, b: number): number => {
                return a + b;
            };
            
            export const multiply = (a: number, b: number): number => a * b;
            
            export const greet = (name: string): void => {
                console.log(`Hello, ${name}!`);
            };
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(validTypeScript);
        
        assertTrue(result.valid(), "Valid TypeScript with arrow functions should pass validation");
        assertTrue(result.errors().isEmpty(), "Valid TypeScript should have no errors");
    }

    /**
     * Test validation of TypeScript with type unions.
     * Validates: Requirement 16.2
     */
    @Test
    void testValidateTypeScriptWithTypeUnions() {
        String validTypeScript = """
            export type Size = 'small' | 'medium' | 'large';
            
            export interface ComponentProps {
                size?: Size;
                variant?: 'primary' | 'secondary' | 'danger';
                disabled?: boolean;
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(validTypeScript);
        
        assertTrue(result.valid(), "Valid TypeScript with type unions should pass validation");
        assertTrue(result.errors().isEmpty(), "Valid TypeScript should have no errors");
    }

    /**
     * Test validation of TypeScript with nested objects.
     * Validates: Requirement 16.2
     */
    @Test
    void testValidateTypeScriptWithNestedObjects() {
        String validTypeScript = """
            export interface Position {
                x: number;
                y: number;
            }
            
            export interface ComponentProps {
                position: Position;
                dimensions: {
                    width: number;
                    height: number;
                };
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(validTypeScript);
        
        assertTrue(result.valid(), "Valid TypeScript with nested objects should pass validation");
        assertTrue(result.errors().isEmpty(), "Valid TypeScript should have no errors");
    }

    // ========== Error Reporting Tests ==========

    /**
     * Test that validation errors include line numbers.
     * Validates: Requirement 16.5
     */
    @Test
    void testErrorReportingIncludesLineNumbers() {
        String invalidJava = """
            package com.example;
            
            public class TestClass {
                private String name
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", invalidJava);
        
        assertFalse(result.valid(), "Invalid Java code should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
        
        CodeValidator.ValidationError error = result.errors().get(0);
        assertTrue(error.lineNumber() > 0, "Error should include line number");
    }

    /**
     * Test that validation errors include descriptive messages.
     * Validates: Requirement 16.5
     */
    @Test
    void testErrorReportingIncludesDescriptiveMessages() {
        String invalidJava = """
            package com.example;
            
            public class TestClass {
                public void method() {
                    int x = "not a number";
                }
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", invalidJava);
        
        assertFalse(result.valid(), "Invalid Java code should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
        
        CodeValidator.ValidationError error = result.errors().get(0);
        assertNotNull(error.message(), "Error should have a message");
        assertFalse(error.message().isEmpty(), "Error message should not be empty");
        assertTrue(error.message().length() > 10, "Error message should be descriptive");
    }

    /**
     * Test that validation errors include error codes.
     * Validates: Requirement 16.5
     */
    @Test
    void testErrorReportingIncludesErrorCodes() {
        String invalidTypeScript = """
            export interface Test {
                name: string;
            
            export function test() {
                console.log("test");
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(invalidTypeScript);
        
        assertFalse(result.valid(), "Invalid TypeScript code should fail validation");
        assertFalse(result.errors().isEmpty(), "Should report errors");
        
        CodeValidator.ValidationError error = result.errors().get(0);
        assertNotNull(error.code(), "Error should have a code");
        assertFalse(error.code().isEmpty(), "Error code should not be empty");
    }

    /**
     * Test that multiple errors are reported.
     * Validates: Requirement 16.3, 16.5
     */
    @Test
    void testErrorReportingMultipleErrors() {
        String invalidJava = """
            package com.example;
            
            public class TestClass {
                private String name
                private int age
                
                public void method() {
                    UndefinedType x = null;
                }
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", invalidJava);
        
        assertFalse(result.valid(), "Invalid Java code should fail validation");
        assertTrue(result.errors().size() > 1, "Should report multiple errors");
    }

    // ========== Edge Cases ==========

    /**
     * Test validation of empty Java class.
     * Validates: Requirement 16.1
     */
    @Test
    void testValidateEmptyJavaClass() {
        String validJava = """
            package com.example;
            
            public class EmptyClass {
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.EmptyClass", validJava);
        
        assertTrue(result.valid(), "Empty Java class should pass validation");
        assertTrue(result.errors().isEmpty(), "Empty Java class should have no errors");
    }

    /**
     * Test validation of empty TypeScript interface.
     * Validates: Requirement 16.2
     */
    @Test
    void testValidateEmptyTypeScriptInterface() {
        String validTypeScript = """
            export interface EmptyInterface {
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(validTypeScript);
        
        assertTrue(result.valid(), "Empty TypeScript interface should pass validation");
        assertTrue(result.errors().isEmpty(), "Empty TypeScript interface should have no errors");
    }

    /**
     * Test validation of Java class with comments.
     * Validates: Requirement 16.1
     */
    @Test
    void testValidateJavaWithComments() {
        String validJava = """
            package com.example;
            
            /**
             * Test class with JavaDoc.
             */
            public class TestClass {
                // Single line comment
                private String name;
                
                /* Multi-line
                   comment */
                public String getName() {
                    return name;
                }
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateJava("com.example.TestClass", validJava);
        
        assertTrue(result.valid(), "Java code with comments should pass validation");
        assertTrue(result.errors().isEmpty(), "Java code with comments should have no errors");
    }

    /**
     * Test validation of TypeScript with comments.
     * Validates: Requirement 16.2
     */
    @Test
    void testValidateTypeScriptWithComments() {
        String validTypeScript = """
            /**
             * Test interface with JSDoc.
             */
            export interface TestInterface {
                // Single line comment
                name: string;
                
                /* Multi-line
                   comment */
                age: number;
            }
            """;
        
        CodeValidator.ValidationResult result = validator.validateTypeScript(validTypeScript);
        
        assertTrue(result.valid(), "TypeScript code with comments should pass validation");
        assertTrue(result.errors().isEmpty(), "TypeScript code with comments should have no errors");
    }
}
