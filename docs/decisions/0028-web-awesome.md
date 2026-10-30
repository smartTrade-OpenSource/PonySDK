# ADR-0028: Web Awesome, a design system driven from Java through its Custom Elements Manifest

- Status: Accepted
- Date: 2026-10-29

## Context

PonySDK's own widgets are plain elements with a default theme (ADR-0024). A team that wants a richer set (dialog, drawer,
tabs, tree, split panel, color picker, rating, toast) has two ways today: write a `PAddOn` per component (ADR-0004), or
use `PWebComponent` untyped (ADR-0010 added typed classes from a Custom Elements Manifest, tried on the four elements of
the showcase only). Web Awesome (the successor of Shoelace, `@awesome.me/webawesome`, MIT) is a library of 73
components that declares its manifest in its `package.json` (`"customElements": "dist/custom-elements.json"`). This ADR
is the proof that a real library of that size works from Java, and the recipe for any other.

Constraints: no CDN at run time; the strict policy of ADR-0015 (`script-src 'self'`, no `eval`, no inline script); the
library loaded by the pages that use it, not by every page; no protocol key, no widget type, `terminal.js` unchanged; the
free (MIT) package only, not `@awesome.me/webawesome-pro`.

## Decision

1. **A pinned npm dependency, installed and copied by the build.** `sample/package.json` holds
   `"@awesome.me/webawesome": "3.14.0"` (published 2026-09-24, 36 days before this ADR), exact, with its lock file.
   `sample/build.gradle` applies the node plugin as `ponysdk` does (Node comes from Gradle, `npm ci --ignore-scripts
   --prefer-offline`), so `./gradlew build` needs no `npm ci` beforehand, and offline once npm's cache holds the package
   (measured: `node_modules` deleted, `./gradlew --offline :sample:webAwesomeFiles :sample:webAwesomeClasses`, 2 s). Two tasks:
   - `webAwesomeFiles` (`Sync`) copies the bundled build, `dist-cdn` (every component a module over shared chunks), minus
     type declarations, React and SSR files, into `build/generated/resources/webawesome/script/webawesome/**`: 610 files,
     3.7 MB, served by PonySDK from the classpath like any `script/` file, with the library's `LICENSE.md`.
   - `webAwesomeClasses` runs `WebComponentGenerator` on `node_modules/@awesome.me/webawesome/dist/custom-elements.json`
     into `com.ponysdk.sample.client.wa`: 73 classes, 650 KB of source, 2,956 public members, in `build/generated`.
2. **Loaded by the page that uses it.** `script/webawesome-loader.js` (2.2 KB, 1.0 KB gzip) is the only file every
   page loads (`setJavascript`). `WebAwesomePage.build` calls `PScript.call(window, "loadWebAwesome")` (by name,
   ADR-0015): the function adds the theme's style sheets and does `import()` of the library's own loader from the origin
   of the script (`'self'`). Its autoloader defines a `<wa-*>` element the first time one is in the document, fetching that
   component's module alone. The base path is set first (`utilities/base-path.js`), since the autoloader looks components up
   when its module is evaluated; the loader would otherwise search a `<script>` tag that a dynamic `import()` does not leave.
   Not `setJavascript` for the library itself: those scripts are classic scripts loaded by every page (a module cannot
   be), and every page would pay the figures below. A failed load is reported once and retried by the next page that asks.
3. **Typed classes: what the real manifest needed.** The generator (ADR-0010) failed on the first member of the library
   that its fixture did not have. Each fix is a test on a minimal excerpt that failed first, in `WebComponentGeneratorTest`:
   - *Two enums of one name*: `wa-color-picker` has a field `format` of `'hex'|'rgb'|'hsl'|'hsv'` and a method
     `getFormattedValue(format)` of eight literals; the generator threw. The second set takes its member's name in front
     (`GetFormattedValueFormat`); the same literals still share one enum; a second clash fails as before.
   - *Underscore members*: `wa-option` lists a backing field `_label` and the property `label`, both public in the
     manifest, which gave two `setLabel`. A name that starts with `_` is internal, as `#` already was.
   - *Types no JSON text can stand for*: Web Awesome lists what its decorators query and its callbacks as public
     fields (`dialog: HTMLDialogElement`, `internals: ElementInternals`, `getSymbol: (value) => string`,
     `popup: WaPopup`, `Map<string, unknown>`). `setDialogJson(String)` would let Java overwrite the dialog's own element.
     A field whose type is a DOM object, a function, a collection, a promise or a custom element of the manifest gets
     no setter and no getter, and a method with such a parameter (an event, an element, a callback) is not generated:
     803 KB of source became 650 KB.
   - *A method that is also an accessor*: the form controls' base class lists `setValue(...args)` beside a `value`
     property, which gave an overload `setValue(JsonValue)` next to `setValue(String)` (`setValue(null)` does not
     compile). The method is generated last and, when its name is one the setters, getters, listeners or slots already
     took, is named `...Method` (`setValueMethod`).
   `PWebComponent`'s own names still take `Property` (`focusProperty`, `getTitleProperty`: 41 members of the 73 classes).
4. **The theme bridge** is `css/pony-webawesome.css` in the sample: the semantic tokens of Web Awesome's default theme
   (surface, text, focus, the fill, border and text of brand, success, warning, danger and neutral, fonts, radii, focus ring,
   motion) set from the `--pony-*` of ADR-0024, with the same rules (under `:where()`, unlayered, so an application rule
   and the library's own `@layer` both lose). Its scope is an element with both classes, `pony-theme` and the library's
   `wa-theme-default`. The second is not optional: Web Awesome defines tokens in terms of others (a control's background is
   `var(--wa-color-surface-default)`) on `:root`, where a custom property is computed once and inherited as its result,
   so a subtree in the dark scheme kept white controls and invisible labels (axe: a dozen contrast failures, on the
   first attempt). `wa-theme-default` makes the library compute them on the container. Light and dark follow `prefers-color-scheme`
   or `[data-theme]` of the container with nothing else (no `.wa-dark`); a Binder error (`aria-invalid` on the element)
   sets `--wa-form-control-border-color` to `--pony-danger`, since the library exposes a validity state and styles none.
   *In the sample, not in `ponysdk`*: it is 100 lines about a third-party library, written against its version; `ponysdk`
   ships the tokens, and the guide says how to write the bridge of another library.
5. **Form controls are fields of the Binder** (ADR-0021): `WaFields.of(PWaInput | PWaTextarea | PWaSelect |
   PWaRadioGroup | PWaColorPicker)` is a `HasPValue<String>`, `of(PWaCheckbox | PWaSwitch)` a `HasPValue<Boolean>`,
   `of(PWaSlider | PWaRating)` a `HasPValue<Double>`; `forField`, converters, validators, Bean Validation, `readBean`,
   `writeBean` and `carry` work as for a text box. The manifest does not type `value` or `checked` (accessors), so the
   generated setters are `setValueJson(String)`; the adapter writes JSON itself and reads `target.value` (`target.checked`)
   from the control's `change` event, by `PEventOptions.fields` (ADR-0003). **The property is stateless**: the browser
   changes it (the user types, a dialog closes itself on Escape) and `PWebComponent` caches what the server last wrote
   and skips an equal value, so `reset` to the text the server wrote before the user typed was never sent. A stateless
   property is sent every time (test: `whatTheServerWritesIsSentEveryTimeEvenWhenItIsTheLastWrittenValue`). The page
   does the same for `open` of the dialog and the drawer.
6. **Errors on the element, and what the shadow DOM keeps out.** `FieldDisplay` sets `aria-invalid="true"` and
   `aria-describedby` on the element and puts a `role="alert"` message after it. On a custom element they land on the
   host: the `<input>` inside the shadow root, the one a screen reader focuses, has neither (checked after
   `setCustomValidity` too: `aria-invalid` stays null in the shadow input). What is announced is the alert region, when the
   message is inserted; what is seen is the border (point 4). `FieldDisplay` is unchanged: `setCustomValidity` would
   add a call per error to a control that shows nothing for it. A message read again when the user comes back to the field
   would need `aria-describedby` inside the shadow root, which the library does not offer.
7. **Icons: none from a CDN.** The library's default icon library fetches Font Awesome Free from `ka-f.fontawesome.com`.
   Its components' own icons (chevrons, close, star) come from its `system` library, inline data URIs, which the page uses
   (no request leaves the origin: asserted in `webawesome.spec.js`). An application that wants `<wa-icon name="house">`
   registers a library over its own copy of the icons (`registerIconLibrary`, `setIconPath`); the sample ships none.
8. **The showcase page** (Widgets, after Web Components): buttons, badge, tag, avatar, dialog, drawer, tabs, tree, split
   panel, slider, rating, progress bar and ring, details, tooltip, callout, toast, and the order ticket of the Forms page
   (the same `OrderTicket` bean, constraints and `carry`) on Web Awesome controls; a switch of the container's scheme
   (client commands, ADR-0027). Code panels are cut from `WebAwesomePage.java` and `WaFields.java`. Four specs join the
   Component costs page.

## Options considered

- **The library's `<script type="module" src=...>` in the page, or `setJavascript`.** Every page loads the library
  (below). PonySDK's scripts are classic. The by-name function keeps ADR-0015's rule (no inline script, no `eval`).
- **The unbundled `dist` (bare imports of `lit`) with a bundler of our own.** A bundler in the sample for a result
  `dist-cdn` already is. Rejected.
- **A CDN (`cdn.jsdelivr.net`, the library's kit).** Forbidden by the constraints; it would also need `addScriptSources`.
- **A `HasPValue` adapter in `ponysdk`** (`Fields.of(PWebComponent, event, path)`). The knowledge that is specific is the
  property, the event and the path of each control, which is the library's; a generic adapter would be a second way to
  say what a typed class says. Kept in the sample; the guide shows how to write one.
- **`setCustomValidity` from `FieldDisplay`** (point 6). No gain with this library; a call per error; a hook that any
  custom element would take. Reconsider for a library whose controls show a custom error.
- **Mapping Web Awesome's colour scales** (`--wa-color-brand-60`...) instead of the semantic tokens. 11 steps per hue and
  four hues to derive from one accent; the semantic layer is what the components read. Components that read a scale
  directly stay on the library's palette.
- **Filtering the manifest's noise in `custom-elements.json`** (the `handle*` and lifecycle methods that are public in
  the manifest: `setValidity`, `formResetCallback`, `handleSizeChange`...). Left: they are harmless, and the manifest has
  no rule for "internal" that the generator could apply.

## Consequences

- **Cost of the library, measured** (Chromium, the showcase on localhost, the Forms page then the Web Awesome tab, cold
  cache): 172 files fetched (130 chunks, 27 components,
  the loader, 12 style sheets): **805,513 B raw, 205,361 B gzip** (the server compresses: 209,961 B of response bodies).
  Scripts 691,750 B (188,224 gzip), style sheets 113,763 B (17,137 gzip). From the click on the tab to the elements
  defined and rendered: 169 ms; second visit, from the browser's cache: 175 ms, 0 B. A page that does not use the library
  loads the 2.2 KB loader only: no request under `webawesome/` (asserted). Not all 73 components: the 27 of the page. A
  component added to a page adds its module (the chunks are shared).
- **The server's wire, per component** (Component costs page, 100 instances each, protocol bytes to create / wire):

  | Native | | Web Awesome | |
  |---|---|---|---|
  | `PButton` | 14 B / 2.7 B | `PWaButton` (+ its text in a `span`) | 45 B / 16 B |
  | `PTextBox` | 9.0 B / 2.0 B | `PWaInput` | 13 B / 3.2 B |
  | `PCheckBox` | 17 B / 2.3 B | `PWaCheckbox` (+ text) | 48 B / 17 B |
  | `PListBox`, 5 items | 67 B / 16 B | `PWaSelect`, 5 `PWaOption` (+ texts) | 318 B / 104 B |

  Server memory per instance: 80 B, 96 B, 96 B, 504 B against 248 B, 104 B, 248 B, 2.8 KB. The difference is the child
  widget that holds a text (`PWebComponent` has no text of its own: a `span` per label) and, for the select, one widget
  per option. The whole Web Awesome page (about 130 widgets, with the code panels' text) is 57.9 KB in 4 frames in the 1.8 s after
  the click; the Forms page, native, 19.2 KB in 2: not a like-for-like figure, the page shows ten times more.
- **`terminal.js` is unchanged**: 97,742 B, 30,278 B gzip. No `ponysdk` runtime class changed (the generator is a build
  tool); no protocol key; `UPGRADING.md` has nothing to say.
- **The policy.** The page's own policy is `script-src 'self' 'sha256-…'`. Web Awesome works under it: no violation in
  the browsers' recorders (Chromium, Firefox, WebKit: `webawesome.spec.js`, the library's modules, shadow roots built
  from constructed style sheets, the system icons as `data:` images, the dialog in the top layer, popups, tooltip,
  toast). Nothing to configure for scripts. Styles: PonySDK sets no `style-src` (ADR-0015), and the library does need
  nothing of it. Tried alone on a page under `default-src 'none'; script-src 'self'; style-src 'self'; img-src data:;
  connect-src 'self' data:; font-src 'self'`: the twelve components tried render, with 9 `style-src-attr` violations (inline
  `style=""` attributes of the library's templates); `style-src-attr 'unsafe-inline'` removes them with no violation left.
  A page under `style-src 'self'` in full does not start today: the terminal writes `<style>` elements (ADR-0015),
  which is not this ADR's. `connect-src` must allow `data:` for the system icons.
- **Accessibility**: axe (WCAG 2.1 A and AA) finds nothing on the page in light and dark, in Chromium, Firefox and
  WebKit, with the order ticket's errors shown, and `a11y-audit.spec.js` lists the page. What axe does not see is
  point 6. The library's own labels are real labels (`label` is wired to its inner input). The sample's tests drive the controls
  by mouse and by `Escape`, not by every key: the keyboard patterns of the select, the tree and the color picker are the
  library's, not checked here.
- **Traps, for the guide**: a property the browser changes (`open`, `value`, `checked`, the active tab) is declared
  `stateless()` or the server's copy is stale (point 5). A library's events can bubble (a nested component's `wa-after-hide`
  reaches the dialog's element): a Java listener sees the detail, not the target, so a dialog that holds other components
  needs the event's `target` read by path. The detail of an event that holds elements
  (`wa-selection-change`: `{ selection: [WaTreeItem] }`) cannot be JSON: read fields by path
  (`PEventOptions.fields("detail.selection.0.id")`). An element named by `for` (a tooltip) needs a DOM id
  (`forceDomId`). `wa-split-panel`, `wa-rating`'s symbols and `wa-slider`'s thumb take pointer events on inner parts.
- **A version of the library is one line of `package.json`**, with its lock file; the classes and files follow in the
  build; Dependabot already watches `/sample`. A removed or renamed member fails the sample's compilation, which is the
  point of typed classes. The bridge is written against 3.14 and may need new tokens.
- **Tests.** `WebComponentGeneratorTest`: the four fixes above, each first failing on its excerpt. `WaFieldsTest` (10): a
  value read on `change`, handlers added and removed, stateless writes, escaping, booleans, numbers, the Binder converting,
  validating, marking and writing the bean. `webawesome.spec.js` (8, in Chromium, Firefox and WebKit): the library loaded
  by its page alone and from the origin only, rendering under the policy, events reaching the server (button, dialog twice,
  drawer, tabs, tree, split panel, tag, details, slider, rating), the ticket's errors and save and reset, the theme's
  tokens in both schemes and the container's switch, axe in both schemes. `a11y-audit.spec.js` has the page.
- **Not done**: the library's translations (`registerTranslation`; the page is English); a Playwright run of every
  component (73); keyboard tests of the select, the tree and the color picker.
