package dev.kymera.keyboard.core

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import dev.kymera.keyboard.commands.SlashCommandRegistry
import dev.kymera.keyboard.layouts.ColumnStaggerCalculator
import dev.kymera.keyboard.layouts.KeyLayout
import dev.kymera.keyboard.layouts.LayoutManager
import dev.kymera.keyboard.layouts.SplitLayoutConfig
import dev.kymera.keyboard.rendering.GlowRenderer
import dev.kymera.keyboard.ui.ThemeManager

/**
 * Custom View for rendering the keyboard.
 * Uses Canvas drawing for optimal performance (<16ms per frame).
 */
class KBKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var layoutManager: LayoutManager? = null
    private var commandRegistry: SlashCommandRegistry? = null
    private var themeManager: ThemeManager? = null
    private var keyListener: ((KeyAction) -> Unit)? = null
    private var currentContext = InputContext.GENERAL

    // Drawing
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val keyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    // Touch tracking
    private var pressedKeyIndex: Int = -1
    private var ctrlActive = false
    private var shiftActive = false

    // Layout
    private var currentLayout: KeyLayout? = null
    private var keyRects = mutableListOf<RectF>()

    // Theme colors (configurable via ThemeManager)
    private var keyBackground = 0xFF2D2D2D.toInt()
    private var keyBackgroundPressed = 0xFF4A4A4A.toInt()
    private var keyText = 0xFFFFFFFF.toInt()
    private var keySpecial = 0xFF1E88E5.toInt()
    private var backgroundColor = 0xFF1A1A1A.toInt()
    private var keyModifierActive = 0xFF4CAF50.toInt()
    private var symbolColor = 0xFFFFC107.toInt()
    private var numberColor = 0xFF64B5F6.toInt()

    // Layout dimensions - minimum 48dp per row for good touch targets
    private var rowHeightDp = 48f
    private val density = resources.displayMetrics.density
    private val minKeyHeightDp = 44f

    // Split keyboard mode (Samsung-style for landscape thumb typing)
    enum class SplitMode { NONE, SPLIT, AUTO }
    private var splitMode = SplitMode.AUTO
    private var splitGapDp = 80f  // Gap between left/right halves
    private var isLandscape = false

    // Column stagger for Moonlander-style layout
    private var columnStaggerEnabled = true
    private val staggerConfig = SplitLayoutConfig()
    private val staggerCalculator = ColumnStaggerCalculator(staggerConfig)

    // JARVIS theme glow effects
    private val glowRenderer = GlowRenderer()
    private var isJarvisTheme = false
    private var activeAgentId: String? = null

    fun setLayoutManager(manager: LayoutManager) {
        layoutManager = manager
        manager.setLayoutChangeListener { layout ->
            currentLayout = layout
            requestLayout()  // Re-measure for new row count
            calculateKeyRects()
            invalidate()
        }
    }

    fun setCommandRegistry(registry: SlashCommandRegistry) {
        commandRegistry = registry
    }

    fun setThemeManager(manager: ThemeManager) {
        themeManager = manager
        manager.setThemeChangeListener { theme ->
            // Update colors from theme
            keyBackground = manager.getKeyBackground()
            keyBackgroundPressed = manager.getKeyBackgroundPressed()
            keyText = manager.getKeyText()
            keySpecial = manager.getKeySpecialBackground()
            backgroundColor = manager.getBackgroundColor()
            keyModifierActive = manager.getKeyModifierActive()
            symbolColor = manager.getSymbolColor()
            numberColor = manager.getNumberColor()
            // Track JARVIS theme for glow effects
            isJarvisTheme = theme.id == "jarvis"
            // BlurMaskFilter requires software rendering for glow effects
            setLayerType(
                if (isJarvisTheme) LAYER_TYPE_SOFTWARE else LAYER_TYPE_HARDWARE,
                null
            )
            invalidate()
        }
        // Apply initial theme
        keyBackground = manager.getKeyBackground()
        keyBackgroundPressed = manager.getKeyBackgroundPressed()
        keyText = manager.getKeyText()
        keySpecial = manager.getKeySpecialBackground()
        backgroundColor = manager.getBackgroundColor()
        keyModifierActive = manager.getKeyModifierActive()
        symbolColor = manager.getSymbolColor()
        numberColor = manager.getNumberColor()
        isJarvisTheme = manager.getCurrentTheme().id == "jarvis"
        // Set initial layer type for glow effects
        setLayerType(
            if (isJarvisTheme) LAYER_TYPE_SOFTWARE else LAYER_TYPE_HARDWARE,
            null
        )
    }

    fun setKeyListener(listener: (KeyAction) -> Unit) {
        keyListener = listener
    }

    fun setContext(context: InputContext) {
        currentContext = context
    }

    fun reset() {
        pressedKeyIndex = -1
        ctrlActive = false
        shiftActive = false
        invalidate()
    }

    fun setCtrlState(active: Boolean) {
        ctrlActive = active
        invalidate()
    }

    fun setShiftState(active: Boolean) {
        shiftActive = active
        invalidate()
    }

    fun setActiveAgent(agentId: String?) {
        activeAgentId = agentId
        invalidate()
    }

    fun setRowHeight(heightDp: Int) {
        // Enforce minimum touch target of 44dp
        rowHeightDp = maxOf(heightDp.toFloat(), minKeyHeightDp)
        requestLayout()
        invalidate()
    }

    fun setSplitMode(mode: SplitMode) {
        splitMode = mode
        calculateKeyRects()
        invalidate()
    }

    fun setSplitGap(gapDp: Float) {
        splitGapDp = gapDp
        calculateKeyRects()
        invalidate()
    }

    private fun shouldSplit(): Boolean {
        return when (splitMode) {
            SplitMode.NONE -> false
            SplitMode.SPLIT -> true
            SplitMode.AUTO -> isLandscape && width > height
        }
    }

    fun performHapticFeedback() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun showCommandPicker(commands: List<String>) {
        // TODO: Show command picker overlay
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)

        // Calculate height based on number of rows
        val rowCount = currentLayout?.rows?.size ?: 4
        val rowHeightPx = (rowHeightDp * density).toInt()
        val desiredHeight = rowCount * rowHeightPx

        // Respect height constraints but prefer our calculated height
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Detect landscape by comparing screen dimensions (more reliable than configuration)
        val displayMetrics = resources.displayMetrics
        isLandscape = displayMetrics.widthPixels > displayMetrics.heightPixels
        calculateKeyRects()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Recalculate on config change as backup
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        post { calculateKeyRects(); invalidate() }
    }

    private fun calculateKeyRects() {
        keyRects.clear()
        val layout = currentLayout ?: return

        val totalRows = layout.rows.size
        val keyHeight = height.toFloat() / totalRows
        val padding = 4f
        val split = shouldSplit()
        val splitGapPx = splitGapDp * density
        val applyStagger = split && columnStaggerEnabled

        var y = 0f
        for ((rowIndex, row) in layout.rows.withIndex()) {
            val keyCount = row.keys.size
            val shouldStaggerRow = applyStagger && staggerCalculator.shouldApplyStagger(rowIndex, totalRows)

            if (split && keyCount > 4) {
                // Split mode: divide keys between left and right halves
                val leftKeys = keyCount / 2
                val rightKeys = keyCount - leftKeys
                val halfWidth = (width - splitGapPx) / 2

                var x = 0f
                for ((index, _) in row.keys.withIndex()) {
                    val isLeftSide = index < leftKeys
                    val sideKeyCount = if (isLeftSide) leftKeys else rightKeys
                    val keyWidth = halfWidth / sideKeyCount

                    if (index == leftKeys) {
                        // Jump to right side after gap
                        x = halfWidth + splitGapPx
                    }

                    // Calculate column stagger offset
                    val columnInHalf = if (isLeftSide) index else index - leftKeys
                    val staggerOffset = if (shouldStaggerRow) {
                        val pos = staggerCalculator.calculatePosition(
                            row = rowIndex,
                            column = columnInHalf,
                            isLeftHalf = isLeftSide,
                            totalColumns = sideKeyCount
                        )
                        pos.yOffset * density
                    } else 0f

                    val rect = RectF(
                        x + padding,
                        y + padding + staggerOffset,
                        x + keyWidth - padding,
                        y + keyHeight - padding + staggerOffset
                    )
                    keyRects.add(rect)
                    x += keyWidth
                }
            } else {
                // Normal mode: full width
                var x = 0f
                val rowKeyWidth = width.toFloat() / keyCount

                for (key in row.keys) {
                    val rect = RectF(
                        x + padding,
                        y + padding,
                        x + rowKeyWidth - padding,
                        y + keyHeight - padding
                    )
                    keyRects.add(rect)
                    x += rowKeyWidth
                }
            }
            y += keyHeight
        }
    }

    /**
     * Enable or disable column stagger for split layout.
     */
    fun setColumnStagger(enabled: Boolean) {
        columnStaggerEnabled = enabled
        calculateKeyRects()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background
        canvas.drawColor(backgroundColor)

        val layout = currentLayout ?: return

        var keyIndex = 0
        for ((rowIndex, row) in layout.rows.withIndex()) {
            for ((keyIndexInRow, key) in row.keys.withIndex()) {
                if (keyIndex >= keyRects.size) break

                val rect = keyRects[keyIndex]
                val isPressed = keyIndex == pressedKeyIndex

                // JARVIS theme glow effects for agent keys
                if (isJarvisTheme) {
                    drawJarvisGlow(canvas, rect, key, isPressed)
                }

                // Key background - highlight CTRL when active, use themed colors
                val isCtrlKey = key.label == "CTRL"
                val isShiftKey = key.label == "SHIFT" || key.label == "⇧"
                keyPaint.color = when {
                    isPressed -> keyBackgroundPressed
                    isCtrlKey && ctrlActive -> keyModifierActive
                    isShiftKey && shiftActive -> keyModifierActive
                    key.isSpecial -> keySpecial
                    else -> keyBackground
                }
                canvas.drawRoundRect(rect, 8f, 8f, keyPaint)

                // Key text
                textPaint.color = keyText
                textPaint.textSize = rect.height() * 0.35f
                val textY = rect.centerY() + textPaint.textSize / 3

                canvas.drawText(key.label, rect.centerX(), textY, textPaint)

                keyIndex++
            }
        }

        // Clear glow filters after rendering
        if (isJarvisTheme) {
            glowRenderer.clearFilters()
        }
    }

    /**
     * Draw JARVIS-style glow effects for special keys.
     * Supports both local agents and Goblin Forge goblins.
     */
    private fun drawJarvisGlow(
        canvas: Canvas,
        rect: RectF,
        key: dev.kymera.keyboard.layouts.Key,
        isPressed: Boolean
    ) {
        val action = key.action

        when (action) {
            // === GOBLIN FORGE KEYS ===
            is KeyAction.GforgeSpawn -> {
                // Goblins get agent-colored glow based on agent type
                val color = when (action.agent) {
                    "claude" -> "#2196F3"    // Blue for Claude
                    "gemini" -> "#9C27B0"    // Purple for Gemini
                    "codex" -> "#4CAF50"     // Green for Codex
                    "ollama" -> "#FF9800"    // Orange for Ollama
                    else -> "#00D4FF"        // Cyan default
                }
                val glowColor = android.graphics.Color.parseColor(color + if (isPressed) "B0" else "60")
                glowRenderer.drawKeyGlow(canvas, rect, glowColor, isActive = isPressed)
            }
            is KeyAction.GforgeTop -> {
                // Dashboard - orange glow (orchestrator color)
                val glowColor = android.graphics.Color.parseColor(if (isPressed) "#FF9800B0" else "#FF980060")
                glowRenderer.drawKeyGlow(canvas, rect, glowColor, isActive = isPressed)
            }
            is KeyAction.GforgeAttach -> {
                // Attach - green glow (connect)
                if (isPressed) {
                    glowRenderer.drawKeyGlow(
                        canvas, rect,
                        android.graphics.Color.parseColor("#4CAF50B0"),
                        isActive = true
                    )
                }
            }
            is KeyAction.GforgeKill -> {
                // Kill - red glow (danger)
                val glowColor = android.graphics.Color.parseColor(if (isPressed) "#FF0000B0" else "#FF000060")
                glowRenderer.drawKeyGlow(canvas, rect, glowColor, isActive = isPressed)
            }
            is KeyAction.GforgeLogs -> {
                // Logs - cyan glow (info)
                if (isPressed) {
                    glowRenderer.drawKeyGlow(
                        canvas, rect,
                        android.graphics.Color.parseColor("#00D4FF80"),
                        isActive = true
                    )
                }
            }
            is KeyAction.GforgeRun -> {
                // Build/Test - green glow when pressed
                if (isPressed) {
                    glowRenderer.drawKeyGlow(
                        canvas, rect,
                        android.graphics.Color.parseColor("#4CAF50B0"),
                        isActive = true
                    )
                }
            }

            // === LEGACY LOCAL AGENT KEYS ===
            is KeyAction.SpawnAgent -> {
                val isActive = activeAgentId == action.agentId
                glowRenderer.drawAgentKeyGlow(canvas, rect, action.agentId, isActive || isPressed)
            }
            is KeyAction.ExpandContext -> {
                glowRenderer.drawContextKeyGlow(canvas, rect, isExpand = true, isActive = isPressed)
            }
            is KeyAction.ResetContext -> {
                glowRenderer.drawContextKeyGlow(canvas, rect, isExpand = false, isActive = isPressed)
            }
            is KeyAction.ExecuteRun,
            is KeyAction.ExecuteSimulate,
            is KeyAction.RefineSelection,
            is KeyAction.AgentStep -> {
                // Thumb cluster execution keys - cyan glow when pressed
                if (isPressed) {
                    glowRenderer.drawKeyGlow(
                        canvas, rect,
                        android.graphics.Color.parseColor("#00D4FF80"),
                        isActive = true
                    )
                }
            }
            is KeyAction.RecallMemory,
            is KeyAction.GenerateDoc,
            is KeyAction.HoldToQuery -> {
                // Left thumb cluster - orange glow when pressed
                if (isPressed) {
                    glowRenderer.drawKeyGlow(
                        canvas, rect,
                        android.graphics.Color.parseColor("#FF6B0080"),
                        isActive = true
                    )
                }
            }
            else -> {
                // No glow for other keys
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressedKeyIndex = findKeyAt(event.x, event.y)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Update pressed key for visual feedback (drag off key cancels)
                val newKey = findKeyAt(event.x, event.y)
                if (newKey != pressedKeyIndex) {
                    pressedKeyIndex = newKey
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                handleKeyPress(event.x, event.y)
                pressedKeyIndex = -1
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedKeyIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findKeyAt(x: Float, y: Float): Int {
        for ((index, rect) in keyRects.withIndex()) {
            if (rect.contains(x, y)) {
                return index
            }
        }
        return -1
    }

    private fun handleKeyPress(x: Float, y: Float) {
        val keyIndex = findKeyAt(x, y)
        if (keyIndex < 0) return

        val layout = currentLayout ?: return

        // Find the key
        var index = 0
        for (row in layout.rows) {
            for (key in row.keys) {
                if (index == keyIndex) {
                    keyListener?.invoke(key.action)
                    return
                }
                index++
            }
        }
    }

}
