package dev.kymera.keyboard.core

/**
 * Represents the detected input context for layout switching.
 */
enum class InputContext {
    /** Default general typing context */
    GENERAL,

    /** Terminal/shell environment (Termux, etc.) */
    TERMINAL,

    /** Code editor context */
    CODE,

    /** KYMERA app chat/editor */
    KYMERA_CHAT
}
