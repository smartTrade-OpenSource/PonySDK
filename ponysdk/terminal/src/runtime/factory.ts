/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { WidgetType as W, WidgetTypeNames } from '#model';
import { AddOnComposite, AddOnObject } from '../objects/addon.ts';
import { ElementWidget } from '../objects/element.ts';
import { Frame } from '../objects/frame.ts';
import { RootPanel } from '../objects/root-panel.ts';
import { JsFunction, Script } from '../objects/script.ts';
import { WebComponent } from '../objects/web-component.ts';
import { Browser, PopupWindow } from '../objects/windows.ts';
import type { Reader } from '../protocol/reader.ts';
import { log } from './log.ts';
import type { Context, ObjectFactory, TerminalObject } from './object.ts';

type Creator = (context: Context, id: number, type: number, reader: Reader) => TerminalObject;

/** The object of each WidgetType this terminal implements. */
const CREATORS = new Map<number, Creator>([
    [W.BROWSER, (context, id, type) => new Browser(context, id, type)],
    [W.WINDOW, PopupWindow.create],
    [W.ROOT_PANEL, RootPanel.create],
    [W.ELEMENT, ElementWidget.create],
    [W.WEB_COMPONENT, WebComponent.create],
    [W.FRAME, Frame.create],
    [W.ADDON, AddOnObject.create],
    [W.ADDON_COMPOSITE, AddOnComposite.create],
    [W.SCRIPT, (context, id, type) => new Script(context, id, type)],
    [W.FUNCTION, JsFunction.create],
]);

export const createObject: ObjectFactory = (type, context, id, reader) => {
    const create = CREATORS.get(type);
    if (create === undefined) {
        log.warn(`The TypeScript terminal has no ${WidgetTypeNames[type] ?? `type #${type}`} yet (#${id})`);
        return null;
    }
    return create(context, id, type, reader);
};
