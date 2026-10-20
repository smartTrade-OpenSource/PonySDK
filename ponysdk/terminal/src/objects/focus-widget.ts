/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ServerToClientModel as K } from '#model';
import type { Message } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * A widget the user acts on (the GWT terminal's PTFocusWidget). A disabled one sends no event. By default a click
 * disables it until the server has processed the click (END_OF_PROCESSING), so that a double click is not processed
 * twice; ENABLED_ON_REQUEST keeps it enabled, LOADING_ON_REQUEST marks it busy meanwhile.
 */
export class FocusWidget extends Widget {
    protected enabled = true;
    private loadingOnRequest = false;
    private enabledOnRequest = false;

    constructor(context: Context, id: number, type: number, element: HTMLElement, typeClass: string) {
        super(context, id, type, element, typeClass);
        if (context.session.tabindexOnlyFormField) this.focusTarget.tabIndex = -1;
    }

    override update(reader: Reader): boolean {
        return apply(FocusWidget.FOCUS_WIDGET, this, reader) || super.update(reader);
    }

    /** The disabled state of the control: its disabled property when it has one, else aria-disabled. */
    setEnabled(enabled: boolean): void {
        const control = this.focusTarget as HTMLElement & { disabled?: boolean };
        if ('disabled' in control) control.disabled = !enabled;
        else if (enabled) control.removeAttribute('aria-disabled');
        else control.setAttribute('aria-disabled', 'true');
    }

    protected override sendDomEvent(type: number, event: Event, details?: (message: Message) => void): void {
        if (this.enabled) super.sendDomEvent(type, event, details);
    }

    protected override onClick(type: number, event: MouseEvent): void {
        if (!this.enabled) return;
        if (!this.enabledOnRequest) this.setEnabled(false);
        if (this.loadingOnRequest) this.element.setAttribute('aria-busy', 'true');
        this.sendMouseEvent(type, event);
    }

    protected override onKey(type: number, event: KeyboardEvent, filter: number[] | null): void {
        if (this.enabled) super.onKey(type, event, filter);
    }

    private static readonly FOCUS_WIDGET: Setters<FocusWidget> = new Map<number, (w: FocusWidget, r: Reader) => void>([
        [K.ENABLED, (w, r) => {
            w.enabled = r.bool;
            w.setEnabled(w.enabled);
        }],
        [K.LOADING_ON_REQUEST, (w, r) => {
            w.loadingOnRequest = r.bool;
        }],
        [K.ENABLED_ON_REQUEST, (w, r) => {
            w.enabledOnRequest = r.bool;
        }],
        [K.END_OF_PROCESSING, w => {
            if (w.loadingOnRequest) w.element.removeAttribute('aria-busy');
            if (!w.enabledOnRequest) w.setEnabled(w.enabled);
        }],
    ]);
}
