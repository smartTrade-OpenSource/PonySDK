/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import type { WindowOperation } from '../objects/windows.ts';

/** The attributes of <pony-app> that are the terminal's, not the application's (ADR-0008). */
export const OWN_ATTRIBUTES: readonly string[] = ['src', 'isolation', 'history'];

/** The <pony-app> element a terminal runs in, as its objects see it (docs/decisions/0008-embedded-application.md). */
export interface Embedding {
    readonly element: HTMLElement;
    /** The application's attributes the handshake carried: PHost's object sends what changed since. */
    readonly attributes: Readonly<Record<string, string>>;
    /** Starts the application again in the element, on a new context: a lost context, RELOAD. */
    restart(): void;
    /** Tells the host: a CustomEvent on the element, bubbling and composed (pony-title, pony-location, ADR-0009). */
    notify(type: string, detail?: unknown): void;
    /** Its windows and frames, which it talks to by postMessage only (ADR-0009, decision 6). */
    readonly children: ChildBridge;
}

/** A window or a frame of an embedded terminal: a child page of the application's origin, fed by postMessage. */
export interface BridgedChild {
    readonly id: number;
    readonly kind: 'window' | 'frame';
    /** The window's name, its title; null for a frame. */
    readonly title: string | null;
    /** Where its messages come from and go to; null once closed. */
    readonly target: Window | null;
    /** The child page goes. */
    closing(): void;
}

/** What an embedded terminal's children are told by postMessage, besides their blocks (child-bridge.ts). */
export type ToChild =
    | { pony: 'init'; keys: string | null; title: string | null; entries: [number, string][] }
    | { pony: 'entries'; entries: [number, string][] }
    | { pony: 'window'; operation: WindowOperation };

/** What they say to it. */
export type ToParent = { pony: 'ready' } | { pony: 'send'; message: string } | { pony: 'closing' };

/** The children of an embedded terminal (parent-bridge.ts). */
export interface ChildBridge {
    register(child: BridgedChild): void;
    unregister(child: BridgedChild): void;
    /** Blocks for the child, after the dictionary entries it may lack. */
    post(child: BridgedChild, bytes: Uint8Array): void;
    /** A window operation the child runs on itself: a cross-origin WindowProxy refuses most of them. */
    operate(child: BridgedChild, operation: WindowOperation): void;
}

/** The element's attributes but the terminal's own: those the application reads through PHost. */
export function hostAttributes(element: Element): Record<string, string> {
    const attributes: Record<string, string> = {};
    for (const attribute of element.attributes) {
        if (!OWN_ATTRIBUTES.includes(attribute.name)) attributes[attribute.name] = attribute.value;
    }
    return attributes;
}
