package dev.kymera.keyboard.core

import dev.kymera.keyboard.agents.CommandContext

/**
 * Semantic layers for context-aware key morphing.
 * Each layer changes the meaning/behavior of keys.
 */
enum class SemanticLayer {
    TEXT,      // Default typing
    CODE,      // Language-specific symbols
    PROMPT,    // Semantic prompt blocks
    SHELL,     // Unix commands
    MATH,      // LaTeX/notation
    DIAGRAM    // Mermaid/ASCII
}

/**
 * Sealed class representing all possible keyboard actions.
 * Following the ky/ pattern for typed action handling.
 */
sealed class KeyAction {
    /** Insert plain text */
    data class InsertText(val text: String) : KeyAction()

    /** Insert paired characters (brackets, quotes) with cursor between */
    data class InsertPaired(val open: String, val close: String) : KeyAction()

    /** Delete character before cursor */
    data object Delete : KeyAction()

    /** Insert newline */
    data object Enter : KeyAction()

    /** Execute slash command */
    data class SlashCommand(val command: String) : KeyAction()

    /** Switch to different layout */
    data class SwitchLayout(val layout: String) : KeyAction()

    /** Send text to KYMERA app */
    data class SendToKymera(val text: String, val type: String = "command") : KeyAction()

    /** Terminal control key (Ctrl+C, Esc, etc.) */
    data class ControlKey(val key: String) : KeyAction()

    /** Cycle to next theme */
    data object NextTheme : KeyAction()

    /** Cycle to next layout */
    data object NextLayout : KeyAction()

    /** Show layout picker */
    data object ShowLayoutPicker : KeyAction()

    /** Show theme picker */
    data object ShowThemePicker : KeyAction()

    /** Start voice input (STT) */
    data object VoiceInput : KeyAction()

    /** Toggle split keyboard mode */
    data object ToggleSplit : KeyAction()

    // === AGENTIC ACTIONS ===

    /** Spawn specific agent with optional context */
    data class SpawnAgent(
        val agentId: String,  // "coding", "research", "quant", "orchestrator"
        val withSelection: Boolean = true
    ) : KeyAction()

    /** Execute agent command with context */
    data class AgentCommand(
        val command: String,
        val params: Map<String, String> = emptyMap()
    ) : KeyAction()

    /** Inject/expand context window */
    data object ExpandContext : KeyAction()

    /** Prune/reset context */
    data object ResetContext : KeyAction()

    /** Recall last agent output */
    data object RecallMemory : KeyAction()

    /** Generate documentation */
    data object GenerateDoc : KeyAction()

    /** Execute current code/cell */
    data object ExecuteRun : KeyAction()

    /** Trigger simulation/backtest */
    data object ExecuteSimulate : KeyAction()

    /** Refine/optimize selection */
    data object RefineSelection : KeyAction()

    /** Advance agent pipeline stage */
    data object AgentStep : KeyAction()

    /** Hold-to-query LLM (push-to-talk style) */
    data class HoldToQuery(val isStart: Boolean) : KeyAction()

    /** Switch semantic layer */
    data class SwitchLayer(val layer: SemanticLayer) : KeyAction()

    /** Cycle to next agent */
    data object NextAgent : KeyAction()

    // === GOBLIN FORGE ACTIONS (Termux-based) ===

    /**
     * Spawn a gforge goblin (agent instance).
     * Runs: gforge spawn <name> --agent <agent> [--project <path>]
     */
    data class GforgeSpawn(
        val goblinName: String,
        val agent: String = "claude",
        val project: String = "."
    ) : KeyAction()

    /**
     * Attach to a goblin's tmux session.
     * Opens Termux and runs: gforge attach <name>
     */
    data class GforgeAttach(val goblinName: String? = null) : KeyAction()

    /**
     * Kill a goblin and cleanup resources.
     * Runs: gforge kill <name>
     */
    data class GforgeKill(val goblinName: String? = null) : KeyAction()

    /**
     * Show goblin output logs.
     * Opens Termux and runs: gforge logs <name> -n <lines>
     */
    data class GforgeLogs(val goblinName: String? = null, val lines: Int = 50) : KeyAction()

    /**
     * Launch gforge TUI dashboard.
     * Opens Termux and runs: gforge top
     */
    data object GforgeTop : KeyAction()

    /**
     * Send a task to a goblin.
     * Runs: gforge task "<task>" --goblin <name>
     */
    data class GforgeTask(val task: String, val goblinName: String? = null) : KeyAction()

    /**
     * Run a template command (build, test, dev).
     * Runs: gforge run <template>
     */
    data class GforgeRun(val template: String) : KeyAction()

    /**
     * List all goblins.
     * Opens Termux and runs: gforge list
     */
    data object GforgeList : KeyAction()

    /**
     * Show gforge system status.
     * Opens Termux and runs: gforge status
     */
    data object GforgeStatus : KeyAction()

    /**
     * Show diff for a goblin's changes.
     * Opens Termux and runs: gforge diff <name>
     */
    data class GforgeDiff(val goblinName: String? = null, val staged: Boolean = false) : KeyAction()
}

/**
 * Result from slash command execution.
 */
sealed class CommandResult {
    data class InsertText(val text: String) : CommandResult()
    data class SendToKymera(
        val text: String,
        val type: String,
        val context: CommandContext? = null
    ) : CommandResult()
    data class ShowPicker(val commands: List<String>) : CommandResult()
    data object NotFound : CommandResult()
}
