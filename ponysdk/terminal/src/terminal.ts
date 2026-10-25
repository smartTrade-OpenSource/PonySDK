/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, MappingPath, ProtocolVersion } from '#model';
import { Reconnection } from './connection/reconnection.ts';
import { Socket, type SocketListener } from './connection/socket.ts';
import { Cookies } from './objects/cookies.ts';
import { watchWindows } from './objects/windows.ts';
import type { Message } from './protocol/messages.ts';
import { Dispatcher, type Host } from './runtime/dispatcher.ts';
import { createObject, loadAll, loadingOf } from './runtime/factory.ts';
import type { Embedding } from './runtime/embedding.ts';
import type { History } from './runtime/history.ts';
import { log } from './runtime/log.ts';
import { type Mount, PageMount } from './runtime/mount.ts';
import { type AddOnFactory, CONTEXT_KEY_HEADER, type Session } from './runtime/object.ts';
import { installStyles } from './runtime/styles.ts';

/** WebSocket.CLOSE_PAGE_LEFT on the server: the page goes for good, and its context with it (ADR-0005). */
const CLOSE_PAGE_LEFT = 4002;

/** The subprotocol an embedded terminal offers, which the server accepts (Subprotocols.java, ADR-0009). */
const SUBPROTOCOL = 'ponysdk';

/** Base64url of the text's UTF-8: a subprotocol is a token, which a credential need not be. */
function base64url(text: string): string {
    let binary = '';
    for (const byte of new TextEncoder().encode(text)) binary += String.fromCharCode(byte);
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** The terminal of the opener or of the parent frame, as a child terminal calls it. */
interface ParentTerminal {
    setReadyWindow(id: number | string): void;
    setReadyFrame(id: number | string): void;
    sendDataToServerFromWindow(json: string): void;
    dictionaryEntry(id: number): string | undefined;
}

/**
 * The terminal of a page: the main window's, connected to the server by its WebSocket,
 * or that of a window or a frame opened by the server, fed by its opener or its parent;
 * or the terminal of a <pony-app> element in another page (its Mount, docs/decisions/0008-embedded-application.md).
 *
 * Its public methods are window.pony, which ponysdk.js, the addons and the child terminals call: their names are a
 * contract.
 */
export class Terminal implements Session, Host {
    contextId = 0;
    tabindexOnlyFormField = false;
    /** An embedded terminal's keys (ADR-0009): K proves its context in a header or a subprotocol, S in a URL. */
    contextKey: string | null = null;
    private streamKey: string | null = null;
    readonly dispatcher: Dispatcher;
    private readonly mount: Mount;
    private readonly history: History;
    private started = false;
    private socket: Socket | undefined;
    private reconnection: Reconnection | undefined;
    private parent: ParentTerminal | undefined;
    private transport: (message: Message) => void = () => log.warn('A message before the connection is dropped');
    private historyListened = false;
    private chunksScheduled = false;
    private heartbeat: ReturnType<typeof setInterval> | undefined;

    constructor(mount: Mount = new PageMount()) {
        this.mount = mount;
        this.history = mount.history;
        this.dispatcher = new Dispatcher(this, createObject, this, this.history, id => this.parent?.dictionaryEntry(id),
            loadingOf);
    }

    start(): void {
        if (this.started) return;
        this.started = true;
        const embedded = this.mount.embedding !== null;
        if (!embedded) installStyles(); // a <pony-app> element installs them where it renders
        const parameters = new URLSearchParams(this.mount.url.search);
        if (!embedded && parameters.has(C.UI_CONTEXT_ID)) this.startChild(parameters);
        else this.startMain(embedded);
        watchWindows(this.dispatcher.windows, this.mount.signal);
    }

    private startMain(embedded: boolean): void {
        const signal = this.mount.signal;
        window.addEventListener('pagehide', event => this.close(!event.persisted), signal === undefined ? undefined : { signal });
        this.reconnection = new Reconnection(() => this.hostPageBaseURL, () => this.reconnectSocket(this.buildReconnectUrl()),
            this.mount);
        const url = this.socketUrl('');
        // The early socket of the bootstrap page is its own terminal's, never a <pony-app> element's
        this.socket = embedded ? new Socket(new WebSocket(url, this.subprotocols(false)), this.socketListener())
            : Socket.connect(url, this.socketListener());
        log.info(embedded ? `PonySDK terminal started for ${this.mount.url.href}` : 'PonySDK TypeScript terminal is started');
    }

    /** A window or a frame opened by the server: its opener or its parent feeds it and relays its messages. */
    private startChild(parameters: URLSearchParams): void {
        this.contextId = Number(parameters.get(C.UI_CONTEXT_ID));
        const tabindex = parameters.get(C.OPTION_TABINDEX_ACTIVATED);
        if (tabindex !== null) this.tabindexOnlyFormField = tabindex === 'true';
        const windowId = parameters.get(C.WINDOW_ID);
        const frameId = parameters.get(C.FRAME_ID) ?? '';
        const source = (windowId !== null ? window.opener : window.parent) as (Window & { pony?: ParentTerminal }) | null;
        const parent = source?.pony;
        if (source === null || parent === undefined) {
            log.error('No terminal opened this page');
            return;
        }
        this.parent = parent;
        window.addEventListener('message', event => {
            if (event.source !== source || !(event.data instanceof Uint8Array)) return;
            const bytes = event.data;
            this.dispatcher.guard(() => {
                if (windowId !== null) this.dispatcher.receiveInWindow(bytes);
                else this.dispatcher.receiveInFrame(bytes);
                this.loadChunksLater();
            });
        });
        if (windowId !== null) parent.setReadyWindow(windowId);
        else parent.setReadyFrame(frameId);
        this.init(message => parent.sendDataToServerFromWindow(JSON.stringify(message)));
        log.info('PonySDK TypeScript terminal is started for a child window');
    }

    /** On each connection: messages go to the new transport, the cookies are sent, the splash screen hides. */
    /**
     * The widgets loaded on demand, once the page is built: one second after the first message, when the browser is
     * idle, so that they do not compete with the page, and the first of them does not wait (ADR-0004).
     */
    private loadChunksLater(): void {
        if (this.chunksScheduled) return;
        this.chunksScheduled = true;
        const idle = (window as { requestIdleCallback?: (callback: () => void, options: { timeout: number }) => void })
            .requestIdleCallback;
        setTimeout(() => {
            if (idle !== undefined) idle(loadAll, { timeout: 4000 });
            else loadAll();
        }, 1000);
    }

    private init(transport: (message: Message) => void): void {
        this.transport = transport;
        if (!this.historyListened) {
            this.historyListened = true;
            this.history.onChange(token => {
                if (token) this.dispatcher.send({ [C.TYPE_HISTORY]: token });
            });
        }
        this.dispatcher.register(new Cookies(this.dispatcher));
        if (this.mount.embedding !== null) return; // the element shows its own children until the first widget
        const loading = document.getElementById('loading');
        if (loading !== null) {
            loading.style.width = '0px';
            loading.style.height = '0px';
            loading.style.display = 'none';
        } else {
            log.warn('Include a splash screen element with id="loading" in the page');
        }
    }

    private socketListener(): SocketListener {
        return {
            opened: socket => this.init(message => socket.send(JSON.stringify(message))),
            received: (message, arrivedAt) => {
                this.dispatcher.receive(message, arrivedAt);
                this.loadChunksLater();
            },
            closed: code => {
                if (code !== 1000) this.reconnection?.socketClosed();
            },
        };
    }

    private socketUrl(parameters: string): string {
        const l = this.mount.url;
        return `${l.protocol.replace('http', 'ws')}//${l.host}${l.pathname}${MappingPath.WEBSOCKET}?${C.TYPE_HISTORY}=`
            + `${this.history.token}${parameters}&${C.PROTOCOL_VERSION}=${ProtocolVersion.CURRENT}${l.search.replace('?', '&')}`
            + this.mount.handshake;
    }

    // ---- Host --------------------------------------------------------------------------------------------------------

    send(message: Message): void {
        this.transport(message);
    }

    contextCreated(id: number, tabindexOnlyFormField: boolean, heartbeatSeconds: number, keys: string | null): boolean {
        if (this.reconnection?.reconnecting) {
            // The server could not resume this context (restarted, or the window is over) and started a new one,
            // whose page follows on this socket: reload, instead of stacking it under the old page
            this.closeSocket('context lost, reloading');
            this.destroy();
            return false;
        }
        this.contextId = id;
        this.tabindexOnlyFormField = tabindexOnlyFormField;
        const [contextKey, streamKey] = keys?.split('.') ?? [];
        this.contextKey = contextKey ?? null;
        this.streamKey = streamKey ?? null;
        this.setHeartBeatPeriod(heartbeatSeconds);
        return true;
    }

    /**
     * What an embedded terminal offers with its handshake, out of the URL (ADR-0009): its context's key to resume
     * it, or its host's credential to start one. Nothing for a page's.
     */
    private subprotocols(reconnecting: boolean): string[] | undefined {
        if (this.mount.embedding === null) return undefined;
        if (reconnecting && this.contextKey !== null) return [SUBPROTOCOL, `${SUBPROTOCOL}.key.${this.contextKey}`];
        const credential = this.mount.credential;
        return credential === null ? [SUBPROTOCOL] : [SUBPROTOCOL, `${SUBPROTOCOL}.credential.${base64url(credential)}`];
    }

    contextResumed(): void {
        this.reconnection?.succeeded();
        this.onReconnected();
    }

    contextDestroyed(): void {
        this.destroy();
    }

    /** A file the server sends: a hidden frame downloads it. */
    streamRequested(requestId: number): void {
        const frame = document.createElement('iframe');
        frame.style.cssText = 'width:0;height:0;border:0;visibility:hidden;position:fixed';
        this.mount.body.appendChild(frame);
        setTimeout(() => {
            frame.src = this.streamUrl(requestId);
        });
    }

    streamUrl(requestId: number): string {
        const url = `${this.hostPageBaseURL}${MappingPath.STREAM}?${C.UI_CONTEXT_ID}=${this.contextId}`
            + `&${C.STREAM_REQUEST_ID}=${requestId}`;
        return this.streamKey === null ? url : `${url}&${C.STREAM_KEY}=${this.streamKey}`;
    }

    private destroy(): void {
        this.dispatcher.windows.closeAll();
        this.mount.reload();
    }

    /**
     * A <pony-app> element's terminal goes: its socket closes as a page left (the server destroys the context at once,
     * ADR-0005), its objects release what they hold on the page. The element aborts its Mount's signal.
     */
    dispose(): void {
        this.close(true);
        clearInterval(this.heartbeat);
        this.dispatcher.destroyAll();
    }

    // ---- Session -----------------------------------------------------------------------------------------------------

    get hostPageBaseURL(): string {
        return this.getHostPageBaseURL();
    }

    get body(): HTMLElement {
        return this.mount.body;
    }

    byId(id: string): HTMLElement | null {
        return this.mount.byId(id);
    }

    get embedding(): Embedding | null {
        return this.mount.embedding;
    }

    // ---- window.pony -------------------------------------------------------------------------------------------------

    getHostPageBaseURL(): string {
        let url = this.mount.url.href;
        const hash = url.indexOf('#');
        if (hash !== -1) url = url.substring(0, hash);
        const query = url.indexOf('?');
        if (query !== -1) url = url.substring(0, query);
        return url.substring(0, url.lastIndexOf('/') + 1);
    }

    /** A message of a child terminal, already wrapped. */
    sendDataToServerFromWindow(json: string): void {
        this.dispatcher.sendRaw(JSON.parse(json) as Message);
    }

    /**
     * Data of an addon for its server object. With a callback, an HTTP request whose headers carry the data, and
     * whose response goes to the callback.
     */
    sendDataToServer(objectId: number | string, data: Record<string, unknown>, callback?: (response: string) => void): void {
        const id = Number(objectId);
        if (typeof callback !== 'function') {
            this.dispatcher.send({ [C.OBJECT_ID]: id, [C.NATIVE]: data });
            return;
        }
        const request = new XMLHttpRequest();
        request.onload = () => callback(request.responseText);
        request.open('GET', this.hostPageBaseURL + MappingPath.AJAX);
        request.withCredentials = this.mount.embedding !== null; // the session cookie, from the host's origin
        // The server reads these by their enum names; the key, when the cookie does not come (ADR-0009)
        if (this.contextKey !== null) request.setRequestHeader(CONTEXT_KEY_HEADER, this.contextKey);
        request.setRequestHeader('UI_CONTEXT_ID', String(this.contextId));
        request.setRequestHeader('OBJECT_ID', String(id));
        for (const [key, value] of Object.entries(data)) {
            request.setRequestHeader(key, value === null || value === undefined ? '' : typeof value === 'string' ? value : JSON.stringify(value));
        }
        request.send();
    }

    request(objectId: number | string, data: Record<string, unknown>, callback?: (response: string) => void): void {
        this.sendDataToServer(objectId, data, callback);
    }

    setReadyFrame(frameId: number | string): void {
        this.dispatcher.setReady(Number(frameId), 'frame');
    }

    setReadyWindow(windowId: number | string): void {
        this.dispatcher.setReady(Number(windowId), 'window');
    }

    registerAddOnFactory(signature: string, factory: AddOnFactory): void {
        this.dispatcher.registerAddOnFactory(signature, factory);
    }

    /** An entry of this terminal's string dictionary, for a child terminal that never received it. */
    dictionaryEntry(id: number): string | undefined {
        try {
            return this.dispatcher.reader.entry(id);
        } catch {
            return undefined;
        }
    }

    /**
     * pageLeft: the page goes for good (not kept in the back-forward cache), the server can drop its context at once
     * (ADR-0005); otherwise it may come back and reconnect.
     */
    close(pageLeft = false): void {
        if (pageLeft) this.socket?.close(CLOSE_PAGE_LEFT, 'page left');
        else this.socket?.close();
        this.dispatcher.windows.closeAll();
    }

    /** A normal close: no message follows and no reconnection starts. */
    closeSocket(reason: string): void {
        this.socket?.close(1000, reason);
    }

    getContextId(): number {
        return this.contextId;
    }

    setContextId(contextId: number): void {
        this.contextId = contextId;
    }

    isTabindexOnlyFormField(): boolean {
        return this.tabindexOnlyFormField;
    }

    setTabindexOnlyFormField(tabindexOnlyFormField: boolean): void {
        this.tabindexOnlyFormField = tabindexOnlyFormField;
    }

    /**
     * Watches the socket: no message for twice the server's period closes it and starts a reconnection. A batch
     * that kept the event loop busy longer than the period is not a silence: the silence counts from the page's
     * waking, since the messages that came meanwhile may wait behind the timers (Firefox, Safari). The server needs
     * nothing from this page to know it is there: the browser answers its pings (RFC 6455, 5.5.2), even while this
     * page is busy.
     */
    setHeartBeatPeriod(seconds: number): void {
        if (seconds === 0) return;
        const period = seconds * 1000;
        const timeout = period * 2;
        let lastCheck = Date.now(); // a page busy before the first check is starved too
        let awake = -1; // the end of the last starvation
        clearInterval(this.heartbeat);
        this.heartbeat = setInterval(() => {
            const socket = this.socket;
            if (socket === undefined || socket.readyState !== WebSocket.OPEN) return;
            const now = Date.now();
            const starved = now - lastCheck > period;
            lastCheck = now;
            if (starved) {
                awake = now;
                return;
            }
            if (now - Math.max(socket.lastMessageTime, awake) > timeout) {
                socket.close(1000, `no data received since ${now - socket.lastMessageTime} ms`);
                this.reconnection?.connectionLost();
            }
        }, 500);
    }

    onReconnected(): void {
        log.info(`Transparent reconnection succeeded for context #${this.contextId}`);
        this.mount.reconnected();
    }

    buildReconnectUrl(): string {
        return this.socketUrl(`&${C.RECONNECT_UI_CONTEXT_ID}=${this.contextId}`);
    }

    reconnectSocket(url: string): void {
        this.socket = new Socket(new WebSocket(url, this.subprotocols(true)), this.socketListener());
    }
}
