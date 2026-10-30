# Client commands

A click can show a panel, toggle a class or take the focus without waiting for the server: the terminal runs the
commands the server declared, in the browser, on the DOM event. Why, and what was measured:
[ADR-0027](../decisions/0027-client-commands.md).

```java
final PCommands commands = PCommands.NONE.toggle(panel).toggleClass(menu, "open").focus(input);

// the click is also sent: the server's listener runs, after the commands
button.addEventListener("click", PEventOptions.NONE.commands(commands), e -> save());

// no listener: the browser does it all and sends nothing
button.addEventListener("mouseenter", PEventOptions.NONE.commands(PCommands.NONE.addClass(hint, "on")));
```

## Commands

`PCommands` is immutable: each method returns a copy with the command added. They run in the order declared. A target
is a widget, or a composite that wraps one (`IsPWidget`).

| Command | In the browser |
|---|---|
| `show(w)`, `hide(w)` | What `setVisible(true)` and `setVisible(false)` render: `display: none` and `aria-hidden` |
| `toggle(w)` | `hide` if the widget shows, `show` if its `display` is `none` |
| `addClass(w, name)`, `removeClass(w, name)`, `toggleClass(w, name)` | The class on the element; `removeClass` keeps the widget's type class (`pony-Button`) |
| `setAttribute(w, name, value)`, `removeAttribute(w, name)` | The attribute; the value is text |
| `focus(w)` | The focus, on the control inside a widget that holds one (a check box's input) |
| `dispatch(w, name)` | A bubbling `CustomEvent` of that name on the widget: its listeners by name (`addEventListener`) run |

A name is one token: a class or an event name with white space, or an empty one, is refused when the command is
built (`IllegalArgumentException`). An attribute name is a plain name, and not an event handler's: `onclick` or any
name starting with `on` is refused, here and again by the terminal. Nothing a command does is code: no selector, no
script, the page needs no `'unsafe-eval'` ([security](security.md)).

## Sent to the server, or not

| You write | The terminal |
|---|---|
| `addEventListener(name, options, listener)` with `options.commands(...)` | runs the commands, then sends the event (at the rate of `throttle` or `debounce` if you set one) |
| `addEventListener(name, options)`, commands only | runs the commands, sends nothing |

- Commands are not rate limited: only the sending is. A `debounce` of 300 ms on `input` still lets a command run on
  every key.
- `lock()` holds the sending alone: commands run on every event, locked or not.
- `preventDefault()` and `stopPropagation()` apply before the commands.
- A name has either listeners or commands alone: `addEventListener(name, options)` on a name that has a listener, and
  the reverse, throw `IllegalStateException`. To have both, give the commands to the listener. Registering commands
  alone again for a name replaces the previous ones.
- The registration is a `HandlerRegistration`: `removeHandler()` stops the terminal listening, as for a listener.
- It needs protocol 4, as `addEventListener` always did.

## A target that is gone

A command looks its target up when the event happens. A target the server has collected, or not created yet, is
skipped without a word; the other commands run and the event is sent. A command can hold no widget alive: it keeps the
id.

## What the server knows

Nothing. A command changes the browser; the server's view of the widget is what the server set.

- `isVisible()` stays true after a `hide` command. A later `panel.setVisible(true)` sends nothing, because the server
  thinks it is already visible: the panel stays hidden. The server also drops the events of a widget it believes
  hidden, not of one a command hid.
- The same for style names and attributes: `addStyleName` of a name the server has, `setAttribute` with a value it
  holds, send nothing. `setStyleName` with another list replaces the class attribute, and the classes of the commands
  with it.
- A page reloaded, or moved to another server ([deployment](deployment.md)), shows what the server holds.

If the server must know, give the commands to a listener and repeat what they did. Use `show` and `hide`, not `toggle`:
an answer that comes late then repeats the state the user chose, instead of undoing a click.

```java
open.addEventListener("click", PEventOptions.NONE.commands(PCommands.NONE.show(panel)), e -> panel.setVisible(true));
close.addEventListener("click", PEventOptions.NONE.commands(PCommands.NONE.hide(panel)), e -> panel.setVisible(false));
```

## When not to

A command cannot read the event, branch, or wait for data. A reaction that depends on any of those stays a listener
on the server. A command is for what the browser can do alone: a menu, a tab, a focus, a hint, a class for a style.

## In the showcase

The Client commands page (Under the hood) puts a panel shown by the server next to one shown by the terminal, with
a checkbox that delays the server's answers by 800 ms, a button that asks the server what it sees of the panel, and
one that removes the panel to show the skipped target. Its code panels are cut from the page's source.
