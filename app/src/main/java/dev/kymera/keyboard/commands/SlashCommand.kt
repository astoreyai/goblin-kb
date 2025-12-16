package dev.kymera.keyboard.commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A slash command definition.
 */
@Serializable
data class SlashCommand(
    val command: String,
    val description: String,
    val category: String,
    val action: SlashAction,
    @SerialName("is_built_in")
    val isBuiltIn: Boolean = false
)

/**
 * The action performed when a slash command is executed.
 */
@Serializable
sealed class SlashAction {
    /** Insert plain text */
    @Serializable
    @SerialName("insert_text")
    data class InsertText(val text: String) : SlashAction()

    /** Insert template with placeholders */
    @Serializable
    @SerialName("insert_template")
    data class InsertTemplate(val template: String) : SlashAction()

    /** Send command to local agent app (fallback when Termux unavailable) */
    @Serializable
    @SerialName("send_to_local_agent")
    data class SendToLocalAgent(val command: String, val type: String = "command") : SlashAction()
}

/**
 * Category metadata.
 */
data class CommandCategory(
    val name: String,
    val icon: String,
    val description: String
)

// --- JSON Configuration Classes ---

@Serializable
data class CommandsConfig(
    val version: Int = 1,
    val categories: Map<String, CategoryConfig>
)

@Serializable
data class CategoryConfig(
    val icon: String = "",
    val description: String = "",
    val commands: List<CommandConfig>
)

@Serializable
data class CommandConfig(
    val command: String,
    val description: String,
    val action: String,
    val value: String,
    val type: String = "command"
) {
    fun toSlashAction(): SlashAction = when (action) {
        "insert_text" -> SlashAction.InsertText(value)
        "insert_template" -> SlashAction.InsertTemplate(value)
        "send_to_local_agent" -> SlashAction.SendToLocalAgent(value, type)
        else -> SlashAction.InsertText(value)
    }
}
