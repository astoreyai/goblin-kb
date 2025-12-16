# Goblin KB - Agentic Developer Keyboard

> "Command your goblins from anywhere."

**Part of the Goblin Empire** - Android keyboard with direct [Goblin Forge](https://github.com/astoreyai/goblin-forge) integration via Termux.

---

## Quick Reference

```bash
# From keyboard JARVIS layout:
CODE   → gforge spawn coder --agent claude
RSRCH  → gforge spawn researcher --agent gemini
QUANT  → gforge spawn quant --agent claude -p ~/projects/screener
TOP    → gforge top (TUI dashboard)
ATT    → gforge attach <active>
KILL   → gforge kill <active>
LOGS   → gforge logs <active>
BUILD  → gforge run build
TEST   → gforge run test
```

---

## Architecture

```
goblin-kb/
├── app/src/main/java/dev/kymera/keyboard/
│   ├── core/              # InputMethodService, View, KeyAction
│   │   ├── KBKeyboardService.kt   # Main IME, action dispatch
│   │   ├── KBKeyboardView.kt      # Canvas rendering, glow effects
│   │   ├── KeyAction.kt           # 50+ action types (sealed class)
│   │   └── ModifierState.kt       # CTRL/SHIFT sticky modifiers
│   ├── layouts/           # 8 keyboard layouts
│   │   ├── LayoutManager.kt       # All layouts defined here
│   │   └── ColumnStaggerLayout.kt # Moonlander geometry
│   ├── communication/     # External app bridges
│   │   └── TermuxBridge.kt        # Termux RUN_COMMAND intents
│   ├── rendering/
│   │   └── GlowRenderer.kt        # JARVIS glow effects
│   ├── commands/          # Slash command system
│   └── ui/                # Themes, settings
└── docs/
```

---

## Goblin Forge Integration

### TermuxBridge

Sends `RUN_COMMAND` intents to Termux:

```kotlin
object TermuxBridge {
    fun spawnGoblin(ctx, name, agent, project)  // gforge spawn
    fun attachGoblin(ctx, name)                 // gforge attach
    fun killGoblin(ctx, name)                   // gforge kill
    fun showLogs(ctx, name, lines)              // gforge logs
    fun launchTop(ctx)                          // gforge top
    fun runTemplate(ctx, template)              // gforge run
}
```

### KeyAction Types

```kotlin
// Goblin Forge actions
data class GforgeSpawn(val goblinName: String, val agent: String, val project: String)
data class GforgeAttach(val goblinName: String?)
data class GforgeKill(val goblinName: String?)
data class GforgeLogs(val goblinName: String?, val lines: Int)
data object GforgeTop
data class GforgeRun(val template: String)
data object GforgeList
data object GforgeStatus
data class GforgeDiff(val goblinName: String?, val staged: Boolean)
```

### Permissions

```xml
<uses-permission android:name="com.termux.permission.RUN_COMMAND" />
<queries>
    <package android:name="com.termux" />
</queries>
```

---

## Layouts

| Layout | Rows | Purpose |
|--------|------|---------|
| **JARVIS** | 6 | Goblin Forge control + split keyboard |
| **Code** | 7 | Navigation, paired brackets |
| **Terminal** | 8 | Ctrl combos (C-c, C-d, etc.) |
| **Vim** | 7 | ESC, motion keys |
| **Programmer** | 7 | Symbol-first |
| **Compact** | 5 | Minimal |
| **QWERTY** | 4 | Standard |
| **Symbols** | 6 | Full grid |

### JARVIS Layout

```
┌────────────────────────────────────────────────────────────────┐
│ CODE  RSRCH  QUANT  TOP  ATT  KILL  ESC  TAB  CTRL             │
│ 1  2  3  4  5  6  7  8  9  0                                   │
│ Q  W  E  R  T  Y  U  I  O  P                                   │
│ A  S  D  F  G  H  J  K  L  ;                                   │
│ SHIFT  Z  X  C  V  B  N  M  DEL                                │
│ SYM  LOGS  SPACE  BUILD  TEST  ENTER                           │
└────────────────────────────────────────────────────────────────┘
```

---

## Themes

10 themes including **JARVIS** with glow effects:

| Theme | Background | Accent |
|-------|------------|--------|
| JARVIS | #0A0F14 | #00D4FF |
| Dark | #1A1A1A | #2196F3 |
| Monokai | #272822 | #A6E22E |
| Dracula | #282A36 | #BD93F9 |
| Nord | #2E3440 | #88C0D0 |

### Glow Colors (JARVIS)

- Claude spawn → Blue
- Gemini spawn → Purple
- Dashboard → Orange
- Kill → Red
- Build/Test → Green

---

## Development

### Build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Test

```bash
./gradlew test
```

### Adding New Layout

1. Add to `LayoutManager.ALL_LAYOUTS`
2. Create `createXxxLayout()` function
3. Add to `getDefaultLayout()` switch

### Adding New KeyAction

1. Add to `KeyAction.kt` sealed class
2. Handle in `KBKeyboardService.onKeyPressed()`
3. Update `KBKeyboardView.drawJarvisGlow()` if needed

---

## Rules

1. **Kotlin only** - No Java
2. **Goblin Empire** - Primary integration is Goblin Forge via Termux
3. **Performance** - <16ms key press latency
4. **Feature flags** - All features toggleable in settings

---

## Related

- [Goblin Forge](https://github.com/astoreyai/goblin-forge) - Multi-agent CLI orchestrator
- [Goblin Empire](https://github.com/astoreyai) - Full toolchain
