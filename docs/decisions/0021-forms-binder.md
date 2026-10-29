# ADR-0021: A binder between the fields of a screen and a bean, with Bean Validation

- Status: Accepted
- Date: 2026-10-29

## Context

A form in PonySDK 2 is a `Form` of `FormField`s (`com.ponysdk.core.ui.form`): one wrapper class per widget type
(`IntegerTextBoxFormField`, `StringListBoxFormField`...), `FieldValidator`s that judge the text the box shows, and a
`DataConverter` that throws (`Integer.parseInt`) on a text that is no number. It has no bean: the application reads
each field and copies the values by hand. An error is a style class and a `data-title` attribute, which a screen
reader does not read. The people who write forms for traders want what a Java UI toolkit gives: bind the fields to a
bean's properties, convert, validate, write the bean only when everything is valid, and take the rules from the
bean's Bean Validation annotations rather than writing them twice.

## Decision

1. A new package, `com.ponysdk.core.ui.binder`, beside `form`. `Binder<B>` binds a `HasPValue<V>` (a text box, text
   area, check box, date box; `Fields.of(...)` makes one of a `PListBox` or a `ListBox<D>`) to a property by getter
   and setter references:
   `binder.forField(box).withConverter(Converters.toInteger("Not a whole number")).bind(Order::getQuantity, Order::setQuantity)`.
   `Converter<P, M>` goes from what the field holds to what the bean holds, and its way to the model returns a
   `Result` (a value or the message to show), so a bad text is an error, not an exception. `Converters` has the
   String to `Integer`, `Long`, `Double`, `BigDecimal` and `LocalDate`, and `Date` to `LocalDate` for a date box. A
   `Validator<V>` judges the value past the converters; an empty field reaches it as null.
2. Buffered and unbuffered. `readBean(bean)` shows a bean; `writeBean(target)` validates every field (all show their
   error), and only if all are valid writes the bean, then runs the bean-level validators and constraints: if one
   fails the bean is put back as it was, and the binder says so. `setBean(bean)` binds to the bean instead: each
   valid change is written at once, then the bean-level checks run (the bean is not put back in this mode).
3. The status, `Binder.Status(hasChanges, valid, beanErrors)`, is what a screen needs to enable a button or show a
   hint, and `addStatusChangeListener` is called when it changes. `hasChanges` compares each field with what it held
   when the bean was last read or written; `valid` covers every field, validated silently when the bean is read, and
   the bean's own errors.
4. What a field shows is attributes: `aria-invalid="true"` on the field, `aria-describedby` naming a message element
   (`<div role="alert" class="pony-FieldError">`) that the binder puts right after the field in its panel on the
   first error, and the message in it. A valid field loses the attribute and its message is hidden. `asRequired`
   adds `aria-required`. A check box's root is a label: its attributes go to its input (`setInputAttribute`). A date
   box's terminal removes `aria-invalid` itself when it parses the text, so the server says it again on each error
   of a date box. A field in a panel that cannot take a child after it (a `PSimplePanel`, a table cell) still gets
   `aria-invalid`, and no message: the application then shows the error where it wants, from the status. No protocol
   key, no widget type.
5. Bean Validation (`jakarta.validation`) when it is there. `new Binder<>(Order.class)` applies the constraints of a
   bound property to its field, on the converted value, with the `Default` group, and joins the messages of several
   violations in alphabetical order. The property is read from the method reference (`Order::getQuantity`,
   `isConfirmed`, a record's `quantity()`) through its `SerializedLambda`; for a lambda or another getter,
   `bind(getter, setter, "quantity")` names it, and a missing name fails at bind time rather than skipping the
   constraints. The constraints of the class are the bean's errors; one that names a bound property
   (`addPropertyNode("limitPrice")`) shows on that field; one that names a property no field is bound to is not the
   form's and is ignored.
6. `jakarta.validation-api` is `compileOnly` in `ponysdk` (it was `implementation`, for an annotation of three cell
   renderers), Hibernate Validator and `org.glassfish.expressly` are test dependencies of `ponysdk` and runtime
   dependencies of the sample. `BeanValidation`, the one class that touches `jakarta.validation`, is loaded only after
   `Class.forName("jakarta.validation.Validation")` succeeds and the provider has started; the API without a provider
   (a library brings the API along, nobody brought Hibernate) logs a warning once and applies no constraint.
7. A terminal bug found on the way: `PCheckBox.removeInputAttribute` sends `REMOVE_INPUT_ATTRIBUTE_KEY`, which the
   terminal did not handle, while it removed from the input on `REMOVE_ATTRIBUTE_KEY`, which a plain
   `removeAttribute` of the label sends. An error that left a check box stayed on its input. The check box now
   handles `REMOVE_INPUT_ATTRIBUTE_KEY` (one key in `check-box.ts`).

## Options considered

- Extend `Form` and `FormField`. A typed conversion changes the signature of `FieldValidator` (a `String`) and of
  `DataConverter` (which throws), both public; a bean needs getters and setters the wrappers have no place for; and
  the wrapper per widget type is the part to lose. `form` is left as it is, nothing is deprecated: an application
  that uses it keeps working, and a new screen uses the binder.
- Properties by name (`bind("quantity")`), as Vaadin's `BeanValidationBinder`. Reflection over getters and setters, and
  a string that a rename does not follow. Kept for the cases a method reference cannot give.
- Find the property by calling the getter on a recording proxy. Needs a bytecode library and a bean class that can be
  subclassed.
- Validate the bean for the fields' constraints. The bean would have to take the values before the fields are judged,
  which is what `writeBean` must not do.
- Create each field's message element when it is bound. It adds a child to every layout, errors or not, and the
  field may not be in its panel yet; created on the first error, it costs nothing to a valid form.
- One live region per form instead of a message per field. A screen reader user needs the message of the field they
  are on, which `aria-describedby` gives.
- Validate on each keystroke. The text box reports its value on change (blur, Enter), which is when a message helps
  without flashing while the user types; the binder follows the widget.

## Consequences

- Measured on the real write path (`./gradlew :ponysdk:bench -Pbench.args="v4 form"`, a form of 6 text fields,
  Java 25):
  - the first error of a form creates the message element: +111 B of protocol (+24 B once deflated), +0.7 µs and
    +0.4 KB allocated (+141 B with Bean Validation's longer message); a field that goes invalid then valid again
    with the element in place writes 34 B of protocol for the pair (6 B deflated), 0.4 to 0.7 µs, 200 B allocated;
  - `binder.validate()` of 6 valid fields: 0.1 to 0.2 µs and 192 B; with Bean Validation 1.3 to 2.0 µs and 4.4 KB
    (about 740 B a field, from Hibernate Validator's `validateValue`). One change of one field costs a sixth of it;
  - building the first `Binder` with a bean class starts Hibernate Validator: 469 ms once per JVM, 3 µs for the next.
- The terminal grows by 6 bytes (`terminal.js` 95 330 to 95 336 B; gzip 29 377 B, unchanged).
- Tests: 47 in `com.ponysdk.core.ui.binder` (conversion errors, validators, buffered and unbuffered, status, the
  attributes written for a text box, a list box, a date box and a check box, Bean Validation present, and absent: a
  class loader hides `jakarta.validation`, or its provider, and the binder still works); the package is covered at
  99 % of lines and branches. `sample/tests/forms.spec.js` runs the Forms page in Chromium, Firefox and WebKit,
  including the accessible name and description of a field and the check box that lost its error.
- A message placed by the binder needs a panel that takes a child after the field (`PFlowPanel` and the like);
  a field that cannot take one is marked `aria-invalid` only.
- A validator of a field sees null for an empty one: `v -> v == null || v > 0`.
- The messages of Bean Validation follow the JVM's locale, not the session's.
- An application that relied on `ponysdk` bringing `jakarta.validation-api` declares it (UPGRADING.md).
