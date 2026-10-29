# Theme

A default look for every widget, without writing CSS: light and dark, a focus ring, states from the ARIA attributes,
WCAG AA contrast in both schemes. The showcase's Theme page shows every widget under it. Why it is built so:
[ADR-0024](../decisions/0024-default-theme.md).

## Turn it on

```java
final ApplicationConfiguration configuration = new ApplicationConfiguration();
configuration.setTheme("pony");
```

The bootstrap page then loads `style/pony-theme.css` before the stylesheets of `setStyle`, and its `<html>` gets the
class `pony-theme`: the theme applies where that class is. Off by default: an application that styles the `pony-*`
classes itself is not changed by an upgrade.

A page of your own (a custom `index.html`) adds both itself:

```html
<html class="pony-theme">
<link rel="stylesheet" href="style/pony-theme.css">
```

The theme is a stylesheet: 43 KB, 6 KB gzipped, no web font (the system font stack), no JavaScript.

## Light and dark

The theme follows the system (`prefers-color-scheme`). An attribute on the element that has the class forces a scheme:

```html
<html class="pony-theme" data-theme="dark">
```

`data-theme="light"` forces light on a dark system. From Java, set it by a function of your scripts called with
`PScript.call(PWindow.getMain(), "setScheme", "dark")` (the showcase's `showcaseThemeScheme` in `script/sample.js` is
four lines).

## Tokens

Every colour, space and radius is a custom property. Override one where you want it, in your stylesheet:

```css
:root { --pony-accent: #7c3aed; --pony-accent-hover: #6d28d9; --pony-radius: 2px; }
[data-theme=dark] { --pony-accent: #a78bfa; }     /* its dark value */
```

An override of one class or one attribute beats the theme's own values whatever the order of the stylesheets: its
rules add no specificity. Keep the contrast: the pairs the theme guarantees are in the ADR.

| Token | Use |
|---|---|
| `--pony-bg`, `--pony-surface`, `--pony-surface-2` | page, raised parts (dialogs, fields), hover and headers |
| `--pony-text`, `--pony-muted` | text, secondary text |
| `--pony-border`, `--pony-border-strong` | separators; the border of a control (3:1 against the page) |
| `--pony-accent`, `--pony-accent-hover`, `--pony-accent-soft`, `--pony-on-accent` | links, selection, primary button and the text on it |
| `--pony-danger`, `--pony-success`, `--pony-warning` and `-soft` | messages, `aria-invalid`, notifications |
| `--pony-focus`, `--pony-focus-width`, `--pony-focus-offset` | the keyboard focus ring |
| `--pony-overlay`, `--pony-shadow-sm`, `--pony-shadow`, `--pony-shadow-lg` | modal backdrop, shadows |
| `--pony-space-1` … `--pony-space-6` | 4, 8, 12, 16, 24, 32 px |
| `--pony-radius-sm`, `--pony-radius`, `--pony-radius-lg` | 4, 6, 10 px |
| `--pony-font-family`, `--pony-font-mono`, `--pony-font-size`, `--pony-font-size-sm`, `--pony-font-size-lg`, `--pony-line-height` | type |
| `--pony-control-height`, `--pony-duration` | 32 px; 0.12 s, 0 under `prefers-reduced-motion` |

The table of values is on the showcase's Theme page, read from the stylesheet.

## What it styles

Buttons, anchors, text fields, lists, check boxes, radio buttons, file upload; tabs, stacks, disclosure panels;
dialog boxes, popups, notifications; menus; trees; the date picker and date box; the rich text area and toolbar;
scroll, split and header panels; `PFlexTable` and `PGrid`; DataGrid2; `DropDownContainer` and `ListBox`. States come
from the terminal's attributes: `:hover`, `:focus-visible`, `:disabled`, `aria-invalid`, `aria-selected`,
`aria-expanded`, `aria-pressed`, `aria-busy`, `aria-disabled`.

Two classes are the theme's own, for buttons:

```java
final PButton save = Element.newPButton("Save");
save.addStyleName("pony-primary");   // the accent colour
final PButton delete = Element.newPButton("Delete");
delete.addStyleName("pony-danger");
```

A button the terminal marks `aria-busy` (`showLoadingOnRequest(true)`) shows a spinner. FlexLayout has a skin on the same
tokens: `new FlexLayoutPanel(model, "fl-theme-pony")`.

## Part of a page

The class can be on any element: the theme then styles what that element holds.

```html
<div class="pony-theme" data-theme="dark"> … </div>
```

Popups, dialogs and the lists of drop-downs are children of the page's `<body>`, outside such a container. Give them
the class: `dialog.addStyleName("pony-theme")`, `dialog.setGlassStyleName("pony-theme")`,
`dropDown.addContainerStyleName("pony-theme")`. The date box's popup, the suggestions and the notifications are made
by the terminal and take the theme only from the page's `<html>` (or `<body>`).

In an embedded application ([embedding](embedding.md)), put the class on the element: `<pony-app class="pony-theme">`.

## Keeping your own look

The theme styles `pony-*` classes with no specificity. A rule of yours with one class wins over it:

```css
.pony-Button { border-radius: 999px; }
```

If a rule of the theme still wins, it is in a stylesheet loaded after yours or has more than one class: raise yours by
one class (`.app .pony-Button`).
