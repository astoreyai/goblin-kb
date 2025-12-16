package dev.kymera.keyboard.layouts

import dev.kymera.keyboard.core.KeyAction
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class KeyLayoutTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `KeyConfig parses insert action correctly`() {
        val config = KeyConfig(
            label = "a",
            value = "a",
            action = "insert",
            actionValue = "a"
        )

        val key = config.toKey()

        assertEquals("a", key.label)
        assertEquals("a", key.value)
        assertFalse(key.isSpecial)
        assertTrue(key.action is KeyAction.InsertText)
        assertEquals("a", (key.action as KeyAction.InsertText).text)
    }

    @Test
    fun `KeyConfig parses insert_paired action correctly`() {
        val config = KeyConfig(
            label = "{}",
            value = "{}",
            action = "insert_paired",
            actionValue = "{,}"
        )

        val key = config.toKey()

        assertTrue(key.action is KeyAction.InsertPaired)
        val action = key.action as KeyAction.InsertPaired
        assertEquals("{", action.open)
        assertEquals("}", action.close)
    }

    @Test
    fun `KeyConfig parses delete action correctly`() {
        val config = KeyConfig(
            label = "DEL",
            action = "delete",
            isSpecial = true
        )

        val key = config.toKey()

        assertTrue(key.isSpecial)
        assertTrue(key.action is KeyAction.Delete)
    }

    @Test
    fun `KeyConfig parses switch_layout action correctly`() {
        val config = KeyConfig(
            label = "SYM",
            action = "switch_layout",
            actionValue = "symbols",
            isSpecial = true
        )

        val key = config.toKey()

        assertTrue(key.action is KeyAction.SwitchLayout)
        assertEquals("symbols", (key.action as KeyAction.SwitchLayout).layout)
    }

    @Test
    fun `KeyConfig parses control_key action correctly`() {
        val config = KeyConfig(
            label = "C-c",
            action = "control_key",
            actionValue = "C-c",
            isSpecial = true
        )

        val key = config.toKey()

        assertTrue(key.action is KeyAction.ControlKey)
        assertEquals("C-c", (key.action as KeyAction.ControlKey).key)
    }

    @Test
    fun `KeyLayoutConfig deserializes from JSON`() {
        val jsonString = """
            {
                "name": "test",
                "description": "Test layout",
                "rows": [
                    {
                        "name": "row1",
                        "keys": [
                            { "label": "a" },
                            { "label": "b" }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val config = json.decodeFromString<KeyLayoutConfig>(jsonString)
        val layout = config.toKeyLayout()

        assertEquals("test", layout.name)
        assertEquals(1, layout.rows.size)
        assertEquals(2, layout.rows[0].keys.size)
        assertEquals("a", layout.rows[0].keys[0].label)
        assertEquals("b", layout.rows[0].keys[1].label)
    }

    @Test
    fun `KeyLayout calculates maxKeysPerRow correctly`() {
        val layout = KeyLayout(
            name = "test",
            rows = listOf(
                KeyRow(listOf(Key("a"), Key("b"), Key("c"))),
                KeyRow(listOf(Key("d"), Key("e"))),
                KeyRow(listOf(Key("f"), Key("g"), Key("h"), Key("i")))
            )
        )

        assertEquals(4, layout.maxKeysPerRow)
    }
}
