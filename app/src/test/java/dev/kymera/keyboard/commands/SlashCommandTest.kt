package dev.kymera.keyboard.commands

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SlashCommandTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `CommandConfig creates InsertText action`() {
        val config = CommandConfig(
            command = "/test",
            description = "Test command",
            action = "insert_text",
            value = "test output"
        )

        val action = config.toSlashAction()

        assertTrue(action is SlashAction.InsertText)
        assertEquals("test output", (action as SlashAction.InsertText).text)
    }

    @Test
    fun `CommandConfig creates InsertTemplate action`() {
        val config = CommandConfig(
            command = "/git commit",
            description = "Commit changes",
            action = "insert_template",
            value = "git commit -m \"\${1:message}\""
        )

        val action = config.toSlashAction()

        assertTrue(action is SlashAction.InsertTemplate)
        assertEquals("git commit -m \"\${1:message}\"", (action as SlashAction.InsertTemplate).template)
    }

    @Test
    fun `CommandConfig creates SendToLocalAgent action`() {
        val config = CommandConfig(
            command = "/ai explain",
            description = "Explain code",
            action = "send_to_local_agent",
            value = "explain",
            type = "code"
        )

        val action = config.toSlashAction()

        assertTrue(action is SlashAction.SendToLocalAgent)
        val localAgentAction = action as SlashAction.SendToLocalAgent
        assertEquals("explain", localAgentAction.command)
        assertEquals("code", localAgentAction.type)
    }

    @Test
    fun `CommandsConfig deserializes from JSON`() {
        val jsonString = """
            {
                "version": 1,
                "categories": {
                    "git": {
                        "icon": "git",
                        "description": "Git commands",
                        "commands": [
                            {
                                "command": "/git status",
                                "description": "Show status",
                                "action": "insert_text",
                                "value": "git status"
                            }
                        ]
                    }
                }
            }
        """.trimIndent()

        val config = json.decodeFromString<CommandsConfig>(jsonString)

        assertEquals(1, config.version)
        assertTrue(config.categories.containsKey("git"))
        assertEquals("git", config.categories["git"]?.icon)
        assertEquals(1, config.categories["git"]?.commands?.size)
        assertEquals("/git status", config.categories["git"]?.commands?.get(0)?.command)
    }

    @Test
    fun `SlashCommand serializes and deserializes correctly`() {
        val command = SlashCommand(
            command = "/test",
            description = "Test command",
            category = "custom",
            action = SlashAction.InsertText("test"),
            isBuiltIn = false
        )

        val jsonString = json.encodeToString(SlashCommand.serializer(), command)
        val deserialized = json.decodeFromString(SlashCommand.serializer(), jsonString)

        assertEquals(command.command, deserialized.command)
        assertEquals(command.description, deserialized.description)
        assertEquals(command.category, deserialized.category)
        assertEquals(command.isBuiltIn, deserialized.isBuiltIn)
    }
}
