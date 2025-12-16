package dev.kymera.keyboard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Prediction bar that shows word suggestions above the keyboard.
 * Displays up to 3 suggestions with the primary suggestion highlighted.
 */
class PredictionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Suggestions
    private var suggestions = listOf<String>()
    private var onSuggestionSelected: ((String) -> Unit)? = null

    // Touch tracking
    private var pressedIndex = -1

    // Drawing
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF252525.toInt()
        style = Paint.Style.FILL
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3A3A3A.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 40f
    }

    private val primaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E88E5.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 44f
        isFakeBoldText = true
    }

    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3A3A3A.toInt()
        style = Paint.Style.FILL
    }

    private val suggestionRects = mutableListOf<RectF>()

    /**
     * Set the suggestions to display.
     * First suggestion is treated as primary (highlighted).
     */
    fun setSuggestions(newSuggestions: List<String>) {
        suggestions = newSuggestions.take(3)
        calculateRects()
        invalidate()
    }

    /**
     * Clear all suggestions.
     */
    fun clearSuggestions() {
        suggestions = emptyList()
        invalidate()
    }

    /**
     * Set callback for when a suggestion is selected.
     */
    fun setOnSuggestionSelectedListener(listener: (String) -> Unit) {
        onSuggestionSelected = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateRects()
    }

    private fun calculateRects() {
        suggestionRects.clear()

        if (suggestions.isEmpty()) return

        val itemWidth = width.toFloat() / suggestions.size
        val padding = 4f

        for (i in suggestions.indices) {
            suggestionRects.add(
                RectF(
                    i * itemWidth + padding,
                    padding,
                    (i + 1) * itemWidth - padding,
                    height - padding
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (suggestions.isEmpty()) {
            // Show hint when no suggestions
            textPaint.alpha = 100
            canvas.drawText(
                "Swipe to type",
                width / 2f,
                height / 2f + textPaint.textSize / 3,
                textPaint
            )
            textPaint.alpha = 255
            return
        }

        // Draw suggestions
        for ((index, suggestion) in suggestions.withIndex()) {
            val rect = suggestionRects.getOrNull(index) ?: continue

            // Pressed state
            if (index == pressedIndex) {
                canvas.drawRoundRect(rect, 8f, 8f, pressedPaint)
            }

            // Text
            val paint = if (index == 0) primaryTextPaint else textPaint
            val textY = rect.centerY() + paint.textSize / 3

            canvas.drawText(suggestion, rect.centerX(), textY, paint)

            // Divider (except after last item)
            if (index < suggestions.size - 1) {
                canvas.drawLine(
                    rect.right,
                    rect.top + 10,
                    rect.right,
                    rect.bottom - 10,
                    dividerPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressedIndex = findSuggestionAt(event.x, event.y)
                if (pressedIndex >= 0) {
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val newIndex = findSuggestionAt(event.x, event.y)
                if (newIndex != pressedIndex) {
                    pressedIndex = newIndex
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                val index = findSuggestionAt(event.x, event.y)
                if (index >= 0 && index == pressedIndex) {
                    suggestions.getOrNull(index)?.let { suggestion ->
                        onSuggestionSelected?.invoke(suggestion)
                    }
                }
                pressedIndex = -1
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                invalidate()
            }
        }

        return super.onTouchEvent(event)
    }

    private fun findSuggestionAt(x: Float, y: Float): Int {
        for ((index, rect) in suggestionRects.withIndex()) {
            if (rect.contains(x, y)) {
                return index
            }
        }
        return -1
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 56 * resources.displayMetrics.density.toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }
}
