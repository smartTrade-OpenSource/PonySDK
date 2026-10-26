# EventBus

Decouple components:

```java
// Define event
public class PriceUpdateEvent extends BusinessEvent {
    private final String symbol;
    private final double price;
}

// Subscribe
UIContext.get().getRootEventBus().addHandler(
    PriceUpdateEvent.TYPE,
    event -> priceLabel.setText(event.getPrice())
);

// Publish
UIContext.get().getRootEventBus().fireEvent(
    new PriceUpdateEvent("AAPL", 150.25)
);
```
