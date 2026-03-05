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
export class FormHandler {
    private formElement: HTMLElement;
    private eventBridge: EventBridge;
    private objectId: number;
    private submitListener: ((e: Event) => void) | null = null;

    constructor(formElement: HTMLElement, eventBridge: EventBridge, objectId: number) {
        this.formElement = formElement;
        this.eventBridge = eventBridge;
        this.objectId = objectId;
    }

    /**
     * Attach the submit listener to the form element.
     */
    attach(): void {
        this.submitListener = (e: Event) => {
            e.preventDefault();
            this.handleSubmit();
        };
        this.formElement.addEventListener('submit', this.submitListener);
    }

    /**
     * Detach the submit listener from the form element.
     */
    detach(): void {
        if (this.submitListener) {
            this.formElement.removeEventListener('submit', this.submitListener);
            this.submitListener = null;
        }
    }

    /**
     * Programmatically trigger form submission (e.g., from a button click handler).
     */
    submit(): void {
        this.handleSubmit();
    }

    /**
     * Apply server-side validation errors to matching child input components.
     * Sets customValidity on each matching wa-* element and calls reportValidity.
     * Clears errors on fields not present in the error map.
     *
     * @param errors - Map of field name to error messages from the server
     */
    applyServerErrors(errors: ServerValidationErrors): void {
        const inputs = this.getInputElements();

        for (const input of inputs) {
            const fieldName = this.getFieldName(input);
            const fieldErrors = errors[fieldName];

            if (fieldErrors && fieldErrors.length > 0) {
                this.setCustomValidity(input, fieldErrors[0]);
            } else {
                this.setCustomValidity(input, '');
            }
        }
    }

    /**
     * Clear all validation errors on child input elements.
     */
    clearErrors(): void {
        const inputs = this.getInputElements();
        for (const input of inputs) {
            this.setCustomValidity(input, '');
        }
    }

    // ========== Internal ==========

    private handleSubmit(): void {
        const inputs = this.getInputElements();
        let allValid = true;

        // Validate each input element
        for (const input of inputs) {
            const valid = this.validateInput(input);
            if (!valid) {
                allValid = false;
            }
        }

        if (allValid) {
            // Collect all field values and dispatch a single submit event
            const values = this.collectValues(inputs);
            const payload: FormSubmitPayload = { values };
            this.eventBridge.dispatch(this.objectId, 'submit', payload);
        }
    }

    /**
     * Validate a single input element using native checkValidity/reportValidity.
     * Returns true if valid.
     */
    private validateInput(input: HTMLElement): boolean {
        // Web Awesome components expose checkValidity() and reportValidity()
        const el = input as HTMLElement & {
            checkValidity?: () => boolean;
            reportValidity?: () => boolean;
        };

        if (typeof el.reportValidity === 'function') {
            return el.reportValidity();
        }
        if (typeof el.checkValidity === 'function') {
            return el.checkValidity();
        }

        // Element doesn't support validation — treat as valid
        return true;
    }

    /**
     * Set custom validity message on a wa-* input element.
     */
    private setCustomValidity(input: HTMLElement, message: string): void {
        const el = input as HTMLElement & {
            setCustomValidity?: (msg: string) => void;
            reportValidity?: () => boolean;
        };

        if (typeof el.setCustomValidity === 'function') {
            el.setCustomValidity(message);
            // Trigger display of the error message
            if (message && typeof el.reportValidity === 'function') {
                el.reportValidity();
            }
        }
    }

    /**
     * Get the field name for an input element.
     * Uses the 'name' attribute, falls back to 'label' property, then tag name.
     */
    private getFieldName(input: HTMLElement): string {
        return input.getAttribute('name')
            || (input as unknown as Record<string, unknown>).label as string
            || input.tagName.toLowerCase();
    }

    /**
     * Get the field value from an input element.
     */
    private getFieldValue(input: HTMLElement): unknown {
        const el = input as HTMLElement & { value?: unknown; checked?: boolean };

        // Checkbox/switch: use checked property
        if (input.tagName.toLowerCase() === 'wa-checkbox' || input.tagName.toLowerCase() === 'wa-switch') {
            return el.checked ?? false;
        }

        return el.value ?? '';
    }

    /**
     * Collect values from all input elements into a name→value map.
     */
    private collectValues(inputs: HTMLElement[]): Record<string, unknown> {
        const values: Record<string, unknown> = {};
        for (const input of inputs) {
            const name = this.getFieldName(input);
            values[name] = this.getFieldValue(input);
        }
        return values;
    }

    /**
     * Find all child wa-* input elements within the form.
     * Matches elements whose tag name starts with 'wa-' and that have a value property
     * or are known input-type components.
     */
    getInputElements(): HTMLElement[] {
        const allChildren = this.formElement.querySelectorAll('*');
        const inputs: HTMLElement[] = [];

        const inputTags = new Set([
            'wa-input', 'wa-textarea', 'wa-select', 'wa-checkbox',
            'wa-radio-group', 'wa-switch', 'wa-range', 'wa-color-picker', 'wa-rating',
        ]);

        for (const child of allChildren) {
            const tagName = child.tagName.toLowerCase();
            if (inputTags.has(tagName)) {
                inputs.push(child as HTMLElement);
            }
        }

        return inputs;
    }
}
