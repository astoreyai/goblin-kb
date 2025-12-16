package dev.kymera.keyboard.layouts

import dev.kymera.keyboard.core.KeyAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable layout configuration.
 */
@Serializable
data class KeyLayoutConfig(
    val name: String,
    val description: String = "",
    @SerialName("auto_detect_packages")
    val autoDetectPackages: List<String> = emptyList(),
    val rows: List<KeyRowConfig>
) {
    fun toKeyLayout(): KeyLayout = KeyLayout(
        name = name,
        rows = rows.map { it.toKeyRow() }
    )
}

@Serializable
data class KeyRowConfig(
    val name: String = "",
    val keys: List<KeyConfig>
) {
    fun toKeyRow(): KeyRow = KeyRow(
        keys = keys.map { it.toKey() }
    )
}

@Serializable
data class KeyConfig(
    val label: String,
    val value: String = label,
    val action: String = "insert",
    @SerialName("action_value")
    val actionValue: String = value,
    @SerialName("is_special")
    val isSpecial: Boolean = false
) {
    fun toKey(): Key = Key(
        value = value,
        label = label,
        isSpecial = isSpecial,
        action = parseAction()
    )

    private fun parseAction(): KeyAction = when (action) {
        "insert" -> KeyAction.InsertText(actionValue)
        "insertPaired", "insert_paired" -> {
            val parts = actionValue.split(",")
            if (parts.size == 2) {
                KeyAction.InsertPaired(parts[0], parts[1])
            } else {
                KeyAction.InsertText(actionValue)
            }
        }
        "delete" -> KeyAction.Delete
        "enter" -> KeyAction.Enter
        "switch_layout", "switchLayout" -> KeyAction.SwitchLayout(actionValue)
        "slash_command", "slashCommand" -> KeyAction.SlashCommand(actionValue)
        "control_key", "controlKey" -> KeyAction.ControlKey(actionValue)
        "send_to_local_agent", "sendToLocalAgent" -> KeyAction.SendToLocalAgent(actionValue)
        else -> KeyAction.InsertText(value)
    }
}
