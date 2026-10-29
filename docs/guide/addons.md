# Custom JavaScript: PAddOn

Integrate custom JS libraries:

```java
```java
// Server-side: a widget (here a div) whose behaviour is a JavaScript addon
public class MyChart extends PAddOnComposite<PElement> {

    public MyChart(final String color) {
        super(Element.newDiv(), color); // creation arguments: the addon's this.options
    }

    public void setData(final double... values) {
        callTerminalMethod("setData", (Object[]) toObjects(values)); // the addon's setData(v1, v2, …)
    }

    private static Double[] toObjects(final double[] values) {
        return java.util.Arrays.stream(values).boxed().toArray(Double[]::new);
    }
}
```

```javascript
// Client-side, in a script of ApplicationConfiguration.setJavascript: the name is the Java class's
AbstractAddon.defineAddon("com.example.MyChart", {
    init: function () {
        this.color = this.options[0];   // the creation arguments, as an array
    },
    setData: function () {
        var values = Array.prototype.slice.call(arguments);
        // draw them: D3.js, Chart.js, …
    }
});
```

`sample/src/main/java/com/ponysdk/sample/client/page/addon/BinaryArgsAddOn.java` and `script/sample.js` are a
working pair.

**Typed arguments travel in pure binary.** `callTerminalMethod(name, args...)` encodes `int`/`long`/`double`/`boolean`/`String`/`JsonValue` as a typed binary array — no JSON serialization on either side. Creation arguments can do the same via the `PAddOn(Object...)` (and `PAddOnComposite(widget, Object...)`) constructor; the terminal factory then receives a JS array instead of a parsed JSON object. Binary arrays carry a **uint31 length**, so they are no longer capped at 255 elements — suitable for large typed payloads. The JSON forms (`PAddOn(JsonObject)`) remain available and unchanged for backward compatibility.
