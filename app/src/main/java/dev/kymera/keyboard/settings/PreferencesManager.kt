package dev.kymera.keyboard.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dev.kymera.keyboard.commands.SlashCommand
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages keyboard preferences using SharedPreferences.
 * Provides typed access to all configurable settings.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val PREFS_NAME = "kb_preferences"

        // Keys
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_HAPTIC_STRENGTH = "haptic_strength"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_THEME = "theme"
        private const val KEY_DEFAULT_LAYOUT = "default_layout"
        private const val KEY_SWIPE_ENABLED = "swipe_enabled"
        private const val KEY_AI_BUTTON_ENABLED = "ai_button_enabled"
        private const val KEY_PREDICTIONS_ENABLED = "predictions_enabled"
        private const val KEY_AUTO_CAPITALIZE = "auto_capitalize"
        private const val KEY_AUTO_CORRECT = "auto_correct"
        private const val KEY_CUSTOM_COMMANDS = "custom_commands"
        private const val KEY_PACKAGE_OVERRIDES = "package_overrides"
        private const val KEY_KEY_HEIGHT = "key_height"
        private const val KEY_LONG_PRESS_DELAY = "long_press_delay"
        private const val KEY_SPLIT_MODE = "split_mode"
        private const val KEY_COLUMN_STAGGER = "column_stagger"
        private const val KEY_ROW_HEIGHT = "row_height"

        // Defaults
        private const val DEFAULT_HAPTIC_STRENGTH = 50
        private const val DEFAULT_THEME = "dark"
        private const val DEFAULT_LAYOUT = "code"
        private const val DEFAULT_KEY_HEIGHT = 48 // 48dp meets touch target guidelines
        private const val DEFAULT_LONG_PRESS_DELAY = 300
    }

    // --- Feature Flags ---

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_HAPTIC_ENABLED, value) }

    var hapticStrength: Int
        get() = prefs.getInt(KEY_HAPTIC_STRENGTH, DEFAULT_HAPTIC_STRENGTH)
        set(value) = prefs.edit { putInt(KEY_HAPTIC_STRENGTH, value.coerceIn(0, 100)) }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_SOUND_ENABLED, value) }

    var swipeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SWIPE_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SWIPE_ENABLED, value) }

    var aiButtonEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_BUTTON_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_AI_BUTTON_ENABLED, value) }

    var predictionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_PREDICTIONS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_PREDICTIONS_ENABLED, value) }

    var autoCapitalize: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CAPITALIZE, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_CAPITALIZE, value) }

    var autoCorrect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CORRECT, false) // Off by default for devs
        set(value) = prefs.edit { putBoolean(KEY_AUTO_CORRECT, value) }

    // --- Appearance ---

    var theme: String
        get() = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        set(value) = prefs.edit { putString(KEY_THEME, value) }

    // Alias for ThemeManager compatibility
    var currentTheme: String
        get() = theme
        set(value) { theme = value }

    var defaultLayout: String
        get() = prefs.getString(KEY_DEFAULT_LAYOUT, DEFAULT_LAYOUT) ?: DEFAULT_LAYOUT
        set(value) = prefs.edit { putString(KEY_DEFAULT_LAYOUT, value) }

    var keyHeight: Int
        get() = prefs.getInt(KEY_KEY_HEIGHT, DEFAULT_KEY_HEIGHT)
        set(value) = prefs.edit { putInt(KEY_KEY_HEIGHT, value.coerceIn(40, 100)) }

    var longPressDelay: Int
        get() = prefs.getInt(KEY_LONG_PRESS_DELAY, DEFAULT_LONG_PRESS_DELAY)
        set(value) = prefs.edit { putInt(KEY_LONG_PRESS_DELAY, value.coerceIn(100, 1000)) }

    // --- Split Mode & Stagger ---

    fun getSplitMode(): String = prefs.getString(KEY_SPLIT_MODE, "auto") ?: "auto"

    fun setSplitMode(mode: String) = prefs.edit { putString(KEY_SPLIT_MODE, mode) }

    fun isColumnStaggerEnabled(): Boolean = prefs.getBoolean(KEY_COLUMN_STAGGER, true)

    fun setColumnStaggerEnabled(enabled: Boolean) = prefs.edit { putBoolean(KEY_COLUMN_STAGGER, enabled) }

    fun getRowHeight(): Int = prefs.getInt(KEY_ROW_HEIGHT, DEFAULT_KEY_HEIGHT)

    fun setRowHeight(height: Int) = prefs.edit { putInt(KEY_ROW_HEIGHT, height.coerceIn(44, 64)) }

    // --- Custom Commands ---

    fun getCustomCommands(): List<SlashCommand> {
        val jsonString = prefs.getString(KEY_CUSTOM_COMMANDS, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<SlashCommand>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustomCommands(commands: List<SlashCommand>) {
        val jsonString = json.encodeToString(commands)
        prefs.edit { putString(KEY_CUSTOM_COMMANDS, jsonString) }
    }

    // --- Package Overrides ---

    /**
     * Get context override for a specific package.
     * Returns null if no override set (use auto-detection).
     */
    fun getPackageOverride(packageName: String): String? {
        val overrides = getPackageOverrides()
        return overrides[packageName]
    }

    fun setPackageOverride(packageName: String, context: String?) {
        val overrides = getPackageOverrides().toMutableMap()
        if (context == null) {
            overrides.remove(packageName)
        } else {
            overrides[packageName] = context
        }
        savePackageOverrides(overrides)
    }

    private fun getPackageOverrides(): Map<String, String> {
        val jsonString = prefs.getString(KEY_PACKAGE_OVERRIDES, "{}") ?: "{}"
        return try {
            json.decodeFromString<Map<String, String>>(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun savePackageOverrides(overrides: Map<String, String>) {
        val jsonString = json.encodeToString(overrides)
        prefs.edit { putString(KEY_PACKAGE_OVERRIDES, jsonString) }
    }

    // --- Bulk Operations ---

    /**
     * Export all settings as JSON for backup.
     */
    fun exportSettings(): String {
        val settings = KeyboardSettings(
            hapticEnabled = hapticEnabled,
            hapticStrength = hapticStrength,
            soundEnabled = soundEnabled,
            swipeEnabled = swipeEnabled,
            aiButtonEnabled = aiButtonEnabled,
            predictionsEnabled = predictionsEnabled,
            autoCapitalize = autoCapitalize,
            autoCorrect = autoCorrect,
            theme = theme,
            defaultLayout = defaultLayout,
            keyHeight = keyHeight,
            longPressDelay = longPressDelay,
            customCommands = getCustomCommands(),
            packageOverrides = getPackageOverrides()
        )
        return json.encodeToString(settings)
    }

    /**
     * Import settings from JSON backup.
     */
    fun importSettings(jsonString: String): Boolean {
        return try {
            val settings = json.decodeFromString<KeyboardSettings>(jsonString)
            hapticEnabled = settings.hapticEnabled
            hapticStrength = settings.hapticStrength
            soundEnabled = settings.soundEnabled
            swipeEnabled = settings.swipeEnabled
            aiButtonEnabled = settings.aiButtonEnabled
            predictionsEnabled = settings.predictionsEnabled
            autoCapitalize = settings.autoCapitalize
            autoCorrect = settings.autoCorrect
            theme = settings.theme
            defaultLayout = settings.defaultLayout
            keyHeight = settings.keyHeight
            longPressDelay = settings.longPressDelay
            saveCustomCommands(settings.customCommands)
            savePackageOverrides(settings.packageOverrides)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reset all settings to defaults.
     */
    fun resetToDefaults() {
        prefs.edit { clear() }
    }
}
