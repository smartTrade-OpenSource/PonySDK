/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { ClientToServerModel as C, HandlerModel as H, ServerToClientModel as K } from '#model';
import { about } from '../protocol/messages.ts';
import type { Reader } from '../protocol/reader.ts';
import { BaseObject, type Context } from '../runtime/object.ts';
import { Widget } from '../runtime/widget.ts';
import { POPUP_CLASS } from './popup.ts';

interface Suggestion {
    /** HTML: the matched word starts in strong. */
    display: string;
    replacement: string;
}

function escape(text: string): string {
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * PMultiWordSuggestOracle, as GWT's MultiWordSuggestOracle: a suggestion matches when each word of the query starts
 * one of its words, case aside. Matches come sorted, the matched starts in strong; for a given lower-cased text, the
 * suggestion added last is the one shown. As GWT, one more than the limit is kept.
 */
export class SuggestOracle extends BaseObject {
    private readonly realSuggestions = new Map<string, string[]>();
    /** Candidates (lower-cased suggestions) by word. */
    private readonly candidatesByWord = new Map<string, Set<string>>();

    override update(reader: Reader): boolean {
        if (reader.key === K.SUGGESTION) {
            this.addSuggestion(reader.str ?? '');
            return true;
        }
        if (reader.key === K.CLEAR) {
            this.realSuggestions.clear();
            this.candidatesByWord.clear();
            return true;
        }
        return super.update(reader);
    }

    addSuggestion(suggestion: string): void {
        const candidate = suggestion.toLowerCase();
        let real = this.realSuggestions.get(candidate);
        if (real === undefined) this.realSuggestions.set(candidate, real = []);
        real.unshift(suggestion);
        for (const word of candidate.split(' ')) {
            let candidates = this.candidatesByWord.get(word);
            if (candidates === undefined) this.candidatesByWord.set(word, candidates = new Set());
            candidates.add(candidate);
        }
    }

    suggest(rawQuery: string, limit: number): Suggestion[] {
        const query = rawQuery.toLowerCase().replace(/\s+/g, ' ').trim();
        const words = query.split(' ');
        const candidates = this.candidates(query, words);
        for (let i = candidates.length - 1; i > limit; i--) candidates.splice(i, 1);
        const suggestions: Suggestion[] = [];
        for (const candidate of candidates) {
            const real = this.realSuggestions.get(candidate)![0]!;
            let display = '';
            let cursor = 0;
            let index = 0;
            for (;;) {
                const bounds = nextWord(candidate, words, index);
                if (bounds === null) break;
                if (bounds[0] === 0 || candidate.charAt(bounds[0] - 1) === ' ') {
                    display += `${escape(real.substring(cursor, bounds[0]))}<strong>${escape(real.substring(bounds[0], bounds[1]))}</strong>`;
                    cursor = bounds[1];
                }
                index = bounds[1];
            }
            if (cursor === 0) continue;
            suggestions.push({ display: display + escape(real.substring(cursor)), replacement: real });
        }
        return suggestions;
    }

    /** The candidates holding a word that each query word starts, sorted. */
    private candidates(query: string, words: string[]): string[] {
        if (query.length === 0) return [];
        let found: Set<string> | null = null;
        for (const word of words) {
            if (word.length === 0) continue;
            const forWord = new Set<string>();
            for (const [known, candidates] of this.candidatesByWord) {
                if (known.startsWith(word)) for (const c of candidates) forWord.add(c);
            }
            if (found === null) found = forWord;
            else {
                for (const c of found) if (!forWord.has(c)) found.delete(c);
                if (found.size < 2) break;
            }
        }
        return found === null ? [] : [...found].sort();
    }
}

/** The first occurrence of a query word from index, the longest when two start at the same place. */
function nextWord(candidate: string, words: string[], from: number): [number, number] | null {
    let first: [number, number] | null = null;
    for (const word of words) {
        const index = candidate.indexOf(word, from);
        if (index === -1) continue;
        const bounds: [number, number] = [index, index + word.length];
        if (first === null || bounds[0] < first[0] || (bounds[0] === first[0] && bounds[1] > first[1])) first = bounds;
    }
    return first;
}

let listIds = 0;

/**
 * PSuggestBox, on the WAI-ARIA combobox pattern: its text box (TEXTBOX_ID) with a list of suggestions (role listbox)
 * in a popover under it, from its oracle (ORACLE), refreshed after each key that changed the text. The first
 * suggestion is selected; Up and Down move, Enter or Tab take it, a click too. Taking a suggestion puts its
 * replacement in the box and sends HANDLER_STRING_SELECTION with the display and the replacement strings.
 */
export class SuggestBox extends Widget {
    private readonly oracle: SuggestOracle;
    private readonly input: HTMLInputElement;
    private readonly list: HTMLDivElement;
    private suggestions: Suggestion[] = [];
    private current = -1;
    private currentText: string | null = null;
    private limit = 20;
    private sendValueChange = false;
    private sendSelection = false;

    static create(context: Context, id: number, type: number, reader: Reader): SuggestBox {
        reader.read(); // ORACLE
        const oracle = context.object(reader.int);
        reader.read(); // TEXTBOX_ID
        const box = context.object(reader.int);
        if (!(oracle instanceof SuggestOracle) || !(box instanceof Widget)) throw new Error(`Suggest box #${id}: no oracle or text box`);
        return new SuggestBox(context, id, type, box, oracle);
    }

    private constructor(context: Context, id: number, type: number, box: Widget, oracle: SuggestOracle) {
        super(context, id, type, box.element, 'pony-SuggestBox');
        this.children.push(box);
        this.adopt(box);
        this.oracle = oracle;
        this.input = box.element as HTMLInputElement;
        this.list = document.createElement('div');
        this.list.setAttribute('popover', 'manual');
        this.list.setAttribute('role', 'listbox');
        this.list.id = `pony-suggestions-${++listIds}`;
        this.list.className = `${POPUP_CLASS} pony-SuggestPopup`;
        document.body.append(this.list);
        this.input.setAttribute('role', 'combobox');
        this.input.setAttribute('aria-autocomplete', 'list');
        this.input.setAttribute('aria-expanded', 'false');
        this.input.setAttribute('aria-controls', this.list.id);
        this.listen<KeyboardEvent>('keydown', e => this.onKeyDown(e), this.input);
        this.listen('keyup', () => this.refresh(), this.input);
        this.listen('change', () => {
            if (!this.sendValueChange) return;
            const message = about(this.id);
            message[C.HANDLER_STRING_VALUE_CHANGE] = this.input.value;
            this.context.send(message);
        }, this.input);
        this.listen<PointerEvent>('pointerdown', e => {
            const target = e.target as Node;
            if (!this.list.contains(target) && target !== this.input) this.hideList();
        }, document);
        this.listen<MouseEvent>('click', e => {
            const option = (e.target as Element).closest('[role=option]');
            if (option !== null) this.take(Number((option as HTMLElement).dataset['index']));
        }, this.list);
    }

    override update(reader: Reader): boolean {
        if (reader.key === K.LIMIT) {
            this.limit = reader.int;
            return true;
        }
        return super.update(reader);
    }

    override addHandler(reader: Reader, handler: number): void {
        if (handler === H.HANDLER_STRING_VALUE_CHANGE) this.sendValueChange = true;
        else if (handler === H.HANDLER_STRING_SELECTION) this.sendSelection = true;
        else super.addHandler(reader, handler);
    }

    private refresh(): void {
        const text = this.input.value;
        if (text === this.currentText) return;
        this.currentText = text;
        this.suggestions = this.oracle.suggest(text, this.limit);
        if (this.suggestions.length === 0) {
            this.hideList();
            return;
        }
        this.list.replaceChildren(...this.suggestions.map((suggestion, i) => {
            const option = document.createElement('div');
            option.setAttribute('role', 'option');
            option.id = `${this.list.id}-${i}`;
            option.dataset['index'] = String(i);
            option.innerHTML = suggestion.display;
            return option;
        }));
        this.select(0);
        const r = this.input.getBoundingClientRect();
        this.list.style.left = `${r.left + window.scrollX}px`;
        this.list.style.top = `${r.bottom + window.scrollY}px`;
        if (!this.list.matches(':popover-open')) this.list.showPopover();
        this.input.setAttribute('aria-expanded', 'true');
    }

    private select(index: number): void {
        this.current = index;
        const options = this.list.children;
        for (let i = 0; i < options.length; i++) options[i]!.setAttribute('aria-selected', String(i === index));
        const option = options[index];
        if (option !== undefined) this.input.setAttribute('aria-activedescendant', option.id);
        else this.input.removeAttribute('aria-activedescendant');
    }

    private get shown(): boolean {
        return this.list.matches(':popover-open');
    }

    private onKeyDown(event: KeyboardEvent): void {
        switch (event.key) {
            case 'ArrowDown':
                if (this.shown) {
                    this.select(Math.min(this.current + 1, this.suggestions.length - 1));
                    event.preventDefault();
                }
                break;
            case 'ArrowUp':
                if (this.shown) {
                    this.select(Math.max(this.current - 1, 0));
                    event.preventDefault();
                }
                break;
            case 'Enter':
            case 'Tab':
                if (this.shown && this.current !== -1) this.take(this.current);
                else this.hideList();
                break;
            default:
        }
    }

    /** The suggestion's replacement goes in the box, which does not count it as typed: as GWT's SuggestBox. */
    private take(index: number): void {
        const suggestion = this.suggestions[index];
        if (suggestion === undefined) return;
        this.currentText = suggestion.replacement;
        this.input.value = suggestion.replacement;
        this.hideList();
        if (!this.sendSelection) return;
        const message = about(this.id);
        message[C.HANDLER_STRING_SELECTION] = suggestion.display;
        message[C.REPLACEMENT_STRING] = suggestion.replacement;
        this.context.send(message);
    }

    private hideList(): void {
        if (this.shown) this.list.hidePopover();
        this.input.setAttribute('aria-expanded', 'false');
        this.input.removeAttribute('aria-activedescendant');
    }

    override destroy(): void {
        this.list.remove();
        super.destroy();
    }
}
