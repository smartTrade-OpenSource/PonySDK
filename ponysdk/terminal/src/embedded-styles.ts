/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { log } from './runtime/log.ts';

/**
 * The application's stylesheets in a <pony-app> element (docs/decisions/0009-embedding-limits.md, decision 4): written
 * for a page, they are adapted once loaded. Their selectors that name the page (:root, html, body) are rewritten to
 * name the element; in a shadow root, their @font-face rules are declared to the document, since Chrome and Firefox
 * ignore those of a shadow root.
 */

/** The fonts of the applications' stylesheets, declared to the document: one sheet for the page, adopted once. */
let fonts: CSSStyleSheet | undefined;
const fontSources = new Set<string>();

const BOUNDARY = /[\s>+~,(]/;
const IDENT = /[\w-]/;
const PAGE_NAMES = [':root', 'html', 'body'];

/**
 * The selector with the page's names rewritten, or null when it names none: :root and html become root (:host in a
 * shadow root, with its compound as :host(…); pony-app in the page), body becomes the element's container.
 */
export function pageSelector(selector: string, root: string): string | null {
    let out = '';
    let changed = false;
    let quote = '';
    let brackets = 0;
    let i = 0;
    while (i < selector.length) {
        const c = selector[i]!;
        if (quote !== '') {
            if (c === '\\') {
                out += c + selector[i + 1]!; // the CSSOM's text ends no string open
                i += 2;
                continue;
            }
            if (c === quote) quote = '';
        } else if (c === '"' || c === '\'') {
            quote = c;
        } else if (c === '[') {
            brackets++;
        } else if (c === ']') {
            brackets--;
        } else if (brackets === 0 && (i === 0 || BOUNDARY.test(selector[i - 1]!))) {
            const name = PAGE_NAMES.find(n => selector.startsWith(n, i) && !IDENT.test(selector[i + n.length] ?? ''));
            if (name !== undefined) {
                const end = compoundEnd(selector, i + name.length);
                const compound = selector.substring(i + name.length, end);
                if (name === 'body') out += `[data-pony-body]${compound}`;
                else if (root === ':host' && compound !== '') out += `:host(${compound})`;
                else out += root + compound;
                changed = true;
                i = end;
                continue;
            }
        }
        out += c;
        i++;
    }
    return changed ? out : null;
}

/** Where the compound selector that starts at from ends: at a combinator, a comma, or the parenthesis around it. */
function compoundEnd(selector: string, from: number): number {
    let depth = 0;
    let quote = '';
    for (let i = from; i < selector.length; i++) {
        const c = selector[i]!;
        if (quote !== '') {
            if (c === '\\') i++;
            else if (c === quote) quote = '';
        } else if (c === '"' || c === '\'') {
            quote = c;
        } else if (c === '(' || c === '[') {
            depth++;
        } else if (c === ')' || c === ']') {
            if (depth === 0) return i;
            depth--;
        } else if (depth === 0 && /[\s>+~,]/.test(c)) {
            return i;
        }
    }
    return selector.length;
}

/** Adapts a stylesheet of the application once loaded: in a shadow root, root is :host; in the page, pony-app. */
export function adaptStylesheet(sheet: CSSStyleSheet, root: string): void {
    let rules: CSSRuleList;
    try {
        rules = sheet.cssRules;
    } catch {
        log.warn(`${sheet.href}: its rules cannot be read (no CORS): left as written for a page`);
        return;
    }
    adaptRules(rules, sheet.href!, root); // a link's or an @import's
}

function adaptRules(rules: CSSRuleList, base: string, root: string): void {
    for (const rule of rules) {
        if (rule instanceof CSSStyleRule) {
            const selector = pageSelector(rule.selectorText, root);
            if (selector !== null) {
                rule.selectorText = selector;
                // An invalid selector leaves the rule as it was: :host() takes no :has(), for one
                if (rule.selectorText !== selector) log.warn(`"${selector}" is no selector: the rule stays for a page`);
            }
        } else if (rule instanceof CSSFontFaceRule) {
            if (root === ':host') declareFont(rule, base);
            continue;
        } else if (rule instanceof CSSImportRule) {
            if (rule.styleSheet !== null) adaptStylesheet(rule.styleSheet, root);
            continue;
        }
        const nested = (rule as Partial<CSSGroupingRule>).cssRules; // @media, @supports, @layer, @container, nesting
        if (nested !== undefined) adaptRules(nested, base, root);
    }
}

/** An @font-face of a shadow root, declared to the document with its URLs made absolute, once per source. */
function declareFont(rule: CSSFontFaceRule, base: string): void {
    const text = rule.cssText.replace(/url\(\s*(['"]?)(.*?)\1\s*\)/g, (_, _quote: string, url: string) =>
        `url("${new URL(url, base).href}")`);
    if (fontSources.has(text)) return;
    fontSources.add(text);
    if (fonts === undefined) {
        fonts = new CSSStyleSheet();
        document.adoptedStyleSheets = [...document.adoptedStyleSheets, fonts];
    }
    fonts.insertRule(text, fonts.cssRules.length);
}
