# Goblin KB Architecture

## Overview

Goblin KB is a developer-focused Android keyboard with context-aware layouts, slash commands, and **Goblin Forge** integration via Termux. The architecture prioritizes agentic control of multi-agent CLI orchestration from mobile.

## Core Principles

1. **Configuration over Code** - Layouts and commands defined in JSON
2. **Lazy Loading** - Components initialized on-demand
3. **Context Detection** - Auto-switch layouts based on detected app
4. **Feature Flags** - All features toggleable via settings
5. **Termux-First** - Primary integration via Termux RUN_COMMAND

## Module Structure

```
dev.kymera.keyboard/
├── core/           # InputMethodService + key handling
├── layouts/        # Layout management + JSON loading
├── commands/       # Slash command registry
├── communication/  # TermuxBridge + local agent fallback
├── agents/         # Agent registry + command bus
├── rendering/      # JARVIS glow effects
├── ui/             # Settings activity + themes
└── settings/       # SharedPreferences management
```

## Configuration Hierarchy

```
Priority (highest to lowest):
1. Runtime Settings (SharedPreferences)
2. App Broadcasts (external control)
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
│  │  - JARVIS glow effects                              │ │
│  └─────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
          │                                      │
          ▼                                      ▼
   ┌──────────────┐                    ┌──────────────────┐
   │ InputConn.   │                    │ TermuxBridge     │
   │ (text input) │                    │ (gforge cmds)    │
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
| `send_to_local_agent` | Local agent request | `"action_value": "explain"` |

## Goblin Forge Integration

### Primary: Termux (Keyboard → gforge)

```kotlin
TermuxBridge.spawnGoblin(
    context = context,
    name = "coder",
    agent = "claude",
    project = "~/projects/myapp"
)
// Executes: gforge spawn coder --agent claude --project ~/projects/myapp
```

### Fallback: Local Agent (when Termux unavailable)

```kotlin
LocalAgentBridge.sendAgentRequest(
    context = context,
    agentId = "coding",
    command = "explain",
    commandContext = commandContext
)
// Broadcasts to: dev.goblin.agent
```

### Inbound (External → Keyboard)

```kotlin
// Broadcast actions:
dev.kymera.keyboard.SET_MODE        // Switch layout
dev.kymera.keyboard.INSERT_TEXT     // Insert at cursor
dev.kymera.keyboard.UPDATE_COMMANDS // Reload commands
```

## JARVIS Layout

The JARVIS layout provides direct Goblin Forge control:

| Button | gforge Command |
|--------|---------------|
| CODE | `gforge spawn coder --agent claude` |
| RSRCH | `gforge spawn researcher --agent gemini` |
| QUANT | `gforge spawn quant --agent claude` |
| TOP | `gforge top` |
| ATT | `gforge attach <active>` |
| KILL | `gforge kill <active>` |
| LOGS | `gforge logs <active>` |
| BUILD | `gforge run build` |
| TEST | `gforge run test` |

## Performance Targets

| Metric | Target |
|--------|--------|
| Key press latency | <16ms |
| Swipe recognition | <100ms |
| Memory usage | <50MB |
| APK size | <10MB |
