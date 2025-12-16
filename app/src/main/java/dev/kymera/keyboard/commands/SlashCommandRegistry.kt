package dev.kymera.keyboard.commands

import android.content.Context
import android.view.inputmethod.InputConnection
import dev.kymera.keyboard.R
import dev.kymera.keyboard.agents.CommandContext
import dev.kymera.keyboard.core.CommandResult
import dev.kymera.keyboard.core.InputContext
import dev.kymera.keyboard.core.SemanticLayer
import dev.kymera.keyboard.settings.PreferencesManager
import kotlinx.serialization.json.Json

/**
 * Registry for slash commands.
 * Loads built-in commands from JSON and merges with user-defined commands.
 * Supports context injection for template placeholders.
 */
class SlashCommandRegistry(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val prefs by lazy { PreferencesManager(context) }
    private val contextInjector by lazy { ContextInjector(context) }

    // All registered commands
    private val commands = mutableMapOf<String, SlashCommand>()

    // Category metadata
    private val categories = mutableMapOf<String, CommandCategory>()

    init {
        loadBuiltInCommands()
        loadCustomCommands()
    }

    private fun loadBuiltInCommands() {
        try {
            val jsonString = context.resources.openRawResource(R.raw.commands)
                .bufferedReader()
                .use { it.readText() }

            val config = json.decodeFromString<CommandsConfig>(jsonString)

            for ((categoryName, category) in config.categories) {
                categories[categoryName] = CommandCategory(
                    name = categoryName,
                    icon = category.icon,
                    description = category.description
                )

                for (cmd in category.commands) {
                    commands[cmd.command] = SlashCommand(
                        command = cmd.command,
                        description = cmd.description,
                        category = categoryName,
                        action = cmd.toSlashAction(),
                        isBuiltIn = true
                    )
                }
            }
        } catch (e: Exception) {
            // Log error, continue with empty commands
        }
    }

    private fun loadCustomCommands() {
        val customCommands = prefs.getCustomCommands()
        for (cmd in customCommands) {
            commands[cmd.command] = cmd.copy(isBuiltIn = false)
        }
    }

    /**
     * Execute a slash command.
     *
     * @param input The command input (e.g., "/git status")
     * @param inputContext The detected input context
     * @param inputConnection Optional input connection for context extraction
     * @param packageName The package name of the target app
     * @param activeAgentId The currently active agent ID
     * @param semanticLayer The current semantic layer
     */
    fun execute(
        input: String,
        inputContext: InputContext,
        inputConnection: InputConnection? = null,
        packageName: String? = null,
        activeAgentId: String? = null,
        semanticLayer: SemanticLayer = SemanticLayer.TEXT
    ): CommandResult {
        // Exact match
        commands[input]?.let { cmd ->
            return executeCommand(cmd, inputContext, inputConnection, packageName, activeAgentId, semanticLayer)
        }

        // Prefix match - return suggestions
        val matches = commands.keys.filter { it.startsWith(input) }
        if (matches.isNotEmpty()) {
            return CommandResult.ShowPicker(matches)
        }

        return CommandResult.NotFound
    }

    /**
     * Extract context for command execution.
     */
    fun extractContext(
        inputConnection: InputConnection?,
        inputContext: InputContext,
        packageName: String? = null,
        activeAgentId: String? = null,
        semanticLayer: SemanticLayer = SemanticLayer.TEXT,
        lastAgentOutput: String? = null
    ): CommandContext {
        return contextInjector.extractContext(
            inputConnection = inputConnection,
            inputContext = inputContext,
            packageName = packageName,
            activeAgentId = activeAgentId,
            semanticLayer = semanticLayer,
            lastAgentOutput = lastAgentOutput
        )
    }

    private fun executeCommand(
        cmd: SlashCommand,
        inputContext: InputContext,
        inputConnection: InputConnection?,
        packageName: String?,
        activeAgentId: String?,
        semanticLayer: SemanticLayer
    ): CommandResult {
        return when (val action = cmd.action) {
            is SlashAction.InsertText -> CommandResult.InsertText(action.text)
            is SlashAction.InsertTemplate -> {
                // Inject context placeholders into the template
                val injectedTemplate = contextInjector.injectPlaceholders(
                    action.template,
                    inputConnection
                )
                CommandResult.InsertText(injectedTemplate)
            }
            is SlashAction.SendToKymera -> {
                // Extract context for KYMERA commands
                val commandContext = contextInjector.extractContext(
                    inputConnection = inputConnection,
                    inputContext = inputContext,
                    packageName = packageName,
                    activeAgentId = activeAgentId,
                    semanticLayer = semanticLayer
                )
                CommandResult.SendToKymera(action.command, action.type, commandContext)
            }
        }
    }

    /**
     * Get all commands for a category.
     */
    fun getCommandsByCategory(category: String): List<SlashCommand> {
        return commands.values.filter { it.category == category }
    }

    /**
     * Get all categories.
     */
    fun getCategories(): List<CommandCategory> {
        return categories.values.toList()
    }

    /**
     * Search commands by query.
     */
    fun search(query: String): List<SlashCommand> {
        val lowerQuery = query.lowercase()
        return commands.values.filter {
            it.command.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Add a custom command.
     */
    fun addCustomCommand(command: SlashCommand) {
        commands[command.command] = command.copy(isBuiltIn = false)
        saveCustomCommands()
    }

    /**
     * Remove a custom command.
     */
    fun removeCustomCommand(commandName: String) {
        val cmd = commands[commandName]
        if (cmd != null && !cmd.isBuiltIn) {
            commands.remove(commandName)
            saveCustomCommands()
        }
    }

    private fun saveCustomCommands() {
        val customCommands = commands.values.filter { !it.isBuiltIn }
        prefs.saveCustomCommands(customCommands.toList())
    }

    /**
     * Reload commands (after settings change or broadcast).
     */
    fun reload() {
        commands.clear()
        categories.clear()
        loadBuiltInCommands()
        loadCustomCommands()
    }
}
