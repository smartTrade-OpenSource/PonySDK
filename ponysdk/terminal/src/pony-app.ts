/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, WidgetType as W } from '#model';
import { HostObject } from './objects/host.ts';
import { registerAddOnFactory } from './runtime/dispatcher.ts';
import { type Embedding, hostAttributes } from './runtime/embedding.ts';
import { defineCreator } from './runtime/factory.ts';
import { BrowserHistory } from './runtime/history.ts';
import { log } from './runtime/log.ts';
import { MemoryHistory } from './runtime/memory-history.ts';
import type { Mount } from './runtime/mount.ts';
import { adoptStyles, installStyles } from './runtime/styles.ts';
import { Terminal } from './terminal.ts';

/** What the application's bootstrap page loads: its ponyapp.json (BootstrapServlet.buildManifest). */
interface Manifest {
    styles: { id: string; href: string }[];
    /** ponysdk.js first. */
    scripts: string[];
    sharedSheets: string[];
    reconnection: boolean;
}

/** What the page's scripts reach as window.pony when no page terminal runs: the addons' registry. */
interface PageApi {
    registerAddOnFactory: typeof registerAddOnFactory;
    start(): void;
    sendDataToServer(): void;
}

type PonyGlobals = typeof globalThis & {
    onPonySDKModuleLoaded?: (pony: PageApi) => void;
};

type PonyDocument = Document & {
    ponyLoaded?: boolean;
    onPonyLoaded?: (callback: (pony: { wc: { registerSharedSheet(href: string): void } }) => void) => void;
};

/** Once per page: the manifests by application, the scripts and stylesheets by URL. */
const manifests = new Map<string, Promise<Manifest>>();
const scripts = new Map<string, Promise<void>>();
const pageStyles = new Set<string>();

/** In the shadow root: the element is a block, its container shows once the application has put something in it. */
const SHADOW_RULES = ':host{display:block;position:relative}[data-pony-body]{position:relative;height:100%}'
    + '[data-pony-body]:not(:empty)+slot{display:none}';
/** In the page, for isolation="none": the same, the host's children hidden instead of a slot. */
const PAGE_RULES = 'pony-app{display:block;position:relative}pony-app>[data-pony-body]{position:relative;height:100%}'
    + 'pony-app:has(>[data-pony-body]:not(:empty))>:not([data-pony-body]){display:none}';

function manifestOf(base: string): Promise<Manifest> {
    let manifest = manifests.get(base);
    if (manifest === undefined) {
        manifest = fetch(`${base}ponyapp.json`, { credentials: 'include' }).then(response => {
            if (!response.ok) throw new Error(`${base}ponyapp.json: HTTP ${response.status}`);
            return response.json() as Promise<Manifest>;
        });
        manifest.catch(() => manifests.delete(base)); // tried again by the next element
        manifests.set(base, manifest);
    }
    return manifest;
}

/**
 * Classic scripts, as the bootstrap page runs them: downloaded together, run in order, once per page. Those the page
 * already has (the application's own page, embedding another) do not run again.
 */
function loadScripts(urls: string[]): Promise<void> {
    return Promise.all(urls.map(url => {
        let loading = scripts.get(url);
        if (loading === undefined && [...document.scripts].some(script => script.src === url)) {
            loading = Promise.resolve();
            scripts.set(url, loading);
        }
        if (loading === undefined) {
            loading = new Promise((resolve, reject) => {
                const script = document.createElement('script');
                script.src = url;
                script.async = false;
                script.onload = () => resolve();
                script.onerror = () => reject(new Error(`${url} did not load`));
                document.head.append(script);
            });
            scripts.set(url, loading);
        }
        return loading;
    })).then(() => undefined);
}

/**
 * The application's scripts, ponysdk.js first unless the page has it, then ponysdk.js is handed the page's addon
 * registry as window.pony, unless a page terminal already is.
 */
async function loadApplication(manifest: Manifest, base: string): Promise<void> {
    const globals = globalThis as PonyGlobals;
    const [ponysdk, ...others] = manifest.scripts.map(script => new URL(script, base).href);
    await loadScripts(typeof globals.onPonySDKModuleLoaded === 'function' || ponysdk === undefined ? others : [ponysdk, ...others]);
    const page = document as PonyDocument;
    for (const sheet of manifest.sharedSheets) {
        const href = new URL(sheet, base).href;
        page.onPonyLoaded?.(pony => pony.wc.registerSharedSheet(href));
    }
    if (page.ponyLoaded !== true && typeof globals.onPonySDKModuleLoaded === 'function') {
        globals.onPonySDKModuleLoaded({
            registerAddOnFactory,
            start(): void {
                // each <pony-app> element starts its own terminal
            },
            sendDataToServer(): void {
                log.warn('window.pony is the page\'s addon registry: an addon sends through its own terminal');
            },
        });
    }
}

/**
 * <pony-app src="…">: a PonySDK application in another page (docs/decisions/0008-embedded-application.md). Each
 * element runs a terminal of its own, on a context of its own.
 * - src: the application's URL. Its query gives the application's parameters, its fragment the first history token.
 * - isolation: shadow (the default), the application renders in a shadow root; none, in the page.
 * - history: memory (the default), the token stays in the element; hash, the page's fragment is the application's.
 * Its other attributes are the application's (PHost). Its children show until the application's first widget. It
 * dispatches pony-connection-lost and pony-reconnected.
 */
export class PonyApp extends HTMLElement {
    static readonly observedAttributes = ['src', 'isolation', 'history'];

    private terminal: Terminal | undefined;
    private lifetime: AbortController | undefined;
    private body: HTMLElement | undefined;
    /** Started, or starting: the manifest and the scripts load before the terminal. */
    private active = false;
    /** Bumped by each stop: a start that awaited meanwhile gives up. */
    private generation = 0;
    private restartScheduled = false;

    connectedCallback(): void {
        if (!this.active) void this.start(); // a move puts the element back at once: it keeps running
    }

    /** Stops if still out of the page at the next task: a move removes the element and puts it back at once. */
    disconnectedCallback(): void {
        setTimeout(() => {
            if (!this.isConnected) this.stop();
        });
    }

    /** A running application starts again; one still starting reads isolation and history when it runs. */
    attributeChangedCallback(name: string, previous: string | null, value: string | null): void {
        if (previous === value || !this.isConnected) return;
        if (this.terminal !== undefined || (this.active && name === 'src')) this.restartLater();
        else if (!this.active) void this.start();
    }

    /** Starts the application again on a new context, out of the current task (it may be the terminal's). */
    private restartLater(): void {
        if (this.restartScheduled) return;
        this.restartScheduled = true;
        setTimeout(() => {
            this.restartScheduled = false;
            this.stop();
            if (this.isConnected) void this.start();
        });
    }

    private async start(): Promise<void> {
        const src = this.getAttribute('src');
        if (src === null || src === '') return;
        this.active = true;
        const generation = this.generation;
        const url = new URL(src, document.baseURI);
        if (!url.pathname.endsWith('/')) url.pathname += '/'; // the application's path
        const base = new URL('.', url).href;
        try {
            const manifest = await manifestOf(base);
            await loadApplication(manifest, base);
            if (generation !== this.generation || !this.isConnected) return;
            this.run(url, base, manifest);
        } catch (e) {
            log.error(`<pony-app src="${src}"> cannot start`, e);
            this.active = false;
        }
    }

    private run(url: URL, base: string, manifest: Manifest): void {
        const shadow = this.getAttribute('isolation') !== 'none';
        const body = document.createElement('div');
        body.setAttribute('data-pony-body', '');
        body.setAttribute('part', 'body');
        const stylesheets = manifest.styles.map(({ id, href }) => {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.id = id;
            link.href = new URL(href, base).href;
            return link;
        });
        let byId: (id: string) => HTMLElement | null;
        if (shadow) {
            const root = this.shadowRoot ?? this.attachShadow({ mode: 'open' });
            adoptStyles(root);
            const rules = document.createElement('style');
            rules.textContent = SHADOW_RULES;
            root.replaceChildren(rules, ...stylesheets, body, document.createElement('slot'));
            byId = id => root.getElementById(id);
        } else {
            installStyles();
            if (!pageStyles.has(PAGE_RULES)) {
                pageStyles.add(PAGE_RULES);
                const rules = document.createElement('style');
                rules.textContent = PAGE_RULES;
                document.head.append(rules);
            }
            for (const link of stylesheets) {
                if (pageStyles.has(link.href)) continue;
                pageStyles.add(link.href);
                document.head.append(link);
            }
            if (this.shadowRoot !== null) { // isolation was shadow: the shadow root only shows the element's children
                this.shadowRoot.adoptedStyleSheets = [];
                this.shadowRoot.replaceChildren(document.createElement('slot'));
            }
            this.append(body);
            byId = id => document.getElementById(id);
        }

        const lifetime = new AbortController();
        const attributes = hostAttributes(this);
        const embedding: Embedding = { element: this, attributes, restart: () => this.restartLater() };
        const events = (type: string) => this.dispatchEvent(new CustomEvent(type, { bubbles: true, composed: true }));
        const mount: Mount = {
            url,
            body,
            byId,
            history: this.getAttribute('history') === 'hash' ? new BrowserHistory(lifetime.signal) : new MemoryHistory(url.hash),
            embedding,
            transparentReconnection: manifest.reconnection,
            handshake: `&${C.HOST_ATTRIBUTES}=${encodeURIComponent(JSON.stringify(attributes))}`,
            signal: lifetime.signal,
            connectionLost: () => {
                events('pony-connection-lost');
                return false; // the application's #reconnection notice, if it has one
            },
            reconnected: () => events('pony-reconnected'),
            serverBack: () => this.restartLater(),
            reload: () => this.restartLater(),
        };
        this.lifetime = lifetime;
        this.body = body;
        this.terminal = new Terminal(mount);
        this.terminal.start();
    }

    /** The application goes: its context is destroyed (ADR-0005), its objects release the page; the children show. */
    private stop(): void {
        this.generation++;
        this.active = false;
        this.terminal?.dispose();
        this.terminal = undefined;
        this.lifetime?.abort();
        this.lifetime = undefined;
        this.body?.remove();
        this.body = undefined;
        this.shadowRoot?.replaceChildren(document.createElement('slot'));
    }
}

/**
 * Defines <pony-app>, once per page: two applications' terminals may both be in it. Its terminals create PHost's
 * object, which comes with it.
 */
export function definePonyApp(): void {
    if (customElements.get('pony-app') !== undefined) return;
    defineCreator(W.HOST, HostObject.create);
    customElements.define('pony-app', PonyApp);
}
