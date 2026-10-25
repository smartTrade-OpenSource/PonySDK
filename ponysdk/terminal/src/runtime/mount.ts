/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import type { Embedding } from './embedding.ts';
import { BrowserHistory, type History } from './history.ts';
import { log } from './log.ts';

/**
 * Where a terminal runs: its page, or a <pony-app> element of another page
 * (docs/decisions/0008-embedded-application.md). What differs between the two goes through here.
 */
export interface Mount {
    /** The application's URL: the page's location, or the element's src. The server's paths start at its path. */
    readonly url: URL | Location;
    /** Where the root panel, popups and lists go. */
    readonly body: HTMLElement;
    byId(id: string): HTMLElement | null;
    readonly history: History;
    readonly embedding: Embedding | null;
    /** A lost connection resumes the context (transparent reconnection). */
    readonly transparentReconnection: boolean;
    /** Query parameters the handshake adds, '&' first: the host's attributes. */
    readonly handshake: string;
    /** The credential the host gave for this start, offered with the handshake (ADR-0009); null in a page. */
    readonly credential: string | null;
    /** Aborted when the terminal goes: its listeners on the window. */
    readonly signal: AbortSignal | undefined;
    /** The connection is lost: tells the page or the host. True when the page shows its own notice. */
    connectionLost(): boolean;
    /** The context resumed after a lost connection. */
    reconnected(): void;
    /** The server answers again, without transparent reconnection. */
    serverBack(): void;
    /** The context is gone: the page reloads, the element starts its application again. */
    reload(): void;
}

/** What the page may define to take part (ponysdk.js, BootstrapServlet, the application). */
interface PageHooks {
    ponyReconnectMode?: boolean;
    onPonyReconnected?: () => void;
    showReconnectionInformation?: () => void;
}

/** A terminal that owns its page: the bootstrap page, a custom page, a window or a frame of the application. */
export class PageMount implements Mount {
    readonly url = location;
    readonly history = new BrowserHistory();
    readonly embedding = null;
    readonly handshake = '';
    readonly credential = null;
    readonly signal = undefined;

    get body(): HTMLElement {
        return document.body;
    }

    byId(id: string): HTMLElement | null {
        return document.getElementById(id);
    }

    get transparentReconnection(): boolean {
        return (window as PageHooks).ponyReconnectMode === true;
    }

    connectionLost(): boolean {
        const listeners = (document as unknown as { onConnectionLostListeners?: (() => void)[] }).onConnectionLostListeners ?? [];
        for (const listener of listeners) {
            try {
                listener();
            } catch (e) {
                log.error('A connection-lost listener failed', e);
            }
        }
        const show = (window as PageHooks).showReconnectionInformation;
        if (typeof show !== 'function') return false;
        show();
        return true;
    }

    reconnected(): void {
        const hook = (window as PageHooks).onPonyReconnected;
        if (typeof hook === 'function') hook();
    }

    serverBack(): void {
        const hook = (window as PageHooks).onPonyReconnected;
        if (typeof hook === 'function') hook();
        else this.reload();
    }

    /** Through ponysdk.js when it is there. */
    reload(): void {
        const doReload = (document as unknown as { doReload?: () => void }).doReload;
        if (typeof doReload === 'function') doReload();
        else location.reload();
    }
}
