/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { log } from '../runtime/log.ts';

const RETRY_TIMEOUT = 30_000;
const RETRY_PERIOD = 2_000;

/** What the page may define to take part (ponysdk.js, BootstrapServlet, the application). */
interface PageHooks {
    ponyReconnectMode?: boolean;
    onPonyReconnected?: () => void;
    showReconnectionInformation?: () => void;
}

/** Reloads the page, through ponysdk.js when it is there. */
export function reload(): void {
    const doReload = (document as unknown as { doReload?: () => void }).doReload;
    if (typeof doReload === 'function') doReload();
    else location.reload();
}

/**
 * Notices a lost connection and gets it back (the GWT terminal's ReconnectionChecker). The page is told (the
 * element #reconnection, or showReconnectionInformation), then the server is pinged every 2 s. Once it answers: with
 * transparent reconnection (ponyReconnectMode), a new socket resumes the context; else onPonyReconnected runs, or
 * the page reloads.
 */
export class Reconnection {
    private readonly baseURL: () => string;
    private readonly reconnect: () => void;
    private errorDetected = false;
    /** A new socket is on its way: its own failure must not start a second cycle. */
    private isReconnecting = false;

    constructor(baseURL: () => string, reconnect: () => void) {
        this.baseURL = baseURL;
        this.reconnect = reconnect;
    }

    get reconnecting(): boolean {
        return this.isReconnecting;
    }

    /** RECONNECT_CONTEXT: the server resumed the context. */
    succeeded(): void {
        this.isReconnecting = false;
        this.errorDetected = false;
        const notice = document.getElementById('reconnection');
        if (notice !== null) notice.style.display = 'none';
    }

    /** An abnormal close: the first starts the cycle, that of a new socket retries it. */
    socketClosed(): void {
        if (this.isReconnecting) {
            this.isReconnecting = false;
            log.error('The new WebSocket failed: retrying');
            setTimeout(() => this.ping(), RETRY_PERIOD);
        } else {
            this.connectionLost();
        }
    }

    connectionLost(): void {
        if (this.errorDetected || this.isReconnecting) return;
        this.errorDetected = true;
        log.error('Connection to the server lost');
        const listeners = (document as unknown as { onConnectionLostListeners?: (() => void)[] }).onConnectionLostListeners ?? [];
        for (const listener of listeners) {
            try {
                listener();
            } catch (e) {
                log.error('A connection-lost listener failed', e);
            }
        }
        const hooks = window as unknown as PageHooks;
        if (typeof hooks.showReconnectionInformation === 'function') {
            hooks.showReconnectionInformation();
        } else {
            const notice = document.getElementById('reconnection');
            if (notice !== null) notice.style.display = 'block';
            const text = document.getElementById('reconnecting');
            if (text !== null) text.innerHTML = 'Connection to server lost<br>Reconnecting ...';
        }
        setTimeout(() => this.ping(), RETRY_PERIOD);
    }

    private ping(): void {
        const request = new XMLHttpRequest();
        request.open('GET', `${this.baseURL()}?ping=${Date.now()}&retry`);
        request.timeout = RETRY_TIMEOUT;
        request.onreadystatechange = () => {
            if (request.readyState !== XMLHttpRequest.DONE) return;
            if (request.status === 200) this.serverReachable();
            else setTimeout(() => this.ping(), RETRY_PERIOD);
        };
        request.send();
    }

    private serverReachable(): void {
        this.errorDetected = false;
        const hooks = window as unknown as PageHooks;
        if (hooks.ponyReconnectMode === true) {
            this.isReconnecting = true;
            this.reconnect();
        } else if (typeof hooks.onPonyReconnected === 'function') {
            hooks.onPonyReconnected();
        } else {
            reload();
        }
    }
}
