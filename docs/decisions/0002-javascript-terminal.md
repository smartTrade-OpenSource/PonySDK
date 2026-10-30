# ADR-0002: A TypeScript terminal on native elements, replacing GWT

- Status: Accepted; implemented (see Outcome)
- Date: 2026-10-20

## Context

The terminal is the browser side of PonySDK: it decodes the server's instructions, builds the DOM and sends events
back. It is written in Java and compiled to JavaScript by GWT 2.13.1 (`PonyTerminal.gwt.xml`, `gwtc` task).

Size, measured on `NC_V3`:

- `core/terminal`: 107 files, 11,414 lines; 2,893 for the core (socket, buffers, `UIBuilder`, reconnection,
  requests) and 8,521 for the 79 widget wrappers under `ui/`.
- `core/model`: 21 files, 1,548 lines, compiled on both sides. It is the protocol: `ServerToClientModel` (250 keys),
  `ClientToServerModel` (58), `WidgetType`, `HandlerModel`, `DomHandlerType` (20 event types) and the value enums.
- Output: two permutations (`safari`, `gecko1_8`) of 383 KB each, 122 KB gzip, 98 KB brotli. A forced `gwtc` takes
  7.4 s with a warm unit cache.

What GWT provides:

1. The compiler. It is the part that would be easiest to give up.
2. The widget library of `gwt-user`. The terminal imports 66 of its classes. The widgets it uses rest on 43
   classes with behaviour, 11,968 non-comment lines of GWT source. Much of it builds DOM that browsers now provide:
   tables for layout, nine-cell tables for rounded corners, an iframe in design mode for rich text, a hidden iframe
   for file upload, a JavaScript layout engine, z-index management for popups.
3. One source for the protocol: the `model` enums are the same file on both sides, so the ordinals cannot drift.
4. Utilities: JSON (35 imports), `Scheduler`, `Timer`, `History`, `Cookies`, `RegExp`, and `DateTimeFormat`, which
   parses and formats the `SimpleDateFormat` pattern `PDateBox` sends.

What GWT costs:

- `gwt-user` (17 MB) and elemental2 (1.7 MB) are on the published runtime classpath (`implementation.extendsFrom
  gwt`, `ponysdk/build.gradle:129`): every application server ships them. `PEmulator`, in main sources, imports
  `com.google.gwt.json.client.JSONObject`.
- The build needs `gwt-dev` (9.4 MB) and, for debugging, the code server (`sample` `codeserver` task).
- The terminal and the shared `model` package are held to `-sourceLevel 17` and to the subset of the JRE that GWT
  emulates.
- The output is hard to debug in production: class metadata and enum names are stripped (`PonyTerminal.gwt.xml`).

What depends on the terminal's output:

- CSS. GWT widgets emit `gwt-*` class names (`gwt-DialogBox`, `gwt-MenuItem-selected`, `gwt-TabLayoutPanelTab`...).
  core_ui styles 49 distinct `gwt-*` classes in about 60 SCSS files; `ponysdk.css` is written on them too.
- The JavaScript API. core_ui defines 75 addons with `AbstractAddon.defineAddon` (already plain JavaScript, in
  `ponysdk.js`) and calls `pony.sendDataToServer`, `pony.getHostPageBaseURL`, `pony.getContextId` and `pony.close`.
  `PonySDK` exports 20 methods through `@JsType`. A frame or a window opened by the server calls
  `parent.pony.setReadyFrame` or `opener.pony.setReadyWindow`.
- Addons receive a widget's root element and may walk into its children.

FlexLayout already runs as a plain JavaScript addon (`flexlayout.js`, 2,025 lines; `flexlayout-addon.js`, 1,098).

## Decision

### 1. The Java API and the protocol do not change

Applications keep their Java code: every `P*` class, its methods and its events stay. The wire format stays. Only
the terminal is replaced, and the DOM it builds is new.

### 2. Language and build

The terminal is written in strict TypeScript and bundled by esbuild into one ES2022 script with source maps. `tsc
--noEmit` checks types; unit tests run on `node:test`. TypeScript, esbuild and the Node type definitions are the
only development dependencies; the script has no runtime dependency. The Gradle build provisions Node and packages the script in the
jar.

### 3. One source for the protocol

The Java enums in `core/model` stay the source. A Gradle task generates `model.ts` (names and ordinals) into the
build directory before esbuild runs. Nothing generated is committed.

### 4. Runtime structure

- `Reader` and `Writer`: binary decoding for protocols v2 and v3 with the string dictionary, and the client-to-server
  messages.
- `Dispatcher`: the instruction loop and the registry of live objects by id.
- `Component`: one root element and one `AbortController`. Listeners, observers and timers are registered through
  it; destroying the component releases all of them.
- Behaviour shared by widgets (focus, enabled, text, value, handlers) is composed into components, not inherited
  through a chain like GWT's `UIObject` → `Widget` → `FocusWidget` → `ButtonBase` → `Button`.
- Each component type declares a table from model key to setter. A key a component does not handle is logged with
  its name and the component type, and the loop goes on to the next instruction.

### 5. Native elements

Each widget is the HTML element that does its job, with the ARIA roles and keyboard behaviour of the WAI-ARIA
Authoring Practices where HTML has no element.

| Server class | Element |
|---|---|
| `PButton`, `PPushButton` | `<button>` |
| `PTextBox`, `PPasswordTextBox`, `PTextArea` | `<input type="text">`, `<input type="password">`, `<textarea>` |
| `PCheckBox`, `PRadioButton` | `<label>` around `<input type="checkbox">` or `<input type="radio">` |
| `PListBox` | `<select>`, `multiple` when it allows several |
| `PLabel`, `PHTML`, `PAnchor`, `PImage` | `<div>`, `<div>`, `<a>`, `<img>` |
| `PFlowPanel`, `PSimplePanel`, `PScrollPanel`, `PFocusPanel`, `PAbsolutePanel` | `<div>` |
| `PComposite` | the element of the widget it wraps |
| `PHorizontalPanel`, `PVerticalPanel` | a flex `<div>`, one wrapper per child for the cell properties |
| `PFlexTable`, `PGrid` | `<table>` |
| `PDisclosurePanel` | `<details>` and `<summary>` |
| `PTabPanel`, `PTabLayoutPanel` | `tablist`, `tab`, `tabpanel` |
| `PStackLayoutPanel` | headers as `<button aria-expanded>` |
| `PMenuBar`, `PMenuItem` | `menubar`, `menu`, `menuitem`; submenus as popovers |
| `PTree`, `PTreeItem` | `tree`, `treeitem` on `<ul>` and `<li>` |
| `PPopupPanel`, `PDecoratedPopupPanel` | `popover="auto"` when auto-hide, `popover="manual"` otherwise; modal and glass: `<dialog>`, `showModal()`, `::backdrop` |
| `PDialogBox` | `<dialog>` with a caption bar for dragging |
| `PDecoratorPanel` | a `<div>`; the decoration is CSS |
| `PSuggestBox` | `<input role="combobox">` and a `listbox` popover |
| `PLayoutPanel`, `PRootLayoutPanel`, `PSimpleLayoutPanel` | absolute children placed by CSS constraints |
| `PDockLayoutPanel`, `PSplitLayoutPanel`, `PHeaderPanel` | CSS grid; splitters dragged with pointer events |
| `PDateBox` | `<input type="text">` formatted with the server's pattern, and a `PDatePicker` in a popover |
| `PDatePicker` | a calendar on `<table role="grid">` |
| `PRichTextArea` | a `contenteditable` `<div>` |
| `PFileUpload` | `<input type="file">`, sent with `fetch` and `FormData` |
| `PFrame`, `PElement`, `PWebComponent`, `PAddOn` | unchanged |

Popups and dialogs live in the browser's top layer: no z-index to manage. Layout animation uses CSS transitions;
`ResizeObserver` replaces GWT's resize propagation.

`PDatePicker` is a component of its own because `<input type="date">` cannot style given dates
(`addStyleToDates`), report the shown range (`addShowRangeHandler`) or hide the year arrows, and it formats dates
with the browser locale instead of the server's pattern.

### 6. Styling

Each root element carries one type class, `pony-<Type>` (`pony-Button`, `pony-DialogBox`), which `setStyleName`
does not remove. Under GWT, `setStyleName` replaced the primary `gwt-*` name as well. State is read from native
attributes and ARIA (`disabled`, `checked`, `open`, `aria-selected`, `aria-expanded`), not from dependent class
names such as `gwt-MenuItem-selected`. `ponysdk.css` is rewritten on these.

### 7. Same JavaScript API

`window.pony` keeps the 20 methods of `PonySDK`. `AbstractAddon`, `document.onPonyLoaded`,
`document.onConnectionLost` and the frame and window handshakes stay as they are.

### 8. A behaviour oracle

The DOM differs by design, so it is not compared. What must not differ is what the server sees. Playwright
scenarios drive each widget (clicks, typing, keys, focus, selection, opening and closing, dragging a splitter or a
dialog, picking a date) and server-side updates, on both terminals, and capture the frames the client sends
(`page.on('websocket')`, `framesent`). The normalized traces must be equal.

### 9. Both terminals until the JavaScript one passes

The work is done on `NC_V3`. `BootstrapServlet` serves either terminal, selected by `ApplicationConfiguration`. GWT
is the default until the oracle, the Playwright suite and the showcase pass on the JavaScript terminal. GWT is then
removed.

### 10. A legacy layer, best effort

After GWT is removed, an optional module enabled by `ApplicationConfiguration` adds the GWT class names to the new
DOM: the primary names (`gwt-Button`) and the dependent names derived from state (`gwt-MenuItem-selected`,
`gwt-RadioButton-disabled`). It does not rebuild GWT structures (the caption table of `DialogBox`, the nine cells of
`DecoratorPanel`, the tables of `MenuBar`). The table in section 5 is the migration guide for stylesheets that rely
on them.

### 11. Browsers

Current Chrome, Edge, Firefox and Safari. The terminal uses `<dialog>`, the popover API (Chrome 114, Firefox 125,
Safari 17), `ResizeObserver` and ES2022 without polyfills.

## Plan

| Phase | Content |
|---|---|
| P0 | Node, esbuild and `tsc` in Gradle; `model.ts` generation; terminal switch in `BootstrapServlet`; oracle harness with its first scenarios |
| P1 | `Reader`/`Writer` (v2, v3, dictionary), tested in Node on frames recorded by the Java encoder; `Dispatcher`, `Component`, socket, heartbeat, reconnection and its notice, `pony` API, History, Cookies, Script, Window, Frame, Element, AddOn, WebComponent |
| P2 | Label, HTML, FunctionalLabel, Anchor, Image, Button, PushButton, TextBox, PasswordTextBox, TextArea, CheckBox, RadioButton, ListBox, FileUpload; Flow, Simple, Scroll, Focus, Absolute, Horizontal and Vertical panels; Composite |
| P3 | FlexTable, Grid, DisclosurePanel, DecoratorPanel, TabPanel |
| P4 | PopupPanel, DecoratedPopupPanel, DialogBox, MenuBar, SuggestBox, Tree |
| P5 | LayoutPanel, RootLayoutPanel, SimpleLayoutPanel, DockLayoutPanel, SplitLayoutPanel, TabLayoutPanel, StackLayoutPanel, HeaderPanel, animation |
| P6 | DatePicker, DateBox and `SimpleDateFormat` patterns, RichTextArea and its toolbar, masked text box |
| P7 | `ponysdk.css` rewritten, showcase restyled, Playwright suite on the JavaScript terminal, default flipped |
| P8 | GWT removed: `gwtc`, `gwt` and `gwtdev` configurations, code server, `PonyTerminal.gwt.xml`, `core/terminal`, `-sourceLevel 17`; `PEmulator` moved to `jakarta.json` |
| P9 | Legacy layer; core_ui checked against it |

Each phase ends with the oracle green for what it covers. P1 and P2 together are the proof of concept: they give
the bundle size and the porting rate the rest is estimated from.

## Options considered

- The GWT DOM, element for element (first draft of this ADR): application CSS keeps working, but the terminal
  keeps tables for layout, nine-cell decorations, an iframe for rich text and its own popup stacking, and cannot use
  native elements, ARIA or the top layer. Rejected; the legacy layer recovers the class names.
- Plain JavaScript with JSDoc types checked by `tsc --checkJs`: same output, more verbose types. Rejected for
  TypeScript.
- Keep the GWT compiler and drop `gwt-user` for elemental2: the widget work is the same and the compiler stays.
  Rejected.
- J2CL: replaces the compiler only, brings no widgets, and adds Closure and Bazel-oriented tooling. Rejected.
- `<input type="date">` for `PDateBox` and `PDatePicker`: see section 5. Rejected.

## Consequences

- Application servers no longer ship `gwt-user` and elemental2 (18.7 MB); the build no longer needs `gwt-dev`.
- `core/model` and the server leave the GWT dialect.
- The protocol code is testable in Node.
- Keyboard handling, focus and accessibility come from native elements and ARIA patterns.
- The code read in browser devtools is the source.
- Stylesheets written on `gwt-*` classes must migrate, or rely on the legacy layer where only class names are
  involved. core_ui: 49 classes, about 60 SCSS files.
- Addons that walk into a widget's children break where the structure changed.
- Until P8, a terminal fix is made twice.
- PR #366 grows with this work and merges after P8: V3 ships without GWT.
- The bundle size is unknown until P2. Today's reference is 122 KB gzip.

## Risks

- R1. Differences the traffic oracle does not see: focus order, visual layout. Focus and keyboard get scripted
  cases per widget; layout is reviewed on the showcase.
- R2. Layout panels move from GWT's JavaScript engine to CSS: sizes an application computes from them may differ.
- R3. `SimpleDateFormat` patterns: a date typed by a user must parse as it does today. Cases are generated with the
  Java `SimpleDateFormat` and checked in Node.
- R4. A fix made on one terminal and forgotten on the other during the transition.
- R5. The cost of migrating core_ui's stylesheets is not measured.

## Outcome

P0 to P9 are done on `NC_V3` (2026-10-21): the TypeScript terminal is the only one, GWT is gone from the build, the
jar and the server. `NC_V3` was merged into master as 3.0 on 2026-10-29 (`93ae190e`, PR #366), then deleted.

- Bundle: 134 KB minified, 39 KB gzip, against 122 KB gzip for one GWT permutation; 27 KB gzip before the first
  widget since ADR-0004. The jar no longer pulls `gwt-user`, elemental2 or jsinterop; the build no longer needs
  `gwt-dev`.
- Oracle: 35 scenarios recorded with GWT pass on Chromium, Firefox and WebKit. One difference is kept on purpose:
  a clicked radio button sends its value once (GWT sent it twice). The traces stay as references; a new scenario is
  recorded on the TypeScript terminal and reviewed by hand.
- Keyboard and accessibility, which the oracle does not see (R1): 9 scripted cases (`typescript-terminal.spec.js`).
  The showcase, web component, reconnection and FlexLayout suites pass.
- R3: the date patterns are checked against Java's `SimpleDateFormat`, 98 formatted dates and 10 typed texts written by
  `TerminalFixtures`.
- Defects found on the way and fixed on the server: `PComposite`, `PMenuItem` (enabled state), `PSuggestBox` (text
  box creation). Fixed in the TypeScript terminal only: the heartbeat stopped after the first lost connection, the
  history listener was added again at each reconnection, child windows did not share the string dictionary.
- Legacy layer (P9): `ApplicationConfiguration.setLegacyClassNames(true)` loads `legacy.js` (6.8 KB, 2 KB gzip)
  before the terminal. It watches the DOM and gives the primary names, the names of the state (selected, disabled,
  open, hovered) and the names of the parts that still exist; a style name the server sets replaces the primary name
  and gives the dependent names, as in GWT. core_ui's stylesheets use 73 GWT names: 67 are given. Not given: the
  cells of the dialog box's table (`dialogTopCenterInner`, `dialogBottomCenterInner`), the tab container
  (`gwt-TabLayoutPanelContentContainer`), the push button's face (`html-face`), and two names GWT never set
  (`gwt-label`, `dialogControls`). About 20 of its selectors go through GWT's structure and match nothing: the tables
  of `Tree` and `MenuBar`, the `label` inside `CheckBox` and `RadioButton` (the label is the widget now), the `input`
  inside `DateBox` (the input is the widget), the anchors of `StackLayoutPanel`.
- Fixed after the migration, on the one terminal left: `TYPE_REMOVE_HANDLER` names its object from protocol 4
  (ADR-0003); `PHTMLTable` replays what it was given before attachment in order, `PTabLayoutPanel` creates the tab
  widgets given before attachment; `PDatePicker` shows the day of its time zone, `PDateBox` that of its format.
