/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { log } from '../runtime/log.ts';
import type { Mount } from '../runtime/mount.ts';

const RETRY_TIMEOUT = 30_000;
const RETRY_PERIOD = 2_000;

/**
 * Notices a lost connection and gets it back. The page or the host is told (Mount.connectionLost, else the element
 * #reconnection), then the server is pinged every 2 s. Once it answers: with transparent reconnection, a new socket
 * resumes the context; else the mount decides (Mount.serverBack: a hook, or a reload).
 */
export class Reconnection {
    private readonly baseURL: () => string;
    private readonly reconnect: () => void;
    private readonly mount: Mount;
    private errorDetected = false;
    /** A new socket is on its way: its own failure must not start a second cycle. */
    private isReconnecting = false;

    constructor(baseURL: () => string, reconnect: () => void, mount: Mount) {
        this.baseURL = baseURL;
        this.reconnect = reconnect;
        this.mount = mount;
    }

    get reconnecting(): boolean {
        return this.isReconnecting;
    }

    /** RECONNECT_CONTEXT: the server resumed the context. */
    succeeded(): void {
        this.isReconnecting = false;
        this.errorDetected = false;
        const notice = this.mount.byId('reconnection');
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
        if (!this.mount.connectionLost()) {
            const notice = this.mount.byId('reconnection');
            if (notice !== null) notice.style.display = 'block';
            const text = this.mount.byId('reconnecting');
            if (text !== null) text.innerHTML = 'Connection to server lost<br>Reconnecting ...';
        }
        setTimeout(() => this.ping(), RETRY_PERIOD);
    }

    private ping(): void {
        if (this.mount.signal?.aborted === true) return; // the terminal is gone: its element left the page
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
        if (this.mount.transparentReconnection) {
            this.isReconnecting = true;
            this.reconnect();
        } else {
            this.mount.serverBack();
        }
    }
}
