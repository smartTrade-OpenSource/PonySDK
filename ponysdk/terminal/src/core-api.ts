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
