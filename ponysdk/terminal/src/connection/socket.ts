/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { log } from '../runtime/log.ts';

/** What the socket reports. */
export interface SocketListener {
    /** May come before connect() returns: when the page's socket was already open, its opening is replayed. */
    opened(socket: Socket): void;
    received(message: Uint8Array): void;
    /** Closed by the server, the network or this page; 1000 is a normal close. */
    closed(code: number): void;
}

/** What the bootstrap page records of the socket it opens while this script downloads (BootstrapServlet). */
interface EarlySocket {
    socket: WebSocket;
    opened: boolean;
    messages: ArrayBuffer[];
    closeEvent: CloseEvent | null;
}

/** The WebSocket to the server (the GWT terminal's WebSocketClient). */
export class Socket {
    private readonly socket: WebSocket;
    private readonly listener: SocketListener;
    /** When the last message arrived, or the socket opened: the heartbeat watches it. */
    lastMessageTime = -1;

    /**
     * The first connection of the main window: adopts the socket the page opened meanwhile, the server having built
     * the page in the meantime, and replays what it received; without one (a custom page), opens url. The handlers
     * are set and the past events replayed in the same task: no event can come in between.
     */
    static connect(url: string, listener: SocketListener): Socket {
        const holder = window as unknown as { ponyEarlySocket?: EarlySocket };
        const early = holder.ponyEarlySocket;
        if (early === undefined) return new Socket(new WebSocket(url), listener);
        delete holder.ponyEarlySocket;
        const socket = new Socket(early.socket, listener);
        if (early.opened) socket.onOpen();
        for (const message of early.messages) socket.onMessage(message);
        if (early.closeEvent !== null) socket.onClose(early.closeEvent);
        log.info(`WebSocket opened by the page, ${early.messages.length} messages replayed`);
        return socket;
    }

    constructor(socket: WebSocket, listener: SocketListener) {
        this.socket = socket;
        this.listener = listener;
        socket.binaryType = 'arraybuffer';
        socket.onopen = () => this.onOpen();
        socket.onclose = event => this.onClose(event);
        socket.onerror = () => log.error('WebSocket error');
        socket.onmessage = event => this.onMessage(event.data as unknown);
    }

    get readyState(): number {
        return this.socket.readyState;
    }

    /** Sent while open only: a message after close (the page unloading still reports its visibility) is lost anyway. */
    send(text: string): void {
        if (this.socket.readyState === WebSocket.OPEN) this.socket.send(text);
    }

    close(code?: number, reason?: string): void {
        this.socket.close(code, reason);
    }

    private onOpen(): void {
        this.lastMessageTime = Date.now();
        log.info('WebSocket connected');
        this.listener.opened(this);
    }

    private onClose(event: CloseEvent): void {
        log.info(`WebSocket disconnected: ${event.code}`);
        this.listener.closed(event.code);
    }

    private onMessage(data: unknown): void {
        this.lastMessageTime = Date.now();
        if (!(data instanceof ArrayBuffer)) return;
        try {
            this.listener.received(new Uint8Array(data));
        } catch (e) {
            log.error(`Error while processing a message of ${data.byteLength} bytes`, e);
        } finally {
            // A long batch is not a dead server: the heartbeat must not read the time it started
            this.lastMessageTime = Date.now();
        }
    }
}
