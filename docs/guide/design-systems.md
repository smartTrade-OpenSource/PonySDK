# Design systems

A library of web components (Web Awesome, Shoelace, Vaadin's, Material Web, your company's) used from Java as typed
classes, served by your application, loaded by the pages that use it. Web Awesome is the worked example: the
[showcase](../../sample/src/main/java/com/ponysdk/sample/client/WebAwesomePage.java) page "Web Awesome" and
[ADR-0028](../decisions/0028-web-awesome.md), which has the measurements. The steps hold for any library that ships a
[Custom Elements Manifest](https://custom-elements-manifest.open-wc.org/) (`custom-elements.json`: Lit, Shoelace, Web
Awesome, FAST, Spectrum, Vaadin; `@custom-elements-manifest/analyzer` writes one for another).

## 1. The dependency, and its files, from the build

A pinned npm dependency, an exact version with its lock file. The node plugin installs it in the build (as `ponysdk`'s
own terminal does), so a clean checkout builds with no manual `npm ci`, and offline once npm's cache holds the package:

```groovy
plugins { id 'com.github.node-gradle.node' version '7.1.0' }
node { version = '24.22.1'; download = true; nodeProjectDir = projectDir; npmInstallCommand = 'ci' }
tasks.named('npmInstall') { args = ['--ignore-scripts', '--prefer-offline'] }

def library = layout.projectDirectory.dir('node_modules/@awesome.me/webawesome')
def libraryFiles = tasks.register('webAwesomeFiles', Sync) {
    dependsOn 'npmInstall'
    from(library.dir('dist-cdn')) { include '**/*.js', 'styles/**/*.css'; into 'script/webawesome' }
    from(library.file('LICENSE.md')) { into 'script/webawesome' }
    into layout.buildDirectory.dir('generated/resources/webawesome')
}
sourceSets.main.resources.srcDir(libraryFiles)
```

PonySDK serves `script/webawesome/**` from the classpath, from the application's own origin: `'self'` for the policy
(ADR-0015), no CDN. Copy the library's *bundled* build (`dist-cdn` here: its dependencies inside, ES modules with
relative imports), not the one that imports bare names (`lit`), which needs a bundler.

## 2. The typed classes, from its manifest

`package.json` of the library names its manifest (`"customElements"`). Feed it to the generator
([web components](web-components.md#typed-classes-from-a-custom-elements-manifest)):

```groovy
def classes = tasks.register('webAwesomeClasses', JavaExec) {
    classpath = configurations.runtimeClasspath
    mainClass = 'com.ponysdk.core.tools.WebComponentGenerator'
    dependsOn 'npmInstall'
    def manifest = library.file('dist/custom-elements.json')
    def output = layout.buildDirectory.dir('generated/sources/webawesome')
    inputs.file(manifest); outputs.dir(output)
    doFirst { output.get().asFile.deleteDir() }
    argumentProviders.add({ [manifest.asFile.path, 'com.example.wa', output.get().asFile.path] } as CommandLineArgumentProvider)
}
sourceSets.main.java.srcDir(classes)
```

One class per element, `PWaButton` for `<wa-button>`: a setter per property and attribute (an enum for a union of
literals), a listener per event, the methods, a `slotFooter()` per named slot, `setCss...` per CSS custom property.
What a real manifest holds that the generator leaves out, so the class has nothing you cannot call: members whose name
starts with `_` or `#`, fields and parameters of a type JSON cannot stand for (a DOM element, an event, a function, a
`Map`, another custom element of the manifest). When two members would give one Java signature, one is renamed
(`Property` for what `PWebComponent` already has, `Method` for a method named like a property's accessor, the member's
name in front of a second enum of the same name); the generator prints each rename.

A property the manifest does not type (an accessor: Web Awesome's `value` of a form control) is `setValueJson(String)`,
the JSON text. Anything else the manifest does not say, the untyped `PWebComponent` API does.

## 3. Loaded by the page that uses it

A script of `setJavascript` is a classic script that every page loads. The library is a set of ES modules: a small
script, loaded by every page, that loads them when a page asks, by name (a function of `window`, as
[ADR-0015](../decisions/0015-content-security-policy.md) asks, no inline script):

```javascript
window.loadWebAwesome = function () {
    // base: the folder of this script's library, absolute, from document.currentScript.src
    return import(base + 'utilities/base-path.js').then(function (path) {
        path.setBasePath(base); // the autoloader looks its components up there when its module is evaluated
        return import(base + 'webawesome.loader.js');
    });
};
```

```java
PScript.call(PWindow.getMain(), "loadWebAwesome"); // in the build of the page that uses the library
```

The library's autoloader defines a `<wa-*>` element the first time one is in the document and fetches that component's
module alone. In Web Awesome's showcase, 27 components, 172 files, 205 KB gzip on the page that uses them, and 1 KB gzip
(the loader) on the others. The same function adds the library's theme style sheet and the bridge below.

## 4. The theme bridge

A library reads its look from custom properties. Set them from PonySDK's tokens ([theme](theme.md)), on the container of
the pages that use the library, in a style sheet of your own:

```css
:where(.pony-theme.wa-theme-default) {
    --wa-color-surface-default: var(--pony-surface);
    --wa-color-text-normal: var(--pony-text);
    --wa-color-brand-fill-loud: var(--pony-accent);
    --wa-color-brand-on-loud: var(--pony-on-accent);
    /* ... one line per token the components read; css/pony-webawesome.css has 66 */
    --wa-border-radius-m: var(--pony-radius);
}
```

Light and dark follow, with the system's `prefers-color-scheme` and the container's `[data-theme]`, since `--pony-*` do.
Two rules that cost a debugging session each:

- **The library's own scope class on the same element** (`wa-theme-default`). A library that defines a token in terms of
  another on `:root` (a control's background is `var(--wa-color-surface-default)`) has it computed there, once, and
  inherited as a result: your override of the other does not reach it in a subtree. The library's theme class recomputes
  them on the container.
- **Under `:where()` and unlayered**: no specificity, so the application's rules win, and a library that puts its theme in
  a `@layer` loses to anything unlayered whatever the order of the sheets.

Where it lives: in your application, written against the version you pin. `ponysdk` ships the tokens only.

## 5. Form controls in a Binder

The [Binder](forms.md) binds a `HasPValue<V>` to a bean's property. A form control of a library is one when an adapter
reads its value from its event and writes it as a property; `WaFields` of the sample has nine:

```java
binder.forField(WaFields.of(instrument)).bind(Order::getInstrument, Order::setInstrument);          // PWaInput
binder.forField(WaFields.of(side)).withConverter(constant(Side::valueOf, Side::name)).bind(...);     // PWaSelect
binder.forField(WaFields.of(quantity)).withConverter(Converters.toInteger("Not a whole number")).bind(...);
binder.forField(WaFields.of(confirmed)).bind(Order::isConfirmed, Order::setConfirmed);               // PWaCheckbox
binder.carry("ticket"); // the typed values survive a move of the page, as for a text box
```

Writing one for another library is about ten lines per control: the property (`value`, `checked`), the event (`change`),
the path of the value in the event (`target.value`), how to read it and how to write it:

```java
element.addEventListener("change", PEventOptions.fields("target.value"), e -> { value = e.getString("target.value"); fire(); });
```

**The property the browser changes is `stateless()`.** `PWebComponent` keeps what the server last wrote and sends nothing
when the next write is equal. A user types `XXXXXX` over `EURUSD`; the server's Reset writes `EURUSD`; it is equal to the
last write, and the browser keeps `XXXXXX`. `element.property("value").stateless()` sends every write. The same for any
property the library changes itself: `open` of a dialog (Escape closes it), the active tab, `checked`.

**Errors.** The Binder sets `aria-invalid` and `aria-describedby` on the element and puts a `role="alert"` message after
it. The element is the custom element, not the `<input>` in its shadow root that a screen reader focuses: those two
attributes do not reach it. The message is announced when it appears; the bridge draws the control's border from
`[aria-invalid=true]`. A library whose controls show a custom error (`setCustomValidity`) can be told too:
`element.call("setCustomValidity", message)`. Web Awesome's show nothing for it, and the shadow `<input>` gets no
`aria-invalid` either.

## 6. Events

`onWaTabShow(detail -> ...)` is a custom event's `detail`, parsed. A native event (`change`, `input`, `click`) has none:
`addEventListener("click", PEventOptions.NONE, e -> ...)`, with the fields the server needs by path
(`PEventOptions.fields("target.value")`). A detail that holds elements, as Web Awesome's tree selection, is not JSON: read
a field of it (`detail.selection.0.id`). Several things need no round trip at all: [client commands](client-commands.md)
(open, toggle a class, set `data-theme`).

## 7. The policy

Web Awesome runs under PonySDK's `script-src 'self'` with nothing configured, in Chromium, Firefox and WebKit:
no `eval`, constructed style sheets, its system icons inline as `data:` images. Two things to know:

- A page that sets `style-src` without `'unsafe-inline'` blocks the `style=""` attributes of the library's templates
  (9 on the sample's components): add `style-src-attr 'unsafe-inline'`. PonySDK sets no `style-src` and its terminal writes
  `<style>` elements, so such a page is yours to build.
- The library's default *icon* library is Font Awesome Free on a CDN. Use `library="system"` icons, or register your own
  library over files you serve (`registerIconLibrary`, `setIconPath`), or an icon is a request to another origin.

## Checking it

The sample's tests are the pattern: a spec that records `securitypolicyviolation`, asserts that a page that does not use
the library requests none of it and that none of its requests leaves your origin; one that drives each component and
checks what the server received; the theme in both schemes; the axe audit of the page in both schemes
(`sample/tests/webawesome.spec.js`, `sample/tests/a11y-audit.spec.js`, `WaFieldsTest`).
