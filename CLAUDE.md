# KB - Kymera Developer Keyboard

**Android IME for developers** with context-aware layouts, slash commands, and KYMERA integration.

## Architecture

```
kb/
├── app/                    # Android application
│   └── src/main/
│       ├── java/dev/kymera/keyboard/
│       │   ├── core/       # InputMethodService, key handling
│       │   ├── layouts/    # Layout management, JSON loading
│       │   ├── commands/   # Slash command registry
│       │   ├── communication/  # KYMERA app broadcasts
│       │   ├── ui/         # Themes, settings UI
│       │   └── settings/   # SharedPreferences management
│       └── res/
│           └── raw/        # JSON layouts, commands
├── configs/                # Development configs
└── docs/                   # Documentation
```

## Configuration Hierarchy

1. **Runtime Settings** (SharedPreferences) - User customizations
2. **App Broadcasts** (KYMERA) - Dynamic mode changes
3. **App Resources** (res/raw/*.json) - Built-in layouts/commands
4. **Defaults** - Fallback QWERTY

## Core Features

### Context Detection
Auto-switch layouts based on detected app/field:
- `TERMINAL` - Terminal apps (Termux, etc.)
- `CODE` - Code editors, KYMERA editor fields
- `GENERAL` - Default QWERTY

### Layouts (JSON-defined)
- `qwerty.json` - Standard layout
- `code.json` - Quick symbols, paired brackets
- `terminal.json` - Ctrl combos, pipe, shell symbols
- `symbols.json` - Full symbol grid

### Slash Commands
Registry pattern with categories:
- `/git` - Git operations
- `/docker` - Docker commands
- `/ai` - KYMERA AI requests
- Custom user-defined commands

## Development Rules

1. **Kotlin** - Pure Kotlin, no Java
2. **JSON configs** - Layouts/commands in JSON, not hardcoded
3. **Feature flags** - All features toggleable via settings
4. **Performance** - <16ms key press latency target
5. **Memory** - <50MB runtime target

## Build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```
