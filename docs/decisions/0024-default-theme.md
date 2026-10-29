# ADR-0024: A default theme for the widgets

- Status: Accepted
- Date: 2026-10-29

## Context

The terminal's widgets are native elements with a `pony-<Type>` class and ARIA states (ADR-0002, section 5).
`style/ponysdk.css` styles none of it: it holds the layout the drop-downs and the DataGrid2 need (a 5 KB file). An
application that wants a finished look writes the CSS of every widget it uses, as the showcase does in a 1,080-line
`showcase.css`. Vaadin's Lumo and Aura give a finished look with one line of configuration; PonySDK gave none.

Constraints that shape the answer:

- 2.x applications style the `pony-*` classes (and `gwt-*` ones through `legacyClassNames`) themselves. A look that
  turns on for everybody would restyle them.
- The bootstrap page runs under a Content-Security-Policy (ADR-0015): the theme is a file, no inline style.
- The terminal's own structural rules (`styles.ts`) are unlayered and written in `:where()`: a stylesheet in a CSS
  layer would lose to them.
- Popups, dialogs, the lists of drop-downs and the suggestions are children of the page's `<body>`, not of the widget
  that opens them (`PopupPanel.show`, `DropDownContainerAddon.show`).
- A `<pony-app>` element (ADR-0008, ADR-0009) shows the application in a shadow root, where `:root` is `:host`.

## Decision

1. `style/pony-theme.css`, served from the classpath like `ponysdk.css`: a small design system for the widgets of the
   terminal and of the `core.ui` packages (below). System font stack, no web font.
2. Tokens are custom properties prefixed `--pony-`: 19 shared (font family and sizes, a 6-step spacing scale, three
   radii, control height, focus ring width and offset, motion duration) and 22 per scheme (`bg`, `surface`,
   `surface-2`, `text`, `muted`, `border`, `border-strong`, `accent`, `accent-hover`, `accent-soft`, `on-accent`,
   `danger`, `success`, `warning` with their `-soft` tints, `focus`, `overlay`, three shadows). Light by default;
   dark with `prefers-color-scheme: dark`, and `[data-theme=dark]` / `[data-theme=light]` force a scheme either way.
   `color-scheme` follows, so that the native parts (scrollbars, check boxes, `<select>` popups) match.
3. Scope is a class. Nothing applies without `pony-theme`: on `<html>`, the theme is the page's; on another element, it
   is that element's; as `:host(.pony-theme)`, it is an embedded application's. Every rule starts with
   `:where(.pony-theme, :host(.pony-theme))`, so it adds no specificity: an application rule with one class wins over
   any rule of the theme, whatever the order of the stylesheets. Tokens too: `:root { --pony-accent: … }` wins.
   A popup, a dialog box or a drop-down list sits outside a scoped container: its rules are also written for the
   element that has the class itself (`.pony-DialogBox.pony-theme`), so `addStyleName("pony-theme")` on the popup,
   `setGlassStyleName("pony-theme")` on its glass and `addContainerStyleName("pony-theme")` on a drop-down theme them.
   Popups the terminal creates (date box, suggestions, notifications) take the theme from `<html>` only.
4. Activation: `ApplicationConfiguration.setTheme(String)`; `"pony"` is the only value, `null` (the default) loads
   nothing, another name throws `IllegalArgumentException`; `Builder.theme(String)` too. `BootstrapServlet` then
   writes `<html class="pony-theme">` and the theme's `<link>` before the application's own stylesheets (they win by
   order as well as by specificity), through the same list as `setStyle`; the manifest of an embedded application
   (`ponyapp.json`) lists it first. Off by default, for the 2.x applications that style `pony-*` themselves: an
   upgrade changes nothing until the setting is made.
5. Coverage: buttons (default, `pony-primary`, `pony-danger`, disabled, `aria-busy` with a spinner), push buttons,
   anchors; text box, password, text area, date box, suggest box (`aria-invalid`, `:disabled`, `[readonly]`,
   placeholder), `PListBox`; check box, radio button, file upload; `PTabPanel`, `PTabLayoutPanel`,
   `PStackLayoutPanel`, `PDisclosurePanel`; dialog box, popup, decorated popup, closable dialog, glass, `:modal`
   backdrop, the four notifications; menu bar, menu items, submenus, separators; tree; date picker; rich text area and
   toolbar; scroll, split, header, decorator panels; `PFlexTable`, `PGrid`; DataGrid2 (header, footer, rows,
   `pony-hovered`, `pony-selected`, `pony-pinned`, resizer, loading bar, error message); `DropDownContainer`,
   `ListBox` and its infinite-scroll items; toolbar, breadcrumbs, pager, twin list box, form field errors. FlexLayout
   has its own tokens: `.fl-theme-pony` maps them on the theme's (`new FlexLayoutPanel(model, "fl-theme-pony")`).
   States are read from `:hover`, `:focus-visible`, `:disabled`, `aria-invalid`, `aria-selected`, `aria-expanded`,
   `aria-pressed`, `aria-current`, `aria-busy`, `aria-disabled`, `open`.
6. One focus ring for the keyboard: `:focus-visible { outline: 2px solid var(--pony-focus); outline-offset: 2px }`;
   items of a list, a bar or a grid draw it inside (`-2px`).
7. `prefers-reduced-motion: reduce` sets `--pony-duration` to 0 (every transition of the theme uses it) and stops the
   spinner and the loading bar.
8. WCAG 2.x AA, computed from the tokens by the stylesheet's own values (ratios below), and checked in the browser by
   `sample/tests/theme.spec.js` on the resolved tokens and on the rendered text of 16 kinds of widget.
9. The showcase's Theme page shows every widget in a frame: a second session of the showcase whose page loads the
   theme (`showcaseTheme()` in `sample.js`: the `<link>` and the class on `<html>`) and leaves `showcase.css` out,
   so that the widgets show the theme alone. The showcase keeps its own look. The page has the Light / Dark / System
   toggle and the token table, read from the stylesheet.

### Contrast of the key pairs

| Pair | Light | Dark | Needs |
|---|---|---|---|
| `text` on `bg` / `surface` | 17.85 | 15.21 / 14.33 | 4.5 |
| `text` on `surface-2` (hover, headers) | 16.30 | 11.82 | 4.5 |
| `text` on `accent-soft` (selected row) | 14.63 | 9.29 | 4.5 |
| `muted` on `bg` / `surface-2` | 7.58 / 6.92 | 8.38 / 6.51 | 4.5 |
| `accent` on `bg` / `surface-2` (links, selected tab) | 5.17 / 4.72 | 7.41 / 5.75 | 4.5 |
| `on-accent` on `accent` (primary button) | 5.17 | 7.41 | 4.5 |
| `on-accent` on `danger` (danger button) | 6.47 | 6.81 | 4.5 |
| `danger` on `bg` / on `danger-soft` | 6.47 / 5.30 | 6.81 / 5.90 | 4.5 |
| `success` on `bg` / on `success-soft` | 5.48 / 4.84 | 9.79 / 6.53 | 4.5 |
| `warning` on `bg` / on `warning-soft` | 7.09 / 6.37 | 11.28 / 8.31 | 4.5 |
| `text` on `danger-soft` / `warning-soft` (notifications) | 14.61 / 16.03 | 13.19 / 11.20 | 4.5 |
| `border-strong` on `bg` / `surface-2` (control borders) | 3.64 / 3.32 | 5.01 / 3.90 | 3 |
| `focus` on `bg` / `surface-2` (ring) | 5.17 / 4.72 | 10.44 / 8.11 | 3 |

Disabled controls are exempt from the contrast rule (WCAG 1.4.3); the theme dims them without hiding them.

## Options considered

- Scoping by `:root`, a class only as a second scope (the first version): loaded, the file applies to the whole page,
  so a container cannot be themed alone, and the showcase could not show it beside its own look. A class on `<html>`
  costs one attribute in the bootstrap page and gives one mechanism for the three cases.
- A CSS layer (`@layer pony-theme`), so that any application rule wins: the terminal's rules and `ponysdk.css` are
  unlayered and would beat the theme on every property both set (splitters, the date box popup, rich text borders).
  `:where()` gives the same override rule without that cost.
- `light-dark()` for each token: half the token text, but one value per scheme cannot be overridden alone, and it
  needs Chrome 123 and Safari 17.5, above the floor of ADR-0002 (Chrome 114, Safari 17).
- CSS nesting or `@scope` to write the prefix once: Chrome 120, Safari 17.2 and Firefox 146 (`@scope`), above the
  floor. The prefix is repeated instead: 9.0 KB of the raw file, 186 B of the gzip (6,168 against 5,982 with a
  one-class prefix).
- On by default, `setTheme(null)` to turn it off: it restyles every 2.x application at the upgrade.
- A `Theme` enum or one method per scheme (`PWindow.setColorScheme`): the scheme is one attribute and an application
  that wants it from Java sets it with `PScript.call` or a web component; no widget behind it needs the server to know.
- Web fonts (Inter): a download per page and a flash of fallback text; the system stack is what the platform's
  applications use.
- Primary and danger as `PButton` API (`setVariant`): a class name does the same and nothing is added to the Java API
  (ADR-0012).

## Consequences

- `style/pony-theme.css`: 43,585 B (193 rules), 6,168 B gzip (`gzip -9`; 4,523 B once comments and whitespace are
  stripped, which the file is not). Loaded only when asked for: one request, no web font, no JavaScript; the
  bootstrap page grows by one `<link>` and a class (104 B). Off, the page and the terminal are as before:
  `terminal.js` is unchanged, and so is the protocol.
- The theme sets no `display` on a `<dialog>` or a popover, which would defeat their own `display: none` when closed.
- Tests: `ApplicationConfigurationTest.testTheme`, `BootstrapServletTest` (3 cases: order of the links, the class on
  `<html>`, the manifest, absent when off); `sample/tests/theme.spec.js` (11 cases, run on Chromium, Firefox and WebKit:
  tokens in both schemes and the `[data-theme]` overrides, the contrast of 22 pairs of tokens and of 16 rendered
  widgets in both, the focus ring, the ARIA states, reduced motion, dialog and popover, the scope under a class and
  an application rule winning, the Theme page).
- A scoped theme does not reach the date box, the suggestions and the notifications, which the terminal and
  `PNotificationManager` create outside the container; the class goes on `<html>` or `<body>` for those.
- `:host(.pony-theme)` for a `<pony-app>` element was checked with a scratch page on a shadow root in the three
  engines (the selector list, the `:not()` and the attribute forms all match), not through the embedding suite.
