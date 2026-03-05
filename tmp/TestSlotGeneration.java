import com.ponysdk.core.ui.codegen.generator.CodeGeneratorImpl;
import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.SlotDef;
import java.util.Collections;
import java.util.List;

public class TestSlotGeneration {
    public static void main(String[] args) {
        CodeGeneratorImpl generator = new CodeGeneratorImpl("com.ponysdk.core.ui.webawesome");
        
        // Test 1: Component with named slots
        ComponentDefinition buttonDef = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(
                new SlotDef("prefix", "Content to place before the button text"),
                new SlotDef("suffix", "Content to place after the button text")
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
        
        String buttonCode = generator.generateWrapperClass(buttonDef);
        System.out.println("=== Button with named slots ===");
        System.out.println(buttonCode);
        System.out.println();
        
        // Test 2: Component with default slot
        ComponentDefinition cardDef = new ComponentDefinition(
            "wa-card",
            "Card",
            "A card component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new SlotDef("", "Default slot for card content")),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
        
        String cardCode = generator.generateWrapperClass(cardDef);
        System.out.println("=== Card with default slot ===");
        System.out.println(cardCode);
        
        // Verify key elements are present
        boolean hasSlotMethods = buttonCode.contains("// ========== GENERATED SLOT METHODS ==========");
        boolean hasAddPrefix = buttonCode.contains("public void addPrefix(final PComponent<?> child)");
        boolean hasAddToSlot = buttonCode.contains("addToSlot(\"prefix\", child);");
        boolean hasPComponentImport = buttonCode.contains("import com.ponysdk.core.ui.component.PComponent;");
        boolean hasAddContent = cardCode.contains("public void addContent(final PComponent<?> child)");
        boolean hasDefaultSlot = cardCode.contains("addToSlot(\"\", child);");
        
        System.out.println("\n=== Verification ===");
        System.out.println("Has slot methods section: " + hasSlotMethods);
        System.out.println("Has addPrefix method: " + hasAddPrefix);
        System.out.println("Has addToSlot call: " + hasAddToSlot);
        System.out.println("Has PComponent import: " + hasPComponentImport);
        System.out.println("Has addContent method: " + hasAddContent);
        System.out.println("Has default slot call: " + hasDefaultSlot);
        
        if (hasSlotMethods && hasAddPrefix && hasAddToSlot && hasPComponentImport && hasAddContent && hasDefaultSlot) {
            System.out.println("\n✓ All checks passed!");
        } else {
            System.out.println("\n✗ Some checks failed!");
            System.exit(1);
        }
    }
}
