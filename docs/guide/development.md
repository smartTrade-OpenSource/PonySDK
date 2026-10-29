# Development tools

Two settings for a development machine, off by default: the inspector shows what the server sends to each element,
and hot reload shows a recompiled screen without restarting. Why they are built so:
[ADR-0016](../decisions/0016-development-tools.md).

## Debug mode and the inspector

```java
configuration.setDebugMode(true);
```

Each widget then tells the terminal, at its creation, its Java class and the line of the application that created
it. The terminal writes them on the element:

```html
<button class="pony-Button" data-pony="PButton InputsPage.java:85" data-pony-id="64">Click me</button>
```

The browser's element inspector names the Java behind each element; `data-pony-id` is the object's id, as the
server's logs print it (`#64`).

The first such widget loads the inspector (`ponyterminal/chunks/inspector.js`, 3 KB gzip). A badge, bottom right,
opens its panel; `Alt+Shift+I` too.

| Panel | What it shows |
|---|---|
| Totals | Messages and bytes received, the rate over the last 5 s, messages and bytes sent, live objects |
| Bytes by class | Per class: objects, updates, bytes received |
| Busiest objects | The ten objects that received the most bytes, with their updates and the events they sent |
| Pick | The element under the pointer, outlined, with its class, line, id, bytes and events; a click selects it, logs it to the console (`[pony] #64 PButton InputsPage.java:85`) and does not reach the application; `Escape` stops picking |
| Reset | Counts from now |

Counts start with the first widget: a few blocks before it (the context, the window) are not counted. The bytes of
an update run go to each object in turn.

What it costs: nothing when debug mode is off. On, each widget's creation carries one string more, and the server
walks the stack of each creation. Leave it off in production: the strings name the application's classes, and the
Costs page of the showcase measures the normal mode.

The showcase: `./gradlew :sample:runShowcase -Pdebug`.

## Hot reload

```java
configuration.setHotReload(true);
```

With a `JavaApplicationManager`, and the entry point's classes in a directory (`build/classes/java/main`, an IDE's
output), not a jar. The server scans that directory twice a second. Once class files changed and stayed still for
300 ms, so that a compilation is taken whole:

1. a new class loader defines again the classes of the entry point's package, and of its subpackages, found there;
2. every live context is destroyed: its page reloads, at the same history token, and its new context starts from
   the new classes.

On the showcase, a page recompiled by `javac` showed in the open browser 1.3 s later.

| Reloaded | Not reloaded |
|---|---|
| The classes of the entry point's package and subpackages, in the watched directory | The framework, the libraries, the classes of other packages (the main class, if it is elsewhere) |

What stays across a reload stays of the old classes: an object created by the main class and handed to the new
screens, a static field of a class that is not reloaded, a thread started by an old class. The state of the screens
(what a user typed) is lost: the page reloads.

A reloaded class and a class of the same package that is not (one from a jar) cannot reach each other's
package-private members after a reload (`IllegalAccessError`): a reloaded package is a new runtime package.

The showcase: `./gradlew :sample:runShowcase -PhotReload`, then recompile from the IDE, or run
`./gradlew :sample:compileJava` in another terminal.
