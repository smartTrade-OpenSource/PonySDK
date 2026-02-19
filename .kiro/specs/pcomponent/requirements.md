# Requirements Document

## Introduction

PComponent is a modern TypeScript-based addon system for PonySDK that replaces the legacy GWT-based PAddOn. It provides a type-safe, framework-agnostic component system with support for React, Vue, Svelte, and Web Components. The system maintains low latency through differential updates (JSON Patch) and smart throttling per client connection, while integrating seamlessly with the existing PonySDK binary protocol.

## Glossary

- **PComponent**: The server-side Java base class for typed components with props diffing
- **Component_Terminal**: The TypeScript-based client-side terminal that handles component lifecycle
- **Props**: Typed state data (Java Records) sent from server to client
- **Props_Diff**: The computed difference between previous and current props
- **JSON_Patch**: RFC 6902 standard for expressing differences between JSON documents
- **Framework_Adapter**: Client-side adapter that bridges PComponent to specific UI frameworks (React, Vue, Svelte, Web Components)
- **Throttle_Config**: Configuration controlling update frequency per component
- **Update_Priority**: Priority level determining order of update delivery (HIGH, NORMAL, LOW)
- **Binary_Update**: High-frequency update mode using binary encoding for performance-critical data
- **Component_Registry**: Server-side registry mapping component signatures to their factories
- **Event_Bridge**: Bidirectional communication channel for client-to-server events

## Requirements

### Requirement 1: Server-Side Component Base Class

**User Story:** As a developer, I want a typed base class for server-side components, so that I can define components with type-safe props using Java Records.

#### Acceptance Criteria

1. THE PComponent SHALL accept a generic type parameter TProps constrained to Java Record types
2. WHEN a PComponent is created, THE PComponent SHALL serialize the initial props as full JSON
3. WHEN props are updated, THE PComponent SHALL compute the diff between previous and current props
4. WHEN a diff is computed, THE PComponent SHALL send only changed fields using JSON Patch format
5. THE PComponent SHALL maintain a reference to the previous props state for diffing
6. WHEN a component is destroyed, THE PComponent SHALL send a destroy message to the terminal

### Requirement 2: Framework-Agnostic Component Variants

**User Story:** As a developer, I want framework-specific component classes, so that I can create components targeting React, Vue, Svelte, or Web Components.

#### Acceptance Criteria

1. THE PReactComponent SHALL extend PComponent and specify React as the target framework
2. THE PVueComponent SHALL extend PComponent and specify Vue as the target framework
3. THE PSvelteComponent SHALL extend PComponent and specify Svelte as the target framework
4. THE PWebComponent SHALL extend PComponent and specify Web Components as the target framework
5. WHEN a framework-specific component is created, THE Component SHALL include the framework identifier in the creation message

### Requirement 3: Props Diffing and Update Modes

**User Story:** As a developer, I want efficient props updates, so that only changed data is transmitted over the WebSocket.

#### Acceptance Criteria

1. WHEN props are updated with changes, THE Props_Diff SHALL generate a JSON Patch array containing only modified paths
2. WHEN props are updated with no changes, THE PComponent SHALL not send any update message
3. WHEN binary update mode is enabled, THE PComponent SHALL serialize props changes using binary encoding
4. THE PComponent SHALL support three update modes: FULL_JSON for creation, JSON_PATCH for normal updates, and BINARY for high-frequency updates
5. FOR ALL valid props objects, serializing then deserializing SHALL produce an equivalent object (round-trip property)

### Requirement 4: Throttling Configuration

**User Story:** As a developer, I want to configure update throttling per component, so that I can control update frequency based on component needs.

#### Acceptance Criteria

1. THE Throttle_Config SHALL specify a minimum interval between updates in milliseconds
2. WHEN updates occur faster than the throttle interval, THE PComponent SHALL batch updates and send only the latest state
3. THE Throttle_Config SHALL support disabling throttling for real-time components
4. WHEN throttling is enabled, THE PComponent SHALL preserve the most recent props state for the next send window
5. THE Throttle_Config SHALL be configurable per component instance

### Requirement 5: Update Priority System

**User Story:** As a developer, I want to assign priorities to component updates, so that critical updates are delivered before less important ones.

#### Acceptance Criteria

1. THE Update_Priority SHALL support three levels: HIGH, NORMAL, and LOW
2. WHEN multiple updates are queued, THE PComponent SHALL process HIGH priority updates before NORMAL and LOW
3. WHEN a component is created, THE Update_Priority SHALL default to NORMAL
4. THE Update_Priority SHALL be changeable at runtime for any component

### Requirement 6: TypeScript Terminal Implementation

**User Story:** As a developer, I want a TypeScript-based terminal, so that I can use modern tooling and type safety on the client side.

#### Acceptance Criteria

1. THE Component_Terminal SHALL parse binary messages from the existing PonySDK protocol
2. THE Component_Terminal SHALL maintain a registry of active component instances by object ID
3. WHEN a component creation message is received, THE Component_Terminal SHALL instantiate the appropriate Framework_Adapter
4. WHEN a props update message is received, THE Component_Terminal SHALL apply the JSON Patch to the current props
5. WHEN a destroy message is received, THE Component_Terminal SHALL unmount the component and clean up resources
6. THE Component_Terminal SHALL coexist with the existing GWT terminal without conflicts

### Requirement 7: Framework Adapters

**User Story:** As a developer, I want framework-specific adapters, so that PComponents can render using React, Vue, Svelte, or Web Components.

#### Acceptance Criteria

1. THE React_Adapter SHALL mount React components and update them via props changes
2. THE Vue_Adapter SHALL mount Vue components and update them via reactive props
3. THE Svelte_Adapter SHALL mount Svelte components and update them via store updates
4. THE WebComponent_Adapter SHALL mount custom elements and update them via property setters
5. WHEN props are updated, THE Framework_Adapter SHALL trigger a re-render with the new props
6. WHEN a component is unmounted, THE Framework_Adapter SHALL properly dispose of framework resources

### Requirement 8: Type Generation

**User Story:** As a developer, I want automatic TypeScript interface generation from Java Records, so that client and server types stay synchronized.

#### Acceptance Criteria

1. THE Type_Generator SHALL parse Java Record definitions and produce TypeScript interfaces
2. WHEN a Java Record contains nested Records, THE Type_Generator SHALL generate nested TypeScript interfaces
3. THE Type_Generator SHALL map Java primitive types to TypeScript equivalents (int→number, String→string, boolean→boolean)
4. WHEN a Java Record contains Optional fields, THE Type_Generator SHALL mark them as optional in TypeScript
5. THE Type_Generator SHALL generate type guards for runtime validation

### Requirement 9: Event Bridge

**User Story:** As a developer, I want to send events from client components back to the server, so that I can handle user interactions server-side.

#### Acceptance Criteria

1. THE Event_Bridge SHALL serialize client events using the existing ClientToServerModel protocol
2. WHEN an event is dispatched from a component, THE Event_Bridge SHALL include the component's object ID
3. THE Event_Bridge SHALL support typed event payloads matching server-side event handlers
4. WHEN an event handler is registered on the server, THE PComponent SHALL receive the event with its payload
5. THE Event_Bridge SHALL batch multiple events within the same frame for efficiency

### Requirement 10: Protocol Extension

**User Story:** As a developer, I want the protocol extended for PComponent messages, so that the system integrates with existing PonySDK infrastructure.

#### Acceptance Criteria

1. THE ServerToClientModel SHALL include new enum values for PCOMPONENT_CREATE, PCOMPONENT_UPDATE, and PCOMPONENT_PROPS
2. THE ClientToServerModel SHALL include new enum values for PCOMPONENT_EVENT
3. WHEN a PComponent message is sent, THE Protocol SHALL use the existing binary encoding format
4. THE Protocol extension SHALL maintain backward compatibility with existing GWT widgets
5. THE Protocol SHALL support the existing message batching and compression mechanisms

### Requirement 11: Component Lifecycle Management

**User Story:** As a developer, I want proper lifecycle management, so that components are correctly mounted, updated, and unmounted.

#### Acceptance Criteria

1. WHEN a component is attached to a window, THE PComponent SHALL send a creation message with initial props
2. WHEN a component receives props updates, THE Component_Terminal SHALL invoke the adapter's update method
3. WHEN a component is detached, THE PComponent SHALL send a destroy message
4. WHEN the window is destroyed, THE PComponent SHALL clean up all attached components
5. THE Component_Terminal SHALL track component mount state to prevent duplicate mounts or updates to unmounted components

### Requirement 12: Performance Requirements

**User Story:** As a developer, I want PComponent to match or exceed PAddOn performance, so that migration does not degrade user experience.

#### Acceptance Criteria

1. THE PComponent SHALL achieve same or lower latency than PAddOn for equivalent operations
2. WHEN using JSON Patch updates, THE PComponent SHALL transmit 20x-100x less data than full JSON for typical prop changes
3. THE PComponent SHALL leverage existing batching and compression without additional overhead
4. WHEN binary update mode is used, THE PComponent SHALL minimize serialization overhead for high-frequency updates

### Requirement 13: Migration Compatibility

**User Story:** As a developer, I want PComponent to coexist with PAddOn, so that I can migrate components progressively.

#### Acceptance Criteria

1. THE PComponent system SHALL operate alongside existing PAddOn components without conflicts
2. WHEN both PComponent and PAddOn are used in the same application, THE Protocol SHALL route messages correctly to each system
3. THE Component_Terminal SHALL be loadable independently of the GWT terminal
4. WHEN migrating a component, THE Developer SHALL be able to replace PAddOn with PComponent without changing other components
