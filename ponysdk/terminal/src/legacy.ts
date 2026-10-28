/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

// The legacy layer (docs/decisions/0002-javascript-terminal.md, section 10): a script of its own, loaded before the
// terminal when ApplicationConfiguration asks for it.

/**
 * GWT's class names on the terminal's DOM, for stylesheets written on them: the primary name of each widget
 * (gwt-Button), the dependent names its state gives (gwt-MenuItem-selected, gwt-CheckBox-disabled), and the names of
 * the parts that still exist (datePickerDay, gwt-TabLayoutPanelTab). GWT's structures are not rebuilt: the caption
 * table of a dialog box, the nine cells of a decorator, the tables of a menu bar have no element to carry their names.
 *
 * As GWT, a style name the server sets (setStyleName) replaces the primary name, and the dependent names derive from
 * its first word: the terminal notes it in data-gwt-primary.
 */

/** The GWT primary name of each type class; the types GWT gave none are absent. */
const PRIMARY: Record<string, string> = {
    'pony-Anchor': 'gwt-Anchor',
    'pony-Button': 'gwt-Button',
    'pony-CheckBox': 'gwt-CheckBox',
    'pony-DateBox': 'gwt-DateBox',
    'pony-DatePicker': 'gwt-DatePicker',
    'pony-DecoratedPopupPanel': 'gwt-DecoratedPopupPanel',
    'pony-DecoratorPanel': 'gwt-DecoratorPanel',
    'pony-DialogBox': 'gwt-DialogBox',
    'pony-DisclosurePanel': 'gwt-DisclosurePanel',
    'pony-FileUpload': 'gwt-FileUpload',
    'pony-FunctionalLabel': 'gwt-Label',
    'pony-HTML': 'gwt-HTML',
    'pony-Image': 'gwt-Image',
    'pony-Label': 'gwt-Label',
    'pony-ListBox': 'gwt-ListBox',
    'pony-MenuBar': 'gwt-MenuBar',
    'pony-MenuItem': 'gwt-MenuItem',
    'pony-MenuItemSeparator': 'gwt-MenuItemSeparator',
    'pony-PasswordTextBox': 'gwt-PasswordTextBox',
    'pony-PopupPanel': 'gwt-PopupPanel',
    'pony-PushButton': 'gwt-PushButton',
    'pony-RadioButton': 'gwt-RadioButton',
    'pony-RichTextArea': 'gwt-RichTextArea',
    'pony-RichTextToolbar': 'gwt-RichTextToolbar',
    'pony-SplitLayoutPanel': 'gwt-SplitLayoutPanel',
    'pony-StackLayoutPanel': 'gwt-StackLayoutPanel',
    'pony-SuggestBox': 'gwt-SuggestBox',
    'pony-TabLayoutPanel': 'gwt-TabLayoutPanel',
    'pony-TabPanel': 'gwt-TabPanel',
    'pony-TextArea': 'gwt-TextArea',
    'pony-TextBox': 'gwt-TextBox',
    'pony-Tree': 'gwt-Tree',
};

/** What GWT named the elements that hold no type class, by selector. */
const PARTS: [string, (e: Element) => string[]][] = [
    ['.pony-Glass', e => (e.classList.length > 1 ? [] : ['gwt-PopupPanelGlass'])], // the server's glass name replaces it
    ['.pony-MenuPopup', () => ['gwt-MenuBarPopup']],
    ['.pony-DialogBox .pony-Caption', () => ['Caption']],
    ['.pony-DialogBox .pony-Content', () => ['dialogContent']],
    ['.pony-DisclosurePanel > summary', () => ['header']],
    ['.pony-DisclosurePanel > div', () => ['content']],
    ['.pony-StackLayoutPanel-header', e => ['gwt-StackLayoutPanelHeader', ...when(hovered.has(e), 'gwt-StackLayoutPanelHeader-hovering')]],
    ['.pony-StackLayoutPanel-content', () => ['gwt-StackLayoutPanelContent']],
    ['.pony-TabLayoutPanel > [role=tablist]', () => ['gwt-TabLayoutPanelTabs']],
    ['.pony-TabLayoutPanel > [role=tablist] > [role=tab]', e => ['gwt-TabLayoutPanelTab', ...when(selected(e), 'gwt-TabLayoutPanelTab-selected')]],
    ['.pony-TabLayoutPanel > [role=tabpanel]', () => ['gwt-TabLayoutPanelContent']],
    ['.pony-TabPanel > [role=tablist]', () => ['gwt-TabBar']],
    ['.pony-TabPanel > [role=tablist] > [role=tab]', e => ['gwt-TabBarItem', ...when(selected(e), 'gwt-TabBarItem-selected')]],
    ['.pony-TabPanel > [role=tabpanel]', () => ['gwt-TabPanelBottom']],
    ['.pony-Splitter-horizontal', () => ['gwt-SplitLayoutPanel-HDragger']],
    ['.pony-Splitter-vertical', () => ['gwt-SplitLayoutPanel-VDragger']],
    ['.pony-TreeItem-content', e => ['gwt-TreeItem', ...when(e.closest('[role=treeitem]')?.getAttribute('aria-selected') === 'true', 'gwt-TreeItem-selected')]],
    ['.pony-SuggestPopup', () => ['gwt-SuggestBoxPopup']],
    ['.pony-SuggestPopup [role=option]', e => ['item', ...when(selected(e), 'item-selected')]],
    ['.pony-DateBoxPopup', () => ['dateBoxPopup']],
    ['.pony-DatePicker-header', () => ['datePickerMonthSelector']],
    ['.pony-DatePicker-month', () => ['datePickerMonth']],
    ['.pony-DatePicker-previous', () => ['datePickerPreviousButton']],
    ['.pony-DatePicker-next', () => ['datePickerNextButton']],
    ['.pony-DatePicker-previousYear', () => ['datePickerPreviousYearButton']],
    ['.pony-DatePicker-nextYear', () => ['datePickerNextYearButton']],
    ['.pony-DatePicker-days', () => ['datePickerDays']],
    ['.pony-DatePicker-days th', e => [(e as HTMLTableCellElement).cellIndex >= 5 ? 'datePickerWeekendLabel' : 'datePickerWeekdayLabel']],
    ['.pony-DatePicker-day', day],
];

/** The dependent names a widget's state gives, from its primary name. */
const DEPENDENT: Record<string, (e: Element, primary: string) => string[]> = {
    'pony-CheckBox': (e, p) => when(input(e)?.disabled === true, `${p}-disabled`),
    'pony-RadioButton': (e, p) => when(input(e)?.disabled === true, `${p}-disabled`),
    'pony-TextBox': (e, p) => when((e as HTMLInputElement).readOnly, `${p}-readonly`),
    'pony-PasswordTextBox': (e, p) => when((e as HTMLInputElement).readOnly, `${p}-readonly`),
    'pony-TextArea': (e, p) => when((e as HTMLTextAreaElement).readOnly, `${p}-readonly`),
    'pony-DateBox': e => when(e.getAttribute('aria-invalid') === 'true', 'dateBoxFormatError'),
    'pony-MenuBar': (e, p) => [`${p}-${e.getAttribute('aria-orientation') === 'vertical' ? 'vertical' : 'horizontal'}`],
    'pony-MenuItem': (e, p) => [
        ...when(menuItemSelected(e), `${p}-selected`),
        ...when(e.getAttribute('aria-disabled') === 'true', `${p}-disabled`),
    ],
    'pony-DisclosurePanel': (e, p) => [`${p}-${(e as HTMLDetailsElement).open ? 'open' : 'closed'}`],
    'pony-RichTextArea': e => when(e.classList.contains('pony-hasRichTextToolbar'), 'hasRichTextToolbar'),
};

/** A day of a date picker: GWT's styles of a day; the value and today show on the days of the month only. */
function day(e: Element): string[] {
    const filler = e.hasAttribute('data-filler');
    const value = !filler && e.getAttribute('aria-selected') === 'true';
    const highlighted = hovered.has(e);
    return ['datePickerDay', ...when(filler, 'datePickerDayIsFiller'), ...when(e.hasAttribute('data-weekend'), 'datePickerDayIsWeekend'),
        ...when(!filler && e.getAttribute('aria-current') === 'date', 'datePickerDayIsToday'), ...when(value, 'datePickerDayIsValue'),
        ...when(e.getAttribute('aria-disabled') === 'true', 'datePickerDayIsDisabled'), ...when(highlighted, 'datePickerDayIsHighlighted'),
        ...when(value && highlighted, 'datePickerDayIsValueAndHighlighted')];
}

/** As GWT's selected item of a bar: under the pointer, or with its submenu open, or focused while none is hovered. */
function menuItemSelected(e: Element): boolean {
    if (hovered.has(e) || e.getAttribute('aria-expanded') === 'true') return true;
    // the focus of the document, or of the shadow root of a <pony-app> element
    return e === (e.getRootNode() as Document | ShadowRoot).activeElement && !siblingItems(e).some(item => hovered.has(item));
}

function siblingItems(e: Element): Element[] {
    return [...(e.parentElement?.children ?? [])].filter(item => item.classList.contains('pony-MenuItem'));
}

function when(condition: boolean, name: string): string[] {
    return condition ? [name] : [];
}

function selected(e: Element): boolean {
    return e.getAttribute('aria-selected') === 'true';
}

function input(e: Element): HTMLInputElement | null {
    return e.querySelector('input');
}

/** The elements under the pointer that GWT styled while hovered. */
const hovered = new WeakSet<Element>();
const HOVERED = '.pony-MenuItem, .pony-StackLayoutPanel-header, .pony-DatePicker-day';

/** The names this layer gave each element, to take back those its state no longer gives. */
const given = new WeakMap<Element, Set<string>>();

/** The names an element must have. */
function names(e: Element): string[] {
    const result: string[] = [];
    const primaryAttribute = e.getAttribute('data-gwt-primary');
    for (const typeClass of e.classList) {
        const gwt = PRIMARY[typeClass];
        if (gwt === undefined) continue;
        if (typeClass === 'pony-TextBox' && e.classList.contains('pony-SuggestBox')) continue; // GWT's SuggestBox renames it
        const primary = primaryAttribute ?? gwt;
        if (primaryAttribute === null) result.push(gwt);
        if (primary !== '') result.push(...(DEPENDENT[typeClass]?.(e, primary) ?? []));
    }
    for (const [selector, part] of PARTS) if (e.matches(selector)) result.push(...part(e));
    return result;
}

function decorate(e: Element): void {
    const wanted = new Set(names(e));
    const previous = given.get(e);
    if (previous !== undefined) for (const name of previous) if (!wanted.has(name) && e.classList.contains(name)) e.classList.remove(name);
    for (const name of wanted) if (!e.classList.contains(name)) e.classList.add(name);
    if (wanted.size > 0 || previous !== undefined) given.set(e, wanted);
}

const CANDIDATES = '[class*="pony-"], [role=tablist], [role=tab], [role=tabpanel], [role=option], summary, th, .pony-DisclosurePanel > div';

function decorateTree(root: Element): void {
    decorate(root);
    for (const e of root.querySelectorAll(CANDIDATES)) decorate(e);
}

/** What a change of an element's state changes: itself, and the widget whose state it carries. */
function decorateAround(e: Element): void {
    decorate(e);
    const owner = e.parentElement?.closest('.pony-CheckBox, .pony-RadioButton, [role=treeitem]');
    if (owner !== null && owner !== undefined) {
        decorate(owner);
        const content = owner.querySelector(':scope > .pony-TreeItem-row > .pony-TreeItem-content');
        if (content !== null) decorate(content);
    }
    if (e.matches('[role=treeitem]')) {
        const content = e.querySelector(':scope > .pony-TreeItem-row > .pony-TreeItem-content');
        if (content !== null) decorate(content);
    }
}

/** The document, and the shadow roots of <pony-app> elements, whose pointer and focus events are listened to. */
const listened = new WeakSet<Node>();

/**
 * Decorates root's elements, those added and those changed after: the document in the application's page, the
 * container of a <pony-app> element otherwise (docs/decisions/0009-embedding-limits.md, decision 8). The pointer and
 * focus events are listened to on root's root node, the document or the element's shadow root, where they are not
 * retargeted to the element.
 */
function observe(root: Element): void {
    decorateTree(root);
    new MutationObserver(records => {
        for (const record of records) {
            if (record.type === 'childList') {
                for (const node of record.addedNodes) if (node instanceof Element) decorateTree(node);
            } else if (record.target instanceof Element) {
                decorateAround(record.target);
            }
        }
    }).observe(root, {
        subtree: true, childList: true, attributes: true,
        attributeFilter: ['class', 'data-gwt-primary', 'aria-selected', 'aria-disabled', 'aria-expanded', 'aria-orientation',
            'aria-current', 'aria-invalid', 'data-filler', 'disabled', 'readonly', 'open'],
    });
    const node = root.getRootNode();
    if (listened.has(node)) return;
    listened.add(node);
    const track = (event: Event, over: boolean): void => {
        const target = (event.target as Element | null)?.closest?.(HOVERED);
        if (target === null || target === undefined) return;
        if (!over && target.contains((event as PointerEvent).relatedTarget as Node | null)) return;
        if (over) hovered.add(target);
        else hovered.delete(target);
        if (target.classList.contains('pony-MenuItem')) for (const item of siblingItems(target)) decorate(item);
        else decorate(target);
    };
    node.addEventListener('pointerover', e => track(e, true), true);
    node.addEventListener('pointerout', e => track(e, false), true);
    const focus = (event: Event): void => {
        const target = (event.target as Element | null)?.closest?.('.pony-MenuItem');
        if (target !== null && target !== undefined) decorate(target);
    };
    node.addEventListener('focusin', focus, true);
    node.addEventListener('focusout', e => setTimeout(() => focus(e)), true);
}

(window as unknown as { ponyLegacy: { observe(root: Element): void } }).ponyLegacy = { observe };

// The application's page; a <pony-app> element that loads this script observes its container instead
if (document.currentScript?.getAttribute('data-pony-legacy') !== 'element') {
    document.documentElement.setAttribute('data-pony-legacy', '');
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => observe(document.documentElement));
    else observe(document.documentElement);
}
