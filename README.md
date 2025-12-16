# Goblin KB

> "Command your goblins from anywhere."

**Goblin KB** is an agentic Android keyboard designed for developers, featuring JARVIS-style split layout with direct [Goblin Forge](https://github.com/astoreyai/goblin-forge) integration via Termux.

```
     ╔═══════════════════════════════════════════════════════════════╗
     ║                                                               ║
     ║   ░██████╗░░█████╗░██████╗░██╗░░░░░██╗███╗░░██╗               ║
     ║   ██╔════╝░██╔══██╗██╔══██╗██║░░░░░██║████╗░██║               ║
     ║   ██║░░██╗░██║░░██║██████╦╝██║░░░░░██║██╔██╗██║               ║
     ║   ██║░░╚██╗██║░░██║██╔══██╗██║░░░░░██║██║╚████║               ║
     ║   ╚██████╔╝╚█████╔╝██████╦╝███████╗██║██║░╚███║               ║
     ║   ░╚═════╝░░╚════╝░╚═════╝░╚══════╝╚═╝╚═╝░░╚══╝               ║
     ║                                                               ║
     ║   ██╗░░██╗██████╗░                                            ║
     ║   ██║░██╔╝██╔══██╗                                            ║
     ║   █████═╝░██████╦╝                                            ║
     ║   ██╔═██╗░██╔══██╗                                            ║
     ║   ██║░╚██╗██████╦╝                                            ║
     ║   ╚═╝░░╚═╝╚═════╝░                                            ║
     ║                                                               ║
     ║          Agentic Developer Keyboard for Android               ║
     ║                                                               ║
     ╚═══════════════════════════════════════════════════════════════╝
```

## Features

- **Goblin Forge Integration**: Spawn and control gforge goblins directly from keyboard buttons
- **JARVIS Layout**: Iron Man HUD-inspired split keyboard with glow effects
- **8 Keyboard Layouts**: JARVIS, Code, Terminal, Vim, Programmer, Compact, QWERTY, Symbols
- **Moonlander-Style Stagger**: Column-staggered keys for landscape thumb typing
- **10 Themes**: Including JARVIS with dynamic glow effects
- **Context Detection**: Auto-switch layouts based on app (terminal/code/general)
- **Slash Commands**: 38+ developer commands with custom command support
- **Termux Bridge**: Execute gforge commands via Android intents

## Quick Start

### 1. Install APK

Download from [Releases](https://github.com/astoreyai/goblin-kb/releases) or build:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Enable Keyboard

1. Open Settings → Language & Input → On-screen keyboard
2. Enable "KB JARVIS Keyboard"
3. Set as default input method

### 3. Setup Termux (for Goblin Forge)

```bash
# Install Termux from F-Droid
# Then in Termux:
pkg install tmux git golang

# Build gforge for ARM64
git clone https://github.com/astoreyai/goblin-forge.git
cd goblin-forge
GOOS=linux GOARCH=arm64 go build -o ~/.local/bin/gforge ./cmd/gforge
chmod +x ~/.local/bin/gforge

# Initialize
gforge config init
```

### 4. Grant Permissions

The keyboard will request `com.termux.permission.RUN_COMMAND` to communicate with Termux.

## JARVIS Layout

The JARVIS layout is designed for agentic control of Goblin Forge:

```
┌──────────────────────────────────────────────────────────────────────────┐
│  GOBLIN CONTROL ROW                                                       │
│  CODE    RSRCH   QUANT   TOP     ATT    KILL   ESC   TAB   CTRL          │
│  (spawn) (spawn) (spawn) (dash)  (att)  (kill)                           │
├──────────────────────────────────────────────────────────────────────────┤
│  1  2  3  4  5  6  7  8  9  0                                            │
│  Q  W  E  R  T  Y  U  I  O  P                                            │
│  A  S  D  F  G  H  J  K  L  ;                                            │
│  SHIFT  Z  X  C  V  B  N  M  DEL                                         │
├──────────────────────────────────────────────────────────────────────────┤
│  FORGE COMMANDS                                                           │
│  SYM    LOGS    SPACE    BUILD   TEST   ENTER                            │
└──────────────────────────────────────────────────────────────────────────┘
```

### Button Actions

| Button | gforge Command | Description |
|--------|---------------|-------------|
| **CODE** | `gforge spawn coder --agent claude` | Spawn Claude coding agent |
| **RSRCH** | `gforge spawn researcher --agent gemini` | Spawn Gemini research agent |
| **QUANT** | `gforge spawn quant --agent claude -p ~/projects/screener` | Spawn quant agent |
| **TOP** | `gforge top` | Launch TUI dashboard |
| **ATT** | `gforge attach <active>` | Attach to active goblin |
| **KILL** | `gforge kill <active>` | Kill active goblin |
| **LOGS** | `gforge logs <active> -n 50` | Show goblin output |
| **BUILD** | `gforge run build` | Run build template |
| **TEST** | `gforge run test` | Run test template |

### Glow Effects

Keys glow based on their function:
- **Claude spawn**: Blue
- **Gemini spawn**: Purple
- **Dashboard**: Orange
- **Kill**: Red
- **Build/Test**: Green

## All Layouts

| Layout | Rows | Best For |
|--------|------|----------|
| **JARVIS** | 6 | Goblin Forge control, landscape split |
| **Code** | 7 | IDE/editor coding with nav keys |
| **Terminal** | 8 | Shell commands (Ctrl+C, pipes, etc.) |
| **Vim** | 7 | Vim/Neovim with ESC and motions |
| **Programmer** | 7 | Symbol-first CodeBoard style |
| **Compact** | 5 | Minimal typing |
| **QWERTY** | 4 | Standard input |
| **Symbols** | 6 | Full symbol grid + settings |

## Themes

- JARVIS (Iron Man HUD with glow)
- Dark
- Light
- Monokai
- Dracula
- Nord
- Solarized Dark
- OLED Black
- High Contrast
- BeeRaider

## Slash Commands

Access via `/cmd` key in Code/Terminal layouts:

```
/git status       → git status
/git add          → git add .
/git commit       → Prompts for message
/docker ps        → docker ps
/ai summarize     → Send to AI
...and 30+ more
```

Custom commands can be added in Settings.

## Architecture

```
app/src/main/java/dev/kymera/keyboard/
├── core/                    # InputMethodService, KeyAction, View
│   ├── KBKeyboardService.kt # Main IME service
│   ├── KBKeyboardView.kt    # Canvas-based keyboard rendering
│   ├── KeyAction.kt         # All possible key actions (sealed class)
│   └── ModifierState.kt     # CTRL/SHIFT sticky modifiers
├── agents/                  # Local agent system (fallback)
├── commands/                # Slash command registry
├── communication/           # External app bridges
│   └── TermuxBridge.kt      # Termux RUN_COMMAND intents
├── layouts/                 # Layout definitions
│   ├── LayoutManager.kt     # All 8 layouts
│   └── ColumnStaggerLayout.kt # Moonlander geometry
├── rendering/               # Visual effects
│   └── GlowRenderer.kt      # JARVIS glow effects
├── settings/                # Preferences
└── ui/                      # Themes, Settings UI
```

## Configuration

Settings available in the keyboard app:
- Theme selection
- Default layout
- Split mode (Auto/Always/Never)
- Column stagger (Moonlander-style)
- Row height (44-64dp)
- Haptic feedback

## Development

### Requirements

- Android Studio Hedgehog or later
- Android SDK 33+
- Kotlin 1.9+

### Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

### Project Structure

```
goblin-kb/
├── app/
│   └── src/main/
│       ├── java/dev/kymera/keyboard/  # Kotlin sources
│       └── res/                        # Layouts, themes, commands
├── docs/
│   └── ARCHITECTURE.md
├── CLAUDE.md                           # Architecture for AI assistants
└── README.md
```

## Termux Integration

The keyboard communicates with Termux via the `RUN_COMMAND` intent:

```kotlin
Intent("com.termux.RUN_COMMAND").apply {
    putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.local/bin/gforge")
    putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("spawn", "coder", "--agent", "claude"))
    putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
}
```

Requires the `com.termux.permission.RUN_COMMAND` permission.

## Fallback Behavior

When Termux is not installed, buttons fall back to local agent broadcasts:
- Spawn buttons → `SpawnAgent` broadcast to `dev.goblin.agent`
- Kill → Context reset
- Build → Execute broadcast

## Related Projects

- [Goblin Forge](https://github.com/astoreyai/goblin-forge) - Multi-agent CLI orchestrator

## License

Apache-2.0
