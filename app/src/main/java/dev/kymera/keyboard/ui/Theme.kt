package dev.kymera.keyboard.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Theme definition for keyboard appearance.
 * Inspired by BeeRaider's color-coded key system and CodeBoard's customization.
 */
@Serializable
data class Theme(
    val id: String,
    val name: String,

    @SerialName("background")
    val backgroundColor: String = "#1A1A1A",

    @SerialName("key_background")
    val keyBackground: String = "#2D2D2D",

    @SerialName("key_background_pressed")
    val keyBackgroundPressed: String = "#404040",

    @SerialName("key_text")
    val keyText: String = "#FFFFFF",

    @SerialName("key_special")
    val keySpecialBackground: String = "#1E88E5",

    @SerialName("key_special_text")
    val keySpecialText: String = "#FFFFFF",

    @SerialName("key_modifier_active")
    val keyModifierActive: String = "#4CAF50",

    @SerialName("accent_color")
    val accentColor: String = "#2196F3",

    @SerialName("symbol_color")
    val symbolColor: String = "#FFC107",

    @SerialName("number_color")
    val numberColor: String = "#64B5F6",

    @SerialName("border_color")
    val borderColor: String = "#333333",

    @SerialName("popup_background")
    val popupBackground: String = "#424242"
) {
    companion object {
        // Dark theme - default developer theme
        val DARK = Theme(
            id = "dark",
            name = "Dark",
            backgroundColor = "#1A1A1A",
            keyBackground = "#2D2D2D",
            keyBackgroundPressed = "#404040",
            keyText = "#FFFFFF",
            keySpecialBackground = "#1E88E5",
            keySpecialText = "#FFFFFF",
            keyModifierActive = "#4CAF50",
            accentColor = "#2196F3",
            symbolColor = "#FFC107",
            numberColor = "#64B5F6"
        )

        // Light theme - for outdoor use
        val LIGHT = Theme(
            id = "light",
            name = "Light",
            backgroundColor = "#F5F5F5",
            keyBackground = "#FFFFFF",
            keyBackgroundPressed = "#E0E0E0",
            keyText = "#212121",
            keySpecialBackground = "#1976D2",
            keySpecialText = "#FFFFFF",
            keyModifierActive = "#388E3C",
            accentColor = "#1976D2",
            symbolColor = "#F57C00",
            numberColor = "#1565C0",
            borderColor = "#BDBDBD"
        )

        // High contrast - accessibility
        val HIGH_CONTRAST = Theme(
            id = "high_contrast",
            name = "High Contrast",
            backgroundColor = "#000000",
            keyBackground = "#000000",
            keyBackgroundPressed = "#333333",
            keyText = "#FFFFFF",
            keySpecialBackground = "#FFFF00",
            keySpecialText = "#000000",
            keyModifierActive = "#00FF00",
            accentColor = "#FFFF00",
            symbolColor = "#FFFF00",
            numberColor = "#00FFFF",
            borderColor = "#FFFFFF"
        )

        // Solarized Dark - popular dev theme
        val SOLARIZED_DARK = Theme(
            id = "solarized_dark",
            name = "Solarized Dark",
            backgroundColor = "#002B36",
            keyBackground = "#073642",
            keyBackgroundPressed = "#094959",
            keyText = "#839496",
            keySpecialBackground = "#268BD2",
            keySpecialText = "#FDF6E3",
            keyModifierActive = "#859900",
            accentColor = "#2AA198",
            symbolColor = "#B58900",
            numberColor = "#6C71C4"
        )

        // Solarized Light
        val SOLARIZED_LIGHT = Theme(
            id = "solarized_light",
            name = "Solarized Light",
            backgroundColor = "#FDF6E3",
            keyBackground = "#EEE8D5",
            keyBackgroundPressed = "#D6D0C1",
            keyText = "#657B83",
            keySpecialBackground = "#268BD2",
            keySpecialText = "#FDF6E3",
            keyModifierActive = "#859900",
            accentColor = "#2AA198",
            symbolColor = "#B58900",
            numberColor = "#6C71C4"
        )

        // Monokai - classic code editor theme
        val MONOKAI = Theme(
            id = "monokai",
            name = "Monokai",
            backgroundColor = "#272822",
            keyBackground = "#3E3D32",
            keyBackgroundPressed = "#49483E",
            keyText = "#F8F8F2",
            keySpecialBackground = "#A6E22E",
            keySpecialText = "#272822",
            keyModifierActive = "#66D9EF",
            accentColor = "#F92672",
            symbolColor = "#E6DB74",
            numberColor = "#AE81FF"
        )

        // Dracula - trendy dark theme
        val DRACULA = Theme(
            id = "dracula",
            name = "Dracula",
            backgroundColor = "#282A36",
            keyBackground = "#44475A",
            keyBackgroundPressed = "#6272A4",
            keyText = "#F8F8F2",
            keySpecialBackground = "#BD93F9",
            keySpecialText = "#282A36",
            keyModifierActive = "#50FA7B",
            accentColor = "#FF79C6",
            symbolColor = "#F1FA8C",
            numberColor = "#8BE9FD"
        )

        // Nord - calm and clean
        val NORD = Theme(
            id = "nord",
            name = "Nord",
            backgroundColor = "#2E3440",
            keyBackground = "#3B4252",
            keyBackgroundPressed = "#434C5E",
            keyText = "#ECEFF4",
            keySpecialBackground = "#5E81AC",
            keySpecialText = "#ECEFF4",
            keyModifierActive = "#A3BE8C",
            accentColor = "#88C0D0",
            symbolColor = "#EBCB8B",
            numberColor = "#81A1C1"
        )

        // OLED Black - true black for AMOLED screens
        val OLED_BLACK = Theme(
            id = "oled_black",
            name = "OLED Black",
            backgroundColor = "#000000",
            keyBackground = "#121212",
            keyBackgroundPressed = "#1F1F1F",
            keyText = "#E0E0E0",
            keySpecialBackground = "#1E88E5",
            keySpecialText = "#FFFFFF",
            keyModifierActive = "#4CAF50",
            accentColor = "#2196F3",
            symbolColor = "#FFC107",
            numberColor = "#64B5F6"
        )

        // BeeRaider inspired - color-coded categories
        val BEERAIDER = Theme(
            id = "beeraider",
            name = "BeeRaider",
            backgroundColor = "#1C1C1C",
            keyBackground = "#2A2A2A",
            keyBackgroundPressed = "#3D3D3D",
            keyText = "#FFFFFF",
            keySpecialBackground = "#4A90D9",  // Blue for special
            keySpecialText = "#FFFFFF",
            keyModifierActive = "#5CB85C",     // Green for modifiers
            accentColor = "#F0AD4E",           // Yellow/amber accent
            symbolColor = "#D9534F",           // Red for symbols
            numberColor = "#5BC0DE"            // Cyan for numbers
        )

        val ALL_THEMES = listOf(
            DARK, LIGHT, HIGH_CONTRAST,
            SOLARIZED_DARK, SOLARIZED_LIGHT,
            MONOKAI, DRACULA, NORD,
            OLED_BLACK, BEERAIDER, JarvisTheme.THEME
        )
    }
}
