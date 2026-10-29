/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ServerToClientModel as K } from '#model';
import type { Reader } from '../protocol/reader.ts';
import { log } from '../runtime/log.ts';
import { type AddOn, BaseObject, type Context } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/** The creation arguments of an addon: PADDON_CREATION (JSON) or PADDON_CREATION_ARGS (an array), if any. */
function creationArguments(reader: Reader): unknown {
    reader.read();
    if (reader.key === K.PADDON_CREATION) return JSON.parse(reader.str);
    if (reader.key === K.PADDON_CREATION_ARGS) return reader.array;
    reader.rewind();
    return null;
}

function factory(context: Context, reader: Reader): (id: number, args: unknown, widgetId: string | null,
                                                     element: HTMLElement | null) => AddOn {
    reader.read(); // FACTORY
    const signature = reader.str;
    const found = context.addOnFactory(signature);
    if (found === undefined) throw new Error(`AddOn factory not found for signature: ${signature}`);
    return found;
}

/**
 * The addon sends through its own terminal (AbstractAddon.sendDataToServer): a page with <pony-app> elements has one
 * per element, and a window.pony that is none of them (ADR-0008).
 */
function bind(addOn: AddOn, context: Context): AddOn {
    addOn.terminal = context.session;
    return addOn;
}

/**
 * PAddOn: a JavaScript object defined by AbstractAddon.defineAddon in the page, created by its factory and driven by
 * method calls (PADDON_METHOD with PADDON_ARGUMENTS).
 */
export class AddOnObject extends BaseObject {
    protected addOn: AddOn | undefined;
    protected destroyed = false;

    static create(context: Context, id: number, type: number, reader: Reader): AddOnObject {
        const create = factory(context, reader);
        const args = creationArguments(reader);
        const object = new AddOnObject(context, id, type);
        try {
            object.addOn = bind(create(id, args, null, null), context);
            object.addOn.onInit();
        } catch (e) {
            log.error(`${object} could not be created`, e);
        }
        return object;
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.PADDON_METHOD) {
            const method = reader.str;
            if (reader.read() === K.PADDON_ARGUMENTS) {
                this.call(method, reader.array);
            } else {
                reader.rewind();
                this.call(method, null);
            }
            return true;
        }
        if (reader.key === K.DESTROY) {
            this.destroy();
            return true;
        }
        return super.update(reader);
    }

    protected call(method: string, args: unknown): void {
        if (this.destroyed) {
            log.warn(`${this} is destroyed: ${method} is discarded`);
            return;
        }
        try {
            this.addOn?.update(method, args);
        } catch (e) {
            log.error(`${this}.${method}`, e);
        }
    }

    override destroy(): void {
        if (this.destroyed) return;
        this.destroyed = true;
        this.addOn?.destroy();
    }
}

/**
 * PAddOnComposite: an addon bound to a widget's element. It is told when the widget enters and leaves the document;
 * the calls made while the widget is out wait until it enters.
 */
export class AddOnComposite extends AddOnObject {
    private readonly pending: [string, unknown][] = [];
    private widget: Widget | undefined;
    private initialized = false;

    static override create(context: Context, id: number, type: number, reader: Reader): AddOnComposite {
        const create = factory(context, reader);
        reader.read();
        let args: unknown = null;
        if (reader.key === K.PADDON_CREATION || reader.key === K.PADDON_CREATION_ARGS) {
            reader.rewind();
            args = creationArguments(reader);
            reader.read();
        }
        const widgetId = reader.int; // WIDGET_ID
        const object = new AddOnComposite(context, id, type);
        const widget = context.object(widgetId) as Widget;
        object.widget = widget;
        widget.onAttachChange(attached => {
            if (attached) {
                object.addOn?.onAttached();
                object.flush();
            } else {
                object.addOn?.onDetached();
            }
        });
        try {
            object.addOn = bind(create(id, args, String(widgetId), widget.element), context);
            object.addOn.onInit();
            if (widget.attached) object.addOn.onAttached();
            object.initialized = true;
        } catch (e) {
            log.error(`${object} could not be created`, e);
        }
        return object;
    }

    /** Destroyed, the call is discarded by AddOnObject's; the pending ones went with the destruction. */
    protected override call(method: string, args: unknown): void {
        if (this.destroyed || (this.initialized && this.widget?.attached)) {
            this.flush();
            super.call(method, args);
        } else {
            this.pending.push([method, args]);
        }
    }

    private flush(): void {
        const calls = this.pending.splice(0);
        for (const [method, args] of calls) super.call(method, args);
    }

    override destroy(): void {
        this.pending.length = 0;
        super.destroy();
    }
}
