package com.ponysdk.core.ui.wa.codegen;

import java.util.List;

/**
 * Test fixtures containing sample ComponentDefinition instances extracted from the real
 * Web Awesome custom-elements.json manifest.
 * 
 * These fixtures are used to validate the Code Generator implementation against real component data.
 * 
 * @see <a href="https://cdn.jsdelivr.net/npm/@awesome.me/webawesome/dist/custom-elements.json">Web Awesome Manifest</a>
 */
public class ComponentDefinitionFixtures {

    /**
     * Sample ComponentDefinition for wa-input component.
     * Extracted from custom-elements.json (Web Awesome v3.2.1+)
     */
    public static ComponentDefinition waInput() {
        return new ComponentDefinition(
            "wa-input",
            "WaInput",
            "Inputs collect data from the user.",
            """
            /**
             * @summary Inputs collect data from the user.
             * @documentation https://webawesome.com/docs/components/input
             * @status stable
             * @since 2.0
             *
             * @dependency wa-icon
             *
             * @slot label - The input's label. Alternatively, you can use the `label` attribute.
             * @slot start - An element, such as `<wa-icon>`, placed at the start of the input control.
             * @slot end - An element, such as `<wa-icon>`, placed at the end of the input control.
             * @slot clear-icon - An icon to use in lieu of the default clear icon.
             * @slot show-password-icon - An icon to use in lieu of the default show password icon.
             * @slot hide-password-icon - An icon to use in lieu of the default hide password icon.
             * @slot hint - Text that describes how to use the input. Alternatively, you can use the `hint` attribute.
             *
             * @event blur - Emitted when the control loses focus.
             * @event change - Emitted when an alteration to the control's value is committed by the user.
             * @event focus - Emitted when the control gains focus.
             * @event input - Emitted when the control receives input.
             * @event wa-clear - Emitted when the clear button is activated.
             * @event wa-invalid - Emitted when the form control has been checked for validity and its constraints aren't satisfied.
             *
             * @csspart label - The label
             * @csspart hint - The hint's wrapper.
             * @csspart base - The wrapper being rendered as an input
             * @csspart input - The internal `<input>` control.
             * @csspart start - The container that wraps the `start` slot.
             * @csspart clear-button - The clear button.
             * @csspart password-toggle-button - The password toggle button.
             * @csspart end - The container that wraps the `end` slot.
             *
             * @cssstate blank - The input is empty.
             */
            """,
            List.of(
                new PropertyDef("type", "'date' | 'datetime-local' | 'email' | 'number' | 'password' | 'search' | 'tel' | 'text' | 'time' | 'url'", "String", "The type of input. Works the same as a native `<input>` element, but only a subset of types are supported. Defaults to `text`.", "'text'", false),
                new PropertyDef("value", "string | null", "String", "The default value of the form control. Primarily used for resetting the form control.", null, false),
                new PropertyDef("size", "'small' | 'medium' | 'large'", "String", "The input's size.", "'medium'", false),
                new PropertyDef("appearance", "'filled' | 'outlined' | 'filled-outlined'", "String", "The input's visual appearance.", "'outlined'", false),
                new PropertyDef("pill", "boolean", "boolean", "Draws a pill-style input with rounded edges.", "false", false),
                new PropertyDef("label", "string", "String", "The input's label. If you need to display HTML, use the `label` slot instead.", "''", false),
                new PropertyDef("hint", "string", "String", "The input's hint. If you need to display HTML, use the `hint` slot instead.", "''", false),
                new PropertyDef("with-clear", "boolean", "boolean", "Adds a clear button when the input is not empty.", "false", false),
                new PropertyDef("placeholder", "string", "String", "Placeholder text to show as a hint when the input is empty.", "''", false),
                new PropertyDef("readonly", "boolean", "boolean", "Makes the input readonly.", "false", false),
                new PropertyDef("password-toggle", "boolean", "boolean", "Adds a button to toggle the password's visibility. Only applies to password types.", "false", false),
                new PropertyDef("required", "boolean", "boolean", "Makes the input a required field.", "false", false),
                new PropertyDef("pattern", "string", "String", "A regular expression pattern to validate input against.", null, false),
                new PropertyDef("minlength", "number", "Integer", "The minimum length of input that will be considered valid.", null, false),
                new PropertyDef("maxlength", "number", "Integer", "The maximum length of input that will be considered valid.", null, false),
                new PropertyDef("min", "number | string", "String", "The input's minimum value. Only applies to date and number input types.", null, false),
                new PropertyDef("max", "number | string", "String", "The input's maximum value. Only applies to date and number input types.", null, false),
                new PropertyDef("disabled", "boolean", "boolean", "Disables the form control.", "false", false),
                new PropertyDef("name", "string | null", "String", "The name of the input, submitted as a name/value pair with form data.", "null", false)
            ),
            List.of(
                new EventDef("input", "Emitted when the control receives input.", "InputEvent"),
                new EventDef("change", "Emitted when an alteration to the control's value is committed by the user.", "Event"),
                new EventDef("blur", "Emitted when the control loses focus.", "BlurEvent"),
                new EventDef("focus", "Emitted when the control gains focus.", "FocusEvent"),
                new EventDef("wa-clear", "Emitted when the clear button is activated.", "WaClearEvent"),
                new EventDef("wa-invalid", "Emitted when the form control has been checked for validity and its constraints aren't satisfied.", "WaInvalidEvent")
            ),
            List.of(
                new SlotDef("label", "The input's label. Alternatively, you can use the `label` attribute."),
                new SlotDef("start", "An element, such as `<wa-icon>`, placed at the start of the input control."),
                new SlotDef("end", "An element, such as `<wa-icon>`, placed at the end of the input control."),
                new SlotDef("clear-icon", "An icon to use in lieu of the default clear icon."),
                new SlotDef("show-password-icon", "An icon to use in lieu of the default show password icon."),
                new SlotDef("hide-password-icon", "An icon to use in lieu of the default hide password icon."),
                new SlotDef("hint", "Text that describes how to use the input. Alternatively, you can use the `hint` attribute.")
            ),
            List.of(
                new CssPartDef("label", "The label"),
                new CssPartDef("hint", "The hint's wrapper."),
                new CssPartDef("base", "The wrapper being rendered as an input"),
                new CssPartDef("input", "The internal `<input>` control."),
                new CssPartDef("start", "The container that wraps the `start` slot."),
                new CssPartDef("clear-button", "The clear button."),
                new CssPartDef("password-toggle-button", "The password toggle button."),
                new CssPartDef("end", "The container that wraps the `end` slot.")
            ),
            List.of(),
            "stable"
        );
    }

    /**
     * Sample ComponentDefinition for wa-button component.
     * Extracted from custom-elements.json (Web Awesome v3.2.1+)
     */
    public static ComponentDefinition waButton() {
        return new ComponentDefinition(
            "wa-button",
            "WaButton",
            "Buttons represent actions that are available to the user.",
            """
            /**
             * @summary Buttons represent actions that are available to the user.
             * @documentation https://webawesome.com/docs/components/button
             * @status stable
             * @since 2.0
             *
             * @dependency wa-icon
             * @dependency wa-spinner
             *
             * @event blur - Emitted when the button loses focus.
             * @event focus - Emitted when the button gains focus.
             * @event wa-invalid - Emitted when the form control has been checked for validity and its constraints aren't satisfied.
             *
             * @slot - The button's label.
             * @slot start - An element, such as `<wa-icon>`, placed before the label.
             * @slot end - An element, such as `<wa-icon>`, placed after the label.
             *
             * @csspart base - The component's base wrapper.
             * @csspart start - The container that wraps the `start` slot.
             * @csspart label - The button's label.
             * @csspart end - The container that wraps the `end` slot.
             * @csspart caret - The button's caret icon, a `<wa-icon>` element.
             * @csspart spinner - The spinner that shows when the button is in the loading state.
             */
            """,
            List.of(
                new PropertyDef("variant", "'neutral' | 'brand' | 'success' | 'warning' | 'danger'", "String", "The button's theme variant. Defaults to `neutral` if not within another element with a variant.", "'neutral'", false),
                new PropertyDef("appearance", "'accent' | 'filled' | 'outlined' | 'filled-outlined' | 'plain'", "String", "The button's visual appearance.", "'accent'", false),
                new PropertyDef("size", "'small' | 'medium' | 'large'", "String", "The button's size.", "'medium'", false),
                new PropertyDef("with-caret", "boolean", "boolean", "Draws the button with a caret. Used to indicate that the button triggers a dropdown menu or similar behavior.", "false", false),
                new PropertyDef("disabled", "boolean", "boolean", "Disables the button.", "false", false),
                new PropertyDef("loading", "boolean", "boolean", "Draws the button in a loading state.", "false", false),
                new PropertyDef("pill", "boolean", "boolean", "Draws a pill-style button with rounded edges.", "false", false),
                new PropertyDef("type", "'button' | 'submit' | 'reset'", "String", "The type of button. Note that the default value is `button` instead of `submit`, which is opposite of how native `<button>` elements behave.", "'button'", false),
                new PropertyDef("name", "string | null", "String", "The name of the button, submitted as a name/value pair with form data, but only when this button is the submitter.", "null", false),
                new PropertyDef("value", "string", "String", "The value of the button, submitted as a pair with the button's name as part of the form data, but only when this button is the submitter.", null, false),
                new PropertyDef("href", "string", "String", "When set, the underlying button will be rendered as an `<a>` with this `href` instead of a `<button>`.", null, false),
                new PropertyDef("target", "'_blank' | '_parent' | '_self' | '_top'", "String", "Tells the browser where to open the link. Only used when `href` is present.", null, false)
            ),
            List.of(
                new EventDef("blur", "Emitted when the button loses focus.", "BlurEvent"),
                new EventDef("focus", "Emitted when the button gains focus.", "FocusEvent"),
                new EventDef("wa-invalid", "Emitted when the form control has been checked for validity and its constraints aren't satisfied.", "WaInvalidEvent")
            ),
            List.of(
                new SlotDef("", "The button's label."),
                new SlotDef("start", "An element, such as `<wa-icon>`, placed before the label."),
                new SlotDef("end", "An element, such as `<wa-icon>`, placed after the label.")
            ),
            List.of(
                new CssPartDef("base", "The component's base wrapper."),
                new CssPartDef("start", "The container that wraps the `start` slot."),
                new CssPartDef("label", "The button's label."),
                new CssPartDef("end", "The container that wraps the `end` slot."),
                new CssPartDef("caret", "The button's caret icon, a `<wa-icon>` element."),
                new CssPartDef("spinner", "The spinner that shows when the button is in the loading state.")
            ),
            List.of(),
            "stable"
        );
    }

    /**
     * Sample ComponentDefinition for wa-dialog component.
     * Extracted from custom-elements.json (Web Awesome v3.2.1+)
     */
    public static ComponentDefinition waDialog() {
        return new ComponentDefinition(
            "wa-dialog",
            "WaDialog",
            "Dialogs, sometimes called \"modals\", appear above the page and require the user's immediate attention.",
            """
            /**
             * @summary Dialogs, sometimes called "modals", appear above the page and require the user's immediate attention.
             * @documentation https://webawesome.com/docs/components/dialog
             * @status stable
             * @since 2.0
             *
             * @dependency wa-button
             *
             * @slot - The dialog's main content.
             * @slot label - The dialog's label. Alternatively, you can use the `label` attribute.
             * @slot header-actions - Optional actions to add to the header. Works best with `<wa-button>`.
             * @slot footer - The dialog's footer, usually one or more buttons representing various options.
             *
             * @event wa-show - Emitted when the dialog opens.
             * @event wa-after-show - Emitted after the dialog opens and all animations are complete.
             * @event {{ source: Element }} wa-hide - Emitted when the dialog is requested to close.
             * @event wa-after-hide - Emitted after the dialog closes and all animations are complete.
             *
             * @csspart dialog - The dialog's internal `<dialog>` element.
             * @csspart header - The dialog's header. This element wraps the title and header actions.
             * @csspart header-actions - Optional actions to add to the header. Works best with `<wa-button>`.
             * @csspart title - The dialog's title.
             * @csspart close-button - The close button, a `<wa-button>`.
             * @csspart close-button__base - The close button's exported `base` part.
             * @csspart body - The dialog's body.
             * @csspart footer - The dialog's footer.
             *
             * @cssproperty --spacing - The amount of space around and between the dialog's content.
             * @cssproperty --width - The preferred width of the dialog.
             * @cssproperty [--show-duration=200ms] - The animation duration when showing the dialog.
             * @cssproperty [--hide-duration=200ms] - The animation duration when hiding the dialog.
             */
            """,
            List.of(
                new PropertyDef("open", "boolean", "boolean", "Indicates whether or not the dialog is open. Toggle this attribute to show and hide the dialog.", "false", false),
                new PropertyDef("label", "string", "String", "The dialog's label as displayed in the header. You should always include a relevant label, as it is required for proper accessibility.", "''", false),
                new PropertyDef("without-header", "boolean", "boolean", "Disables the header. This will also remove the default close button.", "false", false),
                new PropertyDef("light-dismiss", "boolean", "boolean", "When enabled, the dialog will be closed when the user clicks outside of it.", "false", false)
            ),
            List.of(
                new EventDef("wa-show", "Emitted when the dialog opens.", "WaShowEvent"),
                new EventDef("wa-after-show", "Emitted after the dialog opens and all animations are complete.", "WaAfterShowEvent"),
                new EventDef("wa-hide", "Emitted when the dialog is requested to close. Calling `event.preventDefault()` will prevent the dialog from closing.", "WaHideEvent"),
                new EventDef("wa-after-hide", "Emitted after the dialog closes and all animations are complete.", "WaAfterHideEvent")
            ),
            List.of(
                new SlotDef("", "The dialog's main content."),
                new SlotDef("label", "The dialog's label. Alternatively, you can use the `label` attribute."),
                new SlotDef("header-actions", "Optional actions to add to the header. Works best with `<wa-button>`."),
                new SlotDef("footer", "The dialog's footer, usually one or more buttons representing various options.")
            ),
            List.of(
                new CssPartDef("dialog", "The dialog's internal `<dialog>` element."),
                new CssPartDef("header", "The dialog's header. This element wraps the title and header actions."),
                new CssPartDef("header-actions", "Optional actions to add to the header. Works best with `<wa-button>`."),
                new CssPartDef("title", "The dialog's title."),
                new CssPartDef("close-button", "The close button, a `<wa-button>`."),
                new CssPartDef("close-button__base", "The close button's exported `base` part."),
                new CssPartDef("body", "The dialog's body."),
                new CssPartDef("footer", "The dialog's footer.")
            ),
            List.of(
                new CssPropertyDef("--spacing", "The amount of space around and between the dialog's content.", null),
                new CssPropertyDef("--width", "The preferred width of the dialog. Note that the dialog will shrink to accommodate smaller screens.", null),
                new CssPropertyDef("--show-duration", "The animation duration when showing the dialog.", "200ms"),
                new CssPropertyDef("--hide-duration", "The animation duration when hiding the dialog.", "200ms")
            ),
            "stable"
        );
    }

    /**
     * Returns all sample component definitions.
     */
    public static List<ComponentDefinition> all() {
        return List.of(waInput(), waButton(), waDialog());
    }

    // Record definitions matching the design document

    public record ComponentDefinition(
        String tagName,
        String className,
        String summary,
        String jsDoc,
        List<PropertyDef> properties,
        List<EventDef> events,
        List<SlotDef> slots,
        List<CssPartDef> cssParts,
        List<CssPropertyDef> cssProperties,
        String status
    ) {}

    public record PropertyDef(
        String name,
        String type,
        String javaType,
        String description,
        String defaultValue,
        boolean reflects
    ) {}

    public record EventDef(
        String name,
        String description,
        String detailType
    ) {}

    public record SlotDef(
        String name,
        String description
    ) {}

    public record CssPartDef(
        String name,
        String description
    ) {}

    public record CssPropertyDef(
        String name,
        String description,
        String defaultValue
    ) {}
}
