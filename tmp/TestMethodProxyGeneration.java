import com.ponysdk.core.ui.codegen.generator.CodeGenerator;
import com.ponysdk.core.ui.codegen.generator.CodeGeneratorImpl;
import com.ponysdk.core.ui.codegen.model.*;

import java.util.Collections;
import java.util.List;

/**
 * Standalone test to verify method proxy generation for task 7.6
 */
public class TestMethodProxyGeneration {
    
    public static void main(String[] args) {
        CodeGenerator generator = new CodeGeneratorImpl("com.test");
        
        System.out.println("=== Test 1: Void method with no parameters ===");
        testVoidMethod(generator);
        
        System.out.println("\n=== Test 2: Method with parameters ===");
        testMethodWithParameters(generator);
        
        System.out.println("\n=== Test 3: Async method returning CompletableFuture ===");
        testAsyncMethod(generator);
        
        System.out.println("\n=== Test 4: Method with return type ===");
        testMethodWithReturnType(generator);
        
        System.out.println("\n✅ All method proxy generation tests passed!");
    }
    
    private static void testVoidMethod(CodeGenerator generator) {
        ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("focus", "Focuses the button", Collections.emptyList(), "void", false)
            ),
            Collections.emptyList(),
            "stable"
        );
        
        String code = generator.generateWrapperClass(def);
        
        // Verify method signature
        assert code.contains("public void focus()") : "Missing void method signature";
        
        // Verify bridge call
        assert code.contains("callComponentMethod(\"focus\");") : "Missing bridge call";
        
        // Verify JavaDoc
        assert code.contains("* Focuses the button") : "Missing JavaDoc";
        
        System.out.println("✓ Void method generation works correctly");
    }
    
    private static void testMethodWithParameters(CodeGenerator generator) {
        ComponentDefinition def = new ComponentDefinition(
            "wa-input",
            "Input",
            "An input",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef(
                    "setSelectionRange",
                    "Sets the selection range",
                    List.of(
                        new ParameterDef("start", "number", "int", "Start position"),
                        new ParameterDef("end", "number", "int", "End position")
                    ),
                    "void",
                    false
                )
            ),
            Collections.emptyList(),
            "stable"
        );
        
        String code = generator.generateWrapperClass(def);
        
        // Verify method signature with parameters
        assert code.contains("public void setSelectionRange(final int start, final int end)") 
            : "Missing method signature with parameters";
        
        // Verify bridge call with parameters
        assert code.contains("callComponentMethod(\"setSelectionRange\", start, end);") 
            : "Missing bridge call with parameters";
        
        // Verify parameter JavaDoc
        assert code.contains("@param start Start position") : "Missing parameter JavaDoc";
        assert code.contains("@param end End position") : "Missing parameter JavaDoc";
        
        System.out.println("✓ Method with parameters generation works correctly");
    }
    
    private static void testAsyncMethod(CodeGenerator generator) {
        ComponentDefinition def = new ComponentDefinition(
            "wa-dialog",
            "Dialog",
            "A dialog",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("show", "Shows the dialog", Collections.emptyList(), "Promise<void>", true)
            ),
            Collections.emptyList(),
            "stable"
        );
        
        String code = generator.generateWrapperClass(def);
        
        // Verify CompletableFuture return type
        assert code.contains("public CompletableFuture<Void> show()") 
            : "Missing CompletableFuture return type";
        
        // Verify async bridge call
        assert code.contains("return callComponentMethodAsync(\"show\");") 
            : "Missing async bridge call";
        
        // Verify CompletableFuture import
        assert code.contains("import java.util.concurrent.CompletableFuture;") 
            : "Missing CompletableFuture import";
        
        System.out.println("✓ Async method generation works correctly");
    }
    
    private static void testMethodWithReturnType(CodeGenerator generator) {
        ComponentDefinition def = new ComponentDefinition(
            "wa-input",
            "Input",
            "An input",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new MethodDef("checkValidity", "Checks validity", Collections.emptyList(), "boolean", false)
            ),
            Collections.emptyList(),
            "stable"
        );
        
        String code = generator.generateWrapperClass(def);
        
        // Verify return type
        assert code.contains("public boolean checkValidity()") 
            : "Missing boolean return type";
        
        // Verify return statement
        assert code.contains("return callComponentMethod(\"checkValidity\");") 
            : "Missing return statement";
        
        System.out.println("✓ Method with return type generation works correctly");
    }
}
