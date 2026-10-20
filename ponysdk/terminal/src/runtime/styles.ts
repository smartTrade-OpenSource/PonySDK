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
`;

export function installStyles(): void {
    if (document.querySelector('style[data-pony-terminal]') !== null) return;
    const style = document.createElement('style');
    style.dataset['ponyTerminal'] = '';
    style.textContent = RULES;
    document.head.prepend(style);
}
