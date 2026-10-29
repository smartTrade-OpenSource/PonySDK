/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

// What the modules loaded on demand use of the core: terminal.js exports it, and they import it from there
// (docs/decisions/0004-terminal-chunks.md). The build fails when a chunk imports a name missing here.
export {
    ClientToServerModel,
    HandlerModel,
    PAlignment,
    PDirection,
    PUnitValue,
    ServerToClientModel,
} from '#model';
export { about, FLAG } from './protocol/messages.ts';
export { BaseObject } from './runtime/object.ts';
export { Widget } from './runtime/widget.ts';
export { FocusWidget } from './objects/focus-widget.ts';
export { POPUP_CLASS } from './objects/popup.ts';
// The <pony-app> element (docs/decisions/0008-embedded-application.md)
export { WidgetType } from '#model';
export { customEventMessage } from './objects/web-component.ts';
export { registerAddOnFactory } from './runtime/dispatcher.ts';
export { defineCreator } from './runtime/factory.ts';
export { BrowserHistory, decode } from './runtime/history.ts';
export { log } from './runtime/log.ts';
export { adoptStyles, installStyles } from './runtime/styles.ts';
export { Terminal } from './terminal.ts';
// The children of an embedded terminal (docs/decisions/0009-embedding-limits.md)
export { applyWindowOperation } from './objects/windows.ts';
