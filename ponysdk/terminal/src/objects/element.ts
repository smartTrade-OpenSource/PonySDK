/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ServerToClientModel as K } from '#model';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters, type TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/** PElement: any tag, holding text, HTML or widgets. */
export class ElementWidget extends Widget {
    static create(context: Context, id: number, type: number, reader: Reader): ElementWidget {
        reader.read(); // TAG
        return new ElementWidget(context, id, type, document.createElement(reader.str));
    }

    override update(reader: Reader): boolean {
        return apply(ELEMENT, this, reader) || super.update(reader);
    }

    /** At INDEX among the children when given, else at the end. */
    override add(reader: Reader, child: TerminalObject): void {
        reader.read();
        if (reader.key === K.INDEX && child instanceof Widget) {
            this.insert(child, reader.int);
        } else {
            if (reader.key !== K.INDEX) reader.rewind();
            super.add(reader, child);
        }
    }
}

const ELEMENT: Setters<ElementWidget> = new Map<number, (w: ElementWidget, r: Reader) => void>([
    [K.HTML, (w, r) => {
        w.element.innerHTML = r.str;
    }],
    [K.TEXT, (w, r) => {
        w.element.textContent = r.str;
    }],
]);
