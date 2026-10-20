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
`;

export function installStyles(): void {
    if (document.querySelector('style[data-pony-terminal]') !== null) return;
    const style = document.createElement('style');
    style.dataset['ponyTerminal'] = '';
    style.textContent = RULES;
    document.head.prepend(style);
}
