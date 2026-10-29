# ADR-0010: Typed Java classes for web components, generated from their Custom Elements Manifest

- Status: Accepted; amended by [ADR-0011](0011-one-way-to-listen.md) (listeners return their registration, attributes through `setAttribute`)
- Date: 2026-10-26

## Context

`PWebComponent` drives any custom element by name (`PWebComponent.java:77`):

```java
PWebComponent counter = new PWebComponent("my-counter");
counter.attr("label", "Clicks");
counter.property("count").set(0);
counter.property("content").set("\"Card body\"");   // a JSON text: the quotes are the caller's
counter.onEvent("count-changed", e -> e.getString("X"));   // the detail, a JSON string under a protocol key
counter.call("reset");
counter.slot("toolbar").add(button);
```

Every name is a string and every value is JSON text the caller writes: a misspelt property, a missing pair of
quotes or an event the element never fires compiles and fails silently in the browser. The sample shows it
(`WebComponentsPage.java`): a string property needs `"\"…\""`, the event's detail is read by its protocol key.

Web component libraries describe their elements in a Custom Elements Manifest (`custom-elements.json`, schema
1.0 and 2.x, named by the `customElements` field of the library's `package.json`): per element its tag, class,
public fields and their TypeScript types, attributes, events, methods and their parameters, slots, CSS custom
properties, descriptions and deprecations. Lit, Shoelace / Web Awesome, FAST, Spectrum and Vaadin ship one; the
`@custom-elements-manifest/analyzer` writes one for any other.

## Decision

1. The `ponysdk` jar holds a generator, `com.ponysdk.core.tools.WebComponentGenerator`: a manifest, a Java package
   and an output directory in, Java sources out. A `main` for a build (a Gradle `JavaExec`, Maven's
   `exec-maven-plugin`) and a static `generate(Path manifest, String javaPackage, Path outputDirectory)`. It reads
   the manifest with jakarta.json, already a dependency, and writes the sources as text: no new dependency. The
   sources go to the build's generated sources, not to the repository.
2. One class per declaration that has `customElement: true` and a `tagName`: `P<ClassName>` (`MyCounter` gives
   `PMyCounter`), extending `PWebComponent`, with a no-argument constructor and a `TAG` constant. Members inherited
   in the manifest (`inheritedFrom`) are generated as the class's own. `PWebComponent`'s untyped API stays
   available on the generated class for what the manifest does not say.
3. Members, each with the manifest's description as Javadoc and `@Deprecated` when deprecated:

   | Manifest | Generated | Written as |
   |---|---|---|
   | public field, not static | `setCount(T)`, `getCount()` (the cached value); no setter when `readonly` | `property(name).set(json)` |
   | attribute without a field | `setLabelAttribute(String)`; `boolean` type: `setDisabled(boolean)` adds or removes it | `attr` / `removeAttr` |
   | event | `onCountChanged(Consumer<JsonValue> detail)`, `JsonValue.NULL` for no detail | `onEvent`, the detail parsed once per event |
   | public method, not static | `reset()`, `scrollTo(double x, double y)`; returns `void`: the protocol has no way back | `call(name, args)` |
   | named slot | `slotToolbar()`, the `SlotPanel`; the default slot is `add` | `slot(name)` |
   | CSS custom property | `setCssAccentColor(String)` | `setStyleProperty("--accent-color", value)` |

4. Types, from the TypeScript text of the manifest:

   | TypeScript | Java | JSON written |
   |---|---|---|
   | `string` | `String` | quoted and escaped |
   | `number` | `double` | an integral value without `.0` (`42`, not `42.0`) |
   | `boolean` | `boolean` | `true` / `false` |
   | a union of string literals (`'light' \| 'dark'`) | a nested enum, `Theme.LIGHT` | its literal, quoted |
   | `T \| undefined`, `T \| null` | `T` boxed, `null` removes the property | as `T` |
   | any other type (arrays, objects, references) | `String`, the setter named `setStatsJson` | as given, the TypeScript type in the Javadoc |

   Typed classes for object types (`{ desk: string; trades: number }`) are not part of this ADR.
5. Names: kebab-case and camelCase become Java camelCase; a Java keyword takes a trailing `_`; a generated method
   whose signature is one `PWebComponent` already has (a `title` field gives `setTitle(String)`, which `PWidget`
   has) takes the suffix `Property` (`setTitleProperty`). The generator fails on two declarations of one tag or of
   one class name, naming both.
6. The generated class writes the frames the hand-written calls write: no protocol change, nothing in the terminal,
   `terminal.js` does not change.
7. The sample: a `custom-elements.json` beside `script/webcomponents.js` describing its four elements, the sample's
   build generates their classes into `build/generated/sources/webcomponents`, and `WebComponentsPage` uses them
   (`counter.setCount(42)`, `card.setContent("Card body")`, `stats.setStatsJson(json)`, `counter.onCountChanged`);
   its code cards show the typed calls.

## Options considered

- A Gradle plugin: a second artifact to publish to the plugin portal, and Maven builds left out. The `main` serves
  both with a few lines of build script; a plugin can wrap it later without changing the generator.
- An annotation processor (`@WebComponents("custom-elements.json")` on a package): since JDK 23 javac runs
  processors only when asked (`-proc:full`), and Gradle needs the `annotationProcessor` configuration anyway; the
  manifest is not Java source, so the processor would read a resource by hand. No gain over a build step.
- Classes written at run time (proxies over an interface the user writes): the user writes the API the manifest
  already holds.
- Typed values through jakarta `JsonValue` instead of JSON text in `PWebComponent`: a change of its public API
  for the generated code alone; the generated setters write the JSON text themselves.

## Verification

- Generator tests on a fixture manifest covering every row of the two tables, the collisions and the failures:
  the sources are compiled in the test (`javax.tools`), their methods checked by reflection, and the frames each
  setter writes compared with the hand-written `PWebComponent` calls, byte for byte.
- `webcomponents.spec.js` passes unchanged on Chrome, Firefox and WebKit with the sample on the generated classes.
- Mutation: the integral `double` written with `.0`, a string written unquoted, the `Property` suffix removed: a
  test fails for each.

## Consequences

- A manifest describes TypeScript types as text: a type the table does not know becomes JSON text, typed no
  better than today, and a manifest written by hand can be wrong about its element. The generated class is as
  right as the manifest.
- A library upgrade regenerates its classes: a removed property or event fails the application's compilation
  instead of failing in the browser.
- The jar grows by the generator, one class read only by builds.

## Outcome (2026-10-26)

- `WebComponentGenerator` (`ponysdk/src/main/java/com/ponysdk/core/tools/`): 4 classes, 36.7 KB, 17.4 KB compressed in
  the jar. `PWebComponent` gains the protected helpers the generated code calls: `jsonOf(String)`, `jsonOf(double)`,
  `stringOf`, `numberOf`, `booleanOf`, `detailOf`.
- Details the tables left open: a `readonly` field gives nothing (its getter would only ever return null); getters
  return the boxed type (`Double getCount()`, `Boolean getAnimated()`), null until set; a parameter of another type is
  passed as the string given.
- `WebComponentGeneratorTest`: 14 tests, the generated sources compiled with `-Xlint:all -Werror`; the frames of 15
  generated calls equal those of the hand-written ones. Each mutation of the Verification section fails 2, 2 and all
  of them (the fixture's `title` field no longer compiles).
- The sample: `sample/src/main/resources/script/custom-elements.json`, the task `:sample:webComponents`,
  `WebComponentsPage` on `PMyCounter`, `PMyStats`, `PMyDashboard`, `PMyCard`; `webcomponents.spec.js` unchanged,
  12/12 on Chrome, Firefox and WebKit.
