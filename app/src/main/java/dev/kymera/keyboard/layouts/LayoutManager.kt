package dev.kymera.keyboard.layouts

import android.content.Context
import dev.kymera.keyboard.core.KeyAction
import dev.kymera.keyboard.core.SemanticLayer
import kotlinx.serialization.json.Json

/**
 * Manages keyboard layouts loaded from JSON configuration files.
 * Supports lazy loading and caching of layouts.
 */
class LayoutManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val layoutCache = mutableMapOf<String, KeyLayout>()
    private var currentLayout: KeyLayout? = null
    private var layoutChangeListener: ((KeyLayout) -> Unit)? = null

    // Layout names
    companion object {
        const val LAYOUT_QWERTY = "qwerty"
        const val LAYOUT_CODE = "code"
        const val LAYOUT_TERMINAL = "terminal"
        const val LAYOUT_SYMBOLS = "symbols"
        const val LAYOUT_PROGRAMMER = "programmer"  // Symbol-first CodeBoard style
        const val LAYOUT_COMPACT = "compact"        // Minimal 4-row layout
        const val LAYOUT_VIM = "vim"                // Vim-optimized
        const val LAYOUT_JARVIS = "jarvis"          // Agentic split layout

        val ALL_LAYOUTS = listOf(
            LAYOUT_CODE, LAYOUT_PROGRAMMER, LAYOUT_TERMINAL,
            LAYOUT_VIM, LAYOUT_COMPACT, LAYOUT_QWERTY, LAYOUT_SYMBOLS,
            LAYOUT_JARVIS
        )
    }

    init {
        // Pre-load default layout (code for developer keyboard)
        loadLayout(LAYOUT_CODE)
    }

    fun setLayoutChangeListener(listener: (KeyLayout) -> Unit) {
        layoutChangeListener = listener
        currentLayout?.let { listener(it) }
    }

    /**
     * Load a layout by name. Layouts are cached after first load.
     */
    fun loadLayout(name: String) {
        // Check cache first
        layoutCache[name]?.let { layout ->
            currentLayout = layout
            layoutChangeListener?.invoke(layout)
            return
        }

        // Load from resources
        val layout = loadLayoutFromResources(name) ?: getDefaultLayout(name)
        layoutCache[name] = layout
        currentLayout = layout
        layoutChangeListener?.invoke(layout)
    }

    private fun loadLayoutFromResources(name: String): KeyLayout? {
        return try {
            val resourceId = context.resources.getIdentifier(
                "layout_$name",
                "raw",
                context.packageName
            )
            if (resourceId == 0) return null

            val jsonString = context.resources.openRawResource(resourceId)
                .bufferedReader()
                .use { it.readText() }

            json.decodeFromString<KeyLayoutConfig>(jsonString).toKeyLayout()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns built-in default layouts when JSON not found.
     * Handles *_shift patterns by returning base layout (shift state managed by service).
     */
    private fun getDefaultLayout(name: String): KeyLayout {
        // Handle shift layout requests - return base layout
        // Shift state is managed by KBKeyboardService, not by separate layouts
        val baseName = if (name.endsWith("_shift")) {
            name.removeSuffix("_shift")
        } else {
            name
        }

        return when (baseName) {
            LAYOUT_CODE -> createCodeLayout()
            LAYOUT_TERMINAL -> createTerminalLayout()
            LAYOUT_SYMBOLS -> createSymbolsLayout()
            LAYOUT_PROGRAMMER -> createProgrammerLayout()
            LAYOUT_COMPACT -> createCompactLayout()
            LAYOUT_VIM -> createVimLayout()
            LAYOUT_QWERTY -> createQwertyLayout()
            LAYOUT_JARVIS -> createJarvisLayout()
            else -> createQwertyLayout()
        }
    }

    fun getAllLayouts(): List<String> = ALL_LAYOUTS

    fun nextLayout() {
        val currentName = currentLayout?.name ?: LAYOUT_CODE
        val currentIndex = ALL_LAYOUTS.indexOf(currentName)
        val nextIndex = (currentIndex + 1) % ALL_LAYOUTS.size
        loadLayout(ALL_LAYOUTS[nextIndex])
    }

    private fun createQwertyLayout(): KeyLayout = KeyLayout(
        name = LAYOUT_QWERTY,
        rows = listOf(
            KeyRow(listOf(
                Key("q"), Key("w"), Key("e"), Key("r"), Key("t"),
                Key("y"), Key("u"), Key("i"), Key("o"), Key("p")
            )),
            KeyRow(listOf(
                Key("a"), Key("s"), Key("d"), Key("f"), Key("g"),
                Key("h"), Key("j"), Key("k"), Key("l")
            )),
            KeyRow(listOf(
                Key("SHIFT", isSpecial = true, action = KeyAction.SwitchLayout("qwerty_shift")),
                Key("z"), Key("x"), Key("c"), Key("v"),
                Key("b"), Key("n"), Key("m"),
                Key("DEL", isSpecial = true, action = KeyAction.Delete)
            )),
            KeyRow(listOf(
                Key("123", isSpecial = true, action = KeyAction.SwitchLayout(LAYOUT_SYMBOLS)),
                Key("/", action = KeyAction.InsertText("/")),
                Key(","),
                Key(" ", label = "SPACE", action = KeyAction.InsertText(" ")),
                Key("."),
                Key("ENTER", isSpecial = true, action = KeyAction.Enter)
            ))
        )
    )

    private fun createCodeLayout(): KeyLayout = KeyLayout(
        name = LAYOUT_CODE,
        rows = listOf(
            // Navigation toolbar - dedicated arrow keys (research: most requested feature)
            KeyRow(listOf(
                Key("ESC", isSpecial = true, action = KeyAction.ControlKey("Esc")),
                Key("TAB", isSpecial = true, action = KeyAction.InsertText("\t")),
                Key("←", isSpecial = true, action = KeyAction.ControlKey("Left")),
                Key("↑", isSpecial = true, action = KeyAction.ControlKey("Up")),
                Key("↓", isSpecial = true, action = KeyAction.ControlKey("Down")),
                Key("→", isSpecial = true, action = KeyAction.ControlKey("Right")),
                Key("HOME", isSpecial = true, action = KeyAction.ControlKey("Home")),
                Key("END", isSpecial = true, action = KeyAction.ControlKey("End")),
                Key("CTRL", isSpecial = true, action = KeyAction.ControlKey("Ctrl"))
            )),
            // Quick symbols row - paired brackets and common operators
            KeyRow(listOf(
                Key("{}", action = KeyAction.InsertPaired("{", "}")),
                Key("()", action = KeyAction.InsertPaired("(", ")")),
                Key("[]", action = KeyAction.InsertPaired("[", "]")),
                Key("<>", action = KeyAction.InsertPaired("<", ">")),
                Key("\"\"", action = KeyAction.InsertPaired("\"", "\"")),
                Key("''", action = KeyAction.InsertPaired("'", "'")),
                Key("=>"),
                Key("->"),
                Key("::"),
                Key("//")
            )),
            // Numbers row with shifted symbols on long-press hint
            KeyRow(listOf(
                Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
                Key("6"), Key("7"), Key("8"), Key("9"), Key("0")
            )),
            // QWERTY row 1
            KeyRow(listOf(
                Key("q"), Key("w"), Key("e"), Key("r"), Key("t"),
                Key("y"), Key("u"), Key("i"), Key("o"), Key("p")
            )),
            // QWERTY row 2
            KeyRow(listOf(
                Key("a"), Key("s"), Key("d"), Key("f"), Key("g"),
                Key("h"), Key("j"), Key("k"), Key("l"), Key(";")
            )),
            // QWERTY row 3
            KeyRow(listOf(
                Key("SHIFT", isSpecial = true, action = KeyAction.SwitchLayout("code_shift")),
                Key("z"), Key("x"), Key("c"), Key("v"),
                Key("b"), Key("n"), Key("m"),
                Key("DEL", isSpecial = true, action = KeyAction.Delete)
            )),
            // Bottom row - essential symbols and commands
            KeyRow(listOf(
                Key("SYM", isSpecial = true, action = KeyAction.SwitchLayout(LAYOUT_SYMBOLS)),
                Key("/cmd", isSpecial = true, action = KeyAction.SlashCommand("/")),
                Key(" ", label = "SPACE", action = KeyAction.InsertText(" ")),
                Key("_"), Key("="), Key("."),
                Key("ENTER", isSpecial = true, action = KeyAction.Enter)
            ))
        )
    )

    private fun createTerminalLayout(): KeyLayout = KeyLayout(
        name = LAYOUT_TERMINAL,
        rows = listOf(
            // Navigation row - arrows always visible for terminal use
            KeyRow(listOf(
                Key("ESC", isSpecial = true, action = KeyAction.ControlKey("Esc")),
                Key("←", isSpecial = true, action = KeyAction.ControlKey("Left")),
                Key("↑", isSpecial = true, action = KeyAction.ControlKey("Up")),
                Key("↓", isSpecial = true, action = KeyAction.ControlKey("Down")),
                Key("→", isSpecial = true, action = KeyAction.ControlKey("Right")),
                Key("HOME", isSpecial = true, action = KeyAction.ControlKey("Home")),
                Key("END", isSpecial = true, action = KeyAction.ControlKey("End")),
                Key("PGUP", isSpecial = true, action = KeyAction.ControlKey("PageUp")),
                Key("PGDN", isSpecial = true, action = KeyAction.ControlKey("PageDown"))
            )),
            // Control combos row - essential terminal shortcuts
            KeyRow(listOf(
                Key("C-c", isSpecial = true, action = KeyAction.ControlKey("C-c")),
                Key("C-d", isSpecial = true, action = KeyAction.ControlKey("C-d")),
                Key("C-z", isSpecial = true, action = KeyAction.ControlKey("C-z")),
                Key("C-l", isSpecial = true, action = KeyAction.ControlKey("C-l")),
                Key("C-r", isSpecial = true, action = KeyAction.ControlKey("C-r")),
                Key("C-a", isSpecial = true, action = KeyAction.ControlKey("C-a")),
                Key("C-e", isSpecial = true, action = KeyAction.ControlKey("C-e")),
                Key("C-w", isSpecial = true, action = KeyAction.ControlKey("C-w")),
                Key("TAB", isSpecial = true, action = KeyAction.InsertText("\t"))
            )),
            // Shell symbols row - pipe, redirect, backgrounding
            KeyRow(listOf(
                Key("|"), Key("&"), Key(">"), Key("<"), Key(">>", action = KeyAction.InsertText(">>")),
                Key("$"), Key("#"), Key("~"), Key("`"), Key("\\")
            )),
            // Numbers row
            KeyRow(listOf(
                Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
                Key("6"), Key("7"), Key("8"), Key("9"), Key("0")
            )),
            // QWERTY row 1
            KeyRow(listOf(
                Key("q"), Key("w"), Key("e"), Key("r"), Key("t"),
                Key("y"), Key("u"), Key("i"), Key("o"), Key("p")
            )),
            // QWERTY row 2
            KeyRow(listOf(
                Key("a"), Key("s"), Key("d"), Key("f"), Key("g"),
                Key("h"), Key("j"), Key("k"), Key("l"), Key("/")
            )),
            // QWERTY row 3
            KeyRow(listOf(
                Key("-"), Key("z"), Key("x"), Key("c"), Key("v"),
                Key("b"), Key("n"), Key("m"), Key("."),
                Key("DEL", isSpecial = true, action = KeyAction.Delete)
            )),
            // Bottom row
            KeyRow(listOf(
                Key("CODE", isSpecial = true, action = KeyAction.SwitchLayout(LAYOUT_CODE)),
                Key("/cmd", isSpecial = true, action = KeyAction.SlashCommand("/")),
                Key(" ", label = "SPACE", action = KeyAction.InsertText(" ")),
                Key("_"), Key("="),
                Key("ENTER", isSpecial = true, action = KeyAction.Enter)
            ))
        )
    )

    private fun createSymbolsLayout(): KeyLayout = KeyLayout(
        name = LAYOUT_SYMBOLS,
        rows = listOf(
            // Settings row with theme/layout/voice/split switchers
            KeyRow(listOf(
                Key("🎨", isSpecial = true, action = KeyAction.NextTheme, label = "Theme"),
                Key("⌨", isSpecial = true, action = KeyAction.NextLayout, label = "Layout"),
                Key("🎤", isSpecial = true, action = KeyAction.VoiceInput, label = "Voice"),
                Key("⇔", isSpecial = true, action = KeyAction.ToggleSplit, label = "Split"),
                Key("!"), Key("@"), Key("#"), Key("$"),
                Key("%"), Key("^")
            )),
            KeyRow(listOf(
                Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
                Key("6"), Key("7"), Key("8"), Key("9"), Key("0")
            )),
            KeyRow(listOf(
                Key("-"), Key("="), Key("["), Key("]"), Key("\\"),
                Key(";"), Key("'"), Key(","), Key("."), Key("/")
            )),
            KeyRow(listOf(
                Key("+"), Key("_"), Key("{"), Key("}"), Key("|"),
                Key(":"), Key("\""), Key("<"), Key(">"), Key("?")
            )),
            KeyRow(listOf(
                Key("("), Key(")"), Key("`"), Key("~"), Key("€"),
                Key("£"), Key("¥"), Key("©"), Key("®"), Key("°")
            )),
            KeyRow(listOf(
                Key("CODE", isSpecial = true, action = KeyAction.SwitchLayout(LAYOUT_CODE)),
                Key(" ", label = "SPACE", action = KeyAction.InsertText(" ")),
                Key("DEL", isSpecial = true, action = KeyAction.Delete),
                Key("ENTER", isSpecial = true, action = KeyAction.Enter)
            ))
        )
    )

    /**
     * Programmer layout - CodeBoard inspired symbol-first design.
     * Prioritizes symbols and operators for coding efficiency.
     */
    private fun createProgrammerLayout(): KeyLayout = KeyLayout(
        name = LAYOUT_PROGRAMMER,
        rows = listOf(
            // Row 1: Most common programming operators
            KeyRow(listOf(
                Key("="), Key("+"), Key("-"), Key("*"), Key("/"),
                Key("<"), Key(">"), Key("("), Key(")"), Key("!")
            )),
            // Row 2: Brackets, quotes, common symbols
            KeyRow(listOf(
                Key("{"), Key("}"), Key("["), Key("]"), Key(";"),
                Key(":"), Key("'"), Key("\""), Key("&"), Key("|")
            )),
            // Row 3: Numbers
            KeyRow(listOf(
                Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
                Key("6"), Key("7"), Key("8"), Key("9"), Key("0")
            )),
            // Row 4: QWERTY top
            KeyRow(listOf(
                Key("q"), Key("w"), Key("e"), Key("r"), Key("t"),
                Key("y"), Key("u"), Key("i"), Key("o"), Key("p")
            )),
            // Row 5: QWERTY middle
            KeyRow(listOf(
                Key("a"), Key("s"), Key("d"), Key("f"), Key("g"),
                Key("h"), Key("j"), Key("k"), Key("l"), Key("_")
            )),
            // Row 6: QWERTY bottom
            KeyRow(listOf(
                Key("⇧", isSpecial = true, action = KeyAction.SwitchLayout("programmer_shift")),
                Key("z"), Key("x"), Key("c"), Key("v"),
                Key("b"), Key("n"), Key("m"),
                Key("⌫", isSpecial = true, action = KeyAction.Delete)
            )),
            // Row 7: Bottom navigation
            KeyRow(listOf(
                Key("ABC", isSpecial = true, action = KeyAction.SwitchLayout(LAYOUT_QWERTY)),
                Key("←", isSpecial = true, action = KeyAction.ControlKey("Left")),
                Key("→", isSpecial = true, action = KeyAction.ControlKey("Right")),
                Key(" ", label = "SPACE", action = KeyAction.InsertText(" ")),
                Key(".", action = KeyAction.InsertText(".")),
                Key("↵", isSpecial = true, action = KeyAction.Enter)
            ))
        )
    )

    /**
     * Compact layout - minimal 4-row design for quick typing.
     */
    private fun createCompactLayout(): KeyLayout = KeyLayout(
        name = LAYOUT_COMPACT,
        rows = listOf(
            // Numbers/Symbols toggle row
            KeyRow(listOf(
                Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
                Key("6"), Key("7"), Key("8"), Key("9"), Key("0")
            )),
            // QWERTY top
            KeyRow(listOf(
                Key("q"), Key("w"), Key("e"), Key("r"), Key("t"),
                Key("y"), Key("u"), Key("i"), Key("o"), Key("p")
            )),
            // QWERTY middle + semicolon
            KeyRow(listOf(
                Key("a"), Key("s"), Key("d"), Key("f"), Key("g"),
                Key("h"), Key("j"), Key("k"), Key("l"), Key(";")
            )),
            // Bottom with common symbols
            KeyRow(listOf(
                Key("⇧", isSpecial = true, action = KeyAction.SwitchLayout("compact_shift")),
                Key("z"), Key("x"), Key("c"), Key("v"),
                Key("b"), Key("n"), Key("m"), Key("."),
                Key("⌫", isSpecial = true, action = KeyAction.Delete)
            )),
            // Space bar row
            KeyRow(listOf(
                Key("SYM", isSpecial = true, action = KeyAction.SwitchLayout(LAYOUT_SYMBOLS)),
                Key(","),
                Key(" ", label = "SPACE", action = KeyAction.InsertText(" ")),
                Key("↵", isSpecial = true, action = KeyAction.Enter)
            ))
        )
    )

    /**
     * Vim layout - optimized for Vim users with Esc and mode indicators.
     */
    private fun createVimLayout(): KeyLayout = KeyLayout(
        name = LAYOUT_VIM,
        rows = listOf(
            // Vim command row
            KeyRow(listOf(
                Key("ESC", isSpecial = true, action = KeyAction.ControlKey("Esc")),
                Key(":"), Key("w"), Key("q"), Key("!"),
                Key("/"), Key("?"), Key("n"), Key("N"),
                Key("u", label = "undo")
            )),
            // Movement keys prominent
            KeyRow(listOf(
                Key("h", label = "←"), Key("j", label = "↓"), Key("k", label = "↑"), Key("l", label = "→"),
                Key("0"), Key("$"), Key("^"), Key("G"), Key("gg", action = KeyAction.InsertText("gg")), Key("%")
            )),
            // Numbers
            KeyRow(listOf(
                Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
                Key("6"), Key("7"), Key("8"), Key("9"), Key(".")
            )),
            // QWERTY
            KeyRow(listOf(
                Key("q"), Key("w"), Key("e"), Key("r"), Key("t"),
                Key("y"), Key("u"), Key("i"), Key("o"), Key("p")
            )),
            KeyRow(listOf(
                Key("a"), Key("s"), Key("d"), Key("f"), Key("g"),
                Key("H"), Key("J"), Key("K"), Key("L"), Key(";")
            )),
            KeyRow(listOf(
                Key("SHIFT", isSpecial = true, action = KeyAction.SwitchLayout("vim_shift")),
                Key("z"), Key("x"), Key("c"), Key("v"),
                Key("b"), Key("n"), Key("m"),
                Key("DEL", isSpecial = true, action = KeyAction.Delete)
            )),
            KeyRow(listOf(
                Key("CODE", isSpecial = true, action = KeyAction.SwitchLayout(LAYOUT_CODE)),
                Key("CTRL", isSpecial = true, action = KeyAction.ControlKey("Ctrl")),
                Key(" ", label = "SPACE", action = KeyAction.InsertText(" ")),
                Key("ENTER", isSpecial = true, action = KeyAction.Enter)
            ))
        )
    )

    /**
     * JARVIS layout - Agentic split keyboard with Moonlander-style column stagger.
     * Designed for landscape split mode with dedicated Goblin Forge controls.
     *
     * Structure:
     * - Row 0: Goblin row (spawn goblins, dashboard, attach/kill)
     * - Row 1: Numbers row
     * - Row 2: QWERTY Q-P
     * - Row 3: QWERTY A-;
     * - Row 4: QWERTY Z-/ with SHIFT
     * - Row 5: Bottom row with forge commands
     *
     * Integration: Uses TermuxBridge to run gforge commands in Termux.
     * Falls back to local agent system if Termux not installed.
     */
    private fun createJarvisLayout(): KeyLayout = KeyLayout(
        name = LAYOUT_JARVIS,
        rows = listOf(
            // Goblin row - spawn goblins via Termux gforge
            KeyRow(listOf(
                // Spawn goblins with different agents
                Key("CODE", isSpecial = true, action = KeyAction.GforgeSpawn("coder", "claude"), label = "Coder"),
                Key("RSRCH", isSpecial = true, action = KeyAction.GforgeSpawn("researcher", "gemini"), label = "Research"),
                Key("QUANT", isSpecial = true, action = KeyAction.GforgeSpawn("quant", "claude", "~/projects/screener"), label = "Quant"),
                Key("TOP", isSpecial = true, action = KeyAction.GforgeTop, label = "Dashboard"),
                // Context/session management
                Key("ATT", isSpecial = true, action = KeyAction.GforgeAttach(), label = "Attach"),
                Key("KILL", isSpecial = true, action = KeyAction.GforgeKill(), label = "Kill"),
                // Navigation keys
                Key("ESC", isSpecial = true, action = KeyAction.ControlKey("Esc")),
                Key("TAB", isSpecial = true, action = KeyAction.InsertText("\t")),
                Key("CTRL", isSpecial = true, action = KeyAction.ControlKey("Ctrl"))
            )),
            // Numbers row
            KeyRow(listOf(
                Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
                Key("6"), Key("7"), Key("8"), Key("9"), Key("0")
            )),
            // QWERTY row 1
            KeyRow(listOf(
                Key("q"), Key("w"), Key("e"), Key("r"), Key("t"),
                Key("y"), Key("u"), Key("i"), Key("o"), Key("p")
            )),
            // QWERTY row 2
            KeyRow(listOf(
                Key("a"), Key("s"), Key("d"), Key("f"), Key("g"),
                Key("h"), Key("j"), Key("k"), Key("l"), Key(";")
            )),
            // QWERTY row 3 with SHIFT
            KeyRow(listOf(
                Key("SHIFT", isSpecial = true, action = KeyAction.SwitchLayout("jarvis_shift")),
                Key("z"), Key("x"), Key("c"), Key("v"),
                Key("b"), Key("n"), Key("m"),
                Key("DEL", isSpecial = true, action = KeyAction.Delete)
            )),
            // Bottom row - gforge commands + typing essentials
            KeyRow(listOf(
                Key("SYM", isSpecial = true, action = KeyAction.SwitchLayout(LAYOUT_SYMBOLS)),
                Key("LOGS", isSpecial = true, action = KeyAction.GforgeLogs(), label = "Logs"),
                Key(" ", label = "SPACE", action = KeyAction.InsertText(" ")),
                Key("BUILD", isSpecial = true, action = KeyAction.GforgeRun("build"), label = "Build"),
                Key("TEST", isSpecial = true, action = KeyAction.GforgeRun("test"), label = "Test"),
                Key("ENTER", isSpecial = true, action = KeyAction.Enter)
            ))
        )
    )

    fun getCurrentLayout(): KeyLayout? = currentLayout
}
