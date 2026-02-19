# Implementation Plan: PComponent

## Overview

This plan implements PComponent in phases: protocol extensions first, then server-side Java components, then client-side TypeScript terminal and adapters. Property-based tests validate correctness at each step.

## Tasks

- [x] 1. Extend Protocol Models
  - [x] 1.1 Add PComponent enum values to ServerToClientModel
    - Add PCOMPONENT_CREATE, PCOMPONENT_UPDATE, PCOMPONENT_PROPS_FULL, PCOMPONENT_PROPS_PATCH, PCOMPONENT_PROPS_BINARY, PCOMPONENT_FRAMEWORK, PCOMPONENT_SIGNATURE
    - _Requirements: 10.1_
  
  - [x] 1.2 Add PComponent enum values to ClientToServerModel
    - Add PCOMPONENT_EVENT, PCOMPONENT_EVENT_TYPE, PCOMPONENT_EVENT_PAYLOAD
    - _Requirements: 10.2_
  
  - [x] 1.3 Add COMPONENT to WidgetType enum
    - Add new widget type for PComponent
    - _Requirements: 10.1_

- [x] 2. Implement Core Server Components
  - [x] 2.1 Create FrameworkType enum
    - Define REACT, VUE, SVELTE, WEB_COMPONENT values
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/FrameworkType.java`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  
  - [x] 2.2 Create UpdatePriority enum
    - Define HIGH, NORMAL, LOW with ordering
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/UpdatePriority.java`
    - _Requirements: 5.1_
  
  - [x] 2.3 Create ThrottleConfig class
    - Implement interval configuration and enable/disable
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/ThrottleConfig.java`
    - _Requirements: 4.1, 4.3, 4.5_
  
  - [x] 2.4 Create PropsDiffer class
    - Implement JSON diff computation using javax.json.JsonPatch
    - Implement binary diff for high-frequency updates
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/PropsDiffer.java`
    - _Requirements: 1.3, 1.4, 3.1, 3.4_
  
  - [x] 2.5 Write property test for PropsDiffer round-trip
    - **Property 2: Props Diff Round-Trip**
    - **Validates: Requirements 1.3, 1.4, 3.1, 6.4**
  
  - [x] 2.6 Write property test for no-change detection
    - **Property 3: No-Change Detection**
    - **Validates: Requirements 3.2**

- [x] 3. Implement PComponent Base Class
  - [x] 3.1 Create PComponent abstract class
    - Extend PObject with generic TProps constraint
    - Implement props storage, diffing integration, throttle config
    - Implement setProps with diff computation
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/PComponent.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  
  - [x] 3.2 Implement enrichForCreation for full JSON serialization
    - Send initial props as full JSON on component creation
    - _Requirements: 1.2, 3.4_
  
  - [x] 3.3 Implement saveUpdate for differential updates
    - Send JSON Patch for normal updates, binary for high-frequency
    - _Requirements: 3.1, 3.3, 3.4_
  
  - [x] 3.4 Write property test for props serialization round-trip
    - **Property 1: Props Serialization Round-Trip**
    - **Validates: Requirements 3.5**

- [x] 4. Implement Framework-Specific Components
  - [x] 4.1 Create PReactComponent class
    - Extend PComponent with REACT framework type
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/PReactComponent.java`
    - _Requirements: 2.1, 2.5_
  
  - [x] 4.2 Create PVueComponent class
    - Extend PComponent with VUE framework type
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/PVueComponent.java`
    - _Requirements: 2.2, 2.5_
  
  - [x] 4.3 Create PSvelteComponent class
    - Extend PComponent with SVELTE framework type
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/PSvelteComponent.java`
    - _Requirements: 2.3, 2.5_
  
  - [x] 4.4 Create PWebComponent class
    - Extend PComponent with WEB_COMPONENT framework type
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/PWebComponent.java`
    - _Requirements: 2.4, 2.5_
  
  - [x] 4.5 Write property test for framework adapter instantiation
    - **Property 7: Framework Adapter Instantiation**
    - **Validates: Requirements 2.5, 6.3**

- [x] 5. Implement Throttle Controller
  - [x] 5.1 Create ThrottleController class
    - Implement scheduled update batching
    - Preserve latest props state during throttle window
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/ThrottleController.java`
    - _Requirements: 4.2, 4.4_
  
  - [x] 5.2 Write property test for throttle batching
    - **Property 4: Throttle Batching Preserves Latest State**
    - **Validates: Requirements 4.2**

- [x] 6. Implement Update Prioritizer
  - [x] 6.1 Create UpdatePrioritizer class
    - Implement priority queue for updates
    - Process HIGH before NORMAL before LOW
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/UpdatePrioritizer.java`
    - _Requirements: 5.2, 5.3, 5.4_
  
  - [x] 6.2 Write property test for priority ordering
    - **Property 5: Priority Ordering**
    - **Validates: Requirements 5.2**

- [x] 7. Checkpoint - Server-Side Complete
  - Ensure all Java tests pass, ask the user if questions arise.

- [x] 8. Set Up TypeScript Project
  - [x] 8.1 Create TypeScript project structure
    - Create `ponysdk-component-terminal/` directory
    - Set up package.json, tsconfig.json
    - Add dependencies: fast-json-patch, fast-check (dev)
    - _Requirements: 6.6, 13.3_
  
  - [x] 8.2 Define core TypeScript interfaces
    - ComponentMessage, FrameworkAdapter, ComponentEvent
    - File: `ponysdk-component-terminal/src/types.ts`
    - _Requirements: 6.1_

- [x] 9. Implement Component Terminal
  - [x] 9.1 Create ComponentRegistry class
    - Implement component storage by object ID
    - Implement adapter factory registration
    - File: `ponysdk-component-terminal/src/ComponentRegistry.ts`
    - _Requirements: 6.2, 6.3_
  
  - [x] 9.2 Create ComponentTerminal class
    - Implement binary message parsing
    - Handle create, update, destroy messages
    - File: `ponysdk-component-terminal/src/ComponentTerminal.ts`
    - _Requirements: 6.1, 6.3, 6.4, 6.5_
  
  - [x] 9.3 Write property test for protocol message round-trip
    - **Property 6: Protocol Message Round-Trip**
    - **Validates: Requirements 6.1, 10.3**

- [x] 10. Implement Framework Adapters
  - [x] 10.1 Create base FrameworkAdapter interface
    - Define mount, unmount, setProps, applyPatches, applyBinary
    - File: `ponysdk-component-terminal/src/adapters/FrameworkAdapter.ts`
    - _Requirements: 7.5, 7.6_
  
  - [x] 10.2 Create ReactAdapter class
    - Implement React 18 createRoot mounting
    - Implement props update and patch application
    - File: `ponysdk-component-terminal/src/adapters/ReactAdapter.ts`
    - _Requirements: 7.1_
  
  - [x] 10.3 Create VueAdapter class
    - Implement Vue 3 createApp mounting
    - Implement reactive props updates
    - File: `ponysdk-component-terminal/src/adapters/VueAdapter.ts`
    - _Requirements: 7.2_
  
  - [x] 10.4 Create SvelteAdapter class
    - Implement Svelte component mounting
    - Implement store-based updates
    - File: `ponysdk-component-terminal/src/adapters/SvelteAdapter.ts`
    - _Requirements: 7.3_
  
  - [x] 10.5 Create WebComponentAdapter class
    - Implement custom element mounting
    - Implement property setter updates
    - File: `ponysdk-component-terminal/src/adapters/WebComponentAdapter.ts`
    - _Requirements: 7.4_
  
  - [x] 10.6 Write property test for mount state idempotence
    - **Property 11: Mount State Idempotence**
    - **Validates: Requirements 11.5**

- [x] 11. Implement Event Bridge
  - [x] 11.1 Create EventBridge class
    - Implement event batching within animation frame
    - Implement binary encoding for ClientToServerModel
    - File: `ponysdk-component-terminal/src/EventBridge.ts`
    - _Requirements: 9.1, 9.2, 9.5_
  
  - [x] 11.2 Integrate EventBridge with adapters
    - Pass EventBridge to adapters for event dispatch
    - _Requirements: 9.3, 9.4_
  
  - [x] 11.3 Write property test for event bridge serialization
    - **Property 9: Event Bridge Serialization**
    - **Validates: Requirements 9.1, 9.2, 9.5**

- [x] 12. Checkpoint - Client-Side Complete
  - Ensure all TypeScript tests pass, ask the user if questions arise.

- [x] 13. Implement Type Generator
  - [x] 13.1 Create Java Record parser
    - Parse Record definitions using reflection or annotation processing
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/typegen/RecordParser.java`
    - _Requirements: 8.1_
  
  - [x] 13.2 Create TypeScript interface generator
    - Generate interfaces from parsed Records
    - Handle nested types, primitives, Optional
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/typegen/TypeScriptGenerator.java`
    - _Requirements: 8.2, 8.3, 8.4_
  
  - [x] 13.3 Create type guard generator
    - Generate runtime validation functions
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/typegen/TypeGuardGenerator.java`
    - _Requirements: 8.5_
  
  - [x] 13.4 Write property test for type generation correctness
    - **Property 8: Type Generation Correctness**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4**

- [x] 14. Implement Server-Side Event Handling
  - [x] 14.1 Add event handler registration to PComponent
    - Implement onEvent method with typed handlers
    - File: Update `PComponent.java`
    - _Requirements: 9.4_
  
  - [x] 14.2 Create ComponentEventHandler interface
    - Define handler contract for component events
    - File: `ponysdk/src/main/java/com/ponysdk/core/ui/component/ComponentEventHandler.java`
    - _Requirements: 9.3, 9.4_

- [x] 15. Performance Optimization
  - [x] 15.1 Implement binary update mode in PComponent
    - Add method to send binary-encoded props for high-frequency updates
    - Integrate with PropsDiffer.computeBinaryDiff
    - _Requirements: 3.3, 12.4_
  
  - [x] 15.2 Write property test for JSON Patch data reduction
    - **Property 10: JSON Patch Data Reduction**
    - **Validates: Requirements 12.2**

- [x] 16. Integration and Wiring
  - [x] 16.1 Register PComponent in UIBuilder
    - Add component type handling to existing infrastructure
    - _Requirements: 13.1, 13.2_
  
  - [x] 16.2 Create terminal entry point
    - Export ComponentTerminal for browser loading
    - File: `ponysdk-component-terminal/src/index.ts`
    - _Requirements: 13.3_
  
  - [x] 16.3 Add build configuration
    - Configure TypeScript compilation for browser deployment
    - _Requirements: 13.3_

- [x] 17. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- All tasks including property tests are required
- Java implementation uses jqwik for property-based testing
- TypeScript implementation uses fast-check for property-based testing
- Minimum 100 iterations per property test
- The TypeScript terminal can be loaded alongside or instead of GWT terminal
