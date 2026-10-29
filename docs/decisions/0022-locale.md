# ADR-0022: A locale per context: the visitor's language, texts, formats and direction

- Status: Accepted
- Date: 2026-10-29

## Context

A PonySDK application had no notion of the visitor's language:

- `PString` read the framework's messages (`conf/MessagesCore`) in the JVM's default locale, one bundle, English only.
- The browser's `Accept-Language` reached the server in the handshake's headers (`UIContext.getHeader`) and nothing
  used it.
- The date picker and the date box wrote English month and day names (GWT's default locale), began the week on
  Monday, and read only English names back.
- Nothing told the browser the page's language or direction: no `lang`, no `dir="rtl"`.
- A reload keeps the history token and the cookies (ADR-0017), and so does the move of a page to another server.

An application for traders in Paris, Cairo and London needs the locale to be chosen once, to survive a reload and a
server move, to reach the texts, the numbers, the dates and the direction of the page, and to cost nothing to an
application that has one language.

## Decision

1. **Supported locales.** `ApplicationConfiguration.setSupportedLocales(List<Locale>)`; empty, the default, turns
   everything below off: contexts have the JVM's default locale and no page is touched.
2. **Negotiation.** `UIContext.getLocale()` is, in this order: the locale in the cookie `ponysdk-locale`; the best
   match of `Accept-Language` (RFC 4647 lookup, so `fr-CH` gets `fr`; then a locale of the same language, so `de`
   gets `de-CH`); the first supported locale. A value that is malformed or not supported is ignored. The handshake's
   cookie header is read at the context's creation: a reload and a page moved by a drain (ADR-0017) arrive with it.
   The bootstrap page runs the same function (`Locales.negotiate`).
3. **Changing it: reload.** `UIContext.setLocale(Locale)` takes the supported locale that fits (else
   `IllegalArgumentException`), writes the cookie (one year, path `/`, `SameSite=Lax`) and destroys the context as
   `SessionAdmin.destroy` does (`DESTROY_CONTEXT`): the client reloads its page at its history token and comes back
   in the new locale. The current locale changes nothing, so an entry point may call it at each start. An embedded
   context waits 500 ms for its terminal's cookie request (ADR-0009, decision 7) before it ends.
4. **Texts.** `Messages.of("i18n/App")` names `ResourceBundle`s; `get(key, args...)` reads the current context's
   locale (`get(locale, key, args...)` from another thread). A key is searched in the bundles in order; in each, the
   locale, its language, then the base file, never the JVM's default locale. With arguments the text is a
   `MessageFormat` in the locale, and a plural is a `choice`. A key no bundle has gives `!key!` and one warning. A
   pattern in error gives its text as written and one error. `PString` keeps its API, reads the same chain in the
   context's locale, and the framework's messages exist in French and Arabic.
5. **Formats.** `Formats.of(locale)` / `Formats.current()`: `number`, `percent`, `currency`, `date`, `dateTime` and
   `dateFormat(...)`, the `SimpleDateFormat` a `PDateBox` takes, in digits 0-9 (`nu-latn`).
6. **The terminal reads `lang`.** No text travels: the date picker and the date box take the names of months, days and
   day periods from the browser's `Intl` for the `lang` of the page, the week's first day and weekend from
   `Intl.Locale.getWeekInfo` where the browser has it. Without a `lang`, as before: English, Monday. The digits typed
   in Arabic or Persian are read.
7. **`lang` and `dir` on the root.** The page's `PRootPanel` is created with `lang` and `dir` (`rtl` for Arabic,
   Hebrew, Persian, Urdu...) when the application has supported locales, so that its children find them. The
   bootstrap page is served as `<html lang dir>` (first paint in the right direction, `Vary: Accept-Language,
   Cookie`). The terminal's root panel copies the body's `lang` and `dir` to the document element, where the browser
   reads the direction of the scrollbars. In a `<pony-app>` element the container has them, and the host page's root
   is not touched.
8. **Direction in the terminal.** The date picker's arrows move the way the page reads, its month buttons are turned
   about (`:dir(rtl)`), the date box's popup opens under the field's right edge; a tree's indent is
   `padding-inline-start`. The other physical assumptions are listed in the consequences.

## Options considered

- **Re-render in place on a change.** The application sets its texts imperatively (`setText(messages.get(...))`):
  the framework cannot find what to rewrite, so each screen would implement a listener and rebuild itself. The
  terminal computes the picker's names, week and arrows at creation, and the direction of a whole page changes with
  one attribute: in place, every date widget would be rebuilt too, or new protocol keys sent (there are none left to
  take). A reload is one rule, the machinery of ADR-0017, and the application keeps what it needs in the token and
  its cookies. It costs the context's state and a page load: 96 to 161 ms, median 128 ms, from click to the new
  title, on a local showcase.
- **An `I18NProvider` interface as in Vaadin.** More general, and every application writes the bundle loading, the
  fallback and the missing key behaviour. The bundles are the format Java applications have; `Messages.of` is the
  provider for them.
- **ICU4J for plurals and formats.** CLDR's plural rules for every language, a dependency of about 14 MB. The JDK has no
  plural rule API; `ChoiceFormat` writes the singular and plural of English and French, and of Arabic up to 99
  (`0#...|1#...|2#...|3#...|11#...`). What a modulo decides (Arabic 103 to 110, Polish, Russian) is not expressible:
  such an application adds ICU4J for those texts. Open question Q1: a plural rule table when a supported language
  needs it.
- **A missing key that throws**, as Spring's `MessageSource`: a page would fail for a missing translation. `!key!` is
  visible in a test and in a review, and the page goes on.
- **`ResourceBundle.getBundle(name, locale)` as is**: when the locale has no file, the JVM's default locale is tried
  before the base file; a French server would show French to an Arabic visitor of an application with no Arabic.
  A `Control` without fallback leaves it out.
- **The root panel's `lang` and `dir` written before the entry point** (`PWindow.getMain().getPRootPanel()` in
  `ApplicationManager`): built first, it takes an object id, and the behaviour oracle's traces
  (`sample/tests/oracle.spec.js`) number the objects. Written by `PRootPanel`'s constructor, the ids do not move.
- **`dir` on the body only**: the document element decides the side of the scrollbar and the page's scroll origin.
- **A cookie only when the visitor chooses**: the cookie is written by `setLocale` only; the browser's languages decide
  until then.

## Consequences

- Measured: `terminal.js` 95,330 -> 95,698 B (29,377 -> 29,509 gzip), the document element's copy of `lang` and
  `dir`; the date picker chunk 15,629 -> 17,789 B (5,647 -> 6,411 gzip), loaded with the first date widget.
- On the wire, the root's `lang` and `dir` are 23 bytes once per page (a root and a label: 34 -> 57 B, string
  dictionary on, before compression) with supported locales; none without.
- Per call, on a warm JVM (2 million calls, `ThreadMXBean`): `Messages.get` without arguments 42 ns, 136 B; with one
  argument 266 ns, 1,320 B; a plural 356 ns, 2,224 B (4,294 ns, 6,832 B before the parsed patterns were kept: a
  `MessageFormat` is parsed once per locale and pattern, and cloned for each call). `Formats.number` 275 ns, 888 B;
  `date` 111 ns, 640 B. The negotiation, once per context and once per bootstrap request: 1.1 us, 6.8 KB.
- Tests: Java (`LocalesTest`, `MessagesTest`, `FormatsTest`, `UIContextLocaleTest`, `BootstrapServletTest`,
  `PStringTest`); Node (`locale.test.ts`, which also compares the terminal's names with `SimpleDateFormat`'s for
  French and Arabic from a fixture the JDK writes: the server reads the terminal's text back with the JDK's
  names); Playwright (`sample/tests/i18n.spec.js`, three browsers).
- Limits, not lifted:
  - The date picker's button labels (`Previous month`) and the date box popup's (`Choose a date`) stay English: no
    text travels and no protocol key is left.
  - Physical assumptions that remain: `LayoutPanel`, `DockLayoutPanel` and `SplitLayoutPanel` place by left and
    right, and `DockLayoutPanel` puts LINE_START and LINE_END where it puts WEST and EAST (`layout.ts`), which GWT
    swaps in a right-to-left page; the arrows of a menu, a tab panel and a tree are not turned
    about; a mouse event's position relative to a scrolled element adds `scrollLeft`, negative in a right-to-left
    one; the server's popup positions are left coordinates. The showcase, in Arabic, is mirrored by its flexbox
    layout alone.
  - A window opened by `PWindow.open` has `lang` and `dir` on its body, not on its document element.
  - The week's first day needs `Intl.Locale.getWeekInfo`: Chromium and Safari have it, Firefox keeps Monday.
  - The cookie of an embedded page follows the browser's rules for third-party cookies (ADR-0009).
  - Another tab of the same browser keeps its locale until it reloads.
- 3.0 behaviour changes (UPGRADING.md): on a JVM whose default locale is French or Arabic, the framework's messages
  are now in that language; `PString.get` after `switchLocal` has no fallback to the JVM's locale.
