/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { WidgetType as W } from '#model';
import { AddOnComposite, AddOnObject } from '../objects/addon.ts';
import { Anchor, Button, PushButton } from '../objects/buttons.ts';
import { CheckBox, RadioButton } from '../objects/check-box.ts';
import { DisclosurePanel } from '../objects/disclosure-panel.ts';
import { ElementWidget } from '../objects/element.ts';
import { FileUpload } from '../objects/file-upload.ts';
import { Frame } from '../objects/frame.ts';
import { Image } from '../objects/image.ts';
import { ListBox } from '../objects/list-box.ts';
import {
    CellPanel,
    Composite,
    createAbsolutePanel,
    DecoratorPanel,
    FlowPanel,
    FocusPanel,
    ScrollPanel,
    SimplePanel,
} from '../objects/panels.ts';
import { createDecoratedPopupPanel, DialogBox, PopupPanel } from '../objects/popup.ts';
import { RootPanel } from '../objects/root-panel.ts';
import { JsFunction, Script } from '../objects/script.ts';
import { FunctionalLabel, Html, Label } from '../objects/text.ts';
import { createPasswordTextBox, createTextArea, createTextBox } from '../objects/text-box.ts';
import { TabPanel } from '../objects/tab-panel.ts';
import { createFlexTable, Grid } from '../objects/tables.ts';
import { WebComponent } from '../objects/web-component.ts';
import { Browser, PopupWindow } from '../objects/windows.ts';
import type { Reader } from '../protocol/reader.ts';
import { log } from './log.ts';
import { nameOf } from './names.ts';
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
    [W.LABEL, Label.create],
    [W.HTML, Html.create],
    [W.FUNCTIONAL_LABEL, FunctionalLabel.create],
    [W.ANCHOR, Anchor.create],
    [W.IMAGE, Image.create],
    [W.BUTTON, Button.create],
    [W.PUSH_BUTTON, PushButton.create],
    [W.TEXTBOX, createTextBox],
    [W.PASSWORD_TEXTBOX, createPasswordTextBox],
    [W.TEXT_AREA, createTextArea],
    [W.CHECKBOX, CheckBox.create],
    [W.RADIO_BUTTON, RadioButton.create],
    [W.LISTBOX, ListBox.create],
    [W.FILE_UPLOAD, FileUpload.create],
    [W.FLOW_PANEL, FlowPanel.create],
    [W.SIMPLE_PANEL, SimplePanel.create],
    [W.SCROLL_PANEL, ScrollPanel.create],
    [W.FOCUS_PANEL, FocusPanel.create],
    [W.ABSOLUTE_PANEL, createAbsolutePanel],
    [W.HORIZONTAL_PANEL, CellPanel.createHorizontal],
    [W.VERTICAL_PANEL, CellPanel.createVertical],
    [W.COMPOSITE, Composite.create],
    [W.FLEX_TABLE, createFlexTable],
    [W.GRID, Grid.create],
    [W.DISCLOSURE_PANEL, DisclosurePanel.create],
    [W.DECORATOR_PANEL, DecoratorPanel.create],
    [W.TAB_PANEL, TabPanel.create],
    [W.POPUP_PANEL, PopupPanel.create],
    [W.DECORATED_POPUP_PANEL, createDecoratedPopupPanel],
    [W.DIALOG_BOX, DialogBox.create],
    [W.TAB_LAYOUT_PANEL, TabPanel.createLayout],
]);

type Creators = [number, Creator][];

/**
 * The widgets many pages do not show at first, in chunks loaded on demand (docs/decisions/0004-terminal-chunks.md):
 * the types of each module, and its loading.
 */
const ON_DEMAND: [number[], () => Promise<Creators>][] = [
    [[W.DATEPICKER, W.DATEBOX], async () => {
        const m = await import('../objects/date-picker.ts');
        return [[W.DATEPICKER, m.DatePicker.create], [W.DATEBOX, m.DateBox.create]];
    }],
    [[W.RICH_TEXT_AREA, W.RICH_TEXT_TOOLBAR], async () => {
        const m = await import('../objects/rich-text.ts');
        return [[W.RICH_TEXT_AREA, m.RichTextArea.create], [W.RICH_TEXT_TOOLBAR, m.RichTextToolbar.create]];
    }],
    [[W.LAYOUT_PANEL, W.ROOT_LAYOUT_PANEL, W.SIMPLE_LAYOUT_PANEL, W.DOCK_LAYOUT_PANEL, W.SPLIT_LAYOUT_PANEL, W.HEADER_PANEL],
        async () => {
            const m = await import('../objects/layout.ts');
            return [[W.LAYOUT_PANEL, m.LayoutPanel.create], [W.ROOT_LAYOUT_PANEL, m.RootLayoutPanel.create],
                [W.SIMPLE_LAYOUT_PANEL, m.SimpleLayoutPanel.create], [W.DOCK_LAYOUT_PANEL, m.DockLayoutPanel.create],
                [W.SPLIT_LAYOUT_PANEL, m.SplitLayoutPanel.create], [W.HEADER_PANEL, m.HeaderPanel.create]];
        }],
    [[W.STACKLAYOUT_PANEL], async () => {
        const m = await import('../objects/stack-layout-panel.ts');
        return [[W.STACKLAYOUT_PANEL, m.StackLayoutPanel.create]];
    }],
    [[W.TREE, W.TREE_ITEM], async () => {
        const m = await import('../objects/tree.ts');
        return [[W.TREE, m.Tree.create], [W.TREE_ITEM, m.TreeItem.create]];
    }],
    [[W.MENU_BAR, W.MENU_ITEM, W.MENU_ITEM_SEPARATOR], async () => {
        const m = await import('../objects/menu.ts');
        return [[W.MENU_BAR, m.MenuBar.create], [W.MENU_ITEM, m.MenuItem.create],
            [W.MENU_ITEM_SEPARATOR, m.createMenuItemSeparator]];
    }],
    [[W.MULTIWORD_SUGGEST_ORACLE, W.SUGGESTBOX], async () => {
        const m = await import('../objects/suggest.ts');
        return [[W.MULTIWORD_SUGGEST_ORACLE, (context, id, type) => new m.SuggestOracle(context, id, type)],
            [W.SUGGESTBOX, m.SuggestBox.create]];
    }],
];

/** The loading of each module on demand, once started; its types' creators join CREATORS when it ends. */
const loadings = new Map<() => Promise<Creators>, Promise<void>>();

function load(module: () => Promise<Creators>): Promise<void> {
    let loading = loadings.get(module);
    if (loading === undefined) {
        loading = module().then(creators => {
            for (const [type, create] of creators) CREATORS.set(type, create);
        });
        loadings.set(module, loading);
    }
    return loading;
}

/**
 * The loading of a type's module, or null when there is nothing to wait for: the type is loaded, unknown, or its
 * module failed to load (the factory then reports it).
 */
export function loadingOf(type: number): Promise<void> | null {
    if (CREATORS.has(type)) return null;
    const module = ON_DEMAND.find(([types]) => types.includes(type))?.[1];
    if (module === undefined) return null;
    const loading = load(module);
    return failed.has(module) ? null : loading.catch(e => {
        failed.add(module);
        throw e;
    });
}

const failed = new Set<() => Promise<Creators>>();

/** Loads the modules not loaded yet: when the browser is idle, so that a first rare widget does not wait. */
export function loadAll(): void {
    for (const [, module] of ON_DEMAND) load(module).catch(() => failed.add(module));
}

export const createObject: ObjectFactory = (type, context, id, reader) => {
    const create = CREATORS.get(type);
    if (create === undefined) {
        log.warn(`The TypeScript terminal has no ${nameOf(W, type) ?? `type #${type}`} yet (#${id})`);
        return null;
    }
    return create(context, id, type, reader);
};
