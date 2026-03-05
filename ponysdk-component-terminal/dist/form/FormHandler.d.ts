/**
 * Client-side form handler for wa-form components.
 *
 * Handles form submission, client-side validation of child wa-* input elements,
 * and server validation error display.
 *
 * Requirements: 9.2 - Validate all child input components on submit
 * Requirements: 9.3 - Display server validation errors on matching input components
 * Requirements: 9.5 - Dispatch single submission event with all field values when valid
 */
import type { EventBridge } from '../EventBridge.js';
/** Validation error entry from the server: field name → error messages */
export interface ServerValidationErrors {
    [fieldName: string]: string[];
}
/** Payload dispatched on successful form submission */
export interface FormSubmitPayload {
    values: Record<string, unknown>;
}
/**
 * Handles client-side form submission and validation for wa-form containers.
 *
 * Responsibilities:
 * - Listen for submit events on the form element
 * - Validate all child wa-* input elements using checkValidity()/reportValidity()
 * - Collect field values and dispatch a single 'submit' event via EventBridge
 * - Apply server-side validation errors to matching input components
 */
export declare class FormHandler {
    private formElement;
    private eventBridge;
    private objectId;
    private submitListener;
    constructor(formElement: HTMLElement, eventBridge: EventBridge, objectId: number);
    /**
     * Attach the submit listener to the form element.
     */
    attach(): void;
    /**
     * Detach the submit listener from the form element.
     */
    detach(): void;
    /**
     * Programmatically trigger form submission (e.g., from a button click handler).
     */
    submit(): void;
    /**
     * Apply server-side validation errors to matching child input components.
     * Sets customValidity on each matching wa-* element and calls reportValidity.
     * Clears errors on fields not present in the error map.
     *
     * @param errors - Map of field name to error messages from the server
     */
    applyServerErrors(errors: ServerValidationErrors): void;
    /**
     * Clear all validation errors on child input elements.
     */
    clearErrors(): void;
    private handleSubmit;
    /**
     * Validate a single input element using native checkValidity/reportValidity.
     * Returns true if valid.
     */
    private validateInput;
    /**
     * Set custom validity message on a wa-* input element.
     */
    private setCustomValidity;
    /**
     * Get the field name for an input element.
     * Uses the 'name' attribute, falls back to 'label' property, then tag name.
     */
    private getFieldName;
    /**
     * Get the field value from an input element.
     */
    private getFieldValue;
    /**
     * Collect values from all input elements into a name→value map.
     */
    private collectValues;
    /**
     * Find all child wa-* input elements within the form.
     * Matches elements whose tag name starts with 'wa-' and that have a value property
     * or are known input-type components.
     */
    getInputElements(): HTMLElement[];
}
//# sourceMappingURL=FormHandler.d.ts.map