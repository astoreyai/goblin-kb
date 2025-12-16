package dev.kymera.keyboard.ui

/**
 * JARVIS/Iron Man inspired theme constants.
 * HUD-style aesthetic with cyan glows, dark panels, and tech-forward design.
 */
object JarvisTheme {
    // Primary JARVIS colors
    const val JARVIS_CYAN = "#00D4FF"
    const val JARVIS_ORANGE = "#FF6B00"
    const val JARVIS_BLUE = "#0066FF"
    const val JARVIS_WHITE = "#E8F4F8"
    const val JARVIS_RED = "#FF3333"

    // Background layers
    const val HUD_BACKGROUND = "#0A0F14"
    const val HUD_PANEL = "#101820"
    const val HUD_PANEL_PRESSED = "#1A2530"
    const val HUD_GLOW = "#00D4FF20"  // 12% opacity cyan

    // State colors (with alpha for glow effects)
    const val ACTIVE_GLOW = "#00FF8850"
    const val WARNING_GLOW = "#FF6B0050"
    const val ERROR_GLOW = "#FF000050"

    // Agent-specific colors
    const val CODING_AGENT_COLOR = "#2196F3"    // Blue
    const val RESEARCH_AGENT_COLOR = "#9C27B0"  // Purple
    const val QUANT_AGENT_COLOR = "#4CAF50"     // Green
    const val ORCHESTRATOR_COLOR = "#FF9800"    // Orange

    // Context indicator colors
    const val CONTEXT_EXPAND = "#00BCD4"
    const val CONTEXT_RESET = "#F44336"

    // Semantic layer colors
    const val LAYER_TEXT = "#FFFFFF"
    const val LAYER_CODE = "#2196F3"
    const val LAYER_PROMPT = "#9C27B0"
    const val LAYER_SHELL = "#4CAF50"
    const val LAYER_MATH = "#FF9800"
    const val LAYER_DIAGRAM = "#E91E63"

    /**
     * The JARVIS theme definition.
     */
    val THEME = Theme(
        id = "jarvis",
        name = "JARVIS",
        backgroundColor = HUD_BACKGROUND,
        keyBackground = HUD_PANEL,
        keyBackgroundPressed = HUD_PANEL_PRESSED,
        keyText = JARVIS_WHITE,
        keySpecialBackground = JARVIS_BLUE,
        keySpecialText = JARVIS_WHITE,
        keyModifierActive = JARVIS_CYAN,
        accentColor = JARVIS_CYAN,
        symbolColor = JARVIS_ORANGE,
        numberColor = JARVIS_CYAN,
        borderColor = "#00D4FF40",  // Cyan border with alpha
        popupBackground = HUD_PANEL
    )
}
