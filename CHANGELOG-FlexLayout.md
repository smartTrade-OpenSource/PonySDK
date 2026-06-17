# FlexLayout Changelog

## 1.0.0 (2026-06-14)

Initial release of the FlexLayout component for PonySDK.

### Layout Core
- Dockable panel layout with drag & drop tab management
- Splittable tabsets (top/bottom/left/right edge splits)
- Splitter resize with real-time layout adjustment
- Tab close, reorder, rename (F2 / slow double-click)
- Double-click tab to maximize/restore tabset
- Middle-click to close tab
- Ctrl+W close active tab
- Empty tabset placeholder ("Drop here")

### Sidebars (Border Panels)
- Left (2 zones: top/bottom), Right (2 zones), Bottom
- Toggle visibility per zone
- Slide open/close with 150ms animation
- Split panel when both zones open (with draggable splitter)
- Snap-to-close when resized below 80px (opacity feedback)
- Collapse button to hide entire lateral at once
- Resize with real-time layout tracking (center + bottom pushed)
- Tab styles: auto, icon, label, iconLabel (configurable per zone)
- Panel header with tab name + close button

### Badges & Notifications
- Badge on sidebar tabs: dot (colored glow) or number bubble
- Skinnable badge colors per theme
- Auto-clear on tab click or panel content click
- Toast notifications (info/success/warning/error with auto-dismiss)

### Drag & Drop
- Drag tabs between tabsets (reorder, move, split)
- Drag tabs between layout and sidebars
- External drag sources (register any widget as drag source)
- Drop zone highlight with pulsing animation
- Enlarged sidebar drop zone (+15px) for easier targeting

### Pop-out / Pop-in
- Float mode (overlay div, draggable + resizable)
- Window mode (separate browser window)
- Pop-in restores tab to original position
- Sidebar panel pop-out support

### Persistence & Model
- Save/load model as JSON
- Model migration API (version field + handler for schema evolution)
- Auto-save (debounced 2s after last change)
- Undo/redo (Ctrl+Z/Y, 50 entries max, widget rehydration on restore)

### Keyboard Shortcuts (configurable)
- Ctrl+B: toggle left sidebar
- Ctrl+J: toggle bottom sidebar
- Ctrl+W: close active tab
- Ctrl+P: command palette
- Ctrl+Z/Y: undo/redo
- F2: rename tab
- Escape: close all sidebar panels
- Full keymap customizable via `FlexKeymap` builder
- macOS Cmd key support (Cmd = Ctrl)

### Command Palette
- Ctrl+P opens search overlay
- Lists all open tabs (layout + sidebars)
- Type to filter, Enter to navigate, Escape to close

### Context Menu
- Right-click on tabs: Close, Close Others, Move to Center
- Respects lock mode and enableClose flags

### Theming (11 themes)
- Default (Catppuccin), Light, Gray (VS Code)
- Nord, Solarized Dark, GitHub Dark, Monokai
- Corporate Light, Deep Orange, Rounded, Underline
- All colors via CSS custom properties (`--fl-*`)
- Sidebar-specific variables for fine-grained control

### Performance
- Lazy tab content (factory called on first select only)
- Fast path: SELECT_TAB, MAXIMIZE_TOGGLE, splitter drag (0 re-render)
- Partial render for SELECT_BORDER_TAB
- DocumentFragment batch DOM insertion
- ResizeObserver (replaces per-action window resize events)
- CSS containment (`contain: layout style paint`)
- `content-visibility: auto` for sidebar strip overflow

### Accessibility
- ARIA roles (tablist, tab, tabpanel)
- Arrow key navigation in sidebar strips
- Keyboard focus management
- RTL support (auto-detect, swap left/right)
- Touch gestures (swipe from edge to open sidebars)

### App Integration Hooks
- `onBeforeAction`: block specific actions or protect tabs
- `onTabVisible` / `onTabHidden`: lifecycle events for subscriptions
- `onTabSelected`: server notification on tab change
- `getOpenTabs` / `getLayoutSummary`: introspection
- `setBlockedActions` / `setBlockedTabs`: declarative protection
- `setLocked(true)`: full read-only mode (no drag/close/resize)
- Tab metadata (`setTabConfig` / `getTabConfig`)
- Collaboration API (`applyRemoteAction`, `setOnActionBroadcast`)

### Advanced Features
- Pinned tabs (no close, no drag, pin icon)
- Tab groups with colors (colored underline)
- Status bar (PWidget slot at layout bottom)
- Max tabs per tabset (configurable limit)
- Tab tooltip on hover (full name)

### Java API
- Package: `com.ponysdk.core.ui.flexlayout`
- Typed builders: `FlexTab`, `FlexTabset`, `FlexBorder`, `FlexLayoutModel`
- Enums: `FlexAction`, `FlexTheme`, `TabStyle`
- Config: `FlexKeymap`, `KeyBinding` (all modifier combinations)
- Final classes, private constructors, package-private internals
- No JSON in public API surface
- Full JavaDoc

### Testing
- 30 JUnit unit tests (model builders, serialization)
- 81 Playwright E2E tests (functional)
- 6 visual snapshot tests (regression detection)
- Memory profiling test (50 open/close cycles)
- Load test (50 simultaneous tabs)

### CI/CD
- GitHub Actions: build → JUnit → E2E → publish
- Retries for flaky tests in CI
- Test artifacts on failure

### Bundle Size
- `flexlayout.js`: ~94 KB (raw), ~72 KB (minified)
- `flexlayout.css`: ~33 KB
