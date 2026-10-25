/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, WidgetType as W } from '#model';
import { adaptStylesheet } from './embedded-styles.ts';
import { HostObject } from './objects/host.ts';
import { ParentBridge } from './parent-bridge.ts';
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
    /** GWT's class names: the legacy layer's script, null without (ADR-0009, decision 8). */
    legacy: string | null;
    /** The application's terminal, and the version of mountPonyApp's contract it has (ADR-0009, decision 10). */
    terminal?: string;
    mount?: number;
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

/** The legacy layer, once per page, told not to start on the host's document: each element gives it its container. */
function loadLegacy(url: string): Promise<void> {
    let loading = scripts.get(url);
    if (loading === undefined) {
        loading = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = url;
            script.setAttribute('data-pony-legacy', 'element');
            script.onload = () => resolve();
            script.onerror = () => reject(new Error(`${url} did not load`));
            document.head.append(script);
        });
        scripts.set(url, loading);
    }
    return loading;
}

/**
 * The application's scripts, ponysdk.js first unless the page has it, then ponysdk.js is handed the page's addon
 * registry as window.pony, unless a page terminal already is.
 */
async function loadApplication(manifest: Manifest, base: string): Promise<void> {
    const globals = globalThis as PonyGlobals;
    const [ponysdk, ...others] = manifest.scripts.map(script => new URL(script, base).href);
    const loaded = typeof globals.onPonySDKModuleLoaded === 'function';
    if (loaded && ponysdk !== undefined && ![...document.scripts].some(script => script.src === ponysdk)) {
        // Its globals are the addons' API: one per page (ADR-0009, decision 10)
        log.warn(`${ponysdk} not loaded: the page's ponysdk.js serves the addons of every application`);
    }
    await loadScripts(loaded || ponysdk === undefined ? others : [ponysdk, ...others]);
    if (manifest.legacy !== null) await loadLegacy(new URL(manifest.legacy, base).href);
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
 * A stylesheet of the application, read with CORS so that it is adapted to the element once loaded (ADR-0009,
 * decision 4); one the CORS refuses loads again as it is, unadapted.
 */
function stylesheet(href: string, id: string, origin: string, root: string): HTMLLinkElement {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.id = id;
    link.crossOrigin = new URL(href).origin === origin ? 'use-credentials' : 'anonymous';
    link.href = href;
    link.addEventListener('load', () => {
        if (link.sheet !== null) adaptStylesheet(link.sheet, root);
    }, { once: true });
    link.addEventListener('error', () => {
        if (!link.isConnected) return;
        log.warn(`${href}: refused with CORS, loaded as it is: written for a page, not adapted to the element`);
        const plain = document.createElement('link');
        plain.rel = 'stylesheet';
        plain.id = id;
        plain.href = href;
        link.replaceWith(plain);
    }, { once: true });
    return link;
}

/** The terminal of this element's module: an application whose manifest names another runs on that one. */
const OWN_TERMINAL = new URL('../terminal.js', import.meta.url).href;

/** The contract of mount 1 (ADR-0009, decision 10), frozen: what terminal.js of every version exports. */
type MountPonyApp = (element: HTMLElement, src: string) => Promise<{ stop(): void }>;

/** What a host page may give an element: its credential. */
type HostElement = HTMLElement & { credential?: (() => string | Promise<string>) | undefined };

/**
 * An application in an element: run by this module's terminal, or, when the application's manifest names the terminal
 * of another PonySDK version (another URL), by that one, which the element hands itself to (mountPonyApp).
 */
class ElementApplication {
    private readonly element: HostElement;
    private readonly src: () => string | null;
    /** Whether an application of another terminal is handed to it: false in the terminal it is handed to. */
    private readonly delegating: boolean;
    private terminal: Terminal | undefined;
    /** The application of another terminal, running. */
    private delegate: { stop(): void } | undefined;
    private lifetime: AbortController | undefined;
    private body: HTMLElement | undefined;
    /** Started, or starting: the manifest and the scripts load before the terminal. */
    active = false;
    /** Bumped by each stop: a start that awaited meanwhile gives up. */
    private generation = 0;
    private restartScheduled = false;

    constructor(element: HostElement, src: () => string | null, delegating: boolean) {
        this.element = element;
        this.src = src;
        this.delegating = delegating;
    }

    get running(): boolean {
        return this.terminal !== undefined || this.delegate !== undefined;
    }

    /** Starts the application again on a new context, out of the current task (it may be the terminal's). */
    restartLater(): void {
        if (this.restartScheduled) return;
        this.restartScheduled = true;
        setTimeout(() => {
            this.restartScheduled = false;
            this.stop();
            if (this.element.isConnected) void this.start();
        });
    }

    async start(): Promise<void> {
        const src = this.src();
        if (src === null || src === '') return;
        this.active = true;
        const generation = this.generation;
        const url = new URL(src, document.baseURI);
        if (!url.pathname.endsWith('/')) url.pathname += '/'; // the application's path
        const base = new URL('.', url).href;
        try {
            const manifest = await manifestOf(base);
            const terminal = manifest.terminal === undefined ? OWN_TERMINAL : new URL(manifest.terminal, base).href;
            if (this.delegating && terminal !== OWN_TERMINAL) {
                await this.handOver(terminal, src, generation);
                return;
            }
            await loadApplication(manifest, base);
            const credential = await this.credentialForStart();
            if (generation !== this.generation || !this.element.isConnected) return;
            this.run(url, base, manifest, credential);
        } catch (e) {
            log.error(`<pony-app src="${src}"> cannot start`, e);
            this.active = false;
        }
    }

    /** Another PonySDK's application: its own terminal runs it in this element (ADR-0009, decision 10). */
    private async handOver(terminal: string, src: string, generation: number): Promise<void> {
        const other = await import(terminal) as { mountPonyApp?: MountPonyApp };
        if (typeof other.mountPonyApp !== 'function') throw new Error(`${terminal} runs no application in an element of another version`);
        if (generation !== this.generation || !this.element.isConnected) return;
        const delegate = await other.mountPonyApp(this.element, src);
        if (generation === this.generation) this.delegate = delegate;
        else delegate.stop(); // stopped meanwhile
    }

    /** The host's credential for a new context; none when the host gave none, or its function failed. */
    private async credentialForStart(): Promise<string | null> {
        const credential = this.element.credential;
        if (typeof credential !== 'function') return null;
        try {
            const value = await credential.call(this.element);
            return typeof value === 'string' ? value : null;
        } catch (e) {
            log.error('<pony-app>: the credential function failed, the application starts without one', e);
            return null;
        }
    }

    private run(url: URL, base: string, manifest: Manifest, credential: string | null): void {
        const element = this.element;
        const shadow = element.getAttribute('isolation') !== 'none';
        const body = document.createElement('div');
        body.setAttribute('data-pony-body', '');
        body.setAttribute('part', 'body');
        const stylesheets = manifest.styles.map(({ id, href }) => stylesheet(new URL(href, base).href, id, url.origin,
            shadow ? ':host' : 'pony-app'));
        let byId: (id: string) => HTMLElement | null;
        if (shadow) {
            const root = element.shadowRoot ?? element.attachShadow({ mode: 'open' });
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
            if (element.shadowRoot !== null) { // isolation was shadow: the shadow root only shows the element's children
                element.shadowRoot.adoptedStyleSheets = [];
                element.shadowRoot.replaceChildren(document.createElement('slot'));
            }
            element.append(body);
            byId = id => document.getElementById(id);
        }

        const lifetime = new AbortController();
        const attributes = hostAttributes(element);
        const notify = (type: string, detail?: unknown) =>
            element.dispatchEvent(new CustomEvent(type, { detail, bubbles: true, composed: true }));
        const embedding: Embedding = {
            element, attributes, restart: () => this.restartLater(), notify,
            get children() {
                return children; // made with the terminal, used once it runs
            },
        };
        const mount: Mount = {
            url,
            body,
            byId,
            history: element.getAttribute('history') === 'hash' ? new BrowserHistory(lifetime.signal) : new MemoryHistory(url.hash),
            embedding,
            transparentReconnection: manifest.reconnection,
            handshake: `&${C.HOST_ATTRIBUTES}=${encodeURIComponent(JSON.stringify(attributes))}`,
            credential,
            legacy: manifest.legacy !== null,
            signal: lifetime.signal,
            connectionLost: () => {
                notify('pony-connection-lost');
                return false; // the application's #reconnection notice, if it has one
            },
            reconnected: () => notify('pony-reconnected'),
            serverBack: () => this.restartLater(),
            reload: () => this.restartLater(),
        };
        this.lifetime = lifetime;
        this.body = body;
        this.terminal = new Terminal(mount);
        const children = new ParentBridge(this.terminal, url.origin, lifetime.signal);
        if (mount.legacy) (window as unknown as { ponyLegacy?: { observe(root: Element): void } }).ponyLegacy?.observe(body);
        this.terminal.start();
    }

    /** The application goes: its context is destroyed (ADR-0005), its objects release the page; the children show. */
    stop(): void {
        this.generation++;
        this.active = false;
        this.delegate?.stop();
        this.delegate = undefined;
        this.terminal?.dispose();
        this.terminal = undefined;
        this.lifetime?.abort();
        this.lifetime = undefined;
        this.body?.remove();
        this.body = undefined;
        this.element.shadowRoot?.replaceChildren(document.createElement('slot'));
    }
}

/**
 * <pony-app src="…">: a PonySDK application in another page (docs/decisions/0008-embedded-application.md). Each
 * element runs a terminal of its own, on a context of its own: that of the application's PonySDK version, which may
 * not be the one that defined the element (ADR-0009).
 * - src: the application's URL. Its query gives the application's parameters, its fragment the first history token.
 * - isolation: shadow (the default), the application renders in a shadow root; none, in the page.
 * - history: memory (the default), the token stays in the element; hash, the page's fragment is the application's.
 * Its other attributes are the application's (PHost). Its children show until the application's first widget. It
 * dispatches pony-connection-lost and pony-reconnected; pony-title and pony-location, the window title and the
 * location the application asks for (ADR-0009).
 * - credential, a property: a function returning the credential of the user (a JWT, an OAuth access token) or a
 *   promise of it, called before each start; the application reads it with PHost.getCredential()
 *   (docs/decisions/0009-embedding-limits.md). A property, never an attribute: the DOM is every script's to read.
 */
export class PonyApp extends HTMLElement {
    static readonly observedAttributes = ['src', 'isolation', 'history'];

    // Declared only: a field would replace the value a host set before the element was defined
    declare credential: (() => string | Promise<string>) | undefined;

    private readonly application = new ElementApplication(this, () => this.getAttribute('src'), true);

    connectedCallback(): void {
        if (!this.application.active) void this.application.start(); // a move puts the element back at once: it keeps running
    }

    /** Stops if still out of the page at the next task: a move removes the element and puts it back at once. */
    disconnectedCallback(): void {
        setTimeout(() => {
            if (!this.isConnected) this.application.stop();
        });
    }

    /** A running application starts again; one still starting reads isolation and history when it runs. */
    attributeChangedCallback(name: string, previous: string | null, value: string | null): void {
        if (previous === value || !this.isConnected) return;
        const application = this.application;
        if (application.running || (application.active && name === 'src')) application.restartLater();
        else if (!application.active) void application.start();
    }
}

/**
 * Runs the application of src in a <pony-app> element that another PonySDK version defined: the contract of mount 1
 * (ADR-0009, decision 10), which terminal.js exports. This version's terminals create PHost's object.
 */
export async function mountPonyApp(element: HTMLElement, src: string): Promise<{ stop(): void }> {
    defineCreator(W.HOST, HostObject.create);
    const application = new ElementApplication(element, () => src, false);
    await application.start();
    return { stop: () => application.stop() };
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
