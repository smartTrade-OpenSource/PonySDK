# Implementation Plan: PonySDK UI TestKit

## Overview

Create a standalone Gradle module `ponysdk-testkit` providing a reflection-free inspector API for testing PonySDK UI components. Implementation proceeds bottom-up: module setup → predicates → core inspector → event simulation → specialized inspectors → integration tests.

## Tasks

- [x] 1. Set up ponysdk-testkit module structure
  - [x] 1.1 Create Gradle module and build configuration
    - Create `ponysdk-testkit/build.gradle` with `compileOnly` dependency on `ponysdk`, JUnit 4, Mockito
    - Add jqwik dependency for property-based testing (`testImplementation`)
    - Add JUnit 5 `testImplementation` dependencies
    - Configure `publishing` block identical to `ponysdk/build.gradle`: MavenPublication with `artifactId = 'ponysdk-testkit'`, GitHubPackages repository, credentials from `GITHUB_ACTOR`/`GITHUB_TOKEN` env vars
    - Set `group = 'com.ponysdk'` and reuse the root `version` so the testkit version stays in sync with ponysdk
    - Apply `withJavadocJar()` and `withSourcesJar()` like the ponysdk module
    - Update root `settings.gradle` to include `ponysdk-testkit`
    - Verify `./gradlew -PBUILD_RELEASE publish` picks up the new module (the existing `.github/workflows/release.yml` will publish it automatically on tag push — no workflow change needed)
    - Create package directories: `src/main/java/com/ponysdk/test/inspector/` and `src/main/java/com/ponysdk/test/inspector/predicate/`
    - Create test directories: `src/test/java/com/ponysdk/test/inspector/`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 Create InspectorFactory interface
    - Define `InspectorFactory<I extends InspectorWidget>` with `basePredicate()` and `create(PWidget)` methods
    - Add Javadoc explaining the typed lookup contract and extension pattern
    - _Requirements: 7.1, 7.4_

- [x] 2. Implement Predicates utility class
  - [x] 2.1 Implement Predicates factory methods
    - Create `com.ponysdk.test.inspector.predicate.Predicates` final class with private constructor
    - Implement `style(String... styleNames)` — matches widgets with ALL specified styles
    - Implement `type(Class<T>)` — matches widgets by instanceof
    - Implement `text(String)` — matches widgets whose text equals the string
    - Implement `debugId(String)` — matches widgets with specified debug ID
    - Implement `attr(String name, String value)` — matches widgets with specified attribute
    - Implement `position(int index)` — stateful predicate matching Nth widget
    - Override `toString()` on all returned predicates for descriptive error messages
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [ ]* 2.2 Write property test: style() predicate correctness (Property 9)
    - **Property 9: style() predicate correctness**
    - For arbitrary style sets S and widget with styles W, `style(S).test(w)` iff S ⊆ W
    - **Validates: Requirements 4.1**

  - [ ]* 2.3 Write property test: predicate composition (Property 10)
    - **Property 10: Predicate composition**
    - For arbitrary predicates p1, p2 and widget w: `p1.and(p2).test(w) == (p1.test(w) && p2.test(w))`, same for `.or()` and `.negate()`
    - **Validates: Requirements 4.7**

- [ ] 3. Implement InspectorWidget core (state queries and traversal)
  - [x] 3.1 Implement InspectorWidget constructor and state queries
    - Create `com.ponysdk.test.inspector.InspectorWidget` class (non-final)
    - Implement constructor with null check (throws NPE)
    - Implement static factory `of(PWidget)`
    - Implement `getText()` with polymorphic dispatch (PLabel, PButtonBase, PTextBoxBase, PHTML, PCheckBox, PElement)
    - Implement `isVisible()`, `isEnabled()`, `hasStyle(String)`, `getWidget()`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 8.4_

  - [ ]* 3.2 Write property test: wrapping round-trip (Property 1)
    - **Property 1: Inspector wrapping round-trip**
    - For any PWidget w, `InspectorWidget.of(w).getWidget() == w` (reference equality)
    - **Validates: Requirements 2.6**

  - [ ]* 3.3 Write property test: getText() correctness (Property 2)
    - **Property 2: getText() correctness**
    - For any widget with text set to string S, `InspectorWidget.of(w).getText()` returns S
    - **Validates: Requirements 2.2**

  - [ ]* 3.4 Write property test: state delegation (Property 3)
    - **Property 3: State delegation**
    - `isVisible()`, `isEnabled()`, `hasStyle()` return values consistent with widget's actual state
    - **Validates: Requirements 2.3, 2.4, 2.5**

  - [x] 3.5 Implement widget traversal (find, findAll, has)
    - Implement recursive depth-first search through `HasPWidgets` containers
    - Implement `find(Predicate)` — returns first visible match, throws AssertionError with hierarchy dump on failure
    - Implement `findAll(Predicate)` — returns all visible matches
    - Implement `has(Predicate)` — returns true if any visible match exists
    - Implement typed `find(InspectorFactory, Predicate...)` combining base + extra predicates
    - Implement typed `findAll(InspectorFactory, Predicate...)`
    - Implement `dumpHierarchy()` for error messages (class name, styles, text per widget)
    - Skip invisible widgets and their entire subtrees
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 9.1, 9.2_

  - [ ]* 3.6 Write property test: find() locates first visible match (Property 4)
    - **Property 4: find() locates first visible match**
    - For any tree with at least one visible match, `find(predicate)` returns the first in depth-first order
    - **Validates: Requirements 3.1**

  - [ ]* 3.7 Write property test: findAll() returns all visible matches (Property 5)
    - **Property 5: findAll() returns all visible matches**
    - `findAll(predicate)` returns exactly the set of all visible descendants satisfying the predicate
    - **Validates: Requirements 3.2**

  - [ ]* 3.8 Write property test: has() equivalence (Property 6)
    - **Property 6: has() equivalence**
    - `has(p)` returns true iff `findAll(p)` is non-empty
    - **Validates: Requirements 3.3**

  - [ ]* 3.9 Write property test: find() failure is descriptive (Property 7)
    - **Property 7: find() failure is descriptive**
    - When no match exists, `find()` throws AssertionError containing predicate description + hierarchy dump
    - **Validates: Requirements 3.4, 9.1, 9.2**

  - [ ]* 3.10 Write property test: invisible subtrees excluded (Property 8)
    - **Property 8: Invisible subtrees excluded**
    - Invisible containers and their children are never returned by traversal
    - **Validates: Requirements 3.6**

  - [ ]* 3.11 Write property test: typed find combines predicates correctly (Property 11)
    - **Property 11: Typed find combines predicates correctly**
    - `find(factory, extras...)` matches widgets satisfying `factory.basePredicate().and(extra1).and(extra2)...`
    - **Validates: Requirements 3.1, 4.7**

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement user action simulation
  - [x] 5.1 Implement event firing methods
    - Implement protected `fireEvent(DomHandlerType)` — builds JsonObject with `ClientToServerModel.DOM_HANDLER_TYPE` and calls `widget.onClientData()`
    - Implement protected `fireEvent(DomHandlerType, int keyCode)` — adds `VALUE_KEY` to the JsonObject
    - Implement protected `fireValueChange(String value)` — builds JsonObject with `HANDLER_STRING_VALUE_CHANGE`
    - Implement protected `checkEnabled()` — throws AssertionError if widget is disabled
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.6, 5.7, 7.2_

  - [x] 5.2 Implement public action methods
    - Implement `click()`, `doubleClick()`, `focus()`, `blur()` using `fireEvent()`
    - Implement `keyDown(int)`, `keyUp(int)` using `fireEvent(type, keyCode)`
    - Implement `type(String text)` and `type(String text, boolean characterByCharacter)`
    - All action methods call `checkEnabled()` first and return `this` for fluent chaining
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

  - [ ]* 5.3 Write property test: click() fires handler (Property 12)
    - **Property 12: click() fires handler**
    - For any enabled widget with a click handler, `click()` invokes that handler exactly once
    - **Validates: Requirements 5.1**

  - [ ]* 5.4 Write property test: keyDown()/keyUp() fires with correct code (Property 13)
    - **Property 13: keyDown()/keyUp() fires with correct code**
    - Key events carry the exact key code passed to the method
    - **Validates: Requirements 5.3, 5.4**

  - [ ]* 5.5 Write property test: type(text, false) sets value (Property 14)
    - **Property 14: type(text, false) sets value**
    - After `type(s, false)`, the widget's text equals s
    - **Validates: Requirements 5.5**

  - [ ]* 5.6 Write property test: type(text, true) fires per-character events (Property 15)
    - **Property 15: type(text, true) fires per-character events**
    - For string of length N, fires exactly N × (KEY_DOWN + VALUE_CHANGE + KEY_UP) sequences
    - **Validates: Requirements 5.5**

  - [ ]* 5.7 Write property test: disabled widget throws on action (Property 16)
    - **Property 16: Disabled widget throws on action**
    - Any action on a disabled widget throws AssertionError
    - **Validates: Requirements 5.7**

- [ ] 6. Implement InspectorInfiniteScroll
  - [x] 6.1 Implement InspectorInfiniteScroll class
    - Create `com.ponysdk.test.inspector.InspectorInfiniteScroll` extending `InspectorWidget`
    - Define `FACTORY` with base predicate matching widgets that have an `InfiniteScrollAddon`
    - Implement `simulateRender(int beginIndex, int maxSize)` — builds JsonObject and calls `onClientData()`
    - Implement `simulateFullRender()` — calls `getFullSize()` then `simulateRender(0, fullSize)`
    - _Requirements: 6.1, 6.5_

- [ ] 7. Implement InspectorListBox
  - [x] 7.1 Implement InspectorListBox class
    - Create `com.ponysdk.test.inspector.InspectorListBox` extending `InspectorWidget`
    - Define `FACTORY` with base predicate `style("dd-listbox")`
    - Implement `open()` — clicks `dd-container-button` + calls `getInfiniteScroll().simulateFullRender()`
    - Implement `close()` — hides the dropdown container
    - Implement `isOpen()` — checks for `dd-container-opened` style
    - Implement `select(String... labels)` — opens, clicks matching items, closes
    - Implement `getSelectedLabels()` — returns currently selected item labels
    - Implement `getAvailableLabels()` — returns all selectable item labels
    - Implement `filter(String text)` — types into search input
    - Implement `clear()` — clicks the clear button
    - Implement `getInfiniteScroll()` — returns InspectorInfiniteScroll for advanced use
    - Error handling: AssertionError with available labels when select fails
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 9.3_

  - [ ]* 7.2 Write property test: ListBox select round-trip (Property 17)
    - **Property 17: ListBox select round-trip**
    - `select(labels)` followed by `getSelectedLabels()` returns exactly those labels
    - **Validates: Requirements 6.3, 6.4**

  - [ ]* 7.3 Write unit tests for InspectorListBox lifecycle
    - Test open/close state transitions
    - Test filter with search enabled/disabled
    - Test clear button availability
    - Test error messages when label not found
    - _Requirements: 6.1, 6.2, 6.6, 6.7, 6.8, 6.9_

- [x] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Integration and wiring
  - [x] 9.1 Write integration tests with PSuite
    - Create test class extending PSuite
    - Test full workflow: create widget tree → wrap in InspectorWidget → find → interact → assert
    - Test InspectorListBox with real ListBox widget (mocked UIContext)
    - Test custom inspector extension pattern (create a sample custom inspector in tests)
    - Verify traversal works with widgets added to `PWindow.getMain()`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 7.1, 7.3_

  - [x] 9.2 Add Javadoc documentation for extension pattern
    - Add comprehensive Javadoc to InspectorWidget showing how to extend
    - Add Javadoc to InspectorFactory showing the FACTORY pattern
    - Include code example in Javadoc for creating a custom inspector
    - _Requirements: 7.4_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik
- Unit tests validate specific examples and edge cases
- All tests use PSuite infrastructure (mocked UIContext) — no real browser needed
- The module has zero PonySDK source modifications — purely additive

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "3.1"] },
    { "id": 3, "tasks": ["3.2", "3.3", "3.4", "3.5"] },
    { "id": 4, "tasks": ["3.6", "3.7", "3.8", "3.9", "3.10", "3.11", "5.1"] },
    { "id": 5, "tasks": ["5.2", "6.1"] },
    { "id": 6, "tasks": ["5.3", "5.4", "5.5", "5.6", "5.7", "7.1"] },
    { "id": 7, "tasks": ["7.2", "7.3", "9.1"] },
    { "id": 8, "tasks": ["9.2"] }
  ]
}
```
