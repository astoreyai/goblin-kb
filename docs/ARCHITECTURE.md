# KB Keyboard Architecture

## Overview

KB is a developer-focused Android keyboard with context-aware layouts, slash commands, and KYMERA integration. The architecture follows patterns from the ky/ codebase.

## Core Principles

1. **Configuration over Code** - Layouts and commands defined in JSON
2. **Lazy Loading** - Components initialized on-demand
3. **Context Detection** - Auto-switch layouts based on detected app
4. **Feature Flags** - All features toggleable via settings

## Module Structure

```
dev.kymera.keyboard/
├── core/           # InputMethodService + key handling
├── layouts/        # Layout management + JSON loading
├── commands/       # Slash command registry
├── communication/  # KYMERA app broadcasts
├── ui/             # Settings activity + themes
└── settings/       # SharedPreferences management
```

## Configuration Hierarchy

```
Priority (highest to lowest):
1. Runtime Settings (SharedPreferences)
2. App Broadcasts (KYMERA app)
3. App Resources (res/raw/*.json)
4. Built-in Defaults (hardcoded)
```

## Data Flow

```
┌──────────────────────────────────────────────────────────┐
│                    KBKeyboardService                      │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────┐  │
│  │ Context    │  │ Layout     │  │ SlashCommand       │  │
│  │ Detector   │──│ Manager    │──│ Registry           │  │
│  └────────────┘  └────────────┘  └────────────────────┘  │
│         │               │                   │             │
│         ▼               ▼                   ▼             │
│  ┌─────────────────────────────────────────────────────┐ │
│  │                  KBKeyboardView                      │ │
│  │  - Canvas rendering                                  │ │
│  │  - Touch handling                                    │ │
│  │  - Swipe detection                                   │ │
│  └─────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
          │                                      │
          ▼                                      ▼
   ┌──────────────┐                    ┌──────────────────┐
   │ InputConn.   │                    │ KymeraBridge     │
   │ (text input) │                    │ (AI requests)    │
   └──────────────┘                    └──────────────────┘
```

## JSON Schemas

### Layout Schema (res/raw/layout_*.json)

```json
{
  "name": "code",
  "description": "Developer layout",
  "auto_detect_packages": ["com.app.name"],
  "rows": [
    {
      "name": "row_name",
      "keys": [
        {
          "label": "{}",
          "value": "{}",
          "action": "insert_paired",
          "action_value": "{,}",
          "is_special": false
        }
      ]
    }
  ]
}
```

### Commands Schema (res/raw/commands.json)

```json
{
  "version": 1,
  "categories": {
    "git": {
      "icon": "git",
      "description": "Git commands",
      "commands": [
        {
          "command": "/git status",
          "description": "Show status",
          "action": "insert_text",
          "value": "git status"
        }
      ]
    }
  }
}
```

## Action Types

| Action | Description | Example |
|--------|-------------|---------|
| `insert` | Insert text | `"value": "git status"` |
| `insert_paired` | Insert with cursor between | `"action_value": "{,}"` |
| `delete` | Backspace | - |
| `enter` | Newline | - |
| `switch_layout` | Change layout | `"action_value": "symbols"` |
| `slash_command` | Execute command | `"action_value": "/"` |
| `control_key` | Terminal control | `"action_value": "C-c"` |
| `send_to_kymera` | AI request | `"action_value": "explain"` |

## KYMERA Integration

### Outbound (Keyboard → KYMERA)

```kotlin
KymeraBridge.sendAiRequest(
    context = context,
    text = selectedCode,
    type = "explain",
    inputContext = "CODE"
)
```

### Inbound (KYMERA → Keyboard)

```kotlin
// Broadcast actions:
dev.kymera.keyboard.SET_MODE        // Switch layout
dev.kymera.keyboard.INSERT_TEXT     // Insert at cursor
dev.kymera.keyboard.UPDATE_COMMANDS // Reload commands
```

## Performance Targets

| Metric | Target |
|--------|--------|
| Key press latency | <16ms |
| Swipe recognition | <100ms |
| Memory usage | <50MB |
| APK size | <10MB |
