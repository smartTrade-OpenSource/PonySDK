/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

/**
 * What the widgets need to work, whatever the application's stylesheets: layout of the cell panels, scrolling,
 * positioning. In a stylesheet rather than inline, so that hiding and showing a widget (display) keeps it; first in
 * the head and inside :where(), so that any rule of the application wins.
 */
const RULES = `
:where(.pony-HorizontalPanel){display:flex;flex-direction:row;width:fit-content}
:where(.pony-VerticalPanel){display:flex;flex-direction:column;width:fit-content}
:where(.pony-Cell){display:flex;flex-direction:column;min-width:0}
:where(.pony-ScrollPanel){overflow:auto;position:relative}
:where(.pony-AbsolutePanel){position:relative;overflow:hidden}
:where(.pony-Popup){position:absolute;inset:auto;margin:0;padding:0;border:0;max-width:none;max-height:none;overflow:visible;color:inherit;background:none}
:where(.pony-Popup)::backdrop{background:none}
:where(.pony-Glass){position:fixed;inset:0;width:auto;height:auto;margin:0;padding:0;border:0;background:rgb(0 0 0/.3)}
:where(.pony-MenuBar[aria-orientation=horizontal]){display:flex}
:where(.pony-MenuItem){cursor:default;white-space:nowrap}
:where(.pony-MenuItem[aria-disabled=true]){opacity:.5}
:where(.pony-Tree ul){list-style:none;margin:0;padding:0}
:where(.pony-Tree [role=group]){padding-left:1.25em}
:where(.pony-TreeItem-toggle){display:inline-block;width:1em;text-align:center;cursor:pointer}
:where(.pony-TreeItem-toggle[data-state=closed])::before{content:"▸"}
:where(.pony-TreeItem-toggle[data-state=open])::before{content:"▾"}
:where(.pony-TreeItem-content){cursor:default}
:where(.pony-SuggestPopup [role=option]){cursor:default;white-space:nowrap}
:where(.pony-LayoutPanel,.pony-SimpleLayoutPanel,.pony-DockLayoutPanel,.pony-SplitLayoutPanel){position:relative;overflow:hidden}
:where(.pony-RootLayoutPanel){position:absolute;inset:0;overflow:hidden}
:where(.pony-Layer){position:absolute;overflow:hidden}
:where(.pony-Splitter){position:absolute;background:rgb(0 0 0/.08);touch-action:none}
:where(.pony-Splitter-horizontal){cursor:col-resize}
:where(.pony-Splitter-vertical){cursor:row-resize}
:where(.pony-TabLayoutPanel,.pony-StackLayoutPanel,.pony-HeaderPanel){display:flex;flex-direction:column;overflow:hidden}
:where(.pony-TabLayoutPanel>[role=tablist]){flex:none;display:flex}
:where(.pony-TabLayoutPanel>[role=tabpanel],.pony-StackLayoutPanel-content,.pony-HeaderPanel-content){position:relative;flex:1 1 0;overflow:hidden}
:where(.pony-StackLayoutPanel-header){flex:none;display:block;width:100%;text-align:start}
:where(.pony-HeaderPanel-header,.pony-HeaderPanel-footer){flex:none}
:where(.pony-DateBoxPopup,.pony-SuggestPopup){background:Canvas;border:1px solid GrayText}
:where(.pony-DatePicker-header){display:flex;align-items:center}
:where(.pony-DatePicker-month){flex:1;text-align:center}
:where(.pony-DatePicker-days){border-collapse:collapse}
:where(.pony-DatePicker-day){text-align:center;cursor:pointer}
:where(.pony-DatePicker-day[data-filler]){opacity:.5}
:where(.pony-DatePicker-day[aria-current=date]){font-weight:bold}
:where(.pony-DatePicker-day[aria-selected=true]:not([data-filler])){background:Highlight;color:HighlightText}
:where(.pony-DatePicker-day[aria-disabled=true]){opacity:.3;cursor:default}
:where(.pony-RichTextArea){min-height:6em;overflow:auto;border:1px solid GrayText;padding:2px}
:where(.pony-RichTextToolbar-row){display:flex;flex-wrap:wrap;gap:2px}
:where(.pony-RichTextToolbar [aria-pressed=true]){background:Highlight;color:HighlightText}
`;

let sheet: CSSStyleSheet | undefined;

/** The same rules in the shadow root of a <pony-app> element: one sheet, shared by the elements (ADR-0008). */
export function adoptStyles(root: ShadowRoot): void {
    if (sheet === undefined) {
        sheet = new CSSStyleSheet();
        sheet.replaceSync(RULES);
    }
    root.adoptedStyleSheets = [sheet];
}

export function installStyles(): void {
    if (document.querySelector('style[data-pony-terminal]') !== null) return;
    const style = document.createElement('style');
    style.dataset['ponyTerminal'] = '';
    style.textContent = RULES;
    document.head.prepend(style);
}
