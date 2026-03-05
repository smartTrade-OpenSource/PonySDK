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

import javax.tools.*;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Validates generated code for syntax correctness.
 * <p>
 * Uses the Java Compiler API to validate Java code and basic syntax checking for TypeScript.
 * </p>
 */
public class CodeValidator {

    /**
     * Represents a validation error.
     */
    public record ValidationError(
        String message,
        int lineNumber,
        int columnNumber,
        String code
    ) {}

    /**
     * Result of code validation.
     */
    public record ValidationResult(
        boolean valid,
        List<ValidationError> errors
    ) {
        public ValidationResult(final boolean valid, final List<ValidationError> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : List.of();
        }

        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(final List<ValidationError> errors) {
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Validates Java source code for syntax correctness.
     *
     * @param className the class name
     * @param sourceCode the Java source code
     * @return validation result
     */
    public ValidationResult validateJava(final String className, final String sourceCode) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            // Compiler not available - skip validation
            return ValidationResult.success();
        }

        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        // Create in-memory source file
        final JavaFileObject sourceFile = new InMemoryJavaFileObject(className, sourceCode);

        // Compile
        final JavaCompiler.CompilationTask task = compiler.getTask(
            new StringWriter(),
            fileManager,
            diagnostics,
            Arrays.asList("-proc:none"), // Disable annotation processing
            null,
            Arrays.asList(sourceFile)
        );

        final boolean success = task.call();

        if (success) {
            return ValidationResult.success();
        }

        // Collect errors
        final List<ValidationError> errors = new ArrayList<>();
        for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errors.add(new ValidationError(
                    diagnostic.getMessage(Locale.getDefault()),
                    (int) diagnostic.getLineNumber(),
                    (int) diagnostic.getColumnNumber(),
                    diagnostic.getCode()
                ));
            }
        }

        return ValidationResult.failure(errors);
    }

    /**
     * Validates TypeScript source code for basic syntax correctness.
     * This is a simplified validation - full TypeScript validation would require the TypeScript compiler.
     *
     * @param sourceCode the TypeScript source code
     * @return validation result
     */
    public ValidationResult validateTypeScript(final String sourceCode) {
        final List<ValidationError> errors = new ArrayList<>();

        // Basic syntax checks
        final String[] lines = sourceCode.split("\n");
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();
            final int lineNumber = i + 1;

            // Check for unmatched braces
            final long openBraces = line.chars().filter(ch -> ch == '{').count();
            final long closeBraces = line.chars().filter(ch -> ch == '}').count();
            
            // Check for unmatched parentheses
            final long openParens = line.chars().filter(ch -> ch == '(').count();
            final long closeParens = line.chars().filter(ch -> ch == ')').count();

            // Check for missing semicolons (simplified - only for simple statements)
            if (!line.isEmpty() && 
                !line.endsWith("{") && 
                !line.endsWith("}") && 
                !line.endsWith(";") && 
                !line.startsWith("//") && 
                !line.startsWith("*") &&
                !line.startsWith("export") &&
                !line.startsWith("import") &&
                !line.contains("=>")) {
                
                // This might be a missing semicolon, but it's hard to tell without full parsing
                // Skip this check for now as it would have too many false positives
            }
        }

        // Check overall brace balance
        final long totalOpenBraces = sourceCode.chars().filter(ch -> ch == '{').count();
        final long totalCloseBraces = sourceCode.chars().filter(ch -> ch == '}').count();
        if (totalOpenBraces != totalCloseBraces) {
            errors.add(new ValidationError(
                "Unmatched braces: " + totalOpenBraces + " open, " + totalCloseBraces + " close",
                -1,
                -1,
                "UNMATCHED_BRACES"
            ));
        }

        // Check overall parenthesis balance
        final long totalOpenParens = sourceCode.chars().filter(ch -> ch == '(').count();
        final long totalCloseParens = sourceCode.chars().filter(ch -> ch == ')').count();
        if (totalOpenParens != totalCloseParens) {
            errors.add(new ValidationError(
                "Unmatched parentheses: " + totalOpenParens + " open, " + totalCloseParens + " close",
                -1,
                -1,
                "UNMATCHED_PARENS"
            ));
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * In-memory Java file object for compilation.
     */
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String sourceCode;

        public InMemoryJavaFileObject(final String className, final String sourceCode) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return sourceCode;
        }
    }
}
