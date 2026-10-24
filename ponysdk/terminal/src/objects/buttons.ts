/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ServerToClientModel as K } from '#model';
import type { Reader } from '../protocol/reader.ts';
import { apply, type Context, type Setters } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';
import { FocusWidget } from './focus-widget.ts';

/** TEXT and HTML of a button or a link. */
const CONTENT: Setters<FocusWidget> = new Map<number, (w: FocusWidget, r: Reader) => void>([
    [K.TEXT, (w, r) => {
        w.element.textContent = r.str;
    }],
    [K.HTML, (w, r) => {
        w.element.innerHTML = r.str ?? '';
    }],
]);

/** PButton. */
export class Button extends FocusWidget {
    static create(context: Context, id: number, type: number, _reader: Reader): Button {
        const button = document.createElement('button');
        button.type = 'button';
        return new Button(context, id, type, button, 'pony-Button');
    }

    override update(reader: Reader): boolean {
        return apply(CONTENT, this, reader) || super.update(reader);
    }
}

/** PPushButton: a button showing an image, the PImage of WIDGET_ID. */
export class PushButton extends Button {
    static override create(context: Context, id: number, type: number, reader: Reader): PushButton {
        reader.read(); // WIDGET_ID
        const image = context.object(reader.int);
        const element = document.createElement('button');
        element.type = 'button';
        const button = new PushButton(context, id, type, element, 'pony-PushButton');
        if (image instanceof Widget) button.append(image);
        return button;
    }
}

/** PAnchor: a link; without HREF, a link the server handles. */
export class Anchor extends FocusWidget {
    static create(context: Context, id: number, type: number): Anchor {
        return new Anchor(context, id, type, document.createElement('a'), 'pony-Anchor');
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.HREF) {
            if (reader.str === null) this.element.removeAttribute('href');
            else this.element.setAttribute('href', reader.str);
            return true;
        }
        return apply(CONTENT, this, reader) || super.update(reader);
    }
}
