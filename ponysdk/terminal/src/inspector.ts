/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

// The inspector of a terminal whose server is in debug mode (ApplicationConfiguration.setDebugMode): loaded on the
// first DEBUG_INFO, a chunk of its own (docs/decisions/0004-terminal-chunks.md). It counts what the server sends to
// each object and what the terminal sends back, and shows it over the page: Alt+Shift+I, or its badge.

import { ClientToServerModel as C, ServerToClientModel as K, WidgetType } from '#model';
import type { Message } from './protocol/messages.ts';
import type { Dispatcher, Recording, Watcher } from './runtime/dispatcher.ts';
import type { TerminalObject } from './runtime/object.ts';
import { BaseObject } from './runtime/object.ts';
import { Widget } from './runtime/widget.ts';

/** What the server sent to one object, and what the terminal sent about it. */
export interface ObjectStats {
    readonly id: number;
    bytes: number;
    creates: number;
    updates: number;
    /** Messages the terminal sent about the object: its events. */
    events: number;
    collected: boolean;
}

/** The counts of the inspector, apart from the page: the Node tests drive them. */
export class Stats implements Watcher {
    framesIn = 0;
    bytesIn = 0;
    messagesOut = 0;
    bytesOut = 0;
    readonly objects = new Map<number, ObjectStats>();
    /** [time, bytes] of the server's messages in the last seconds, for the rate. */
    private readonly recent: [number, number][] = [];

    private readonly now: () => number;

    constructor(now: () => number = () => performance.now()) {
        this.now = now;
    }

    frame(bytes: number): void {
        this.framesIn++;
        this.bytesIn += bytes;
        const now = this.now();
        this.recent.push([now, bytes]);
        while (this.recent.length > 0 && this.recent[0]![0] < now - 5000) this.recent.shift();
    }

    block(instruction: number, id: number, bytes: number): void {
        const o = this.of(id);
        o.bytes += bytes;
        if (instruction === K.TYPE_CREATE) o.creates++;
        else if (instruction === K.TYPE_UPDATE) o.updates++;
        else if (instruction === K.TYPE_GC) o.collected = true;
    }

    sent(message: Message): void {
        this.messagesOut++;
        this.bytesOut += JSON.stringify(message).length;
        const id = message[C.OBJECT_ID];
        if (typeof id === 'number') this.of(id).events++;
    }

    /** Bytes per second the server sent over the last five seconds. */
    rate(): number {
        const now = this.now();
        return this.recent.filter(([t]) => t >= now - 5000).reduce((sum, [, b]) => sum + b, 0) / 5;
    }

    /** The objects that took most bytes. */
    busiest(count: number): ObjectStats[] {
        return [...this.objects.values()].sort((a, b) => b.bytes - a.bytes).slice(0, count);
    }

    /** Bytes, objects and updates by class, most bytes first; classOf names an object (its Java class when known). */
    byClass(classOf: (id: number) => string): { name: string; objects: number; bytes: number; updates: number }[] {
        const classes = new Map<string, { name: string; objects: number; bytes: number; updates: number }>();
        for (const o of this.objects.values()) {
            const name = classOf(o.id);
            let c = classes.get(name);
            if (c === undefined) classes.set(name, c = { name, objects: 0, bytes: 0, updates: 0 });
            c.objects++;
            c.bytes += o.bytes;
            c.updates += o.updates;
        }
        return [...classes.values()].sort((a, b) => b.bytes - a.bytes);
    }

    private of(id: number): ObjectStats {
        let o = this.objects.get(id);
        if (o === undefined) this.objects.set(id, o = { id, bytes: 0, creates: 0, updates: 0, events: 0, collected: false });
        return o;
    }
}

const TYPE_NAMES = new Map<number, string>(Object.entries(WidgetType).map(([name, value]) => [value, name]));

/**
 * What the inspector calls an object: its DEBUG_INFO for a widget of the server, its type for an object that is no
 * widget on the server (a tree item, a window, an addon), "object" for one the terminal created itself or one collected.
 */
export function objectName(object: TerminalObject | undefined): string {
    const info = object instanceof Widget ? object.element.getAttribute('data-pony') : null;
    if (info !== null) return info;
    return TYPE_NAMES.get(object instanceof BaseObject ? object.type : -1) ?? 'object';
}

/** "1.2 KB" */
export function size(bytes: number): string {
    return bytes < 1024 ? `${Math.round(bytes)} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB`
        : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

const CSS = `
:host { all: initial; }
.badge, .panel, .overlay, .label { font: 12px/1.4 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    color: #e5e7eb; z-index: 2147483647; position: fixed; box-sizing: border-box; }
.badge { right: 12px; bottom: 12px; background: #111827; border: 1px solid #374151; border-radius: 6px;
    padding: 4px 8px; cursor: pointer; opacity: .85; }
.badge:hover { opacity: 1; }
.panel { right: 12px; bottom: 44px; width: min(560px, calc(100vw - 24px)); max-height: 70vh; overflow: auto;
    background: #111827f2; border: 1px solid #374151; border-radius: 8px; padding: 10px; }
.panel[hidden], .overlay[hidden], .label[hidden] { display: none; }
.bar { display: flex; gap: 6px; align-items: center; margin-bottom: 8px; }
.bar strong { flex: 1; }
button { font: inherit; color: inherit; background: #1f2937; border: 1px solid #4b5563; border-radius: 4px;
    padding: 2px 8px; cursor: pointer; }
button[aria-pressed="true"] { background: #2563eb; border-color: #2563eb; }
h3 { font-size: 11px; text-transform: uppercase; letter-spacing: .05em; color: #9ca3af; margin: 10px 0 4px; }
table { width: 100%; border-collapse: collapse; }
td { padding: 1px 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 260px; }
td.n { text-align: right; color: #93c5fd; }
.totals { display: grid; grid-template-columns: repeat(3, 1fr); gap: 2px 12px; }
.totals b { color: #93c5fd; font-weight: normal; }
.overlay { pointer-events: none; outline: 2px solid #2563eb; background: #2563eb22; }
.label { pointer-events: none; background: #2563eb; color: #fff; padding: 2px 6px; border-radius: 3px; }
.selected { color: #fde68a; }
`;

/** A terminal of the page the inspector counts: the page's own, or that of an application it embeds. */
interface Counted {
    readonly dispatcher: Dispatcher;
    readonly stats: Stats;
    readonly name: string;
}

/** An object of one of the counted terminals: an id is a terminal's own. */
interface Ref {
    readonly terminal: Counted;
    readonly id: number;
}

/** The inspector over the page: a badge, a panel, and a picker that names the element under the pointer. */
class Inspector {
    private readonly terminals: Counted[] = [];
    private readonly root: ShadowRoot;
    private readonly panel: HTMLElement;
    private readonly overlay: HTMLElement;
    private readonly label: HTMLElement;
    private readonly pickButton: HTMLButtonElement;
    private readonly host: HTMLElement;
    private picking = false;
    private selected: Ref | null = null;
    private timer: ReturnType<typeof setInterval> | undefined;

    constructor() {
        const host = document.createElement('pony-inspector');
        this.host = host;
        this.root = host.attachShadow({ mode: 'open' });
        const sheet = new CSSStyleSheet();
        sheet.replaceSync(CSS);
        this.root.adoptedStyleSheets = [sheet];

        const badge = this.element('button', 'badge', 'Pony inspector');
        badge.title = 'Alt+Shift+I';
        badge.addEventListener('click', () => this.toggle());
        this.panel = this.element('section', 'panel');
        this.panel.hidden = true;
        this.panel.setAttribute('aria-label', 'Pony inspector');
        this.overlay = this.element('div', 'overlay');
        this.overlay.hidden = true;
        this.label = this.element('div', 'label');
        this.label.hidden = true;
        this.pickButton = this.element('button', '', 'Pick') as HTMLButtonElement;
        this.pickButton.setAttribute('aria-pressed', 'false');
        this.pickButton.addEventListener('click', () => this.pick(!this.picking));
        this.root.append(badge, this.panel, this.overlay, this.label);
        document.body.append(host);

        document.addEventListener('keydown', e => {
            if (e.altKey && e.shiftKey && e.code === 'KeyI') {
                e.preventDefault();
                this.toggle();
            } else if (e.key === 'Escape' && this.picking) {
                this.pick(false);
            }
        }, true);
        document.addEventListener('mousemove', e => this.picking && this.hover(e), true);
        document.addEventListener('click', e => {
            if (!this.picking || e.composedPath().includes(host)) return;
            e.preventDefault();
            e.stopPropagation();
            this.select(e);
        }, true);
    }

    /** Counts a terminal of the page from what it recorded: the first is the page's, the next ones it embeds. */
    add(dispatcher: Dispatcher, recording: Recording): Watcher {
        const stats = new Stats();
        replay(recording, stats);
        this.terminals.push({ dispatcher, stats, name: this.terminals.length === 0 ? 'page' : `embedded ${this.terminals.length}` });
        return stats;
    }

    private element(tag: string, className: string, text?: string): HTMLElement {
        const e = document.createElement(tag);
        if (className) e.className = className;
        if (text !== undefined) e.textContent = text;
        return e;
    }

    toggle(): void {
        this.panel.hidden = !this.panel.hidden;
        clearInterval(this.timer);
        if (this.panel.hidden) {
            this.pick(false);
        } else {
            this.render();
            this.timer = setInterval(() => this.render(), 500);
        }
    }

    private pick(on: boolean): void {
        this.picking = on;
        this.pickButton.setAttribute('aria-pressed', String(on));
        if (!on) this.overlay.hidden = this.label.hidden = true;
    }

    /**
     * The object an element shows: the closest element that carries an id (DEBUG_INFO), in the terminal that drew it;
     * none over the inspector, nor for an element no counted terminal has.
     */
    private objectOf(event: MouseEvent): [Ref, HTMLElement] | null {
        if (event.composedPath().includes(this.host)) return null;
        // the element itself, inside an embedded application's shadow root too: the target is its host at this level
        const element = (event.composedPath()[0] as Element).closest<HTMLElement>('[data-pony-id]');
        if (element === null) return null;
        const id = Number(element.getAttribute('data-pony-id'));
        const terminal = this.terminals.find(t => (t.dispatcher.peek(id) as Partial<Widget> | undefined)?.element === element);
        return terminal === undefined ? null : [{ terminal, id }, element];
    }

    private describe(ref: Ref): string {
        return objectName(ref.terminal.dispatcher.peek(ref.id));
    }

    /** Where the object is, when it is not the page's own terminal. */
    private where(ref: Ref): string {
        return ref.terminal === this.terminals[0] ? '' : ` · ${ref.terminal.name}`;
    }

    private hover(event: MouseEvent): void {
        const found = this.objectOf(event);
        if (found === null) {
            this.overlay.hidden = this.label.hidden = true;
            return;
        }
        const [ref, element] = found;
        const o = ref.terminal.stats.objects.get(ref.id);
        const rect = element.getBoundingClientRect();
        Object.assign(this.overlay.style, { left: `${rect.left}px`, top: `${rect.top}px`, width: `${rect.width}px`,
            height: `${rect.height}px` });
        this.label.textContent = `${element.getAttribute('data-pony')} · #${element.getAttribute('data-pony-id')}`
            + this.where(ref) + (o === undefined ? ''
            : ` · ${size(o.bytes)} in ${o.creates + o.updates} blocks · ${o.events} events`);
        Object.assign(this.label.style, { left: `${Math.max(0, rect.left)}px`,
            top: `${rect.top > 24 ? rect.top - 22 : rect.bottom + 2}px` });
        this.overlay.hidden = this.label.hidden = false;
    }

    private select(event: MouseEvent): void {
        const found = this.objectOf(event);
        this.pick(false);
        if (found === null) return;
        const [ref, element] = found;
        console.log(`[pony] #${ref.id} ${this.describe(ref)}${this.where(ref)}`, element, ref.terminal.stats.objects.get(ref.id));
        this.selected = ref;
        this.render();
    }

    private render(): void {
        const all = this.terminals;
        const sum = (f: (s: Stats) => number): number => all.reduce((total, t) => total + f(t.stats), 0);
        const objects = all.flatMap(terminal => [...terminal.stats.objects.values()].map(o => ({ terminal, o })));
        const bar = this.element('div', 'bar');
        const reset = this.element('button', '', 'Reset');
        reset.addEventListener('click', () => {
            for (const { stats } of all) {
                stats.framesIn = stats.bytesIn = stats.messagesOut = stats.bytesOut = 0;
                stats.objects.clear();
            }
            this.render();
        });
        const close = this.element('button', '', 'Close');
        close.addEventListener('click', () => this.toggle());
        bar.append(this.element('strong', '', 'Pony inspector'), this.pickButton, reset, close);

        const totals = this.element('div', 'totals');
        for (const [name, value] of [['frames in', String(sum(s => s.framesIn))], ['bytes in', size(sum(s => s.bytesIn))],
            ['rate', `${size(sum(s => s.rate()))}/s`], ['messages out', String(sum(s => s.messagesOut))],
            ['bytes out', size(sum(s => s.bytesOut))], ['objects', String(objects.filter(({ o }) => !o.collected).length)]] as const) {
            const cell = this.element('span', '', `${name} `);
            cell.append(this.element('b', '', value));
            totals.append(cell);
        }

        const parts: HTMLElement[] = [bar, totals];
        if (all.length > 1) {
            parts.push(this.element('h3', '', 'Terminals'), this.table(all.map(t => [t.name, `${t.stats.objects.size} obj`,
                `${t.stats.messagesOut} out`, size(t.stats.bytesIn)])));
        }
        if (this.selected !== null) {
            const ref = this.selected;
            const o = ref.terminal.stats.objects.get(ref.id);
            parts.push(this.element('h3', '', 'Selected'), this.element('div', 'selected', `#${ref.id} ${this.describe(ref)}`
                + this.where(ref) + (o === undefined ? ''
                : ` · ${size(o.bytes)} · ${o.creates} create, ${o.updates} updates · ${o.events} events`)));
        }
        const classes = new Map<string, { name: string; objects: number; bytes: number; updates: number }>();
        for (const terminal of all) {
            for (const c of terminal.stats.byClass(id => this.describe({ terminal, id }).split(' ')[0]!)) {
                const merged = classes.get(c.name);
                if (merged === undefined) classes.set(c.name, { ...c });
                else {
                    merged.objects += c.objects;
                    merged.bytes += c.bytes;
                    merged.updates += c.updates;
                }
            }
        }
        parts.push(this.element('h3', '', 'Bytes by class'), this.table([...classes.values()].sort((a, b) => b.bytes - a.bytes)
            .slice(0, 10).map(c => [c.name, `${c.objects} obj`, `${c.updates} upd`, size(c.bytes)])));
        parts.push(this.element('h3', '', 'Busiest objects'), this.table(objects.sort((a, b) => b.o.bytes - a.o.bytes).slice(0, 10)
            .map(({ terminal, o }) => {
                const ref = { terminal, id: o.id };
                return [`#${o.id} ${this.describe(ref)}${this.where(ref)}`, `${o.updates} upd`, `${o.events} ev`, size(o.bytes)];
            })));
        this.panel.replaceChildren(...parts);
    }

    private table(rows: string[][]): HTMLElement {
        const table = this.element('table', '');
        for (const row of rows) {
            const tr = this.element('tr', '');
            row.forEach((text, i) => tr.append(this.element('td', i === 0 ? '' : 'n', text)));
            table.append(tr);
        }
        return table;
    }
}

/** Gives a watcher what the dispatcher recorded since the first DEBUG_INFO. */
export function replay(recording: Recording, watcher: Watcher): void {
    for (const event of recording.events) {
        if (!Array.isArray(event)) watcher.sent(event as Message);
        else if (event[0] === -1) watcher.frame(event[2]);
        else watcher.block(event[0], event[1], event[2]);
    }
}

/** The page's inspector, created by its first terminal in debug mode. */
let inspector: Inspector | null = null;

/** Takes over from the recording the dispatcher kept since the first DEBUG_INFO: one inspector counts every terminal. */
export function install(dispatcher: Dispatcher, recording: Recording): void {
    inspector ??= new Inspector();
    dispatcher.watcher = inspector.add(dispatcher, recording);
}
