# Forms

A form binds the fields of a screen to the properties of a bean: the binder converts what a field holds, validates
it, shows what is wrong on the field, and writes the bean only when everything is valid. Why it is built so, and what
was measured: [ADR-0021](../decisions/0021-forms-binder.md). The Forms page of the showcase is a working order ticket.

```java
final Binder<Order> binder = new Binder<>(Order.class);
binder.forField(instrument).bind(Order::getInstrument, Order::setInstrument);
binder.forField(quantity).withConverter(Converters.toInteger("Not a whole number"))
    .bind(Order::getQuantity, Order::setQuantity);

binder.readBean(order);                  // the fields show the bean
save.addClickHandler(e -> {
    if (binder.writeBean(order)) service.save(order); // all valid: the bean took the values
});
```

`instrument` and `quantity` are `PTextBox`es, in a panel (`PFlowPanel`) before they are bound or after. The binder
is used on the UI thread of its context, like the widgets.

## Fields, converters, validators

`forField` takes a `HasPValue<V>`: `PTextBox`, `PTextArea`, `PCheckBox`, `PDateBox`. A list box holds a selection, not
a value; `Fields.of` makes a field of it:

```java
binder.forField(Fields.of(sideBox, Side.class)).bind(Order::getSide, Order::setSide);   // PListBox: addItem("Buy", Side.BUY)
binder.forField(Fields.of(venueBox)).bind(Order::getVenue, Order::setVenue);            // ListBox<Venue>, one selection
```

What the field holds goes through the converters and validators in the order they are added, and the first error
stops the chain.

| Method | Does |
|---|---|
| `withConverter(Converter<V, T>)` | The value so far becomes a `T`; what is bound is the last one |
| `withValidator(Validator<V>)`, `withValidator(Predicate<V>, message)` | Checks the value so far |
| `asRequired(message)` | Refuses an empty field (no value, or a blank text) and marks it `aria-required`; put it first to judge the text |
| `bind(getter, setter)` | Ends the binding. A null setter makes it read only |

`Converters` has `toInteger`, `toLong`, `toDouble`, `toBigDecimal` and `toLocalDate` (an ISO date) from a text, each
with the message of a conversion error, and `dateToLocalDate(zone)` for a `PDateBox`'s `Date`. A blank text is no
value (null), no value shows as an empty text. Your own, from two functions:
`Converter.of(Lot::parse, Lot::toString, "Not a lot")` (an `IllegalArgumentException` or `DateTimeException` of the first
is the conversion error). A validator that is a lambda returns `Result.ok(value)` or `Result.error(message)`.

A validator sees null for an empty field: `v -> v == null || v > 0`.

A text box reports its value on change (when it loses the focus, or on Enter), which is when a message helps: the
field does not flash an error while the user is still typing.

## Buffered and unbuffered

- `readBean(bean)` shows the bean and holds nothing of it. `writeBean(target)` validates every field, showing each
  error, and writes only if all are valid: the bean is written all or nothing. Then it runs the bean-level checks
  (below); if one fails, the bean is put back as it was. It returns true when the bean was written. `readBean(null)`
  empties the fields.
- `setBean(bean)` binds to the bean: each valid change is written to it at once, an invalid one is not, and the
  bean-level checks run after each write (the bean is not put back in this mode). `setBean(null)` unbinds.
- `validate()` validates every field now, and the bean when it is bound. `Binding.validate()` does one field, which
  `bind` returns: for a field whose validity depends on another.

## What the form is

```java
binder.addStatusChangeListener(status -> {
    unsaved.setVisible(status.hasChanges());                // a field differs from the bean last read or written
    save.setEnabled(status.valid());                        // every field, and the bean, are valid
    problems.setInnerText(String.join(" ", status.beanErrors()));
});
```

The listener is called when the status changes, and `getStatus()` reads it. A form read from a bean that is not valid
is invalid at once, with no error shown: the errors show as the user edits, or on `validate()` and `writeBean`.
A button that stays enabled and says what is wrong on click is easier to use with a keyboard and a screen reader
than a disabled one.

## What a field shows

An error is on the field, for the eyes and for a screen reader, in attributes only:

```html
<input aria-invalid="true" aria-describedby="pony-error-93" ...>
<div id="pony-error-93" role="alert" class="pony-FieldError">Not a whole number</div>
```

The message element is created at the first error, right after the field in its panel; the next errors reuse it, and a
valid field loses `aria-invalid` and `aria-describedby`, its message hidden. A screen reader reads the message as it
appears (`role="alert"`) and as a description when the field takes the focus. The binder does not style anything:
`.pony-FieldError` and `[aria-invalid="true"]` are yours, and the colour must not be the only sign (the showcase puts a
symbol in front of the message with `content: "..." / ""`, which a screen reader skips).

- A `PCheckBox`'s attributes are on its input, not on its label root.
- A field in a panel that cannot take a child after it (`PSimplePanel`, a table cell) is marked `aria-invalid` and has no
  message: show the error from the status. Wrap the field in a `PFlowPanel` to have one.
- Give each field a real label: `field.forceDomId()` and a `<label for>` (`Element.newLabel()`), as the Forms page does.

## Bean Validation

With `jakarta.validation` on the classpath and a provider such as Hibernate Validator, `new Binder<>(Order.class)`
applies the constraints of the bean to its fields:

```java
public class Order {
    @Pattern(regexp = "[A-Z]{6}", message = "Six capital letters, like EURUSD")
    private String instrument;
    @NotNull @Min(1) @Max(1_000_000)
    private Integer quantity;
    // getters, setters
}
```

```groovy
dependencies {
    implementation 'jakarta.validation:jakarta.validation-api:3.1.1'
    runtimeOnly 'org.hibernate.validator:hibernate-validator:9.1.5.Final'
    runtimeOnly 'org.glassfish.expressly:expressly:6.0.0'     // the expression language of the messages
}
```

- A constraint of a bound property applies to its field, to the value past the converters (a `@Min` of an `Integer`
  judges the number, a conversion error comes first). Several violations show together, in alphabetical order.
- The property is the one of the method reference: `Order::getQuantity` and `Order::isConfirmed` are `quantity` and
  `confirmed`, a record's `Order::quantity` is `quantity`. For a lambda, name it:
  `bind(o -> o.getQuantity(), Order::setQuantity, "quantity")`. A getter that tells no name fails at `bind`, so a
  constraint is never skipped without saying; a name that is no property fails there too.
- The constraints of the class are the bean's errors, checked when the bean has the values (`writeBean`, or each
  write of `setBean`): the status has them in `beanErrors`, the bean is put back, and a rule across fields is one
  annotation:

  ```java
  @LimitNeedsPrice               // a limit order has a limit price
  public class Order { ... }
  ```

  A class-level constraint that names a bound property (`context.buildConstraintViolationWithTemplate(message)
  .addPropertyNode("limitPrice").addConstraintViolation()`) shows on that field instead. One that names a property no
  field is bound to is not the form's, and is ignored.
- Only the `Default` group applies. The messages are in the JVM's locale.
- Your own bean-level rule, without Bean Validation: `binder.withValidator(order -> ...)`, run after the bean has the
  values.

Without `jakarta.validation` on the classpath, or with it and no provider (a library brought the API, nobody brought
Hibernate Validator: one warning in the log), nothing breaks and no constraint applies: the converters and validators
of the binding are still there. `new Binder<>()` never applies a constraint. The first binder of a bean class starts
Hibernate Validator, about half a second once per JVM.

## Testing

A form is tested with [`PSuite`](testing.md) and no browser: `PEmulator.valueChange` and a client message to the widget
play the user, the binder's status and the widget's attributes (`getAttribute("aria-invalid")`) are the result.
`BinderTest` of `ponysdk` does it for a text box, a list box, a date box and a check box.

## The older `form` package

`com.ponysdk.core.ui.form` (`Form`, `FormField`, `FieldValidator`) still works and is unchanged. It judges the text a
box shows and has no bean; a new screen uses the binder.
