/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import type { Context, TerminalObject } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';

interface Tab {
    readonly widget: Widget;
    readonly tab: HTMLButtonElement;
    readonly panel: HTMLDivElement;
}

let tabIds = 0;

/**
 * PTabPanel: a tab list and one panel per widget, on the WAI-ARIA tabs pattern: arrow keys, Home and End move the
 * selection. The first tab added is selected. A selection, by the user or by the server (SELECTED_INDEX), sends
 * HANDLER_BEFORE_SELECTION then HANDLER_SELECTION to the server when it listens, as GWT's TabBar fires both.
 */
export class TabPanel extends Widget {
    private readonly list: HTMLDivElement;
    private readonly tabs: Tab[] = [];
    private selected = -1;
    private listensBeforeSelection = false;
    private listensSelection = false;

    static create(context: Context, id: number, type: number): TabPanel {
        return new TabPanel(context, id, type);
    }

    private constructor(context: Context, id: number, type: number) {
        const list = document.createElement('div');
        list.setAttribute('role', 'tablist');
        const element = document.createElement('div');
        element.append(list);
        super(context, id, type, element, 'pony-TabPanel');
        this.list = list;
        this.listen<KeyboardEvent>('keydown', e => this.onArrow(e), list);
    }

    /** TAB_TEXT or TAB_WIDGET, then BEFORE_INDEX when given. */
    override add(reader: Reader, child: TerminalObject): void {
        if (!(child instanceof Widget)) return;
        const tab = document.createElement('button');
        tab.type = 'button';
        tab.setAttribute('role', 'tab');
        tab.id = `pony-tab-${++tabIds}`;
        const panel = document.createElement('div');
        panel.setAttribute('role', 'tabpanel');
        panel.setAttribute('aria-labelledby', tab.id);
        panel.id = `${tab.id}-panel`;
        tab.setAttribute('aria-controls', panel.id);

        const key = reader.read();
        let tabWidget: Widget | undefined;
        if (key === K.TAB_TEXT) tab.textContent = reader.str;
        else if (key === K.TAB_WIDGET) {
            const found = this.context.object(reader.int);
            if (found instanceof Widget) tabWidget = found;
        }
        let index = this.tabs.length;
        if (reader.read() === K.BEFORE_INDEX) index = Math.min(reader.int, this.tabs.length);
        else reader.rewind();

        this.list.insertBefore(tab, this.tabs[index]?.tab ?? null);
        this.element.insertBefore(panel, this.tabs[index]?.panel ?? null);
        this.tabs.splice(index, 0, { widget: child, tab, panel });
        if (index <= this.selected) this.selected++;
        if (tabWidget !== undefined) this.appendInside(tab, tabWidget);
        this.appendInside(panel, child);
        this.listen('click', () => this.select(this.tabs.findIndex(t => t.tab === tab)), tab);
        this.refresh();
        if (this.tabs.length === 1) this.select(0);
    }

    override remove(_reader: Reader, child: TerminalObject): void {
        const index = this.tabs.findIndex(t => t.widget === child);
        if (index === -1) return;
        const [removed] = this.tabs.splice(index, 1);
        for (const widget of [...this.children]) {
            if (removed!.tab.contains(widget.element) || removed!.panel.contains(widget.element)) this.removeChild(widget);
        }
        removed!.tab.remove();
        removed!.panel.remove();
        if (this.selected === index) this.selected = -1;
        else if (this.selected > index) this.selected--;
        this.refresh();
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.SELECTED_INDEX) {
            this.select(reader.int);
            return true;
        }
        if (reader.key === K.ANIMATION) return true;
        return super.update(reader);
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_SELECTION) this.listensSelection = true;
        else if (handler === H.HANDLER_BEFORE_SELECTION) this.listensBeforeSelection = true;
        else super.addHandler(reader, handler);
    }

    private select(index: number): void {
        if (index < 0 || index >= this.tabs.length) return;
        if (this.listensBeforeSelection) this.sendSelection(C.HANDLER_BEFORE_SELECTION, index);
        this.selected = index;
        this.refresh();
        if (this.listensSelection) this.sendSelection(C.HANDLER_SELECTION, index);
    }

    private sendSelection(key: string, index: number): void {
        const message = about(this.id);
        message[key] = index;
        this.context.send(message);
    }

    private refresh(): void {
        this.tabs.forEach(({ tab, panel }, i) => {
            const selected = i === this.selected;
            tab.setAttribute('aria-selected', String(selected));
            tab.tabIndex = selected || (this.selected === -1 && i === 0) ? 0 : -1;
            panel.hidden = !selected;
        });
    }

    private onArrow(event: KeyboardEvent): void {
        const current = this.tabs.findIndex(t => t.tab === document.activeElement);
        if (current === -1) return;
        const last = this.tabs.length - 1;
        const next = event.key === 'ArrowRight' ? (current === last ? 0 : current + 1)
            : event.key === 'ArrowLeft' ? (current === 0 ? last : current - 1)
            : event.key === 'Home' ? 0 : event.key === 'End' ? last : -1;
        if (next === -1) return;
        event.preventDefault();
        this.tabs[next]!.tab.focus();
        this.select(next);
    }
}
