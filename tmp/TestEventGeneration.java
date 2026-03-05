import com.ponysdk.core.ui.codegen.generator.CodeGeneratorImpl;
import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.EventDef;
import java.util.Collections;
import java.util.List;

public class TestEventGeneration {
    public static void main(String[] args) {
        CodeGeneratorImpl generator = new CodeGeneratorImpl("com.ponysdk.core.ui.webawesome");
        
        ComponentDefinition def = new ComponentDefinition(
            "wa-button",
            "Button",
            "A button component",
            "",
            Collections.emptyList(),
            List.of(
                new EventDef("wa-click", "Emitted when clicked", null, true, false),
                new EventDef("wa-focus", "Emitted when focused", null, true, false)
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
        
        String source = generator.generateWrapperClass(def);
        System.out.println(source);
    }
}
