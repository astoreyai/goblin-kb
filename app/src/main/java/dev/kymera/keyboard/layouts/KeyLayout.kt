package dev.kymera.keyboard.layouts

import dev.kymera.keyboard.core.KeyAction

/**
 * Represents a complete keyboard layout.
 */
data class KeyLayout(
    val name: String,
    val rows: List<KeyRow>
) {
    val maxKeysPerRow: Int
        get() = rows.maxOfOrNull { it.keys.size } ?: 10
}

/**
 * A row of keys in the layout.
 */
data class KeyRow(
    val keys: List<Key>
)

/**
 * A single key in the layout.
 */
data class Key(
    val value: String,
    val label: String = value,
    val isSpecial: Boolean = false,
    val action: KeyAction = KeyAction.InsertText(value)
)
