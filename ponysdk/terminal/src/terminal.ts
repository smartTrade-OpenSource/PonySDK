/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, MappingPath, ProtocolVersion } from '#model';
import { reload, Reconnection } from './connection/reconnection.ts';
import { Socket, type SocketListener } from './connection/socket.ts';
import { Cookies } from './objects/cookies.ts';
import { watchWindows } from './objects/windows.ts';
import { FLAG, type Message } from './protocol/messages.ts';
import { Dispatcher, type Host } from './runtime/dispatcher.ts';
import { createObject, loadAll, loadingOf } from './runtime/factory.ts';
import { BrowserHistory } from './runtime/history.ts';
import { log } from './runtime/log.ts';
import type { AddOnFactory, Session } from './runtime/object.ts';
import { installStyles } from './runtime/styles.ts';

/** The terminal of the opener or of the parent frame, as a child terminal calls it. */
interface ParentTerminal {
    setReadyWindow(id: number | string): void;
    setReadyFrame(id: number | string): void;
    sendDataToServerFromWindow(json: string): void;
    dictionaryEntry(id: number): string | undefined;
}

/**
 * The terminal of a page: the main window's, connected to the server by its WebSocket,
 * or that of a window or a frame opened by the server, fed by its opener or its parent.
 *
 * Its public methods are window.pony, which ponysdk.js, the addons and the child terminals call: their names are a
 * contract.
 */
export class Terminal implements Session, Host {
    contextId = 0;
    tabindexOnlyFormField = false;
    readonly dispatcher: Dispatcher;
    private readonly history = new BrowserHistory();
    private started = false;
    private socket: Socket | undefined;
    private reconnection: Reconnection | undefined;
    private parent: ParentTerminal | undefined;
    private transport: (message: Message) => void = () => log.warn('A message before the connection is dropped');
    private historyListened = false;
    private chunksScheduled = false;
    private heartbeat: ReturnType<typeof setInterval> | undefined;

    constructor() {
        this.dispatcher = new Dispatcher(this, createObject, this, this.history, id => this.parent?.dictionaryEntry(id),
            loadingOf);
    }

    start(): void {
        if (this.started) return;
        this.started = true;
        installStyles();
        const parameters = new URLSearchParams(location.search);
        if (parameters.has(C.UI_CONTEXT_ID)) this.startChild(parameters);
        else this.startMain();
        watchWindows(this.dispatcher.windows);
    }

    private startMain(): void {
        window.addEventListener('pagehide', () => this.close());
        this.reconnection = new Reconnection(() => this.hostPageBaseURL, () => this.reconnectSocket(this.buildReconnectUrl()));
        this.socket = Socket.connect(this.socketUrl(''), this.socketListener());
        log.info('PonySDK TypeScript terminal is started');
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
            received: message => {
                this.dispatcher.receive(message);
                this.loadChunksLater();
            },
            closed: code => {
                if (code !== 1000) this.reconnection?.socketClosed();
            },
        };
    }

    private socketUrl(parameters: string): string {
        const l = location;
        return `${l.protocol.replace('http', 'ws')}//${l.host}${l.pathname}${MappingPath.WEBSOCKET}?${C.TYPE_HISTORY}=`
            + `${this.history.token}${parameters}&${C.PROTOCOL_VERSION}=${ProtocolVersion.CURRENT}${l.search.replace('?', '&')}`;
    }

    // ---- Host --------------------------------------------------------------------------------------------------------

    send(message: Message): void {
        this.transport(message);
    }

    contextCreated(id: number, tabindexOnlyFormField: boolean, heartbeatSeconds: number): boolean {
        if (this.reconnection?.reconnecting) {
            // The server could not resume this context (restarted, or the window is over) and started a new one,
            // whose page follows on this socket: reload, instead of stacking it under the old page
            this.closeSocket('context lost, reloading');
            this.destroy();
            return false;
        }
        this.contextId = id;
        this.tabindexOnlyFormField = tabindexOnlyFormField;
        this.setHeartBeatPeriod(heartbeatSeconds);
        return true;
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
        document.body.appendChild(frame);
        setTimeout(() => {
            frame.src = `${this.hostPageBaseURL}${MappingPath.STREAM}?${C.UI_CONTEXT_ID}=${this.contextId}`
                + `&${C.STREAM_REQUEST_ID}=${requestId}`;
        });
    }

    private destroy(): void {
        this.dispatcher.windows.closeAll();
        reload();
    }

    // ---- Session -----------------------------------------------------------------------------------------------------

    get hostPageBaseURL(): string {
        return this.getHostPageBaseURL();
    }

    // ---- window.pony -------------------------------------------------------------------------------------------------

    getHostPageBaseURL(): string {
        let url = location.href;
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
        request.open('GET', MappingPath.AJAX);
        // The server reads these two by their enum names
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

    close(): void {
        this.socket?.close();
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
     * that kept the event loop busy longer than the period is not a silence. A heartbeat request goes out every
     * second at most.
     */
    setHeartBeatPeriod(seconds: number): void {
        if (seconds === 0) return;
        const period = seconds * 1000;
        const timeout = period * 2;
        let lastCheck = -1;
        let lastSent = -1;
        clearInterval(this.heartbeat);
        this.heartbeat = setInterval(() => {
            const socket = this.socket;
            if (socket === undefined || socket.readyState !== WebSocket.OPEN) return;
            const now = Date.now();
            const starved = lastCheck > 0 && now - lastCheck > period;
            lastCheck = now;
            if (starved) return;
            if (now - socket.lastMessageTime > timeout) {
                socket.close(1000, `no data received since ${now - socket.lastMessageTime} ms`);
                this.reconnection?.connectionLost();
                return;
            }
            if (lastSent < now - 1000) {
                socket.send(JSON.stringify({ [C.HEARTBEAT_REQUEST]: FLAG }));
                lastSent = now;
            }
        }, 500);
    }

    onReconnected(): void {
        log.info(`Transparent reconnection succeeded for context #${this.contextId}`);
        const hook = (window as unknown as { onPonyReconnected?: () => void }).onPonyReconnected;
        if (typeof hook === 'function') hook();
    }

    buildReconnectUrl(): string {
        return this.socketUrl(`&${C.RECONNECT_UI_CONTEXT_ID}=${this.contextId}`);
    }

    reconnectSocket(url: string): void {
        this.socket = new Socket(new WebSocket(url), this.socketListener());
    }
}
