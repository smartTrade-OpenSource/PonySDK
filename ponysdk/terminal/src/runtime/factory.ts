/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

import { WidgetType as W, WidgetTypeNames } from '#model';
import { AddOnComposite, AddOnObject } from '../objects/addon.ts';
import { Anchor, Button, PushButton } from '../objects/buttons.ts';
import { CheckBox, RadioButton } from '../objects/check-box.ts';
import { DisclosurePanel } from '../objects/disclosure-panel.ts';
import { ElementWidget } from '../objects/element.ts';
import { FileUpload } from '../objects/file-upload.ts';
import { Frame } from '../objects/frame.ts';
import { Image } from '../objects/image.ts';
import { ListBox } from '../objects/list-box.ts';
import { createMenuItemSeparator, MenuBar, MenuItem } from '../objects/menu.ts';
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
import { SuggestBox, SuggestOracle } from '../objects/suggest.ts';
import { TabPanel } from '../objects/tab-panel.ts';
import { createFlexTable, Grid } from '../objects/tables.ts';
import { Tree, TreeItem } from '../objects/tree.ts';
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
    [W.MENU_BAR, MenuBar.create],
    [W.MENU_ITEM, MenuItem.create],
    [W.MENU_ITEM_SEPARATOR, createMenuItemSeparator],
    [W.TREE, Tree.create],
    [W.TREE_ITEM, TreeItem.create],
    [W.MULTIWORD_SUGGEST_ORACLE, (context, id, type) => new SuggestOracle(context, id, type)],
    [W.SUGGESTBOX, SuggestBox.create],
]);

export const createObject: ObjectFactory = (type, context, id, reader) => {
    const create = CREATORS.get(type);
    if (create === undefined) {
        log.warn(`The TypeScript terminal has no ${WidgetTypeNames[type] ?? `type #${type}`} yet (#${id})`);
        return null;
    }
    return create(context, id, type, reader);
};
