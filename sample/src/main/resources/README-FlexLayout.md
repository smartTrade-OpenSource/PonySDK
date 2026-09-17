# FlexLayout for PonySDK

## Overview

FlexLayout is a server-side Java API for building IDE-style dockable tab layouts in PonySDK applications. It provides resizable split panels, draggable tabs, collapsible sidebars, keyboard shortcuts, and theming — all driven from Java with no client-side code required.

## Quick Start

```java
import com.ponysdk.sample.client.page.addon.flexlayout.*;

// Build the layout model
FlexLayoutModel model = FlexLayoutModel.row()
    .tabset(FlexTabset.create().weight(70)
        .tab(FlexTab.create("Editor").component("code-editor").icon("fa-code"))
        .tab(FlexTab.create("Preview").component("preview")))
    .tabset(FlexTabset.create().weight(30)
        .tab(FlexTab.create("Console").component("console").pinned()));

// Configure borders (sidebars)
FlexBorder leftBorder = FlexBorder.left().size(250).tabStyle(TabStyle.ICON);
FlexBorder bottomBorder = FlexBorder.bottom().size(200);

// Set up keyboard shortcuts
FlexKeymap keymap = FlexKeymap.defaults()
    .bind(FlexAction.COMMAND_PALETTE, KeyBinding.ctrlShift("p"));

// Apply a theme
FlexTheme theme = FlexTheme.NORD;
```

## Features

- **Split panels** — horizontal and vertical splits with draggable splitters
- **Tabsets** — tabbed containers with drag-and-drop reordering
- **Tab management** — close, pin, rename, maximize, pop-out/pop-in
- **Sidebars** — collapsible left, right, and bottom borders with icon/label strips
- **Keyboard shortcuts** — configurable keybindings for all layout actions
- **Themes** — 11 built-in themes (light, dark, nord, solarized, etc.)
- **Drag and drop** — between tabsets, to/from sidebars, external drag sources
- **Undo/redo** — full history of layout mutations
- **Layout lock** — prevent user modifications when needed
- **Persistence** — serialize/deserialize model as JSON

## API Reference

### Layout

| Class | Purpose |
|-------|---------|
| `FlexLayoutModel` | Root model (row or column of tabsets) |
| `FlexTabset` | Container for tabs with weight and max children |

```java
FlexLayoutModel.row().tabset(...).tabset(...)
FlexLayoutModel.column().tabset(...)
FlexTabset.create().weight(50).tab(...).maxChildren(10)
```

### Tabs

| Class | Purpose |
|-------|---------|
| `FlexTab` | Individual tab definition |
| `FlexTabInfo` | Runtime tab state snapshot (read-only) |

```java
FlexTab.create("Name").component("type").icon("fa-icon").pinned()
FlexTab.create("Name").config(Map.of("key", "value"))
List<FlexTabInfo> tabs = FlexTabInfo.fromJson(jsonString);
```

### Sidebars

| Class | Purpose |
|-------|---------|
| `FlexBorder` | Border panel configuration |
| `TabStyle` | Display style for border tab strips |

```java
FlexBorder.left().size(220).tabStyle(TabStyle.ICON)
FlexBorder.rightBottom().size(180).tabStyle(TabStyle.ICON_LABEL)
FlexBorder.bottom().size(200)
```

### Keyboard

| Class | Purpose |
|-------|---------|
| `FlexKeymap` | Keyboard shortcut configuration |
| `KeyBinding` | Single key combination |
| `FlexAction` | Enum of bindable actions |

```java
FlexKeymap.defaults()
    .bind(FlexAction.TOGGLE_LEFT, KeyBinding.ctrlShift("b"))
    .bind(FlexAction.CLOSE_ACTIVE_TAB, KeyBinding.ctrl("w"))
    .unbind(FlexAction.REDO)

KeyBinding.ctrl("s")
KeyBinding.ctrlShift("p")
KeyBinding.alt("1")
KeyBinding.of(true, true, false, "k")  // Ctrl+Shift+K
KeyBinding.key("F2")                    // No modifiers
```

### Themes

| Class | Purpose |
|-------|---------|
| `FlexTheme` | Enum of visual themes |

Available themes: `DEFAULT`, `LIGHT`, `GRAY`, `NORD`, `SOLARIZED`, `GITHUB`, `MONOKAI`, `CORPORATE`, `DEEP_ORANGE`, `ROUNDED`, `UNDERLINE`.

```java
String cssClass = FlexTheme.MONOKAI.getCssClass(); // "fl-theme-monokai"
```

### Hooks

Tab lifecycle events are handled through the PonySDK addon mechanism. The client sends events (`tabSelect`, `tabClose`, `modelSnapshot`) which can be handled server-side via `AbstractAddon.onData()`.

## Theming Guide

Apply a theme by setting the CSS class on the layout container:

```java
// Server-side: pass theme in initial config
FlexTheme theme = FlexTheme.NORD;
// The addon applies theme.getCssClass() to the .fl-layout element
```

### Custom themes

Create a CSS file with overrides:

```css
.fl-theme-custom {
  --fl-bg: #1a1a2e;
  --fl-tab-bg: #16213e;
  --fl-tab-active-bg: #0f3460;
  --fl-splitter: #e94560;
  --fl-border: #0f3460;
}
```

Then use `FlexTheme.DEFAULT` and add your custom class manually.

## Migration Guide

### Model version changes

The layout model is serialized as JSON via `toJson()`. When upgrading:

1. **Adding new fields** — New fields use defaults; old JSON models load without changes.
2. **Renaming fields** — Update stored JSON models. Use a migration script:
   ```java
   String json = loadStoredModel();
   json = json.replace("\"oldField\"", "\"newField\"");
   ```
3. **Removing fields** — Old fields in stored JSON are ignored by the parser.
4. **Structural changes** — If `FlexLayoutModel` structure changes, re-create models using the new API and persist them.

### Best practices

- Always version your stored layout JSON (add a `"version"` field)
- Test model loading with old JSON after upgrades
- Use `FlexTabInfo.fromJson()` to validate runtime state matches expectations
