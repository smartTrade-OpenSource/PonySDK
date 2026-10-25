/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

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
}

/** The element's attributes but the terminal's own: those the application reads through PHost. */
export function hostAttributes(element: Element): Record<string, string> {
    const attributes: Record<string, string> = {};
    for (const attribute of element.attributes) {
        if (!OWN_ATTRIBUTES.includes(attribute.name)) attributes[attribute.name] = attribute.value;
    }
    return attributes;
}
