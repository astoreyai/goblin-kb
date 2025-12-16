package dev.kymera.keyboard.ui

import android.content.Context
import android.graphics.Color
import dev.kymera.keyboard.settings.PreferencesManager

/**
 * Manages keyboard themes and provides parsed color values.
 */
class ThemeManager(private val context: Context) {

    private val prefs = PreferencesManager(context)
    private var currentTheme: Theme = Theme.DARK
    private var themeChangeListener: ((Theme) -> Unit)? = null

    init {
        loadTheme(prefs.currentTheme)
    }

    fun loadTheme(themeId: String) {
        currentTheme = Theme.ALL_THEMES.find { it.id == themeId } ?: Theme.DARK
        prefs.currentTheme = currentTheme.id
        themeChangeListener?.invoke(currentTheme)
    }

    fun getCurrentTheme(): Theme = currentTheme

    fun getAllThemes(): List<Theme> = Theme.ALL_THEMES

    fun setThemeChangeListener(listener: (Theme) -> Unit) {
        themeChangeListener = listener
    }

    fun nextTheme() {
        val currentIndex = Theme.ALL_THEMES.indexOfFirst { it.id == currentTheme.id }
        val nextIndex = (currentIndex + 1) % Theme.ALL_THEMES.size
        loadTheme(Theme.ALL_THEMES[nextIndex].id)
    }

    // Parsed color accessors
    fun getBackgroundColor(): Int = parseColor(currentTheme.backgroundColor)
    fun getKeyBackground(): Int = parseColor(currentTheme.keyBackground)
    fun getKeyBackgroundPressed(): Int = parseColor(currentTheme.keyBackgroundPressed)
    fun getKeyText(): Int = parseColor(currentTheme.keyText)
    fun getKeySpecialBackground(): Int = parseColor(currentTheme.keySpecialBackground)
    fun getKeySpecialText(): Int = parseColor(currentTheme.keySpecialText)
    fun getKeyModifierActive(): Int = parseColor(currentTheme.keyModifierActive)
    fun getAccentColor(): Int = parseColor(currentTheme.accentColor)
    fun getSymbolColor(): Int = parseColor(currentTheme.symbolColor)
    fun getNumberColor(): Int = parseColor(currentTheme.numberColor)
    fun getBorderColor(): Int = parseColor(currentTheme.borderColor)
    fun getPopupBackground(): Int = parseColor(currentTheme.popupBackground)

    private fun parseColor(hex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.MAGENTA // Obvious error color
        }
    }
}
