# Requirements: PonySDK UI TestKit

## Introduction

The UI TestKit is a standalone Gradle module (`ponysdk-testkit`) published as a Maven artifact (`com.ponysdk:ponysdk-testkit`) that provides a simple, reflection-free API for testing PonySDK UI components. It allows test authors to simulate user actions (click, type, key press) and read widget state (text, styles, visibility) through a clean inspector pattern. Users with custom widgets can extend the pattern to create their own inspectors.

## Glossary

- **Inspector**: A wrapper around a PWidget that exposes methods to query state and simulate user interactions without reflection.
- **Predicate**: A composable filter used to locate widgets in a hierarchy by style, type, text, or other properties.
- **Client Data Event**: A JsonObject sent to `PWidget.onClientData()` to simulate a browser DOM event server-side.
- **PSuite**: The existing PonySDK test base class that mocks UIContext and WebSocket for server-side widget testing.

## Requirements

### Requirement 1: Standalone Module

**User Story:** As a PonySDK user, I want to add a single test dependency to get the full testing toolkit, so that I can test my UI code without building my own test infrastructure.

#### Acceptance Criteria

1. THE testkit SHALL be a separate Gradle module named `ponysdk-testkit` in the PonySDK repository.
2. THE testkit SHALL be publishable as a Maven artifact `com.ponysdk:ponysdk-testkit`.
3. THE testkit SHALL declare `com.ponysdk:ponysdk` as a `compileOnly` dependency (provided by the consumer).
4. THE testkit SHALL declare JUnit 4 and Mockito as `compileOnly` dependencies.
5. THE testkit SHALL have zero transitive runtime dependencies beyond PonySDK itself.

### Requirement 2: Widget Inspection (InspectorWidget)

**User Story:** As a test author, I want to wrap any PWidget in an inspector and query its state, so that I can make assertions without accessing private fields.

#### Acceptance Criteria

1. THE InspectorWidget SHALL be constructable from any PWidget instance.
2. THE InspectorWidget SHALL expose `getText()` returning the displayed text regardless of widget type (PLabel, PButton, PHTML, PTextBox, PCheckBox, PElement).
3. THE InspectorWidget SHALL expose `isVisible()` returning the widget visibility state.
4. THE InspectorWidget SHALL expose `isEnabled()` returning whether the widget is interactive.
5. THE InspectorWidget SHALL expose `hasStyle(String)` returning whether the widget has the given CSS class.
6. THE InspectorWidget SHALL expose `getWidget()` returning the underlying PWidget for advanced use cases.
7. WHEN constructed with a null widget, THE InspectorWidget SHALL throw NullPointerException.

### Requirement 3: Widget Traversal

**User Story:** As a test author, I want to find child widgets by style, type, text, or custom predicate, so that I can locate specific elements in a complex widget tree.

#### Acceptance Criteria

1. THE InspectorWidget SHALL provide `find(Predicate)` returning an InspectorWidget for the first matching visible descendant.
2. THE InspectorWidget SHALL provide `findAll(Predicate)` returning a List of InspectorWidget for all matching visible descendants.
3. THE InspectorWidget SHALL provide `has(Predicate)` returning a boolean indicating if any visible descendant matches.
4. WHEN `find(Predicate)` matches nothing, IT SHALL throw AssertionError with a message describing the predicate and a dump of the visible widget hierarchy.
5. THE traversal SHALL recursively search through all containers implementing HasPWidgets.
6. THE traversal SHALL skip invisible widgets and their children.

### Requirement 4: Composable Predicates

**User Story:** As a test author, I want a concise API to express search criteria, so that I can find widgets without verbose code.

#### Acceptance Criteria

1. THE Predicates class SHALL provide `style(String... styleNames)` matching widgets that have ALL specified style names.
2. THE Predicates class SHALL provide `type(Class<T>)` matching widgets that are instances of the specified class.
3. THE Predicates class SHALL provide `text(String)` matching widgets whose text content equals the specified string.
4. THE Predicates class SHALL provide `debugId(String)` matching widgets with the specified debug ID.
5. THE Predicates class SHALL provide `position(int)` matching the Nth widget among those satisfying a sibling predicate.
6. ALL predicate factory methods SHALL return standard `java.util.function.Predicate<PWidget>` instances.
7. Predicates SHALL be combinable using standard `.and()`, `.or()`, `.negate()` methods.

### Requirement 5: User Action Simulation

**User Story:** As a test author, I want to simulate clicks, key presses, and text input on widgets, so that I can trigger handlers and verify behavior as if a real user interacted.

#### Acceptance Criteria

1. THE InspectorWidget SHALL provide `click()` that fires a CLICK DOM event via `onClientData`.
2. THE InspectorWidget SHALL provide `doubleClick()` that fires a DOUBLE_CLICK DOM event.
3. THE InspectorWidget SHALL provide `keyDown(int keyCode)` that fires a KEY_DOWN event with the given code.
4. THE InspectorWidget SHALL provide `keyUp(int keyCode)` that fires a KEY_UP event with the given code.
5. THE InspectorWidget SHALL provide `type(String text, boolean characterByCharacter)` that simulates typing. When `characterByCharacter` is true, it fires KEY_DOWN + sets HANDLER_STRING_VALUE_CHANGE progressively + fires KEY_UP for each character. When false, it sets HANDLER_STRING_VALUE_CHANGE with the full text in one shot.
6. THE InspectorWidget SHALL provide an overload `type(String text)` that defaults to `characterByCharacter = false`.
6. THE InspectorWidget SHALL provide `focus()` and `blur()` that fire FOCUS and BLUR DOM events.
7. WHEN an action is performed on a disabled widget, IT SHALL throw AssertionError with message "Cannot interact with disabled widget".

### Requirement 6: InspectorListBox

**User Story:** As a test author, I want a dedicated inspector for ListBox that handles the dropdown lifecycle, so that I can select values and verify state without knowing ListBox internals.

#### Acceptance Criteria

1. THE InspectorListBox SHALL provide `open()` that clicks the dropdown button to open the container.
2. THE InspectorListBox SHALL provide `close()` that hides the dropdown container.
3. THE InspectorListBox SHALL provide `select(String... labels)` that opens the dropdown, clicks items matching each label, then closes.
4. THE InspectorListBox SHALL provide `getSelectedLabels()` returning the list of currently selected item labels.
5. THE InspectorListBox SHALL provide `getAvailableLabels()` returning all selectable item labels from the dropdown.
6. THE InspectorListBox SHALL provide `isOpen()` returning the dropdown open state.
7. THE InspectorListBox SHALL provide `filter(String text)` that types into the search input when search is enabled.
8. THE InspectorListBox SHALL provide `clear()` that clicks the clear button when available.
9. WHEN `select(String)` is called with a non-existent label, IT SHALL throw AssertionError listing available labels.

### Requirement 7: Extensibility for Custom Widgets

**User Story:** As a developer with custom PonySDK widgets, I want to create my own inspector by extending InspectorWidget, so that I can provide domain-specific test methods for my components.

#### Acceptance Criteria

1. THE InspectorWidget class SHALL be non-final and designed for extension.
2. THE InspectorWidget SHALL expose protected helper methods: `fireEvent(DomHandlerType)`, `fireEvent(DomHandlerType, int keyCode)`, `fireClientData(JsonObject)`.
3. Custom inspectors SHALL be able to use `find()` and `findAll()` to locate internal sub-widgets.
4. THE testkit SHALL include documentation (Javadoc) showing how to create a custom inspector for a composite widget.

### Requirement 8: PSuite Compatibility

**User Story:** As a test author, I want the testkit to work with PSuite without extra setup, so that I can start using inspectors immediately in existing test classes.

#### Acceptance Criteria

1. THE testkit SHALL work within the mocked UIContext established by PSuite.
2. THE testkit SHALL NOT require a real WebSocket connection or browser.
3. WHEN a widget is added to `PWindow.getMain()`, THE InspectorWidget SHALL be able to traverse its full hierarchy including dropdown addons attached to PRootPanel.
4. THE testkit SHALL provide a static factory `InspectorWidget.of(PWidget)` as the primary entry point.

### Requirement 9: Error Reporting

**User Story:** As a test author, I want clear error messages when a widget lookup fails, so that I can quickly diagnose what went wrong.

#### Acceptance Criteria

1. WHEN `find()` fails, THE error message SHALL include the predicate description.
2. WHEN `find()` fails, THE error message SHALL include a text dump of the visible widget hierarchy showing class names, styles, and text content.
3. WHEN `select()` fails on InspectorListBox, THE error message SHALL list all available labels in the dropdown.
