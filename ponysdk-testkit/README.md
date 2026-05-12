# PonySDK UI TestKit

A reflection-free, inspector-based API for testing PonySDK UI components server-side. Drive
widgets the way a user would - click, type, keypress, focus, blur, select, filter - with no
real browser, no private-field access and no WebSocket round trip.

## Who it's for

Developers writing JUnit tests against PonySDK widgets. The testkit plugs into the existing
`PSuite` base class (mocked `UIContext` and WebSocket) and can wrap any `PWidget` with
`InspectorWidget.of(widget)`.

## Installation

### Maven

```xml
<dependency>
    <groupId>com.ponysdk</groupId>
    <artifactId>ponysdk-testkit</artifactId>
    <version>${ponysdk.version}</version>
    <scope>test</scope>
</dependency>
```

### Gradle

```gradle
testImplementation "com.ponysdk:ponysdk-testkit:${ponysdkVersion}"
```

The testkit declares `ponysdk`, JUnit 4 and Mockito as `compileOnly` dependencies - they are
provided by your project so the testkit adds zero transitive runtime weight.

## Basic usage

```java
import static com.ponysdk.test.inspector.predicate.Predicates.style;
import static com.ponysdk.test.inspector.predicate.Predicates.text;
import static com.ponysdk.test.inspector.predicate.Predicates.type;

import com.ponysdk.test.inspector.InspectorWidget;

InspectorWidget panel = InspectorWidget.of(myPanel);

// Query state
assertTrue(panel.isVisible());
assertEquals("Hello", panel.find(type(PLabel.class)).getText());

// Drive actions (fluent chaining)
panel.find(type(PTextBox.class)).type("user@example.com").blur();
panel.find(style("submit-button")).click();

// Assert on the outcome
assertEquals("Saved", panel.find(style("status")).getText());
assertTrue(panel.has(text("Welcome, user@example.com")));
```

Traversal is pre-order depth-first over every `HasPWidgets` container. Invisible subtrees
are skipped. When `find()` misses, the resulting `AssertionError` includes the predicate
description and a text dump of the visible hierarchy.

## Typed inspectors

Specialized inspectors expose a `FACTORY` constant for typed lookups:

```java
// Locate a ListBox and select a value
InspectorListBox country = panel.find(InspectorListBox.FACTORY);
country.select("France");
assertEquals(List.of("France"), country.getSelectedLabels());

// Narrow down with extra predicates (AND-combined with the factory's base predicate)
InspectorListBox account = panel.find(InspectorListBox.FACTORY, attr("aria-label", "account"));
InspectorListBox third   = panel.find(InspectorListBox.FACTORY, position(2));
```

## Custom inspector

Create your own inspector by extending `InspectorWidget` and exposing an `InspectorFactory`
constant:

```java
public class InspectorPriceSpinner extends InspectorWidget {

    public static final InspectorFactory<InspectorPriceSpinner> FACTORY =
            new InspectorFactory<InspectorPriceSpinner>() {
                public Predicate<PWidget> basePredicate() {
                    return Predicates.style("price-spinner");
                }
                public InspectorPriceSpinner create(PWidget widget) {
                    return new InspectorPriceSpinner(widget);
                }
            };

    public InspectorPriceSpinner(PWidget widget) {
        super(widget);
    }

    public InspectorPriceSpinner increment() {
        find(Predicates.style("btn-up")).click();
        return this;
    }

    public String getValue() {
        return find(Predicates.style("value-display")).getText();
    }
}

// In tests
InspectorPriceSpinner spinner = panel.find(InspectorPriceSpinner.FACTORY);
spinner.increment();
assertEquals("101.5", spinner.getValue());
```

The factory pattern keeps lookups typed at compile time - no reflection, no casts.

## Built-in inspectors

| Inspector | Purpose |
| --- | --- |
| `InspectorWidget` | Core inspector - works on any `PWidget`. State queries, traversal, actions. |
| `InspectorListBox` | Dropdown lifecycle (open/close/select/filter/clear), selection read-back. |
| `InspectorInfiniteScroll` | Simulates `InfiniteScrollAddon` render/scroll events for pagination tests. |

## Available predicates

| Predicate | Matches widgets |
| --- | --- |
| `style(String...)` | carrying **all** supplied CSS class names |
| `type(Class<? extends PWidget>)` | that are instances of the given class |
| `text(String)` | whose resolved text equals the supplied string |
| `debugId(String)` | with the supplied debug ID |
| `attr(String name, String value)` | whose attribute `name` equals `value` |
| `position(int)` | the Nth widget (0-based) among those satisfying the other predicates |

All predicates return standard `java.util.function.Predicate<PWidget>` instances and combine
with `.and()`, `.or()`, `.negate()`. Each overrides `toString()` for descriptive failure
messages.

## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
