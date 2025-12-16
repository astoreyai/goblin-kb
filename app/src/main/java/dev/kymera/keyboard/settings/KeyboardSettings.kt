package dev.kymera.keyboard.settings

import dev.kymera.keyboard.commands.SlashCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Complete keyboard settings for export/import.
 */
@Serializable
data class KeyboardSettings(
    @SerialName("haptic_enabled")
    val hapticEnabled: Boolean = true,

    @SerialName("haptic_strength")
    val hapticStrength: Int = 50,

    @SerialName("sound_enabled")
    val soundEnabled: Boolean = false,

    @SerialName("swipe_enabled")
    val swipeEnabled: Boolean = true,

    @SerialName("ai_button_enabled")
    val aiButtonEnabled: Boolean = true,

    @SerialName("predictions_enabled")
    val predictionsEnabled: Boolean = true,

    @SerialName("auto_capitalize")
    val autoCapitalize: Boolean = true,

    @SerialName("auto_correct")
    val autoCorrect: Boolean = false,

    val theme: String = "dark",

    @SerialName("default_layout")
    val defaultLayout: String = "code",

    @SerialName("key_height")
    val keyHeight: Int = 48,

    @SerialName("long_press_delay")
    val longPressDelay: Int = 300,

    @SerialName("custom_commands")
    val customCommands: List<SlashCommand> = emptyList(),

    @SerialName("package_overrides")
    val packageOverrides: Map<String, String> = emptyMap()
)
