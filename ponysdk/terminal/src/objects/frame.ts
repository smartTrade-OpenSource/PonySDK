/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { BridgedChild, ChildBridge } from '../runtime/embedding.ts';
import type { ChildTerminal, Context } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

/**
 * PFrame: an iframe running a terminal of its own, on the same context. This terminal hands it the blocks of the
 * frame (FRAME_ID); the frame's terminal says when it is ready, and sends its messages through this one.
 */
export class Frame extends Widget implements ChildTerminal, BridgedChild {
    readonly url: string;
    readonly kind = 'frame';
    readonly title = null;
    /** An embedded terminal's frame: talked to by postMessage only (ADR-0009). */
    private readonly bridge: ChildBridge | null;

    static create(context: Context, id: number, type: number, reader: Reader): Frame {
        reader.read();
        let url = '';
        if (reader.key === K.URL) url = reader.str ?? '';
        else reader.rewind();
        const session = context.session;
        url += `${url.includes('?') ? '&' : '?'}${C.UI_CONTEXT_ID}=${session.contextId}&${C.FRAME_ID}=${id}`;
        if (session.tabindexOnlyFormField) url += `&${C.OPTION_TABINDEX_ACTIVATED}=true`;
        // In a <pony-app> element, relative to the application, not to the host page (ADR-0008), and talked to by
        // postMessage only (ADR-0009)
        const bridge = session.embedding?.children ?? null;
        if (bridge !== null) url = new URL(`${url}&${C.BRIDGED}=1`, session.hostPageBaseURL).href;
        return new Frame(context, id, type, url, bridge);
    }

    constructor(context: Context, id: number, type: number, url: string, bridge: ChildBridge | null = null) {
        const iframe = document.createElement('iframe');
        iframe.setAttribute('src', url);
        super(context, id, type, iframe);
        this.url = url;
        this.bridge = bridge;
        bridge?.register(this);
    }

    get target(): Window | null {
        return (this.element as HTMLIFrameElement).contentWindow;
    }

    override destroy(): void {
        this.bridge?.unregister(this);
        super.destroy();
    }

    postMessage(bytes: Uint8Array): void {
        if (this.bridge !== null) this.bridge.post(this, bytes);
        // A copy of the slice only: posting a view would clone the whole buffer it is cut from. To this page's origin,
        // never to any: a frame that navigated elsewhere gets nothing
        else (this.element as HTMLIFrameElement).contentWindow?.postMessage(bytes.slice(), location.origin);
    }

    setReady(): void {
        const message = about(this.id);
        message[C.HANDLER_OPEN] = this.url;
        this.context.send(message);
    }
}
