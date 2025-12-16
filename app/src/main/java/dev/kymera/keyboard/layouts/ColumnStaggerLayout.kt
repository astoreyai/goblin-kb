package dev.kymera.keyboard.layouts

/**
 * Column stagger configuration for ergonomic keyboard layouts.
 * Based on Moonlander/Ergodox column offsets for natural finger positioning.
 *
 * Offsets are in dp (density-independent pixels), positive = down, negative = up.
 */
data class ColumnStagger(
    val columnOffsets: List<Float>  // Vertical offset per column in dp
) {
    /**
     * Get offset for a specific column index.
     * Returns 0 if index is out of bounds.
     */
    fun getOffset(columnIndex: Int): Float {
        return columnOffsets.getOrElse(columnIndex) { 0f }
    }

    /**
     * Get mirrored version (for right hand).
     */
    fun mirrored(): ColumnStagger {
        return ColumnStagger(columnOffsets.reversed())
    }
}

/**
 * Pre-defined column stagger configurations.
 */
object MoonlanderStagger {
    /**
     * Standard Moonlander column offsets for left hand.
     * Columns: pinky -> ring -> middle -> index -> index-reach
     * Middle finger column is highest (most offset down).
     */
    val LEFT_HAND = ColumnStagger(
        columnOffsets = listOf(0f, 4f, 10f, 6f, 2f)  // Q W E R T columns
    )

    /**
     * Standard Moonlander column offsets for right hand.
     * Mirror of left hand.
     */
    val RIGHT_HAND = ColumnStagger(
        columnOffsets = listOf(2f, 6f, 10f, 4f, 0f)  // Y U I O P columns
    )

    /**
     * Subtle stagger for mobile (smaller offsets).
     */
    val LEFT_HAND_MOBILE = ColumnStagger(
        columnOffsets = listOf(0f, 2f, 5f, 3f, 1f)
    )

    val RIGHT_HAND_MOBILE = ColumnStagger(
        columnOffsets = listOf(1f, 3f, 5f, 2f, 0f)
    )

    /**
     * No stagger (flat layout).
     */
    val FLAT = ColumnStagger(
        columnOffsets = listOf(0f, 0f, 0f, 0f, 0f)
    )
}

/**
 * Split layout configuration.
 */
data class SplitLayoutConfig(
    val leftStagger: ColumnStagger = MoonlanderStagger.LEFT_HAND_MOBILE,
    val rightStagger: ColumnStagger = MoonlanderStagger.RIGHT_HAND_MOBILE,
    val gapDp: Float = 60f,  // Gap between left and right halves
    val thumbClusterEnabled: Boolean = true,
    val hudStripEnabled: Boolean = true,
    val agentRowEnabled: Boolean = true
)

/**
 * Position of a key in a split staggered layout.
 */
data class StaggeredKeyPosition(
    val row: Int,
    val column: Int,
    val isLeftHalf: Boolean,
    val xOffset: Float,  // Additional X offset in dp
    val yOffset: Float   // Additional Y offset from column stagger in dp
)

/**
 * Calculator for staggered key positions.
 */
class ColumnStaggerCalculator(
    private val config: SplitLayoutConfig = SplitLayoutConfig()
) {
    /**
     * Calculate key position with column stagger applied.
     *
     * @param row Row index (0-based from top)
     * @param column Column index within the half (0-based from left)
     * @param isLeftHalf Whether this key is in the left half
     * @param totalColumns Total columns in this half
     * @return StaggeredKeyPosition with offsets
     */
    fun calculatePosition(
        row: Int,
        column: Int,
        isLeftHalf: Boolean,
        totalColumns: Int
    ): StaggeredKeyPosition {
        val stagger = if (isLeftHalf) config.leftStagger else config.rightStagger

        // Get column stagger offset
        val yOffset = stagger.getOffset(column)

        // X offset is 0 for now (could add horizontal stagger)
        val xOffset = 0f

        return StaggeredKeyPosition(
            row = row,
            column = column,
            isLeftHalf = isLeftHalf,
            xOffset = xOffset,
            yOffset = yOffset
        )
    }

    /**
     * Check if a row should have stagger applied.
     * Typically only alpha rows (QWERTY rows) are staggered, not toolbar rows.
     */
    fun shouldApplyStagger(rowIndex: Int, totalRows: Int): Boolean {
        // Don't stagger first row (agent/toolbar) or last row (thumb cluster)
        return rowIndex in 1 until (totalRows - 1)
    }
}
