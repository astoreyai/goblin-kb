package dev.kymera.keyboard.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dev.kymera.keyboard.R
import dev.kymera.keyboard.layouts.LayoutManager
import dev.kymera.keyboard.settings.PreferencesManager

/**
 * Settings activity for KB JARVIS Keyboard.
 * Provides theme picker, layout picker, and keyboard configuration.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var prefs: PreferencesManager
    private lateinit var themeContainer: LinearLayout
    private lateinit var layoutContainer: LinearLayout

    private var selectedThemeId: String = "dark"
    private var selectedLayoutId: String = "code"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PreferencesManager(this)
        statusText = findViewById(R.id.statusEnabled)
        themeContainer = findViewById(R.id.themeContainer)
        layoutContainer = findViewById(R.id.layoutContainer)

        // Load saved preferences
        selectedThemeId = prefs.currentTheme
        selectedLayoutId = prefs.defaultLayout

        // Enable keyboard button
        findViewById<Button>(R.id.btnEnableKeyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        // Select keyboard button
        findViewById<Button>(R.id.btnSelectKeyboard).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        // Setup theme picker
        setupThemePicker()

        // Setup layout picker
        setupLayoutPicker()

        // Setup split mode
        setupSplitMode()

        // Setup column stagger toggle
        setupColumnStagger()

        // Setup row height slider
        setupRowHeight()
    }

    private fun setupThemePicker() {
        val themes = listOf(
            ThemePreview("jarvis", "JARVIS", "#0A0F14", "#00D4FF"),
            ThemePreview("dark", "Dark", "#1A1A1A", "#2196F3"),
            ThemePreview("light", "Light", "#F5F5F5", "#1976D2"),
            ThemePreview("monokai", "Monokai", "#272822", "#A6E22E"),
            ThemePreview("dracula", "Dracula", "#282A36", "#BD93F9"),
            ThemePreview("nord", "Nord", "#2E3440", "#88C0D0"),
            ThemePreview("solarized_dark", "Solarized", "#002B36", "#268BD2"),
            ThemePreview("oled_black", "OLED", "#000000", "#2196F3"),
            ThemePreview("high_contrast", "High Contrast", "#000000", "#FFFF00"),
            ThemePreview("beeraider", "BeeRaider", "#1C1C1C", "#F0AD4E")
        )

        for (theme in themes) {
            val card = createThemeCard(theme)
            themeContainer.addView(card)
        }
    }

    private fun createThemeCard(theme: ThemePreview): LinearLayout {
        val density = resources.displayMetrics.density
        val cardWidth = (80 * density).toInt()
        val cardHeight = (90 * density).toInt()
        val margin = (8 * density).toInt()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(cardWidth, cardHeight).apply {
                marginEnd = margin
            }

            // Background with rounded corners
            background = GradientDrawable().apply {
                setColor(Color.parseColor(theme.bgColor))
                cornerRadius = 8 * density
                if (theme.id == selectedThemeId) {
                    setStroke((2 * density).toInt(), Color.parseColor(theme.accentColor))
                } else {
                    setStroke((1 * density).toInt(), Color.parseColor("#333333"))
                }
            }

            setOnClickListener {
                selectedThemeId = theme.id
                prefs.currentTheme = theme.id
                refreshThemeSelection()
            }
        }

        // Accent color indicator
        val accent = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (40 * density).toInt(),
                (8 * density).toInt()
            ).apply {
                topMargin = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor(theme.accentColor))
                cornerRadius = 4 * density
            }
        }
        card.addView(accent)

        // Theme name
        val name = TextView(this).apply {
            text = theme.name
            setTextColor(
                if (theme.bgColor == "#F5F5F5") Color.BLACK
                else Color.WHITE
            )
            textSize = 11f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
        }
        card.addView(name)

        return card
    }

    private fun refreshThemeSelection() {
        themeContainer.removeAllViews()
        setupThemePicker()
    }

    private fun setupLayoutPicker() {
        val layouts = listOf(
            LayoutPreview("jarvis", "JARVIS", "Agents + Split"),
            LayoutPreview("code", "Code", "Dev focused"),
            LayoutPreview("programmer", "Programmer", "Symbol first"),
            LayoutPreview("terminal", "Terminal", "Ctrl combos"),
            LayoutPreview("vim", "Vim", "ESC + Motion"),
            LayoutPreview("compact", "Compact", "Minimal 4-row"),
            LayoutPreview("qwerty", "QWERTY", "Standard"),
            LayoutPreview("symbols", "Symbols", "Full grid")
        )

        for (layout in layouts) {
            val card = createLayoutCard(layout)
            layoutContainer.addView(card)
        }
    }

    private fun createLayoutCard(layout: LayoutPreview): LinearLayout {
        val density = resources.displayMetrics.density
        val cardWidth = (90 * density).toInt()
        val cardHeight = (70 * density).toInt()
        val margin = (8 * density).toInt()

        val isSelected = layout.id == selectedLayoutId
        val bgColor = if (isSelected) "#00D4FF" else "#101820"
        val textColor = if (isSelected) "#000000" else "#E8F4F8"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(cardWidth, cardHeight).apply {
                marginEnd = margin
            }
            setPadding(
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt()
            )

            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 8 * density
            }

            setOnClickListener {
                selectedLayoutId = layout.id
                prefs.defaultLayout = layout.id
                refreshLayoutSelection()
            }
        }

        // Layout name
        val name = TextView(this).apply {
            text = layout.name
            setTextColor(Color.parseColor(textColor))
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        card.addView(name)

        // Description
        val desc = TextView(this).apply {
            text = layout.description
            setTextColor(Color.parseColor(if (isSelected) "#333333" else "#888888"))
            textSize = 10f
            gravity = Gravity.CENTER
        }
        card.addView(desc)

        return card
    }

    private fun refreshLayoutSelection() {
        layoutContainer.removeAllViews()
        setupLayoutPicker()
    }

    private fun setupSplitMode() {
        val radioGroup = findViewById<RadioGroup>(R.id.splitModeGroup)
        val currentMode = prefs.getSplitMode()

        when (currentMode) {
            "none" -> radioGroup.check(R.id.splitModeNone)
            "auto" -> radioGroup.check(R.id.splitModeAuto)
            "always" -> radioGroup.check(R.id.splitModeAlways)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.splitModeNone -> "none"
                R.id.splitModeAuto -> "auto"
                R.id.splitModeAlways -> "always"
                else -> "auto"
            }
            prefs.setSplitMode(mode)
        }
    }

    private fun setupColumnStagger() {
        val switch = findViewById<Switch>(R.id.switchColumnStagger)
        switch.isChecked = prefs.isColumnStaggerEnabled()

        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setColumnStaggerEnabled(isChecked)
        }
    }

    private fun setupRowHeight() {
        val seekBar = findViewById<SeekBar>(R.id.seekRowHeight)
        val txtHeight = findViewById<TextView>(R.id.txtRowHeight)

        val currentHeight = prefs.getRowHeight()
        seekBar.progress = currentHeight
        txtHeight.text = "${currentHeight}dp"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                txtHeight.text = "${progress}dp"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                prefs.setRowHeight(seekBar.progress)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateKeyboardStatus()
    }

    private fun updateKeyboardStatus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList

        val isEnabled = enabledMethods.any {
            it.packageName == packageName
        }

        if (isEnabled) {
            statusText.text = "Enabled"
            statusText.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            statusText.text = "Not enabled"
            statusText.setTextColor(Color.parseColor("#FF9800"))
        }
    }

    data class ThemePreview(
        val id: String,
        val name: String,
        val bgColor: String,
        val accentColor: String
    )

    data class LayoutPreview(
        val id: String,
        val name: String,
        val description: String
    )
}
