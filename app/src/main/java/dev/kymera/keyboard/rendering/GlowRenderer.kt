package dev.kymera.keyboard.rendering

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import dev.kymera.keyboard.ui.JarvisTheme

/**
 * Renders JARVIS-style glow effects for keys and UI elements.
 * Uses BlurMaskFilter for outer glow effects.
 *
 * NOTE: BlurMaskFilter requires software rendering. The view using this
 * renderer should call setLayerType(LAYER_TYPE_SOFTWARE, null) for glow
 * effects to work, or disable hardware acceleration for the view.
 */
class GlowRenderer {

    // Track if glow is enabled (can be disabled for performance)
    var glowEnabled = true

    // Glow paint with blur effect
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // Inner glow paint (softer)
    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // HUD strip paint
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val hudTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 28f
    }

    // Cached blur filters
    private val outerGlowFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER)
    private val innerGlowFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
    private val softGlowFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.OUTER)

    /**
     * Draw a key with outer glow effect.
     *
     * @param canvas The canvas to draw on
     * @param rect The key rectangle
     * @param glowColor The glow color (usually with alpha)
     * @param isActive Whether the key is currently active (stronger glow)
     * @param cornerRadius Corner radius for rounded rect
     */
    fun drawKeyGlow(
        canvas: Canvas,
        rect: RectF,
        glowColor: Int,
        isActive: Boolean = false,
        cornerRadius: Float = 8f
    ) {
        glowPaint.color = glowColor
        glowPaint.maskFilter = if (isActive) softGlowFilter else outerGlowFilter
        glowPaint.strokeWidth = if (isActive) 4f else 3f

        // Draw the glow outline
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)

        // For active keys, add inner glow
        if (isActive) {
            innerGlowPaint.color = Color.argb(
                50,
                Color.red(glowColor),
                Color.green(glowColor),
                Color.blue(glowColor)
            )
            innerGlowPaint.maskFilter = innerGlowFilter
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, innerGlowPaint)
        }
    }

    /**
     * Draw glow effect for agent key based on agent ID.
     */
    fun drawAgentKeyGlow(
        canvas: Canvas,
        rect: RectF,
        agentId: String,
        isActive: Boolean = false
    ) {
        val color = when (agentId) {
            "coding" -> Color.parseColor(JarvisTheme.CODING_AGENT_COLOR)
            "research" -> Color.parseColor(JarvisTheme.RESEARCH_AGENT_COLOR)
            "quant" -> Color.parseColor(JarvisTheme.QUANT_AGENT_COLOR)
            "orchestrator" -> Color.parseColor(JarvisTheme.ORCHESTRATOR_COLOR)
            else -> Color.parseColor(JarvisTheme.JARVIS_CYAN)
        }

        // Add alpha for glow effect
        val glowColor = Color.argb(if (isActive) 180 else 100, Color.red(color), Color.green(color), Color.blue(color))
        drawKeyGlow(canvas, rect, glowColor, isActive)
    }

    /**
     * Draw context indicator glow (CTX+/CTX-).
     */
    fun drawContextKeyGlow(
        canvas: Canvas,
        rect: RectF,
        isExpand: Boolean,
        isActive: Boolean = false
    ) {
        val baseColor = if (isExpand) {
            Color.parseColor(JarvisTheme.CONTEXT_EXPAND)
        } else {
            Color.parseColor(JarvisTheme.CONTEXT_RESET)
        }
        val glowColor = Color.argb(if (isActive) 180 else 100, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
        drawKeyGlow(canvas, rect, glowColor, isActive)
    }

    /**
     * Draw HUD strip at the bottom of the keyboard.
     *
     * @param canvas The canvas to draw on
     * @param rect The strip rectangle
     * @param items List of HUD items to display
     */
    fun drawHUDStrip(
        canvas: Canvas,
        rect: RectF,
        items: List<HUDItem>
    ) {
        // Background with slight transparency
        hudPaint.color = Color.parseColor(JarvisTheme.HUD_PANEL)
        canvas.drawRect(rect, hudPaint)

        // Top border glow
        glowPaint.color = Color.parseColor(JarvisTheme.JARVIS_CYAN)
        glowPaint.maskFilter = outerGlowFilter
        glowPaint.strokeWidth = 2f
        canvas.drawLine(rect.left, rect.top, rect.right, rect.top, glowPaint)

        // Draw HUD items
        if (items.isEmpty()) return

        val itemWidth = rect.width() / items.size
        var x = rect.left

        for (item in items) {
            val itemRect = RectF(x, rect.top, x + itemWidth, rect.bottom)
            drawHUDItem(canvas, itemRect, item)
            x += itemWidth
        }
    }

    private fun drawHUDItem(canvas: Canvas, rect: RectF, item: HUDItem) {
        val centerX = rect.centerX()
        val centerY = rect.centerY()

        // Label (smaller, above)
        hudTextPaint.textSize = 20f
        hudTextPaint.color = Color.parseColor(JarvisTheme.JARVIS_WHITE)
        hudTextPaint.alpha = 180
        canvas.drawText(item.label, centerX, centerY - 8f, hudTextPaint)

        // Value (larger, below)
        hudTextPaint.textSize = 26f
        hudTextPaint.color = Color.parseColor(item.color)
        hudTextPaint.alpha = 255
        canvas.drawText(item.value, centerX, centerY + 16f, hudTextPaint)

        // Progress bar if present
        item.progress?.let { progress ->
            val barWidth = rect.width() * 0.8f
            val barHeight = 4f
            val barLeft = rect.left + (rect.width() - barWidth) / 2
            val barTop = rect.bottom - 10f

            // Background bar
            hudPaint.color = Color.parseColor("#333333")
            canvas.drawRoundRect(
                barLeft, barTop,
                barLeft + barWidth, barTop + barHeight,
                2f, 2f, hudPaint
            )

            // Progress fill
            hudPaint.color = Color.parseColor(item.color)
            canvas.drawRoundRect(
                barLeft, barTop,
                barLeft + barWidth * progress, barTop + barHeight,
                2f, 2f, hudPaint
            )
        }
    }

    /**
     * Draw pulsing glow effect (for animated elements).
     *
     * @param canvas The canvas to draw on
     * @param rect The rectangle to glow
     * @param color Base glow color
     * @param pulsePhase Phase of the pulse animation (0.0 to 1.0)
     */
    fun drawPulsingGlow(
        canvas: Canvas,
        rect: RectF,
        color: Int,
        pulsePhase: Float
    ) {
        // Modulate alpha based on pulse phase
        val alpha = (80 + 80 * kotlin.math.sin(pulsePhase * 2 * Math.PI)).toInt()
        val glowColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        drawKeyGlow(canvas, rect, glowColor, isActive = true)
    }

    /**
     * Clear mask filters (call when done with glow rendering to avoid affecting other draws).
     */
    fun clearFilters() {
        glowPaint.maskFilter = null
        innerGlowPaint.maskFilter = null
    }
}

/**
 * Item displayed in the HUD strip.
 */
data class HUDItem(
    val label: String,
    val value: String,
    val color: String = JarvisTheme.JARVIS_CYAN,
    val progress: Float? = null  // Optional progress (0.0 to 1.0)
)
